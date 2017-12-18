package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

/**
 * Response to:
 *
 * https://api.github.com/rate_limit
 * 
 * rate is deprecated.  Use core.
 * The limit is 5000 per hour per token
 *
 * @author mac002
 *
 */
public class RateLimit extends AbstractModel {
	
	@Expose public RateInfo core = new RateInfo();
	@Expose public RateInfo search = new RateInfo();
	@Expose public RateInfo graphql = new RateInfo();
	@Expose public RateInfo rate = new RateInfo(); // deprecated.  Use core.
	
	public RateLimit() {
		super();
	}


	public RateInfo getSearch() {
		return search;
	}

	public void setSearch(RateInfo search) {
		this.search = search;
	}

	public RateInfo getGraphql() {
		return graphql;
	}

	public void setGraphql(RateInfo graphql) {
		this.graphql = graphql;
	}

	public RateInfo getRate() {
		return rate;
	}

	public void setRate(RateInfo rate) {
		this.rate = rate;
	}


	public RateInfo getCore() {
		return core;
	}


	public void setCore(RateInfo core) {
		this.core = core;
	}
}
