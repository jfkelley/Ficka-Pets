package com.server.fickapets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BattleServlet extends HttpServlet {

	private static final long serialVersionUID = 6227254987843138881L;

	public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String action = req.getRequestURI().toLowerCase();
		if (action.equals("/create")) {
			createBattle(resp, req.getParameter("uid1"), req.getParameter("uid2"));
		} else if (action.equals("/sendmove")) {
			doMove(resp, req.getParameter("uid"), req.getParameter("bid"), req.getParameter("move"), Double.parseDouble(req.getParameter("strength")));
		} else if (action.equals("/openbattles") || action.equals("outgoingchallenges") || action.equals("incomingchallenges")) {
			handleBattleQuery(resp, action, req.getParameter("uid"));
		} else if (action.equals("/closebattle")) {
			closeBattle(resp, req.getParameter("uid"), req.getParameter("bid"));
		} else if (action.equals("/battledata")) {
			battleData(resp, req.getParameter("uid"), req.getParameter("bid"));
		} else if (action.equals("/registeruser")) {
			registerUser(resp, req.getParameter("id"));
		} else if (action.equals("/findfriends")) {
			String content = streamToString(req.getInputStream());
			findFriends(resp, content);
		}
	}
	
	private void registerUser(HttpServletResponse resp, String id) throws IOException {
		if (checkNonNullParams(resp, id)) return;
		User.create(id);
		resp.setStatus(200);
	}
	
	private static class Friend {
		private String name;
		private String id;
		public Friend(String name, String id){
			this.name = name; this.id = id;
		}
	}

	/*
	 *  JSON parsing/encoding by hand here :(
	 *  Many JSON libraries simply don't work with GAE, and the one that I could find that does,
	 *  doesn't support encoding well, only decoding.
	 */
	
	private void findFriends(HttpServletResponse resp, String data) throws IOException {
		if (checkNonNullParams(resp, data)) return;
		List<Friend> friends = filterNotUsingApp(fromJSON(data));
		PrintWriter wr = resp.getWriter();
		wr.write(toJSON(friends));
		wr.close();
	}
	
	private static final String NAME_PATTERN = "\"name\":\\s*\"([^\"]+)\"";
	private static final String ID_PATTERN = "\"id\":\\s*\"([^\"]+)\"";
	
	private static final Pattern JSON_FRIEND_PATTERN = Pattern.compile(NAME_PATTERN + "\\s*,\\s*" + ID_PATTERN);

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		String line;
		while (!(line = s.nextLine()).equals("")) {
			Matcher m = JSON_FRIEND_PATTERN.matcher(line);
			if (m.find()) {
				System.out.println(m.group(0));
				System.out.println(m.group(1));
				System.out.println(m.group(2));
			} else {
				System.out.println("not found");
			}
		}
	}
	
	private List<Friend> fromJSON(String data) {
		Matcher m = JSON_FRIEND_PATTERN.matcher(data);
		List<Friend> friends = new ArrayList<Friend>();
		while (m.find()) {
			String name = m.group(1);
			String friendId = m.group(2);
			friends.add(new Friend(name, friendId));
		}
		return friends;
	}
	
	private List<Friend> filterNotUsingApp(List<Friend> allFriends) {
		List<String> ids = new ArrayList<String>();
		for (Friend f : allFriends) {
			ids.add(f.id);
		}
		List<String> keep = User.filterNonexisting(ids);
		List<Friend> usingApp = new ArrayList<Friend>();
		for (Friend f : allFriends) {
			if (keep.contains(f.id)) usingApp.add(f);
		}
		return usingApp;
	}
	
	private String toJSON(List<Friend> friends) {
		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		for (int i = 0; i < friends.size(); i++) {
			Friend f = friends.get(i);
			sb.append(String.format("\t{\n\t\t\"name\": \"%s\",\n\t\t\"id\": \"%s\"\n\t}", f.name, f.id));
			if (i != friends.size() - 1) sb.append(",");
			sb.append("\n");
		}
		sb.append("]");
		return sb.toString();
	}

	private void battleData(HttpServletResponse resp, String uid, String bid) throws IOException {
		if (checkNonNullParams(resp, uid, bid)) return;
		Battle b = Battle.getById(bid);
		if (b == null) {
			resp.sendError(400, "No battle with bid: " + bid);
		} else {
			String xml = b.getXMLDataForUser(uid);
			if (xml == null) {
				resp.sendError(400, "User with uid: " + uid + " is not a particpant in battle with bid: " + bid);
			} else {
				resp.setStatus(200);
				PrintWriter wr = resp.getWriter();
				wr.write(xml);
				wr.close();
			}
		}
	}

	private void handleBattleQuery(HttpServletResponse resp, String action, String uid) throws IOException {
		if (checkNonNullParams(resp, uid)) return;
		List<Battle> battles;
		if (action.equals("openbattles")) {
			battles = Battle.getOpenBattles(uid);
		} else if (action.equals("outgoingchallenges")) {
			battles = Battle.getOutgoingChallenges(uid);
		} else {
			battles = Battle.getIncomingChallenges(uid);
		}
		resp.setStatus(200);
		PrintWriter wr = resp.getWriter();
		for (int i = 0; i < battles.size(); i++) {
			wr.write(battles.get(i).getId());
			if (i != battles.size() - 1) wr.write("\n");
		}
		wr.close();
	}

	private void closeBattle(HttpServletResponse resp, String uid, String bid) throws IOException {
		if (checkNonNullParams(resp, uid, bid)) return;
		Battle battle = Battle.getById(bid);
		if (battle == null) {
			resp.sendError(400, "No battle with bid: " + bid);
		} else {
			if (battle.close(uid)) {
				if (!battle.deleteIfClosed()) battle.save();
				resp.setStatus(200);
			} else {
				resp.sendError(400, "User with uid: " + uid + " is not a particpant in battle with bid: " + bid + ", or that player has already closed that battle.");
			}
		}
	}


	private void doMove(HttpServletResponse resp, String uid, String bid, String move, Double strength) throws IOException {
		if (checkNonNullParams(resp, uid, bid, move, strength)) return;
		Battle battle = Battle.getById(bid);
		if (battle == null) {
			resp.sendError(400, "No battle with bid: " + bid);
		} else {
			if (battle.setMove(uid, move, strength)) {
				battle.save();
				resp.setStatus(200);
			} else {
				resp.sendError(400, "User with uid: " + uid + " is not a particpant in battle with bid: " + bid + ", or that player has already made a move in that battle.");
			}
		}
	}

	private void createBattle(HttpServletResponse resp, String uid1, String uid2) throws IOException {
		if (checkNonNullParams(resp, uid1, uid2)) return;
		Battle b = new Battle(uid1, uid2);
		b.save();
		resp.setStatus(200);
		resp.getWriter().write(b.getId());
	}
	
	private boolean checkNonNullParams(HttpServletResponse resp, Object... params) throws IOException {
		for (Object o : params) {
			if (o == null) {
				resp.sendError(400, "missing one or more parameters");
				return true;
			}
		}
		return false;
	}
	
	private static String streamToString(InputStream in) throws IOException {
		InputStreamReader rd = new InputStreamReader(in, "UTF-8");
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[0x1000];
		int n;
		while ((n = rd.read(buf)) != -1) {
			sb.append(Arrays.copyOfRange(buf, 0, n));
		}
		return sb.toString();
	}
}