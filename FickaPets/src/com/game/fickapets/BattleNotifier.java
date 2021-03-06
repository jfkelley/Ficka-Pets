package com.game.fickapets;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class BattleNotifier extends IntentService {
	private final IBinder binder = new MyBinder();
	
	public BattleNotifier() {
		super("BattleNotifier");
	}
	
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_REDELIVER_INTENT;
	}
	/* adds all the battle ids to the currentBattles set - also returns my id */
	private Set<String> extractBattleIds(List<BattleState> battles, Set<String> battleIdSet) {
		String myId = null;
		for (int i = 0; i < battles.size(); i++) {
			BattleState battle = battles.get(i);
			if (myId == null) {
				myId = battle.myId;
			}
			battleIdSet.add(battle.bid);
		}
		return battleIdSet;
	}
	
	private void sendChallengeNotification(String name, BattleState thisBattle) {
		Intent notificationIntent = new Intent(this, BattleActivity.class);
		notificationIntent = thisBattle.addStateToIntent(notificationIntent);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification notification = new Notification(Pet.thePet(this).getIcon(), "", System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(), "New Battle Challenge", name + " has challenged your pet to a battle", contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NotificationIdHandler.getNewId(), notification);
	}
	
	protected void onHandleIntent(Intent intent) {
		String myId = PersistenceHandler.getFacebookId(this);
		FickaServer fickaServer = new FickaServer(this);
		Set<String> currentBattles = new HashSet<String>();

		/* ask if there are any incoming challenges */
		while (myId != null) {
			/* we should be in a separate thread, so don't need to create new thread for server requests */
			List<String> challengeIds = null;
			currentBattles.clear();
			List<BattleState> battles = PersistenceHandler.getBattles(this);
			currentBattles = extractBattleIds(battles, currentBattles);
			challengeIds = fickaServer.getChallenges(myId);
			if (challengeIds != null) {
				for (String battleId : challengeIds) {
					if (!currentBattles.contains(battleId)) {
						Map<String, String> battleMap = null;
						String name = null;
						try {
							battleMap = fickaServer.getBattleData(myId, battleId);
							name = fickaServer.getNameForId(battleMap.get(FickaServer.OPP_ID_KEY));
						} catch(Exception ex) {}
						if (battleMap == null || name == null) continue;
						Bundle battleBundle = new Bundle();
						battleBundle.putString(BattleState.OPPONENT_ID, battleMap.get(FickaServer.OPP_ID_KEY));
						battleBundle.putString(BattleState.OPPONENT_MOVE, battleMap.get(FickaServer.OPP_MOVE_KEY));
						battleBundle.putString(BattleState.OPPONENT_STRENGTH, battleMap.get(FickaServer.OPP_STRENGTH_KEY));
						battleBundle.putString(BattleState.OPPONENT_NAME, name);
						battleBundle.putString(BattleState.PET_IMG_NAME, battleMap.get(FickaServer.OPP_PET_KEY));
						battleBundle.putString(BattleState.MY_ID, myId);
						battleBundle.putString(BattleState.BATTLE_ID, battleId);
						BattleState thisBattle = new BattleState(this, battleBundle);
						PersistenceHandler.saveBattle(this, thisBattle.toJSON());
						sendChallengeNotification(name, thisBattle);
					}
				}
			}
			try {
				Thread.sleep(7000);
			} catch(InterruptedException ex) {}
				
		}
	}
	
	
	/* binding not used currently - but might want to use it in future */
	public class MyBinder extends Binder {
		BattleNotifier getService() {
			return BattleNotifier.this;
		}
	}
	
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
}
