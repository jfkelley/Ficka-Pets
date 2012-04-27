package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FickaPetsStart extends Activity {
	Pet pet;
	
	public Pet getPet () {
		return pet;
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        pet = new Pet ();
        
        
        
        
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        Integer startHour = cal.get(Calendar.HOUR_OF_DAY);
        Integer startMinute = cal.get(Calendar.MINUTE);
        Integer second = cal.get(Calendar.SECOND);
        String startTime = "H: " + startHour.toString() + "  M: " + startMinute.toString() + "  S: " + second.toString();
        
        TextView start = (TextView) findViewById (R.id.startTime);
        start.setText(startTime);
       
        new MainThread(this).execute(pet);
    }
    
    public void feedPressed (View view) {
    	pet.feed();
    }
    
    public void sleepPressed (View view) {
        Button sleepButton = (Button) findViewById (R.id.sleepButton);
        if (pet.isSleeping ()) {
        	sleepButton.setText (R.string.awake);
        	pet.wakeUp ();
        } else {
        	sleepButton.setText (R.string.sleep);
        	pet.putToSleep ();
        }
    }
}