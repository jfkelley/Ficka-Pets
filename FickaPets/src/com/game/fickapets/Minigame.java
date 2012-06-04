package com.game.fickapets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

public abstract class Minigame extends Activity {
	public static final String[] MINIGAME_NAMES = new String[]{"Coin Catch", "Tic-Tac-Toe"};
	public static final Class<? extends Minigame>[] MINIGAME_CLASSES = new Class[]{CatchCoins.class, TicTacToe.class};
	
	private static final String STATE_FILE = "minigamesFile";
	private static final int TRUE = 1;
	private static final int FALSE = 0;
	
	private static final String PLAYED_SUFFIX = "_hasPlayed";
	
	public abstract String getName();
	public abstract String getInstructions();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (hasPlayedBefore()) {
			showInstructions();
			setPlayedBefore(true);
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
					public void onClick(DialogInterface dialog, int which) { }
				})
				.create();
		} else {
			return null;
		}
	}
}
