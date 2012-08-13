
package com.bk.railway.servlet;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.helper.DataStoreHelper;
import com.bk.railway.helper.GetInDateProxy;
import com.bk.railway.helper.OrderHelper;
import com.bk.railway.helper.TaskUtil;
import com.bk.railway.util.DebugMessage;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RailwayOrderServlet extends HttpServlet {

    private final static Logger LOG = Logger.getLogger(RailwayOrderServlet.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String record_id = request.getParameter(Constants.RECORD_ID);

        if (null == record_id || "".equals(record_id)) {
            throw new IllegalArgumentException("invaild " + Constants.RECORD_ID + "=" + record_id);
        }

        try {
            final String bookableDateString = GetInDateProxy.newInstance().getBookableString(
                    request.getParameter(Constants.GETIN_DATE));
            final boolean isBeforeToday = GetInDateProxy.isBeforeToday(request.getParameter(Constants.GETIN_DATE));
            
            RecordStatus nextRecordStatus;
            
            if (null == bookableDateString && isBeforeToday) {
                nextRecordStatus = RecordStatus.CANCELED;
            }
            else if(null == bookableDateString){
                nextRecordStatus = RecordStatus.POSTPONED;
            }
            else {
                nextRecordStatus = RecordStatus.DONE;
            }
            
            final OrderHelper.OrderRequest orderRequest = new OrderHelper.OrderRequest(
                    request.getParameter(Constants.PERSON_ID),
                    request.getParameter(Constants.FROM_STATATION),
                    request.getParameter(Constants.TO_STATATION),
                    null == bookableDateString ? GetInDateProxy.bookableStringToDate(request.getParameter(Constants.GETIN_DATE))  : bookableDateString,
                    request.getParameter(Constants.TRAIN_NO),
                    Integer.parseInt(request.getParameter(Constants.ORDER_QTY)));

            final Map<String,String> databaseProperties = new HashMap<String,String>();
            databaseProperties.put(Constants.PERSON_ID, orderRequest.person_id);
            databaseProperties.put(Constants.FROM_STATATION, orderRequest.from_station);
            databaseProperties.put(Constants.TO_STATATION, orderRequest.to_station);
            databaseProperties.put(Constants.GETIN_DATE,GetInDateProxy.bookableStringToDate(orderRequest.getin_date));
            databaseProperties.put(Constants.TRAIN_NO, orderRequest.train_no);
            databaseProperties.put(Constants.ORDER_QTY, String.valueOf(orderRequest.order_qty));            
            
            OrderHelper.OrderResponse orderResponse = null;

            if(RecordStatus.DONE == nextRecordStatus) {
                try {
                    orderResponse = new OrderHelper(orderRequest).doOrder();
                } catch (Exception e) {
                    LOG.severe(DebugMessage.toString(e));
                }
            }

            if (null == orderResponse) {

                final TaskOptions orderTask = TaskOptions.Builder.withUrl("/order")
                        .method(Method.POST)
                        .param(Constants.RECORD_ID, record_id)
                        .param(Constants.PERSON_ID, orderRequest.person_id)
                        .param(Constants.FROM_STATATION, orderRequest.from_station)
                        .param(Constants.TO_STATATION, orderRequest.to_station)
                        .param(Constants.GETIN_DATE, orderRequest.getin_date)
                        .param(Constants.TRAIN_NO, orderRequest.train_no)
                        .param(Constants.ORDER_QTY, String.valueOf(orderRequest.order_qty))
                        .taskName(record_id)
                        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0)); // No
                                                                                   // retry
                if(RecordStatus.DONE == nextRecordStatus) {

                    databaseProperties.put(Constants.RECORD_STATUS, RecordStatus.QUEUE.toString());
                    
                    orderTask.etaMillis(TaskUtil.getEtaInMill(60L * 1000L)); //delay 1 min
    
                    LOG.info("add delayOrderTask=" + orderTask);
                    QueueFactory.getDefaultQueue().add(orderTask);
                }
                else if(RecordStatus.POSTPONED == nextRecordStatus) {
                    databaseProperties.put(Constants.RECORD_STATUS, RecordStatus.POSTPONED.toString());
                    
                    orderTask.etaMillis(TaskUtil.getTomorrowEtaInMill()); //delay 1 day
    
                    LOG.info("add postponedOrderTask=" + orderTask);
                    QueueFactory.getDefaultQueue().add(orderTask);
                }
                else {
                    databaseProperties.put(Constants.RECORD_STATUS, RecordStatus.CANCELED.toString());
                }
                
            }
            else {
                databaseProperties.put(Constants.ORDER_NO, orderResponse.orderno);
                databaseProperties.put(Constants.RECORD_STATUS, RecordStatus.DONE.toString());

                final JsonObject json = new JsonObject();
                json.add(Constants.ORDER_NO, new JsonPrimitive(orderResponse.orderno));
                json.add(Constants.PERSON_ID, new JsonPrimitive(orderResponse.person_id));
                json.add(Constants.RECORD_ID, new JsonPrimitive(record_id));

                response.getWriter().print(json.toString());
                response.getWriter().flush();
            }
            
            DataStoreHelper.storeWithRetry(100, KeyFactory.stringToKey(record_id), databaseProperties);

            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            LOG.severe(DebugMessage.toString(e));
        }

    }
}
