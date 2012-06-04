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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BattleActivity extends Activity {
	/* successful attacks to win if opponent has equal strength */
	private static final double MOVES_TO_WIN = 4;
	private static final int ATTACK_DIALOG = 0;
	private static final int SHOW_MOVE_DIALOG = 1;
	private static final int SHOW_VICTOR_DIALOG = 2;
	private static final int SHOW_GAMEOVER_DIALOG = 3;
	private static final int CREATE_GAME_FAIL = 4;
	private static final int SERVER_FAIL = 5;

	/* evade beats attack, attack beats magic, and magic beats defend */
	private static final int EVADE = 0;
	private static final int ATTACK = 1;
	private static final int MAGIC = 2;
	
	/* only used in gameOver dialog - set in gameOver() method */
	private boolean iWonMove;
	
	BattleState bState;
	
	private PollOpponentMove pollOpponentMove;
	private FickaServer server;

	private boolean gameOver = false;
	private boolean gameIsTie = false;
	
	private WaitingView waitingView;
	private AttackButton attackButton;
	 
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.on_progress);
    	
    	server = new FickaServer(this);
    	waitingView = new WaitingView();
    	attackButton = new AttackButton();
    	Bundle extras = getIntent().getExtras();
    	
    	/* empties bundle into battle state object */
    	bState = new BattleState(this, extras);
    	if (bState.bid == null) {
    		new CreateGameTask().execute();
    	} else {
    		inflateBattleScene();
        	/*
        	RelativeLayout rl = (RelativeLayout)findViewById(R.id.battleLayout);
        	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params.addRule(RelativeLayout.ABOVE, R.id.myPet);
        	ImageView img = new ImageView(this);
        	img.setId(R.id.attack_button);
        	img.setImageResource(R.drawable.attack_button);
        	rl.addView(img);*/
    		if (bState.getMyMove() != null) {
        		/* already made a move so view is invisible */
        		attackButton.setInvisible();
        		
        		if (bState.getOpponentMove() == null) {
        			startWaitingOnOppMove();
        		} else {
        			/* if we have opponent move, we also have opponent strength */
        			playMove();
        		}
        	}
    	}
    }
    

    /* setup the battle layout */
    private void inflateBattleScene() {
		setContentView(R.layout.battle);
    	setProgressBars(bState.myHealth, bState.opponentHealth);
    	TextView opponentName = (TextView)findViewById(R.id.opponentBattleName);
    	opponentName.setText(getFirstName(bState.opponentName));
    	TextView myName = (TextView)findViewById(R.id.myBattleName);
    	myName.setText("me");
    	attackButton.setVisible();
    	buildMoveImages();
    }
    /*
    private int getMargin(RelativeLayout rootView) {
    	int width = rootView.getWidth();
    	return (int)(width / 4);
    }*/
    
    /* this is called after we know layout dimensions - I'm using it to place the name textviews
     */
    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	TextView opponentName = (TextView)findViewById(R.id.opponentBattleName);
    	int margin = getMargin((RelativeLayout)opponentName.getParent());
    	RelativeLayout.LayoutParams opponentParams = (RelativeLayout.LayoutParams)opponentName.getLayoutParams();
    	opponentParams.leftMargin(margin);
    	
    }*/
    
    
    
    private void startWaitingOnOppMove() {
    	pollOpponentMove = new PollOpponentMove();
    	pollOpponentMove.execute();
    	waitingView.addWaitingView();
    
    }
    
    private void setProgressBars(Integer myBattleHealth, Integer opponentBattleHealth) {
    	((ProgressBar)findViewById(R.id.myBattleHealthBar)).setProgress(myBattleHealth);
    	((ProgressBar)findViewById(R.id.opponentBattleHealthBar)).setProgress(opponentBattleHealth);
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
    

    /* called when attack button is pressed */
    public void onFightPressed(View v) {
    	if (v.getVisibility() == View.VISIBLE) {
        	showDialog(ATTACK_DIALOG);
    	}
    }
    
    private int imgIdForMove(int move) {
    	switch(move) {
    	case ATTACK:
    		return R.drawable.attack;
    	case EVADE:
    		return R.drawable.defend;
    	case MAGIC:
    		return R.drawable.magic;
    	default:
    		return -1;
    	}
    }
         
    private void addMoveToUI(Integer move, boolean isMyMove) {
    	LinearLayout ll;
    	if (isMyMove) {
    		ll = (LinearLayout)findViewById(R.id.myMoves);
    		
    	} else {
    		ll = (LinearLayout)findViewById(R.id.oppMoves);
    	}
    	ImageView newImg = new ImageView(this);
		newImg.setImageResource(imgIdForMove(move));
		//LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.android.widget.TableRow.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		
		ll.addView(newImg);
		final ScrollView scrollView = (ScrollView)findViewById(R.id.scrollView);
		scrollView.post(new Runnable() {
			public void run() {
				scrollView.fullScroll(View.FOCUS_DOWN);
			}
		});
    }
    
    private void buildMoveImages() {
    	int index = 0;
    	Integer myMove = bState.getMyMove(index);
    	while (myMove != null) {
    		addMoveToUI(myMove, true);
    		index++;
    		myMove = bState.getMyMove(index);
    	}
    	index = 0;
    	Integer oppMove = bState.getOppMove(index);
    	while (oppMove != null) {
    		addMoveToUI(oppMove, false);
    		index++;
    		oppMove = bState.getOppMove(index);
    	}
    }
    
    private String getFirstName(String fullName) {
    	String[] names = fullName.split(" ");
    	if (names.length > 0) {
    		return names[0];
    	}
    	return "";
    }
    
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
    	case ATTACK_DIALOG:
    		final String[] moves = new String[3];
    		moves[0] = "Evade";
    		moves[1] = "Attack";
    		moves[2] = "Magic";
    		AlertDialog attackDialog = new AlertDialog.Builder(this)
				.setTitle("Pick a move")
				.setItems(moves, new OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						bState.setMyMove(item);
						addMoveToUI(bState.getMyMove(), true);
						/* checking again in case of double click on this dialog */
						if (attackButton.isVisible()) {
							waitingView.addWaitingView();
							new SendMoveTask().execute(bState.numMovesPlayed);
							if (bState.getOpponentMove() == null) {
								startWaitingOnOppMove();
							} else {
								playMove();
							}
							
						}
					}
				})
				.create();
    		return attackDialog;
    	case SHOW_MOVE_DIALOG:
			AlertDialog moveDialog = new AlertDialog.Builder(this)
				.setMessage("")
				.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
				    	addMoveToUI(bState.getOpponentMove(), false);
						continueMove1();
					}
				}).create();
			return moveDialog;
    	case SHOW_VICTOR_DIALOG:
    		AlertDialog victorDialog = new AlertDialog.Builder(this)
    			/* will set message in onPrepareDialog */
				.setMessage("")
				.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						continueMove2();
					}
				}).create();
    		return victorDialog;
    	case SHOW_GAMEOVER_DIALOG:
    		String message;
    		if (iWonMove) {
    			message = "You Win!";
    		} else {
    			message = bState.opponentName + " has beaten you";
    		}
    		AlertDialog gameOverDialog = new AlertDialog.Builder(this)
    			.setMessage(message)
    			.setNeutralButton("Exit Battle", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					finish();
    				}
    			}).create();
    		return gameOverDialog;
    	case CREATE_GAME_FAIL:
    		AlertDialog createGameFail = new AlertDialog.Builder(this)
    			.setMessage("Failed to create new game. If network is connected, our server may be down")
    			.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					finish();
    				}
    			}).create();
    		return createGameFail;
    	case SERVER_FAIL:
    		AlertDialog serverFail = new AlertDialog.Builder(this)
    			.setMessage("Cannot connect to server.  If you're connected to the Internet, our server may be down\nTry again later, and sorry for the inconvenience")
    			.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					finish();
    				}
    			}).create();
    		return serverFail;
		default:
			return null;
    	}
    }
    
    private String getNameOfMove(Integer move) {
    	switch(move) {
    	case EVADE:
    		return "evade";
    	case ATTACK:
    		return "attack";
    	case MAGIC:
    		return "magic";
    	default:
    		return null;
    	}
    }
    
    protected void onPrepareDialog(int id, Dialog d) {
    	AlertDialog dialog = (AlertDialog)d;
    	switch(id) {
    	case SHOW_MOVE_DIALOG:
    		dialog.setMessage(bState.opponentName + " used " + getNameOfMove(bState.getOpponentMove()));
    		break;
    	case SHOW_VICTOR_DIALOG:
    		boolean iWin = getWinner(bState.getMyMove(), bState.getOpponentMove());
    		if (iWin) {
    			dialog.setMessage(getNameOfMove(bState.getMyMove()) + " beats " + getNameOfMove(bState.getOpponentMove()));
    		} else {
    			dialog.setMessage(getNameOfMove(bState.getOpponentMove()) + " beats " + getNameOfMove(bState.getMyMove()));
    		}
    		/* forgot to handle ties */
    		if (bState.getMyMove().equals(bState.getOpponentMove())) {
    			dialog.setMessage("Both pets damaged");
    		}
    		break;
    	case SHOW_GAMEOVER_DIALOG:
    		if (gameIsTie) {
    			dialog.setMessage("Both pets passed out! Battle is a tie");
    		}
    		break;
    	default:
    		return;
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
  
    private void closeBattle() {
    	new Thread(new Runnable() {
    		public void run() {
    			server.closeBattle(bState.bid, bState.myId);
    		}
    	}).start();
    }
    
    private void gameOver(boolean iWonMove) {
    	this.iWonMove = iWonMove;
    	gameOver = true;
    	showDialog(SHOW_GAMEOVER_DIALOG);

    	closeBattle();
    }
    
    /* returns the loser's new battle health having lost this move */
    private int getLoserBattleHealth(boolean iWonMove, double damage) {
    	int battleHealth;
    	int battleHealthDrop;
    	if (!iWonMove) {
    		battleHealthDrop = getBattleHealthDrop(damage, bState.myStartingStrength);
        	battleHealth = ((ProgressBar) findViewById(R.id.myBattleHealthBar)).getProgress();
    	} else {
    		battleHealthDrop = getBattleHealthDrop(damage, bState.opponentStartingStrength);
        	battleHealth = ((ProgressBar) findViewById(R.id.opponentBattleHealthBar)).getProgress();
    	}
    	int resultingHealth = battleHealth - battleHealthDrop;
    	if (resultingHealth < 0) resultingHealth = 0;
    	return resultingHealth;
    }
    
    private void updateProgressBarAndHealth(boolean iWonMove, int newBattleHealth) {
    	if (!iWonMove) {
    		int progressId = R.id.myBattleHealthBar;
    		Integer oldBattleHealth = ((ProgressBar) findViewById(progressId)).getProgress();
    		new HealthDropAnimation().execute(newBattleHealth, oldBattleHealth, progressId);
    		bState.myHealth = newBattleHealth;
    	} else {
    		Integer progressId = R.id.opponentBattleHealthBar;
    		Integer oldBattleHealth = ((ProgressBar) findViewById(progressId)).getProgress();
    		new HealthDropAnimation().execute(newBattleHealth, oldBattleHealth, progressId);
    		bState.opponentHealth = newBattleHealth;
    	}
    }
    
  
    /* first alert dialog bounces to this next alert dialog, which bounces to continueMove2 which finishes up the move */
    private void continueMove1() {
    	showDialog(SHOW_VICTOR_DIALOG);
    }
    
    /* finishes clean up and animation associated with move */
    private void continueMove2() {
    	boolean iWonMove = getWinner(bState.getMyMove(), bState.getOpponentMove());
    	/* damage is proportional to victim's starting strength.  If damage is half victim's starting strength, 
    	 * then victim loses half his life
    	 */
    	double damageToLoser;
    	/* for ties */
    	double damageToMe;
    	double damageToOpp;
    	

    	
    	if (bState.getMyMove().equals(bState.getOpponentMove())) {
    		damageToMe = getDamageOnAttack(bState.opponentStartingStrength);
    		damageToOpp = getDamageOnAttack(bState.myStartingStrength);
    		int myBattleHealth = getLoserBattleHealth(false, damageToMe);
    		int oppBattleHealth = getLoserBattleHealth(true, damageToOpp);
    		updateProgressBarAndHealth(false, myBattleHealth);
    		updateProgressBarAndHealth(true, oppBattleHealth);
    		if (myBattleHealth == 0 && oppBattleHealth == 0) {
    			gameIsTie = true;
    			gameOver(true);	/* parameter doesn't matter - message will be changed in onPrepareDialog */
    		} else if (myBattleHealth == 0) {
    			gameOver(false);
    		} else if (oppBattleHealth == 0) {
    			gameOver(true);
    		}
    	} else {
    		if (iWonMove) {
        		/* attacker strength determines damage done to victim */
        		damageToLoser = getDamageOnAttack(bState.myStartingStrength);
        	} else {
        		damageToLoser = getDamageOnAttack(bState.opponentStartingStrength);
        	}
    		int battleHealth = getLoserBattleHealth(iWonMove, damageToLoser);
        	updateProgressBarAndHealth(iWonMove, battleHealth);
        	if (battleHealth == 0) {
        		gameOver(iWonMove);
        	}
    	}
    	
    	
    	
    	/* setting fightButton to visible closes the loop.  When another move is selected
    	 * we start waiting on opponent move again
    	 */
    	
        attackButton.setVisible();
    	bState.setMyMove(null);
    	bState.setOpponentMove(null);
    	bState.numMovesPlayed = bState.numMovesPlayed + 1;
    	Log.v("FickaPets", "num moves is now " + bState.numMovesPlayed.toString());
    }
    
    /* if this is called from PollOpponentMove, no need to put opponentMove in battle state since it's
     * cleared anyways after method finishes.  But if opponentMove was known before our move made, need
     * to clear it
     */
    private void playMove() {
    	assert bState.getMyMove() != null && bState.getOpponentMove() != null;
    	showDialog(SHOW_MOVE_DIALOG);
    }
    
    private class AttackButton {
    	public static final int ATTACK_BTN_ID = R.id.attackButtonBattle;
    	private void removeWaitingView() {
    		View v = BattleActivity.this.findViewById(WaitingView.WAITING_VIEW_ID);
    		if (v != null) {
    			RelativeLayout rootView = (RelativeLayout)v.getParent();
    			rootView.removeView(v);
    		}
    	}
    	
    	private void placeScrollView() {
    		ScrollView scrollView = (ScrollView)BattleActivity.this.findViewById(R.id.scrollView);
    		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)scrollView.getLayoutParams();
    		params.addRule(RelativeLayout.ABOVE, ATTACK_BTN_ID);
    		scrollView.setLayoutParams(params);
    	}
    	private boolean buttonOnScreen() {
    		return BattleActivity.this.findViewById(ATTACK_BTN_ID) != null;
    	}
    	private ImageView addAttackButtonToLayout() {
    		if (buttonOnScreen()) {
    			return (ImageView)BattleActivity.this.findViewById(ATTACK_BTN_ID);
    		}
    		ImageView btn = new ImageView(BattleActivity.this);
    		btn.setImageResource(R.drawable.attack_button);
    		btn.setId(ATTACK_BTN_ID);
    		btn.setOnClickListener(new View.OnClickListener() {
    			public void onClick(View v) {
    				onFightPressed(v);
    			}
    		});
    		RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    		btnParams.addRule(RelativeLayout.ABOVE, R.id.myPet);
    		btnParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
    		
    		RelativeLayout rootView = (RelativeLayout)findViewById(R.id.battleLayout);
    		rootView.addView(btn, btnParams);
    		placeScrollView();
    		return btn;
    	}
    	/* adds button to view if it doesn't already exist, otherwise returns existing button */
    	private ImageView addAttackButton() {
    		removeWaitingView();
    		return addAttackButtonToLayout();
    	}
    	
    	public void setVisible() {
    		ImageView btn = addAttackButton();
    		btn.setVisibility(ImageView.VISIBLE);
    	}
    	public void setInvisible() {
    		ImageView btn = addAttackButton();
    		btn.setVisibility(ImageView.INVISIBLE);
    	}
    	public boolean isVisible() {
    		if (buttonOnScreen()) {
    			ImageView btn = (ImageView)findViewById(ATTACK_BTN_ID);
    			return btn.getVisibility() == ImageView.VISIBLE;
    		}
    		return false;
    	}
    }
    
    private class WaitingView {
    	public static final int WAITING_VIEW_ID = R.id.waitingForBattleMoveLinLayout;
    	
    	public void addWaitingView() {
    		removeAttackBtn();
    		addWaitingViewToLayout();
    	}
    	
    	private void removeAttackBtn() {
    		View v = BattleActivity.this.findViewById(AttackButton.ATTACK_BTN_ID);
    		if (v != null) {
    			RelativeLayout rootView = (RelativeLayout)v.getParent();
    			rootView.removeView(v);
    		}
    	}
    	private void placeScrollView() {
    		ScrollView scrollView = (ScrollView)BattleActivity.this.findViewById(R.id.scrollView);
    		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)scrollView.getLayoutParams();
    		params.addRule(RelativeLayout.ABOVE, WAITING_VIEW_ID);
    		scrollView.setLayoutParams(params);
    	}
    	private void addWaitingViewToLayout() {
    		View v = BattleActivity.this.findViewById(WAITING_VIEW_ID);
    		if (v != null) {
    			return;
    		}
    		
    		LayoutInflater inflater = getLayoutInflater();
        	LinearLayout newView = (LinearLayout) inflater.inflate(R.layout.waiting_layout, null, false);
        	newView.setId(WAITING_VIEW_ID);
        	TextView waitingText = (TextView)newView.getChildAt(0);
        	waitingText.setText("Waiting on " + getFirstName(bState.opponentName) + "'s move");
        	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        	params.addRule(RelativeLayout.ABOVE, R.id.myPet);
        	params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        	
        	RelativeLayout rootView = (RelativeLayout)findViewById(R.id.battleLayout);
        	rootView.addView(newView, params);
        	placeScrollView();
    	}
    }
    
    private class HealthDropAnimation extends AsyncTask<Integer, Integer, Void> {
    	private Double velocity = 50.0; //health points per second
    	
		@Override
		protected Void doInBackground(Integer... params) {
			Integer newHealth = params[0];
			Integer currentHealth = params[1];
			Integer progressId = params[2];
			while (currentHealth > newHealth) {
				currentHealth -= 1;
				publishProgress(currentHealth, progressId);
				try {
					Thread.sleep((long)(1000/velocity));
				} catch(InterruptedException ex) {
					System.out.println("Couldn't sleep");
				}
			}
			return null;
		}
		
		protected void onProgressUpdate(Integer...params) {
			Integer health = params[0];
			Integer progressId = params[1];
    		((ProgressBar) findViewById(progressId)).setProgress(health);
		}
    }
    
    
    
    /* Background asynctasks for talking to server -------------------------------------------------------------------------------------*/

    
	private boolean battleCreated() {
		if (bState.bid == null) {
			return false;
		}
		return true;
	}
	
	private void waitUntilBattleCreated() {
		while (!battleCreated()) {
			try {
				Thread.sleep(2000);
			} catch(InterruptedException ex) {}
		}
	}
    
    /* Should be called after we've made a move and need to get the opponent's move to continue.  Polls
     * the server for opponent's move every SECONDS_BETWEEN_POLL.
     */
    private class PollOpponentMove extends AsyncTask<Void, Void, String[]> {
    	private static final int SECONDS_BETWEEN_POLL = 7;

		@Override
		protected String[] doInBackground(Void... params) {
			/* possible to make move before server returned with battle id */
			waitUntilBattleCreated();
			Map<String, String> battleMap = null;
			try {
				battleMap = server.getBattleData(bState.myId, bState.bid);
			} catch(IOException ex) {}
			while (!isNewMove(battleMap)) {
				if (isCancelled()) return null;
				try {
					Thread.sleep(SECONDS_BETWEEN_POLL * 1000);
				} catch(InterruptedException ex) { 
					System.out.println("sleep got interrupted while waiting for opponent's move");
					ex.printStackTrace();
				}
				try {
					battleMap = server.getBattleData(bState.myId, bState.bid);
				} catch(IOException ex) {
					System.out.println("failed to get data from server - waiting for opponent's move");
					ex.printStackTrace();
				}
			}
			Log.v("FickaPets", "done polling server for opponent move - got " + battleMap.get(FickaServer.OPP_MOVE_KEY));
			String[] result = new String[2];
			result[0] = battleMap.get(FickaServer.OPP_MOVE_KEY);
			result[1] = battleMap.get(FickaServer.OPP_STRENGTH_KEY);
			return result;
		}
		
		private boolean isNewMove(Map<String, String> battleMap) {
			if (battleMap != null && battleMap.get(FickaServer.OPP_MOVE_KEY) != null) {
				String encodedMoves = battleMap.get(FickaServer.OPP_MOVE_KEY);
				String[] moves = encodedMoves.split(" ");
				if (moves.length > bState.numMovesPlayed) {
					return true;
				}
			}
			return false;
		}
		
		/* oppMove should never be null when we get here */
		private Integer getOpponentMove(String encodedMoves) {
			Log.v("MOVES", encodedMoves);
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
			Double opponentStrength = Double.valueOf(data[1]);
			assert opponentMove != null && opponentStrength != null;
			bState.opponentStartingStrength = opponentStrength;
			bState.setOpponentMove(opponentMove);
			attackButton.setInvisible();
    		playMove();
		}
    }
    /* creates a new game */
    private class CreateGameTask extends AsyncTask<Void, Void, String> {
    	protected String doInBackground(Void...voids) {
    		String bid = null;
			bid = server.createGame(bState.myId, bState.opponentId);
    		return bid;
    	}
    	protected void onPostExecute(String bid) {
    		if (bid == null) {
    			showDialog(CREATE_GAME_FAIL);
    			return;
    		}
    		inflateBattleScene();
    		bState.myStartingStrength = Pet.thePet(BattleActivity.this).getAttributes().strength;
    		bState.bid = bid;
    	}
    }
    /* sends move */
    private class SendMoveTask extends AsyncTask<Integer, Void, Boolean> {
    	protected Boolean doInBackground(Integer...integers) {
    		Boolean sent = false;
    		try {
    			Integer numMovesPlayed = integers[0];
    			waitUntilBattleCreated();
    			sent = server.sendMove(numMovesPlayed.toString() + "_" + bState.getMyMove().toString(), bState.myId, bState.bid, bState.myStartingStrength.toString());
    			showDialog(SERVER_FAIL);
    			Log.v("FickaPets", "my move (" + bState.getMyMove().toString() + ") was sent successfully");
    		} catch(Exception ex) {
    			System.out.println("failed to send move");
    			ex.printStackTrace();
    		}
			return sent;
    	}
    	
    	protected void onPostExecute(Boolean sent) {
    		if (!sent) {
    			showDialog(SERVER_FAIL);
    		}
    	}
    }
}