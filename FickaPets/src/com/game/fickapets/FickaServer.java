package com.game.fickapets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import android.content.Context;
import android.net.http.AndroidHttpClient;

public class FickaServer {
	public static final String OPP_MOVE_KEY = "opponentmove";
	public static final String OPP_STRENGTH_KEY = "opponentStrength";
	public static final String OPP_ID_KEY = "opponentIdentifier";
	
	private static final String BASE_URL = "http://10.31.112.120:8888/";
	private static final String GRAPH_BASE_URL = "https://graph.facebook.com/";
		
	private static final String CREATE = BASE_URL + "create?uid1=%s&uid2=%s";
	private static final String SEND_MOVE = BASE_URL + "sendmove?uid=%s&bid=%s&move=%s&strength=%s";
	private static final String BATTLE_DATA = BASE_URL + "battledata?uid=%s&bid=%s";
	private static final String CLOSE_BATTLE = BASE_URL + "closebattle?uid=%s&bid=%s";
	private static final String FIND_FRIENDS = BASE_URL + "findfriends";
	private static final String INCOMING_CHALLENGES = BASE_URL + "incomingchallenges?uid=%s";
	
	Context context;
	public FickaServer(Context context) {
		this.context = context;
	}
	
	
	
	private static String urlFormat(String format, Object... objects) throws UnsupportedEncodingException {
		String[] strings = new String[objects.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = URLEncoder.encode(objects[i].toString(), "UTF-8");
		}
		return String.format(format, (Object[])strings);
	}
	
	public String createGame(String myId, String theirId) throws IOException {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		String url = urlFormat(CREATE, myId, theirId);
		
		HttpGet get = new HttpGet(url);

		HttpResponse resp = client.execute(get);

		if (resp.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
			client.close();
			return null;
		}
		client.close();
		String battleId = responseToString(resp);
		return battleId;
	}

	private HttpRequestBase setCharEncoding(HttpRequestBase req) {
		req.setHeader("Content-type", "text/plain; charset=UTF-8");
		return req;
	}
	
	public boolean sendMove(String move, String uid, String battleId, String strength) throws IOException {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		String url = urlFormat(SEND_MOVE, uid, battleId, move, strength);
		HttpGet get = new HttpGet(url);
		setCharEncoding(get);
		HttpResponse resp = client.execute(get);
		client.close();
		
		if (resp.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
			return false;
		}
		return true;
	}
	/* returns a map with opponentMove and opponentStrenth. */
	public Map<String, String> getBattleData(String mId, String battleId) throws IOException {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		HttpGet get;
		InputStream stringStream;
		Element battleElem;
		Map<String, String> battleMap;
		
		String url = urlFormat(BATTLE_DATA, mId, battleId);
		get = new HttpGet(url);
		setCharEncoding(get);
		String response = responseToString(client.execute(get));
		stringStream = new ByteArrayInputStream(response.getBytes());
		battleElem = XMLUtils.getDocumentElement(stringStream);
		
		battleMap = new HashMap<String, String>();
		String oppMove = XMLUtils.getChildElementTextByTagName(battleElem, "opponentMove");
		String oppStrength = XMLUtils.getChildElementTextByTagName(battleElem, "opponentStrength");
		String oppId = XMLUtils.getChildElementTextByTagName(battleElem, "opponentId");
		if (oppMove.equals("null")) oppMove = null;
		if (oppStrength.equals("null")) oppStrength = null;
		if (oppId.equals("null")) oppId = null;
		battleMap.put(OPP_MOVE_KEY, oppMove);
		battleMap.put(OPP_STRENGTH_KEY, oppStrength);
		battleMap.put(OPP_ID_KEY, oppId);
		client.close();

		return battleMap;
	}
	
	public void closeBattle(String bid, String uid) throws IOException {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		String url = urlFormat(CLOSE_BATTLE, uid, bid);
		HttpGet get = new HttpGet(url);
		setCharEncoding(get);
		HttpResponse resp = client.execute(get);
		int status = resp.getStatusLine().getStatusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			//other guy probably already closed the battle.
		}
		client.close();

	}
	
	public List<String> getChallenges(String uid) throws IOException {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());
		String url = urlFormat(INCOMING_CHALLENGES, uid);
		HttpGet get = new HttpGet(url);
		setCharEncoding(get);
		HttpResponse resp = client.execute(get);
		int status = resp.getStatusLine().getStatusCode();
		client.close();
		if (status != HttpURLConnection.HTTP_OK){
			return null;
		}
		String response = responseToString(resp);
		String[] challengeIds = response.equals("") ? new String[0] : response.split("\n");
		List<String> challenges = new ArrayList<String>();
		for (String challengeId : challengeIds) {
			challenges.add(challengeId);
		}
		return challenges;
	}
	
	public JSONArray applyFriendFilter(JSONArray allFriends) throws Exception {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		HttpPost post = new HttpPost(FIND_FRIENDS);
		post.setEntity(new StringEntity(allFriends.toString()));
		setCharEncoding(post);
		HttpResponse resp = client.execute(post);
		int status = resp.getStatusLine().getStatusCode();
		client.close();
		if (status != HttpURLConnection.HTTP_OK) {
			System.out.println("failed to get filtered friends from server");
			return null;
		}
		String strResp = responseToString(resp);
		return new JSONArray(strResp);
	}
	
	
	private String responseToString(HttpResponse resp) throws IOException {

		HttpEntity entity = resp.getEntity();
		BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		char[] cbuf = new char[1024];
		int charsRead = br.read(cbuf, 0, cbuf.length);
		while (charsRead != -1) {
			sb.append(cbuf, 0, charsRead);
			charsRead = br.read(cbuf, 0, cbuf.length);
		}
		return sb.toString();
	}
	
	public String getNameForId(String uid) throws Exception {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());
		String url = urlFormat(GRAPH_BASE_URL + "%s", uid);
		HttpGet get = new HttpGet(url);
		setCharEncoding(get);
		HttpResponse resp = client.execute(get);
		int status = resp.getStatusLine().getStatusCode();
		client.close();
		if (status != HttpURLConnection.HTTP_OK) {
			System.out.println("failed to get basic info from fb");
			return null;
		}
		JSONObject json = new JSONObject(responseToString(resp));
		return json.getString("name");
	}
	
}
