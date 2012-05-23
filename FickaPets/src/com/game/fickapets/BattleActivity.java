package com.game.fickapets;


import java.io.IOException;
import java.util.Map;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public class BattleActivity extends Activity {
	/* successful attacks to win if opponent has equal strength */
	private static final double MOVES_TO_WIN = 4;
	private static final int ATTACK_DIALOG = 0;
	public static final String OPPONENT_NAME_KEY = "opponentName";
	public static final String OPPONENT_ID_KEY = "opponentId";
	public static final String MY_ID_KEY = "myId";
	public static final String BATTLE_ID_KEY = "battleIdKey";
	public static final String MY_MOVE_KEY = "myMoveKey";

	/* magic is 1, water attack is 2, fire attack is 3.  1 beats 2, 2 beats 3, and 3 beats 1 */
	
	private String opponentId;
	private String opponentName;
	private String myId;
	private String battleId;
	private FickaServer server;
	private Integer myMove;
	private Double myStartingStrength;
	private Double opponentStartingStrength;
	private boolean gameOver = false;
	private PollOpponentMove pollOpponentMove;
	
	 
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.battle);
    	server = new FickaServer(this);
    	Bundle extras = getIntent().getExtras();
    	opponentId = extras.getString(OPPONENT_ID_KEY);
    	opponentName = extras.getString(OPPONENT_NAME_KEY);
    	myId = extras.getString(MY_ID_KEY);
    	String bid = extras.getString(BATTLE_ID_KEY);
    	String myMove = extras.getString(MY_MOVE_KEY);
    	
    	/* check that bid and myMove aren't empty or null since they're null from FindFriendsActivity
    	 * and myMove could be empty if we didn't make a move before leaving battle last time
    	 */
    	if (myMove != null && !myMove.equals("")) {
    		this.myMove = Integer.valueOf(myMove);
    		/* already made a move so view is invisible */
    		findViewById(R.id.fightButton).setVisibility(View.INVISIBLE);
    	}
    	if (bid != null && !bid.equals("")) {
    		battleId = bid;
    	} else {
    		new CreateGameTask().execute();
    	}
    }
    
    /* here, we serialize battle into json and write it out to file */
    public void onDestroy() {
    	super.onDestroy();
    	if (pollOpponentMove != null) pollOpponentMove.cancel(true);
    	if (!gameOver && battleId != null) {
    		String myMove = this.myMove == null ? "" : this.myMove.toString();
    		PersistenceHandler.saveBattle(this, battleId, opponentName, myMove, myId, opponentId);
    	}
    }
   
    
    /* returns the damage done to victim.  The victim's starting strength is treated as life and after
     * attacker wins a move, the victim's life goes down by the damage returned here.  If this returns 50 and
     * victim's starting strength is 100, then victim loses half his life. If both opponents start with the same
     * strength, then it takes MOVES_TO_WIN victories to win a battle
     */
    private double getDamageOnAttack(double attackerStrength) {
    	return attackerStrength / MOVES_TO_WIN;
    }
    

    
    public void onFightPressed(View v) {
    	if (v.getVisibility() == View.VISIBLE) {
        	showDialog(ATTACK_DIALOG);
    	}
    }
    
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
    	case ATTACK_DIALOG:
    		final String[] moves = new String[3];
    		/* I can't remember what moves we wanted to do */
    		moves[0] = "Magic";
    		moves[1] = "Water Attack";
    		moves[2] = "Fire Attack";
    		AlertDialog attackDialog = new AlertDialog.Builder(this)
				.setTitle("Pick a move")
				.setItems(moves, new OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						myMove = item;
						/* checking again in case of double click on this dialog */
						if (findViewById(R.id.fightButton).getVisibility() == View.VISIBLE) { 
							new SendMoveTask().execute();
							pollOpponentMove = new PollOpponentMove();
							pollOpponentMove.execute();
							findViewById(R.id.fightButton).setVisibility(View.INVISIBLE);
						}
					}
				})
				.create();
    		return attackDialog;
		default:
			return null;
    	}
    }
    
    
    
    /* returns true if myMove wins and false if opponentMove wins */
    private boolean getWinner(Integer myMove, Integer opponentMove) {
    	assert (myMove == 1 || myMove == 2 || myMove == 3);
    	assert (opponentMove == 1 || opponentMove == 2 || opponentMove == 3);
    	//1 beats 2. 2 beats 3.  3 beats 1
    	int diff = myMove - opponentMove;
    	if (diff == 1 || diff == -2) return false;
    	return true;
    }
    
    int getBattleHealthDrop(double damage, Double startingStrength) {
    	double drop = (damage / startingStrength) * 100.0;
    	return (int) Math.ceil(drop);
    }
    
    private void gameOver(boolean iWin) {
    	gameOver = true;
    	if (iWin) {
    		Toast.makeText(this, "You win!", Toast.LENGTH_SHORT).show();
    		/* adjust strength or whatever we do to reward victories here */
    	} else {
    		Toast.makeText(this, opponentName + " has beaten you", Toast.LENGTH_SHORT);
    	}
    	new Thread(new Runnable() {
    		public void run() {
    			try {
    				server.closeBattle(battleId, myId);
    			} catch(IOException ex) {
    				System.out.println("failed to close battle");
    				ex.printStackTrace();
    			}
    			runOnUiThread(new Runnable() {
    				public void run() {
    					finish();
    				}
    			});
    		}
    	}).run();
    }
    
    private void animateMove(boolean iWonMove, Integer myMove, Integer opponentMove, double damage) {
    	int battleHealthDrop;
    	int battleHealth;
    	if (!iWonMove) {
    		battleHealthDrop = getBattleHealthDrop(damage, myStartingStrength);
        	battleHealth = ((ProgressBar) findViewById(R.id.myBattleHealth)).getProgress();
    	} else {
    		battleHealthDrop = getBattleHealthDrop(damage, opponentStartingStrength);
        	battleHealth = ((ProgressBar) findViewById(R.id.opponentBattleHealth)).getProgress();
    	}
    	int newBattleHealth;
    	if (battleHealth - battleHealthDrop < 0) {
    		newBattleHealth = 0;
    	} else {
    		newBattleHealth = (battleHealth - battleHealthDrop);
    	}
    	if (!iWonMove) {
    		((ProgressBar) findViewById(R.id.myBattleHealth)).setProgress(newBattleHealth);
    	} else {
    		((ProgressBar) findViewById(R.id.opponentBattleHealth)).setProgress(newBattleHealth);
    	}
    	if (newBattleHealth <= 0) {
    		gameOver(iWonMove);
    	}
    }
    
    
    private void playMove(Integer opponentMove, Double opponentStrength) {
    	assert myMove != null;
    	boolean iWin = getWinner(myMove, opponentMove);
    	/* damage is proportional to victim's starting strength.  If damage is half victim's starting strength, 
    	 * then victim loses half his life
    	 */
    	double damageToLoser;
    	if (iWin) {
    		/* attacker strength determines damage done to victim */
    		damageToLoser = getDamageOnAttack(myStartingStrength);
    	} else {
    		damageToLoser = getDamageOnAttack(opponentStartingStrength);
    	}
    	animateMove(iWin, myMove, opponentMove, damageToLoser);
    	myMove = null;
    	findViewById(R.id.battleButton).setVisibility(View.VISIBLE);
    }
    
    /* Background asynctasks for talking to server -------------------------------------------------------------------------------------*/
    
    
	private boolean battleCreated() {
		if (battleId != null) return true;
		return false;
	}
	
	private void waitUntilBattleCreated() throws InterruptedException {
		while (!battleCreated()) {
			Thread.sleep(2000);
		}
	}
    
    /* Should be called after we've made a move and need to get the opponent's move to continue.  Polls
     * the server for opponent's move every SECONDS_BETWEEN_POLL.
     */
    private class PollOpponentMove extends AsyncTask<Void, Void, String[]> {
    	private static final int SECONDS_BETWEEN_POLL = 3;

		@Override
		protected String[] doInBackground(Void... params) {
			try {
				/* possible to make move before server returned with battle id */
				waitUntilBattleCreated();
				Map<String, String> battleMap = server.getBattleData(myId, battleId);
				while (battleMap.get(FickaServer.OPP_MOVE_KEY).equals("null")) {
					if (isCancelled()) return null;
					Thread.sleep(SECONDS_BETWEEN_POLL * 1000);
					battleMap = server.getBattleData(myId, battleId);
				}
				String[] result = new String[2];
				result[0] = battleMap.get(FickaServer.OPP_MOVE_KEY);
				result[1] = battleMap.get(FickaServer.OPP_STRENGTH_KEY);
				return result;
			} catch(Exception ex) {
				System.out.println("Couldn't get battle data");
				ex.printStackTrace();
				return null;
			}
		}
		
		protected void onPostExecute(String[] data) {
			if (data == null) return;
			String opponentMove = data[0];
			String opponentStrength = data[1];
			
    		playMove(Integer.valueOf(opponentMove), Double.valueOf(opponentStrength));
    		/* Not sure what to do for multiple moves - need to send something to server here */
		}
    }
    /* creates a new game */
    private class CreateGameTask extends AsyncTask<Void, Void, String> {
    	protected String doInBackground(Void...voids) {
    		String bid = null;
    		try {
    			bid = server.createGame(myId, opponentId);
    		} catch(IOException ex) {
    			System.out.println("failed to create game on server");
    			ex.printStackTrace();
    		}
    		return bid;
    	}
    	protected void onPostExecute(String bid) {
    		myStartingStrength = Pet.thePet(BattleActivity.this).getAttributes().strength;
    		battleId = bid;
    	}
    }
    /* sends move */
    private class SendMoveTask extends AsyncTask<Void, Void, Void> {
    	protected Void doInBackground(Void...voids) {
    		try {
    			//String myMove = strings[0];
    			//String myId = strings[1];
    			//String battleId = strings[2];
    			waitUntilBattleCreated();
    			server.sendMove(myMove.toString(), myId, battleId, myStartingStrength.toString());
    		} catch(Exception ex) {
    			System.out.println("failed to send move");
    			ex.printStackTrace();
    		}
			return null;
    	}
    }
    
}