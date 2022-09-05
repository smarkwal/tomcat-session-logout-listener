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
		for (String address : filters) {
			address = address.trim();

			if (address.contains("/")) {
				boolean match = matchesRange(remoteAddr, address);
				if (match) {
					return true;
				}
			} else {
				boolean match = matchesAddress(remoteAddr, address);
				if (match) {
					return true;
				}
			}
		}

		return false;
	}

	static boolean matchesAddress(String remoteAddr, String address) {
		if (remoteAddr == null) {
			return false;
		}
		if (address == null) {
			return false;
		}
		if (address.equals("*")) {
			return true;
		}
		return remoteAddr.equals(address);
	}

	static boolean matchesRange(String remoteAddr, String range) {
		if (remoteAddr == null) {
			return false;
		}
		if (!remoteAddr.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
			return false;
		}

		if (range == null) {
			return false;
		}
		if (!range.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")) {
			return false;
		}

		int pos = range.indexOf('/');
		String subnet = range.substring(0, pos);
		String mask = range.substring(pos + 1);

		long subnetLong = parseAddress(subnet);
		long remoteAddrLong = parseAddress(remoteAddr);

		long size = 1L << (32 - Integer.parseInt(mask));
		subnetLong = subnetLong & -size; // set all bits to the right of the mask to 0

		return remoteAddrLong >= subnetLong && remoteAddrLong < subnetLong + size;
	}

	static long parseAddress(String address) {
		if (!address.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
			throw new IllegalArgumentException("address");
		}
		String[] parts = address.split("\\.");
		long result = 0;
		for (int i = 0; i < 4; i++) {
			long number = Long.parseLong(parts[i]);
			result += (number % 256) << (8 * (3 - i));
		}
		return result;
	}

}
