
package com.bk.railway.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.helper.GetInDateProxy;
import com.bk.railway.helper.QueryHelper;
import com.bk.railway.util.DebugMessage;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RailwayPreQueryServlet extends HttpServlet {
    private final static Logger LOG = Logger.getLogger(RailwayPreQueryServlet.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            // final String bookableDateString =
            // GetInDateProxy.newInstance().getBookableString(
            // request.getParameter(Constants.GETIN_DATE));
            // final boolean isBeforeToday =
            // GetInDateProxy.isBeforeToday(request
            // .getParameter(Constants.GETIN_DATE));

            final QueryHelper.QueryRequest queryRequest = new QueryHelper.QueryRequest(
                    request.getParameter(Constants.PERSON_ID),
                    request.getParameter(Constants.FROM_STATATION),
                    request.getParameter(Constants.TO_STATATION),
                    GetInDateProxy.bookableStringToDate(request.getParameter(Constants.GETIN_DATE)),
                    Integer.parseInt(request.getParameter(Constants.ORDER_QTY)));

            // Try to search database
            final Query.Filter stationFilter = createStationFilter(queryRequest.from_station,queryRequest.to_station);//Query.CompositeFilterOperator.or(stationFilterList);
            final Query.Filter dateFilter = new Query.FilterPredicate(Constants.GETIN_DATE,
                    Query.FilterOperator.EQUAL, GetInDateProxy.bookableStringToDate(request
                            .getParameter(Constants.GETIN_DATE)));
            final Query.Filter quantityFilter = new Query.FilterPredicate(Constants.ORDER_QTY,
                    Query.FilterOperator.GREATER_THAN_OR_EQUAL,
                    String.valueOf(queryRequest.order_qty));

            final Query query = new Query(Constants.QUERY_DATABASE);
            query.setFilter(Query.CompositeFilterOperator.and(stationFilter, dateFilter,quantityFilter));
            final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            final PreparedQuery pq = datastore.prepare(query);
            final QueryResultIterator<Entity> itEntity = pq.asQueryResultIterable().iterator();
            final Map<String, Map<String, List<Entity>>> routeMap = new HashMap<String, Map<String, List<Entity>>>();

            while (itEntity.hasNext()) {
                final Entity entity = itEntity.next();
                final String from_station = (String) entity.getProperty(Constants.FROM_STATATION);
                final String to_station = (String) entity.getProperty(Constants.TO_STATATION);
                final Map<String, List<Entity>> stationMap = routeMap.containsKey(from_station) ? routeMap
                        .get(from_station)
                        : new HashMap<String, List<Entity>>();
                final List<Entity> listEntities = stationMap.containsKey(to_station) ? stationMap
                        .get(to_station) : new ArrayList<Entity>();

                listEntities.add(entity);

                stationMap.remove(to_station);
                stationMap.put(to_station, listEntities);

                routeMap.remove(from_station);
                routeMap.put(from_station, stationMap);

                // LOG.info("Found " + entity);
            }

            LOG.info("routeMap=" + routeMap);

            final Map<String, List<Entity>> stationMap = routeMap.get(queryRequest.from_station);
            final JsonArray routeArrays = new JsonArray();

            if (stationMap != null) {
                
                final Set<String> trainNoSet = new HashSet<String>();
                
                for (Map.Entry<String, List<Entity>> entry : stationMap.entrySet()) {
                    
                    for (Entity entity : entry.getValue()) {
                        trainNoSet.add((String) entity.getProperty(Constants.TRAIN_NO));
                    }
                }
                
                LOG.info("trainNoSet=" + trainNoSet);
                
                for (String entity_train_no : trainNoSet) {
                    // final String entity_train_no = (String)
                    // entity.getProperty(
                    // Constants.TRAIN_NO);
                    final Stack<Entity> route = new Stack<Entity>();

                    dfs(queryRequest.from_station, queryRequest.to_station, entity_train_no,
                            routeMap, route);

                    final String last_to_station = route.size() > 0 ? (String) route.peek()
                            .getProperty(Constants.TO_STATATION) : null;
                    if (queryRequest.to_station.equals(last_to_station)) {
                        // Found route path
                        final JsonArray routeArray = new JsonArray();
                        for (int i = 0; i < route.size(); i++) {
                            final JsonObject o = new JsonObject();
                            final Entity oneroute = route.get(i);
                            o.addProperty(Constants.FROM_STATATION,
                                    (String) oneroute.getProperty(Constants.FROM_STATATION));
                            o.addProperty(Constants.TO_STATATION,
                                    (String) oneroute.getProperty(Constants.TO_STATATION));
                            o.addProperty(Constants.TRAIN_NO,
                                    (String) oneroute.getProperty(Constants.TRAIN_NO));
                            o.addProperty(Constants.ORDER_QTY,
                                    (String) oneroute.getProperty(Constants.ORDER_QTY));
                            o.addProperty(RailwayQueryServlet.BORDING_TIME, (String) oneroute
                                    .getProperty(RailwayQueryServlet.BORDING_TIME));
                            o.addProperty(Constants.QUERY_UPDATETIME,
                                    (Long) oneroute.getProperty(Constants.QUERY_UPDATETIME));
                            routeArray.add(o);
                        }
                        routeArrays.add(routeArray);
                    }
                }

            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(routeArrays.toString());
            response.getWriter().flush();

            // --
            /*
             * if (null == bookableDateString && isBeforeToday) {
             * LOG.info("Cancel the query"); } else if (null ==
             * bookableDateString) { // Postpone the task } else { }
             */

        } catch (Exception e) {
            LOG.severe(DebugMessage.toString(e));
        }
    }
    
    private Query.Filter createStationFilter(String from_station,String to_station) {
        /*
        final List<Query.Filter> stationFilterList = new ArrayList<Query.Filter>();
        
        final int form_station_index = Constants.MAJOR_STOP_STATION_TO_ARRAY_INDEX.get(from_station);
        final int to_station_index = Constants.MAJOR_STOP_STATION_TO_ARRAY_INDEX.get(to_station);
        final int direction = to_station_index > form_station_index ? 1 : -1;
        
        LOG.info("form_station_index=" + form_station_index + " to_station_index=" + to_station_index + " direction=" + direction);
        
        int station_index = form_station_index + direction;
        while (station_index != to_station_index
                && station_index < Constants.MAJOR_STOP_STATION_ARRAY.length) {
            
            stationFilterList.add(new Query.FilterPredicate(Constants.TO_STATATION,Query.FilterOperator.EQUAL, station_index));
            stationFilterList.add(new Query.FilterPredicate(Constants.FROM_STATATION,Query.FilterOperator.EQUAL, station_index));
        }
        
        stationFilterList.add(new Query.FilterPredicate(Constants.TO_STATATION,Query.FilterOperator.EQUAL, to_station));
        stationFilterList.add(new Query.FilterPredicate(Constants.FROM_STATATION,Query.FilterOperator.EQUAL, from_station));
        
        return Query.CompositeFilterOperator.or(stationFilterList);
        */
        
        return Query.CompositeFilterOperator.or(new Query.FilterPredicate(Constants.FROM_STATATION,Query.FilterOperator.EQUAL, from_station),new Query.FilterPredicate(Constants.TO_STATATION,Query.FilterOperator.EQUAL, to_station));
    }
    

    private void dfs(String from_station, String to_station, String train_no,
            Map<String, Map<String, List<Entity>>> routeMap, Stack<Entity> paths) {

        final Map<String, List<Entity>> stationMap = routeMap.get(from_station);

        if (stationMap != null) {

            for (Map.Entry<String, List<Entity>> entry : stationMap.entrySet()) {
                for (Entity entity : entry.getValue()) {
                    final String entity_train_no = (String) entity.getProperty(
                            Constants.TRAIN_NO);
                    if (entity_train_no.equals(train_no)) {
                        if (entry.getKey().equals(to_station)) {
                            paths.push(entity);
                            return; // Found!!
                        }
                        else {
                            paths.push(entity);
                            dfs(entry.getKey(), to_station, train_no, routeMap, paths);
                            final String last_to_station = paths.size() > 0 ? (String) paths.peek()
                                    .getProperty(Constants.TO_STATATION) : null;
                            if (!to_station.equals(last_to_station)) {
                                paths.pop();
                            }
                            else {
                                return; // Found!!
                            }
                        }
                    }
                }
            }

        }

    }

}
