package com.bk.railway.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bk.railway.helper.OrderHelper;
import com.bk.railway.servlet.Constants;

public class QueryHelper {
    private final static String QUERY_KEYWORD = "jsp";
    private final static Logger LOG = Logger.getLogger(QueryHelper.class.getName());
    public static class QueryRequest {
        
        public final String person_id;
        public final String from_station;
        public final String to_station;
        public final String getin_date;
        public final int order_qty;
        public final Map<String,String> formData = new HashMap<String,String>();

        public QueryRequest(String person_id,String from_station,String to_station,String getin_date,int order_qty) {
            this.person_id = person_id;
            this.from_station = from_station;
            this.to_station = to_station;
            this.getin_date = getin_date;
            this.order_qty = order_qty;
            
            formData.put("person_id", person_id);
            formData.put("from_station", from_station);
            formData.put("to_station", to_station);
            formData.put("getin_date", getin_date);
            formData.put("train_type", "*4");
            formData.put("getin_start_dtime", "05:00");
            formData.put("getin_end_dtime", "23:59");
            formData.put("order_qty_str", String.valueOf(order_qty));
            formData.put("returnTicket", "0");
        }
        
    }
    
    public static class QueryResponse {
        public final OrderHelper.OrderRequest orderRequest;
        public final String bordingTime;
        public QueryResponse(String person_id,String from_station,String to_station,String getin_date,String train_no,int order_qty,String bordingTime) {
            this.bordingTime = bordingTime;
            orderRequest = new OrderHelper.OrderRequest(person_id,from_station,to_station,getin_date,train_no,order_qty);
        }
        public QueryResponse(OrderHelper.OrderRequest orderRequest,String bordingTime) {
            this.orderRequest = orderRequest;
            this.bordingTime = bordingTime;
        }
        
    }
    
    private final QueryRequest m_queryRequest;
    
    public QueryHelper(QueryRequest request) {
        m_queryRequest = request;
    }
    
    public QueryResponse[] doQuery() throws Exception {
        
        final StringBuffer nextActionStringBuffer = new StringBuffer();
        final Map<String,String> cookies = getCookies(m_queryRequest.formData,nextActionStringBuffer);
        final String nextActionString = nextActionStringBuffer.toString();
        
        LOG.log(Level.INFO,"cookies=" + cookies);
        
        if(null == cookies || cookies.isEmpty()) {
            return null;
        }
        
        LOG.log(Level.INFO,"nextActionString=" + nextActionString);
        
        if(null == nextActionString || "".equals(nextActionString)) {
            return null;
        }
        
        final String answer = TaskUtil.resolveRandom("http://210.71.181.60",cookies, "http://railway.hinet.net/check_ctno1.jsp");
        LOG.info("answer=" + answer);
        
        if(null == answer || answer.length() != 5) {
            return null;
        }
        
        m_queryRequest.formData.put("randInput", answer);
        final QueryResponse[] responses =  queryTicket(m_queryRequest.formData,cookies,nextActionString);
        for(QueryResponse response : responses) {
            LOG.info(response.bordingTime + "\t" + response.orderRequest.person_id + "\t" + Constants.STATION_ID_TOMAP.get(response.orderRequest.from_station) + "\t" + Constants.STATION_ID_TOMAP.get(response.orderRequest.to_station) + "\t" + response.orderRequest.train_no + "\t" + response.orderRequest.order_qty);
        }
        return responses;
    }
    
