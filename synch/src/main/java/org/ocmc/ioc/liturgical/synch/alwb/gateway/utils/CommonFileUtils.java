package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.ocmc.ioc.liturgical.utils.ApacheFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFileUtils {
	   private static final Logger LOGGER = LoggerFactory.getLogger(CommonFileUtils.class);
	
	   
	   public final static String QUOTE = "\"";

		/**
		 * For the given resource, replace the domain portion with a new one
		 * @param resource - the resource name to be converted
		 * @param newDomain - the new domain to use
		 * @return - the converted resource
		 */
		public static String changeResourceDomain(String resource, String newDomain) {
			String result = "";
			try {
				String[] parts = resource.split("_");
				if (parts.length > 3) {
					result = parts[0] + "_" + newDomain;
				}
			} catch (Exception e) {}
			return result;
		}

		/**
		 * Copy the directory and its files into the target directory
		 * @param from - directory to copy
		 * @param to - directory to copy into
		 * @param startWithEmptyTarget - if true, deletes files already existing in the 'to' directory
		 */
		public static void copyDir(String from, String to, boolean startWithEmptyTarget) {
			try {
				File fromDir = new File(from);
				File toDir = new File(to);
				if (startWithEmptyTarget) {
					ApacheFileUtils.deleteDirectory(toDir);
				}
				ApacheFileUtils.copyDirectory(fromDir,toDir);
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			}
		}
	   
		/**
		 * For the given resource name (without a .ares extension),
		 * extract the domain portion of the resource.
		 * In other words, strip off the topic part.
		 * @param resource the resource
		 * @return the domain portion of the resource name
		 */
		public static String domainFromResource(String resource) {
			String result = "";
			try {
				String[] parts = resource.split("_");
				if (parts.length > 3) {
					result = parts[1] + "_" 
							+ parts[2] + "_"
							+ parts[3];
				} else {
					result = null;
				}
			} catch (Exception e) {}
			return result;
		}
		

	   public static String filenameFromHref(String href) {
		String result = "";
		try {
			String [] parts = href.split("m/s")[1].split("/");
			result = parts[parts.length-1];
		} catch (Exception e) {
			e.printStackTrace();
			result = href;
		}
		return result;
	}

	/**
	 * Takes a map as input and returns a new map that only contains
	 * keys that had a substring that matches the target.
	 * @param map - the map to search
	 * @param target - the substring to apply against strings
	 * @return a map only containing entries whose key contains the target.
	 */
	public static TreeMap<String,File> filter(TreeMap<String,File> map, String target) {
		TreeMap<String,File> result = new TreeMap<String,File>();
		Iterator it = map.keySet().iterator();
		String key;
		try {
			while (it.hasNext()) {
				key = (String) it.next();
				if (key.contains(target)) {
					result.put(key, map.get(key));
				}
			}
		} catch (Exception e) {
			result = null;
			e.printStackTrace();
		}
		return result;
	}
	
	
	public static String[] getAresFileParts(
			String file
			, String domainDelimiter
			, boolean countryToLowerCase
			, boolean dropFileExtension
			) {
		String [] theParts;
		String [] result;
		try {
			theParts = file.split("_");
			result = new String[2];
			if (theParts.length ==4) {
				result[0] = theParts[0];
				result[1] = (theParts[1] + domainDelimiter + theParts[2] + domainDelimiter);
				theParts[3] = theParts[3].replace(".tsf", "");
				if (countryToLowerCase) {
					theParts[3] = theParts[3].toLowerCase();
				}
				if (dropFileExtension) {
					theParts[3] = theParts[3].split("\\.")[0];
				}
				result[1] = result[1]  + theParts[3];
			} else {
				result = null;
			}
		} catch (Exception e) {
			result = null;
		}
		return result;
	}
	
	/**
	 * Recursively read contents of directory and return all files found
	 * @param directory the directory
	 * @param extension period + file extension, e.g. .html. If null, returns all 
	 * @return List containing all files found
	 */
	public static List<File> getFilesInDirectory(String directory, final String extension) {
		File dir = new File(directory);
		String [] extensions = {extension};
		List<File> files = null;
		try {
			files = (List<File>) ApacheFileUtils.listFiles(dir, extensions, true);
		} catch (Exception e) {
			files = null;
		}
		return files;
	}
	
	/**
		 * Create a map of files in the directory, where the 
		 * key is the name of the file and the value is the File.
		 * @param directory - where to start the loading
		 * @param domains - the domains to process
		 * @param extension - what file extension to look for
		 * @param dropExtension - true if you want the key to not use the file extension
		 * @return the map
		 */
		public static TreeMap<String,File> getMapOfFilesInDirectory (
					String directory
					, List<String> domains 
					, final String extension 
					, boolean dropExtension
					) {
			TreeMap<String,File> result = new TreeMap<String,File>();
			List<File> files = getFilesInDirectory(directory,extension);
			Iterator<File> it = files.iterator();
			while (it.hasNext()) {
				File f = it.next();
				String name = f.getName();
				if (name.startsWith("._")) { // to handle a bug in Apache FileUtils that only occurs in Windows 8
					name = name.substring(2);
				}
				if (dropExtension) {
					name = name.replace("." + extension, "");
				}
				try {
					if (domains == null || domains.isEmpty()) { // we are not filtering by domain, so just put it in map
						result.put(name, f);
					} else  { // try to filter by domain
						String[] parts = getAresFileParts(f.getName(),"_",false,true);
						if (parts != null && parts.length == 2) { // has a domain
							if (domains.contains(parts[1])) { // is the domain in our list?
								result.put(name, f);
							}
						}
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage());
				}
			}
			return result;
		}
	

	public static List<File> getMatchingFilesInDirectory(String directory, String fileRegularExpression, String extension) {
		List<File> list = getFilesInDirectory(directory, extension);
		List<File> files = new ArrayList<File>();
		Iterator<File> it = list.iterator();
		File f;
		while(it.hasNext()) {
			f = it.next();
			if (f.getName().matches(fileRegularExpression)) {
				files.add(f);
			}
		}
		return files;
	}
	
	public static String[] getPathsToFilesInDirectory(String directory, String extension, String excludeSubPath) {
		List<File> list = getFilesInDirectory(directory, extension);
		List<String> paths = new ArrayList<String>();
		Iterator<File> it = list.iterator();
		while(it.hasNext()) {
			File f = it.next();
			String path = (String)f.getPath();
			if (! path.contains(excludeSubPath)) {
				paths.add(path);
			}
		}
		return paths.toArray(new String[paths.size()]);	
	}
	
	/**
	 * Gets the width of the user's screen and multiplies it by the multipler.
	 * For example, to get a value that is 80% the width of the window,
	 * pass in .8.
	 * @param multiplier the multiplier
	 * @return the screen width
	 */
	public static int getScreenWidth(double multiplier) {
		return (int) (java.awt.Toolkit.getDefaultToolkit().getScreenSize().width*multiplier);
	}
	/**
	 * Finds all index.html files that are for services
	 * @param directory the directory
	 * @param extension the extension
	 * @return list of index.html files
	 */
	public static List<File> getServicesHtmlFilesInDirectory(String directory, final String extension) {
		List<File> list = getFilesInDirectory(directory, extension);
		List<File> files = new ArrayList<File>();
		Iterator<File> it = list.iterator();
		String path = "";
		File f;
		while(it.hasNext()) {
			f = it.next();
			path = FilenameUtils.separatorsToUnix((String) f.getPath());
			if (path.contains("/h/s") && (path.endsWith("index.html"))) {
				files.add(f);
			}
		}
		return files;
	}
	
	
	/**
	 * RestManager the part of the path that starts after the delimiter
	 * @param delimiter - string at end of which to split
	 * @param path - the path to be split
	 * @return the subpath
	 */
	public static String getSubPath(String delimiter, String path) {
		String result = "";
		try {
			result = path.split(delimiter)[1];
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return result;
	}
	
	/**
	 * Read all the lines in the given file and return as a list
	 * @param f - the file to read
	 * @return the lines as a List
	 */
	public static List<String> linesFromFile(File f) {
		Path path = f.toPath();
		List<String> list = null;
		try {
			list = Files.readAllLines(path, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * Convert a List of Strings to an Array of Strings
	 * @param list the list
	 * @return the string array
	 */
	public static String[] listToStringArray(List<String> list) {
		return list.toArray(new String[list.size()]);
	}
	
	/**
	 * The static main method is provided for test purposes and as an example of how to use this class.
	 * <p>Normal usage is to instantiate the class elsewhere 
	 * set the parameters, and call the 
	 * xmlToHtml method or the xmlToPdf method.
	 * 
	 * @param args -- do not use.
	 */
	public static void main(String[] args) {
		String source = "filesIn";
		List<File> files = getFilesInDirectory(source,"pdf");
		System.out.println(files.size() + " files found!");
	}

	/**
	 * 
	 * @param filename the filename
	 * @param delimiter the delimiter
	 * @param printDupes yes or no
	 * @return a map where the key is the first delimited field and the value is the entire line
	 */
	public static TreeMap<String,String> mapFromDelimitedFile(
			String filename
			, String delimiter
			, boolean printDupes
			) {
		Comparator<String> lowerCaseComparator = new Comparator<String>() {
	        @Override public int compare(String o1, String o2) {
	        	String n1 = 	org.apache.commons.lang3.StringUtils.stripAccents(o1.toLowerCase());
	        	String n2 = 	org.apache.commons.lang3.StringUtils.stripAccents(o2.toLowerCase());
	            return n1.compareTo(n2);
	        };         
	    };
		TreeMap<String,String> map = new TreeMap<String,String>(lowerCaseComparator);
		try {
			String line;
			String[] parts;
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				try {
					parts = line.split(delimiter);
					if (parts.length > 0)  {
						if (printDupes && map.containsKey(parts[0])) {
							System.out.println("FileUtils.mapFromDelimitedFile - duplicate ");
							System.out.println(map.get(parts[0]));
							System.out.println(line + "\n");
						}
						map.put(parts[0], line);
					}
				} catch (Exception innerE) {
					innerE.printStackTrace();
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return map;
	}
	
	/**
	 * Convert a list of lists to a two dimensional string array
	 * @param multiList - the list of lists where the 2nd dimension lists are the same size
	 * @return two dimensional string array
	 */
	public static String[][] multiListToMultiArray(List<List<String>> multiList) {
		String[][] array = new String[multiList.size()][];
		String[] blankArray = new String[0];
		for(int i=0; i < multiList.size(); i++) {
		    array[i] = multiList.get(i).toArray(blankArray);
		}
		return array;
	}	
	
	/**
	 * Normalizes a HREF so it points to the local media Eclipse project
	 * @param hrefBaseUrl - part of URL to strip from href.  Needs to end with forward slash
	 * @param localMediaPath - path to prefix to the stripped href.  Needs to end with forward slash
	 * @param href - href to normalize
	 * @return normalized href
	 */
	public static String normalizeMediaPath(
			String hrefBaseUrl
			, String localMediaPath
			, String href
			) {
		String result = "";
		try {
			result = localMediaPath + href.split(hrefBaseUrl)[1];
		} catch (Exception e) {
			result = localMediaPath + href;
		}
		return result;
	}
	public static String pathFromHref(String href) {
		String result = "";
		try {
			String [] parts = href.split("m/s")[1].split("/");
			result = StringUtils.join(parts,"/",0,parts.length-1);
		} catch (Exception e) {
			e.printStackTrace();
			result = href;
		}
		return result;
	}
	/**
	 * Replaces forward slashes in path with periods and returns a file 
	 * name based on the path information
	 * @param path the path
	 * @param fileName the filename
	 * @return new filename
	 */
	public static String pathToName(String path, String fileName) {
		return path.split("m/s/")[1].replace("/", ".") + fileName;
	}

	public static void renameFiles(
			String fromDir, 
			String toDir, 
			boolean startWithEmptyTarget, 
			String fromFileNamePattern,
			String toFileNamePattern) {
		try {
			copyDir(fromDir,toDir,startWithEmptyTarget);
			List<File> theFiles = getFilesInDirectory(toDir,"mp3");
			Iterator<File> it = theFiles.iterator();
			while (it.hasNext()) {
				String filename = it.next().getAbsolutePath();
				String newFilename = filename.replaceFirst(fromFileNamePattern, toFileNamePattern);
				LOGGER.info("From: " + filename);
				LOGGER.info("To: " + newFilename);
				ApacheFileUtils.moveFile(
						ApacheFileUtils.getFile(filename), 
						ApacheFileUtils.getFile(newFilename));
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
	}

		
	
	/**
	 * Convert a Set of Strings to an Array of Strings
	 * @param set the strings
	 * @return the string array
	 */
	public static String[] setToStringArray(Set<String> set) {
		return set.toArray(new String[set.size()]);
	}
	
	/**
	    * Converts a string of delimited values into a List   
	    * @param values the values
	    * @param delimiter the delimiter
	    * @return values as a list
	    */
		public static List<String> toList(String values, String delimiter) {
			List<String> result = new ArrayList<String>();
			try {
				String[] parts = values.split(delimiter);
				for (int i=0; i < parts.length; i++) {
					result.add(parts[i].trim());
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
				LOGGER.error("Error splitting " + values);
				LOGGER.error("Attempted to split using delimiter " + delimiter);
			}
			return result;
		}

	/**
	 * Checks an ares key-value line to see if the value is a key (i.e. a pointer)
	 * If so, it will return the value as key. 
	 * @param line the line
	 * @param domain the domain
	 * @return value as key if found, otherwise returns null
	 * Example:  priest = actors_gr_GR_cog.priest, will return actors_gr_GR_cog
	 */
	public static String valueAsKeyFromPair(String line, String domain) {
		String result = valueFromPair(line);
		if (result != null) {
			result = result.trim();
			if (result.startsWith("\"")) {
				result = null;
			} else {
				if (result.length() > 3) {
					String [] parts = result.split("_"+domain); // throws domain away.  We are left with left key and right key
					if (parts.length > 1) {
						result = parts[0] + "_" + domain;
					} else {
						result = null;
					}
				} else {
					result = null;
				}
			}
		}
		return result;
	}
	/**
	 * Extracts the value from a key-value pair from
	 * an ares file
	 * @param line the line
	 * @return the value
	 */
	public static String valueFromPair(String line) {
		String result = null;
		try {
			if (! line.trim().startsWith("/")) {
				String [] parts = line.split("=");
				if (parts.length == 2) {
					result = parts[1].trim();
					parts = result.split("//");
					if (parts.length == 2) {
						result = parts[0];
					}
 				}
			}
		} catch (Exception e) {
			result = null;
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Create a new file and write the contents to it
	 * @param filename the name of the file, including the path
	 * @param map whose contents to write
	 */
			
	public static void writeFile(String filename, Map<String,String> map) {
		File file = new File(filename);
		BufferedWriter bw = null;
		Iterator<String> it = map.keySet().iterator();
		 
		try {
			file = new File(filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			while (it.hasNext()) {
				bw.write(map.get(it.next()));
				bw.write("\n");
			}
		 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * Create a new file and write the contents to it
	 * @param filename the name of the file, including the path
	 * @param content the contents to write
	 */
			
	public static void writeFile(String filename, String content) {
		File file = new File(filename);
		BufferedWriter bw = null;
		 
		try {
			file = new File(filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			bw.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
