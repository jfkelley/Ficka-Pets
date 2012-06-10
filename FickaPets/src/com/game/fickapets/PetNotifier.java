package com.game.fickapets;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class PetNotifier extends IntentService {
	public static int FOREGROUND_NOTIFICATION = 480210;
	private static String TAG = "PetNotifier.java";
	public PetNotifier () {
		super("NotificationService");
	}
	
	@Override
	public IBinder onBind (Intent intent) {
		return null;
	}
	
	public int onStartCommand (Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		/* won't be recreated if killed */
		return START_STICKY;
	}
	
	private void sendNotification (String title, String message, int icon) {
		Intent notificationIntent = new Intent(this, FickaPetsStart.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification notification = new Notification(icon, "", System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(), title, message, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NotificationIdHandler.getNewId(), notification);
	}
	
	private void runInForeground() {
		Notification notification = new Notification(R.drawable.ic_launcher, "FickaPets", System.currentTimeMillis());
		Intent notificaitonIntent = new Intent(this, FickaPetsStart.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificaitonIntent, 0);
		notification.setLatestEventInfo(this, "FickaPets", "FickaPets", pendingIntent);
		startForeground(FOREGROUND_NOTIFICATION, notification);
	}
	
	protected void onHandleIntent (Intent intent) {
		/* looks like android ignores this as old API */
		setForeground(true);
		//runInForeground();	
		Pet myPet = Pet.thePet(this);
		Vector<Complaint> complaints = myPet.getComplaints(this);
		int icon = myPet.getIcon();
		//ArrayList<Complaint> complaints = (ArrayList<Complaint>) intent.getSerializableExtra("com.game.fickapets.complaints");
		
		Collections.sort (complaints, new ComplaintsCompare());
		while (complaints.size() > 0) {
			Complaint complaint = complaints.get(0);
			long complaintId = PersistenceHandler.getComplaintId(this, complaint.getComplaintType());
			try {
				Thread.sleep (Utility.hoursToMillis (complaint.hoursBeforeComplaint));
			} catch (InterruptedException ex) {
				Log.e(TAG, "Thread sleep was interrupted");
				continue;
			}
			if (PersistenceHandler.tryConfirmComplaintSent(this, complaint.getComplaintType(), complaintId)) {
				sendNotification ("FickaPets", complaint.complaint, icon);
			}
			complaints.remove (0);
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
