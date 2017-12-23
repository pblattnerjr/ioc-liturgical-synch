package org.ocmc.ioc.liturgical.synch.alwb.gateway.models;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.ocmc.ioc.liturgical.synch.alwb.gateway.enums.Action;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.enums.DataStore;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

/**
 * Wraps JsonObject and provides methods
 * to get values with assumption that this QueryForJson
 * has an ares entry schema of key (aka id), value, comment
 * @author mac002
 *
 */
public class AresEntry {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AresEntry.class);

	private JsonObject json;
	
	private Action action = Action.NO_ACTION;
	
	// Json properties
	public static final String ID = "_id";
	public static final String REV = "_rev";
	public static final String DELETED = "_deleted";
	public static final String COMMENT = "comment";
	public static final String VALUE = "value";

	private DataStore source = DataStore.UNKNOWN;
	
	// Existence flags
	private boolean existsInDb = false;
	private boolean existsInAres = false;
	private boolean existsInHistory = false;
	
	// Difference flags
	private boolean aresDifferentThanHistory = false;
	private boolean aresDifferentThanDb = false;
	private boolean historyDifferentThanDb = false;
	
	// Is this a simple key-value pair.
	// false means it has a value that is a redirect.
	private boolean simple = true;
	
	// have we visited this entry while evaluating the list of
	// candidates to take action on?
	private boolean visited = false;
	
	// convenience variable with getter/setter
	// for cases where we are using AresEntry to
	// store an AresLine and need to manually set
	// the topic.
	private String topic = null;
	
	public AresEntry() {
		json = new JsonObject();
	}
	
	public AresEntry(JsonObject json) {
		this.json = json;
	}
	
	public AresEntry(JsonObject json, DataStore source) {
		this.json = json;
		this.source = source;
	}

	/**
	 * 
	 * @param id the id of the doc
	 * @param rev the revision number of the doc.  Null if none.
	 * @param comment the comment
	 * @param value the value.  
	 * @param simple true if a simple key-value pair (e.g. value is text).  false if the value is a redirect.
	 */
	public AresEntry(
			String id
			, String rev
			, String value
			, String comment
			, boolean simple
			) {
		try {
			this.json = new JsonObject();
			this.setId(id);
			this.setRev(rev);
			if (simple) {
				this.setValueAsSimple(value);
			} else {
				this.setValueAsArray(value);
			}
			this.setComment(comment);
			this.simple = simple;
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	public JsonObject getJson() {
		return this.json;
	}
		
	public String getId() {
		try {
			return json.get(ID).getAsString();
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
			return null;
		}
	}
	
	/**
	 * Convenience method for when AresEntry is storing
	 * an Ares Line.
	 * @return topic + "|" + key
	 */
	public String getIdAsCouchDbDocId() {
		String result = getId();
		if (topic != null && topic.length() > 0 && (! result.contains("|"))) {
			result = topic + "|" + result;
		}
		return result;
	}
	
	public void setId(String value) {
		try {
			this.json.addProperty(ID, value);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	public String getRev() {
		try {
			if (json.has(REV)) {
				return json.get(REV).getAsString();
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setRev(String value) {
		try {
			if (value != null && value.length() > 0) {
				this.json.addProperty(REV, value);
			} else {
				if (this.json.has(REV)) {
					this.json.remove(REV);
				}
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	public String getComment() {
		try {
			if (this.json.has(COMMENT)) {
				return json.get(COMMENT).getAsString();
			} else {
				return "";
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
			return null;
		}
	}
	
	public void setComment(String value) {
		try {
			this.json.addProperty(COMMENT, value);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	public String getValue() {
		String result = null;
		try {
			if (this.json.has(VALUE)) {
					JsonElement e = json.get(VALUE);
					if (e.isJsonPrimitive()) {
						result  = e.getAsString();
					} else if (e.isJsonArray()) {
						result = e.getAsJsonArray().toString();
					}
			} else {
				result = "";
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
		return result;
	}
	
	/**
	 * 
	 * @param value a simple text value from an ares key-value pair
	 */
	public void setValueAsSimple(String value) {
		try {
			simple = true;
			this.json.addProperty(VALUE, value);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	/**
	 * 
	 * @param value an ares redirect key, i.e. a value that is an ares key
	 */
	public void setValueAsArray(String value) {
		try {
			simple = false;
			JsonArray e = new JsonArray();
			JsonObject o = new JsonObject();
			o.addProperty(ID, value);
			e.add(o);
			json.add(VALUE, e);
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}
	
	/**
	 * 
	 * @return the redirect key if the value is a redirect
	 */
	public String getRedirectKey() {
		String result = "";
		try {
			if (this.json.has(VALUE)) {
				JsonElement e = json.get(VALUE);
				if (e.isJsonArray()) {
					result = e.getAsJsonArray().get(0).getAsJsonObject().get(ID).getAsString();
				}
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
		return result;
	}

	/**
	 * RestManager a mashup for comparison purposes
	 * @return id + rev + value + comment as a concatenated string
	 */
	public String mash() {
		try {
			return getId()+ getRev() + getValue() + getComment();
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
			return null;
		}
	}

	public Action getAction() {
		return this.action;
	}

	/**
	 * Set the action.  Will also set various properties
	 * depending on what the action is.
	 * @param action AresEntry action
	 * @param rev - revision number
	 */
	public void setAction(Action action, String rev) {
		this.action = action;
		switch (action) {
			case CREATE: {
				setRev(rev);
				break;
			}
			case DELETE: {
				setDeleted(true);
				setRev(rev);
					break;
			}
			case UPDATE: {
				setRev(rev);
				break;
			}
		}
	}
	
	public boolean isDeleted() {
		try {
			if (json.has(DELETED)) {
				return json.get(DELETED).getAsBoolean();
			} else {
				return false;
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
			return false;
		}
	}
	
	public void setDeleted(boolean deleted) {
		try {
			if (deleted) {
				action = Action.DELETE;
				this.json.addProperty(DELETED, deleted);
			}  else {
				this.json.remove(DELETED);
			}
		} catch (Exception e) {
			ErrorUtils.report(LOGGER, e);
		}
	}

	public boolean existsInDb() {
		return existsInDb;
	}

	public void setExistsInDb(boolean existsInDb) {
		this.existsInDb = existsInDb;
	}

	public boolean existsInAres() {
		return existsInAres;
	}

	public void setExistsInAres(boolean existsInAres) {
		this.existsInAres = existsInAres;
	}

	public boolean existsInHistory() {
		return existsInHistory;
	}

	public void setExistsInHistory(boolean existsInHistory) {
		this.existsInHistory = existsInHistory;
	}

	public boolean aresDifferentThanHistory() {
		return aresDifferentThanHistory;
	}

	public void setAresDifferentThanHistory(boolean aresDifferentThanHistory) {
		this.aresDifferentThanHistory = aresDifferentThanHistory;
	}
	
	/**
	 * Sets the boolean based on comparison of the value and comment passed in.
	 * @param value - from history
	 * @param comment - from history
	 */
	public void setAresDifferentThanHistory(String value, String comment) {
		this.aresDifferentThanHistory = ! valueAndCommentMatch(value, comment);
	}

	public boolean aresDifferentThanDb() {
		return aresDifferentThanDb;
	}

	public void setAresDifferentThanDb(boolean aresDifferentThanDb) {
		this.aresDifferentThanDb = aresDifferentThanDb;
	}

	/**
	 * Sets the boolean based on comparison of the value and comment passed in.
	 * @param value - from database
	 * @param comment - from database
	 */
	public void setAresDifferentThanDb(String value, String comment) {
		this.aresDifferentThanDb = ! valueAndCommentMatch(value, comment);
	}

	
	public boolean historyDifferentThanDb() {
		return historyDifferentThanDb;
	}

	public void setHistoryDifferentThanDb(boolean historyDifferentThanDb) {
		this.historyDifferentThanDb = historyDifferentThanDb;
	}

	public DataStore getSource() {
		return source;
	}

	public void setSource(DataStore source) {
		this.source = source;
	}

	public boolean isSimple() {
		return simple;
	}

	public void setSimple(boolean simple) {
		this.simple = simple;
	}
	
	/**
	 * Compare the supplied rev to this entry's rev.
	 * @param rev - originally from CouchDB
	 * @return negative if this entry is older, zero if the same, positive if newer.
	 */
	public int compareRev(String rev) {
		return getRev().compareTo(rev);
	}
	
	/**
	 * Compare this entry's value and comment to the ones passed in.
	 * @param value - to be compared
	 * @param comment - to be compared
	 * @return true of both value and comment match this entity's
	 */
	public boolean valueAndCommentMatch(String value, String comment) {
		String localValue = getValue();
		String localComment = getComment();
		boolean sameValue = localValue.equals(value);
		boolean sameComment = localComment.equals(comment);
		return
				sameValue && sameComment;
	}
	
	/**
	 * Should action be taken for this entry?
	 * @return false if the action is set to NO_ACTION
	 */
	public boolean takeAction() {
		return action != Action.NO_ACTION;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	
	/**
	 * Convert the entry to an ares file entry
	 * @return ares file entry
	 */
	public String toAresLine() {
		String key = getKey();
		return getKey() + " = " + getAresValue() + getAresComment();
	}
	
	/**
	 * RestManager the value formatted as an ares value.
	 * @return a quoted string
	 */
	public String getAresValue() {
		if (getKey().startsWith("A_Resource_Whose_Name")) {
			return getValue();
		} else {
			return quote(getValue());
		}
	}
	
	/**
	 * RestManager the comment formatted for an Ares entry
	 * @return the formatted comment
	 */
	public String getAresComment() {
		String comment = getComment();
		if (comment != null && comment.length() > 0) {
			return " // " + comment;
		} else {
			return "";
		}
	}
	
	/**
	 * Wrap the value in quotation marks
	 * @param value
	 * @return
	 */
	private String quote(String value) {
			return "\"" + value + "\"";
	}
	
	/**
	 * RestManager the topic part of the ID
	 * @return the topic if found, else null
	 */
	public String getTopic() {
		String id = getId();
		if (id.contains("|")) {
			return id.split("|")[0];
		} else {
			return null;
		}
	}

	/**
	 * RestManager the key part of the ID
	 * @return the key
	 */
	public String getKey() {
		String id = getId();
		if (id.contains("|")) {
			return id.split(Pattern.quote("|"))[1];
		} else {
			return id;
		}
	}
	
	/**
	 * A CouchDB revision number has two parts.
	 * 
	 * The first part is a number that is incremented each time
	 * the doc is updated.
	 * 
	 * The second part is an MD5 hash of the contents.
	 * 
	 * @return the incremented part.
	 */
	public int getRevIncrement() {
		int result = -1;
		try {
			String rev = getRev();
			if (rev != null) {
				result = Integer.parseInt(rev.substring(0, 1));
			}
		} catch (Exception e) {
			result = -1;
		}
		return result;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * Create a copy of this AresEntry
	 * @return the copy
	 */
	public AresEntry copy() {
		AresEntry copy = new AresEntry(this.json);
		copy.setAction(this.action, this.getRev());
		copy.setAresDifferentThanDb(this.aresDifferentThanDb);
		copy.setAresDifferentThanHistory(this.aresDifferentThanHistory);
		copy.setExistsInAres(this.existsInAres);
		copy.setExistsInDb(this.existsInDb);
		copy.setExistsInHistory(this.existsInHistory);
		copy.setHistoryDifferentThanDb(this.historyDifferentThanDb);
		copy.setSimple(this.simple);
		copy.setSource(this.source);
		copy.setTopic(this.topic);
		copy.setVisited(this.visited);
		return copy;
	}

}
