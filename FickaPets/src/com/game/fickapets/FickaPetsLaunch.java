package com.game.fickapets;

import android.app.Activity;
import android.content.Intent;

public class FickaPetsLaunch extends Activity {
	boolean sentToChoosePet = false;
	boolean sentToStart = false;
	
	@Override
	public void onResume() {
		super.onResume();
		Pet pet = Pet.thePet(this);
		if (pet == null) {
			sentToChoosePet = true;
			startActivity(new Intent(this, ChoosePetActivity.class));
		} else if (!sentToStart) {
			sentToStart = true;
			startActivity(new Intent(this, FickaPetsStart.class));
		} else {
			finish();
		}
	}

}
