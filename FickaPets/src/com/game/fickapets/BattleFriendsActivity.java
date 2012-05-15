package com.game.fickapets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;

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
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class BattleFriendsActivity extends ListActivity {
	Facebook facebook = new Facebook("439484749410212");
	AsyncFacebookRunner runner;
	Vector<String> photoUrls = new Vector<String>();
	private static final int NUM_PHOTOS = 5;
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
		runner.request("me/friends", new FriendDataListener());

        
        
    }
    private void displayPhotos() {
		ListView lv = getListView();
		MyAdapter adapter = new MyAdapter(BattleFriendsActivity.this);
		lv.setAdapter(adapter);
		for (String img : photoUrls) {
			System.out.println(img);
			adapter.add(img);
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
    
    private  class FriendDataListener implements AsyncFacebookRunner.RequestListener {
    	public void onComplete(String response, Object state) {
    		System.out.println(response);
    		try {
    			final JSONObject json = new JSONObject(response);
    			final JSONArray data = json.getJSONArray("data");
    			for (int i = 0; i < NUM_PHOTOS; i++) {
    				JSONObject friend = data.getJSONObject(i);
    				String friendId = friend.getString("id");
    				photoUrls.add("https://graph.facebook.com/" + friendId + "/picture");
    				//AsyncFacebookRunner runner = new AsyncFacebookRunner(facebook);
    				//runner.request(friendId + "/picture", new PhotoURLListener());
    				
    				/*runOnUiThread(new Thread(new Runnable() {
    					public void run() {
    						
    					}
    				});*/
    			}
    			runOnUiThread(new Thread(new Runnable() {
    				public void run() {
    					displayPhotos();
    				}
    			}));
    		} catch(Exception ex) {
    			
    		}
    	}
    	public void onFacebookError(FacebookError fbe, Object state) {}
    	public void onFileNotFoundException(FileNotFoundException ex, Object state) {}
    	public void onIOException (IOException ex, Object state) {}
    	public void onMalformedURLException(MalformedURLException ex, Object state) {}
    }
    
    
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
   }
}
