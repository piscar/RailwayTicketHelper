package com.bk.railway.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.helper.LoginHelper;
import com.google.appengine.api.datastore.Cursor;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RailwayOrderStatusServlet extends HttpServlet {
    public final static String ENTITY = "entity";
    public final static String CURSOR = "cursor";
    private final static int FETCH_LIMIT = 100;
    private final static Logger LOG = Logger.getLogger(RailwayOrderStatusServlet.class.getName());

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        final String username = LoginHelper.getUsername(request,response);
        
        final String record_id = request.getParameter(Constants.RECORD_ID);
        final String person_id = request.getParameter(Constants.PERSON_ID);
        final String record_status = request.getParameter(Constants.RECORD_STATUS);
        final Cursor startCursor = request.getParameter(CURSOR) != null ? Cursor.fromWebSafeString(request.getParameter(CURSOR)) : null;
        
        final List<Entity> listEntities = new ArrayList<Entity>(10);
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        
        Cursor lastCursor = null;
        
        if(record_id != null && !"".equals(record_id)) {
            final Key recordKey = KeyFactory.stringToKey(record_id);
            
            
            try {
                listEntities.add(datastore.get(recordKey));
            } catch (EntityNotFoundException e) {
                LOG.info("No record created");
            }
        }
        else {
            
            final List<Query.Filter> filterList = new ArrayList<Query.Filter>();
            
            if(!"elixirbb@gmail.com".equals(username)) {
                filterList.add(new Query.FilterPredicate(Constants.RECORD_USERNAME, Query.FilterOperator.EQUAL, username));
            }
            
            if(person_id != null) {
                filterList.add(new Query.FilterPredicate(Constants.PERSON_ID, Query.FilterOperator.EQUAL, person_id));
            }
            
            if(record_status != null) {
                filterList.add(new Query.FilterPredicate(Constants.RECORD_STATUS, Query.FilterOperator.EQUAL, record_status));
            }
            
            final Query query = new Query(Constants.RECORD_DATABASE);
            if(filterList.size() > 1) {
                query.setFilter(Query.CompositeFilterOperator.and(filterList));
            }
            else if(1 == filterList.size()){
                query.setFilter(filterList.get(0));
            }

            query.addSort(Constants.GETIN_DATE,  Query.SortDirection.DESCENDING);
            
            final PreparedQuery pq = datastore.prepare(query);
            final QueryResultIterator<Entity> itEntity = (null == startCursor) ? pq.asQueryResultIterable(FetchOptions.Builder.withLimit(FETCH_LIMIT)).iterator() : pq.asQueryResultIterable(FetchOptions.Builder.withStartCursor(startCursor)).iterator();
            
            while(itEntity.hasNext()) {
                lastCursor = itEntity.getCursor();
                listEntities.add(itEntity.next());
            }
            
        }
        
        final JsonObject json = new JsonObject();
        if(lastCursor != null) {
            json.add(CURSOR, new JsonPrimitive(lastCursor.toWebSafeString()));
        }
        final JsonArray entities = new JsonArray();
        for(Entity entity : listEntities) {
            final JsonObject jsonEntity = new JsonObject();
            for(Map.Entry<String, Object> elem : entity.getProperties().entrySet()) {
                jsonEntity.add(elem.getKey(), new JsonPrimitive(elem.getValue().toString()));
            }
            
            jsonEntity.add(Constants.RECORD_ID, new JsonPrimitive(KeyFactory.keyToString(entity.getKey())));
            
            entities.add(jsonEntity);
            
        }
        json.add(ENTITY, entities);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(json.toString());
        response.getWriter().flush();
    }


}
