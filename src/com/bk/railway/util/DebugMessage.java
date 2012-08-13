package com.bk.railway.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DebugMessage {
    
    public static String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter ps = new PrintWriter(sw);
        
        e.printStackTrace(ps);
        return sw.toString();
    }
    
}
