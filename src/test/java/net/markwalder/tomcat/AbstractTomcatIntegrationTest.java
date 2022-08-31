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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractTomcatIntegrationTest {

	/**
	 * Path to JAR file with valve (built by 'jar' task).
	 * The system property 'project.archiveFilePath' is set in build.gradle.kts.
	 */
	private static final String JAR_FILE = System.getProperty("project.archiveFilePath", "unknown.jar");

	/**
	 * Temporary folder for web application.
	 */
	@TempDir
	static File temporaryFolder;

	static TomcatContainer createTomcatContainer(String tomcatVersion) {
		System.out.println("Prepare Tomcat " + tomcatVersion + " ...");

		// prepare web application in temporary folder
		// (by default, the Tomcat container does not contain any web applications)
		File webappDir = new File(temporaryFolder, tomcatVersion + "/ROOT");
		try {
			FileUtils.forceMkdir(webappDir);
			copyResourceToWebApp("WEB-INF/web.xml", webappDir);
			copyResourceToWebApp("secure/index.jsp", webappDir);
			copyResourceToWebApp("error.jsp", webappDir);
			copyResourceToWebApp("index.jsp", webappDir);
		} catch (IOException e) {
			fail("Failed to create web application: " + webappDir.getAbsolutePath(), e);
		}

		// get JAR file with valve from Gradle build folder
		File jarFile = new File(JAR_FILE);
		assertTrue(jarFile.exists(), "JAR file not found: " + jarFile.getAbsolutePath() + ". Please make sure that the JAR file has been built before running any tests.");

		// configure container
		return new TomcatContainer(tomcatVersion)
				.withServerXML("tomcat/conf/server.xml")
				.withContextXML("tomcat/conf/context.xml")
				.withTomcatUsersXML("tomcat/conf/tomcat-users.xml")
				.withLoggingProperties("tomcat/conf/logging.properties")
				.withWebApp(webappDir)
				.withLib(jarFile);
	}

	private static void copyResourceToWebApp(String path, File webappDir) throws IOException {
		String resource = "tomcat/webapps/ROOT/" + path;
		File file = new File(webappDir, path);
		try (InputStream stream = AbstractTomcatIntegrationTest.class.getClassLoader().getResourceAsStream(resource)) {
			if (stream == null) {
				throw new IOException("Resource not found: " + resource);
			}
			byte[] data = IOUtils.toByteArray(stream);
			FileUtils.writeByteArrayToFile(file, data);
		}
	}

	/**
	 * Tomcat container instance (shared by all tests).
	 */
	private final TomcatContainer container;

	AbstractTomcatIntegrationTest(TomcatContainer container) {
		this.container = container;
	}

	// tests -------------------------------------------------------------------

	@Test
	@DisplayName("GET /")
	void get_root() throws IOException {

		// prepare
		String url = container.getURL() + "/";
		Request request = Request.Get(url);

		// test
		HttpResponse response = request.execute().returnResponse();

		// assert
		assertStatusCode(response, 200);
		assertContentType(response, "text/plain; charset=UTF-8");

		// TODO: String sessionId = getSessionId(response);
		Properties properties = getProperties(response);
		assertThat(properties).containsKey("session.id");
		assertThat(properties).hasSize(1);
	}

	@Test
	@DisplayName("GET /secure/ (unauthenticated)")
	void get_secure_unauthenticated() throws IOException {

		// prepare
		String url = container.getURL() + "/secure/";
		Request request = Request.Get(url);

		// test
		HttpResponse response = request.execute().returnResponse();

		// assert
		assertStatusCode(response, 401);
		assertContentType(response, "text/plain; charset=UTF-8");

		// TODO: String sessionId = getSessionId(response);
		Properties properties = getProperties(response);
		assertThat(properties).containsKey("session.id");
		assertThat(properties).hasSize(1);
	}

	@Test
	@DisplayName("GET /secure/ (authenticated)")
	void get_secure_authenticated() throws IOException {

		// prepare
		String url = container.getURL() + "/secure/";
		String credentials = Base64.getEncoder().encodeToString("alice:alice11".getBytes());
		Request request = Request.Get(url).addHeader("Authorization", "Basic " + credentials);

		// test
		HttpResponse response = request.execute().returnResponse();

		// assert
		assertStatusCode(response, 200);
		assertContentType(response, "text/plain; charset=UTF-8");

		String sessionId = getSessionId(response);
		Properties properties = getProperties(response);
		assertThat(properties).containsEntry("session.id", sessionId);
		assertThat(properties).containsEntry("principal.name", "alice");
		assertThat(properties).hasSize(2);
	}

	@Test
	@DisplayName("GET /session-logout-listener")
	void get_session_logout_listener() throws IOException {

		// prepare
		String url = container.getURL() + "/session-logout-listener?username=alice";
		Request request = Request.Get(url);

		// test
		HttpResponse response = request.execute().returnResponse();

		// assert
		assertStatusCode(response, 200);
		assertContentType(response, "text/plain; charset=UTF-8");

		String body = getBody(response);
		assertEquals("OK", body);

		// TODO: check Tomcat log output
	}

	@Test
	@DisplayName("POST /session-logout-listener")
	void post_session_logout_listener() throws IOException {

		// prepare
		String url = container.getURL() + "/session-logout-listener";
		Request request = Request.Post(url).bodyForm(new BasicNameValuePair("username", "alice"));

		// test
		HttpResponse response = request.execute().returnResponse();

		// assert
		assertStatusCode(response, 200);
		assertContentType(response, "text/plain; charset=UTF-8");

		String body = getBody(response);
		assertEquals("OK", body);

		// TODO: check Tomcat log output
	}

	// helper methods ----------------------------------------------------------

	private static void assertStatusCode(HttpResponse response, int expectedStatusCode) {
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(expectedStatusCode, statusCode);
	}

	private static void assertContentType(HttpResponse response, String expectedContentType) {
		HttpEntity entity = response.getEntity();
		Header contentType = entity.getContentType();
		assertEquals(expectedContentType, contentType.getValue());
	}

	private static String getSessionId(HttpResponse response) {
		Header[] cookies = response.getHeaders("Set-Cookie");
		for (Header cookie : cookies) {
			String value = cookie.getValue();
			if (value.startsWith("JSESSIONID=")) {
				return value.substring(value.indexOf("=") + 1, value.indexOf(";"));
			}
		}
		return null;
	}

	private static String getBody(HttpResponse response) throws IOException {
		HttpEntity entity = response.getEntity();
		return EntityUtils.toString(entity);
	}

	private static Properties getProperties(HttpResponse response) throws IOException {
		HttpEntity entity = response.getEntity();
		InputStream content = entity.getContent();
		Properties properties = new Properties();
		properties.load(content);
		return properties;
	}

}
