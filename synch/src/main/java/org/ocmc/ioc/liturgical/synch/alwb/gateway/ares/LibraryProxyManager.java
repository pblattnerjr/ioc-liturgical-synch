package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.utils.GatewayUtils;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.utils.Resource;
import org.ocmc.ioc.liturgical.utils.ApacheFileUtils;
import org.ocmc.ioc.liturgical.utils.FileUtils;
import org.ocmc.ioc.liturgical.utils.GeneralUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

/**
 * Manages all occurrences of proxies for ares files. Each ares file that has a
 * domain, e.g. en_US_dedes, is included. A proxy is created for each file. The
 * relationship between a topic and a domain is a many-to-many. Therefore, this
 * class maintains maps that allow lookups from either direction.
 * 
 * @author mac002
 *
 */
public class LibraryProxyManager {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(LibraryProxyManager.class);

	private String path;
	private List<String> missingFiles = new ArrayList<String>();
	private TreeMap<String, File> aresFileList;
	private TreeMap<String, File> greekFileList;
	private Map<String,File> mapDomainPaths = new TreeMap<String,File>();
	private TreeMap<String, LibraryFileProxy> mapLoadedAresFiles = new TreeMap<String, LibraryFileProxy>();
	private List<String> searchKeys = new ArrayList<String>();
	private TreeSet<String> pointers = new TreeSet<String>();

	private PointerIndex pointerIndex = null; // loaded lazily, i.e. in
												// getPointers()

	// maps domains (e.g. en_US_dedes) to the topics of filenames that use that
	// domain
	private TreeMap<String, TreeSet<String>> mapDomainToTopics = new TreeMap<String, TreeSet<String>>();

	// maps topics (e.g. actor) to the domains (e.g. en_US_dedes) of filenames
	// that use that domain
	private TreeMap<String, TreeSet<String>> mapTopicToDomains = new TreeMap<String, TreeSet<String>>();

	public LibraryProxyManager(String path) {
		this.path = path;
		aresFileList = FileUtils.getMapOfFilesInDirectory(
				path
				, "ares"
				, true
				);
		indexFiles(aresFileList.values());
	}
	
