package com.game.fickapets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;


public class Notifier extends IntentService {
	
	public Notifier () {
		super("NotificationService");
	}
	
	@Override
	public IBinder onBind (Intent intent) {
		return null;
	}
	
	public int onStartCommand (Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_REDELIVER_INTENT;
	}
	
	private void sendNotification (String title, String message, int id) {
		Intent notificationIntent = new Intent(this, FickaPetsStart.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification notification = new Notification(R.drawable.ic_launcher, "", System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(), title, message, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(id, notification);
	}
	
	protected void onHandleIntent (Intent intent) {

		setForeground(true);
			
			
		@SuppressWarnings("unchecked")
		ArrayList<Complaint> complaints = (ArrayList<Complaint>) intent.getSerializableExtra("com.game.fickapets.complaints");
		
		Collections.sort (complaints, new ComplaintsCompare());
		int id = 0;
		while (complaints.size() > 0) {
			try {
				Thread.sleep (Utility.hoursToMillis (complaints.get (0).hoursBeforeComplaint));
			} catch (Exception ex) {
				System.out.println ("Couldn't put Notifier service to sleep");
				ex.printStackTrace ();
			}
			sendNotification ("FickaPets", complaints.get(0).complaint, id);
			complaints.remove (0);
			id++;
		}
	}
	
	public class ComplaintsCompare implements Comparator<Complaint> {
		public int compare(Complaint c1, Complaint c2) {
			double comp = c1.hoursBeforeComplaint - c2.hoursBeforeComplaint;
			if (comp < 0) {
				return -1;
			} else if (comp != 0) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
