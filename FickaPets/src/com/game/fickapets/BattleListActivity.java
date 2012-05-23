package com.game.fickapets;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class BattleListActivity extends Activity{
	
	JSONArray battleArr;
	UrlImageViewHandler imageViewHandler;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.battle_list);
		
		
		TextView tv = new TextView(this);
		imageViewHandler = new UrlImageViewHandler(this);
		tv.setText("Current Battles");
		ListView lv = (ListView) findViewById(R.id.openBattleList);
		lv.addHeaderView(tv);
		battleArr = PersistenceHandler.getBattles(this);
		List<String> list = fillList(battleArr);
		lv.setAdapter(new BattleAdapter(this, list));
		lv.setOnItemClickListener(new ItemClickListener());
	}
	/* again, not sure how to get array adapter to work without this list */
	private List<String> fillList(JSONArray battleArr) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < battleArr.length(); i++) {
			list.add("string");
		}
		return list;
	}
	
	private class ItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			try {
				Intent intent = new Intent(BattleListActivity.this, BattleActivity.class);
				JSONObject thisBattle = battleArr.getJSONObject(position-1);
				intent.putExtra(BattleActivity.OPPONENT_NAME_KEY, thisBattle.getString(PersistenceHandler.OPPONENT));
				intent.putExtra(BattleActivity.OPPONENT_ID_KEY, thisBattle.getString(PersistenceHandler.OPPONENT_ID));
				intent.putExtra(BattleActivity.MY_ID_KEY, thisBattle.getString(PersistenceHandler.MY_ID));
				intent.putExtra(BattleActivity.MY_MOVE_KEY, thisBattle.getString(PersistenceHandler.MY_MOVE));
				intent.putExtra(BattleActivity.BATTLE_ID_KEY, thisBattle.getString(PersistenceHandler.BATTLE_ID));
				startActivity(intent);
			} catch(Exception ex) {
				System.out.println("Failed to get data out of json array");
				ex.printStackTrace();
			}
		}
		
	}
	
	public void onFindFriendsClicked(View v) {
		Intent intent = new Intent(BattleListActivity.this, FindFriendsActivity.class);
		startActivity(intent);
	}
	
	private class BattleAdapter extends ArrayAdapter<String> {
		public BattleAdapter(Context context, List<String> list) {
			super(context, 0, list);
		}
		
		public View getView(int position, View convertView, ViewGroup group) {
			try {
				TextView textView = (TextView) convertView;
				if (textView == null) {
					textView = new TextView(BattleListActivity.this);
				}
				textView.setCompoundDrawablePadding(10);
				JSONObject thisBattle = battleArr.getJSONObject(position);
				String text = "Continue battle with " + thisBattle.getString(PersistenceHandler.OPPONENT);
				textView.setText(text);
				String url = FindFriendsActivity.FACEBOOK_BASE_URL + thisBattle.getString(PersistenceHandler.BATTLE_ID) + "/picture";
				imageViewHandler.setUrlDrawable(textView, url, R.drawable.ic_launcher);
				return textView;
			} catch(JSONException ex) {
				System.out.println("Failed at pulling battle data from json array");
				ex.printStackTrace();
				return null;
			}
		}
	}
	
}
