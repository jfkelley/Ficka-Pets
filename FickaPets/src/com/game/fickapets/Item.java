package com.game.fickapets;

public class Item {
	private final String id;
	private final String displayName;
	private final String image;
	private final int price;
	private final String prefix;
	public Item(String id, String name, String image, String prefix, int price) {
		this.id = id;
		this.displayName = name;
		this.image = image;
		this.price = price;
		this.prefix = prefix;
	}
	
	public String getId() { return id; }
	public String getImage() { return image; }
	public String getName() { return displayName; }
	public int getPrice() { return price; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String getGrammaticalName() {
		if (prefix.trim().equals("")) {
			return displayName;
		} else {
			return prefix + displayName;
		}
	}
	
}
