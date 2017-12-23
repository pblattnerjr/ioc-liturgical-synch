package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of Ares Key: template.titles_gr_GR_cog.d.onSundayEvening
 * Example of CouchDBKey: template.titles|d.onSundayEvening"
 * @author mac002
 *
 */
public class KeyNameParts {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyNameParts.class);
	private String domain;
	private String topic;
	private String key;
	
	public KeyNameParts() {
		
	}
	
	public KeyNameParts(String fullKey) {
		parseAresKey(fullKey);
	}
	
	public String getCouchDbKey() {
		return topic + "|" + key;
	}
	
	public String getAresKey() {
		return topic + "_" + domain + "." + key;
	}

	public void parseCouchDbKey(String domain, String key) {
		this.domain = domain;
		try {
			String[] parts = key.split("|");
			if (parts.length == 2) {
				this.topic = parts[0];
				this.key = parts[1];
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage() + " using | to split " + key);
		}
	}
	public  void parseAresKey(String key) {
		try {
			String[] parts = key.split("_");
			if (parts.length == 4) { // all ok
				topic = parts[0].trim();
				domain = parts[1].trim() + "_" + parts[2].trim() +"_";
				String[] subparts = parts[3].trim().split("\\.");
				domain = domain + subparts[0].trim();
				this.key = parts[3].replaceFirst(subparts[0]+"\\.", "").trim();
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e, " splitting " + key);
		}
	}
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	

	public static void main(String[] args) {
		 String ares = "template.titles_gr_GR_cog.d.onSundayEvening";
		 String couchDBKey = "template.titles|d.onSundayEvening";
		 KeyNameParts parts  = new KeyNameParts();
		 parts.parseAresKey(ares);
		 System.out.println("Domain: " + parts.getDomain());
		 System.out.println("Topic: " + parts.getTopic());
		 System.out.println("Key: " + parts.getKey());
		 parts.parseCouchDbKey("gr_GR_cog", couchDBKey);
		 System.out.println("Domain: " + parts.getDomain());
		 System.out.println("Topic: " + parts.getTopic());
		 System.out.println("Key: " + parts.getKey());
		 System.out.println(parts.getAresKey());
		 System.out.println(parts.getCouchDbKey());
	}

}
