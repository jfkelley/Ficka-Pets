package com.game.fickapets;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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