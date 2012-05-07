package com.game.fickapets;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ItemShop extends Activity {
	User user;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = PersistenceHandler.buildUser(this);
        setContentView(R.layout.shop);
        LinearLayout ll = (LinearLayout)findViewById(R.id.shopLinearLayout);
        for (Item item : ItemManager.allItems(this)) {
        	addButtonForItem(item, ll);
        }
        updateTotalCoins();
	}
	
	public void onDestroy () {
    	super.onDestroy();
    	PersistenceHandler.saveState (this, user);
    }
	
	private void addButtonForItem(final Item item, LinearLayout ll) {
		Button b = new Button(this);
		b.setText(item.getName() + " - " + item.getPrice());
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				user.buyItem(item);
				updateTotalCoins();
			}
		});
		ll.addView(b);
	}

	private void updateTotalCoins() {
		int coins = user.getCoins();
		String plural = coins == 1 ? "" : "s";
		((TextView)findViewById(R.id.shopTotalCoins)).setText("You have " + coins + " coin" + plural + ".");
	}
}
