package com.server.fickapets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class AdminServlet extends HttpServlet {
	
	private static final List<String> trustedAccounts =
			Arrays.asList(new String[]{"joefkelley@gmail.com", "ctmckenna23@gmail.com"});

	private static final long serialVersionUID = -5356802980456794307L;

	public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		String thisUrl = req.getRequestURI();
		if (userService.getCurrentUser() != null) {
			if (trustedAccounts.contains(userService.getCurrentUser().getEmail().toLowerCase())) {
				serviceTrusted(req, resp);
			} else {
				resp.sendError(403, "only admins can do that, " + userService.getCurrentUser().getEmail());
			}
		} else {
			resp.sendRedirect(userService.createLoginURL(thisUrl));
		}
	}

	private void serviceTrusted(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String action = req.getRequestURI().toLowerCase();
		if (action.equals("/clear")) {
			clear(resp);
		}
	}

	private void clear(HttpServletResponse resp) throws IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		clearEntitiesOfKind(Battle.ENTITY_KIND, datastore);
		clearEntitiesOfKind(User.ENTITY_KIND, datastore);
		PrintWriter wr = resp.getWriter();
		wr.write("<html><body><h1>Datastore cleared.</h1></body></html>");
		wr.close();
	}

	private void clearEntitiesOfKind(String entityKind, DatastoreService datastore) {
		Query q = new Query(entityKind).setKeysOnly();
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e : pq.asIterable()) {
			datastore.delete(e.getKey());
		}
	}

}
