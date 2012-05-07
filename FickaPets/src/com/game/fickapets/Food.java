package com.game.fickapets;

public class Food extends Item {
	private final int health;
	public Food(String id, String image, String name, int price, int health) {
		super(id, image, name, price);
		this.health = health;
	}
	
	public int getHealth() { return health; }
}
