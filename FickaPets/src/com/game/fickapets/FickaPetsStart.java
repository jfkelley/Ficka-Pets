package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FickaPetsStart extends Activity {
	Pet pet;
	
	AsyncTask<Pet, String, Void> updateLoop;
	
	public Pet getPet () {
		return pet;
	}
	


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
    }
    /* always called when activity leaves foreground so set up background service here */
    public void onPause () {
    	super.onPause();
    	Vector<Complaint> complaints = pet.getComplaints(this);
    	Intent notificationService = new Intent(this, Notifier.class);
    	notificationService.putExtra("com.game.fickapets.complaints", complaints);
    	startService(notificationService);
    }
    
    /* looks like onResume is always called when activity comes to foreground, so
     * kill background service here if it's running
     */
    public void onResume () {
    	super.onResume();
    	//kill background service if it's still running
    	Intent notificationService = new Intent(this, Notifier.class);
    	if (!stopService(notificationService)) {
    		System.out.println ("No service was running or we failed to stop it");
    	}
    }
    
    /* Always called when activity gets destroyed, so save pet's state here */
    public void onDestroy () {
    	super.onDestroy();
    	PersistenceHandler.saveState (this, pet);
    }
    

    public void feedPressed (View view) {
    	pet.feed();
    }
    
    
    public void sleepPressed (View view) {
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
    	pet = PersistenceHandler.reset (this);
    	setSleepButton (pet);
    	updateLoop = new MainThread(this).execute(pet);
    }
}