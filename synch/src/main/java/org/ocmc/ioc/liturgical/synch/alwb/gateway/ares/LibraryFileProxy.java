package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.utils.AresEntryId;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.utils.Constants;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.utils.GatewayUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

/**
 * Acts as a proxy to the ares file.
 * @author mac002
 *
 */
public class LibraryFileProxy  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(LibraryFileProxy.class);
	private File file;
	// This map uses the key from a line.  If the line does not have a key,
	// the line number will be used as the key.  For example, a blank line
	// does not have a key.  So, the line number is used as the key.
	private HashMap<String,LibraryLine> linesByKey = new HashMap<String,LibraryLine>();
	// This map uses the Line number.  We only need it when writing out the file.
	// This allows us to preserve the order of the lines.
	public TreeMap<String,String> linesByLineNbr = new TreeMap<String,String>();
	private FileNameParts fileNameParts;
	int lineCount = 0;
		
	public LibraryFileProxy(File file) {
		this.file = file;
		fileNameParts = GatewayUtils.getAresFileNameParts(file.getName());
		loadFromAresFile(file);
	}
	
	public String getDomain() {
		return fileNameParts.getDomain();
	}
	
	public String getTopic() {
		return fileNameParts.getTopic();
	}
		
	/**
	 * Read the ares file and load the maps
	 * @param file - file handle to the ares file
	 */
	private void loadFromAresFile(File file) {
		try {
			BufferedReader br;
			Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
			br = new BufferedReader(r);
			FileNameParts parts = GatewayUtils.getAresFileNameParts(file.getName());
			String line = "";
			String lineCount = "";
			while ((line = br.readLine()) != null) {
				lineCount = formattedLineCount();
				LibraryLine al = new LibraryLine(parts.getDomain(), parts.getTopic(), lineCount, line);
				linesByKey.put(al.getLineIdentifier(), al);
				linesByLineNbr.put(lineCount, al.getLineIdentifier());
			}
			br.close();
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	/**
	 * Increments the line count for this proxy, and returns
	 * a formatted string representation padded with leading zeros.
	 * @return, e.g. L00010
	 */
	public String formattedLineCount() {
		lineCount++;
		return "L" + String.format("%05d", lineCount);
	}
	
	public void writeAresFile() {
		String nextKey = "";
		LibraryLine al;
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF8"));
			Iterator<String> it = linesByLineNbr.keySet().iterator();
			while (it.hasNext()) {
				nextKey = linesByLineNbr.get(it.next());
				al = linesByKey.get(nextKey);
				try {
					writer.append(al.getLine()+"\n");
				} catch (Exception e) {
					System.out.println("");
				}
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	/**
	 * NEW METHODS TO BE ADDED BACK TO SOURCE FOR TMS
	 */
	
	/**
	 * Look in this ares file and get the value for the requested key
	 * @param key the key
	 * @return value
	 */
	public LibraryLine getLibraryLine(String key) {
		LibraryLine result;
		try {
			result = linesByKey.get(key);
		} catch (Exception e) {
			result = null;
		}
		return result;
	}
	
	/**
	 * Set the LibraryLine for the given key.
	 * @param key the key
	 * @param value the value
	 */
	public void setLibraryLine(String key, LibraryLine value) {
		try {
			if (linesByKey.containsKey(key)) {
				linesByKey.put(key, value);
			} else {
				String lineCount = formattedLineCount();
				value.setLineNbr(lineCount);
				linesByKey.put(value.getLineIdentifier(), value);
				linesByLineNbr.put(lineCount, value.getLineIdentifier());
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	
	/**
	 * END NEW METHODS TO BE ADDED BACK TO SOURCE FOR TMS
	 * 
	 */
	
	/**
	 * Look in this ares file and get the value for the requested key
	 * @param key the key
	 * @return value 
	 */
	public String getValue(String key) {
		String result;
		try {
			result = linesByKey.get(key).getValue();
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
			result = "";
		}
		return result;
	}
	
	/**
	 * RestManager the keys for this resource as an iterator
	 * @return iterator over the keys
	 */
	public Iterator<String> getKeys() {
		return linesByKey.keySet().iterator();
	}
	
	public Collection<LibraryLine> getValues() {
		return linesByKey.values();
	}
	
	/**
	 * Set the value for the given key.
	 * The value will be wrapped in quotes, as 
	 * required for an ares value.
	 * @param key the key
	 * @param value the value
	 */
	public void setValue(String key, String value) {
		try {
			if (linesByKey.containsKey(key)) {
				linesByKey.get(key).setValue(value);
			} else {
				String lineCount = formattedLineCount();
				LibraryLine al = new LibraryLine(lineCount, key, value);
				linesByKey.put(al.getLineIdentifier(), al);
				linesByLineNbr.put(lineCount, al.getLineIdentifier());
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
//		writeAresFile();
	}
	
	public void removeLine(String key) {
		try {
			if (linesByKey.containsKey(key)) {
				String lineNbr = linesByKey.get(key).getLineIdentifier();
				linesByKey.remove(key);
				linesByLineNbr.remove(lineNbr);
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	/**
	 * RestManager the ares resource name.  This is the name minus the .ares 
	 * extension and is usually the first property in the ares file.
	 * @return the ares resource name.
	 */
	public String getResourceName() {
		return file.getName().replace(".ares", "");
	}
	
	/**
	 * RestManager the path to the ares file
	 * @return the path
	 */
	public String getResourcePath() {
		return file.getPath();
	}
}
