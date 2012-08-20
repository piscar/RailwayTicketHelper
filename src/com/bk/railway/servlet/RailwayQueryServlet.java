
package com.bk.railway.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.helper.GetInDateProxy;
import com.bk.railway.helper.QueryHelper;
import com.bk.railway.helper.TaskUtil;
import com.bk.railway.util.DebugMessage;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RailwayQueryServlet extends HttpServlet {
    public final static int DEFAULT_ORDER_RETRY_LIMIT = 10;
    public final static String NUM_STOP = "numstop";
    public final static String BORDING_TIME = "bording_time";

    private final static Logger LOG = Logger.getLogger(RailwayQueryServlet.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final int numstop = Integer.parseInt(request.getParameter(NUM_STOP));
        if (numstop < 0) {
            throw new IllegalArgumentException("numstop is " + numstop);
        }

        final String queryKeyString = request.getParameter(Constants.QUERY_ID);

        RecordStatus queryStatus = RecordStatus.UNKNOWN;
        QueryHelper.QueryResponse[] queryResponses = null;

        if (null != queryKeyString && !"".equals(queryKeyString)) {
            final Key queryKey = KeyFactory.stringToKey(queryKeyString);

            final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            try {
                final Entity queryEntity = datastore.get(queryKey);
                queryStatus = RecordStatus.valueOf((String) queryEntity
                        .getProperty(Constants.QUERY_STATUS));

            } catch (EntityNotFoundException e1) {
                throw new IllegalArgumentException("No entity found by queryKeyString="
                        + queryKeyString);
            }
        }

        LOG.info("numstop=" + numstop + "queryStatus=" + queryStatus + " queryKeyString="
                + queryKeyString);

        if (RecordStatus.CANCELED != queryStatus) {
            try {

                final String bookableDateString = GetInDateProxy.newInstance().getBookableString(
                        request.getParameter(Constants.GETIN_DATE));
                final boolean isBeforeToday = GetInDateProxy.isBeforeToday(request
                        .getParameter(Constants.GETIN_DATE));

                final QueryHelper.QueryRequest queryRequest = new QueryHelper.QueryRequest(
                        request.getParameter(Constants.PERSON_ID),
                        request.getParameter(Constants.FROM_STATATION),
                        request.getParameter(Constants.TO_STATATION),
                        null == bookableDateString ? GetInDateProxy
                                .bookableStringToDate(request
                                        .getParameter(Constants.GETIN_DATE))
                                : bookableDateString,
                        Integer.parseInt(request.getParameter(Constants.ORDER_QTY)));

                if (null == bookableDateString && isBeforeToday) {
                    LOG.info("Cancel the query");
                }
                else if (null == bookableDateString) {
                    // Postpone the task
                    try {
                        final TaskOptions queryTask = createTask(
                                queryRequest.person_id,
                                queryRequest.from_station,
                                queryRequest.to_station,
                                GetInDateProxy.bookableStringToDate(request
                                        .getParameter(Constants.GETIN_DATE)),
                                queryRequest.order_qty,
                                numstop,
                                queryKeyString);

                        queryTask.etaMillis(TaskUtil.getTomorrowEtaInMill());
                        queryTask
                                .taskName(getTaskName(queryRequest.from_station,
                                        queryRequest.to_station,
                                        request.getParameter(Constants.GETIN_DATE)));
                        QueueFactory.getDefaultQueue().add(queryTask);

                    } catch (TaskAlreadyExistsException e) {
                        LOG.info("duplictaed task name " + DebugMessage.toString(e));
                    }
                }
                else if (0 == numstop) {
                    queryResponses = new QueryHelper(queryRequest).doQuery();
                }
                else {

                    final int form_station_index = Constants.MAJOR_STOP_STATION_TO_ARRAY_INDEX
                            .get(request.getParameter(Constants.FROM_STATATION));
                    final int to_station_index = Constants.MAJOR_STOP_STATION_TO_ARRAY_INDEX
                            .get(request.getParameter(Constants.TO_STATATION));
                    final int direction = to_station_index > to_station_index ? 1 : -1;

                    int station_index = form_station_index + direction;
                    while (station_index != to_station_index
                            && station_index < Constants.MAJOR_STOP_STATION_ARRAY.length) {

                        final TaskOptions seq1queryTask = createTask(
                                queryRequest.person_id,
                                queryRequest.from_station,
                                Constants.MAJOR_STOP_STATION_ARRAY[station_index],
                                GetInDateProxy.bookableStringToDate(request
                                        .getParameter(Constants.GETIN_DATE)),
                                queryRequest.order_qty,
                                numstop - 1,
                                queryKeyString);

                        final TaskOptions seq2queryTask = createTask(
                                queryRequest.person_id,
                                Constants.MAJOR_STOP_STATION_ARRAY[station_index],
                                queryRequest.to_station,
                                GetInDateProxy.bookableStringToDate(request
                                        .getParameter(Constants.GETIN_DATE)),
                                queryRequest.order_qty,
                                numstop - 1,
                                queryKeyString);

                        LOG.info("seq1queryTask=" + seq1queryTask);
                        LOG.info("seq2queryTask=" + seq2queryTask);

                        try {
                            QueueFactory.getDefaultQueue().add(seq1queryTask);
                        } catch (TaskAlreadyExistsException e) {
                            LOG.info("duplictaed task name " + DebugMessage.toString(e));
                        }

                        try {
                            QueueFactory.getDefaultQueue().add(seq2queryTask);
                        } catch (TaskAlreadyExistsException e) {
                            LOG.info("duplictaed task name " + DebugMessage.toString(e));
                        }

                        station_index += direction;
                    }

                }

            } catch (Exception e) {
                LOG.severe(DebugMessage.toString(e));
            }
        }

        if (queryResponses != null) {
            final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            final List<Entity> listEntities = new ArrayList<Entity>(queryResponses.length);
            final long updateTime = System.currentTimeMillis();

            for (QueryHelper.QueryResponse r : queryResponses) {

                final List<Query.Filter> filterList = new ArrayList<Query.Filter>();
                filterList.add(new Query.FilterPredicate(Constants.FROM_STATATION,
                        Query.FilterOperator.EQUAL, r.orderRequest.from_station));
                filterList.add(new Query.FilterPredicate(Constants.TO_STATATION,
                        Query.FilterOperator.EQUAL, r.orderRequest.to_station));
                filterList.add(new Query.FilterPredicate(Constants.GETIN_DATE,
                        Query.FilterOperator.EQUAL, GetInDateProxy
                                .bookableStringToDate(r.orderRequest.getin_date)));
                filterList.add(new Query.FilterPredicate(Constants.ORDER_QTY,
                        Query.FilterOperator.EQUAL, String.valueOf(r.orderRequest.order_qty)));
                filterList.add(new Query.FilterPredicate(Constants.TRAIN_NO,
                        Query.FilterOperator.EQUAL, String.valueOf(r.orderRequest.train_no)));

                final Query query = new Query(Constants.RECORD_DATABASE);
                query.setFilter(Query.CompositeFilterOperator.and(filterList));

                final PreparedQuery pq = datastore.prepare(query);
                final QueryResultIterator<Entity> itEntity = pq.asQueryResultIterable(
                        FetchOptions.Builder.withLimit(1)).iterator();
                final boolean hasNext = itEntity.hasNext();
                final Entity entity = new Entity(Constants.QUERY_DATABASE);

                if (hasNext) {
                    entity.setPropertiesFrom(itEntity.next());
                }
                entity.setProperty(Constants.QUERY_UPDATETIME, updateTime);
                entity.setProperty(BORDING_TIME, r.bordingTime);

                if (!hasNext) {
                    entity.setProperty(Constants.FROM_STATATION, r.orderRequest.from_station);
                    entity.setProperty(Constants.TO_STATATION, r.orderRequest.to_station);
                    entity.setProperty(Constants.GETIN_DATE,
                            GetInDateProxy.bookableStringToDate(r.orderRequest.getin_date));
                    entity.setProperty(Constants.ORDER_QTY,
                            String.valueOf(r.orderRequest.order_qty));
                    entity.setProperty(Constants.TRAIN_NO, r.orderRequest.train_no);
                }
                listEntities.add(entity);
            }
            datastore.put(listEntities);
            //final Transaction tx = datastore.beginTransaction();
            /*try {
                
                tx.commit();
            } finally {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            }*/
        }
        response.setStatus(HttpServletResponse.SC_OK);
        final JsonArray jsonArray = new JsonArray();
        if (queryResponses != null) {
            for (QueryHelper.QueryResponse r : queryResponses) {
                JsonObject o = new JsonObject();

                o.addProperty(BORDING_TIME, r.bordingTime);
                o.addProperty(Constants.PERSON_ID, r.orderRequest.person_id);
                o.addProperty(Constants.FROM_STATATION, r.orderRequest.from_station);
                o.addProperty(Constants.TO_STATATION, r.orderRequest.to_station);
                o.addProperty(Constants.GETIN_DATE,
                        GetInDateProxy.bookableStringToDate(r.orderRequest.getin_date));
                o.addProperty(Constants.ORDER_QTY, r.orderRequest.order_qty);
                o.addProperty(Constants.TRAIN_NO, r.orderRequest.train_no);

                jsonArray.add(o);
            }

        }
        response.getWriter().print(jsonArray.toString());
        response.getWriter().flush();
    }

    private TaskOptions createTask(String person_id, String from_station, String to_station,
            String getindate, int quantity, int numStop, String queryKeyString) {
        final TaskOptions queryTask = TaskOptions.Builder
                .withUrl("/query")
                .method(Method.POST)
                .param(Constants.PERSON_ID, person_id)
                .param(Constants.FROM_STATATION, from_station)
                .param(Constants.TO_STATATION, to_station)
                .param(Constants.GETIN_DATE, getindate)
                .param(Constants.ORDER_QTY, String.valueOf(quantity))
                .param(NUM_STOP, String.valueOf(numStop))
                .taskName(getTaskName(from_station, to_station, getindate));

        if (queryKeyString != null && !"".equals(queryKeyString)) {
            queryTask.param(Constants.QUERY_ID, queryKeyString);
        }

        return queryTask;
    }

    private String getTaskName(String from_station, String to_station, String getindate) {
        return RailwayQueryServlet.class.getSimpleName() + "-" + from_station + "-" + to_station
                + "-" + getindate.replaceAll("/", "-");
    }

}
