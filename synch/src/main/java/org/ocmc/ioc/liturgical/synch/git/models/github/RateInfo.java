package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class RateInfo extends AbstractModel {
	
	@Expose public int limit = 0;
	@Expose public int remaining = 0;
	@Expose public int reset = 0;
	
	public RateInfo() {
		super();
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getRemaining() {
		return remaining;
	}

	public void setRemaining(int remaining) {
		this.remaining = remaining;
	}

	public int getReset() {
		return reset;
	}

	public void setReset(int reset) {
		this.reset = reset;
	}
}
