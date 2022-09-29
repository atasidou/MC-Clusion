package org.crypto.sse;

import java.util.HashMap;
import java.util.Map;

public class ResolverDB {
	private static Map<byte[][], String> db = new HashMap<byte[][], String>();

	public static Map<byte[][], String> getDb() {
		return db;
	} 
}
