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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

@TestMethodOrder(MethodOrderer.DisplayName.class)
public abstract class AbstractTomcatIntegrationTest {

	/**
	 * Path to JAR file with valve (built by 'jar' task).
	 * The system property 'project.archiveFilePath' is set in build.gradle.kts.
	 */
	private static final String JAR_FILE = System.getProperty("project.archiveFilePath", "unknown.jar");

	// configured password for valve (see context.xml)
	private static final String PASSWORD = "my-secret-123!";

	private static final String SESSION_COOKIE = "JSESSIONID";
	private static final String CONTENT_TYPE_PLAIN = "text/plain;charset=UTF-8";
	private static final String CONTENT_TYPE_HTML = "text/html;charset=UTF-8";
	private static final String LOGIN_PAGE_HEADER = "X-LoginPage";
	private static final String LOGGER_NAME = SessionLogoutListener.class.getName();

	/**
	 * Temporary folder for web application.
	 */
	@TempDir
	static File temporaryFolder;

	static TomcatContainer createTomcatContainer(String tomcatVersion, String javaVersion) {
		System.out.println("Prepare Tomcat " + tomcatVersion + " on Java " + javaVersion + " ...");

		// prepare web application in temporary folder
		// (by default, the Tomcat container does not contain any web applications)
		File webappDir = new File(temporaryFolder, tomcatVersion + "/ROOT");
		try {
			FileUtils.forceMkdir(webappDir);
			copyResourceToWebApp("WEB-INF/web.xml", webappDir);
			copyResourceToWebApp("secure/index.jsp", webappDir);
			copyResourceToWebApp("error.jsp", webappDir);
			copyResourceToWebApp("index.jsp", webappDir);
			copyResourceToWebApp("login.jsp", webappDir);
		} catch (IOException e) {
			fail("Failed to create web application: " + webappDir.getAbsolutePath(), e);
		}

		// get JAR file with valve from Gradle build folder
		File jarFile = new File(JAR_FILE);
		assertTrue(jarFile.exists(), "JAR file not found: " + jarFile.getAbsolutePath() + ". Please make sure that the JAR file has been built before running any tests.");

		// configure container
		return new TomcatContainer(tomcatVersion, javaVersion)
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

	/**
	 * HTTP client instance (new instance for every test).
	 */
	private final CookieStore cookieStore = new BasicCookieStore();
	private final CloseableHttpClient client = HttpClients.custom()
			.setDefaultCookieStore(cookieStore)
			.setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
			.build();

	AbstractTomcatIntegrationTest(TomcatContainer container) {
		this.container = container;
	}

	// lifecycle ---------------------------------------------------------------

	@AfterEach
	void tearDown() throws IOException {
		client.close();
		container.clearLog();
	}

	// tests -------------------------------------------------------------------

	@Test
	@DisplayName("GET /")
	void get_root() throws IOException {

		// prepare
		HttpUriRequest request = get("/");

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);

			String sessionId = getSessionId(cookieStore);
			Properties properties = getProperties(response);
			assertThat(properties).containsEntry("session.id", sessionId);
			assertThat(properties).hasSize(1);
		}
	}

	@Test
	@DisplayName("GET /secure/")
	void get_secure() throws IOException {

		// prepare
		HttpUriRequest request = get("/secure/");

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_HTML);
			assertTrue(response.containsHeader(LOGIN_PAGE_HEADER));
		}

		// login
		request = post("/j_security_check", param("j_username", "user1"), param("j_password", "password1"));

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);

			String sessionId = getSessionId(cookieStore);
			Properties properties = getProperties(response);
			assertThat(properties).containsEntry("session.id", sessionId);
			assertThat(properties).containsEntry("principal.name", "user1");
			assertThat(properties).hasSize(2);
		}
	}

	@Test
	@DisplayName("GET /session-logout-listener (1 - single user)")
	void get_session_logout_listener_single_user() throws IOException {

		// prepare
		HttpUriRequest request = get("/session-logout-listener?username=userX&password=" + PASSWORD);

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "OK");
		}

		// check Tomcat log output
		String log = container.getLog().trim();
		assertThat(log).endsWith(LOGGER_NAME + ".logoutUsers usernames: 'userX'");
	}

	@Test
	@DisplayName("GET /session-logout-listener (2 - multiple users)")
	void get_session_logout_listener_multiple_users() throws IOException {

		// prepare
		HttpUriRequest request = get("/session-logout-listener?username=userX&username=userY&password=" + PASSWORD);

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "OK");
		}

		// check Tomcat log output
		String log = container.getLog().trim();
		assertThat(log).endsWith(LOGGER_NAME + ".logoutUsers usernames: 'userX', 'userY'");
	}

	@Test
	@DisplayName("POST /session-logout-listener (1 - single user)")
	void post_session_logout_listener_single_user() throws IOException {

		// prepare
		HttpUriRequest request = post("/session-logout-listener", param("username", "userX"), param("password", PASSWORD));

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "OK");
		}

		// check Tomcat log output
		String log = container.getLog().trim();
		assertThat(log).endsWith(LOGGER_NAME + ".logoutUsers usernames: 'userX'");
	}

	@Test
	@DisplayName("POST /session-logout-listener (2 - multiple users)")
	void post_session_logout_listener_multiple_users() throws IOException {

		// prepare
		HttpUriRequest request = post("/session-logout-listener", param("username", "userX"), param("username", "userY"), param("password", PASSWORD));

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "OK");
		}

		// check Tomcat log output
		String log = container.getLog().trim();
		assertThat(log).endsWith(LOGGER_NAME + ".logoutUsers usernames: 'userX', 'userY'");
	}

	@Test
	@DisplayName("POST /session-logout-listener (3 - active sessions)")
	void post_session_logout_listener_active_sessions() throws IOException {

		// prepare: login user 1 and 2
		List<Cookie> cookies1 = login("user1", "password1");
		String sessionId1 = getSessionId(cookies1);
		assertNotNull(sessionId1);
		List<Cookie> cookies2 = login("user2", "password2");
		String sessionId2 = getSessionId(cookies2);
		assertNotNull(sessionId2);

		// prepare
		HttpUriRequest request = post("/session-logout-listener", param("username", "user1"), param("username", "user2"), param("password", PASSWORD));

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "OK");
		}

		// check Tomcat log output
		String log = container.getLog();
		assertThat(log).contains(
				LOGGER_NAME + ".logoutUsers usernames: 'user1', 'user2'\n",
				LOGGER_NAME + ".logoutUsers session: id='" + SessionLogoutListener.truncateSessionId(sessionId1) + "...', principal='user1'\n",
				LOGGER_NAME + ".logoutUsers session: id='" + SessionLogoutListener.truncateSessionId(sessionId2) + "...', principal='user2'\n"
		);

		// check that sessions have been invalidated
		assertNoAccess(cookies1);
		assertNoAccess(cookies2);

	}

	@Test
	@DisplayName("POST /session-logout-listener (4 - no password)")
	void post_session_logout_listener_no_password() throws IOException {

		// prepare
		HttpUriRequest request = post("/session-logout-listener", param("username", "userX"));

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertForbidden(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "Forbidden");
		}

		// check Tomcat log output
		String log = container.getLog().trim();
		assertThat(log).endsWith("net.markwalder.tomcat.PasswordCheck.test No password found in request.");
	}

	@Test
	@DisplayName("POST /session-logout-listener (5 - wrong password)")
	void post_session_logout_listener_wrong_password() throws IOException {

		// prepare
		HttpUriRequest request = post("/session-logout-listener", param("username", "userX"), param("password", "wrong-password"));

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert
			assertForbidden(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
			assertBody(response, "Forbidden");
		}

		// check Tomcat log output
		String log = container.getLog().trim();
		assertThat(log).endsWith("net.markwalder.tomcat.PasswordCheck.test Incorrect password.");
	}

	// helper methods ----------------------------------------------------------

	private HttpGet get(String path) {
		String url = container.getURL() + path;
		return new HttpGet(url);
	}

	private HttpPost post(String path, NameValuePair... parameters) {
		String url = container.getURL() + path;
		HttpPost request = new HttpPost(url);
		request.setEntity(new UrlEncodedFormEntity(Arrays.asList(parameters), StandardCharsets.UTF_8));
		return request;
	}

	private List<Cookie> login(String username, String password) throws IOException {

		// GET /secure/
		HttpUriRequest request = get("/secure/");
		try (CloseableHttpResponse response = client.execute(request)) {
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_HTML);
			assertTrue(response.containsHeader(LOGIN_PAGE_HEADER));
		}

		// POST /j_security_check
		request = post("/j_security_check", param("j_username", username), param("j_password", password));
		try (CloseableHttpResponse response = client.execute(request)) {
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_PLAIN);
		}

		// get session ID from cookie
		String sessionId = getSessionId(cookieStore);
		assertNotNull(sessionId);

		// remember cookies
		List<Cookie> cookies = cookieStore.getCookies();

		// clear cookies
		cookieStore.clear();

		return cookies;
	}

	private void assertNoAccess(List<Cookie> cookies) throws IOException {

		// clear old cookies
		cookieStore.clear();

		// add new cookies
		for (Cookie cookie : cookies) {
			cookieStore.addCookie(cookie);
		}

		// prepare
		HttpUriRequest request = get("/secure/");

		// test
		try (CloseableHttpResponse response = client.execute(request)) {

			// assert: access is denied (login page is shown)
			assertOK(response);
			assertContentType(response, CONTENT_TYPE_HTML);
			assertTrue(response.containsHeader(LOGIN_PAGE_HEADER));
		}

	}

	private static NameValuePair param(String name, String value) {
		return new BasicNameValuePair(name, value);
	}

	private static void assertOK(HttpResponse response) {
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(200, statusCode);
	}

	private void assertForbidden(CloseableHttpResponse response) {
		int statusCode = response.getStatusLine().getStatusCode();
		assertEquals(403, statusCode);
	}

	private static void assertContentType(HttpResponse response, String expectedContentType) {
		HttpEntity entity = response.getEntity();
		Header contentType = entity.getContentType();
		assertEquals(expectedContentType, contentType.getValue());
	}

	private static String getSessionId(CookieStore cookieStore) {
		List<Cookie> cookies = cookieStore.getCookies();
		return getSessionId(cookies);
	}

	private static String getSessionId(List<Cookie> cookies) {
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(SESSION_COOKIE)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private static void assertBody(CloseableHttpResponse response, String expectedBody) throws IOException {
		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity);
		assertEquals(expectedBody, body);
	}

	private static Properties getProperties(HttpResponse response) throws IOException {
		HttpEntity entity = response.getEntity();
		InputStream content = entity.getContent();
		Properties properties = new Properties();
		properties.load(content);
		return properties;
	}

}
