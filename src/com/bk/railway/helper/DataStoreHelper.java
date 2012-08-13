package com.bk.railway.helper;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import com.bk.railway.servlet.Constants;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class DataStoreHelper {
    private final static Logger LOG = Logger.getLogger(DataStoreHelper.class.getName());
    
    public static Key storeWithRetry(int maxRetry,Key recordKey,Map<String,String> properties) throws IOException {
        
        maxRetry = Math.min(100, Math.max(10, maxRetry)); //Make sure 10 < maxRetry < 100
        
        Transaction tx = null;
        
        int retry = 0;
        while(retry++ < maxRetry) {
            try {
                final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
                
                Entity record = null;
                
                tx = datastore.beginTransaction();
                
                if(null == recordKey) {
                    record = new Entity(Constants.RECORD_DATABASE);
                }
                else {
                try {
                        record = datastore.get(tx,recordKey);
                        LOG.info("Old record found with key " + KeyFactory.keyToString(recordKey));
                    } catch (EntityNotFoundException e) {
                        record = new Entity(Constants.RECORD_DATABASE);
                        LOG.info("New record created with key " + KeyFactory.keyToString(recordKey));
                    }
                }
                
                record = putRecord(record,properties);
                final Key retKey = datastore.put(tx, record);
                
                tx.commit();
                
                return retKey;
            }
            finally {
                if(tx != null && tx.isActive()) {
                    tx.rollback();
                }
            }
        }
        
        throw new IOException("Failure to put " + properties);
    }
    
    private static Entity putRecord(Entity record,Map<String,String> properties) {
        
        for(Map.Entry<String, String> entry : properties.entrySet()) {
            if(record.hasProperty(entry.getKey()) && record.getProperty(entry.getKey()).equals(entry.getValue())) {
                LOG.info("Skip dup " + entry.getKey() + "=" + entry.getValue());
            }
            else if(Constants.RECORD_ID.equals(entry.getKey())) {
                LOG.info("Skip " + Constants.RECORD_ID);
            }
            else {
                
                if(record.hasProperty(entry.getKey())) {
                    LOG.info("Update " + entry.getKey() + " from " + record.getProperty(entry.getKey()) + " to " + entry.getValue());
                }
                
                record.setProperty(entry.getKey(), entry.getValue());

            }
        }
        
        return record;
        
    }

}
