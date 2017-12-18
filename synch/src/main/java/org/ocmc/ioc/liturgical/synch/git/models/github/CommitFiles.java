package org.ocmc.ioc.liturgical.synch.git.models.github;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class CommitFiles extends AbstractModel {

	@Expose public List<CommitFile> files = new ArrayList<CommitFile>();
	
	public CommitFiles() {
		super();
	}

	public List<CommitFile> getFiles() {
		return files;
	}

	public void setFiles(List<CommitFile> files) {
		this.files = files;
	}
	
	public void addFile(CommitFile file) {
		this.files.add(file);
	}
}
