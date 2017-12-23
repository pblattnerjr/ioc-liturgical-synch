package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

import java.io.File;
import java.util.Iterator;

public class LibraryUtils {
	
	public final static String QUOTE = "\"";


	/**
	 * Remove leading and trailing quote from a quoted string
	 * @param s the string
	 * @return unquoted string
	 */
	public static String trimQuotes(String s) {
		String result = s;
		if (s.length() > 2) {
			if (s.startsWith(QUOTE) || s.startsWith("“")) {
				result = s.substring(1, s.length());
			}
			if (s.endsWith(QUOTE) || s.endsWith("”")) {
				result = result.substring(0, s.length()-2);
			}
		} else if (s.compareTo(QUOTE+QUOTE) == 0) {
			result = "";
		}
		return result;
	}
	
	/**
	 * Wrap the string in quotes
	 * @param s the string
	 * @return quoted string
	 */
	public static String wrapQuotes(String s) {
		if (s.length() > 0) {
			return QUOTE + escapeQuotes(s) + QUOTE;
		} else {
			return QUOTE+QUOTE;
		}
	}
	
	
	public static String escapeQuotes(String text) {
		return text.replaceAll(QUOTE, "\\\\"+QUOTE);
	}
	
	public static String unescapeQuotes(String text) {
			return text.replaceAll("\\\\"+QUOTE,QUOTE);
	}

	/**
	 * Merges two ares files.  They need to be for the same
	 * resource.  Use this when one file contains a subset of the keys and
	 * the other file contains the full set of keys.
	 * @param from - the ares file from which to pull values
	 * @param into - the ares file into which to put the new values
	 */
	public static void merge(String from, String into) {
		try {
			LibraryFileProxy fromProxy = new LibraryFileProxy(new File(from));
			LibraryFileProxy toProxy = new LibraryFileProxy(new File(into));
			Iterator<String> it = fromProxy.getKeys();
			String key;
			String value;
			while (it.hasNext()) {
				key = it.next();
				value = fromProxy.getValue(key);
				toProxy.setValue(key, value);
			}
			toProxy.writeAresFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
