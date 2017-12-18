package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class PersonEvent extends AbstractModel {
	
	@Expose public String date = "" ; // "2017-12-14T17:40:48Z",
	@Expose public String name = ""; // "Michael Colburn",
	@Expose public String email = ""; // "mac002@thecolburns.us"

	public PersonEvent() {
		super();
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
