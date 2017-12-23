package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.ocmc.ioc.liturgical.utils.ApacheFileUtils;
import org.ocmc.ioc.liturgical.utils.FileUtils;
import org.ocmc.ioc.liturgical.utils.GeneralUtils;

/**
 * ares files are key-value pairs.  There are two kinds of values:
 * a string (enclosed in quotes) or a pointer to another file's key, 
 * aka a pointer.
 * 
 * The purpose of this class is to create an index of all the
 * pointers in the library of the specified domain.
 * There are two indexes.  For each file, there is an index
 * of all the files it points to.  For each file, there is also an index
 * of what files point to it.
 * 
 * The index is used so that when we create a new ares file for a given
 * domain, we can also create all required files that it points to.
 * 
 * Normally, we only instantiate this for gr_GR_cog, but it has been
 * written generically in case it is useful for other domains.
 * 
 * @author mac002
 *
 */
public class PointerIndex {
	private String domain;
	private TreeMap<String,List<String>> pointsTo = new TreeMap<String,List<String>>();
	private TreeMap<String,File> fileList;
	private static List<String> pointers;
	
	public PointerIndex(TreeMap<String,File> fullFileList, String domain) {
		this.domain = domain;
		fileList = FileUtils.filter(fullFileList, "gr_GR_cog");
		buildIndexes();
	}
	
	private void buildIndexes() {
		Set<Entry<String,File>>entries = fileList.entrySet();
		for (Entry entry : entries) {
			pointsTo.put((String) entry.getKey(), getPointers((File) entry.getValue(), domain)); 
		}
	}
	
	/**
	 * RestManager the resources pointed to by this resource
	 * @param resource - the resource of interest, e.g. actors_gr_GR_cog
	 * @return a list of resources that are used as values in the keys of the requested resource
	 */
	public List<String> getPointers(String resource) {
		List<String> result = null;
		try {
			result = pointsTo.get(resource);
		} catch (Exception e) {
			result = null;
		}
		return result;
	}
	
	public List<String> getPointersRecursively(String resource) {
		List<String> result = null;
		try {
				pointers = new ArrayList<String>();
				loadPointersRecursively(resource);
				result = pointers;
		} catch (Exception e) {
			result = null;
		}
		return result;
	}	
	
	private void loadPointersRecursively(String resource) {
		List<String> result = getPointers(resource);
		if (result != null) {
			for (String pointer : result) {
				if (! pointers.contains(pointer)) {
					pointers.add(pointer);
					loadPointersRecursively(pointer);
				}
			}
		}
		
	}
	
	public void reportPointers() {
		Set<Entry<String,List<String>>>entries = pointsTo.entrySet();
		for (Entry entry : entries) {
			System.out.println(entry.getKey() + ":");
			List<String> pointers = (List<String>) entry.getValue();
			for (String pointer : pointers) {
				System.out.println("\t"+pointer);
			}
			System.out.println("Number of Files: " + entries.size());
		}
	}
	
	/**
	 * Reads the specified ares file to locate all pointers (values that are a key instead of a string)
	 * For example:
	 * 	person = actors_gr_GR_cog.priests
	 * Would have actors_gr_GR_cog added as a pointer.
	 * @param file - the ares resource file to search
	 * @param domain - the domain to search against
	 * @return the list of pointers
	 */
	private List<String> getPointers(File file, String domain) {
		List<String> result = new ArrayList<String>();
		String [] parts;
		try {
			List<String> lines = ApacheFileUtils.readLines(file, "UTF-8");
			String value = null;
			for (String line : lines) {
				if (! line.contains("A_Resource_Whose_Name")) {
					value = GeneralUtils.valueAsKeyFromPair(line,domain);
					if (value != null) {
						if (! result.contains(value)) {
							result.add(value); 
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}	
	
	public static void main(String[] args) {
		String path = "/Users/mac002/tms/libraries";
		TreeMap<String,File> aresFileList = FileUtils.getMapOfFilesInDirectory(path, "ares",true);
		PointerIndex index = new PointerIndex(aresFileList,"gr_GR_cog");
		index.reportPointers();
	}

}