	/**
	 * The path used for loading the ares files
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Create a new translation library for the specified domain, using the
	 * gr_GR_cog library as the pattern. When finished, a new directory will
	 * exist in the library, along with all the required subfolders and files.
	 * The values of strings will be empty. The values that are pointers (i.e.
	 * keys) will use the new domain.
	 * 
	 * @param domain
	 *            - the new domain, e.g shw_KE_oak
	 */
	public void createNewLibrary(String domain) {
		try {
			// get the list of Greek files to use as the model to clone from
			if (greekFileList == null) {
				greekFileList = FileUtils.filter(aresFileList, "gr_GR_cog");
			}
			Set<Entry<String, File>> entrySet = greekFileList.entrySet();
			File grkFile;
			File newFile;
			String contents;
			String resource;
			for (Entry entry : entrySet) {
				// clone the Greek file to the new domain
				grkFile = (File) entry.getValue();
				newFile = new File(grkFile.getPath().replaceAll("gr_GR_cog",
						domain));
				newFile.getParentFile().mkdirs();
				newFile.createNewFile();
				ApacheFileUtils.copyFile(grkFile, newFile);
				// convert the contents of the copied file to the new domain
				contents = ApacheFileUtils.readFileToString(newFile, "UTF-8");
				contents = contents.replaceAll("gr_GR_cog", domain);
				contents = contents.replaceAll("\".+\"", "\"\"");
				ApacheFileUtils.writeStringToFile(newFile, contents, "UTF-8");
				// register the new file with the TMS
				LibraryFileProxy proxy = new LibraryFileProxy(newFile);
				resource = GatewayUtils
						.getAresFileNameParts(newFile.getName()).getTopic()
						+ "_" + domain;
				mapLoadedAresFiles.put(resource, proxy);
				registerNewResource(resource);
			}
			for (String theValue : pointers) {
				System.out.println(theValue);
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}

	/**
	 * Load the ares files for each combination of topic and domain
	 * 
	 * @param topics the topics
	 * @param domain the domain
	 */
	public void loadLibraryFiles(List<String> topics, String domain) {
		Iterator<String> it = topics.iterator();
		while (it.hasNext()) {
			String topic = it.next();
			String key = topic + "_" + domain;
			File f = aresFileList.get(key);
			if (f == null) {
				if (!missingFiles.contains(key)) {
					missingFiles.add(key);
				}
			} else {
				if (key != null) {
					if (!mapLoadedAresFiles.containsKey(key)) {
						LibraryFileProxy proxy = new LibraryFileProxy(f);
						mapLoadedAresFiles.put(key, proxy);
					}
				}
			}
		}
	}

	/**
	 * NEW METHODS TO BE ADDED BACK INTO TMS SOURCE CODE
	 */
	
	/**
	 * RestManager a map to all the files that have been loaded
	 * @return map
	 */
	public TreeMap<String, LibraryFileProxy> getLoadedFiles() {
		return mapLoadedAresFiles;
	}
	
	/**
	 * RestManager the number of lines that are key-value pairs
	 * @return the count
	 */
	public int getKeyValuePairCount() {
		int result = 0;
		for (LibraryFileProxy fProxy : mapLoadedAresFiles.values()) {
			for (LibraryLine line : fProxy.getValues()) {
				if (line.isKeyValuePair()) {
					result++;
				}
			}
		}
		return result;
	}
	
	public List<String> getDomainsAsList() {
		List<String> result = new ArrayList<String>();
		for (String domain : this.getDomainsAsSet()) {
			result.add(domain);
		}
		return result;
	}
	public void loadAllLibraryFiles() {
		loadAllLibraryFiles(getDomainsAsList());
	}
	/**
	 * Loads files for all domains in the supplied List.
	 * @param domains - domains whose files to load.  If null, loads all
	 */
	public void loadAllLibraryFiles(List<String> domains) {
		Iterator<String> it = aresFileList.keySet().iterator();
		FileNameParts parts;
		while (it.hasNext()) {
			String key = it.next();
			File f = aresFileList.get(key);
			if (f == null) {
				if (!missingFiles.contains(key)) {
					missingFiles.add(key);
				}
			} else {
				if (key != null) {
					parts = GatewayUtils.getAresFileNameParts(f.getName());
					if (parts != null) {
						if (domains == null || domains.contains(parts.getDomain().toLowerCase()) || domains.contains(parts.getDomain())) {
							if (!mapLoadedAresFiles.containsKey(key)) {
								LibraryFileProxy proxy = new LibraryFileProxy(f);
								mapLoadedAresFiles.put(key, proxy);
								Iterator keyIt = proxy.getKeys();
								while (keyIt.hasNext()) {
									String internalKey = (String) keyIt.next();
									if (! internalKey.matches("A_Resource_Whose_Name")) {
										String searchKey = proxy.getDomain() + "|" + proxy.getTopic() + "|" + internalKey;
										searchKeys.add(searchKey);
									}
								}
								if (! mapDomainPaths.containsKey(parts.getDomain())) {
									String domainPath = FileUtils.getParentPath(parts.getDomain(), f.getParent());
									mapDomainPaths.put(parts.getDomain(), new File(domainPath));
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Search key format is domain|topic|key, e.g. gr_GR_cog|Actors|priest
	 * @return the search keys
	 */
	public List<String> getSearchKeys() {
		return searchKeys;
	}
	public void setComment(String resource, String key, String comment) {
		try {
			mapLoadedAresFiles.get(resource).getLibraryLine(key).setComment(comment);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e, "Resource: " + resource + " Key: " + key + " Comment: " + comment);
		}
	}
	
	public String getComment(String resource, String key) {
		return mapLoadedAresFiles.get(resource).getLibraryLine(key).getComment();
	}
	
	/**
	 * For each resource in the list, write to its file
	 * and serializes the values to history.
	 * @param resources the resources
	 */
	public void writeFiles(List<String> resources) {
		LOGGER.info("Saving changes to files...");
		Iterator<String> it  = resources.iterator();
		while (it.hasNext()) {
			writeFile(it.next());
		}
	}
		
	/**
	 * Write the file for the specified resource
	 * @param resource the resource
	 */
	public void writeFile(String resource) {
		try {
			mapLoadedAresFiles.get(resource).writeAresFile();
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e, "Error processing " + resource);
		}
	}

	/**
	 * END NEW METHODS TO BE ADDED BACK INTO TMS SOURCE CODE
	 * 
	 */
	/**
	 * Gets a list of pointers used by the specified resource, e.g. the pointers
	 * used in me.m06.d22_gr_GR_cog
	 * 
	 * @param resource the resource of interest
	 * @return - a list of resources it points to
	 */
	private List<String> getPointers(String resource) {
		List<String> result = null;
		try {
			if (pointerIndex == null) {
				// loaded on when needed, i.e. when the user creates a new
				// domain for translations
				pointerIndex = new PointerIndex(aresFileList, "gr_GR_cog");
			}
			pointerIndex.getPointers(resource);
		} catch (Exception e) {
			result = null;
			ErrorUtils.report(LOGGER, e);
		}

		return result;
	}

	
	public LibraryLine getLine(String resource, String key) {
		try {
			return mapLoadedAresFiles.get(resource).getLibraryLine(key);
		} catch (Exception e) {
			return null;
		}
	}
	
	public void removeLine(String resource, String key) {
		try {
			mapLoadedAresFiles.get(resource).removeLine(key);;
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}

	/**
	 * Populates the maps for the topics and domains found in the ares files
	 * 
	 * @param files the files
	 */
	private void indexFiles(Collection<File> files) {
		Iterator<File> it = files.iterator();
		String filename = "";
		FileNameParts fileNameParts = null;

		while (it.hasNext()) {
			File f = it.next();
			filename = f.getName().replace(".ares", "");
			if (filename.split("_").length > 2) { // filter out ares files that
													// do not have a domain
				fileNameParts = GatewayUtils.getAresFileNameParts(filename);
				addTopicToDomainMap(fileNameParts.getDomain(),
						fileNameParts.getTopic());
				addDomainToTopicMap(fileNameParts.getTopic(),
						fileNameParts.getDomain());
			}
		}
	}

	public List<String> getMissingFileNames() {
		return missingFiles;
	}

	/**
	 * Add the supplied topic to the Domain map if it has not yet been added.
	 * 
	 * @param domain the domain
	 * @param topic the topic
	 */
	private void addTopicToDomainMap(String domain, String topic) {
		TreeSet<String> set = null;
		if (mapDomainToTopics.containsKey(domain)) {
			set = mapDomainToTopics.get(domain);
		} else {
			set = new TreeSet<String>();
		}
		if (!set.contains(topic)) {
			set.add(topic);
			mapDomainToTopics.put(domain, set);
		}
	}

	/**
	 * Add the supplied domain to the topic map if it has not yet been added.
	 * 
	 * @param topic the topic
	 * @param domain the domain
	 */
	private void addDomainToTopicMap(String topic, String domain) {
		TreeSet<String> set = null;
		if (mapTopicToDomains.containsKey(topic)) {
			set = mapTopicToDomains.get(topic);
		} else {
			set = new TreeSet<String>();
		}
		if (!set.contains(domain)) {
			set.add(domain);
			mapTopicToDomains.put(topic, set);
		}
	}

	/**
	 * Return the domains as a string array
	 * 
	 * @return the domains as a string array
	 */
	public String[] getDomainsAsArray() {
		Set<String> set = mapDomainToTopics.keySet();
		return GeneralUtils.setToStringArray(set);
	}
	
	public Set<String> getDomainsAsSet() {
		return mapDomainToTopics.keySet();
	}

	public Set<String> getTopicsAsSet() {
		return mapTopicToDomains.keySet();
	}

	public void printStatistics() {
		System.out.println("Number of files: " + this.getLoadedAresFileCount());
		System.out.println("Number of domains: " + mapDomainToTopics.size());
		System.out.println("Number of topics: " + mapTopicToDomains.size());
		System.out.println("Number of keys: " + this.getKeyValuePairCount());
		
	}

	/**
	 * Report the number of proxies created for the Ares files
	 * 
	 * @return number of proxies (= number of ares files)
	 */
	public int getLoadedAresFileCount() {
		return mapLoadedAresFiles.size();
	}

	/**
	 * Report the number of ares files that have been indexed
	 * 
	 * @return the indexed file count
	 */
	public int getIndexedAresFilesCount() {
		return this.aresFileList.size();
	}

	/**
	 * For the given resource, rev the value corresponding to the key
	 * 
	 * @param resource is topic_domain
	 * @param key the key
	 * @return the value
	 */
	public String getValue(String resource, String key) {
		LibraryFileProxy proxy;
		try {
			proxy = mapLoadedAresFiles.get(resource);
			return proxy.getValue(key);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
			return "";
		}
	}

	
	/**
	 * For the given resource, retrieve by the key and updates its value.
	 * 
	 * @param resource the resource
	 * @param key the key
	 * @param value the value
	 */
	public void setValue(String resource, String key, String value) {
		try {
			if (!mapLoadedAresFiles.containsKey(resource)) {
				String toDomain = GeneralUtils.domainFromResource(resource);
				String fromDomain = "gr_GR_cog";
				String greekResource = resource.replace(toDomain, fromDomain);
				cloneFile(greekResource, fromDomain, toDomain);
				PointerIndex index = new PointerIndex(aresFileList, fromDomain);
				List<String> pointers = index
						.getPointersRecursively(greekResource);
				if (pointers != null) {
					for (String pointer : pointers) {
						System.out.println(pointer);
						cloneFile(pointer, fromDomain, toDomain);
					}
				}
			}
			mapLoadedAresFiles.get(resource).setValue(key, value);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}

	/**
	 * Handles the situation where the user has created a new domain and there
	 * is no ares file for that topic / domain combination yet
	 * 
	 * @param resource the resource
	 * @param key the key
	 * @param value the value
	 * @param comment the comment
	 */
	public void createNewFile(String resource, String key, String value, String comment) {
		String modelResource;
		String modelPath;
		String newPath;
		String modelDomain = "gr_GR_cog";
		if (resource.startsWith("labels")) {
			modelDomain = "en_US_tms";
		}
		modelResource = GeneralUtils.changeResourceDomain(resource,
				modelDomain);
		
		// set newPath based on various conditionValues
		if (mapLoadedAresFiles.containsKey(modelResource)) {
			// if there is a corresponding resource in gr_GR_cog, create newPath based on its folder structure
			modelPath = mapLoadedAresFiles.get(modelResource).getResourcePath();
			newPath = GatewayUtils.replaceDomain(modelPath, modelDomain,
					GeneralUtils.domainFromResource(resource));
		} else {
			// We have no corresponding resource in gr_GR_cog to this one.
			// So, we will set the path to the root folder for this domain
			String domain = GatewayUtils.getDomain(resource);
			if (domain == null || domain.length() < 1) {
				// There is no project for this domain.  We cannot add the doc to ares.
				// Let the user know what is happening.
				LOGGER.info("There is no project folder for " + domain);
				LOGGER.info("The following doc cannot be added:");
				LOGGER.info(resource + "|" + key + " = " + value);
				LOGGER.info("You must create an ares project for this domain.");
				LOGGER.info("Then try pulling again.");
				newPath = null;
			} else {
				// There is a project for this domain, but no folder structure
				// is available, so we will create a special folder to put it in.
				newPath = this.mapDomainPaths.get(domain).getPath();
				// add a special folder to the path
				String specialFolder = "/OtherFromDatabase/";
				newPath = newPath + specialFolder + resource + ".ares";
				// tell the user what we are doing
				LOGGER.info(resource+".ares does in exist.");
				LOGGER.info("The new file is in folder ");
				LOGGER.info(specialFolder);
				LOGGER.info("in library for " + domain);
			}
		}
		if (newPath != null) {
			// Create the new ares file
			StringBuffer content = new StringBuffer();
			content.append("A_Resource_Whose_Name = " + resource);
			content.append(
					"\n" 
							+ key 
							+ " = " 
							+ LibraryUtils.wrapQuotes(value)
							+ (comment != null && comment.length() > 0 ? " // " + comment: "")
					);
			FileUtils.writeFile(newPath, content.toString());
			// Load the new file into the proxy manager
			LibraryFileProxy proxy = new LibraryFileProxy(new File(newPath));
			mapLoadedAresFiles.put(resource, proxy);
			registerNewResource(resource);
		}
	}

	private void cloneFile(String fromResource, String fromDomain,
			String toDomain) {
		File grkFile, newFile;
		String contents, resource;
		try {
			// clone the Greek file to the new domain
			grkFile = aresFileList.get(fromResource);
			newFile = new File(grkFile.getPath().replaceAll(fromDomain,
					toDomain));
			newFile.getParentFile().mkdirs();
			newFile.createNewFile();
			ApacheFileUtils.copyFile(grkFile, newFile);
			// convert the contents of the copied file to the new domain
			contents = ApacheFileUtils.readFileToString(newFile, "UTF-8");
			contents = contents.replaceAll(fromDomain, toDomain);
			contents = contents.replaceAll("\".+\"", "\"\"");
			ApacheFileUtils.writeStringToFile(newFile, contents, "UTF-8");
			// register the new file with the TMS
			LibraryFileProxy proxy = new LibraryFileProxy(newFile);
			resource = GatewayUtils.getAresFileNameParts(newFile.getName())
					.getTopic() + "_" + toDomain;
			mapLoadedAresFiles.put(resource, proxy);
			registerNewResource(resource);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}

	/**
	 * Update the indexes between domains and topics for the newly added
	 * resource
	 * 
	 * @param resource  to use for the indexing
	 */
	private void registerNewResource(String resource) {
		Resource theResource = new Resource(resource);
		addTopicToDomainMap(theResource.getDomain().getDomain(),
				theResource.getTopic());
		addDomainToTopicMap(theResource.getTopic(), theResource.getDomain()
				.getDomain());
	}

	/**
	 * Report the number of domains found among the ares files
	 * 
	 * @return number of domains
	 */
	public int getDomainCount() {
		return mapDomainToTopics.size();
	}

	/**
	 * Report the number of topics found among the ares files
	 * 
	 * @return number of topics
	 */
	public int getTopicCount() {
		return mapTopicToDomains.size();
	}
	
	/**
	 * 
	 * @param resource the resource
	 * @return true if specified resource exists
	 */
	public boolean resourceExists(String resource) {
		return mapLoadedAresFiles.containsKey(resource);
	}
	
	
}
