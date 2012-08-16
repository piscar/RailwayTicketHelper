package com.bk.railway.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Logger;

import com.bk.railway.servlet.Constants;

public class CancelHelper {
    
    private final static Logger LOG = Logger.getLogger(CancelHelper.class.getName());
    //private final static String CANCEL_KEYWORD = new String("取消成功".getBytes(),Constants.WEB_ENCODING);
    public static boolean cancelTicket(String person_id,String ticketno) throws Exception {
        final String CANCEL_KEYWORD = new String("取消成功");
        
        String url = "http://railway.hinet.net/ccancel_rt.jsp?personId=" + URLEncoder.encode(person_id, "utf-8") + "&orderCode=" + URLEncoder.encode(ticketno, "utf-8");
        System.out.println("cancelTicket url=" + url);
        
        URLConnection conn = new URL(url).openConnection();
        conn.addRequestProperty("Referer", "http://railway.hinet.net/ccancel.jsp?personId=" + person_id + "&orderCode=" + ticketno);
        conn.setDoInput(true);
        conn.connect();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),Constants.WEB_ENCODING));
        String line = null;
        try{
            do {
                
                line = br.readLine();
                if(line != null) {
                    LOG.info(">>>" + line);
                    int startpos = line.indexOf(CANCEL_KEYWORD);
                    if(startpos > 0) {
                        return true;
                    }
                }
            }while(line != null);
        }
        finally {
            conn.getInputStream().close();
        }
        
        return false;
    }
}
