
package com.bk.railway.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.helper.CancelHelper;
import com.bk.railway.helper.DataStoreHelper;
import com.bk.railway.helper.TaskUtil;
import com.bk.railway.util.DebugMessage;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RailwayCancelRecordServlet extends HttpServlet {

    private final static Logger LOG = Logger.getLogger(RailwayCancelRecordServlet.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String recordid = request.getParameter(Constants.RECORD_ID);

        if (null == recordid) {
            throw new IllegalArgumentException(Constants.RECORD_ID + "=" + recordid);
        }

        final Key recordKey = KeyFactory.stringToKey(recordid);

        Entity record = null;

        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        try {
            record = datastore.get(recordKey);
            LOG.info("Old record found");
        } catch (EntityNotFoundException e) {
            throw new IllegalArgumentException("No record found by key=" + recordid);
        }

        final String person_id = (String) record.getProperty(Constants.PERSON_ID);
        final String order_no = (String) record.getProperty(Constants.ORDER_NO);
        final RecordStatus record_status = RecordStatus.fromObject(record.getProperty(Constants.RECORD_STATUS));
        
        if(RecordStatus.QUEUE == record_status || RecordStatus.POSTPONED == record_status) {
            final boolean delTaskSucess = QueueFactory.getDefaultQueue().deleteTask(recordid);
            if(!delTaskSucess) {
                LOG.warning("delTaskSucess=" + delTaskSucess);
            }
        }

        boolean success = false;

        if (person_id != null && order_no != null) {
            try {
                if (CancelHelper.cancelTicket(person_id, order_no)) {
                    success = true;
                }
            } catch (Exception e) {
                LOG.severe(DebugMessage.toString(e));
            }
        }
        else {
            
            success = true;
        }
        
        if(success) {
            final Map<String,String> databaseProperties = new HashMap<String,String>();
            databaseProperties.put(Constants.RECORD_STATUS, RecordStatus.CANCELED.toString());
            DataStoreHelper.storeWithRetry(100, recordKey, databaseProperties);
        }

        final JsonObject json = new JsonObject();
        json.add(Constants.RECORD_ID, new JsonPrimitive(recordid));
        json.add(Constants.CANCEL_SUCCESS, new JsonPrimitive(success));

        response.getWriter().print(json.toString());
        response.getWriter().flush();

        response.setStatus(HttpServletResponse.SC_OK);

    }
}
