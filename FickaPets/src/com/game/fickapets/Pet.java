package com.game.fickapets;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;



public class Pet {
	
	private static Pet thePet;
	private static Context lastContext;
	public static Pet thePet(Context context) {
		if (thePet == null) {
			thePet = PersistenceHandler.loadSavedPet(context);
		}
		/*
		while ((thePet = PersistenceHandler.loadSavedPet(context)) == null) {
			Intent intent = new Intent(context, ChoosePetActivity.class);
			context.startActivity(intent);
		}
		*/
		lastContext = context;
		return thePet;
	}
	public static void reset(Context context) {
		thePet = PersistenceHandler.reset(context);
		lastContext = context;
	}
	
	private Condition condition;
	//private double boredom;
	
	private boolean isAwake;
	
	private Set<PetListener> listeners;
	
	private int type = 1; // which pet (just matters for images
	
	public Pet(Attributes atts) {
		condition = new Condition(atts);
		isAwake = atts.isAwake;
		listeners = new HashSet<PetListener>();
		listeners.add(new PetListener() {
			public void petChanged() {
				PersistenceHandler.saveState(lastContext, Pet.this);
			}
		});
		condition.addListener(new ConditionListener() {
			public void conditionChanged(Condition condition) {
				notifyListeners();
			}
		});
	}
	
	public Pet(Attributes atts, int type) {
		this(atts);
		this.type = type;
	}
	
	private void notifyListeners() {
		for (PetListener listener : listeners) {
			listener.petChanged();
		}
	}
	
	public void addListener(PetListener listener) {
		listeners.add(listener);
	}
	
	public void putToSleep () {
		condition.update (isAwake);
		isAwake = false;
		notifyListeners();
	}
	
	public void wakeUp () {
		condition.update (isAwake);
		isAwake = true;
		notifyListeners();
	}
	
	public void feed(Food food) {
		condition.petHasEaten(food.getHungerPoints());
		notifyListeners();
	}
	
	public boolean isSleeping () {
		if (isAwake) return false;
		return true;
	}
	
	public Attributes getAttributes(boolean updateBefore) {
		Attributes atts = new Attributes ();
		atts.isAwake = isAwake;
		return condition.fillAttributes (atts, isAwake, updateBefore);
	}
	
	public Attributes getAttributes () {
		return getAttributes(true);
	}
	
	public Vector<Complaint> getComplaints (Context context) {
		Vector<Complaint> complaints = new Vector<Complaint>();
		return condition.addComplaints(context, complaints, isAwake);
	}
	public boolean isHungry() {
		return condition.getHealth().isHungry();
	}
	public boolean isFull() {
		return condition.getHealth().isFull();
	}
	public boolean isTired() {
		return condition.getHealth().isTired();
	}
	
	public int getType() {
		return type;
	}
	
	private static final String PACKAGE_NAME = "com.game.fickapets";
	private static final String DRAWABLE_DEFTYPE = "drawable";
	
	public int getStateImage(Context context) {
		String str = "pet_";
		str += type;
		
		if (!isAwake) {
			str += "_asleep";
		} else if (isTired()) {
			str += "_tired";
		} else {
			str += "_awake";
		}
		
		if (isHungry()) {
			str += "_hungry";
		} else if (isFull()) {
			str += "_full";
		} else {
			str += "_normal";
		}
		
		return getImage(context, str);
	}
	
	public int getDefaultImage(Context context) {
		return getImage(context, "pet_" + type + "_default");
	}
	
	public int getSmallImage(Context context) {
		return getImage(context, "pet_" + type + "_small");
	}
	
	private int getImage(Context context, String name) {
		return context.getResources().getIdentifier(name, DRAWABLE_DEFTYPE, PACKAGE_NAME);
	}
	
}
