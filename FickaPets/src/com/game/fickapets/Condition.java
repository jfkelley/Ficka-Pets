package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

public class Condition {
	private long lastUpdate;
	private double currentStrength;
	
	private Health health;
	
	private static final double UPDATE_INCREMENT = 0.5;  //hours before strength updates by polling health
	
	public Condition () {
		lastUpdate = Calendar.getInstance(TimeZone.getDefault ()).getTimeInMillis ();
		currentStrength = 1;
		health = new Health ();
	}
	
	/*
	 * Instead of trying to do more math, I just sample the health every half hour and use that to update the strength.
	 * For every sample, the tiredness, hunger, and health get updated.
	 */
	public void update (boolean isAwake) {
		Double hoursSinceUpdate = Health.getHoursSinceUpdate(lastUpdate);
		lastUpdate = Calendar.getInstance(TimeZone.getDefault ()).getTimeInMillis ();
		double updateCounter;
		/* calculate strength and update attributes in half hour increments */
		for (updateCounter = UPDATE_INCREMENT; updateCounter < hoursSinceUpdate; updateCounter += UPDATE_INCREMENT) {
			health.update (isAwake, UPDATE_INCREMENT);
			currentStrength += (health.getHealth () / 100) * UPDATE_INCREMENT;
		}
		double hours = UPDATE_INCREMENT - (updateCounter - hoursSinceUpdate);
		health.update(isAwake, hours);
		currentStrength += (health.getHealth () / 100) * hours;
	}
	
	public double getBattleEffectiveness (boolean isAwake) {
		update (isAwake);
		Attributes atts = new Attributes();
		health.fillAttributes(atts);
		return currentStrength * (1 + atts.health/300) * (1 - atts.tiredness/300);
	}
	
	public void petHasEaten (double hungerPts) {
		health.petHasEaten (hungerPts);
	}
	
	public Attributes fillAttributes (Attributes atts, boolean isAwake) {
		update (isAwake);
		
		atts = health.fillAttributes (atts);
		atts.strength = currentStrength;
		return atts;
	}
	
}
