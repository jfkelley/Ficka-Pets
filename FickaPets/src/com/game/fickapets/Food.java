package com.game.fickapets;

public class Food extends Item {
	private final int hungerPoints;
	public Food(String id, String name, String image, String prefix, int price, int hungerPoints) {
		super(id, name, image, prefix, price);
		this.hungerPoints = hungerPoints;
	}
	
	public int getHungerPoints() { return hungerPoints; }
}
