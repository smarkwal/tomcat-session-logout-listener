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

import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.catalina.connector.Request;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Checks if a given request contains the correct password.
 */
class PasswordCheck implements Predicate<Request> {

	private static final String PASSWORD_PARAMETER = "password";

	private final Supplier<String> passwordProvider;
	private final Log log;

	PasswordCheck(Supplier<String> passwordProvider) {
		this(passwordProvider, LogFactory.getLog(PasswordCheck.class));
	}

	// visible for testing
	PasswordCheck(Supplier<String> passwordProvider, Log log) {
		this.passwordProvider = passwordProvider;
		this.log = log;
	}

	@Override
	public boolean test(Request request) {

		// check if a password has been configured
		String password = passwordProvider.get();
		if (password == null) {
			return true;
		}

		// get password from request
		String requestPassword = request.getParameter(PASSWORD_PARAMETER);
		if (requestPassword == null) {
			log.warn("No password found in request.");
			return false;
		}

		// compare passwords
		boolean result = password.equals(requestPassword);
		if (!result) {
			log.warn("Incorrect password.");
			return false;
		}

		// request is accepted
		return true;
	}

}

