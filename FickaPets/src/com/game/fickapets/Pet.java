package com.game.fickapets;



public class Pet {
	private Condition condition;
	//private double boredom;
	
	private boolean isAwake;
	
	public Pet() {
		condition = new Condition();
		isAwake = true;
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
		return condition.fillAttributes (atts, isAwake);
	}
	
}
