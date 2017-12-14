package org.ocmc.ioc.liturgical.synch.git.models;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class SynchStatus extends AbstractModel {
	@Expose int creates = 0;
	@Expose int renames = 0;
	@Expose int updates = 0;
	@Expose int deletes = 0;
	@Expose int fileCreates = 0;
	@Expose int fileDeletes = 0;
	@Expose int fileRenames = 0;
	@Expose List<String> newFiles = new ArrayList<String>();
	@Expose List<String> deletedFiles = new ArrayList<String>();
	
	public SynchStatus() {
		super();
	}

	public int getCreates() {
		return creates;
	}

	public void setCreates(int creates) {
		this.creates = creates;
	}

	public int getRenames() {
		return renames;
	}

	public void setRenames(int renames) {
		this.renames = renames;
	}

	public int getUpdates() {
		return updates;
	}

	public void setUpdates(int updates) {
		this.updates = updates;
	}

	public int getDeletes() {
		return deletes;
	}

	public void setDeletes(int deletes) {
		this.deletes = deletes;
	}
	
	public void createPlus() {
		this.creates++;
	}
	public void renamePlus() {
		this.renames++;
	}
	public void updatesPlus() {
		this.updates++;
	}
	public void deletesPlus() {
		this.deletes++;
	}
	
	public String toSummary() {
		StringBuffer result = new StringBuffer();
		if (this.creates > 0) {
			result.append("C:" + this.creates);
		}
		if (this.renames > 0) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append("R:" + this.renames);
		}
		if (this.updates > 0) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append("U:" + this.updates);
		}
		if (this.deletes > 0) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append("D:" + this.deletes);
		}
		if (this.fileCreates > 0) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append("FC:" + this.fileCreates);
		}
		if (this.fileDeletes > 0) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append("FD:" + this.fileDeletes);
		}
		if (this.fileRenames > 0) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append("FR:" + this.fileRenames);
		}
		return result.toString();
	}
	
	public int transactionCount() {
		return this.creates + this.renames + this.updates + this.deletes;
	}
	
	public void addStatus(SynchStatus status) {
		this.creates = this.creates + status.creates;
		this.renames = this.renames + status.renames;
		this.updates = this.updates + status.updates;
		this.deletes = this.deletes + status.deletes;
		this.fileDeletes = this.fileDeletes + status.fileDeletes;
		this.fileRenames = this.fileRenames + status.fileRenames;
		this.fileCreates = this.fileCreates + status.fileCreates;
	}

	public int getFileDeletes() {
		return fileDeletes;
	}

	public void setFileDeletes(int fileDeletes) {
		this.fileDeletes = fileDeletes;
	}

	public int getFileRenames() {
		return fileRenames;
	}

	public void setFileRenames(int fileRenames) {
		this.fileRenames = fileRenames;
	}

	public List<String> getDeletedFiles() {
		return deletedFiles;
	}

	public void setDeletedFiles(List<String> deletedFiles) {
		this.deletedFiles = deletedFiles;
	}

	public List<String> getNewFiles() {
		return newFiles;
	}

	public void setNewFiles(List<String> newFiles) {
		this.newFiles = newFiles;
	}
	
	public void recordFileCreate(String filename) {
		if (! this.newFiles.contains(filename)) {
			this.newFiles.add(filename);
		}
	}

	public void recordFileDelete(String filename) {
		if (! this.deletedFiles.contains(filename)) {
			this.deletedFiles.add(filename);
		}
	}

	public int getFileCreates() {
		return fileCreates;
	}

	public void setFileCreates(int fileCreates) {
		this.fileCreates = fileCreates;
	}
	
}
