package com.game.fickapets;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


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
	


	/* magic is 1, water attack is 2, fire attack is 3.  1 beats 2, 2 beats 3, and 3 beats 1 */
	
	/* should never change throughout game */
	/*private String opponentId;
	private String opponentName;
	private String myId;
	private String battleId;
	private Double myStartingStrength;
	private Double opponentStartingStrength;*/
	BattleState bState;
	
	private PollOpponentMove pollOpponentMove;
	private FickaServer server;
	
	/* change as game progresses */
	/*private Integer myMove;
	private Integer opponentMove;
	private Integer myBattleHealth;
	private Integer opponentBattleHealth;
	private Integer numMovesPlayed; */
	private boolean gameOver = false;
	
	
	 
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.battle);
    	server = new FickaServer(this);
    	Bundle extras = getIntent().getExtras();
    	bState = new BattleState(this, extras);
    	setProgressBars(bState.myHealth, bState.opponentHealth);
    	if (bState.myMove != null) {
    		/* already made a move so view is invisible */
    		findViewById(R.id.fightButton).setVisibility(View.INVISIBLE);
    	}
    	if (bState.bid == null) {
    		new CreateGameTask().execute();
    	}
    }
    
    private void setProgressBars(Integer myBattleHealth, Integer opponentBattleHealth) {
    	((ProgressBar)findViewById(R.id.myBattleHealth)).setProgress(myBattleHealth);
    	((ProgressBar)findViewById(R.id.opponentBattleHealth)).setProgress(opponentBattleHealth);
    }
    
    /* here, we serialize battle into json and write it out to file */
    public void onDestroy() {
    	super.onDestroy();
    	if (pollOpponentMove != null) pollOpponentMove.cancel(true);
    	if (!gameOver && bState.bid != null) {
    		PersistenceHandler.saveBattle(this, bState.toJSON());
    		/* if it's game over, erase battle data if it exists */
    	} else if (gameOver) {
    		PersistenceHandler.removeBattle(this, bState.bid);
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
						bState.myMove = item;
						/* checking again in case of double click on this dialog */
						if (findViewById(R.id.fightButton).getVisibility() == View.VISIBLE) { 
							new SendMoveTask().execute(bState.numMovesPlayed);
							if (bState.opponentMove != null) {
								pollOpponentMove = new PollOpponentMove();
								pollOpponentMove.execute();
							}
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
    		Toast.makeText(this, "You win!", Toast.LENGTH_LONG).show();
    		/* adjust strength or whatever we do to reward victories here */
    	} else {
    		Toast.makeText(this, bState.opponentName + " has beaten you", Toast.LENGTH_LONG);
    	}
    	new Thread(new Runnable() {
    		public void run() {
    			try {
    				server.closeBattle(bState.bid, bState.myId);
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
    
    /* returns the loser's new battle health having lost this move */
    private int getLoserBattleHealth(boolean iWonMove, double damage) {
    	int battleHealth;
    	int battleHealthDrop;
    	if (!iWonMove) {
    		battleHealthDrop = getBattleHealthDrop(damage, Double.valueOf(bState.myStartingStrength));
        	battleHealth = ((ProgressBar) findViewById(R.id.myBattleHealth)).getProgress();
    	} else {
    		battleHealthDrop = getBattleHealthDrop(damage, Double.valueOf(bState.opponentStartingStrength));
        	battleHealth = ((ProgressBar) findViewById(R.id.opponentBattleHealth)).getProgress();
    	}
    	int resultingHealth = battleHealth - battleHealthDrop;
    	if (resultingHealth < 0) resultingHealth = 0;
    	return resultingHealth;
    }
    
    private void updateProgressBar(boolean iWonMove, int newBattleHealth) {
    	if (!iWonMove) {
    		((ProgressBar) findViewById(R.id.myBattleHealth)).setProgress(newBattleHealth);
    		bState.myHealth = newBattleHealth;
    	} else {
    		((ProgressBar) findViewById(R.id.opponentBattleHealth)).setProgress(newBattleHealth);
    		bState.myHealth = newBattleHealth;
    	}
    }
    
    private void animateMove(boolean iWonMove, Integer myMove, Integer opponentMove, double damage) {
    	int battleHealth = getLoserBattleHealth(iWonMove, damage);
    	updateProgressBar(iWonMove, battleHealth);
    	if (battleHealth == 0) {
    		gameOver(iWonMove);
    	}
    }
    
    
    private void playMove(Integer opponentMove, Double opponentStrength) {
    	assert bState.myMove != null;
    	boolean iWin = getWinner(bState.myMove, opponentMove);
    	/* damage is proportional to victim's starting strength.  If damage is half victim's starting strength, 
    	 * then victim loses half his life
    	 */
    	double damageToLoser;
    	if (iWin) {
    		/* attacker strength determines damage done to victim */
    		damageToLoser = getDamageOnAttack(Double.valueOf(bState.myStartingStrength));
    	} else {
    		damageToLoser = getDamageOnAttack(Double.valueOf(bState.opponentStartingStrength));
    	}
    	animateMove(iWin, bState.myMove, opponentMove, damageToLoser);
    	bState.myMove = null;
    	findViewById(R.id.battleButton).setVisibility(View.VISIBLE);
    }
    
    /* Background asynctasks for talking to server -------------------------------------------------------------------------------------*/
    

    
	private boolean battleCreated() {
		if (bState.bid == null) {
			return false;
		}
		return true;
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
				Map<String, String> battleMap = server.getBattleData(bState.myId, bState.bid);
				while (battleMap.get(FickaServer.OPP_MOVE_KEY).equals("null")) {
					if (isCancelled()) return null;
					Thread.sleep(SECONDS_BETWEEN_POLL * 1000);
					battleMap = server.getBattleData(bState.myId, bState.bid);
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
		/* oppMove should never be null when we get here */
		private Integer getOpponentMove(String encodedMoves) {
			String[] moves = encodedMoves.split(" ");
			if (moves.length > bState.numMovesPlayed) {
				return Integer.valueOf(moves[bState.numMovesPlayed]);
			} else {
				return null;
			}
		}
		
		protected void onPostExecute(String[] data) {
			if (data == null) return;
			Integer opponentMove = getOpponentMove(data[0]);
			String opponentStrength = data[1];
			
    		playMove(opponentMove, Double.valueOf(opponentStrength));
		}
    }
    /* creates a new game */
    private class CreateGameTask extends AsyncTask<Void, Void, String> {
    	protected String doInBackground(Void...voids) {
    		String bid = null;
    		try {
    			bid = server.createGame(bState.myId, bState.opponentId);
    		} catch(IOException ex) {
    			System.out.println("failed to create game on server");
    			ex.printStackTrace();
    		}
    		return bid;
    	}
    	protected void onPostExecute(String bid) {
    		bState.myStartingStrength = Pet.thePet(BattleActivity.this).getAttributes().strength;
    		bState.bid = bid;
    	}
    }
    /* sends move */
    private class SendMoveTask extends AsyncTask<Integer, Void, Void> {
    	protected Void doInBackground(Integer...integers) {
    		try {
    			//String myMove = strings[0];
    			//String myId = strings[1];
    			//String battleId = strings[2];
    			Integer numMovesPlayed = integers[0];
    			waitUntilBattleCreated();
    			server.sendMove(numMovesPlayed.toString() + "_" + bState.myMove.toString(), bState.myId, bState.bid, bState.myStartingStrength.toString());
    		} catch(Exception ex) {
    			System.out.println("failed to send move");
    			ex.printStackTrace();
    		}
			return null;
    	}
    }
}