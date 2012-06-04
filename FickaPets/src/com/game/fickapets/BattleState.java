package com.game.fickapets;


import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BattleState {
	/* bundle keys */
	public static final String OPPONENT_NAME = "opponentName";
	public static final String OPPONENT_ID = "opponentId";
	public static final String MY_ID = "myId";
	public static final String BATTLE_ID = "battleIdKey";
	public static final String MY_MOVE = "myMoveKey";
	public static final String OPPONENT_MOVE = "opponentMoveKey";
	public static final String NUM_MOVES = "numMovesPlayedKey";
	public static final String OPPONENT_HEALTH = "opponentBattleHealth";
	public static final String MY_HEALTH = "myBattleHealth";
	public static final String MOVES_PLAYED = "numMovesPlayed";
	public static final String MY_STRENGTH = "myStartingStrength";
	public static final String OPPONENT_STRENGTH = "opponentStartingStrength";
	public static final String PET_IMG_NAME = "petImageFilename";
	
	private static final String MY_MOVE_NULL = "myMoveIsNull";
	private static final String OPP_MOVE_NULL = "opponentMoveIsNull";
	
	public String bid;
	private String myMove;
	private String opponentMove;
	public Double myStartingStrength;
	public Double opponentStartingStrength;
	public String opponentName;				/* never null */
	public Integer myHealth;				/* battle health always starts at 100 */
	public Integer opponentHealth;			/* battle health always starts at 100 */
	public String opponentId;				/* never null */
	public String myId;						/* never null */
	public String petImgName;				/* never null */
	public Integer numMovesPlayed;
	
	private Boolean myMoveIsNull = false;
	private Boolean oppMoveIsNull = false;
	
	public BattleState(Context context, Bundle bStateBundle) {
		setStateWithBundle(context, bStateBundle);
	}
	
	/* initializes BattleState object with contents of JSONObject */
	public BattleState(Context context, JSONObject jsonObject) {
		Intent intent = addExtrasToIntent(new Intent(), jsonObject);
		setStateWithBundle(context, intent.getExtras());
	}
	
	private Integer getMove(String moveStr) {
		if (moveStr == null || moveStr.equals("")) return null;
		String[] myMoves = moveStr.split(" ");
		return Integer.valueOf(myMoves[myMoves.length-1]);
	}
	
	public Integer getMyMove() {
		if (myMoveIsNull) return null;
		return getMove(myMove);
	}
	/* hides the opponent's move until the move has been played, but displays our own move after
	 * we've chosen it - this is ued to build the move history in BattleActivity
	 */
	private Integer getMove(int moveIndex, boolean fromMyMoves) {
		String[] moves;
		if (fromMyMoves) {
			if (myMove == null || myMove.length() == 0) return null;
			moves = myMove.split(" ");
		} else {
			if (opponentMove == null || opponentMove.length() == 0 || moveIndex >= numMovesPlayed) return null;
			moves = opponentMove.split(" ");
		}
		if (moveIndex >= moves.length) return null;
		return Integer.valueOf(moves[moveIndex]);
	}
	
	/* returns the move at moveIndex or null there haven't been that many moves */
	public Integer getMyMove(int moveIndex) {
		Integer move = getMove(moveIndex, true);
		return move;
	}
	public Integer getOppMove(int moveIndex) {
		return getMove(moveIndex, false);
	}
	
	public void setMyMove(Integer myMove) {
		if (myMove == null) {
			myMoveIsNull = true;
			return;
		} else {
			myMoveIsNull = false;
			if (this.myMove == null || this.myMove.equals("")) {
				this.myMove = myMove.toString();
			} else {
				this.myMove = this.myMove + " " + (myMove == null ? "null" : myMove.toString());
			}
		}

	}
	
	public void setOpponentMove(Integer oppMove) {
		if (oppMove == null) {
			oppMoveIsNull = true;
		} else {
			oppMoveIsNull = false;
			if (opponentMove == null || opponentMove.equals("")) {
				opponentMove = oppMove.toString();
			} else {
				opponentMove = opponentMove + " " + oppMove.toString();
			}
		}
	}
	
	public Integer getOpponentMove() {
		Log.v("FickaPets", "opponent move str is: " + opponentMove);
		if (oppMoveIsNull) return null;
		Integer move = getMove(opponentMove);
		Log.v("FickaPets", "opponent move returned as: " + move.toString());
		return move;
	}
	
	private void setStateWithBundle(Context context, Bundle bStateBundle) {
		/* these four values are always set in bundle */
    	opponentId = bStateBundle.getString(BattleState.OPPONENT_ID);
    	opponentName = bStateBundle.getString(BattleState.OPPONENT_NAME);
    	myId = bStateBundle.getString(BattleState.MY_ID);
    	petImgName = bStateBundle.getString(BattleState.PET_IMG_NAME);
    	
    	/* the rest of these may not be set in bundle */
    	numMovesPlayed = getMovesPlayed(bStateBundle);
    	myHealth = getHealth(bStateBundle, BattleState.MY_HEALTH);
    	opponentHealth = getHealth(bStateBundle, BattleState.OPPONENT_HEALTH);
    	myStartingStrength = getMyStrength(context, bStateBundle.getString(MY_STRENGTH));
    	opponentStartingStrength = getOpponentStrength(bStateBundle.getString(OPPONENT_STRENGTH));
    	opponentMove = bStateBundle.getString(OPPONENT_MOVE);
    	
    	if (opponentMove == null || opponentMove.equals("")) {
    		oppMoveIsNull = true;
    	} else if (bStateBundle.getString(OPP_MOVE_NULL) != null && !bStateBundle.getString(OPP_MOVE_NULL).equals("")) {
    		oppMoveIsNull = new Boolean(bStateBundle.getString(OPP_MOVE_NULL));
    	}
    	bid = getBattleId(bStateBundle);
    	myMove = bStateBundle.getString(MY_MOVE);
    	if (myMove == null || myMove.equals("")) {
    		myMoveIsNull = true;
    	} else if (bStateBundle.getString(MY_MOVE_NULL) != null) {
    		myMoveIsNull = new Boolean(bStateBundle.getString(MY_MOVE_NULL));
    	}
	}
	
	
	
	private Double getOpponentStrength(String opponentStrength) {
		if (opponentStrength == null || opponentStrength.equals("")) return null;
		return Double.valueOf(opponentStrength);
	}
	
	/* should default to zero */
	private Integer getMovesPlayed(Bundle bundle) {
		String moves = bundle.getString(BattleState.NUM_MOVES);
		if (moves == null || moves.equals("")) {
			return 0;
		} else {
			return Integer.valueOf(moves);
		}
	}
	
	/* defaults to 100. whosHealth is either MY_HEALTH or OPPONENT_HEALTH */
	private Integer getHealth(Bundle bundle, String whosHealth) {
		String health = bundle == null ? null : bundle.getString(whosHealth);
		if (health == null || health.equals("")) {
			return getDefaultBattleHealth();
		} else {
			return Integer.valueOf(health);
		}
	}
	
	private Integer getDefaultBattleHealth() {
		return 100;
	}
	/* 
	private Integer getMyMove(Bundle bundle) {
    	String myMove = bundle.getString(BattleState.MY_MOVE);
    	if (myMove != null && !myMove.equals("")) {
    		return Integer.valueOf(myMove);
    	} else {
    		return null;
    	}
	} */
	
	private String getBattleId(Bundle bundle) {
    	String bid = bundle.getString(BattleState.BATTLE_ID);
    	if (bid != null && !bid.equals("")) {
    		return bid;
    	} else {
    		return null;
    	}
	}
	
	private Double getMyStrength(Context context, String myStartingStrength) {
    	if (myStartingStrength == null || myStartingStrength.equals("")) {
        	return new Double(Pet.thePet(context).getAttributes().strength);
    	} else {
    		return Double.valueOf(myStartingStrength);
    	}
	}
	/* 
	private Integer getOppMove(Bundle bundle) {
		String opponentMove = bundle.getString(BattleState.OPPONENT_MOVE);
		if (opponentMove == null || opponentMove.equals("")) {
			return null;
		} else {
			return Integer.valueOf(opponentMove);
		}
	} 
	*/
	
	
	
	public JSONObject toJSON() {
		JSONObject battle = new JSONObject();
		try {
			battle.put(BATTLE_ID, bid);
			battle.put(MY_MOVE, myMove == null ? null : myMove.toString());
			battle.put(OPPONENT_MOVE, opponentMove == null ? null : opponentMove.toString());
			battle.put(MY_STRENGTH, myStartingStrength == null ? null : myStartingStrength.toString());
			battle.put(OPPONENT_STRENGTH, opponentStartingStrength == null ? null : opponentStartingStrength.toString());
			battle.put(OPPONENT_NAME, opponentName);
			battle.put(MY_HEALTH, myHealth == null ? null : myHealth.toString());
			battle.put(OPPONENT_HEALTH, opponentHealth == null ? null : opponentHealth.toString());
			battle.put(OPPONENT_ID, opponentId);
			battle.put(MY_ID, myId);
			battle.put(NUM_MOVES, numMovesPlayed == null ? null : numMovesPlayed.toString());
			battle.put(MY_MOVE_NULL, toString(myMoveIsNull));
			battle.put(OPP_MOVE_NULL, toString(oppMoveIsNull.toString()));
			battle.put(PET_IMG_NAME, petImgName);
			return battle;
		} catch(Exception ex) {
			System.out.println("failed to build json battle object");
			ex.printStackTrace();
			return null;
		}
	}
	
	
	/* adds battle state held in JSONOBject to intent */
	public static Intent addExtrasToIntent(Intent intent, JSONObject battle) {
		try {
			intent.putExtra(BATTLE_ID, battle.getString(BATTLE_ID));
			intent.putExtra(MY_MOVE, battle.optString(MY_MOVE));
			intent.putExtra(OPPONENT_MOVE, battle.optString(OPPONENT_MOVE));
			intent.putExtra(MY_STRENGTH, battle.optString(MY_STRENGTH));
			intent.putExtra(OPPONENT_STRENGTH, battle.optString(OPPONENT_STRENGTH));
			intent.putExtra(OPPONENT_NAME, battle.getString(OPPONENT_NAME));
			intent.putExtra(MY_HEALTH, battle.optString(MY_HEALTH));
			intent.putExtra(OPPONENT_HEALTH, battle.optString(OPPONENT_HEALTH));
			intent.putExtra(OPPONENT_ID, battle.getString(OPPONENT_ID));
			intent.putExtra(MY_ID, battle.getString(MY_ID));
			intent.putExtra(NUM_MOVES, battle.optString(NUM_MOVES));
			intent.putExtra(OPP_MOVE_NULL, battle.optString(OPP_MOVE_NULL));
			intent.putExtra(MY_MOVE_NULL, battle.optString(MY_MOVE_NULL));
			intent.putExtra(PET_IMG_NAME, battle.getString(PET_IMG_NAME));
			return intent;
		} catch(Exception ex) {
			System.out.println("failed to extract state from JSONObject");
			ex.printStackTrace();
			return null;
		}
	}
	
	public Intent addStateToIntent(Intent intent) {
		return addExtrasToIntent(intent, toJSON());
	}
	
	private String toString(Object o) {
		if (o == null) return null;
		return o.toString();
	}
	
	public Bundle bundleUp() {
	//	String myMove = this.myMove == null ? "" : this.myMove.toString();
		//String opponentMove = this.opponentMove == null ? "" : this.opponentMove.toString();
		Bundle bundle = new Bundle();
		bundle.putString(BATTLE_ID, bid);
		bundle.putString(MY_MOVE, toString(myMove));
		bundle.putString(OPPONENT_MOVE, toString(opponentMove));
		bundle.putString(MY_STRENGTH, toString(myStartingStrength));
		bundle.putString(OPPONENT_STRENGTH, toString(opponentStartingStrength));
		bundle.putString(OPPONENT_NAME, opponentName);
		bundle.putString(MY_HEALTH, toString(myHealth));
		bundle.putString(OPPONENT_HEALTH, toString(opponentHealth));
		bundle.putString(OPPONENT_ID, opponentId);
		bundle.putString(MY_ID, myId);
		bundle.putString(NUM_MOVES, toString(numMovesPlayed));
		bundle.putString(PET_IMG_NAME, petImgName);
		
		bundle.putString(MY_MOVE_NULL, toString(myMoveIsNull));
		bundle.putString(OPP_MOVE_NULL, toString(oppMoveIsNull));
		return bundle;
	}
	
}
