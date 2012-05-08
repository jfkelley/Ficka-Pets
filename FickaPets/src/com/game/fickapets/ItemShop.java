package com.game.fickapets;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ItemShop extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("created shop");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shop);
        LinearLayout ll = (LinearLayout)findViewById(R.id.shopLinearLayout);
        for (Item item : ItemManager.allItems(this)) {
        	addButtonForItem(item, ll);
        }
        updateTotalCoins();
	}
	
	private void addButtonForItem(final Item item, LinearLayout ll) {
		Button b = new Button(this);
		b.setText(item.getName() + " - " + item.getPrice());
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				User.theUser(ItemShop.this).buyItem(item);
				updateTotalCoins();
			}
		});
		ll.addView(b);
	}

	private void updateTotalCoins() {
		int coins = User.theUser(this).getCoins();
		String plural = coins == 1 ? "" : "s";
		((TextView)findViewById(R.id.shopTotalCoins)).setText("You have " + coins + " coin" + plural + ".");
	}
}
