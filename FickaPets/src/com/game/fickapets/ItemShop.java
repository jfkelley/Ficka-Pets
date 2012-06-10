package com.game.fickapets;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ItemShop extends Activity {
	private static final int IMAGE_PADDING = 5;				/* pixels I guess */
	private static final String DRAWABLE_DEFTYPE = "drawable";
	private static final String PACKAGE_NAME = "com.game.fickapets";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_shop);
		LinearLayout ll = (LinearLayout)findViewById(R.id.itemList);

		LayoutInflater inflater = getLayoutInflater();
		for (Item item : ItemManager.allItems(this)) {
			System.out.println(String.format("Item id=%s, name=%s, image=%s, price=%d", item.getId(), item.getName(), item.getImage(), item.getPrice()));
			addButtonForItem(item, ll, inflater);
		}
		updateTotalCoins();
	}

	private void addButtonForItem(final Item item, LinearLayout ll, LayoutInflater inflater) {
		RelativeLayout newRow = null;
		try {
			newRow = (RelativeLayout) inflater.inflate(R.layout.item_row, ll, false);
		} catch(InflateException ex) {
			ex.printStackTrace();
			return;
		}
		newRow.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog alert = new AlertDialog.Builder(ItemShop.this)
				.setMessage("Are you sure you want to buy " + item.getGrammaticalName() + "?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						User.theUser(ItemShop.this).buyItem(item);
						updateTotalCoins();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				})
				.create();
				alert.show();
			}
		});

		TextView rowTextView = (TextView)newRow.getChildAt(0);
		ImageView rowImageView = (ImageView)newRow.getChildAt(1);
		rowTextView.setCompoundDrawablePadding(IMAGE_PADDING);
		rowTextView.setText(item.getName() + " - " + item.getPrice());
		int id = getResources().getIdentifier(item.getImage(), DRAWABLE_DEFTYPE, PACKAGE_NAME);
		rowImageView.setImageResource(id);
		ll.addView(newRow);
	}

	private void updateTotalCoins() {
		int coins = User.theUser(this).getCoins();
		String plural = coins == 1 ? "" : "s";
		((TextView)findViewById(R.id.shopTotalCoins)).setText("Item Shop (you have " + coins + " coin" + plural + ")");
	}
}
