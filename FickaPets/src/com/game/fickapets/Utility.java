package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

/* utility methods that don't really belong to any class */
public class Utility {
	public static long hoursToMillis (double hours) {
		return (long) (hours * 60 * 60 * 1000);
	}
	
	/* Returns the hours between timeInMillis and now */
	public static double hoursSince (long timeInMillis) {
		long diff = Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() - timeInMillis;
		return diff / (double)1000.0 / 60 / 60;
	}
}
