package com.server.fickapets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

public class Battle {
	public static final String ENTITY_KIND = "battle";

	private final String user1, user2;
	private String move1, move2;
	private double strength1, strength2;
	private boolean closed1, closed2;
	private Entity entity;

	public Battle(String user1, String user2) {
		this.user1 = user1; this.user2 = user2;
		closed1 = false; closed2 = false;
		move1 = null; move2 = null;
		entity = new Entity(ENTITY_KIND);
		fillEntityProperties();
	}

	public Battle(Entity entity) {
		Map<String, Object> map = entity.getProperties();
		user1 = (String)map.get("user1");
		user2 = (String)map.get("user2");
		move1 = (String)map.get("move1");
		move2 = (String)map.get("move2");
		closed1 = (Boolean)map.get("closed1");
		closed2 = (Boolean)map.get("closed2");
		strength1 = (Double)map.get("strength1");
		strength2 = (Double)map.get("strength2");
		this.entity = entity;
	}

	private void fillEntityProperties() {
		entity.setProperty("user1", user1);
		entity.setProperty("user2", user2);
		entity.setProperty("move1", move1);
		entity.setProperty("move2", move2);
		entity.setProperty("closed1", closed1);
		entity.setProperty("closed2", closed2);
		entity.setProperty("strength1", strength1);
		entity.setProperty("strength2", strength2);
	}

	public void save() {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(entity);
	}

	public String getId() {
		return KeyFactory.keyToString(entity.getKey());
	}
	/* newMove should have format "num1_num2" where num1 is the move number, which starts with zero and num2 is the move */
	private boolean isNewMove(String newMove, String oldMovesStr) {
		if (oldMovesStr == null) return true;
		String[] pieces = newMove.split("_");
		String[] oldMoves = oldMovesStr.split(" ");
		if (Integer.valueOf(pieces[0]) >= oldMoves.length) {
			return true;
		}
		return false;
	}
	
	public boolean setMove(String uid, String move, double strength) {
		if (user1.equals(uid) && isNewMove(move, move1)) {//move1 == null) {
			if (move1 != null) {
				move1 = move1 + " " + move.split("_")[1];
			} else {
				move1 = move.split("_")[1];
			}
			strength1 = strength;
			return true;
		} else if (user2.equals(uid) && isNewMove(move, move2)) {//move2 == null) {
			if (move2 != null) {
				move2 = move2 + " " + move.split("_")[1];
			} else {
				move2 = move.split("_")[1];
			}
			strength2 = strength;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean close(String uid) {
		if (user1.equals(uid) && !closed1) {
			closed1 = true;
			return true;
		} else if (user2.equals(uid) && !closed2) {
			closed2 = true;
			return true;
		}
		return false;
	}
	
	public boolean isClosed() { return closed1 && closed2; }
	
	public boolean deleteIfClosed() {
		if (isClosed()) {
			DatastoreServiceFactory.getDatastoreService().delete(entity.getKey());
			return true;
		} else {
			return false;
		}
	}
	
	public String getXMLDataForUser(String uid) {
		String theirId, myMove, theirMove;
		double myStrength, theirStrength;
		if (user1.equals(uid)) {
			theirId = user2;
			myMove = move1; theirMove = move2;
			myStrength = strength1; theirStrength = strength2;
		} else if (user2.equals(uid)) {
			theirId = user1;
			myMove = move2; theirMove = move1;
			myStrength = strength2; theirStrength = strength1;
		} else {
			return null;
		}
		return
			"<battle>\n" +
				"\t<opponentId>" + theirId + "</opponentId>\n" +
				"\t<userStrength>" + myStrength + "</userStrength>\n" +
				"\t<opponentStrength>" + theirStrength + "</opponentStrength>\n" +
				"\t<userMove>" + myMove + "</userMove>\n" +
				"\t<opponentMove>" + theirMove + "</opponentMove>\n" +
			"</battle>";
	}
	
	public static Battle getById(String id) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity entity;
		try {
			entity = datastore.get(KeyFactory.stringToKey(id));
		} catch (EntityNotFoundException e) {
			return null;
		}
		return new Battle(entity);
	}
	
	public static List<Battle> getOutgoingChallenges(String uid) {
		Query query = new Query(ENTITY_KIND)
			.addFilter("user1", FilterOperator.EQUAL, uid)
			.addFilter("move2", FilterOperator.EQUAL, null);
		return queryToBattles(query);
	}
	
	public static List<Battle> getIncomingChallenges(String uid) {
		Query query = new Query(ENTITY_KIND)
			.addFilter("user2", FilterOperator.EQUAL, uid)
			.addFilter("move2", FilterOperator.EQUAL, null);
		return queryToBattles(query);
	}

	public static List<Battle> getOpenBattles(String uid) {
		Query q1 = new Query(ENTITY_KIND)
			.addFilter("user1", FilterOperator.EQUAL, uid)
			.addFilter("closed1", FilterOperator.EQUAL, false);
		Query q2 = new Query(ENTITY_KIND)
			.addFilter("user2", FilterOperator.EQUAL, uid)
			.addFilter("closed2", FilterOperator.EQUAL, false);
		List<Battle> list = queryToBattles(addCompleteFilter(q1));
		list.addAll(queryToBattles(addCompleteFilter(q2)));
		return list;
	}
	
	private static Query addCompleteFilter(Query q) {
		return q.addFilter("move1", FilterOperator.NOT_EQUAL, null).addFilter("move2", FilterOperator.NOT_EQUAL, null);
	}
	
	private static List<Battle> queryToBattles(Query q) {
		List<Battle> battles =  new ArrayList<Battle>();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		for (Entity entity : pq.asIterable()) {
			battles.add(new Battle(entity));
		}
		return battles;
	}

}
