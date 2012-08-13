package com.bk.railway.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class NetworkUtil {
    
    public static String toPostData(Map<String,String> formData) throws UnsupportedEncodingException {
        final StringBuffer dataSB = new StringBuffer();
        int c = 0;
        for(Map.Entry<String, String> entry : formData.entrySet()) {
            dataSB.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "utf8"));
            if(c++ < formData.size() - 1) {
                dataSB.append("&");
            }
        }
        
        
        return dataSB.toString();
    }
    
    public static void putCookies(URLConnection conn,Map<String,String> cookies) {
        
        if(cookies.size() > 0) {
            StringBuffer cookieSB = new StringBuffer();
            int count = 0;
            for(Map.Entry<String,String> entry : cookies.entrySet()) {
                cookieSB.append(entry.getKey()).append("=").append(entry.getValue());
                if(count++ < cookies.size() - 1) {
                    cookieSB.append(";");
                }
            }
        }
        
    }
    
    public static String getJSESSIONID(Map<String,String> cookies) {
        for(Map.Entry<String,String> entry : cookies.entrySet()) {
            if("jsessionid".equals(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
}
