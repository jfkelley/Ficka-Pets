package com.game.fickapets;



public class Hunger {

	private static final double INCREASE_RATE_AWAKE = (double)15/32;
	private static final double INCREASE_RATE_ASLEEP = (double)5/16;
	private static final double MAX_HUNGER = 100;
	private static final double MIN_HUNGER = 0;
	
	private double currentHunger;
	
	
	public Hunger (Attributes atts) {
		currentHunger = atts.hunger;
	}
	
	public void update (boolean isAwake, double hoursSinceUpdate) {
		if (isAwake) {
			double increase =  INCREASE_RATE_AWAKE * hoursSinceUpdate;
			currentHunger += increase;
		} else {
			currentHunger += INCREASE_RATE_ASLEEP * hoursSinceUpdate;
		}
		if (currentHunger > MAX_HUNGER) {
			currentHunger = MAX_HUNGER;
		} else if (currentHunger < MIN_HUNGER) {
			currentHunger = MIN_HUNGER;
		}
	}
	
	public void feed (double hungerValue) {
		currentHunger -= hungerValue;
	}
	
	public double getHunger () {
		return currentHunger;
	}
	public double getRate (boolean isAwake) {
		if (isAwake) return INCREASE_RATE_AWAKE;
		return INCREASE_RATE_ASLEEP;
	}
	
	public  double hoursUntil (double hungerReached, boolean isAwake) {
		return (hungerReached - getHunger()) / getRate(isAwake);
	}
	
	
}
