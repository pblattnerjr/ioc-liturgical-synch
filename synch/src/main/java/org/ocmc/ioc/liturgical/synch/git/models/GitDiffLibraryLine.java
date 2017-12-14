package org.ocmc.ioc.liturgical.synch.git.models;

public class GitDiffLibraryLine extends LibraryLine {
	private String who = "";
	private String renamedFromLibrary = "";
	private String renamedFromTopic = "";
	private String renamedFromKey = "";
	private boolean isPlus = true;
	private boolean isFileDelete = false;
	private String timestamp = "";
	
	public GitDiffLibraryLine(String lineNbr, String line) {
		super(lineNbr, line);
	}

	public boolean isPlus() {
		return isPlus;
	}

	public void setPlus(boolean isPlus) {
		this.isPlus = isPlus;
	}

	public String getRenameKeyFrom() {
		return renamedFromKey;
	}

	public void setRenameKeyFrom(String renameKeyFrom) {
		this.renamedFromKey = renameKeyFrom;
	}
	
	public boolean hasRenamedKey() {
		return this.renamedFromKey.length() > 0;
	}
	
	public String toSummary() {
		return    
				(this.isPlus() ? "+" : "-")
				+ this.getDomain() + "~"
				+ this.getTopic() + "~"
				+ this.getKey()
				+ " = "
				+ this.getValue()
				+ (this.hasCommentAfterValue ? " // " + this.getComment() : "")
		;

	}

	public String getRenamedFromLibrary() {
		return renamedFromLibrary;
	}

	public void setRenamedFromLibrary(String renamedFromLibrary) {
		this.renamedFromLibrary = renamedFromLibrary;
	}

	public String getRenamedFromTopic() {
		return renamedFromTopic;
	}

	public void setRenamedFromTopic(String renamedFromTopic) {
		this.renamedFromTopic = renamedFromTopic;
	}

	public String getRenamedFromKey() {
		return renamedFromKey;
	}

	public void setRenamedFromKey(String renamedFromKey) {
		this.renamedFromKey = renamedFromKey;
	}

	public boolean isFileDelete() {
		return isFileDelete;
	}

	public void setFileDelete(boolean isFileDelete) {
		this.isFileDelete = isFileDelete;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getWho() {
		return who;
	}

	public void setWho(String who) {
		this.who = who;
	}

}
