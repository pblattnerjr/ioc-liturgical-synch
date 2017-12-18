package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class CommitFile extends AbstractModel {
	
	@Expose public String sha = "";
	@Expose public String filename = "";
	@Expose public String status = ""; // added, modified, removed, renamed, 
	@Expose public int additions = 0;
	@Expose public int deletions = 0;
	@Expose public int changes = 0;
	@Expose public String blob_url = "";
	@Expose public String raw_url = "";
	@Expose public String contents_url = "";

	public CommitFile() {
		super();
	}

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status.toLowerCase();
	}

	public int getAdditions() {
		return additions;
	}

	public void setAdditions(int additions) {
		this.additions = additions;
	}

	public int getDeletions() {
		return deletions;
	}

	public void setDeletions(int deletions) {
		this.deletions = deletions;
	}

	public int getChanges() {
		return changes;
	}

	public void setChanges(int changes) {
		this.changes = changes;
	}

	public String getBlob_url() {
		return blob_url;
	}

	public void setBlob_url(String blob_url) {
		this.blob_url = blob_url;
	}

	/**
	 * 
	 * @return the url to get the actual contents of the file
	 */
	public String getRaw_url() {
		return raw_url;
	}

	public void setRaw_url(String raw_url) {
		this.raw_url = raw_url;
	}

	public String getContents_url() {
		return contents_url;
	}

	public void setContents_url(String contents_url) {
		this.contents_url = contents_url;
	}

	public boolean fileWasAdded() {
		return this.status.equals("added");
	}
	public boolean fileWasModified() {
		return this.status.equals("modified");
	}
	public boolean fileWasRemoved() {
		return this.status.equals("removed");
	}
	public boolean fileWasRenamed() {
		return this.status.equals("renamed");
	}
}