    private QueryResponse[] queryTicket(Map<String,String> formData,Map<String,String> cookies,String nextActionString) throws Exception{
        
        final Pattern hrefPattern = Pattern.compile("\\?(\\S+)>");
        final Pattern bordingTimePattern = Pattern.compile("(\\d{4}/\\d{2}/\\d{2}\\s\\d{2}:\\d{2})");
        //2012/08/23 08:00
        //final String jsessionid = NetworkUtil.getJSESSIONID(cookies);
        final String sgetData = "?" + NetworkUtil.toPostData(formData);
        final String url = "http://210.71.181.60/" + nextActionString + sgetData;
        final List<QueryResponse> retOrderRequestList = new ArrayList<QueryResponse>(8);
        
        LOG.info("url=" + url);
        
        URLConnection conn = new URL(url).openConnection();
        conn.addRequestProperty("User-Agent",TaskUtil.USER_AGENT );
        NetworkUtil.putCookies(conn,cookies);
        conn.addRequestProperty("Referer", "http://210.71.181.60/check_csearch.jsp");
        
        conn.setDoInput(true);
        conn.connect();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),Constants.WEB_ENCODING));
        String line = null;

        do {
            
            line = br.readLine();
            if(line != null) {
                LOG.info(line);
                int startpos = line.indexOf(QUERY_KEYWORD);
                if(startpos > 0) {
                    do {
                        if(startpos > 0 && startpos + QUERY_KEYWORD.length() < line.length()) {
                            line = line.substring(startpos + QUERY_KEYWORD.length());
                        }
                        else {
                            break;
                        }
                        //Find bording time
                        final Matcher bordingTimeMatcher = bordingTimePattern.matcher(line);
                        if(bordingTimeMatcher.find()) {
                            final String bordingTimeString = bordingTimeMatcher.group(1);
                            
                            final Matcher hrefMatcher = hrefPattern.matcher(line);
                            if(hrefMatcher.find()) {
                                final String orderString = hrefMatcher.group(1);
                                LOG.info("orderString=" + orderString);
                                //analyze order string
                                final Map<String,String> orderDataMap = NetworkUtil.fromPostData(orderString);
    
                                //orderDataMap={to_station=146, getin_date=2012/08/23-00, person_id=C196126834, order_qty_str=01, from_station=100, returnTicket=0, train_no=135}
                                if(orderDataMap.containsKey(Constants.TO_STATATION) &&
                                        orderDataMap.containsKey(Constants.GETIN_DATE) &&
                                        orderDataMap.containsKey(Constants.PERSON_ID) &&
                                        orderDataMap.containsKey(Constants.ORDER_QTY) &&
                                        orderDataMap.containsKey(Constants.FROM_STATATION) &&
                                        orderDataMap.containsKey(Constants.TRAIN_NO)) {
                                    //public OrderRequest(String person_id,String from_station,String to_station,String getin_date,String train_no,int order_qty) {
                                    final OrderHelper.OrderRequest orderRequest = new OrderHelper.OrderRequest(orderDataMap.get(Constants.PERSON_ID),
                                            orderDataMap.get(Constants.FROM_STATATION),
                                            orderDataMap.get(Constants.TO_STATATION),
                                            GetInDateProxy.bookableStringToDate(orderDataMap.get(Constants.GETIN_DATE)),
                                            orderDataMap.get(Constants.TRAIN_NO),
                                            Integer.parseInt(orderDataMap.get(Constants.ORDER_QTY)));
                                    
                                    retOrderRequestList.add(new QueryResponse(orderRequest,bordingTimeString));
                                    
                                }
                                else {
                                    LOG.info("orderDataMap=" + orderDataMap);
                                }
                                
                                
                            }
                            startpos = line.indexOf(QUERY_KEYWORD);
                        }
                    }while(startpos > 0);
                }
            }
        }while(line != null);
        
        conn.getInputStream().close();
        
        return retOrderRequestList.toArray(new QueryResponse[0]);
    }
    
    private Map<String,String> getCookies(Map<String,String> formData,StringBuffer nextActionStringBuffer) throws Exception {
        final Map<String,String> cookies = new HashMap<String,String>();
        final Pattern actionPattern = Pattern.compile("action=\"(\\S+)\"");
        final String formDataEncodedString = NetworkUtil.toPostData(formData);
        LOG.log(Level.INFO, "formDataEncodedString=" + formDataEncodedString);
        
        URLConnection conn = new URL("http://210.71.181.60/check_csearch.jsp").openConnection();
        
        try {
        
            conn.addRequestProperty("User-Agent",TaskUtil.USER_AGENT );
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.addRequestProperty("Content-Length", String.valueOf(formDataEncodedString.length()));
            conn.addRequestProperty("Referer", "http://210.71.181.60/csearch.htm");
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(NetworkUtil.toPostData(formData));
            osw.flush();
            conn.getOutputStream().close();
            conn.connect();
    
            for(Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                if(entry.getKey() != null && "set-cookie".equals(entry.getKey().toLowerCase())) {
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
                //System.out.println(entry.getKey()+"=");
                for(String v : entry.getValue()) {
                    LOG.log(Level.INFO, "\t" + v);
                    //System.out.println("\t" + v);
                }
            }
           
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),Constants.WEB_ENCODING));
            String line = null;
            
            do {
                
                line = br.readLine();
                //System.out.println(">>>" + line);
                if(line != null) {
                    final Matcher matcher = actionPattern.matcher(line);
                    if(matcher.find()) {
                        nextActionStringBuffer.append(matcher.group(1));
                    }
                }
            }while(line != null);
            
           
        
        }
        finally {
            //conn.getOutputStream().close();
            conn.getInputStream().close();
        }
       
        return cookies;
    }


}
