package net.markwalder.tomcat;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SessionLogoutListenerTest {

	@Test
	void invoke() throws ServletException, IOException {

		// mock
		Request request = Mockito.mock(Request.class);
		Response response = Mockito.mock(Response.class);
		Valve next = Mockito.mock(Valve.class);

		// prepare
		SessionLogoutListener listener = new SessionLogoutListener();
		listener.setNext(next);

		// test
		listener.invoke(request, response);

		// assert

		// verify
		Mockito.verify(next).invoke(request, response);
		Mockito.verifyNoMoreInteractions(next);
		Mockito.verifyNoMoreInteractions(request);
		Mockito.verifyNoMoreInteractions(response);

	}

}