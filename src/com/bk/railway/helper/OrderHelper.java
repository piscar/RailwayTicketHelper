package com.bk.railway.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bk.railway.servlet.Constants;

public class OrderHelper {
    public final static String ORDER_KEYWORD = new String("電腦代碼：");
    private final static Logger LOG = Logger.getLogger(OrderHelper.class.getName());
    private final static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.57 Safari/536.11";
    private final static int CONRENT_HARDLIMIT = 10240;
    
    public static class OrderRequest {
        public final String person_id;
        public final String from_station;
        public final String to_station;
        public final String getin_date;
        public final String train_no;
        public final int order_qty;
        public final Map<String,String> formData = new HashMap<String,String>();
        public OrderRequest(String person_id,String from_station,String to_station,String getin_date,String train_no,int order_qty) {
            if(null == person_id || null == from_station || null == to_station || null == getin_date || null == train_no) {
                throw new IllegalArgumentException();
            }
            
            if("".equals(person_id) || "".equals(from_station) || "".equals(to_station) || "".equals(getin_date) || "".equals(train_no)) {
                throw new IllegalArgumentException();
            }
            
            if(order_qty < 1 || order_qty > 5) {
                throw new IllegalArgumentException("order_qty must be 0 < order_qty < 6");
            }
            
            this.person_id = person_id;
            this.from_station = from_station;
            this.to_station = to_station;
            this.getin_date = getin_date;
            this.train_no = train_no;
            this.order_qty = order_qty;
            
            formData.put("person_id", person_id);
            formData.put("from_station", from_station);
            formData.put("to_station", to_station);
            formData.put("getin_date", getin_date);
            formData.put("train_no", train_no);
            formData.put("order_qty_str", String.valueOf(order_qty));
            formData.put("n_order_qty_str", "0");
            formData.put("d_order_qty_str", "0");
            formData.put("b_order_qty_str", "0");
            formData.put("returnTicket", "0");
        }
    }
    
    public static class OrderResponse {
        public final String person_id;
        public final String orderno;
        OrderResponse(String person_id,String orderno) {
            this.person_id = person_id;
            this.orderno = orderno;
        }
    }
    
    private final OrderRequest m_orderRequest;
    public OrderHelper(OrderRequest request) {
        m_orderRequest = request;
    }
    
    
    public OrderResponse doOrder(StringBuffer contentBuffer) throws Exception {
        final StringBuffer nextActionStringBuffer = new StringBuffer();
        final Map<String,String> cookies = getCookies(m_orderRequest.formData,nextActionStringBuffer);
        final String nextActionString = nextActionStringBuffer.toString();
        
        LOG.log(Level.INFO,"cookies=" + cookies);
        
        if(null == cookies || cookies.isEmpty()) {
            return null;
        }
        
        LOG.log(Level.INFO,"nextActionString=" + nextActionString);
        
        if(null == nextActionString || "".equals(nextActionString)) {
            return null;
        }
        
        final String answer = resolveRandom(cookies);
        LOG.info("answer=" + answer);
        
        if(null == answer || answer.length() != 5) {
            return null;
        }
        m_orderRequest.formData.remove("n_order_qty_str");
        m_orderRequest.formData.remove("d_order_qty_str");
        m_orderRequest.formData.remove("b_order_qty_str");
        
        m_orderRequest.formData.put("randInput", answer);
        
        final String ticketno = bookTicket(m_orderRequest.formData,cookies,contentBuffer,nextActionString);
        LOG.info("person_id=" + m_orderRequest.person_id + " ticketno=" + ticketno);
        
        if(null == ticketno || "".equals(ticketno)) {
            return null;
        }
        
        LOG.log(Level.INFO,"retuen fake response");
        return new OrderResponse(m_orderRequest.person_id,ticketno);
    }
    
