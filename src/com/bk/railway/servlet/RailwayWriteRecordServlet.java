package com.bk.railway.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;

public class RailwayWriteRecordServlet extends HttpServlet{

    private final static Logger LOG = Logger.getLogger(RailwayWriteRecordServlet.class.getName());
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse resonse) throws IOException {
        
        
        final String recordid = request.getParameter(Constants.RECORD_ID);

        if(null == recordid || "".equals(recordid)) {
            throw new IllegalArgumentException("invaild "  + Constants.RECORD_ID + "=" + recordid);
        }

        final Key recordKey = KeyFactory.stringToKey(recordid);
        
        Transaction tx = null;
        
        try {
            final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            
            Entity record = null;
            tx = datastore.beginTransaction();
            
            try {
                record = datastore.get(tx,recordKey);
                LOG.info("Old record found");
            } catch (EntityNotFoundException e) {
                record = new Entity(Constants.RECORD_DATABASE,recordKey);
                LOG.info("New record created");
            }
            
            record = putRecord(record,request.getParameterMap());
            datastore.put(tx, record);
            
            tx.commit();
        }
        finally {
            if(tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
        
      
    }
    
    private Entity putRecord(Entity record,Map<String,String[]> parameterMap) {
        
        for(Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if(record.hasProperty(entry.getKey()) && record.getProperty(entry.getKey()).equals(entry.getValue()[0])) {
                LOG.info("Skip dup " + entry.getKey() + "=" + entry.getValue()[0]);
            }
            else if(Constants.RECORD_ID.equals(entry.getKey())) {
                LOG.info("Skip " + Constants.RECORD_ID);
            }
            else {
                
                if(record.hasProperty(entry.getKey())) {
                    LOG.info("Update " + entry.getKey() + " from " + record.getProperty(entry.getKey()) + " to " + entry.getValue()[0]);
                }
                
                record.setProperty(entry.getKey(), entry.getValue()[0]);
                if(entry.getValue().length > 1) {
                    for(int i = 1;i < entry.getValue().length;i++) {
                        LOG.info("Skip " + entry.getKey() + "=" + entry.getValue()[i]);
                    }
                }
            }
        }
        
        return record;
        
    }

}
