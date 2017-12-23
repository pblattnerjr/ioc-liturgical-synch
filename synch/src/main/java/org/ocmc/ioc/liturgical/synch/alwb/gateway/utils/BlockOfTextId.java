package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.DomainNameParts;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.FileNameParts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockOfTextId {
	private static final Logger LOGGER = LoggerFactory.getLogger(BlockOfTextId.class);
	private String domain;
	private DomainNameParts domainNameParts;
	private String topic;
	private String key;
	private String delimiter;
	
	public BlockOfTextId(
			String domain
			, String topic
			, String key
			, String delimiter
			) {
		domainNameParts = new DomainNameParts(domain);
		this.domain = domain.toLowerCase();
		this.topic = topic;
		this.key = key;
		this.delimiter = delimiter;
	}
	
	public BlockOfTextId(FileNameParts parts, String key, String delimiter) {
		domain = parts.getDomain();
		topic = parts.getTopic();
		this.key = key;
		this.delimiter = delimiter;
	}
	
	public BlockOfTextId(String id, String delimiter) {
		this.delimiter = delimiter;
		String splitter = delimiter;
		try {
			if (delimiter.startsWith("|")) {
				splitter = "\\|";
			}
			String[] parts = id.split(splitter);
			if (parts != null && parts.length == 3) {
				domain = parts[0];
				topic = parts[1];
				key = parts[2];
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage() + " splitting " + id + " with delimiter " + delimiter);
		}
	}

	public String getDomain() {
		return domain;
	}
	
	public String getDomainAresStyle() {
		return domainNameParts.getDomainAresStyle();
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

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	
	/**
	 * 
	 * @return topic + delimiter + key
	 */
	public String getId() {
		return topic + delimiter + key;
	}
	
	/**
	 * 
	 * @return domain + delimiter + topic + delimiter + key
	 */
	public String getFullId() {
		return domain + delimiter + topic + delimiter + key;
	}
	
	/**
	 * 
	 * @return topic_domain, with domain lowercase, couchDB style
	 */
	public String getResource() {
		return topic + "_" + domain;
	}
	
	/**
	 * 
	 * @return topic_domain, with the country code uppercase, ares style
	 */
	public String getResourceAresStyle() {
		return topic + "_" + getDomainAresStyle();
	}
}
