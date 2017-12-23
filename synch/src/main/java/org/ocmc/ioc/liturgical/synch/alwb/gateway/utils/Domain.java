package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

/**
 * Convenience class for handling the parts of a domain.
 * A domain is used in the name of ares files.
 * A domain has a code for language_country_realm,
 * e.g. gr_GR_cog, Greek as spoken in Greece, with the
 * text being for the Church of Greece.
 * @author mac002
 *
 */
public class Domain {
	String language;
	String country;
	String realm;
	String domain;
	boolean validDomain;
	
	public Domain(String domain) {
		this.domain = domain;
		validDomain = false;
		try {
			String parts[] = domain.split("_");
			if (parts.length == 3) {
				language = parts[0];
				country = parts[1];
				realm = parts[2];
				validDomain = true;
			}
		} catch (Exception e) {}
	}

	public String getLanguage() {
		return language;
	}

	public String getCountry() {
		return country;
	}

	public String getRealm() {
		return realm;
	}

	public String getDomain() {
		return domain;
	}

	public boolean isValidDomain() {
		return validDomain;
	}
	
	/**
	 * RestManager the language and realm portions of the domain,
	 * separated by an underscore
	 * @return language_realm
	 */
	public String getLanguage_Realm() {
		return language + "_" + realm;
	}
	
	/**
	 * RestManager the language and country portions of the domain,
	 * separated by an underscore
	 * @return language_country
	 */
	public String getLanguage_Country() {
		return language + "_" + country;
	}
	
	public String toString() {
		return domain;
	}
	

}
