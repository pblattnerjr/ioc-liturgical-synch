package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.FileNameParts;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

public class GatewayUtils {
	final static Logger logger = LoggerFactory.getLogger(GatewayUtils.class);

	/**
	 * Parse the given filename into parts, topic and domain.
	 * For example, da_d0_en_US_dedes
	 * @param filename the filename
	 * @return FileNameParts instance
	 */
	public static FileNameParts getAresFileNameParts(String filename) {
		FileNameParts result = null;
		String topic = "";
		String domain = "";
		String localName = filename.replace(".ares", "");
		if (localName.contains("/")) {
			try {
				String[] parts = localName.split("\\/");
				localName = parts[parts.length-1];
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}
		}
		try {
			if (localName.startsWith("._")) { // files copied from Mac get this prefix.  Causes issues on Windows.
				localName = localName.substring(2);
			}
			String [] parts = localName.split("_");
			topic = parts[0];
			domain = parts[1];
			for (int i = 2; i < parts.length; i++) {
				domain = domain + "_" + parts[i]; 
			}
			result = new FileNameParts(topic,domain);
		} catch (Exception e) {
			result = null;
			if (! filename.contains("_")) {
				logger.warn(filename + "  missing realm in filename, e.g. gr_GR_cog.");
			} else {
				logger.warn(filename + "  not named correctly.");
			}
		}
		return result;
	}

	/**
	 * For the given path, replace the domain information with the new domain
	 * @param path the path
	 * @param oldDomain the old domain
	 * @param newDomain the new domain
	 * @return new domain info
	 */
	public static String replaceDomain(
			String path
			, String oldDomain
			, String newDomain
			) {
		String result = "";
		Domain theOldDomain = new Domain(oldDomain);
		Domain theNewDomain = new Domain(newDomain);
		result = path.replaceAll(oldDomain, newDomain);
		result = result.replaceAll(theOldDomain.getLanguage_Realm(), theNewDomain.getLanguage_Realm());
		result = result.replaceAll(theOldDomain.getLanguage_Country(), theNewDomain.getLanguage_Country());
		return result;
	}
	
	/**
	 * Extract the domain part of an ares file name
	 * @param aresFileName name
	 * @return the domain 
	 */
	public static String getDomain(String aresFileName) {
		FileNameParts parts = getAresFileNameParts(aresFileName);
		return parts.getDomain();
	}
	

}
