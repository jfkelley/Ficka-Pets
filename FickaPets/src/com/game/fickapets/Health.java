package com.game.fickapets;


import java.util.Vector;

import android.content.Context;

public class Health {
	
	private double currentHealth;
	
	private Hunger hunger;
	private Tiredness tiredness;
	
	/* values that determine how hunger effects health */
	private static final double extremeHunger = 95;
	private static final double extremeHungerRate = -1.5;
	private static final double hotSpot = 50;				/* where you ideally want to keep your pet for maximum health */
	private static final double positiveRange = 10;			/* max distance from hotspot before negative impact on health */
	
	
	private static final double generallyTired = 32;
	/* values that determine how tiredness effects health */
	private static final double extremeTiredness = 60;
	private static final double extremeTirednessRate = -2;
	
	private static final double MAX_HEALTH = 100;
	private static final double MIN_HEALTH = 0;

	public Health (Attributes atts) {
		currentHealth = atts.health;
		hunger = new Hunger (atts);
		tiredness = new Tiredness (atts);
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
		double splitTime = hoursSinceUpdate;
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
	

	
	/* Updates health, then updates hunger and tiredness.  Important that hunger and tiredness are updated after health since their old values are
	 * used in those equations */
	public void update (boolean isAwake, Double hoursSinceUpdate) {
		currentHealth += hungerEffectOnHealth (hoursSinceUpdate, isAwake);
		currentHealth += tiredEffectOnHealth (hoursSinceUpdate, isAwake);
		if (currentHealth > MAX_HEALTH) {
			currentHealth = MAX_HEALTH;
		} else if (currentHealth < MIN_HEALTH) {
			currentHealth = MIN_HEALTH;
		}
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
	
	private Vector<Complaint> addHungerComplaints(Context context, Vector<Complaint> complaintVec, boolean isAwake) {
		double hoursUntilNegative = hunger.hoursUntil(hotSpot+positiveRange, isAwake);
		if (hoursUntilNegative > 0) {
			Complaint negativeHunger = new Complaint ();
			negativeHunger.hoursBeforeComplaint = hoursUntilNegative;
			negativeHunger.complaint = context.getString(R.string.petNeedsFood);
			complaintVec.add(negativeHunger);
		}
		
		double hoursUntilVeryNegative = hunger.hoursUntil(extremeHunger, isAwake);
		if (hoursUntilVeryNegative > 0) {
			Complaint veryHungry = new Complaint ();
			veryHungry.hoursBeforeComplaint = hoursUntilVeryNegative;
			veryHungry.complaint = context.getString(R.string.petStarving);
			complaintVec.add(veryHungry);
		}
		return complaintVec;
	}
	
	private Vector<Complaint> addTirednessComplaints(Context context, Vector<Complaint> complaintVec, boolean isAwake) {
		
		double hoursUntilTired = tiredness.hoursUntil(generallyTired, isAwake);
		if (hoursUntilTired > 0) {
			Complaint tired = new Complaint();
			tired.hoursBeforeComplaint = hoursUntilTired;
			tired.complaint = context.getString(R.string.petTired);
			complaintVec.add(tired);
		}
		
		double hoursUntilVeryTired = tiredness.hoursUntil(extremeTiredness, isAwake);
		if (hoursUntilVeryTired > 0) {
			Complaint veryTired = new Complaint();
			veryTired = new Complaint();
			veryTired.hoursBeforeComplaint = hoursUntilVeryTired;
			veryTired.complaint = context.getString(R.string.petVeryTired);
			complaintVec.add(veryTired);
		}
		return complaintVec;
	}
	
	public Vector<Complaint> addComplaints (Context context, Vector<Complaint> complaintVec, boolean isAwake) {
		if (complaintVec == null) complaintVec = new Vector<Complaint>();
		
		complaintVec = addHungerComplaints(context, complaintVec, isAwake);
		complaintVec = addTirednessComplaints(context, complaintVec, isAwake);
		return complaintVec;
	}
	
	public boolean isHungry() {
		return hunger.getHunger() > hotSpot + positiveRange;
	}
	
	public boolean isFull() {
		return hunger.getHunger() < hotSpot - positiveRange;
	}
	
	public boolean isTired() {
		return tiredness.getTiredness() > generallyTired;
	}
	
	
}
