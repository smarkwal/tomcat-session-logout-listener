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

	SessionLogoutListener listener = new SessionLogoutListener();

	@Mock
	Valve next;

	@BeforeEach
	void setUp() throws IOException {
		Mockito.lenient().doReturn(writer).when(response).getWriter();
		Mockito.lenient().doReturn(context).when(request).getContext();
		Mockito.lenient().doReturn(manager).when(context).getManager();

		listener.setNext(next);
	}

	@Test
	void invoke() throws ServletException, IOException {

		// prepare
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn(new String[] { "alice", "bob" }).when(request).getParameterValues("username");
		Mockito.doReturn(new Session[] { session }).when(manager).findSessions();
		Mockito.doReturn(principal).when(session).getPrincipal();
		Mockito.doReturn("alice").when(principal).getName();

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(request).getContext();
		Mockito.verify(context).getManager();
		Mockito.verify(manager).findSessions();
		Mockito.verify(session).getPrincipal();
		Mockito.verify(principal).getName();
		Mockito.verify(session).getId();
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).println("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, next);
	}

	@Test
	void invoke_webapp_uri() throws ServletException, IOException {

		// prepare
		Mockito.when(request.getRequestURI()).thenReturn("/index.jsp");

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(next).invoke(request, response);
		Mockito.verifyNoMoreInteractions(request, response, writer, next);
	}

	@Test
	void invoke_endpoint_uri_without_usernames() throws ServletException, IOException {

		// prepare
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn(null).when(request).getParameterValues("username");

		// test
		listener.invoke(request, response);

		// verify
		Mockito.verify(response).setStatus(200);
		Mockito.verify(response).setContentType("text/plain");
		Mockito.verify(response).getWriter();
		Mockito.verify(writer).println("OK");
		Mockito.verifyNoMoreInteractions(request, response, writer, context, manager, next);
	}

}