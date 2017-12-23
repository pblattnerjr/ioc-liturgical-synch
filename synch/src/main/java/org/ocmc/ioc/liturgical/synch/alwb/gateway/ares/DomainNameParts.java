package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

public class DomainNameParts {
	private String language;
	private String country;
	private String realm;
	
	public DomainNameParts(String domain) {
		String[] parts = domain.split("_");
		if (parts.length > 2) {
			language = parts[0];
			country = parts[1];
			realm = parts[2];
		}
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}
	
	public String getDomainCouchDBStyle() {
		return language.toLowerCase() + "_" + country.toLowerCase() + "_" + realm.toLowerCase();
	}
	public String getDomainAresStyle() {
		return language.toLowerCase() + "_" + country.toUpperCase() + "_" + realm.toLowerCase();
	}
}
