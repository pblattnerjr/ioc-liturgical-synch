package org.ocmc.ioc.liturgical.synch.git.models.github;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class CommitDetails extends AbstractModel {
	
	@Expose public Person committer = new Person();
	@Expose public Person author = new Person();
	@Expose public String html_url = "";
	@Expose public Commit commit = new Commit();
	@Expose public String comments_url = "";
	@Expose public String sha = "";
	@Expose public String url = "";
	@Expose public List<Parent> parents = new ArrayList<Parent>();
	
	public CommitDetails() {
		super();
	}

	public Person getCommitter() {
		return committer;
	}

	public void setCommitter(Person committer) {
		this.committer = committer;
	}

	public Person getAuthor() {
		return author;
	}

	public void setAuthor(Person author) {
		this.author = author;
	}

	public String getHtml_url() {
		return html_url;
	}

	public void setHtml_url(String html_url) {
		this.html_url = html_url;
	}

	public Commit getCommit() {
		return commit;
	}

	public void setCommit(Commit commit) {
		this.commit = commit;
	}

	public String getComments_url() {
		return comments_url;
	}

	public void setComments_url(String comments_url) {
		this.comments_url = comments_url;
	}

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Parent> getParents() {
		return parents;
	}

	public void setParents(List<Parent> parents) {
		this.parents = parents;
	}

}
