package com.game.fickapets;

import java.util.Vector;

import android.content.Context;



public class Pet {
	
	private static Pet thePet;
	private static Context lastContext;
	public static Pet thePet(Context context) {
		if (thePet == null) {
			thePet = PersistenceHandler.buildPet(context);
		}
		lastContext = context;
		return thePet;
	}
	public static void reset(Context context) {
		thePet = PersistenceHandler.reset(context);
		lastContext = context;
	}
	private synchronized void petChanged() {
		PersistenceHandler.saveState(lastContext, this);
	}
	
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
		petChanged();
	}
	
	public void wakeUp () {
		condition.update (isAwake);
		isAwake = true;
		petChanged();
	}
	
	public void feed(Food food) {
		condition.petHasEaten(food.getHungerPoints());
		petChanged();
	}
	
	public void feed () {
		double hungerPts = 5;  // for now
		condition.petHasEaten (hungerPts);
		petChanged();
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
	
	public Vector<Complaint> getComplaints (Context context) {
		Vector<Complaint> complaints = new Vector<Complaint>();
		return condition.addComplaints(context, complaints, isAwake);
	}
	
}
