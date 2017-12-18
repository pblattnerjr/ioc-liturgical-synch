package org.ocmc.ioc.liturgical.synch.git.models.github;

import com.google.gson.annotations.Expose;

public class CommitFileRenamed extends CommitFile {
	
	@Expose public String previous_filename = "";
	
	public CommitFileRenamed() {
		super();
	}

	public String getPrevious_filename() {
		return previous_filename;
	}

	public void setPrevious_filename(String previous_filename) {
		this.previous_filename = previous_filename;
	}

}
