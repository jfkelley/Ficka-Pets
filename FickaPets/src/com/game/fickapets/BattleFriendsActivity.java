package com.game.fickapets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

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
import android.widget.ImageView;

public class BattleFriendsActivity extends ListActivity {
	private static String BASE_SERVER_URL = "http://10.31.114.24:8888/";
	private static final String GET_MY_DATA = "me.data";
	private static final String GET_FRIENDS_DATA = "friends.data";
	//private static final int NUM_PHOTOS = 5;
	
	private static final String FIND_FRIENDS_URL = "findfriends";
	private static final String MY_DATA_URL = "me";
	private static final String MY_FRIENDS_DATA_URL = "me/friends";
	
	Facebook facebook = new Facebook("439484749410212");
	//private AsyncFacebookRunner runner;
	//private Vector<String> photoUrls = new Vector<String>();
	private String friendsFacebookIdJson;
	private String mFacebookId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
		PersistenceHandler.saveFacebookAccess(BattleFriendsActivity.this, facebook.getAccessToken(), facebook.getAccessExpires());
		
		AsyncFacebookRunner runner = new AsyncFacebookRunner(facebook);
		runner.request(MY_FRIENDS_DATA_URL, new FacebookDataListener(), GET_FRIENDS_DATA);
		runner.request(MY_DATA_URL, new FacebookDataListener(), GET_MY_DATA);
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
    		AndroidHttpClient client = AndroidHttpClient.newInstance(BattleFriendsActivity.this.getPackageName());
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
    
  /*  
   private class MyAdapter extends ArrayAdapter<String> {
	   public MyAdapter(Context context) {
		   super (context, 0);
	   }
	   
	   @Override
	   public View getView(int position, View convertView, ViewGroup group) {
		   ImageView view = (ImageView) convertView;
		   if (view == null) {
			   view = new ImageView(BattleFriendsActivity.this);
		   }
		   UrlImageViewHandler.setUrlDrawable(view, getItem(position), R.drawable.ic_launcher);
		   return view;
	   }
   } */
}
