package com.game.fickapets;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import android.content.Context;

public class Condition {
	private long lastUpdate;
	private double currentStrength;
	
	private Health health;
	
	private static final double UPDATE_INCREMENT = 0.5;  //in hours
	
	private Set<ConditionListener> listeners;
	
	public Condition (Attributes atts) {
		lastUpdate = atts.lastUpdate;
		currentStrength = 1;
		health = new Health (atts);
		listeners = new HashSet<ConditionListener>();
	}
	
	private void notifyListeners() {
		for (ConditionListener listener : listeners) {
			listener.conditionChanged(this);
		}
	}
	
	public void addListener(ConditionListener listener) {
		listeners.add(listener);
	}
	
	/*
	 * Just sample the health every half hour and use that to update the strength.
	 * For every sample, the tiredness, hunger, and health get updated.
	 */
	public void update (boolean isAwake) {
		Double hoursSinceUpdate = Utility.hoursSince (lastUpdate);
		lastUpdate = Calendar.getInstance(TimeZone.getDefault ()).getTimeInMillis ();
		double updateCounter;
		/* calculate strength and update attributes in half hour increments */
		for (updateCounter = UPDATE_INCREMENT; updateCounter < hoursSinceUpdate; updateCounter += UPDATE_INCREMENT) {
			health.update (isAwake, UPDATE_INCREMENT);
			currentStrength += (health.getHealth () / 100.0) * UPDATE_INCREMENT;
		}
		double hoursLeft = UPDATE_INCREMENT - (updateCounter - hoursSinceUpdate);
		health.update(isAwake, hoursLeft);
		currentStrength += (health.getHealth () / 100.0) * hoursLeft;
		notifyListeners();
	}
	
	public double getBattleEffectiveness (boolean isAwake) {
		update (isAwake);
		Attributes atts = new Attributes();
		health.fillAttributes(atts);
		return currentStrength * (1 + atts.health/300) * (1 - atts.tiredness/300);
	}
	
	public void petHasEaten (double hungerPts) {
		health.petHasEaten (hungerPts);
		notifyListeners();
	}
	
	public Attributes fillAttributes (Attributes atts, boolean isAwake) {
		return fillAttributes(atts, isAwake, true);
	}
	
	public Attributes fillAttributes (Attributes atts, boolean isAwake, boolean updateBefore) {
		if (updateBefore) update (isAwake);
		
		atts = health.fillAttributes (atts);
		atts.strength = currentStrength;
		atts.lastUpdate = lastUpdate;
		return atts;
	}
	
	public Vector<Complaint> addComplaints (Context context, Vector<Complaint> complaintVec, boolean isAwake) {
		return health.addComplaints(context, complaintVec, isAwake);
	}
	
	public Health getHealth() {
		return health;
	}
	
}
