package com.bk.railway.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.helper.DataStoreHelper;
import com.bk.railway.helper.GetInDateProxy;
import com.bk.railway.helper.LoginHelper;
import com.bk.railway.helper.OrderHelper;
import com.bk.railway.helper.TaskUtil;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RailwaySendOrderServlet extends HttpServlet {

    private final static Logger LOG = Logger.getLogger(RailwayOrderServlet.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        final String username = LoginHelper.getUsername(request,response);
        
        try {
            
            final boolean isBeforeToday = GetInDateProxy.isBeforeToday(request.getParameter(Constants.GETIN_DATE));
            
            if(isBeforeToday) {
                throw new IllegalArgumentException("Unable to find date " + request.getParameter(Constants.GETIN_DATE));
            }
            else {
                final OrderHelper.OrderRequest orderRequest = new OrderHelper.OrderRequest(
                        request.getParameter(Constants.PERSON_ID),
                        request.getParameter(Constants.FROM_STATATION),
                        request.getParameter(Constants.TO_STATATION),
                        request.getParameter(Constants.GETIN_DATE),
                        request.getParameter(Constants.TRAIN_NO),
                        Integer.parseInt(request.getParameter(Constants.ORDER_QTY)));
                
                final Map<String,String> databaseProperties = new HashMap<String,String>();
                databaseProperties.put(Constants.PERSON_ID, orderRequest.person_id);
                databaseProperties.put(Constants.FROM_STATATION, orderRequest.from_station);
                databaseProperties.put(Constants.TO_STATATION, orderRequest.to_station);
                databaseProperties.put(Constants.GETIN_DATE,GetInDateProxy.bookableStringToDate(orderRequest.getin_date));
                databaseProperties.put(Constants.TRAIN_NO, orderRequest.train_no);
                databaseProperties.put(Constants.ORDER_QTY, String.valueOf(orderRequest.order_qty));
                databaseProperties.put(Constants.RECORD_USERNAME, username);
                
                final Key recordkey = DataStoreHelper.storeWithRetry(100, null, databaseProperties);
                final String record_id = KeyFactory.keyToString(recordkey);
                
                LOG.info("record_id=" + record_id);
    
                final TaskOptions orderTask = TaskOptions.Builder.withUrl("/order")
                        .method(Method.POST)
                        .param(Constants.RECORD_USERNAME,username)
                        .param(Constants.RECORD_ID, record_id)
                        .param(Constants.PERSON_ID, orderRequest.person_id)
                        .param(Constants.FROM_STATATION, orderRequest.from_station)
                        .param(Constants.TO_STATATION, orderRequest.to_station)
                        .param(Constants.GETIN_DATE, GetInDateProxy.bookableStringToDate(orderRequest.getin_date))
                        .param(Constants.TRAIN_NO, orderRequest.train_no)
                        .param(Constants.ORDER_QTY, String.valueOf(orderRequest.order_qty))
                        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(RailwayOrderServlet.DEFAULT_ORDER_RETRY_LIMIT)); 

                QueueFactory.getDefaultQueue().add(orderTask);
                
                final JsonObject json = new JsonObject();
                json.add(Constants.RECORD_ID, new JsonPrimitive(record_id));
    
                response.getWriter().print(json.toString());
                response.getWriter().flush();
                
                response.setStatus(HttpServletResponse.SC_OK);
            }
            
        } catch (Exception e) {
           throw new IOException(e);
        } 

        
        
    }
}
