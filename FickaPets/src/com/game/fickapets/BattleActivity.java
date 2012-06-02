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
	public static final String NUM_MOVES_KEY = "numMovesPlayedKey";
	public static final String OPPONENT_HEALTH_KEY = "opponentBattleHealth";
	public static final String MY_HEALTH_KEY = "myBattleHealth";

	/* magic is 1, water attack is 2, fire attack is 3.  1 beats 2, 2 beats 3, and 3 beats 1 */
	
	/* should never change throughout game */
	private String opponentId;
	private String opponentName;
	private String myId;
	private String battleId;
	private Double myStartingStrength;
	private Double opponentStartingStrength;
	
	private PollOpponentMove pollOpponentMove;
	private FickaServer server;
	
	/* change as game progresses */
	private Integer myMove;
	private Integer myBattleHealth;
	private Integer opponentBattleHealth;
	private Integer numMovesPlayed;
	private boolean gameOver = false;
	
	
	 
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.battle);
    	server = new FickaServer(this);
    	Bundle extras = getIntent().getExtras();
    	opponentId = extras.getString(OPPONENT_ID_KEY);
    	opponentName = extras.getString(OPPONENT_NAME_KEY);
    	myId = extras.getString(MY_ID_KEY);
    	/* should default to zero */
    	numMovesPlayed = extras.getInt(NUM_MOVES_KEY);
    	/* should default to 100 */
    	myBattleHealth = extras.getInt(MY_HEALTH_KEY) > 0 ? extras.getInt(MY_HEALTH_KEY) : 100;
    	/* default to 100 */
    	opponentBattleHealth = extras.getInt(OPPONENT_HEALTH_KEY) > 0 ? extras.getInt(OPPONENT_HEALTH_KEY) : 100;
    	setProgressBars(myBattleHealth, opponentBattleHealth);
    	
    	
    	/* check that bid and myMove aren't empty or null since they're null from FindFriendsActivity
    	 * and myMove could be empty if we didn't make a move before leaving battle last time
    	 */
    	String bid = extras.getString(BATTLE_ID_KEY);
    	String myMove = extras.getString(MY_MOVE_KEY);
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
    
    private void setProgressBars(Integer myBattleHealth, Integer opponentBattleHealth) {
    	((ProgressBar)findViewById(R.id.myBattleHealth)).setProgress(myBattleHealth);
    	((ProgressBar)findViewById(R.id.opponentBattleHealth)).setProgress(opponentBattleHealth);
    }
    
    /* here, we serialize battle into json and write it out to file */
    public void onDestroy() {
    	super.onDestroy();
    	if (pollOpponentMove != null) pollOpponentMove.cancel(true);
    	if (!gameOver && battleId != null) {
    		String myMove = this.myMove == null ? "" : this.myMove.toString();
    		Bundle bundle = new Bundle();
    		bundle.putString(BATTLE_ID_KEY, battleId);
    		bundle.putString(OPPONENT_NAME_KEY, opponentName);
    		bundle.putString(MY_MOVE_KEY, myMove);
    		bundle.putString(MY_ID_KEY, myId);
    		bundle.putString(OPPONENT_ID_KEY, opponentId);
    		bundle.putInt(MY_HEALTH_KEY, myBattleHealth);
    		bundle.putInt(OPPONENT_HEALTH_KEY, opponentBattleHealth);
    		PersistenceHandler.saveBattle(this, bundle);
    		/* if it's game over, erase battle data if it exists */
    	} else if (gameOver) {
    		PersistenceHandler.removeBattle(this, battleId);
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
							new SendMoveTask().execute(numMovesPlayed);
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
    		Toast.makeText(this, "You win!", Toast.LENGTH_LONG).show();
    		/* adjust strength or whatever we do to reward victories here */
    	} else {
    		Toast.makeText(this, opponentName + " has beaten you", Toast.LENGTH_LONG);
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
    
    /* returns the loser's new battle health having lost this move */
    private int getLoserBattleHealth(boolean iWonMove, double damage) {
    	int battleHealth;
    	int battleHealthDrop;
    	if (!iWonMove) {
    		battleHealthDrop = getBattleHealthDrop(damage, myStartingStrength);
        	battleHealth = ((ProgressBar) findViewById(R.id.myBattleHealth)).getProgress();
    	} else {
    		battleHealthDrop = getBattleHealthDrop(damage, opponentStartingStrength);
        	battleHealth = ((ProgressBar) findViewById(R.id.opponentBattleHealth)).getProgress();
    	}
    	int resultingHealth = battleHealth - battleHealthDrop;
    	if (resultingHealth < 0) resultingHealth = 0;
    	return resultingHealth;
    }
    
    private void updateProgressBar(boolean iWonMove, int newBattleHealth) {
    	if (!iWonMove) {
    		((ProgressBar) findViewById(R.id.myBattleHealth)).setProgress(newBattleHealth);
    		myBattleHealth = newBattleHealth;
    	} else {
    		((ProgressBar) findViewById(R.id.opponentBattleHealth)).setProgress(newBattleHealth);
    		opponentBattleHealth = newBattleHealth;
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
		if (battleId == null) {
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
		/* oppMove should never be null when we get here */
		private Integer getOpponentMove(String encodedMoves) {
			String[] moves = encodedMoves.split(" ");
			if (moves.length > numMovesPlayed) {
				return Integer.valueOf(moves[numMovesPlayed]);
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
    private class SendMoveTask extends AsyncTask<Integer, Void, Void> {
    	protected Void doInBackground(Integer...integers) {
    		try {
    			//String myMove = strings[0];
    			//String myId = strings[1];
    			//String battleId = strings[2];
    			Integer numMovesPlayed = integers[0];
    			waitUntilBattleCreated();
    			server.sendMove(numMovesPlayed.toString() + "_" + myMove.toString(), myId, battleId, myStartingStrength.toString());
    		} catch(Exception ex) {
    			System.out.println("failed to send move");
    			ex.printStackTrace();
    		}
			return null;
    	}
    }
}