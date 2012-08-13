package com.bk.railway.helper;

import java.util.Calendar;
import java.util.Locale;

public class TaskUtil {
    
    public static long getEtaInMill(long minDelayInMS) {
        if(minDelayInMS < 0L) {
            minDelayInMS = 0;
        }
        
        Calendar cal = Calendar.getInstance(Locale.TAIWAN);
        
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        if(hour < 6) {
            cal.set(Calendar.HOUR, 6);
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
        Calendar cal = Calendar.getInstance(Locale.TAIWAN);
        
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        cal.set(Calendar.HOUR, 6);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        
        if(hour >= 6) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return cal.getTimeInMillis();
    }
}
