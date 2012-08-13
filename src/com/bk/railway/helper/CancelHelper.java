package com.bk.railway.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class CancelHelper {
    
    public static boolean cancelTicket(String person_id,String ticketno) throws Exception {
        String url = "http://railway.hinet.net/ccancel_rt.jsp?personId=" + URLEncoder.encode(person_id, "utf8") + "&orderCode=" + URLEncoder.encode(ticketno, "utf8");
        System.out.println("cancelTicket url=" + url);
        
        URLConnection conn = new URL(url).openConnection();
        conn.addRequestProperty("Referer", "http://railway.hinet.net/ccancel.jsp?personId=" + person_id + "&orderCode=" + ticketno);
        conn.setDoInput(true);
        conn.connect();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = null;
        try{
            do {
                
                line = br.readLine();
                if(line != null) {
                    System.out.println(">>>" + line);
                    int startpos = line.indexOf("取消成功");
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
