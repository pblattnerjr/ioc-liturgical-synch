package org.ocmc.ioc.liturgical.synch.git.models;


import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.ocmc.ioc.liturgical.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a line read from an ares file.
 * There are four types of lines:
 *   1. Blank line (indicated by isBlankLine)
 *   2. Comment line, i.e. starts with / or * (indicated by isCommentLine)
 *   3. A simple key-value pair delimited by an equal sign, e.g. x.y.z = "some value", indicated by isSimpleKeyValue
 *   4. A key-value pair that has a comment after the value, e.g. x.y.z = "some value" // I am a comment
 *      This is indicated by hasCommentAfterValue
 *      
 *   If for some reason the line doesn't fit these four criteria, hasError will be set to true
 * @author mac002
 *
 */
public class LibraryLine  {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(LibraryLine.class);
	private String lineNbr = ""; // used to maintain the sequence within the ares file
	private String key = "";
	private String value = "";
	private String domain = "";
	private String topic = "";
	private String comment = "";
	public boolean doNotQuote = false; // sometimes the value is another key, so do not quote it
	public boolean isEmptyValue = false; // handle both x =  and x = ""
	public boolean isBlankLine = false;
	public boolean isCommentLine = false;
	public boolean hasCommentAfterValue = false;
	public boolean isSimpleKeyValue = false;
	public boolean hasError = false;
	public boolean valueIsKey = false;
	
	private String delimiter = " = ";
	
	public LibraryLine(String lineNbr, String line)  {
		this.lineNbr = lineNbr;
		parseLine(lineNbr, line);
	}
	
	public LibraryLine(String lineNbr, String key, String value) {
		this.lineNbr = lineNbr;
		parseLine(lineNbr,key + " = " + value);
	}

	/**
	 * RestManager the identifier for the line.  If it is a key-value
	 * pair, the key is used for the identifier.  Otherwise,
	 * the line number is used as the identifier.
	 * @return identifier
	 */
	public String getLineIdentifier() {
		if (isKeyValuePair() || doNotQuote) {
			return key;
		} else {
			return lineNbr;
		}
	}
	
	/**
	 * RestManager the topic and key as a CouchDB id
	 * @return the id
	 */
	public String getCouchDbId() {
		return topic.trim() + "|" + key.trim();
	}
	
	/**
	 * Is this line a key value-pair, i.e. either a 
	 * simple key-value pair, or a key-value pair with a 
	 * comment after the value
	 * @return true if so
	 */
	public boolean isKeyValuePair() {
		if (isSimpleKeyValue()) {
			return true;
		} else {
			if (isValueIsKey()) {
				return false;
			} else {
				if (hasCommentAfterValue) {
					return true;
				} else {
					return false;
				}
			}
		}
//		return (isSimpleKeyValue() || hasCommentAfterValue);
	}
	
	/**
	 * Parses a line and populates the variables for key, value, and comment
	 * and sets the various boolean descriptors.
	 * @param line
	 */
	private void parseLine(String lineNumber, String line) {
		String result = line.trim();
		if (result.isEmpty()) {
			key = lineNumber;
			value = line;
			isBlankLine = true;
		} else if (result.startsWith("/") || result.startsWith("*")) {
			key = lineNumber;
			value = line;
			isCommentLine = true;
		} else {
			String [] parts = result.split("=");
			if (parts.length == 2) {
				key = parts[0].trim();
				value = parts[1].trim();

				if (value.contains("//")) {
					parts = value.split("//");
					value = parts[0].trim();
					comment = parts[1].trim();
					hasCommentAfterValue = true;
				}
					if (value.length() > 0) {
						if (value.startsWith("\"") || value.startsWith("“")) {
							isSimpleKeyValue = true;
						} else {
							doNotQuote = true; // the value is probably a pointer to another key
							valueIsKey = true;
						}
					} else {
						isSimpleKeyValue = true;
						isEmptyValue = true;
					}
				value = GeneralUtils.unescapeQuotes(GeneralUtils.trimQuotes(value));
			} else {
				if (line.contains("=") && parts[0].length() > 0) {
					key = parts[0].trim();
					value = "";
				} else {
					key = lineNumber;
					value = line;
					hasError = true;
				}
			}
		}
		if (comment == null) {
			comment = "";
		}
	}

	/** 
	 * Reassemble the line, i.e. key = value // comment (if exists)
	 * @return the reassembled line
	 */
	public String getLine() {
		String result = "";
		if (key.contains("A_Resource_Whose_Name") || doNotQuote) {
			result = value;
		} else {
			result = GeneralUtils.wrapQuotes(value);
		}
		try {
			if (isBlankLine || isCommentLine || hasError) {
				result = value;
			} else {
				result = key + delimiter + result + (hasCommentAfterValue ? " // " + comment : "");
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
		return result;
	}
	
	
	public String getLineNbr() {
		return lineNbr;
	}
	
	public void setLineNbr(String lineNumber) {
		this.lineNbr = lineNumber;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
		hasCommentAfterValue = (comment != null && comment.length() > 0);
	}
	
	public boolean isRedirect() {
		if (isAresIdLine() || isCommentLine) {
			return false;
		} else {
			return value.contains("_");
		}
	}
	
	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
	
	/**
	 * 
	 * @param value - unquoted.  The method will 
	 * escape any internal quote marks, then
	 * wrap the entire string in quotes.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	public String getComment() {
		return comment;
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

	public boolean isDoNotQuote() {
		return doNotQuote;
	}

	public void setDoNotQuote(boolean doNotQuote) {
		this.doNotQuote = doNotQuote;
	}

	public boolean isEmptyValue() {
		return isEmptyValue;
	}

	public void setEmptyValue(boolean isEmptyValue) {
		this.isEmptyValue = isEmptyValue;
	}

	public boolean isBlankLine() {
		return isBlankLine;
	}

	public void setBlankLine(boolean isBlankLine) {
		this.isBlankLine = isBlankLine;
	}

	public boolean isCommentLine() {
		return isCommentLine;
	}

	public void setCommentLine(boolean isCommentLine) {
		this.isCommentLine = isCommentLine;
	}

	public boolean isHasCommentAfterValue() {
		return hasCommentAfterValue;
	}

	public void setHasCommentAfterValue(boolean hasCommentAfterValue) {
		this.hasCommentAfterValue = hasCommentAfterValue;
	}

	public boolean isAresIdLine() {
		if (key != null && key.startsWith("A_Resource_Whose_Name")) {
			return true;
		} else {
			return false;
		}

	}
	public boolean isSimpleKeyValue() {
		if (isAresIdLine()) {
			return false;
		} else {
			return isSimpleKeyValue;
		}
	}

	public void setSimpleKeyValue(boolean isSimpleKeyValue) {
		this.isSimpleKeyValue = isSimpleKeyValue;
	}

	public boolean isHasError() {
		return hasError;
	}

	public void setHasError(boolean hasError) {
		this.hasError = hasError;
	}

	public boolean isValueIsKey() {
		return valueIsKey;
	}

	public void setValueIsKey(boolean valueIsKey) {
		this.valueIsKey = valueIsKey;
	}

	public void setKey(String key) {
		this.key = key;
	}

		
}
