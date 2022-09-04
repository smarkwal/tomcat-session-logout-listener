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

import org.apache.catalina.connector.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestInterceptorTest {

	@Mock
	Request request;

	RequestInterceptor requestInterceptor = new RequestInterceptor();

	@Test
	void test_root() {

		// mock
		Mockito.doReturn("/").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();

		// test
		boolean result = requestInterceptor.test(request);

		// assert
		assertFalse(result);

		// verify
		Mockito.verifyNoMoreInteractions(request);

	}

	@Test
	void test_index_html() {

		// mock
		Mockito.doReturn("/index.html").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();

		// test
		boolean result = requestInterceptor.test(request);

		// assert
		assertFalse(result);

		// verify
		Mockito.verifyNoMoreInteractions(request);

	}

	@Test
	void test_endpoint() {

		// mock
		Mockito.doReturn("/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("").when(request).getContextPath();

		// test
		boolean result = requestInterceptor.test(request);

		// assert
		assertTrue(result);

		// verify
		Mockito.verifyNoMoreInteractions(request);

	}

	@Test
	void test_endpoint_with_context_path() {

		// mock
		Mockito.doReturn("/myapp/session-logout-listener").when(request).getRequestURI();
		Mockito.doReturn("/myapp").when(request).getContextPath();

		// test
		boolean result = requestInterceptor.test(request);

		// assert
		assertTrue(result);

		// verify
		Mockito.verifyNoMoreInteractions(request);

	}

}