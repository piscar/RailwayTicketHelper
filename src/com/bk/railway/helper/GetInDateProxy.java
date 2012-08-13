package com.bk.railway.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetInDateProxy {
    private final static Logger LOG = Logger.getLogger(GetInDateProxy.class.getName());
    private final static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static GetInDateProxy m_self;
    
    public synchronized static GetInDateProxy newInstance() {
        if(null == m_self) {
            m_self = new GetInDateProxy();
        }
        
        return m_self;
    }
    
    private Map<String,String> m_bookableList = new HashMap<String,String>();
    private int m_updateYear;
    private int m_updateMonth;
    private int m_updateDay;
    
    
    public static String bookableStringToDate(String bookableString) {
        return bookableString.substring(0,10);
    }
    
    public static boolean isBeforeToday(String bookableString) throws ParseException {
        final Date bookDate = DATEFORMAT.parse(bookableStringToDate(bookableString));
        final Calendar tomorrow = Calendar.getInstance(Locale.TAIWAN);
        final Calendar bookDay = Calendar.getInstance(Locale.TAIWAN);
        
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        
        bookDay.setTimeInMillis(bookDate.getTime());
        
        return bookDay.before(tomorrow);
    }
    
    public synchronized String getBookableString(Date bookDate) throws Exception {

        getAllBookableDate(); //update bookable date
        
        return m_bookableList.get(DATEFORMAT.format(bookDate));
    }
    
    public synchronized String getBookableString(String bookDateString) throws Exception {
        
        DATEFORMAT.parse(bookDateString); //Make sure it is parseable

        getAllBookableDate(); //update bookable date
        
        return m_bookableList.get(bookableStringToDate(bookDateString));
    }

    public synchronized String[] getAllBookableDate() throws Exception {
        
        final Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(Calendar.ZONE_OFFSET, 8);
        final int year = currentCalendar.get(Calendar.YEAR);
        final int month = currentCalendar.get(Calendar.MONTH);
        final int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        
        if(m_updateDay != day || m_updateMonth != month || m_updateYear != year) {
            m_updateDay = day;
            m_updateMonth = month;
            m_updateYear = year;
            
            for(String dateString : findAllowBookedDate()) {
                m_bookableList.put(dateString.substring(0,10), dateString);
            }
            LOG.info("New bookable date m_bookableList=" + m_bookableList + " on " + m_updateYear + "/" + (m_updateMonth + 1) + "/" + m_updateDay);
        }

        return m_bookableList.keySet().toArray(new String[0]);
        
    }
    
    protected GetInDateProxy() {
        
    }
    
    
    
    
    private String[] findAllowBookedDate() throws Exception{
        final Pattern pattern = Pattern.compile("(\\d{4}/\\d{2}/\\d{2}-\\d{2})>\\d{4}/\\d{2}/\\d{2}");
        List<String> retStrings = new ArrayList<String>(14);
        
        
        URLConnection conn = new URL("http://railway.hinet.net/ctno1.htm").openConnection();
        conn.setDoOutput(true);
        conn.connect();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = null;
        
        do {
            
            line = br.readLine();
            if(line != null) {
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    retStrings.add(matcher.group(1));
                }
            }
        }while(line != null);
        
        conn.getInputStream().close();
        
        return retStrings.toArray(new String[0]);
    }
}
