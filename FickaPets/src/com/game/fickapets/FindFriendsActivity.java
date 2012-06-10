package com.game.fickapets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class FindFriendsActivity extends Activity {
	private static final String GET_MY_DATA = "me.data";
	private static final String GET_FRIENDS_DATA = "friends.data";
	private static final String GET_DATA_FOR_PHOTOS = "data for photos";
	private static final int NUM_PHOTOS = 5;						/* num photos taken from unfiltered friends list */
	Map<String, BattleState> currentBattles;

	public static final String FACEBOOK_BASE_URL = "https://graph.facebook.com/";
	private UrlImageViewHandler imageViewHandler;
	
	private Facebook facebook = new Facebook("439484749410212");

	/* this has the url and name of each person displayed */
	private Vector<FriendInfo> friends;
	
	private JSONObject facebookFriendsJson;
	private String mFacebookId;
	
	private boolean registerFailed = false;
	


	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageViewHandler = new UrlImageViewHandler(this);
        friends = new Vector<FriendInfo>();
        currentBattles = new HashMap<String, BattleState>();
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

	    		public void onFacebookError(FacebookError error) {
	    			showDialog(NetworkErrorDialog.FACEBOOK_SERVER_FAIL);
	    		}

	    		public void onError(DialogError e) {
	    			showDialog(NetworkErrorDialog.FACEBOOK_NETWORK_ERROR);
	    		}
	        
	    		public void onCancel() {
	    			showDialog(NetworkErrorDialog.FACEBOOK_NETWORK_ERROR);
	    		}
	    	});
	    } else {
	    	fetchData();
	    }

		
    }
	private void setLoadFriendsLayout() {
		setContentView(R.layout.on_progress);
		TextView textView = (TextView)findViewById(R.id.progressLayoutText);
		textView.setText("Loading facebook friends");
	}
    private void fetchData() {
        setLoadFriendsLayout();
    	AsyncFacebookRunner runner = new AsyncFacebookRunner(facebook);
    	
		/* remove this comment and comment out GET_DATA_FOR_PHOTOS to run friend filter */
		runner.request("me/friends", new FacebookDataListener(), GET_FRIENDS_DATA);
		
		runner.request("me", new FacebookDataListener(), GET_MY_DATA);
		
		// Comment this out and uncomment GET_FRIENDS_DATA
		//to run friend filter.  This just fetches first NUM_PHOTOS friends returned from facebook
		//runner.request("me/friends", new FacebookDataListener(), GET_DATA_FOR_PHOTOS);
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
    
    private void tryToRegister(final String uid) {
    	if (!PersistenceHandler.iAmRegistered(this)) {
    		final FickaServer server = new FickaServer(this);
        	final String pet = Pet.thePet(this).getDefaultImageName();

    		new Thread(new Runnable() {
    			public void run() {
    				boolean success = server.getMeRegistered(uid, pet);
    				if (success) {
    					PersistenceHandler.confirmUserRegistered(FindFriendsActivity.this);
    				} else {
    					registerFailed = true;
    					Runnable postErrorDialog = new Runnable() {
    						public void run() {
    	    					showDialog(NetworkErrorDialog.SERVER_FAIL);
    						}
    					};
    					runOnUiThread(postErrorDialog);
    				}
    			}
    		}).start();
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
    		/* battleNotifier can run for the first time - just got id */
    		Intent battleNotifier = new Intent(this, BattleNotifier.class);
    		startService(battleNotifier);
    		
    	}
    	tryToRegister(this.mFacebookId);
    }
    /* runs in UI thread */
    private void gotFriendsIdsForPhotos(JSONArray friendArr) {
    	try {
    		for (int i = 0; i < NUM_PHOTOS && i < friendArr.length(); i++) {
    			JSONObject friend = friendArr.getJSONObject(i);
    			String id = friend.getString("id");
    			String name = friend.getString("name");
    			String pet = friend.getString("pet");
    			friends.add(new FriendInfo(name, id, pet));
    			String url = FACEBOOK_BASE_URL + id + "/picture";
    			imageViewHandler.preLoadUrl(url);
    		}
    		renderLayout(friends);
    	} catch(Exception ex) {
    		ex.printStackTrace();
    		return;
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
        			methodToRun = new Runnable() {
        				public void run() {
        					gotFriendsIds(json);
        				}
        			};
    			} else if (responseType.equals(GET_DATA_FOR_PHOTOS)) {
    				final JSONArray data = json.getJSONArray("data");
    				while (!registerFailed && !PersistenceHandler.iAmRegistered(FindFriendsActivity.this)) {
    					try {
    						Thread.sleep(50);
    					} catch(InterruptedException ex) {}
    				}
    				/* the dialog will exit activity - but I don't think this thread will be shut down on its own */
    				if (registerFailed) return;
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
    		Runnable methodToRun = new Runnable() {
    			public void run() {
    				showDialog(NetworkErrorDialog.FACEBOOK_SERVER_FAIL);
    			}
    		};
    		runOnUiThread(methodToRun);
    	}
    	public void onFileNotFoundException(FileNotFoundException ex, Object state) {}
    	public void onIOException (IOException ex, Object state) {
    		Runnable methodToRun = new Runnable() {
    			public void run() {
    				showDialog(NetworkErrorDialog.FACEBOOK_NETWORK_ERROR);
    			}
    		};
    		runOnUiThread(methodToRun);
    	}
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
    		} finally {
    			client.close();
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
    		if (filteredFriends == null) {
    			showDialog(NetworkErrorDialog.SERVER_FAIL);
    		} else {
    			gotFriendsIdsForPhotos(filteredFriends);
    		}
    	}
    }
    
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
    	default:
    		return NetworkErrorDialog.createDialog(this, id);
    	}
    }
    
    private void renderLayout(List<FriendInfo> friends) {
    	setContentView(R.layout.find_friends);
    	List<BattleState> battleList = PersistenceHandler.getBattles(this);
    	for (BattleState battle : battleList) {
    		this.currentBattles.put(battle.opponentId, battle);
    	}
    	LinearLayout ll = (LinearLayout)findViewById(R.id.friendsList);
    	
		LayoutInflater inflater = getLayoutInflater();
		for (int i = 0; i < friends.size(); i++) {
			FriendInfo friendInfo = friends.get(i);
			
			RelativeLayout newRow = null;
			try {
				newRow = (RelativeLayout) inflater.inflate(R.layout.active_battle_row, null, false);
			} catch(InflateException ex) {
				ex.printStackTrace();
				return;
			}
			
			
			TextView textView = (TextView)newRow.getChildAt(0);
			textView.setTextColor(Color.rgb(255, 246, 239));
			String text;
			if (!currentBattles.containsKey(friendInfo.id)){
				text = friendInfo.name + "\nStart Battling!";
			} else {
				BattleState battle = currentBattles.get(friendInfo.id);
				text = BattleState.getBattleStateMessage(battle);
			}
			textView.setText(text);
			textView.setCompoundDrawablePadding(5);
			newRow.setOnClickListener(new ClickListener(i));
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 5, 0, 0);
			params.gravity = Gravity.LEFT;
			String url = FACEBOOK_BASE_URL + friendInfo.id + "/picture";
			imageViewHandler.setUrlDrawable(textView, url, R.drawable.mystery);

			ll.addView(newRow, params);
		}
		if (friends.size() == 0) {
			TextView noFriends = new TextView(this);
			noFriends.setGravity(Gravity.LEFT);
			noFriends.setTextColor(Color.rgb(255, 246, 239));
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 5, 0, 0);
			noFriends.setText("None of your friends currently play FickaPets");
			ll.addView(noFriends, params);
		}
    }
    

    
  
 /*  private class FriendArrayAdapter {
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
		   

		   String text = friends.get(position).name + "\n" + "Start Battling!";
		   view.setText(text);
		   
		   String url = FACEBOOK_BASE_URL + friends.get(position).id + "/picture";
		   
		   imageViewHandler.setUrlDrawable(view, url, R.drawable.mystery);
		   return view;
	   }
   } 
   */
    
   private class ClickListener implements OnClickListener {
	   private int index;
    	
	   public ClickListener(int index) {
		   this.index = index;
	   }
    	
	   @Override
	   public void onClick(View v) {
		   Intent intent = new Intent(FindFriendsActivity.this, BattleActivity.class);
		   FriendInfo friendInfo  = friends.get(index);
		   if (!currentBattles.containsKey(friendInfo.id)) {
			   intent.putExtra(BattleState.OPPONENT_NAME, friends.get(index).name);
			   intent.putExtra(BattleState.OPPONENT_ID, friends.get(index).id);
			   intent.putExtra(BattleState.MY_ID, mFacebookId);
			   intent.putExtra(BattleState.PET_IMG_NAME, friends.get(index).pet);
		   } else {
			   BattleState thisBattle = currentBattles.get(friendInfo.id);
			   intent = thisBattle.addStateToIntent(intent);
		   }
		   startActivity(intent);
	   }
   }
 
   
   private class FriendInfo {
	   String name;
	   String id;
	   String pet;
	   public FriendInfo(String name, String id, String pet) {
		   this.name = name;
		   this.id = id;
		   this.pet = pet;
	   }
   }
}
