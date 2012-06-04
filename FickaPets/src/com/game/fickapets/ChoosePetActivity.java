package com.game.fickapets;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class ChoosePetActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.choose_pet);
	}
	
	public void choosePet(View view) {
		String tag = (String)view.getTag();
		int type = Integer.parseInt(tag);
		Pet pet = PersistenceHandler.buildNewPet(this, type);
		PersistenceHandler.saveState(this, pet);
		finish();
	}

}
