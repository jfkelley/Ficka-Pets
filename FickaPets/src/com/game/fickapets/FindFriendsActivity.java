package com.game.fickapets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FindFriendsActivity extends ListActivity {
	private static final String GET_MY_DATA = "me.data";
	private static final String GET_FRIENDS_DATA = "friends.data";
	private static final String GET_DATA_FOR_PHOTOS = "data for photos";
	private static final int NUM_PHOTOS = 5;						/* num photos taken from unfiltered friends list */
	

	public static final String FACEBOOK_BASE_URL = "https://graph.facebook.com/";
	private UrlImageViewHandler imageViewHandler;
	
	private Facebook facebook = new Facebook("439484749410212");

	/* this has the url and name of each person displayed */
	private Vector<FriendPhotoInfo> friends;
	
	private JSONObject facebookFriendsJson;
	private String mFacebookId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        imageViewHandler = new UrlImageViewHandler(this);
        friends = new Vector<FriendPhotoInfo>();
        
	    String accessToken = PersistenceHandler.facebookAccessToken(this);
	    long expires = PersistenceHandler.facebookTokenExpiration(this);
	    if (accessToken != null) {
	    	facebook.setAccessToken(accessToken);
	    }
	    if (expires != 0) {
	    	facebook.setAccessExpires(expires);
	    }
	    if (!facebook.isSessionValid()) {
	    	facebook.authorize(this, new DialogListener() {
	    		//@Override
	    		public void onComplete(Bundle values) {
	    			PersistenceHandler.saveFacebookAccess(FindFriendsActivity.this, facebook.getAccessToken(), facebook.getAccessExpires());
	    			fetchData();
	    		}

	    		public void onFacebookError(FacebookError error) {}

	    		public void onError(DialogError e) {}
	        
	    		public void onCancel() {}
	    	});
	    } else {
	    	fetchData();
	    }

		
    }
    
    private void fetchData() {
    	AsyncFacebookRunner runner = new AsyncFacebookRunner(facebook);
    	
		/* remove this comment and comment out GET_DATA_FOR_PHOTOS to run friend filter */
		//runner.request("me/friends", new FacebookDataListener(), GET_FRIENDS_DATA);
		
		runner.request("me", new FacebookDataListener(), GET_MY_DATA);
		
		// Comment this out and uncomment GET_FRIENDS_DATA
		//to run friend filter.  This just fetches first NUM_PHOTOS friends returned from facebook
		runner.request("me/friends", new FacebookDataListener(), GET_DATA_FOR_PHOTOS);
    }
    
    /* runs in UI thread */
    private void gotFriendsIds(JSONObject facebookFriendsJson) {
    	if (this.facebookFriendsJson == null) {
        	this.facebookFriendsJson = facebookFriendsJson;
        	if (mFacebookId != null) {
    			new FickaServerFilter().execute(facebookFriendsJson);
        	}
    	}
    	
    }
    
    /* runs in UI thread. I want facebook id before we start putting friends in view so it's there
     * before user can move to battle activity */
    private void gotMyId(String mFacebookId) {
    	if (this.mFacebookId == null) {
    		this.mFacebookId = mFacebookId;
    		PersistenceHandler.saveFacebookId(this, mFacebookId);
    		if (facebookFriendsJson != null) {
    			new FickaServerFilter().execute(facebookFriendsJson);
    		}
    	}
    }
    /* runs in UI thread */
    private void gotFriendsIdsForPhotos(JSONArray friendArr) {
		/* this list doesn't do anything.  Haven't been able to get listview working without this thing */
		ArrayList<String> list = new ArrayList<String>();
    	try {
    		for (int i = 0; i < NUM_PHOTOS && i < friendArr.length(); i++) {
    			String id = friendArr.getJSONObject(i).getString("id");
    			String name = friendArr.getJSONObject(i).getString("name");
    			friends.add(new FriendPhotoInfo(name, id));
    			list.add(id);
    			String url = FACEBOOK_BASE_URL + id + "/picture";
    			imageViewHandler.preLoadUrl(url);
    		}
    	} catch(Exception ex) {
    		ex.printStackTrace();
    		return;
    	}
    	setListAdapter(new FriendArrayAdapter(this, list));
    }
    
    
    @Override
    public void onResume() {
    	super.onResume();
    	facebook.extendAccessTokenIfNeeded(this, null);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    private  class FacebookDataListener implements AsyncFacebookRunner.RequestListener {
    	
    	public void onComplete(String response, Object state) {
    		System.out.println(response);
    		try {
    			final JSONObject json = new JSONObject(response);
    			String responseType = (String) state;
    			Runnable methodToRun = null;
    			if (responseType.equals(GET_MY_DATA)) {
    				final String mId = json.getString("id");
    				methodToRun = new Runnable() {
    					public void run() {
    						gotMyId(mId);
    					}
    				};
    			} else if (responseType.equals(GET_FRIENDS_DATA)) {
        			methodToRun = new Runnable() {
        				public void run() {
        					gotFriendsIds(json);
        				}
        			};
        			/* only run since there aren't any friends on server currently */
    			} else if (responseType.equals(GET_DATA_FOR_PHOTOS)) {
    				final JSONArray data = json.getJSONArray("data");
    				methodToRun = new Runnable() {
    					public void run() {
    						gotFriendsIdsForPhotos(data);
    					}
    				};
    			}
     			if (methodToRun != null) {
    				runOnUiThread(methodToRun);
    			}
    		} catch(Exception ex) {
    			ex.printStackTrace();
    			return;
    		}
    	}
    	public void onFacebookError(FacebookError fbe, Object state) {
    		
    	}
    	public void onFileNotFoundException(FileNotFoundException ex, Object state) {}
    	public void onIOException (IOException ex, Object state) {}
    	public void onMalformedURLException(MalformedURLException ex, Object state) {
    		
    	}
    }
   /* goes to server to find friends filtered by who plays game or not */
    private class FickaServerFilter extends AsyncTask<JSONObject, Void, JSONArray> {
    	
    	private String streamToString(InputStream is) throws IOException {
    		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    		char[] bytes = new char[1024];
    		int bytesRead;
    		StringBuilder sb = new StringBuilder();
    		while ((bytesRead = br.read(bytes, 0, bytes.length)) != -1) {
    			sb.append(bytes, 0, bytesRead);
    		}
    		return sb.toString();
    	}
    	private JSONArray concatJSONArray(JSONArray baseArr, JSONArray arrToAppend) throws JSONException{
    		for (int i = 0; i < arrToAppend.length(); i++) {
    			baseArr.put(baseArr.length(), arrToAppend.getJSONObject(i));
    		}
    		return baseArr;
    	}
    	
    	/* flattens linked list of JSONObjects that facebook returns into an array of name:id JSONObjects */
    	private JSONArray flattenFriends(JSONObject friends) {
    		AndroidHttpClient client = AndroidHttpClient.newInstance(FindFriendsActivity.this.getPackageName());
    		JSONArray flattenedArr = null;
    		try {
        		flattenedArr = friends.getJSONArray("data");
    			while (friends.getJSONObject("paging").has("next")) {
    				HttpGet get = new HttpGet(friends.getJSONObject("paging").getString("next"));
    				HttpResponse resp = client.execute(get);
    				int status = resp.getStatusLine().getStatusCode();
    				if (status != HttpURLConnection.HTTP_OK) {
    					return flattenedArr;
    				}
    				friends = new JSONObject(streamToString(resp.getEntity().getContent()));
    				flattenedArr = concatJSONArray(flattenedArr, friends.getJSONArray("data"));
    			}
    		} catch(Exception ex) {
    			System.out.println("failed to flatten array");
    			ex.printStackTrace();
    		}
    		return flattenedArr;
    	}
    	
    	protected JSONArray doInBackground(JSONObject...friends) {
    		JSONArray filteredFriends = null;
    		JSONArray allFriends = flattenFriends(friends[0]);
    		FickaServer server = new FickaServer(FindFriendsActivity.this);
    		try {
    			filteredFriends = server.applyFriendFilter(allFriends);
    		} catch(Exception ex) {
    			ex.printStackTrace();
    		}
    		return filteredFriends;
    	}
    	
    	protected void onPostExecute(JSONArray filteredFriends) {
    		gotFriendsIdsForPhotos(filteredFriends);
    	}
    }
    
  
   private class FriendArrayAdapter extends ArrayAdapter<String> {
	   public FriendArrayAdapter(Context context, List<String> list) {
		   super (context, 0, list);
	   }
	   
	   @Override
	   public View getView(int position, View convertView, ViewGroup group) {
		   TextView view = (TextView) convertView;
		   if (view == null) {
			   view = new TextView(FindFriendsActivity.this);
		   }
		   view.setCompoundDrawablePadding(10);
		   
		   /* this'll eventually be a spannable string. No time for that now */
		   String text = friends.get(position).name + "\n" + "Start Battling!";
		   view.setText(text);
		   
		   String url = FACEBOOK_BASE_URL + friends.get(position).id + "/picture";
		   
		   imageViewHandler.setUrlDrawable(view, url, R.drawable.mystery);
		   return view;
	   }
   } 
   @Override
   public void onListItemClick(ListView lv, View view, int position, long id) {
	   Intent intent = new Intent(FindFriendsActivity.this, BattleActivity.class);
	   intent.putExtra(BattleState.OPPONENT_NAME,friends.get(position).name);
	   intent.putExtra(BattleState.OPPONENT_ID, friends.get(position).id);
	   intent.putExtra(BattleState.MY_ID, mFacebookId);
	   startActivity(intent);
   }
   
   private class FriendPhotoInfo {
	   String name;
	   String id;
	   public FriendPhotoInfo(String name, String id) {
		   this.name = name;
		   this.id = id;
	   }
   }
}
