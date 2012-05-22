package com.server.fickapets;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
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
	  //     new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() throws Exception {
	//	helper.setUp();
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
	public void testUsersAndFriends() throws Exception {
		String userId1 = "a";
		String userId2 = "b";
		String userId3 = "c";
		String userName1 = "alice";
		String userName2 = "bob";
		String userName3 = "carol";
		testGetResponse(200, null, "/registeruser?id=%s", userId1);
		testGetResponse(200, null, "/registeruser?id=%s", userId2);
		String friends1 =
				"[\n" +
				"\t{\n" +
				"\t\t\"name\": \"" + userName2 + "\",\n" +
				"\t\t\"id\": \"" + userId2 + "\"\n" +
				"\t},\n" +
				"\t{\n" +
				"\t\t\"name\": \"" + userName3 + "\",\n" +
				"\t\t\"id\": \"" + userId3 + "\"\n" +
				"\t}\n" +
				"]";
		Response r = testPostResponse(200, null, "/findfriends", friends1, map(new String[0], new String[0]));
		System.out.println(r.text);
	}
	
	@Test
	public void testBattle() throws Exception {
		String user1 = "a";
		String user2 = "b";
		String move1 = "123";
		String move2 = "321";
		double strength1 = 40.0;
		double strength2 = 30.0;
		Response r1 = testGetResponse(200, null, "/create?uid1=%s&uid2=%s", user1, user2);
		
		String battleId = r1.text;
		
		testGetResponse(200, "", "/sendmove?uid=%s&bid=%s&strength=%s&move=%s", 
				user1, battleId, strength1, move1);
		
		testGetResponse(200, battleId, "/outgoingchallenges?uid=%s", user1);
		testGetResponse(200, battleId, "/incomingchallenges?uid=%s", user2);

		testGetResponse(200, "", "/sendmove?uid=%s&bid=%s&strength=%s&move=%s", 
				user2, battleId, strength2, move2);
		
		testGetResponse(200, battleId, "/openbattles?uid=%s", user1);
		testGetResponse(200, battleId, "/openbattles?uid=%s", user2);
	}
	
	private Response testGetResponse(Integer expectedCode, String expectedResponse, String url, Object... params) throws IOException{
		Response r = getUrl(urlFormat(BASE + url, params));
		if (expectedCode != null) {
			assertEquals(expectedCode.intValue(), r.code);
		}
		if (expectedResponse != null) {
			assertEquals(expectedResponse, r.text);
		}
		return r;
	}
	
	private static <K, V> Map<K, V> map(K[] keys, V[] vals) {
		if (keys.length != vals.length) return null;
		Map<K, V> map = new HashMap<K, V>();
		for (int i = 0; i < keys.length; i++) {
			map.put(keys[i], vals[i]);
		}
		return map;
	}
	
	private Response testPostResponse(Integer expectedCode, String expectedResponse, String url, String data, Map<String, String> params) throws IOException {
		HttpPost postRequest = new HttpPost(BASE + url);
		postRequest.setEntity(new StringEntity(data));
		for (String key : params.keySet()) {
			postRequest.getParams().setParameter(key, params.get(key));
		}
		HttpClient httpclient = new DefaultHttpClient();
	    HttpResponse response = httpclient.execute(postRequest);
	    String text = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
	    Integer code = response.getStatusLine().getStatusCode();
	    if (expectedCode != null) assertEquals(expectedCode, code);
	    if (expectedResponse != null) assertEquals(expectedResponse, text);
	    return new Response(code, text);
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
