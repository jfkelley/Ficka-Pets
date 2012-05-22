package com.game.fickapets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.w3c.dom.Element;

import android.content.Context;
import android.net.http.AndroidHttpClient;

public class FickaServer {
	public static final String OPP_MOVE_KEY = "opponentmove";
	public static final String OPP_STRENGTH_KEY = "opponentStrength";
	
	private static final String BASE_URL = "http://10.31.112.42:8888/";
	private static final String CREATE = BASE_URL + "create?uid1=%s&uid2=%s";
	private static final String SEND_MOVE = BASE_URL + "sendmove?uid=%s&bid=%s&move=%s";
	private static final String BATTLE_DATA = BASE_URL + "battledata?uid=%s&bid=%s";
	private static final String CLOSE_BATTLE = BASE_URL + "closebattle?uid=%s&bid=%s";
	private static final String FIND_FRIENDS = BASE_URL + "findfriends";
	
	//private AndroidHttpClient client;
	private Context context;
	public FickaServer(Context context) {
		this.context = context;
		//client = AndroidHttpClient.newInstance(context.getPackageName());
	}
	
	/* must call this when done using this instance, else we'll get an error - could override client's finalize if this becomes an issue*/
	public void close() {
		//client.close();
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
			return null;
		}
		String battleId = responseToString(resp);
		client.close();
		return battleId;
	}

	private HttpRequestBase setCharEncoding(HttpRequestBase req) {
		req.setHeader("Content-type", "text/plain; charset=UTF-8");
		return req;
	}
	
	public boolean sendMove(String move, String uid, String battleId) throws IOException {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		String url = urlFormat(SEND_MOVE, uid, battleId, move);
		HttpGet get = new HttpGet(url);
		setCharEncoding(get);
		HttpParams params = new BasicHttpParams();
		params.setParameter("uid", uid);
		params.setParameter("bid", battleId);
		params.setParameter("move", move);
		get.setParams(params);
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
		battleMap.put(OPP_MOVE_KEY, XMLUtils.getChildElementTextByTagName(battleElem, "opponentMove"));
		battleMap.put(OPP_STRENGTH_KEY, XMLUtils.getChildElementTextByTagName(battleElem, "opponentStrength"));
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
	
	public JSONArray applyFriendFilter(JSONArray allFriends) throws Exception {
		AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());

		HttpPost post = new HttpPost(FIND_FRIENDS);
		post.setEntity(new StringEntity(allFriends.toString()));
		setCharEncoding(post);
		HttpResponse resp = client.execute(post);
		int status = resp.getStatusLine().getStatusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			System.out.println("failed to get filtered friends from server");
			return null;
		}
		String strResp = responseToString(resp);
		client.close();
		return new JSONArray(strResp);
	}
	
	
	private String responseToString(HttpResponse resp) throws IOException {

		HttpEntity entity = resp.getEntity();
		BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		char[] cbuf = new char[1024];
		int charsRead = br.read(cbuf, 0, cbuf.length);
		while (charsRead != -1) {
			sb.append(cbuf);
			charsRead = br.read(cbuf, 0, cbuf.length);
		}
		return sb.toString();
	}
}
