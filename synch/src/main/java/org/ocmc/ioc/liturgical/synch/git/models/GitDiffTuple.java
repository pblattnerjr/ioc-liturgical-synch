package org.ocmc.ioc.liturgical.synch.git.models;

public class GitDiffTuple {
	public enum TYPES {
		CHANGE
		, MINUS_SINGLETON
		, PLUS_SINGLETON
		, RENAME
		, UNKNOWN
	};
	private TYPES type = TYPES.UNKNOWN;
	private GitDiffLibraryLine minus = null;
	private GitDiffLibraryLine plus = null;
	public GitDiffLibraryLine getMinus() {
		return minus;
	}
	public void setMinus(GitDiffLibraryLine minus) {
		this.minus = minus;
		this.categorize();
	}
	public GitDiffLibraryLine getPlus() {
		return plus;
	}
	public void setPlus(GitDiffLibraryLine plus) {
		this.plus = plus;
		this.categorize();
	}
	
	public TYPES getType() {
		return type;
	}
	
	public void setType(TYPES type) {
		this.type = type;
	}
	
	private void categorize() {
		if (minus == null) {
			if (plus == null) {
				this.type = TYPES.UNKNOWN;
			} else {
				this.type = TYPES.PLUS_SINGLETON;
			}
		} else {
			if (plus == null) {
				this.type = TYPES.MINUS_SINGLETON;
			} else {
				this.type = TYPES.CHANGE;
			}
		}
		if (plus != null && plus.hasRenamedKey()) {
			this.type = TYPES.RENAME;
		}
	}
}
