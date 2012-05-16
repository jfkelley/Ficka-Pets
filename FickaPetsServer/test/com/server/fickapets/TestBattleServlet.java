package com.server.fickapets;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
//import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
//import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class TestBattleServlet {
	
	static Collection<Entity> savedEntities;
	//private final LocalServiceTestHelper helper =
	 //       new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() throws Exception {
		//helper.setUp();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("battle");
		Iterable<Entity> entities = datastore.prepare(q).asIterable();
		ArrayList<Key> keys = new ArrayList<Key>();
		savedEntities = new ArrayList<Entity>();
		for (Entity entity : entities) {
			savedEntities.add(entity);
			keys.add(entity.getKey());
		}
		datastore.delete(keys);
	}
	
	private static final String BASE = "http://127.0.0.1:8888";

	@Test
	public void test() throws Exception {
		String user1 = "a";
		String user2 = "b";
		String move1 = "123";
		String move2 = "321";
		double strength1 = 40.0;
		double strength2 = 30.0;
		Response r1 = testResponse(200, null, "/create?uid1=%s&uid2=%s", user1, user2);
		
		String battleId = r1.text;
		
		testResponse(200, "", "/sendmove?uid=%s&bid=%s&strength=%s&move=%s", 
				user1, battleId, strength1, move1);
		
		testResponse(200, battleId, "/outgoingchallenges?uid=%s", user1);
		testResponse(200, battleId, "/incomingchallenges?uid=%s", user2);

		testResponse(200, "", "/sendmove?uid=%s&bid=%s&strength=%s&move=%s", 
				user2, battleId, strength2, move2);
		
		testResponse(200, battleId, "/openbattles?uid=%s", user1);
		testResponse(200, battleId, "/openbattles?uid=%s", user2);
	}
	
	private Response testResponse(Integer expectedCode, String expectedResponse, String url, Object... params) throws IOException{
		Response r = getUrl(urlFormat(BASE + url, params));
		if (expectedCode != null) {
			assertEquals(expectedCode.intValue(), r.code);
		}
		if (expectedResponse != null) {
			assertEquals(expectedResponse, r.text);
		}
		return r;
	}
	
	private String urlFormat(String format, Object... objects) throws UnsupportedEncodingException {
		String[] strings = new String[objects.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = URLEncoder.encode(objects[i].toString(), "UTF-8");
		}
		return String.format(format, (Object[])strings);
	}
	
	private static class Response {
		final int code;
		final String text;
		public Response(int n, String str) {
			code = n; text = str;
		}
		
		public String toString() {
			return code + ": " + text;
		}
	}
	
	private Response getUrl(String url) throws IOException {
		System.out.println(url);
		HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
		String text = IOUtils.toString(conn.getInputStream(), "UTF-8");
		return new Response(conn.getResponseCode(), text);
	}
	
	@After
	public void tearDown() throws Exception {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("battle");
		Iterable<Entity> entities = datastore.prepare(q).asIterable();
		ArrayList<Key> keys = new ArrayList<Key>();
		for (Entity entity : entities) {
			keys.add(entity.getKey());
		}
		datastore.delete(keys);
		datastore.put(savedEntities);
		//helper.tearDown();
	}

}
