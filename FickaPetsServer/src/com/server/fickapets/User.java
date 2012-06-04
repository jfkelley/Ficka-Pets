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
	private static final String PET_IMG_PROPERTY = "pet";
	
	public static void create(String id, String pet) {
		Entity entity = new Entity(ENTITY_KIND);
		entity.setProperty(USER_ID_PROPERTY, id);
		entity.setProperty(PET_IMG_PROPERTY, pet);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(entity);
	}
	
	public static boolean exists(String id) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(ENTITY_KIND).addFilter(USER_ID_PROPERTY, FilterOperator.EQUAL, id);
		return datastore.prepare(query).asIterator().hasNext();
	}
	
	public static List<UserAtts> filterNonexisting(List<String> ids) {
		if (ids.isEmpty()) { return new ArrayList<UserAtts>(); }
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(ENTITY_KIND).addFilter(USER_ID_PROPERTY, FilterOperator.IN, ids);
		List<UserAtts> found = new ArrayList<UserAtts>();
		for (Entity entity : datastore.prepare(query).asIterable()) {
			UserAtts atts = new UserAtts();
			atts.uid = (String)entity.getProperty(USER_ID_PROPERTY);
			atts.pet = (String)entity.getProperty(PET_IMG_PROPERTY);
			found.add(atts);
		}
		return found;
	}
}
