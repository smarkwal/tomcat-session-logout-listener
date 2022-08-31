# Tomcat Session Logout Listener

Tomcat valve for remote session invalidation.

This valve waits for HTTP requests sent to the session logout endpoint `/session-logout-listener`.
If such a request is received, it is intercepted and processed by the valve.
The valve looks for a parameter with name `username`.
If the parameter is present, the valve iterates over all active Tomcat sessions and
invalidate all sessions with a principal name matching the parameter value.

Tested with Tomcat 7.0.109, Tomcat 8.5.82, and Tomcat 9.0.65.

# Build

```shell
# set JAVA_HOME to a JDK 1.8
export JAVA_HOME=/path/to/java-1.8

# build JAR file
./gradlew clean jar
```

The JAR file `tomcat-session-logout-listener-<version>.jar` can be found in `build/libs`.

# Installation

Copy `tomcat-session-logout-listener-<version>.jar` to `$TOMCAT_HOME/lib/`.

# Configuration

Add the following line to `$TOMCAT_HOME/conf/server.xml` or `$TOMCAT_HOME/conf/context.xml`:

```xml
<Valve className="net.markwalder.tomcat.SessionLogoutListener"/>
```

# Usage

Send an HTTP `GET` or `POST` request to the session logout endpoint with a parameter `username` set to the username of the user to be logged out:

```
GET /session-logout-listener?username=alice
```

This request will invalidate all sessions of user "alice".

It is possible to log out multiple users at once by specifying the parameter `username` multiple times:

```
GET /session-logout-listener?username=alice&username=bob
```

The session logout endpoint will always return an HTTP response with status code 200 (OK), content type "text/plain", character set "UTF-8", and the text "OK" as body.

# Logging

Add the following lines to `$TOMCAT_HOME/conf/logging.properties`:

```properties
# Enable debug logging for Tomcat Session Logout Listener
net.markwalder.tomcat.SessionLogoutListener.level = FINE
```

When a request is sent to the session logout endpoint, the following log messages are written:

```
FINE [http-nio-8080-exec-1] net.markwalder.tomcat.SessionLogoutListener.logoutUsers usernames: 'alice', 'bob'
FINE [http-nio-8080-exec-1] net.markwalder.tomcat.SessionLogoutListener.logoutUsers session: id='78F9279D...', principal='alice'
FINE [http-nio-8080-exec-1] net.markwalder.tomcat.SessionLogoutListener.logoutUsers session: id='1EF34298...', principal='bob'
```

For security reasons, only the first 8 characters of the session ID are logged.
