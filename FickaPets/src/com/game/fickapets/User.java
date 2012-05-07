package com.game.fickapets;

import java.util.ArrayList;
import java.util.List;

public class User {

	private int coins;
	private List<Item> inventory;
	
	public User() {
		coins = 1000;
		inventory = new ArrayList<Item>();
	}
	
	public int getCoins() {
		return coins;
	}

	public void setCoins(int coins) {
		this.coins = coins;
	}

	public List<Item> getInventory() {
		return inventory;
	}

	public void setInventory(List<Item> inventory) {
		this.inventory = inventory;
	}
	
	public boolean buyItem(Item item) {
		if (coins > item.getPrice()) {
			coins -= item.getPrice();
			inventory.add(item);
			return true;
		} else {
			return false;
		}
	}
}
