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

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.DockerImageName;

/**
 * Tomcat container.
 */
public class TomcatContainer extends GenericContainer<TomcatContainer> {

	private static final String TOMCAT_HOME = "/usr/local/tomcat";

	private final String tomcatVersion;

	private final StringBuilder log = new StringBuilder();

	public TomcatContainer(String tomcatVersion, String javaVersion) {
		super(getDockerImageName(tomcatVersion, javaVersion));
		this.tomcatVersion = tomcatVersion;

		// make port 8080 accessible
		// (Testcontainers will pick a random free port on the host)
		addExposedPort(8080);
	}

	private static DockerImageName getDockerImageName(String tomcatVersion, String javaVersion) {
		return DockerImageName.parse("tomcat:" + tomcatVersion + "-jdk" + javaVersion);
	}

	public String getTomcatVersion() {
		return tomcatVersion;
	}

	/**
	 * Override Tomcat's server.xml file.
	 *
	 * @param resource Resource on classpath used as server.xml file.
	 */
	public TomcatContainer withServerXML(String resource) {
		return withClasspathResourceMapping(resource, TOMCAT_HOME + "/conf/server.xml", BindMode.READ_ONLY);
	}

	/**
	 * Override Tomcat's context.xml file.
	 *
	 * @param resource Resource on classpath used as context.xml file.
	 */
	public TomcatContainer withContextXML(String resource) {
		return withClasspathResourceMapping(resource, TOMCAT_HOME + "/conf/context.xml", BindMode.READ_ONLY);
	}

	/**
	 * Override Tomcat's tomcat-users.xml file.
	 *
	 * @param resource Resource on classpath used as tomcat-users.xml file.
	 */
	public TomcatContainer withTomcatUsersXML(String resource) {
		return withClasspathResourceMapping(resource, TOMCAT_HOME + "/conf/tomcat-users.xml", BindMode.READ_ONLY);
	}

	/**
	 * Override Tomcat's logging.properties file.
	 *
	 * @param resource Resource on classpath used as logging.properties file.
	 */
	public TomcatContainer withLoggingProperties(String resource) {
		return withClasspathResourceMapping(resource, TOMCAT_HOME + "/conf/logging.properties", BindMode.READ_ONLY);
	}

	/**
	 * Add a web application to Tomcat.
	 *
	 * @param webapp Folder or WAR file of web application.
	 */
	public TomcatContainer withWebApp(File webapp) {
		addFileSystemBind(webapp.getAbsolutePath(), TOMCAT_HOME + "/webapps/" + webapp.getName(), BindMode.READ_ONLY);
		return this;
	}

	/**
	 * Add a library to Tomcat.
	 *
	 * @param jarFile JAR file.
	 */
	public TomcatContainer withLib(File jarFile) {
		addFileSystemBind(jarFile.getAbsolutePath(), TOMCAT_HOME + "/lib/" + jarFile.getName(), BindMode.READ_ONLY);
		return this;
	}

	@Override
	public void start() {
		System.out.println("Start Tomcat " + tomcatVersion + " ...");

		// start container
		super.start();

		// print container log to STDOUT
		enableLogOutput();

		System.out.println("Startup complete.");
	}

	@Override
	public void stop() {
		System.out.println("Stop Tomcat " + tomcatVersion + " ...");

		// shutdown Tomcat (not really required)
		shutdown();

		// stop container
		super.stop();

		System.out.println("Shutdown complete.");
	}

	/**
	 * Write container log to STDOUT.
	 */
	public void enableLogOutput() {
		checkRunning();

		// capture log output (must be enabled AFTER container has been started)
		Consumer<OutputFrame> logConsumer = outputFrame -> {
			switch (outputFrame.getType()) {
				case STDOUT:
				case STDERR:
					String message = outputFrame.getUtf8String();
					System.out.print("[Container Log] " + message);
					log.append(message).append("\n");
				case END:
					// ignore
			}
		};

		followOutput(logConsumer);
	}

	public String getLog() {

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}

		return log.toString();
	}

	public void clearLog() {
		log.setLength(0);
	}

	/**
	 * Get base URL of Tomcat including port number.
	 */
	public String getURL() {
		checkRunning();

		String host = getHost();
		int port = getMappedPort(8080);
		return "http://" + host + ":" + port;
	}

	/**
	 * Initiate shutdown of Tomcat in container.
	 * This does not stop the container itself!
	 */
	public void shutdown() {
		checkRunning();

		try {
			ExecResult result = execInContainer(TOMCAT_HOME + "/bin/catalina.sh", "stop");
			if (result.getExitCode() != 0) {
				System.out.println(result.toString());
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void checkRunning() {
		if (!isRunning()) {
			throw new IllegalStateException("Container is not running.");
		}
	}

}
