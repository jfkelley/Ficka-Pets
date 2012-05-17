package com.game.fickapets;

public class CacheEntry {
	String name;
	Integer bytes;
	public CacheEntry(String name, int bytes) {
		this.name = name;
		this.bytes = bytes;
	}
	
	@Override
	public boolean equals(Object otherObject) {
		CacheEntry otherEntry;
		if (otherObject instanceof CacheEntry) {
			otherEntry = (CacheEntry) otherObject;
			if (otherEntry.name.equals(name)) return true;
		} 
		return false;
	}
}
