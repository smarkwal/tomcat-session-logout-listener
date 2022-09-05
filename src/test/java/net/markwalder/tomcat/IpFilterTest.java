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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpFilterTest {

	private static final String PRIVATE_FILTER = "127.0.0.0/8,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16";

	private static final String ALL_SUBNET = "0.0.0.0/0";
	private static final String LOCAL_SUBNET = "127.0.0.0/8";
	private static final String CLASS_A_SUBNET = "10.0.0.0/8";
	private static final String CLASS_B_SUBNET = "172.16.0.0/12";
	private static final String CLASS_C_SUBNET = "192.168.0.0/16";

	@Test
	void matches_localhost_range() {
		assertFalse(IpFilter.matches("126.255.255.255", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("127.0.0.0", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("127.0.0.1", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("127.1.2.3", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("127.255.255.255", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("128.0.0.0", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("123.45.67.89", PRIVATE_FILTER));
	}

	@Test
	void matches_class_A_private_range() {
		assertFalse(IpFilter.matches("9.255.255.255", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("10.0.0.0", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("10.1.2.3", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("10.255.255.255", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("11.0.0.0", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("123.45.67.89", PRIVATE_FILTER));
	}

	@Test
	void matches_class_B_private_range() {
		assertFalse(IpFilter.matches("172.15.255.255", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("172.16.0.0", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("172.17.2.3", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("172.31.255.255", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("172.32.0.0", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("123.45.67.89", PRIVATE_FILTER));
	}

	@Test
	void matches_class_C_private_range() {
		assertFalse(IpFilter.matches("192.167.255.255", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("192.168.0.0", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("192.168.1.2", PRIVATE_FILTER));
		assertTrue(IpFilter.matches("192.168.255.255", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("192.169.0.0", PRIVATE_FILTER));
		assertFalse(IpFilter.matches("123.45.67.89", PRIVATE_FILTER));
	}

	@Test
	void matches_exact() {
		assertTrue(IpFilter.matches("123.45.67.89", "123.45.67.89"));
		assertFalse(IpFilter.matches("123.45.67.89", "1.2.3.4"));
	}

	@Test
	void matches_illegal_values() {
		assertFalse(IpFilter.matches(null, "10.0.0.0/8"));
		assertFalse(IpFilter.matches("10.0.0.1", null));
	}

	@Test
	void matchesAddress() {
		assertTrue(IpFilter.matchesAddress("127.0.0.1", "127.0.0.1"));
		assertFalse(IpFilter.matchesAddress("127.0.0.1", "10.0.0.1"));
	}

	@Test
	void matchesAddress_wildcard() {
		assertTrue(IpFilter.matchesAddress("127.0.0.1", "*"));
	}

	@Test
	void matchesAddress_illegal_values() {
		assertFalse(IpFilter.matchesAddress(null, "10.0.0.1"));
		assertFalse(IpFilter.matchesAddress("10.0.0.1", null));
		assertFalse(IpFilter.matchesAddress("10.0.0", "10.0.0.1"));
		assertFalse(IpFilter.matchesAddress("10.0.0.1", "10.0.0"));
	}

	@Test
	void matchesRange_local_subnet() {
		assertFalse(IpFilter.matchesRange("126.255.255.255", LOCAL_SUBNET));
		assertTrue(IpFilter.matchesRange("127.0.0.0", LOCAL_SUBNET));
		assertTrue(IpFilter.matchesRange("127.0.0.1", LOCAL_SUBNET));
		assertTrue(IpFilter.matchesRange("127.1.2.3", LOCAL_SUBNET));
		assertTrue(IpFilter.matchesRange("127.255.255.255", LOCAL_SUBNET));
		assertFalse(IpFilter.matchesRange("128.0.0.0", LOCAL_SUBNET));
		assertFalse(IpFilter.matchesRange("123.45.67.89", LOCAL_SUBNET));
	}

	@Test
	void matchesRange_class_A_subnet() {
		assertFalse(IpFilter.matchesRange("9.255.255.255", CLASS_A_SUBNET));
		assertTrue(IpFilter.matchesRange("10.0.0.0", CLASS_A_SUBNET));
		assertTrue(IpFilter.matchesRange("10.1.2.3", CLASS_A_SUBNET));
		assertTrue(IpFilter.matchesRange("10.255.255.255", CLASS_A_SUBNET));
		assertFalse(IpFilter.matchesRange("11.0.0.0", CLASS_A_SUBNET));
		assertFalse(IpFilter.matchesRange("123.45.67.89", CLASS_A_SUBNET));
	}

	@Test
	void matchesRange_class_B_subnet() {
		assertFalse(IpFilter.matchesRange("172.15.255.255", CLASS_B_SUBNET));
		assertTrue(IpFilter.matchesRange("172.16.0.0", CLASS_B_SUBNET));
		assertTrue(IpFilter.matchesRange("172.17.2.3", CLASS_B_SUBNET));
		assertTrue(IpFilter.matchesRange("172.31.255.255", CLASS_B_SUBNET));
		assertFalse(IpFilter.matchesRange("172.32.0.0", CLASS_B_SUBNET));
		assertFalse(IpFilter.matchesRange("123.45.67.89", CLASS_B_SUBNET));
	}

	@Test
	void matchesRange_class_C_subnet() {
		assertFalse(IpFilter.matchesRange("192.167.255.255", CLASS_C_SUBNET));
		assertTrue(IpFilter.matchesRange("192.168.0.0", CLASS_C_SUBNET));
		assertTrue(IpFilter.matchesRange("192.168.1.2", CLASS_C_SUBNET));
		assertTrue(IpFilter.matchesRange("192.168.255.255", CLASS_C_SUBNET));
		assertFalse(IpFilter.matchesRange("192.169.0.0", CLASS_C_SUBNET));
		assertFalse(IpFilter.matchesRange("123.45.67.89", CLASS_C_SUBNET));
	}

	@Test
	void matchesRange_all_IPv4() {
		assertTrue(IpFilter.matchesRange("0.0.0.0", ALL_SUBNET));
		assertTrue(IpFilter.matchesRange("1.2.3.4", ALL_SUBNET));
		assertTrue(IpFilter.matchesRange("255.255.255.255", ALL_SUBNET));
		assertTrue(IpFilter.matchesRange("123.45.67.89", ALL_SUBNET));
	}

	@Test
	void matchesRange_unexpected() {

		// ranges with unexpected subnet addresses (not aligned to subnet mask)
		assertTrue(IpFilter.matchesRange("127.0.0.0", "127.1.2.3/8"));
		assertTrue(IpFilter.matchesRange("10.0.0.0", "10.1.2.3/8"));
		assertTrue(IpFilter.matchesRange("172.16.0.0", "172.17.2.3/12"));
		assertTrue(IpFilter.matchesRange("192.168.0.0", "192.168.1.2/16"));
	}

	@Test
	void matchesRange_illegal_values() {
		assertFalse(IpFilter.matchesRange(null, "10.0.0.0/8"));
		assertFalse(IpFilter.matchesRange("10.0.0.1", null));
		assertFalse(IpFilter.matchesRange("10.0.0", "10.0.0.0/8"));
		assertFalse(IpFilter.matchesRange("10.0.0.1", "10.0.0/8"));
	}

	@Test
	void parseAddress() {

		// localhost
		assertEquals(0x7F000001L, IpFilter.parseAddress("127.0.0.1"));

		// start and end of private ranges
		assertEquals(0x0A000000L, IpFilter.parseAddress("10.0.0.0"));
		assertEquals(0x0AFFFFFFL, IpFilter.parseAddress("10.255.255.255"));
		assertEquals(0xAC100000L, IpFilter.parseAddress("172.16.0.0"));
		assertEquals(0xAC1FFFFFL, IpFilter.parseAddress("172.31.255.255"));
		assertEquals(0xC0A80000L, IpFilter.parseAddress("192.168.0.0"));
		assertEquals(0xC0A8FFFFL, IpFilter.parseAddress("192.168.255.255"));

		// min and max address
		assertEquals(0x00000000L, IpFilter.parseAddress("0.0.0.0"));
		assertEquals(0xFFFFFFFFL, IpFilter.parseAddress("255.255.255.255"));

		// arbitrary addresses
		assertEquals(0x01020304L, IpFilter.parseAddress("1.2.3.4"));

	}

	@Test
	void parseAddress_illegal_value() {
		assertThrows(IllegalArgumentException.class, () -> IpFilter.parseAddress("10.0.0"));
		assertThrows(IllegalArgumentException.class, () -> IpFilter.parseAddress("127.0.0.0.1"));
	}

}