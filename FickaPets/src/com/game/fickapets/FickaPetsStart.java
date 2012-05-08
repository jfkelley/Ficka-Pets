package com.game.fickapets;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FickaPetsStart extends Activity {

	AsyncTask<Pet, String, Void> updateLoop;


	private void setSleepButton (Pet pet) {
		Button sleepButton = (Button) findViewById(R.id.sleepButton);
		if (pet.isSleeping()) {
			sleepButton.setText(R.string.awake);
		} else {
			sleepButton.setText(R.string.sleep);
		}
	}

	private void initLayout (Pet pet) {
		setContentView(R.layout.main);
		setSleepButton (pet);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// should probably also kill background service if it's still running
		super.onCreate(savedInstanceState);
		User.theUser(this);
		initLayout (Pet.thePet(this));


		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		Integer startHour = cal.get(Calendar.HOUR_OF_DAY);
		Integer startMinute = cal.get(Calendar.MINUTE);
		Integer second = cal.get(Calendar.SECOND);
		String startTime = "H: " + startHour.toString() + "  M: " + startMinute.toString() + "  S: " + second.toString();

		TextView start = (TextView) findViewById (R.id.startTime);
		start.setText(startTime);

		updateLoop = new MainThread(this).execute(Pet.thePet(this));
	}
	/* always called when activity leaves foreground so set up background service here */
	public void onPause () {
		super.onPause();
		Vector<Complaint> complaints = Pet.thePet(this).getComplaints(this);
		Intent notificationService = new Intent(this, Notifier.class);
		notificationService.putExtra("com.game.fickapets.complaints", complaints);
		startService(notificationService);
		PersistenceHandler.saveState(this, User.theUser(this));
	}

	/* looks like onResume is always called when activity comes to foreground, so
	 * kill background service here if it's running
	 */
	public void onResume () {
		System.out.println("resumed start");
		super.onResume();
		//kill background service if it's still running
		Intent notificationService = new Intent(this, Notifier.class);
		if (!stopService(notificationService)) {
			System.out.println ("No service was running or we failed to stop it");
		}
	}

	/* Always called when activity gets destroyed, so save pet's state here */
	public void onDestroy () {
		System.out.println("destroy start");
		super.onDestroy();
	}


	public void sleepPressed (View view) {
		Pet pet = Pet.thePet(this);
		Button sleepButton = (Button) findViewById (R.id.sleepButton);
		if (pet.isSleeping ()) {
			sleepButton.setText (R.string.sleep);
			pet.wakeUp ();
		} else {
			sleepButton.setText (R.string.awake);
			pet.putToSleep ();
		}
	}

	public void resetPressed (View view) {
		while (!updateLoop.cancel(true));
		Pet.reset(this);
		setSleepButton (Pet.thePet(this));
		updateLoop = new MainThread(this).execute(Pet.thePet(this));
		User.reset(this);
	}

	public void shopPressed(View view) {
		Intent intent = new Intent(FickaPetsStart.this, ItemShop.class);
		startActivity(intent);
	}

	private static final int PICK_FOOD = 0;
	private static final int NO_FOOD = 1;
	
	public void feedPressed (View view) {
		if (User.theUser(this).getUniqueFood().isEmpty()) {
			showDialog(NO_FOOD);
		} else {
			showDialog(PICK_FOOD);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case PICK_FOOD:
			final List<Food> foodInInventory = User.theUser(this).getUniqueFood();
			String[] items = new String[foodInInventory.size()];
			for (int i = 0; i < items.length; i++) {
				items[i] = foodInInventory.get(i).getName();
			}

			AlertDialog pickFoodAlert = new AlertDialog.Builder(this)
				.setTitle("Pick a food")
				.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						User.theUser(FickaPetsStart.this).feedPet(Pet.thePet(FickaPetsStart.this), foodInInventory.get(item));
					}
				})
				.create();
			return pickFoodAlert;
		case NO_FOOD:
			AlertDialog noFoodAlert = new AlertDialog.Builder(this)
				.setMessage("You don't have any food! Please buy some.")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) { }
				})
				.create();
			return noFoodAlert;
		default:
			return null;
		}
	}
}