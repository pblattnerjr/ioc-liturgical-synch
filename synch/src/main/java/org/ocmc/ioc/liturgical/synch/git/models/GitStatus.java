package org.ocmc.ioc.liturgical.synch.git.models;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;

import com.google.gson.annotations.Expose;

public class GitStatus extends AbstractModel {
	@Expose List<GithubRepo> cloned = new ArrayList<GithubRepo>();
	@Expose List<GithubRepo> updated = new ArrayList<GithubRepo>();
	@Expose List<GithubRepo> unchanged = new ArrayList<GithubRepo>();
	@Expose List<GithubRepo> errors = new ArrayList<GithubRepo>();

	public GitStatus() {
		super();
	}

	public void addCloned(GithubRepo clone) {
		this.cloned.add(clone);
	}

	public void addUpdated(GithubRepo pull) {
		this.updated.add(pull);
	}
	
	public void addUnchanged(GithubRepo pull) {
		this.unchanged.add(pull);
	}
	
	public void addError(GithubRepo error) {
		this.errors.add(error);
	}

	public String toSummary() {
		return "Cloned: " + this.cloned.size() 
				+ ". Unchanged: " + this.unchanged.size()
				+ ". Updated: " + this.updated.size()
				+ ". With errors: " + this.errors.size()
		;
	}

	public List<GithubRepo> getCloned() {
		return cloned;
	}

	public void setCloned(List<GithubRepo> cloned) {
		this.cloned = cloned;
	}

	public List<GithubRepo> getUpdated() {
		return updated;
	}

	public void setUpdated(List<GithubRepo> updated) {
		this.updated = updated;
	}

	public List<GithubRepo> getUnchanged() {
		return unchanged;
	}

	public void setUnchanged(List<GithubRepo> unchanged) {
		this.unchanged = unchanged;
	}

	public List<GithubRepo> getErrors() {
		return errors;
	}

	public void setErrors(List<GithubRepo> errors) {
		this.errors = errors;
	}
}
