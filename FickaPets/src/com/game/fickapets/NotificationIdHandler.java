package com.game.fickapets;

/* makes sure notification ids are unique across services */
public class NotificationIdHandler {
	private static int id = 0;
	
	public static synchronized int getNewId() {
		id += 1;
		return id;
	}
}
