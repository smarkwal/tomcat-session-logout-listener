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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.apache.catalina.connector.Request;
import org.apache.juli.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoteAddrCheckTest {

	@Mock
	Request request;

	@Mock
	Supplier<String> ipFilterProvider;

	@Mock
	Log log;

	RemoteAddrCheck remoteAddrCheck;

	@BeforeEach
	void setUp() {
		remoteAddrCheck = new RemoteAddrCheck(ipFilterProvider, log);
	}

	@Test
	void test_filter_not_set() {

		// mock
		Mockito.doReturn(null).when(ipFilterProvider).get();

		// test
		boolean result = remoteAddrCheck.test(request);

		// assert
		assertTrue(result);

		// verify
		Mockito.verifyNoMoreInteractions(request, ipFilterProvider, log);

	}

	@Test
	void test_filter_localhost() {

		// mock
		Mockito.doReturn("127.0.0.1").when(ipFilterProvider).get();
		Mockito.doReturn("127.0.0.1").when(request).getRemoteAddr();

		// test
		boolean result = remoteAddrCheck.test(request);

		// assert
		assertTrue(result);

		// verify
		Mockito.verifyNoMoreInteractions(request, ipFilterProvider, log);

	}

	@Test
	void test_filter_localhost_for_remote_client() {

		// mock
		Mockito.doReturn("127.0.0.1").when(ipFilterProvider).get();
		Mockito.doReturn("123.45.67.89").when(request).getRemoteAddr();

		// test
		boolean result = remoteAddrCheck.test(request);

		// assert
		assertFalse(result);

		// verify
		Mockito.verify(log).warn("Remote address '123.45.67.89' does not match IP filter.");
		Mockito.verifyNoMoreInteractions(request, ipFilterProvider, log);

	}

	@Test
	void test_filter_localhost_for_unknown_client() {

		// mock
		Mockito.doReturn("127.0.0.1").when(ipFilterProvider).get();
		Mockito.doReturn(null).when(request).getRemoteAddr();

		// test
		boolean result = remoteAddrCheck.test(request);

		// assert
		assertFalse(result);

		// verify
		Mockito.verify(log).warn("No remote address found in request");
		Mockito.verifyNoMoreInteractions(request, ipFilterProvider, log);

	}

}