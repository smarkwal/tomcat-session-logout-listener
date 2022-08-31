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
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

	private static final String ENDPOINT_URI = "/session-logout-listener";
	private static final String USERNAME_PARAMETER = "username";

	private static final Log log = LogFactory.getLog(SessionLogoutListener.class);

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		// check if request is sent to session logout endpoint URI
		String requestURI = request.getRequestURI();
		if (requestURI.endsWith(ENDPOINT_URI)) {

			// check if parameter "username" is present
			String[] usernames = request.getParameterValues(USERNAME_PARAMETER);
			if (usernames != null && usernames.length > 0) {

				// logout all users with the given usernames
				logoutUsers(request, usernames);

			}

			// return OK message and stop request processing
			sendResponse(response);
			return;
		}

		// forward request to next valve in the pipeline
		getNext().invoke(request, response);

	}

	private static void logoutUsers(Request request, String[] usernames) {

		if (log.isDebugEnabled()) {
			log.debug("usernames: '" + String.join("', '", usernames) + "'");
		}

		// add all usernames to a set
		Set<String> usernamesSet = new HashSet<>();
		Collections.addAll(usernamesSet, usernames);

		// get all Tomcat sessions for the current webapp context
		Context context = request.getContext();
		Session[] sessions = getAllSessions(context);

		// for every session ...
		for (Session session : sessions) {

			// if session contains a principal (has been authenticated) ...
			Principal principal = session.getPrincipal();
			if (principal != null) {

				// if principal name matches one of the usernames ...
				String principalName = principal.getName();
				if (usernamesSet.contains(principalName)) {

					String sessionId = session.getId();

					// logout the session
					session.expire();

					if (log.isDebugEnabled()) {
						String truncatedSessionId = sessionId.substring(0, 8); // log only first 8 characters of session ID
						log.debug("session: id='" + truncatedSessionId + "...', principal='" + principalName + "'");
					}

				}
			}
		}
	}

	private static Session[] getAllSessions(Context context) {
		Manager manager = context.getManager();
		return manager.findSessions();
	}

	private static void sendResponse(Response response) throws IOException {
		response.setStatus(200);
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().print("OK");
	}

}
