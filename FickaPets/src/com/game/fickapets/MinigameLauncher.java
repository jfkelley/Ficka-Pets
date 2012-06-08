package com.game.fickapets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

public class MinigameLauncher extends Activity {
	public static final String[] MINIGAME_NAMES = new String[]{CatchCoins.NAME, TicTacToe.NAME};
	public static final String[] MINIGAME_INSTRUCTIONS = new String[]{CatchCoins.INSTRUCTIONS, TicTacToe.INSTRUCTIONS};
	public static final Class<? extends Activity>[] MINIGAME_CLASSES = new Class[]{CatchCoins.class, TicTacToe.class};
	public static final String INTENT_PARAM = "minigame";
	
	private static final String STATE_FILE = "minigamesFile";
	private static final int TRUE = 1;
	private static final int FALSE = 0;
	
	private static final String PLAYED_SUFFIX = "_hasPlayed";
	
	private boolean sentToGame = false;
	
	@Override
	public void onResume() {
		super.onResume();
		if (sentToGame) {
			finish();
		} else {
			if (!hasPlayedBefore()) {
				setPlayedBefore(true);
				showInstructions();
			} else {
				goToGame();
			}
		}
	}
	
	private boolean hasPlayedBefore() {
		SharedPreferences gameState = getSharedPreferences(STATE_FILE, 0);
		return gameState.getInt(getName() + PLAYED_SUFFIX, FALSE) == TRUE;
	}
	
	private void setPlayedBefore(boolean played) {
		SharedPreferences gameState = getSharedPreferences(STATE_FILE, 0);
		Editor editor = gameState.edit();
		editor.putInt(getName() + PLAYED_SUFFIX, played ? TRUE : FALSE);
		editor.commit();
	}
	
	private static final int INSTRUCTIONS_DIALOG = 0xABCDE; // doesn't matter
	
	private void showInstructions() {
		showDialog(INSTRUCTIONS_DIALOG);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == INSTRUCTIONS_DIALOG) {
			return new AlertDialog.Builder(this)
				.setMessage(getInstructions())
				.setCancelable(true)
				.setTitle(getName() + " Instructions")
				.setNeutralButton("OK", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						goToGame();
					}
				})
				.create();
		} else {
			return null;
		}
	}
	
	private void goToGame() {
		sentToGame = true;
		int index = getIntent().getIntExtra(INTENT_PARAM, -1);
		Intent intent = new Intent(this, MINIGAME_CLASSES[index]);
		startActivity(intent);
	}
	
	private String getName() {
		int index = getIntent().getIntExtra(INTENT_PARAM, -1);
		return MINIGAME_NAMES[index];
	}
	
	private String getInstructions() {
		int index = getIntent().getIntExtra(INTENT_PARAM, -1);
		return MINIGAME_INSTRUCTIONS[index];
	}
}
