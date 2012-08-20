package com.bk.railway.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

public class TaskUtil {
    
    public final static TimeZone TIMEZONE = TimeZone.getTimeZone("Asia/Taipei");
    public final static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.57 Safari/536.11";
    
    private final static Logger LOG = Logger.getLogger(TaskUtil.class.getName());
    
    public static long getEtaInMill(long minDelayInMS) {
        if(minDelayInMS < 0L) {
            minDelayInMS = 0;
        }
        
        Calendar cal = Calendar.getInstance(TIMEZONE);
        
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        if(hour < 6) {
            cal.set(Calendar.HOUR_OF_DAY, 6);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.SECOND,0);
            cal.set(Calendar.MILLISECOND,0);
            return cal.getTimeInMillis();
        }
        else {
            return cal.getTimeInMillis() + minDelayInMS;
        }
        
    }
    
    public static long getTomorrowEtaInMill() {
        Calendar cal = Calendar.getInstance(TIMEZONE);
        
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        
        if(hour >= 6) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return cal.getTimeInMillis();
    }
    
    public static String resolveRandom(String host,Map<String,String> cookies,String referer) throws Exception {
        String jsessionid = NetworkUtil.getJSESSIONID(cookies);
        String url = host + "/ImageOut.jsp;jsessionid=" + jsessionid;
        LOG.info("resolveRandom url=" + url);
        
        final URLConnection conn = new URL(url).openConnection();
        
        final URL hostCaptchaURL = new URL("http://ec2-46-137-229-229.ap-southeast-1.compute.amazonaws.com:8016/servlets/handle");
        LOG.info("resolveRandom hostCaptchaURL=" + hostCaptchaURL);
        
        final URLConnection hostCaptchaConn = hostCaptchaURL.openConnection();
        
        try {
            NetworkUtil.putCookies(conn,cookies);
            conn.addRequestProperty("Referer", referer);
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
            hostCaptchaConn.getOutputStream().close();
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
                //hostCaptchaConn.getOutputStream().close();
            }
            
        }
      
    }
    
}
