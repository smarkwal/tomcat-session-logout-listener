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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;

class IpFilter {

	private IpFilter() {
		// utility class
	}

	static boolean matches(String remoteAddr, String filter) {
		if (remoteAddr == null) {
			return false;
		}
		if (filter == null) {
			return false;
		}

		String[] filters = filter.split(",");
		for (String filterAddr : filters) {
			filterAddr = filterAddr.trim();

			if (filterAddr.contains("/")) {
				boolean match = matchesRange(remoteAddr, filterAddr);
				if (match) {
					return true;
				}
			} else {
				boolean match = matchesAddress(remoteAddr, filterAddr);
				if (match) {
					return true;
				}
			}
		}

		return false;
	}

	static boolean matchesAddress(String remoteAddr, String filterAddr) {
		if (remoteAddr == null) {
			return false;
		}
		if (filterAddr == null) {
			return false;
		}
		if (filterAddr.equals("*")) {
			return true;
		}

		// validate remote address and filter range
		if (remoteAddr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
			if (filterAddr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
				// IPv4 addresses
			} else {
				return false;
			}
		} else if (remoteAddr.matches("[0-9a-fA-F:]{2,39}")) {
			if (filterAddr.matches("[0-9a-fA-F:]{2,39}")) {
				// IPv6 addresses
			} else {
				return false;
			}
		} else {
			return false;
		}

		try {
			InetAddress remoteInet = InetAddress.getByName(remoteAddr);
			InetAddress filterInet = InetAddress.getByName(filterAddr);
			return remoteInet.equals(filterInet);
		} catch (UnknownHostException e) {
			return false;
		}
	}

	static boolean matchesRange(String remoteAddr, String filterRange) {
		if (remoteAddr == null) {
			return false;
		}
		if (filterRange == null) {
			return false;
		}

		// validate remote address and filter range
		if (remoteAddr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
			if (filterRange.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,3}")) {
				// IPv4 address and IPv4 range
			} else {
				return false;
			}
		} else if (remoteAddr.matches("[0-9a-fA-F:]{2,39}")) {
			if (filterRange.matches("[0-9a-fA-F:]{2,39}/\\d{1,3}")) {
				// IPv6 address and IPv6 range
			} else {
				return false;
			}
		} else {
			return false;
		}

		// split filter range into address and mask
		int pos = filterRange.indexOf('/');
		String filterAddr = filterRange.substring(0, pos);
		String filterMask = filterRange.substring(pos + 1);

		// try to parse IPv4 or IPv6 addresses
		InetAddress remoteInet;
		InetAddress filterInet;
		try {
			remoteInet = InetAddress.getByName(remoteAddr);
			filterInet = InetAddress.getByName(filterAddr);
		} catch (UnknownHostException e) {
			// ignore invalid addresses
			return false;
		}

		// IP address class must match
		if (remoteInet.getClass() != filterInet.getClass()) {
			return false;
		}

		// quick test
		if (remoteInet.equals(filterInet)) {
			return true;
		}

		// convert addresses into bit sets
		BitSet remoteBits = toBitSet(remoteInet);
		BitSet filterBits = toBitSet(filterInet);

		// compare bit sets up to mask limit
		int bits = Integer.parseInt(filterMask); // number of equal bits (starting from left)
		for (int b = 0; b < bits; b++) {
			boolean remoteBit = remoteBits.get(b);
			boolean filterBit = filterBits.get(b);
			if (remoteBit != filterBit) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Converts the given IP address into a bit set, using big-endian byte and
	 * bit order.
	 *
	 * @param address IP address to convert.
	 * @return Bit set.
	 */
	static BitSet toBitSet(InetAddress address) {

		// get raw address bytes
		byte[] bytes = address.getAddress();

		// prepare bit set
		BitSet bitSet = new BitSet(bytes.length * 8);

		// loop over all bytes (left to right, 0 to 3 or 0 to 15)
		for (int byteIndex = 0; byteIndex < bytes.length; byteIndex++) {

			// get byte value
			byte value = bytes[byteIndex];

			// loop over all bits (left to right, 0 to 7)
			for (int bitIndex = 0; bitIndex < 8; bitIndex++) {

				// check if bit is set
				int mask = 1 << (7 - bitIndex);
				if ((value & mask) != 0) {

					// set bit in bit set
					int index = byteIndex * 8 + bitIndex;
					bitSet.set(index);
				}
			}
		}

		return bitSet;
	}

}
