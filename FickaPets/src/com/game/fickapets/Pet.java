package com.game.fickapets;



public class Pet {
	private Condition condition;
	//private double boredom;
	
	private boolean isAwake;
	
	public Pet(Attributes atts) {
		condition = new Condition(atts);
		isAwake = atts.isAwake;
	}
	
	public void putToSleep () {
		condition.update (isAwake);
		isAwake = false;
	}
	
	public void wakeUp () {
		condition.update (isAwake);
		isAwake = true;
	}
	public void feed () {
		double hungerPts = 10;  // for now
		condition.petHasEaten (hungerPts);
	}
	
	public boolean isSleeping () {
		if (isAwake) return false;
		return true;
	}
	
	public Attributes getAttributes () {
		Attributes atts = new Attributes ();
		atts.isAwake = isAwake;
		return condition.fillAttributes (atts, isAwake);
	}
	
}
