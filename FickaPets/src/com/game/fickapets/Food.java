package com.game.fickapets;

public class Food extends Item {
	private final int hungerPoints;
	public Food(String id, String image, String name, int price, int hungerPoints) {
		super(id, image, name, price);
		this.hungerPoints = hungerPoints;
	}
	
	public int getHungerPoints() { return hungerPoints; }
}
