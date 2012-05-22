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

	
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.battle);
    	Bundle extras = getIntent().getExtras();
    	opponentId = extras.getString(FindFriendsActivity.OPPONENT_ID_KEY);
    	opponentName = extras.getString(FindFriendsActivity.OPPONENT_NAME_KEY);
    	myId = extras.getString(FindFriendsActivity.MY_ID_KEY);
    	server = new FickaServer(this); 
    	createNewBattle(myId, opponentId, server);
    }
    
    /* here, we serialize battle into json and write it out to file */
    public void onDestroy() {
    	if (!gameOver) {
    		PersistenceHandler.saveBattle(this, battleId, opponentName, myMove.toString(), myId, opponentId);
    	}
    }
    private class CreateGameTask extends AsyncTask<Void, Void, String> {
    	protected String doInBackground(Void...voids) {
    		try {
    			return server.createGame(myId, opponentId);
    		} catch(IOException ex) {
    			System.out.println("failed to create game on server");
    			ex.printStackTrace();
    			return null;
    		}
    	}
    	protected void onPostExecute(String bid) {
    		battleId = bid;
    		myStartingStrength = Pet.thePet(BattleActivity.this).getAttributes().strength;
    	}
    }
   
    private void createNewBattle(final String mId, final String opponentId, final FickaServer server) {
    	new CreateGameTask().execute();
    	/*new Thread(new Runnable() {
    		public void run() {
    			try {
    				final String bid = server.createGame(mId, opponentId);
    				runOnUiThread(new Runnable() {
    					public void run() {
    						battleId = bid;
    						myStartingStrength = Pet.thePet(BattleActivity.this).getAttributes().strength;
    					}
    				});
    			} catch(Exception ex) {
    				ex.printStackTrace();
    			}
    		}
    	}).run();*/
    }
    /* returns the damage done to victim.  The victim's starting strength is treated as life and after
     * attacker wins a move, the victim's life goes down by the damage returned here.  If this returns 50 and
     * victim's starting strength is 100, then victim loses half his life. If both opponents start with the same
     * strength, then it takes MOVES_TO_WIN victories to win a battle
     */
    private double getDamageOnAttack(double attackerStrength) {
    	return attackerStrength / MOVES_TO_WIN;
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
						new PollOponnentMove().execute();
					}
				})
				.create();
    		return attackDialog;
		default:
			return null;
    	}
    }
    
    public void onFightPressed(View v) {
    	if (v.getVisibility() == View.VISIBLE) {
        	showDialog(ATTACK_DIALOG);
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
    	});
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
    		damageToLoser = getDamageOnAttack(opponentStartingStrength);
    	} else {
    		damageToLoser = getDamageOnAttack(myStartingStrength);
    	}
    	animateMove(iWin, myMove, opponentMove, damageToLoser);
    }
    
    /* Should be called after we've made a move and need to get the opponent's move to continue.  Polls
     * the server for opponent's move every SECONDS_BETWEEN_POLL.
     */
    private class PollOponnentMove extends AsyncTask<Void, Void, String[]> {
    	private String opponentStrength;
    	private String opponentMove;
    	private static final int SECONDS_BETWEEN_POLL = 3;
    	private boolean battleCreated() {
    		if (battleId != null) return true;
    		return false;
    	}
    	
    	private void waitUntilBattleCreated() throws InterruptedException {
    		while (!battleCreated()) {
    			Thread.sleep(2000);
    		}
    	}
    	
		@Override
		protected String[] doInBackground(Void... params) {
			try {
				waitUntilBattleCreated();
				Map<String, String> battleMap = server.getOppMove(myId, battleId);
				while (battleMap.get(FickaServer.OPP_MOVE_KEY) == null) {
					Thread.sleep(SECONDS_BETWEEN_POLL * 1000);
				}
				opponentMove = battleMap.get(FickaServer.OPP_MOVE_KEY);
				opponentStrength = battleMap.get(FickaServer.OPP_STRENGTH_KEY);
				String[] result = new String[2];
				result[0] = opponentMove;
				result[1] = opponentStrength;
				return result;
			} catch(Exception ex) {}
			return null;
		}
		
		protected void onPostExecute(String[] data) {
			String opponentMove = data[0];
			String opponentStrength = data[1];
    		playMove(Integer.valueOf(opponentMove), Double.valueOf(opponentStrength));
		}
    }
    
}