package com.server.fickapets;

public class UserAtts {
	String uid;
	String pet;
	
	public UserAtts() {
		uid = "";
		pet = "";
	}
	
	public UserAtts(String uid, String pet) {
		this.uid = uid;
		this.pet = pet;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		UserAtts atts = (UserAtts)o;
		return this.uid.equals(atts.uid);
	}
}
