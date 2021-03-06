package com.game.fickapets;

import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class FickaPetsStart extends Activity {

	AsyncTask<Pet, String, Void> updateLoop;
	AsyncTask<Void, Boolean, Void> blinker;
	
	private static final int NO_FOOD = 0;
	
	private static final int MINIGAME = 1;
	
	private void setSleepButton (Pet pet) {
		ImageView sleepButton = (ImageView) findViewById(R.id.sleepButton);
		if (pet.isSleeping()) {
			sleepButton.setImageResource(R.drawable.wake_button);
		} else {
			sleepButton.setImageResource(R.drawable.sleep_button);
		}
	}

	private void initLayout (Pet pet) {
		setContentView(R.layout.main);
		setSleepButton (pet);
		setPetStateImage(pet);
	}
	
    private void setPetStateImage(final Pet pet) {
    	// Use an AsyncTask in case we get called from a different thread.
    	AsyncTask<Object, Object, Integer> task = new AsyncTask<Object, Object, Integer>(){
			protected Integer doInBackground(Object... params) {
				return getPetStateImageId(pet);
			}
			protected void onPostExecute(Integer result) {
				setPetImg(result);
			}
    	};
    	task.execute();
	}
    
    private void setPetImg(Integer imgId) {
    	ImageView image = (ImageView) findViewById(R.id.petImageView);
		image.setImageResource(imgId);
    }
    
    

	/** Called when the activity is first created. */
    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pet = PersistenceHandler.buildPet(this);
        initLayout (pet);
        
        
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        Integer startHour = cal.get(Calendar.HOUR_OF_DAY);
        Integer startMinute = cal.get(Calendar.MINUTE);
        Integer second = cal.get(Calendar.SECOND);
        String startTime = "H: " + startHour.toString() + "  M: " + startMinute.toString() + "  S: " + second.toString();
        
        TextView start = (TextView) findViewById (R.id.startTime);
        start.setText(startTime);
       
        updateLoop = new MainThread(this).execute(pet);
    } */

	private int getPetStateImageId(Pet pet) {
		return pet.getStateImage(this);
	}
	
	public void startChallengeNotificationService() {
		Intent battleNotifier = new Intent(this, BattleNotifier.class);
		startService(battleNotifier);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		User.theUser(this);
		
		initLayout (Pet.thePet(this));
		
		startChallengeNotificationService();

		Pet.thePet(this).addListener(new PetListener(){
			public void petChanged() {
				setPetStateImage(Pet.thePet(FickaPetsStart.this));
			}
		});
		blinker = new BlinkTask().execute();
		updateLoop = new MainThread(this).execute(Pet.thePet(this));
	}
	
	private class BlinkTask extends AsyncTask<Void, Boolean, Void> {
		private static final long blinkDuration = 250;
		
		private long getLongBetweenBounds(Random randoGenerator, long lowerBound, long upperBound) {
			float range = upperBound - lowerBound;
			int distInRange = (int)(randoGenerator.nextFloat() * range);
			long retVal = lowerBound + distInRange;
			return retVal;
		}
		
		protected Void doInBackground(Void...voids) {
			Random rando = new Random();
			while (!isCancelled()) {
				long millisBetweenBlink = getLongBetweenBounds(rando, 7, 20) * 1000;
				try {
					Thread.sleep(millisBetweenBlink);
				} catch(InterruptedException ex) {}
				publishProgress(true);
				try {
					Thread.sleep(blinkDuration);
				} catch(InterruptedException ex) {}
				publishProgress(false);
			}
			return null;
		}
		protected void onProgressUpdate(Boolean...bools) {
			Boolean setBlinkImg = bools[0];
			if (setBlinkImg) {
				setPetImg(Pet.thePet(FickaPetsStart.this).getBlinkImg());
			} else {
				setPetStateImage(Pet.thePet(FickaPetsStart.this));
			}
		}
		
	}


	/* always called when activity leaves foreground so set up background service here */
	public void onPause () {
		super.onPause();
		//Vector<Complaint> complaints = Pet.thePet(this).getComplaints(this);
		Intent notificationService = new Intent(this, PetNotifier.class);
		//notificationService.putExtra("com.game.fickapets.complaints", complaints);
		startService(notificationService);
		PersistenceHandler.saveState(this, User.theUser(this));
	}

	/* looks like onResume is always called when activity comes to foreground, so
	 * kill background service here if it's running
	 */
	public void onResume () {
		System.out.println("resumed start");
		super.onResume();
		//kill health notification service if it's still running
		Intent notificationService = new Intent(this, PetNotifier.class);
		if (!stopService(notificationService)) {
			System.out.println ("No service was running or we failed to stop it");
		}
	}

	@Override
	public void onDestroy () {
		super.onDestroy();
		blinker.cancel(true);
		updateLoop.cancel(true);
	}
	
	public void gamePressed(View view) {
		showDialog(MINIGAME);
	}
    
    
	public void feedPressed (View view) {
		if (User.theUser(this).getUniqueFood().isEmpty()) {
			showDialog(NO_FOOD);
		} else {
			// Create and manually display the menu each time, instead of going through showDialog/onCreateDialog
			// This fixes the problem with android showing the same food each time
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
			pickFoodAlert.show();
		}
	}
	
	public void sleepPressed (View view) {
		Pet pet = Pet.thePet(this);
		ImageView sleepButton = (ImageView) findViewById (R.id.sleepButton);
		if (pet.isSleeping ()) {
			sleepButton.setImageResource (R.drawable.sleep_button);
			pet.wakeUp ();
		} else {
			sleepButton.setImageResource (R.drawable.wake_button);
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
	
	public void battlePressed(View view) {
		Intent intent = new Intent(FickaPetsStart.this, BattleListActivity.class);
		startActivity(intent);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case NO_FOOD:
			AlertDialog noFoodAlert = new AlertDialog.Builder(this)
				.setMessage("You don't have any food! Please buy some.")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) { }
				})
				.create();
			return noFoodAlert;
		case MINIGAME:
			AlertDialog pickGameAlert = new AlertDialog.Builder(this)
				.setTitle("Pick a game")
				.setItems(MinigameLauncher.MINIGAME_NAMES, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(FickaPetsStart.this, MinigameLauncher.class);
						intent.putExtra(MinigameLauncher.INTENT_PARAM, which);
						startActivity(intent);
					}
				})
				.create();
			return pickGameAlert;
		default:
			return null;
		}
	}
}