    private String bookTicket(Map<String,String> formData,Map<String,String> cookies,StringBuffer contentBuffer,String nextActionString) throws Exception{
        
        final Pattern pattern = Pattern.compile("(\\d{5,7})");
       
        //final String jsessionid = NetworkUtil.getJSESSIONID(cookies);
        final String sgetData = "?" + NetworkUtil.toPostData(formData);
        final String url = "http://railway.hinet.net/" + nextActionString + sgetData;
        
        String ticketno = null;
        
        System.out.println("url=" + url);
        
        URLConnection conn = new URL(url).openConnection();
        conn.addRequestProperty("User-Agent",USER_AGENT );
        NetworkUtil.putCookies(conn,cookies);
        conn.addRequestProperty("Referer", "http://railway.hinet.net/check_ctno1.jsp");
        
        conn.setDoInput(true);
        conn.connect();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),Constants.WEB_ENCODING));
        String line = null;

        do {
            
            line = br.readLine();
            if(line != null) {
                if(contentBuffer != null && contentBuffer.length() + line.length() < CONRENT_HARDLIMIT) {
                    contentBuffer.append(line).append("\n");
                }
                LOG.info(line);
                int startpos = line.indexOf(ORDER_KEYWORD);
                if(startpos > 0) {
                    final Matcher matcher = pattern.matcher(line.substring(startpos + ORDER_KEYWORD.length()));
                    if(matcher.find()) {
                        ticketno = matcher.group(1);
                    }
                }
            }
        }while(line != null);
        
        //debugOut.flush();
        //debugOut.close();
        
        conn.getInputStream().close();
        
        return ticketno;
    }
    
    
    private String resolveRandom(Map<String,String> cookies) throws Exception {
        String jsessionid = NetworkUtil.getJSESSIONID(cookies);
        String url = "http://railway.hinet.net/ImageOut.jsp;jsessionid=" + jsessionid;
        LOG.info("resolveRandom url=" + url);
        
        final URLConnection conn = new URL(url).openConnection();
        
        final URL hostCaptchaURL = new URL("http://ec2-46-137-229-229.ap-southeast-1.compute.amazonaws.com:8016/servlets/handle");
        LOG.info("resolveRandom hostCaptchaURL=" + hostCaptchaURL);
        
        final URLConnection hostCaptchaConn = hostCaptchaURL.openConnection();
        
        try {
            NetworkUtil.putCookies(conn,cookies);
            conn.addRequestProperty("Referer", "http://railway.hinet.net/check_ctno1.jsp");
            conn.addRequestProperty("User-Agent",USER_AGENT );
            conn.setDoInput(true);
            conn.connect();
            
            
            hostCaptchaConn.setReadTimeout(60*1000); //60seconds
            hostCaptchaConn.setDoInput(true);
            hostCaptchaConn.setDoOutput(true);
            
            final InputStream is = conn.getInputStream();
            final byte[] buffer = new byte[4096];
            int nByteRead = 0;
            
            do {
                nByteRead = is.read(buffer);
                if(nByteRead > 0) {
                    hostCaptchaConn.getOutputStream().write(buffer, 0, nByteRead);
                }
            }while(nByteRead > 0);
            
            hostCaptchaConn.getOutputStream().flush();
            hostCaptchaConn.connect();
            
            final BufferedReader br = new BufferedReader(new InputStreamReader(hostCaptchaConn.getInputStream()));
            
            return br.readLine();
            
        }
        finally {
            if(conn != null) {
                conn.getInputStream().close();
            }
            if(hostCaptchaConn != null) {
                hostCaptchaConn.getInputStream().close();
                hostCaptchaConn.getOutputStream().close();
            }
            
        }
      
    }
    
    private Map<String,String> getCookies(Map<String,String> formData,StringBuffer nextActionStringBuffer) throws Exception {
        final Map<String,String> cookies = new HashMap<String,String>();
        final Pattern actionPattern = Pattern.compile("action=\"(\\S+)\"");
        final String formDataEncodedString = NetworkUtil.toPostData(formData);
        LOG.log(Level.INFO, "formDataEncodedString=" + formDataEncodedString);
        
        URLConnection conn = new URL("http://railway.hinet.net/check_ctno1.jsp").openConnection();
        
        try {
        
            conn.addRequestProperty("User-Agent",USER_AGENT );
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.addRequestProperty("Content-Length", String.valueOf(formDataEncodedString.length()));
            conn.addRequestProperty("Referer", "http://railway.hinet.net/ctno1.htm");
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(NetworkUtil.toPostData(formData));
            osw.flush();
            
            conn.connect();
    
            for(Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                if("set-cookie".equals(entry.getKey().toLowerCase())) {
                    for(String v : entry.getValue()) {
                        for(String token : v.split(";")) {
                            final int eqpos = token.indexOf("=");
                            if(eqpos > 0) {
                                String key = token.substring(0, eqpos);
                                String value = token.substring(eqpos + 1);
                                if(!"expires".equals(key.toLowerCase())) {
                                    cookies.put(key, value);
                                }
                            }
                        }
                    }
                }
                
                LOG.log(Level.INFO, entry.getKey()+"=");
                for(String v : entry.getValue()) {
                    LOG.log(Level.INFO, "\t" + v);
                }
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),Constants.WEB_ENCODING));
            String line = null;
            
            do {
                
                line = br.readLine();
                if(line != null) {
                    final Matcher matcher = actionPattern.matcher(line);
                    if(matcher.find()) {
                        nextActionStringBuffer.append(matcher.group(1));
                    }
                }
            }while(line != null);
            
            
        
        }
        finally {
            conn.getOutputStream().close();
            conn.getInputStream().close();
        }
       
        return cookies;
    }

    
    
}
