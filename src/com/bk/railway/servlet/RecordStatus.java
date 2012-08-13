package com.bk.railway.servlet;

public enum RecordStatus {
    UNKNOWN("unkonwn"),
    POSTPONED("postponed"),
    QUEUE("queue"),
    DONE("done"),
    CANCELED("cancel");
    
    private String s;
    RecordStatus(String s) {
        this.s = s;
    }
    @Override
    public String toString() {
        return s;
    }
    
    public static RecordStatus fromObject(Object o) {
        for(RecordStatus r : RecordStatus.values()) {
            if(r.s.equals(o)) {
                return r;
            }
        }
        return UNKNOWN;
    }
}
