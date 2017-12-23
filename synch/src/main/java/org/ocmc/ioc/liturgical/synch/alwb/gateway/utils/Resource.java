package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

public class Resource {
	String resource;
	String topic;
	Domain domain;
	boolean validResource;
	
	public Resource(String domain, String topic) {
		this.domain = new Domain(domain);
		this.topic = topic;
		resource = topic + "_" + domain;
	}
	
	public Resource(String resource) {
		this.resource = resource;
		validResource = false;
		try {
			String [] parts = resource.split("_");
			if (parts.length > 3) {
				topic = parts[0];
				String substring = resource.replaceFirst(topic+"_", "");
				domain = new Domain(substring);
				if (domain.isValidDomain()) {
					validResource = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getResource() {
		return resource;
	}

	public String getTopic() {
		return topic;
	}

	public Domain getDomain() {
		return domain;
	}

	public boolean isValidResource() {
		return validResource;
	}
}
