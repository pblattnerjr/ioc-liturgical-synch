package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

public enum DataStore {
	UNKNOWN(-1)
	, ARES(0) // an ALWB ares file
	, HISTORY(1) // H2
	, DB(2) // CouchDB
	, JTEM(3) // the Json version of an ALWB atem file
	, ALWB(4) // ALWB -- could be either ARES or JTEM
	;
	
	private final int code;
	
	private DataStore(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
