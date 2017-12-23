package org.ocmc.ioc.liturgical.synch.alwb.gateway.enums;

public enum RuleActionValues {
	CREATE_IN_ALWB(Action.CREATE, DataStore.ALWB, "create-in-alwb-if")
	, UPDATE_IN_ALWB(Action.CREATE, DataStore.ALWB, "update-in-alwb-if")
	, DELETE_IN_ALWB(Action.CREATE, DataStore.ALWB, "delete-in-alwb-if")
	, CREATE_IN_DB(Action.CREATE,  DataStore.DB, "create-in-db-if")
	, UPDATE_IN_DB(Action.CREATE,  DataStore.DB, "update-in-db-if")
	, DELETE_IN_DB(Action.CREATE,  DataStore.DB, "delete-in-db-if")
	;

	private Action action;
	private DataStore target;
	private final String jsonId;
	
	private RuleActionValues(
			Action action
			, DataStore target
			, String jsonId) {
		this.action = action;
		this.target = target;
		this.jsonId = jsonId;
	}
	
	
	public String toJsonId() {
		return this.jsonId;
	}


	public Action getAction() {
		return action;
	}


	public DataStore getTarget() {
		return target;
	}
	
	public boolean isCreate() {
		return action == Action.CREATE;
	}

	public boolean isUpdate() {
		return action == Action.UPDATE;
	}

	public boolean isDelete() {
		return action == Action.DELETE;
	}
	
	public boolean hasTargetAlwb() {
		return target == DataStore.ALWB;
	}
	public boolean hasTargetDb() {
		return target == DataStore.DB;
	}
}
