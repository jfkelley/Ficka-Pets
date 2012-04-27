package com.game.fickapets;

import android.os.AsyncTask;
import android.widget.TextView;

public class MainThread extends AsyncTask<Pet, String, Void> {
	FickaPetsStart ficka;
	
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

	}
	
	@Override
	protected Void doInBackground(Pet ... pets) {
        String[] strings = new String[4];

	    while (true) {
	        	
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
