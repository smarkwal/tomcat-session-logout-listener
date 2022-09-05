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
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.juli.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionLogoutListenerTest {

	@Mock
	Request request;

	@Mock
	Response response;

	@Mock
	PrintWriter writer;

	@Mock
	Context context;

	@Mock
	Manager manager;

	@Mock
	Session session;

	@Mock
	Principal principal;

	@Mock
	Log log;

	SessionLogoutListener listener;

	@Mock
	Valve next;

	@BeforeEach
	void setUp() {
		listener = new SessionLogoutListener(log);
		listener.setNext(next);
	}

	@Test
	void invoke() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();
		Mockito.doReturn(new String[] { "alice", "bob" }).when(request).getParameterValues("username");
		Mockito.doReturn(true).when(log).isDebugEnabled();
		Mockito.doReturn(context).when(request).getContext();
		Mockito.doReturn(manager).when(context).getManager();
		Mockito.doReturn(new Session[] { session }).when(manager).findSessions();
		Mockito.doReturn(true).when(session).isValid();
		Mockito.doReturn(principal).when(session).getPrincipal();
		Mockito.doReturn("12345678901234567890").when(session).getId();
		Mockito.doReturn("alice").when(principal).getName();
		Mockito.doReturn(writer).when(response).getWriter();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(log, Mockito.times(2)).isDebugEnabled();
		Mockito.verify(log).debug("usernames: 'alice', 'bob'");
		Mockito.verify(request).getContext();
		Mockito.verify(context).getManager();
		Mockito.verify(manager).findSessions();
		Mockito.verify(session).isValid();
		Mockito.verify(session).getPrincipal();
		Mockito.verify(principal).getName();
		Mockito.verify(session).getId();
		Mockito.verify(session).expire();
		Mockito.verify(log).debug("session: id='12345678...', principal='alice'");
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_webapp_uri() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/index.jsp").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(next).invoke(request, response);
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_endpoint_uri_without_usernames() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();
		Mockito.doReturn(null).when(request).getParameterValues("username");
		Mockito.doReturn(writer).when(response).getWriter();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_endpoint_uri_with_wrong_password() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();
		Mockito.doReturn("wrong-password").when(request).getParameter("password");
		Mockito.doReturn(writer).when(response).getWriter();

		// prepare
		listener.setPassword("secret-password123!");

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(response).setStatus(403);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("Forbidden");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_endpoint_uri_with_unknown_client() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("123.45.67.89").when(request).getRemoteAddr();
		Mockito.doReturn(writer).when(response).getWriter();

		// prepare
		listener.setIpFilter("10.0.0.0/8");

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(response).setStatus(403);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("Forbidden");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_endpoint_uri_other_users() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();
		Mockito.doReturn(new String[] { "alice", "bob" }).when(request).getParameterValues("username");
		Mockito.doReturn(true).when(log).isDebugEnabled();
		Mockito.doReturn(context).when(request).getContext();
		Mockito.doReturn(manager).when(context).getManager();
		Mockito.doReturn(new Session[] { session }).when(manager).findSessions();
		Mockito.doReturn(true).when(session).isValid();
		Mockito.doReturn(principal).when(session).getPrincipal();
		Mockito.doReturn("peter").when(principal).getName();
		Mockito.doReturn(writer).when(response).getWriter();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(log).isDebugEnabled();
		Mockito.verify(log).debug("usernames: 'alice', 'bob'");
		Mockito.verify(request).getContext();
		Mockito.verify(context).getManager();
		Mockito.verify(manager).findSessions();
		Mockito.verify(session).isValid();
		Mockito.verify(session).getPrincipal();
		Mockito.verify(principal).getName();
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_endpoint_uri_unauthenticated_session() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();
		Mockito.doReturn(new String[] { "alice", "bob" }).when(request).getParameterValues("username");
		Mockito.doReturn(true).when(log).isDebugEnabled();
		Mockito.doReturn(context).when(request).getContext();
		Mockito.doReturn(manager).when(context).getManager();
		Mockito.doReturn(new Session[] { session }).when(manager).findSessions();
		Mockito.doReturn(true).when(session).isValid();
		Mockito.doReturn(null).when(session).getPrincipal();
		Mockito.doReturn(writer).when(response).getWriter();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(log).isDebugEnabled();
		Mockito.verify(log).debug("usernames: 'alice', 'bob'");
		Mockito.verify(request).getContext();
		Mockito.verify(context).getManager();
		Mockito.verify(manager).findSessions();
		Mockito.verify(session).isValid();
		Mockito.verify(session).getPrincipal();
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

	@Test
	void invoke_endpoint_uri_invalid_session() throws ServletException, IOException {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();
		Mockito.doReturn(new String[] { "alice", "bob" }).when(request).getParameterValues("username");
		Mockito.doReturn(true).when(log).isDebugEnabled();
		Mockito.doReturn(context).when(request).getContext();
		Mockito.doReturn(manager).when(context).getManager();
		Mockito.doReturn(new Session[] { session }).when(manager).findSessions();
		Mockito.doReturn(false).when(session).isValid();
		Mockito.doReturn(writer).when(response).getWriter();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(log).isDebugEnabled();
		Mockito.verify(log).debug("usernames: 'alice', 'bob'");
		Mockito.verify(request).getContext();
		Mockito.verify(context).getManager();
		Mockito.verify(manager).findSessions();
		Mockito.verify(session).isValid();
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).setCharacterEncoding("UTF-8");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).print("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, session, principal, log, next);
	}

}