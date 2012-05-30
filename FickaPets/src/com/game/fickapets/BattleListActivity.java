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
	
	List<BattleState> battleArr;
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
		/* when i learn more about array adapters, get rid of this list */
		List<String> list = fillList(battleArr);
		lv.setAdapter(new BattleAdapter(this, list));
		lv.setOnItemClickListener(new ItemClickListener());
	}
	/* again, not sure how to get array adapter to work without this list - returns a list of strings that is the same size as the listview */
	private List<String> fillList(List<BattleState> battleArr) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < battleArr.size(); i++) {
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
				intent = BattleState.addExtrasToIntent(intent, battleArr.get(position-1).toJSON());
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
				JSONObject thisBattle = battleArr.get(position).toJSON();
				String text = "Continue battle with " + thisBattle.getString(BattleState.OPPONENT_NAME);
				textView.setText(text);
				String url = FindFriendsActivity.FACEBOOK_BASE_URL + thisBattle.getString(BattleState.OPPONENT_ID) + "/picture";
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
