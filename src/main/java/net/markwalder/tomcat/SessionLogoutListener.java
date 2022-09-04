/*
 * MIT License
 *
 * Copyright (c) 2022 Stephan Markwalder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.markwalder.tomcat;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class SessionLogoutListener extends ValveBase {

	private final Log log;

	// TODO: inject as dependencies
	private final Predicate<Request> interceptor = new RequestInterceptor();
	private final Predicate<Request> accessCheck = new AccessCheck();
	private final Function<Request, Set<String>> requestParser = new RequestParser();

	public SessionLogoutListener() {
		this(LogFactory.getLog(SessionLogoutListener.class));
	}

	// visible for testing
	SessionLogoutListener(Log log) {
		this.log = log;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		// check if request is sent to session logout endpoint
		if (interceptor.test(request)) {

			handleRequest(request, response);
			return;
		}

		// forward request to next valve in the pipeline
		getNext().invoke(request, response);

	}

	private void handleRequest(Request request, Response response) throws IOException {

		// check if request is authenticated and authorized
		if (!accessCheck.test(request)) {
			sendResponse(403, "Forbidden", response);
			return;
		}

		// get usernames from request
		Set<String> usernames = requestParser.apply(request);
		if (!usernames.isEmpty()) {

			// logout all users with the given usernames
			Context context = request.getContext();
			logoutUsers(context, usernames);

		}

		// return OK message and stop request processing
		sendResponse(200, "OK", response);
	}

	private void logoutUsers(Context context, Set<String> usernames) {

		if (log.isDebugEnabled()) {
			log.debug("usernames: '" + String.join("', '", usernames) + "'");
		}

		// get all Tomcat sessions for the current webapp context
		Session[] sessions = getAllSessions(context);

		// for every session ...
		for (Session session : sessions) {

			// if the session is still valid ...
			if (session.isValid()) {

				// if session contains a principal (has been authenticated) ...
				Principal principal = session.getPrincipal();
				if (principal != null) {

					// if principal name matches one of the usernames ...
					String principalName = principal.getName();
					if (usernames.contains(principalName)) {

						// remember session ID
						String sessionId = session.getId();

						// logout the session
						session.expire();

						if (log.isDebugEnabled()) {
							String truncatedSessionId = truncateSessionId(sessionId); // log only first 8 characters of session ID
							log.debug("session: id='" + truncatedSessionId + "...', principal='" + principalName + "'");
						}

					}
				}
			}
		}
	}

	private static Session[] getAllSessions(Context context) {
		Manager manager = context.getManager();
		return manager.findSessions();
	}

	public static String truncateSessionId(String sessionId) {
		return sessionId.substring(0, 8);
	}

	private static void sendResponse(int status, String message, Response response) throws IOException {
		response.setStatus(status);
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		PrintWriter writer = response.getWriter();
		writer.print(message);
	}

}
