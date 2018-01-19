package org.ocmc.ioc.liturgical.synch.git.models;

import com.google.gson.annotations.Expose;

public class GitDiffLibraryLine extends LibraryLine {
	public enum STATUSES {
		ADDED_KEY_VALUE
		, MODIFIED_VALUE
		, REMOVED_KEY_VALUE
		, RENAMED_KEY
		, UNKNOWN
		};
	@Expose public STATUSES status = STATUSES.UNKNOWN;
	@Expose public String who = "";
	@Expose public String fromLibrary = "";
	@Expose public String fromTopic = "";
	@Expose public String fromKey = "";
	@Expose public String fromValue = "";
	@Expose public boolean fromValueIsRedirect = false;
	@Expose public String fromComment = "";
	@Expose public boolean isFileDelete = false;
	@Expose public String timestamp = "";
	@Expose public boolean isPlus = false;
	
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
		return fromKey;
	}

	public void setRenameKeyFrom(String renameKeyFrom) {
		this.fromKey = renameKeyFrom;
	}
	
	public boolean hasRenamedKey() {
		return this.fromKey.length() > 0;
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

	public String getFromLibrary() {
		return fromLibrary;
	}

	public void setFromLibrary(String fromLibrary) {
		this.fromLibrary = fromLibrary;
	}

	public String getFromTopic() {
		return fromTopic;
	}

	public void setFromTopic(String fromTopic) {
		this.fromTopic = fromTopic;
	}

	public String getFromKey() {
		return fromKey;
	}

	public void setFromKey(String renamedFromKey) {
		this.fromKey = renamedFromKey;
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

	public STATUSES getStatus() {
		return status;
	}

	public void setStatus(STATUSES status) {
		this.status = status;
	}

	public String getFromValue() {
		return fromValue;
	}

	public void setFromValue(String fromValue) {
		this.fromValue = fromValue;
	}

	public String getFromComment() {
		return fromComment;
	}

	public void setFromComment(String fromComment) {
		this.fromComment = fromComment;
	}

	public boolean isFromValueIsRedirect() {
		return fromValueIsRedirect;
	}

	public void setFromValueIsRedirect(boolean fromValueIsRedirect) {
		this.fromValueIsRedirect = fromValueIsRedirect;
	}

}
