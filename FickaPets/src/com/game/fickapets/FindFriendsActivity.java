package com.game.fickapets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
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
	private static String BASE_SERVER_URL = "http://10.31.114.24:8888/";
	private static final String GET_MY_DATA = "me.data";
	private static final String GET_FRIENDS_DATA = "friends.data";
	private static final String GET_DATA_FOR_PHOTOS = "data for photos";
	private static final int NUM_PHOTOS = 25;
	
	private static final String FIND_FRIENDS_URL = "findfriends";
	private static final String FACEBOOK_BASE_URL = "https://graph.facebook.com/";
	
	private UrlImageViewHandler imageViewHandler;
	
	Facebook facebook = new Facebook("439484749410212");
	//private AsyncFacebookRunner runner;
	//private Vector<String> photoUrls = new Vector<String>();
	/* this has the url and name of each person displayed */
	private Vector<FriendPhotoInfo> friends;
	
	private String friendsFacebookIdJson;
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

        		}

        		public void onFacebookError(FacebookError error) {}

        		public void onError(DialogError e) {}
            
        		public void onCancel() {}
        	});
        }
		PersistenceHandler.saveFacebookAccess(FindFriendsActivity.this, facebook.getAccessToken(), facebook.getAccessExpires());
		
		AsyncFacebookRunner runner = new AsyncFacebookRunner(facebook);
		//runner.request("me/friends", new FacebookDataListener(), GET_FRIENDS_DATA);
		//runner.request("me", new FacebookDataListener(), GET_MY_DATA);
		runner.request("me/friends", new FacebookDataListener(), GET_DATA_FOR_PHOTOS);
    }
    
    /* runs in UI thread */
    private void gotFriendsIds(String friendsFacebookIdJson) {
    	if (this.friendsFacebookIdJson == null) {
        	this.friendsFacebookIdJson = friendsFacebookIdJson;
        	if (mFacebookId != null) {
    			new AsyncFacebookFriendFinder().execute();
        	}
    	}
    	
    }
    
    /* runs in UI thread */
    private void gotMyId(String mFacebookId) {
    	if (this.mFacebookId == null) {
    		this.mFacebookId = mFacebookId;
    		if (friendsFacebookIdJson != null) {
    			new AsyncFacebookFriendFinder().execute();
    		}
    	}
    }
    /* runs in UI thread */
    private void gotFriendsIdsForPhotos(JSONArray friendArr) {
    	/* uh, not actually using urls vector.  Using friends vector since I need url and name. urls vec exists because listadapter
    	 * only accepts vector of strings.  uugghh */
    	Vector<String> fakeOut = new Vector<String>();
    	try {
    		for (int i = 0; i < NUM_PHOTOS && i < friendArr.length(); i++) {
    			String id = friendArr.getJSONObject(i).getString("id");
    			fakeOut.add(id);
    			String name = friendArr.getJSONObject(i).getString("name");
    			friends.add(new FriendPhotoInfo(name, id));
    			String url = FACEBOOK_BASE_URL + id + "/picture";
    			imageViewHandler.preLoadUrl(url);
    		}
    	} catch(Exception ex) {
    		ex.printStackTrace();
    		return;
    	}
    	setListAdapter(new FriendArrayAdapter(this, fakeOut));
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
        			final JSONArray data = json.getJSONArray("data");
        			final String friendsArr = data.toString();
        			methodToRun = new Runnable() {
        				public void run() {
        					gotFriendsIds(friendsArr);
        				}
        			};
    			} else if (responseType.equals(GET_DATA_FOR_PHOTOS)) {
    				final JSONArray data = json.getJSONArray("data");
    				methodToRun = new Runnable() {
    					public void run() {
    						gotFriendsIdsForPhotos(data);
    					}
    				};
    			}
     			if (methodToRun != null) {
    				runOnUiThread(new Thread(methodToRun));
    			}
    		} catch(Exception ex) {
    			ex.printStackTrace();
    			return;
    		}
    	}
    	public void onFacebookError(FacebookError fbe, Object state) {}
    	public void onFileNotFoundException(FileNotFoundException ex, Object state) {}
    	public void onIOException (IOException ex, Object state) {}
    	public void onMalformedURLException(MalformedURLException ex, Object state) {}
    }
   
    private class AsyncFacebookFriendFinder extends AsyncTask<Void, Void, JSONObject> {
    	
    	private String streamToString(InputStream is) throws IOException {
    		BufferedReader br = new BufferedReader(new InputStreamReader(is));
    		char[] bytes = new char[1024];
    		int bytesRead;
    		StringBuilder sb = new StringBuilder();
    		while ((bytesRead = br.read(bytes, 0, bytes.length)) != -1) {
    			sb.append(bytes, 0, bytesRead);
    		}
    		return sb.toString();
    	}
    	
    	protected JSONObject doInBackground(Void...params) {
    		AndroidHttpClient client = AndroidHttpClient.newInstance(FindFriendsActivity.this.getPackageName());
    		String url = BASE_SERVER_URL + FIND_FRIENDS_URL;
    		JSONObject jsonResponse = null;
    		try {
        		HttpPost post = new HttpPost(url);
        		post.setEntity(new StringEntity(friendsFacebookIdJson));
        		HttpParams httpParams = new BasicHttpParams();
        		httpParams.setParameter("id", mFacebookId);
        		post.setParams(httpParams); 
    			
        		HttpResponse httpResponse = client.execute(post);
        		/* this returns a 404 right now */
        		int status = httpResponse.getStatusLine().getStatusCode();
        		if (status != HttpURLConnection.HTTP_OK){
        			// add a status message here
        			return null;
        		}
        		String response = streamToString(httpResponse.getEntity().getContent());
        		jsonResponse = new JSONObject(response);
        		System.out.println(jsonResponse);
    		} catch(Exception ex) {
    			ex.printStackTrace();
    		} finally {
    			client.close();
    		}
			return jsonResponse;
    	}
    	
    	protected void onPostExecute(JSONObject jsonResponse) {
    		System.out.println(jsonResponse);
    	}
    }
    
  
   private class FriendArrayAdapter extends ArrayAdapter<String> {
	   public FriendArrayAdapter(Context context, List<String> urls) {
		   super (context, 0, urls);
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
		   
		   imageViewHandler.setUrlDrawable(view, url, R.drawable.ic_launcher);
		   return view;
	   }
   } 
   @Override
   public void onListItemClick(ListView lv, View view, int position, long id) {
	   Intent intent = new Intent(FindFriendsActivity.this, BattleActivity.class);
	   intent.putExtra("name",friends.get(position).name);
	   intent.putExtra("id", friends.get(position).id);
	   
	   /* battle activity not done */
	   //startActivity(intent);
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
