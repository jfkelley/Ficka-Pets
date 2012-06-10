package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

import android.os.AsyncTask;

public class MainThread extends AsyncTask<Pet, String, Void> {
	FickaPetsStart ficka;
	
	public MainThread (FickaPetsStart ficka) {
		this.ficka = ficka;
	}

	/* Runs in the main UI thread.  AsyncTask calls this when doInBackground calls publishProgress.
	 * Also sends the data passed to publishProgress
	 */
	protected void onProgressUpdate(String ... strings) {
		System.out.println("Strength: " + strings[0]);
		System.out.println("Health: " + strings[1]);
		System.out.println("Hunger: " + strings[2]);
		System.out.println("Tiredness: " + strings[3]);
		
		/*
    	TextView strength = (TextView) ficka.findViewById(R.id.strengthEditable);
    	strength.setText(strings[0]);
    	
    	TextView health = (TextView) ficka.findViewById(R.id.healthEditable);
    	health.setText(strings[1]);
    	
    	TextView hunger = (TextView) ficka.findViewById(R.id.hungerEditable);
    	hunger.setText(strings[2]);
    	
    	TextView tiredness = (TextView) ficka.findViewById(R.id.tiredEditable);
    	tiredness.setText(strings[3]);
    	*/
    	
    	Calendar cal = Calendar.getInstance(TimeZone.getDefault());
    	Integer startHour = cal.get(Calendar.HOUR_OF_DAY);
    	Integer startMinute = cal.get(Calendar.MINUTE);
    	Integer second = cal.get(Calendar.SECOND);
    	String currentTime = "H: " + startHour.toString() + "  M: " + startMinute.toString() + "  S: " + second.toString();
    	
    	System.out.println("Current time: " + currentTime);
    	
    	/*
    	TextView now = (TextView) ficka.findViewById(R.id.endTime);
    	now.setText(currentTime);
    	*/

	}
	/* 
	 * This runs in a separate thread. When publishProgress is called, AsyncTask sends the parameter
	 * to onProgressUpdate.  onProgressUpdate runs in the main UI thread so it can update the UI.
	 */
	@Override
	protected Void doInBackground(Pet ... pets) {
        String[] strings = new String[4];

        
	    while (!isCancelled()) {
	        //Vibrator v = (Vibrator) ficka.getSystemService (Context.VIBRATOR_SERVICE);
	        //v.vibrate (1000);
	    	Attributes atts = pets[0].getAttributes ();
	        	
	    	strings[0] = new Double(atts.strength).toString();
	    	
        
	    	strings[1] = new Double(atts.health).toString();
	    	strings[2] = new Double(atts.hunger).toString();
	    	strings[3] = new Double(atts.tiredness).toString();
	    	publishProgress(strings);
	    	try {
	    		
	    		Thread.sleep(30000);
	    	} catch (Exception ex) {}
	    }
	    return null;
	}

}
