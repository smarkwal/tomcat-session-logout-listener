package net.markwalder.tomcat;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

public class SessionLogoutListener extends ValveBase {

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		// forward request to next valve in the pipeline
		getNext().invoke(request, response);

	}

}
