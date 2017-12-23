package org.ocmc.ioc.liturgical.synch.alwb.gateway.enums;

public enum Action {
	ERROR(-1)
	, NO_ACTION(0)
	, CREATE(1)
	,UPDATE(2)
	, DELETE(3);
	
	private final int actionCode;
	
	private Action(int actionCode) {
		this.actionCode = actionCode;
	}
	
	public int getActionCode() {
		return this.actionCode;
	}
}
