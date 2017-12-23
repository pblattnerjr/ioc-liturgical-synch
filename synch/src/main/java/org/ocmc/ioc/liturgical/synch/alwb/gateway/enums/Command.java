package org.ocmc.ioc.liturgical.synch.alwb.gateway.enums;

import java.util.Map;
import java.util.TreeMap;

public enum Command {
	ARES_MERGE (true, false, DataStore.DB, "ares-merge")
	, ARES_PUSH_FORCED (true, false, DataStore.DB, "ares-push-forced")
	, ARES_PUSH_NON_FORCED (false, false, DataStore.DB, "ares-push-non-forced")
	, ARES_PULL_FORCED (true, true, DataStore.ARES, "ares-pull-forced")
	, ARES_PULL_NON_FORCED (false, true, DataStore.ARES, "ares-pull-non-forced")
	, JTEM_PUSH_FORCED (true, false, DataStore.DB, "jtem-push-forced")
	, JTEM_PUSH_NON_FORCED (false, false, DataStore.DB, "jtem-push-non-forced")
	, JTEM_PULL_FORCED (true, true, DataStore.JTEM, "jtem-pull-forced")
	, JTEM_PULL_NON_FORCED (false, true, DataStore.JTEM, "jtem-pull-non-forced")
	;

	private final boolean forced;
	private final boolean pull;
	private final DataStore target;
	private final String jsonId;
	
	private Command(
			boolean forced
			, boolean pull
			, DataStore target
			, String jsonId) {
		this.forced = forced;
		this.pull = pull;
		this.target = target;
		this.jsonId = jsonId;
	}
	
	public boolean forced() {
		return this.forced;
	}
	
	public boolean pull() {
		return this.pull;
	}
	
	public boolean push() {
		return ! this.pull;
	}
	
	public boolean targetAres() {
		return this.target == DataStore.ARES;
	}

	public boolean targetDb() {
		return this.target == DataStore.DB;
	}
	
	public String toJsonId() {
		return this.jsonId;
	}
	
	/**
	 * Find the Command for this json ID
	 * @param id the id
	 * @return Command
	 */
	public static Command forJsonId(String id) {
		for (Command c : Command.values()) {
			if (c.jsonId.equals(id)) {
				return c;
			}
		}
		return null;
	}

}
