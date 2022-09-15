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
	void matchesAddress_IPv6() {
		assertTrue(IpFilter.matchesAddress("0123:4567:89ab:cdef:0123:4567:89ab:cdef", "0123:4567:89ab:cdef:0123:4567:89ab:cdef"));
		assertTrue(IpFilter.matchesAddress("fc00::1", "fc00::1"));
		assertTrue(IpFilter.matchesAddress("fc00::1", "FC00::1"));
		assertTrue(IpFilter.matchesAddress("fc00::1", "fc00:0:0:0:0:0:0:1"));
		assertTrue(IpFilter.matchesAddress("fc00::1", "fc00:0000:0000:0000:0000:0000:0000:0001"));
		assertTrue(IpFilter.matchesAddress("0:0:0:0:0:0:0:1", "::1")); // localhost
		assertFalse(IpFilter.matchesAddress("fc00::1", "fc00::2"));
	}

	@Test
	void matchesAddress_IPv6_mixed_with_IPv4() {
		assertFalse(IpFilter.matchesAddress("127.0.0.1", "::1"));
		assertFalse(IpFilter.matchesAddress("::1", "127.0.0.1"));
	}

	@Test
	void matchesAddress_IPv6_illegal_address() {
		assertFalse(IpFilter.matchesAddress("1::2::3", "1:2::3"));
		assertFalse(IpFilter.matchesAddress("1:2::3", "1::2::3"));
	}

	@Test
	void matchesRange_IPv6() {

		assertTrue(IpFilter.matchesRange("::", "::/0"));
		assertTrue(IpFilter.matchesRange("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "::/0"));

		assertTrue(IpFilter.matchesRange("::", "::/128"));
		assertFalse(IpFilter.matchesRange("::1", "::/128"));

		assertFalse(IpFilter.matchesRange("::0", "::1/128"));
		assertTrue(IpFilter.matchesRange("::1", "::1/128"));
		assertFalse(IpFilter.matchesRange("::2", "::1/128"));

		assertFalse(IpFilter.matchesRange("::fffe:ffff:ffff:ffff", "::ffff:0:0:0/96"));
		assertTrue(IpFilter.matchesRange("::ffff:0:0:0", "::ffff:0:0:0/96"));
		assertTrue(IpFilter.matchesRange("::ffff:0:ffff:ffff", "::ffff:0:0:0/96"));
		assertFalse(IpFilter.matchesRange("::ffff:1:0:0", "::ffff:0:0:0/96"));

		assertFalse(IpFilter.matchesRange("64:ff9a:ffff:ffff:ffff:ffff:ffff:ffff", "64:ff9b::/96"));
		assertTrue(IpFilter.matchesRange("64:ff9b::", "64:ff9b::/96"));
		assertTrue(IpFilter.matchesRange("64:ff9b::ffff:ffff", "64:ff9b::/96"));
		assertFalse(IpFilter.matchesRange("64:ff9b::1:0:0", "64:ff9b::/96"));

		assertFalse(IpFilter.matchesRange("64:ff9b:0:ffff:ffff:ffff:ffff:ffff", "64:ff9b:1::/48"));
		assertTrue(IpFilter.matchesRange("64:ff9b:1::", "64:ff9b:1::/48"));
		assertTrue(IpFilter.matchesRange("64:ff9b:1:ffff:ffff:ffff:ffff:ffff", "64:ff9b:1::/48"));
		assertFalse(IpFilter.matchesRange("64:ff9b:2::0", "64:ff9b:1::/48"));

		assertFalse(IpFilter.matchesRange("ff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "100::/64"));
		assertTrue(IpFilter.matchesRange("100::", "100::/64"));
		assertTrue(IpFilter.matchesRange("100::ffff:ffff:ffff:ffff", "100::/64"));
		assertFalse(IpFilter.matchesRange("101::", "100::/64"));

		assertFalse(IpFilter.matchesRange("2000:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "2001:0000::/32"));
		assertTrue(IpFilter.matchesRange("2001::", "2001:0000::/32"));
		assertTrue(IpFilter.matchesRange("2001::ffff:ffff:ffff:ffff:ffff:ffff", "2001:0000::/32"));
		assertFalse(IpFilter.matchesRange("2001:1::", "2001:0000::/32"));

		assertFalse(IpFilter.matchesRange("2001:1f:ffff:ffff:ffff:ffff:ffff:ffff", "2001:20::/28"));
		assertTrue(IpFilter.matchesRange("2001:20::", "2001:20::/28"));
		assertTrue(IpFilter.matchesRange("2001:2f:ffff:ffff:ffff:ffff:ffff:ffff", "2001:20::/28"));
		assertFalse(IpFilter.matchesRange("2001:30::", "2001:20::/28"));

		assertFalse(IpFilter.matchesRange("2001:db7:ffff:ffff:ffff:ffff:ffff:ffff", "2001:db8::/32"));
		assertTrue(IpFilter.matchesRange("2001:db8::", "2001:db8::/32"));
		assertTrue(IpFilter.matchesRange("2001:db8:ffff:ffff:ffff:ffff:ffff:ffff", "2001:db8::/32"));
		assertFalse(IpFilter.matchesRange("2001:db9::", "2001:db8::/32"));

		assertFalse(IpFilter.matchesRange("2001:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "2002::/16"));
		assertTrue(IpFilter.matchesRange("2002::", "2002::/16"));
		assertTrue(IpFilter.matchesRange("2002:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "2002::/16"));
		assertFalse(IpFilter.matchesRange("2003::", "2002::/16"));

		assertFalse(IpFilter.matchesRange("fbff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "fc00::/7"));
		assertTrue(IpFilter.matchesRange("fc00::", "fc00::/7"));
		assertTrue(IpFilter.matchesRange("fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "fc00::/7"));
		assertFalse(IpFilter.matchesRange("fe00::", "fc00::/7"));

		assertFalse(IpFilter.matchesRange("fe7f:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "fe80::/64"));
		assertTrue(IpFilter.matchesRange("fe80::", "fe80::/64"));
		assertTrue(IpFilter.matchesRange("fe80::ffff:ffff:ffff:ffff", "fe80::/64"));
		assertFalse(IpFilter.matchesRange("fe80:0:0:1::", "fe80::/64"));

		assertFalse(IpFilter.matchesRange("feff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "ff00::/8"));
		assertTrue(IpFilter.matchesRange("ff00::", "ff00::/8"));
		assertTrue(IpFilter.matchesRange("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "ff00::/8"));

	}

	@Test
	void matchesRange_IPv6_mixed_with_IPv4() {
		assertFalse(IpFilter.matchesRange("127.0.0.1", "::1/128"));
		assertFalse(IpFilter.matchesRange("::1", "127.0.0.1/8"));
	}

	@Test
	void matchesRange_IPv6_illegal_address() {
		assertFalse(IpFilter.matchesRange("1::2::3", "1:2::3/0"));
		assertFalse(IpFilter.matchesRange("1:2::3", "1::2::3/0"));
	}

}