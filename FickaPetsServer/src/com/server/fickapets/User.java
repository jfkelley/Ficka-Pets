package com.server.fickapets;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

public class User {
	public static final String ENTITY_KIND = "user";
	private static final String USER_ID_PROPERTY = "uid";
	
	public static void create(String id) {
		Entity entity = new Entity(ENTITY_KIND);
		entity.setProperty(USER_ID_PROPERTY, id);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(entity);
	}
	
	public static boolean exists(String id) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(ENTITY_KIND).addFilter(USER_ID_PROPERTY, FilterOperator.EQUAL, id);
		return datastore.prepare(query).asIterator().hasNext();
	}
	
	public static List<String> filterNonexisting(List<String> ids) {
		if (ids.isEmpty()) { return new ArrayList<String>(); }
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(ENTITY_KIND).addFilter(USER_ID_PROPERTY, FilterOperator.IN, ids);
		List<String> found = new ArrayList<String>();
		for (Entity entity : datastore.prepare(query).asIterable()) {
			found.add((String) entity.getProperty(USER_ID_PROPERTY));
		}
		return found;
	}
}
