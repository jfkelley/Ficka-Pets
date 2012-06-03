package com.game.fickapets;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class User {
	
	private static User theUser;
	private static Context lastContext;
	private int coins;
	private List<Item> inventory;
	
	public static User theUser(Context context) {
		if (theUser == null) {
			theUser = PersistenceHandler.buildUser(context);
		}
		lastContext = context;
		return theUser;
	}
	public static void reset(Context context) {
		theUser.coins = 1000;
		theUser.inventory = new ArrayList<Item>();
		lastContext = context;
		theUser.userChanged();
	}
	private synchronized void userChanged() {
		PersistenceHandler.saveState(lastContext, this);
	}

	public User() {
		this(1000, new ArrayList<Item>());
	}
	
	public User(int coins, List<Item> inventory) {
		this.coins = coins;
		this.inventory = inventory;
	}
	
	public int getCoins() {
		return coins;
	}

	public void setCoins(int coins) {
		this.coins = coins;
		userChanged();
	}
	
	public void addCoins(int coins) {
		setCoins(getCoins() + coins);
	}
	
	public void addCoin() {
		addCoins(1);
	}

	public List<Item> getInventory() {
		return inventory;
	}
	
	public List<Food> getUniqueFood() {
		List<Food> list = new ArrayList<Food>();
		for (Item item : inventory) {
			if ((item instanceof Food) && !list.contains(item)) list.add((Food)item);
		}
		return list;
	}

	public void setInventory(List<Item> items) {
		inventory = new ArrayList<Item>();
		for (Item item : items) {
			if (item != null) inventory.add(item);
		}
		userChanged();
	}
	
	public boolean buyItem(Item item) {
		if (coins > item.getPrice()) {
			coins -= item.getPrice();
			inventory.add(item);
			userChanged();
			return true;
		} else {
			return false;
		}
	}
	
	public void feedPet(Pet pet, Food food) {
		if (inventory.remove(food)) {
			pet.feed(food);
			userChanged();
		}
	}
}
