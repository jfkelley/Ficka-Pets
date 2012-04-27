package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.widget.TextView;

public class MainThread extends AsyncTask<Pet, String, Void> {
	FickaPetsStart ficka;
	Context current;
	
	public MainThread (FickaPetsStart ficka) {
		this.ficka = ficka;
	}

	protected void onProgressUpdate(String ... strings) {
    	TextView strength = (TextView) ficka.findViewById(R.id.strengthEditable);
    	strength.setText(strings[0]);
    	
    	TextView health = (TextView) ficka.findViewById(R.id.healthEditable);
    	health.setText(strings[1]);
    	
    	TextView hunger = (TextView) ficka.findViewById(R.id.hungerEditable);
    	hunger.setText(strings[2]);
    	
    	TextView tiredness = (TextView) ficka.findViewById(R.id.tiredEditable);
    	tiredness.setText(strings[3]);
    	
    	Calendar cal = Calendar.getInstance(TimeZone.getDefault());
    	Integer startHour = cal.get(Calendar.HOUR_OF_DAY);
    	Integer startMinute = cal.get(Calendar.MINUTE);
    	Integer second = cal.get(Calendar.SECOND);
    	String currentTime = "H: " + startHour.toString() + "  M: " + startMinute.toString() + "  S: " + second.toString();
    	TextView now = (TextView) ficka.findViewById(R.id.endTime);
    	now.setText(currentTime);

	}
	
	@Override
	protected Void doInBackground(Pet ... pets) {
        String[] strings = new String[4];

        
	    while (true) {
	        //Vibrator v = (Vibrator) ficka.getSystemService (Context.VIBRATOR_SERVICE);
	        //v.vibrate (1000);
	    	Attributes atts = pets[0].getAttributes ();
	        	
	    	strings[0] = new Double(atts.strength).toString();
	    	
        
	    	strings[1] = new Double(atts.health).toString();
	    	strings[2] = new Double(atts.hunger).toString();
	    	strings[3] = new Double(atts.tiredness).toString();
	    	publishProgress(strings);
	    	try {
	    		Thread.sleep(5000);
	    	} catch (Exception ex) {
	    		System.out.println("it broked.");
	    	}
	    }
	}

}
