package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;
import com.google.gson.annotations.Expose;

public class Commit extends AbstractModel {
	@Expose public int comment_count ;
	@Expose  public PersonEvent committer = new PersonEvent();
	@Expose public PersonEvent author = new PersonEvent();
	@Expose public Tree tree = new Tree();
	@Expose public String message = "";
	@Expose public String url = "";
	@Expose public Verification verification = new Verification();

	
	
	public Commit() {
		super();
	}



	public int getComment_count() {
		return comment_count;
	}



	public void setComment_count(int comment_count) {
		this.comment_count = comment_count;
	}



	public PersonEvent getCommitter() {
		return committer;
	}



	public void setCommitter(PersonEvent committer) {
		this.committer = committer;
	}



	public PersonEvent getAuthor() {
		return author;
	}



	public void setAuthor(PersonEvent author) {
		this.author = author;
	}



	public Tree getTree() {
		return tree;
	}



	public void setTree(Tree tree) {
		this.tree = tree;
	}



	public String getMessage() {
		return message;
	}



	public void setMessage(String message) {
		this.message = message;
	}



	public String getUrl() {
		return url;
	}



	public void setUrl(String url) {
		this.url = url;
	}



	public Verification getVerification() {
		return verification;
	}



	public void setVerification(Verification verification) {
		this.verification = verification;
	}
}
