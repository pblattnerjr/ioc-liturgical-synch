package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class Object extends AbstractModel {
	@Expose public String sha = "";
	@Expose public String type = "";
	@Expose public String url = "";

	public Object() {
		super();
	}

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
