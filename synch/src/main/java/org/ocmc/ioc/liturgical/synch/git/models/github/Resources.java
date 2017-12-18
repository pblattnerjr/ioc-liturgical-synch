package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class Resources extends AbstractModel {
	@Expose public RateLimit resources = new RateLimit();
	
	public Resources() {
		super();
	}

	public RateLimit getResources() {
		return resources;
	}

	public void setResources(RateLimit resources) {
		this.resources = resources;
	}

}
