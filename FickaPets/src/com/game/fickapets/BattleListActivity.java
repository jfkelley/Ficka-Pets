package com.game.fickapets;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BattleListActivity extends Activity{
	private static final int IMAGE_PADDING = 5;				/* pixels I guess */
	List<BattleState> battleArr;
	UrlImageViewHandler imageViewHandler;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		createDisplay();
	}
	
	public void onResume() {
		super.onResume();
		createDisplay();
	}
	
	private void createDisplay() {
		setContentView(R.layout.battle_list);
		imageViewHandler = new UrlImageViewHandler(this);
		battleArr = PersistenceHandler.getBattles(this);
		
		LinearLayout ll = (LinearLayout)findViewById(R.id.currentBattles);
		
	
		/*TextView textView = new TextView(this);
		
		textView.setTextColor(Color.WHITE);
		textView.setText("hello");
		ll.addView(textView);*/
		
		LayoutInflater inflater = getLayoutInflater();
		for (int i = 0; i < battleArr.size(); i++) {
			BattleState activeBattle = battleArr.get(i);
			RelativeLayout newRow = null;
			try {
				newRow = (RelativeLayout) inflater.inflate(R.layout.active_battle_row, null, false);
			} catch(InflateException ex) {
				ex.printStackTrace();
				return;
			}
			newRow.setOnClickListener(new ClickListener(i));
			TextView rowTextView = (TextView)newRow.getChildAt(0);
			ImageView rowImageView = (ImageView)newRow.getChildAt(1);
			rowTextView.setCompoundDrawablePadding(IMAGE_PADDING);
			String url = FindFriendsActivity.FACEBOOK_BASE_URL + activeBattle.opponentId + "/picture";
			String oppName = getDisplayName(activeBattle);
			int defaultPhoto = R.drawable.mystery;
			if (activeBattle.getOpponentMove() != null) {
				rowTextView.setText("vs " + oppName + "\npicked a move");
				imageViewHandler.setUrlDrawable(rowTextView, url, defaultPhoto);
				rowImageView.setImageResource(R.drawable.green_dot);
			} else if (activeBattle.getMyMove() != null) {
				rowTextView.setText("vs " + oppName + "\nWaiting for their move");
				imageViewHandler.setUrlDrawable(rowTextView, url, defaultPhoto);
				rowImageView.setImageResource(R.drawable.yellow_dot);
			} else {
				rowTextView.setText("vs " + oppName + "\nMake your move");
				imageViewHandler.setUrlDrawable(rowTextView, url, defaultPhoto);
				rowImageView.setImageResource(R.drawable.green_dot);
			}
			ll.addView(newRow);
		} 	
		if (battleArr.size() == 0) {
			TextView noBattleMsg = new TextView(this);
			noBattleMsg.setText("No active battles");
			noBattleMsg.setTextColor(Color.rgb(255, 246, 239));
			ll.addView(noBattleMsg);
		}
	}
	
	private String getDisplayName(BattleState activeBattle) {
		String[] names = activeBattle.opponentName.split(" ");
		String firstName = "";
		String lastNameLetter = "";
		if (names.length > 0) {
			firstName = names[0];
		}
		if (names.length > 1) {
			lastNameLetter = names[names.length-1].substring(0, 1);
		}
		return firstName + " " + lastNameLetter + ".";
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		LinearLayout ll = (LinearLayout)findViewById(R.id.currentBattles);
		int listWidth = getLinearLayoutWidth(ll);
		setNewBattleButtonWidth(ll, listWidth);
		setTitleSeparatorWidth(ll, listWidth);
		LinearLayout.LayoutParams llParams = (LinearLayout.LayoutParams)ll.getLayoutParams();
		llParams.width = listWidth;
		ll.setLayoutParams(llParams);

		
		
	}
	
	private void setTitleSeparatorWidth(LinearLayout ll, int llWidth) {
		int width = (int)(llWidth * 0.75);
		ImageView lineSeparator = (ImageView)ll.getChildAt(1);
		ViewGroup.LayoutParams params = lineSeparator.getLayoutParams();
		params.width = width;
		lineSeparator.setLayoutParams(params);
	}
	
	private void setNewBattleButtonWidth(LinearLayout ll, int width) {
		LinearLayout parentLinLayout = (LinearLayout)ll.getParent();
		//ImageView button = (ImageView)parentLinLayout.getChildAt(0);
		Button button = (Button)parentLinLayout.getChildAt(0);
		ViewGroup.LayoutParams params = button.getLayoutParams();
		params.width = width;
		button.setLayoutParams(params);
		//button.getLayoutParams().width = width;
	}
	
	private int getLinearLayoutWidth(LinearLayout ll) {
		LinearLayout parent = (LinearLayout) ll.getParent();
		return (int)(parent.getWidth() * 0.85);
	}
	
	private class ClickListener implements OnClickListener {
		private int index;
		public ClickListener(int index) {
			this.index = index;
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(BattleListActivity.this, BattleActivity.class);
			intent.putExtras(battleArr.get(index).bundleUp());
			startActivity(intent);
		}
	}
	
	public void onFindFriendsClicked(View v) {
		Intent intent = new Intent(BattleListActivity.this, FindFriendsActivity.class);
		startActivity(intent);
	}
	
}
