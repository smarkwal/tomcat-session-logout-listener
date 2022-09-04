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
class PasswordCheckTest {

	@Mock
	Request request;

	@Mock
	Supplier<String> passwordProvider;

	@Mock
	Log log;

	PasswordCheck passwordCheck;

	@BeforeEach
	void setUp() {
		passwordCheck = new PasswordCheck(passwordProvider, log);
	}

	@Test
	void test_password_not_set() {

		// mock
		Mockito.doReturn(null).when(passwordProvider).get();

		// test
		boolean result = passwordCheck.test(request);

		// assert
		assertTrue(result);

		// verify
		Mockito.verifyNoMoreInteractions(request, passwordProvider, log);

	}

	@Test
	void test_password_correct() {

		// mock
		Mockito.doReturn("secret-password-123").when(passwordProvider).get();
		Mockito.doReturn("secret-password-123").when(request).getParameter("password");

		// test
		boolean result = passwordCheck.test(request);

		// assert
		assertTrue(result);

		// verify
		Mockito.verifyNoMoreInteractions(request, passwordProvider, log);

	}

	@Test
	void test_password_parameter_missing() {

		// mock
		Mockito.doReturn("secret-password-123").when(passwordProvider).get();
		Mockito.doReturn(null).when(request).getParameter("password");

		// test
		boolean result = passwordCheck.test(request);

		// assert
		assertFalse(result);

		// verify
		Mockito.verify(log).warn("No password found in request");
		Mockito.verifyNoMoreInteractions(request, passwordProvider, log);

	}

	@Test
	void test_password_incorrect() {

		// mock
		Mockito.doReturn("secret-password-123").when(passwordProvider).get();
		Mockito.doReturn("let-me-in").when(request).getParameter("password");

		// test
		boolean result = passwordCheck.test(request);

		// assert
		assertFalse(result);

		// verify
		Mockito.verify(log).warn("Incorrect password");
		Mockito.verifyNoMoreInteractions(request, passwordProvider, log);

	}

}