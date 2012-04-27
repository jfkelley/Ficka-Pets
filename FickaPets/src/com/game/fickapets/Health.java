package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

public class Health {
	
	private Hunger hunger;
	private Tiredness tiredness;
	
	/* values that determine how hunger effects health */
	private static final double extremeHunger = 95;
	private static final double extremeHungerRate = -1.5;
	private static final double hotSpot = 50;				/* where you ideally want to keep your pet for maximum health */
	private static final double positiveRange = 30;			/* max distance from hotspot before negative impact on health */
	
	/* values that determine how tiredness effects health */
	private static final double extremeTiredness = 95;
	private static final double extremeTirednessRate = -25;
	
	private double currentHealth;
	private long lastUpdate;
	
	
	public Health () {
		currentHealth = 90;
		hunger = new Hunger ();
		tiredness = new Tiredness ();
	}
	
	/*
	 * Math for getting health:  x = hunger, t = time, y = health
	 * dy/dt = 1 - ((x(t)-50)/30) if x(t) > 50; x(t) = x_current + rate*t
	 * y = integral (dy) = (1+50/30)t - rate*t^2/60 - x_current*t/30
	 * 
	 * if x(t) < 50, dy/dt = 1 - ((50-x(t))/30)
	 * y = integral (dy) = (1-50/30)t + rate*t^2/60 + x_current*t/30
	 * 
	 * It's a little different when x(t) < 50, so I find hunger effect up to fifty, then, if reached, add
	 * the effect after fifty up to extremeHunger point.  Then add extremeHunger effect.
	 */
	private double hungerEffectOnHealth (double hoursSinceUpdate, boolean isAwake) {
		double currentHunger = hunger.getHunger();
		double splitTime = hoursSinceUpdate;	/* time until equation changes to reflect absolute value or extreme hunger rate */
		double effect = 0;
		
		if (currentHunger < hotSpot) {
			double time = hunger.hoursUntil (hotSpot, isAwake)   ; //hours until hunger reaches 50
			if (time < hoursSinceUpdate) {
				splitTime = time;
			}
			effect += (1 - (hotSpot/positiveRange) + (hunger.getRate(isAwake)*splitTime/(positiveRange*2)) + (currentHunger/positiveRange)) * splitTime;
			hoursSinceUpdate -= splitTime;
			currentHunger += hunger.getRate (isAwake) * splitTime;
		}
		if (currentHunger < extremeHunger && hoursSinceUpdate > 0) {
			double time = hunger.hoursUntil(extremeHunger, isAwake);
			splitTime = hoursSinceUpdate;
			if (time < hoursSinceUpdate) {
				splitTime = time;
			}
			effect += (1 + (hotSpot/positiveRange) - (hunger.getRate(isAwake)*splitTime/(positiveRange*2)) - currentHunger/positiveRange) * splitTime;
			hoursSinceUpdate -= splitTime;
			currentHunger += hunger.getRate (isAwake) * splitTime;
		}
		if (hoursSinceUpdate > 0) {
			effect += extremeHungerRate * hoursSinceUpdate;
		}
		return effect;
	}
	/* find out if tiredness will effect health in this update.  If it will, skip forward to the point
	 * when tiredness starts effecting health and add it to the health score.
	 */
	private double tiredEffectOnHealth (double hoursSinceUpdate, boolean isAwake) {
		double splitTime = tiredness.hoursUntil (extremeTiredness, isAwake);
		if (splitTime > 0 && splitTime < hoursSinceUpdate) {
			hoursSinceUpdate -= splitTime;
			return extremeTirednessRate * hoursSinceUpdate;
		}
		return 0;
	}
	
	/* Returns the hours from lastUpdate to current time */
	public static double getHoursSinceUpdate (long lastUpdate) {
		long diff = Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() - lastUpdate;
		return diff / (double)1000.0 / 60 / 60;
	}
	
	/* Updates health, then updates hunger and tiredness.  Important that hunger and tiredness are updated after health since their old values are
	 * used in those equations */
	public void update (boolean isAwake, Double hoursSinceUpdate) {
		if (hoursSinceUpdate == null) hoursSinceUpdate = Health.getHoursSinceUpdate(lastUpdate);
		currentHealth += hungerEffectOnHealth (hoursSinceUpdate, isAwake);
		currentHealth += tiredEffectOnHealth (hoursSinceUpdate, isAwake);

		hunger.update (isAwake, hoursSinceUpdate);
		tiredness.update (isAwake, hoursSinceUpdate);
	}
	
	public double getHealth () {
		return currentHealth;
	}
	
	public void petHasEaten (double hungerPts) {
		hunger.feed (hungerPts);
	}
	
	
	public Attributes fillAttributes (Attributes atts) {
		atts.health = currentHealth;
		atts.hunger = hunger.getHunger ();
		atts.tiredness = tiredness.getTiredness ();
		return atts;
	}
	
	
}
