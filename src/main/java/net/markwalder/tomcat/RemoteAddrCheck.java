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
 * Checks if a given request has been sent from an allowed remote address.
 */
class RemoteAddrCheck implements Predicate<Request> {

	private final Supplier<String> ipFilterProvider;
	private final Log log;

	RemoteAddrCheck(Supplier<String> ipFilterProvider) {
		this(ipFilterProvider, LogFactory.getLog(RemoteAddrCheck.class));
	}

	// visible for testing
	RemoteAddrCheck(Supplier<String> ipFilterProvider, Log log) {
		this.ipFilterProvider = ipFilterProvider;
		this.log = log;
	}

	@Override
	public boolean test(Request request) {

		// check if IP filter has been configured
		String ipFilter = ipFilterProvider.get();
		if (ipFilter == null) {
			return true;
		}

		// get remote address from request
		String remoteAddr = request.getRemoteAddr();
		if (remoteAddr == null) {
			log.warn("No remote address found in request.");
			return false;
		}

		// TODO: support X-Forwarded-For header
		// see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For

		boolean result = IpFilter.matches(remoteAddr, ipFilter);
		if (!result) {
			log.warn("Remote address '" + remoteAddr + "' does not match IP filter.");
			return false;
		}

		// request is accepted
		return true;
	}

}

