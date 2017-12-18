package org.ocmc.ioc.liturgical.synch.git.models.github;

import com.google.gson.annotations.Expose;

/**
 * Covers added, modified, removed.  But not renamed. See CommiteFileRenamed.  
 * @author mac002
 *
 */
public class CommitFileChanged extends CommitFile {
	
	@Expose public String patch = "";

	public CommitFileChanged() {
		super();
	}

	public String getPatch() {
		return patch;
	}

	public void setPatch(String patch) {
		this.patch = patch;
	}

}
