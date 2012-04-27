package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;



public class Tiredness {
	private static final double INCREASE_RATE = (double)95 / 48;
	private static final double DECREASE_CONSTANT = -0.432;
	
	private static final int INITIAL_SLEEP_TIME = 22;    //on 24 hour clock: 10pm.  Only used once when game starts
	private static final int HOURS_PER_SLEEP = 8;
	private static final double MIN_TIREDNESS_AFTER_ONE_DAY = INCREASE_RATE * (24 - HOURS_PER_SLEEP);
	
	private static double currentTiredness;
	
	public Tiredness () {
		int currentHour;
		int currentMinute;
		double hoursUntilSleep;
		
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		currentHour = cal.get(Calendar.HOUR_OF_DAY);
		currentMinute = cal.get(Calendar.MINUTE);
		hoursUntilSleep = INITIAL_SLEEP_TIME - currentHour;
		if (hoursUntilSleep < 0) hoursUntilSleep = 24 - currentHour + INITIAL_SLEEP_TIME;
		
		hoursUntilSleep -= (double)currentMinute / 60;
		
		
		currentTiredness = MIN_TIREDNESS_AFTER_ONE_DAY - (INCREASE_RATE * hoursUntilSleep);
	}
	
	public double getTiredness () {
		return currentTiredness;
	}
	

	/* needs to be updated before isAwake is changed. */
	public void update (boolean isAwake, double hoursSinceUpdate) {
		if (isAwake) {
			currentTiredness += hoursSinceUpdate * INCREASE_RATE;
		} else {
			currentTiredness = Math.exp (DECREASE_CONSTANT * hoursSinceUpdate + Math.log (currentTiredness));
		}
	}
	
	public double hoursUntil (double tirednessReached, boolean isAwake) {
		if (isAwake) {
			return (tirednessReached - currentTiredness) / INCREASE_RATE;
		} else {
			return (Math.log(tirednessReached) - Math.log(currentTiredness)) / DECREASE_CONSTANT;
		}
	}
	
	
	
}
