package org.ocmc.ioc.liturgical.synch.alwb.gateway.enums;

/**
 * Defines the commands used for interacting with a CouchDb database server.
 * 
 * What is the difference between short and long flags?  This is defined by a Posix standard.
 * 
 * -ab is equivalent to -a -b.  The flags used with single dash must be a single character.
 * --ab means a command whose name is ab.
 * 
 * Verbs: 
 * 		-c create
 * 		-s	 show (i.e., read)
 * 		-m modify
 * 		-d	 delete
 * 		-r  replicate (one db to another)
 * 		-t	 truncate (remove all docs from a db)
 * 
 * Objects:
 * 		-u user
 * 		-b	 database
 * 		-e entry (a CouchDB doc)
 *     -p password
 *     -a authorization
 * 
 * Parms
 * 		-k key, e.g. username, or database name, or doc _id
 * 		-v value, e.g. text of a doc, the password for a user
 * Result type
 * 		-j JsonObject (if not specified, will be whatever the method is programmed to return)
 * 
 * @author mac002
 *
 * Used: a, b, c, d, e, f, g, h, i, j, k, m, n, o, p, q, r, s, t, u, v, w, x, y
 * Available: v, z
 */
public enum CliDbCommands {
	FORMAT_JSON("j", "jsonresult", "Show the json returned by the server.", false, true)
	, INTERACTIVE("i", "interactive", "Enter interactive command shell.", false, false)
	, OBJ_AUTH("a", "auth","Command is regarding an authorization (aka role).", false, true)
	, OBJ_DB("b", "db","Command is regarding a database.", false, true)
	, OBJ_ENTRY("e", "entry","Command is regarding an entry, i.e. a CouchDB doc.", false, true)
	, OBJ_USER("u", "user","Command is regarding a user", false, true)
	, OBJ_PWD("p", "pwd","Command is regarding a user password.", false, true)
	, PARM_COMMENT("y", "comment","The comment for a doc.", true, true)
	, PARM_DB("l", "dbname","The name of a database.", true, true)
	, PARM_ID("f", "docid","The id for a doc.", true, true)
	, PARM_JSON("n", "jsondata","A json string.", true, true)
	, PARM_PWD("w", "password","A user's password.", true, true)
	, PARM_ROLE("o", "roles","Roles of a user or database", true, true)
	, PARM_KEY("k", "key","The key (aka name, ID) of a user, a doc (entry), or database.", true, true)
	, PARM_TEXT("x", "text","The text value for a doc.", true, true)
	, PARM_USER("v", "username","A username (aka id).", true, true)
	, VERB_CREATE("c", "create","Create something, e.g. a user, database, or entry (aka doc).", false, true)
	, VERB_DELETE("d", "delete","Delete something, e.g. a user, database, or entry (aka doc).", false, true)
	, VERB_GET("g", "get","RestManager something from the database server. -k required & must be a valid REST path.", false, true)
	, VERB_MODIFY("m", "modify","Modify something, e.g. a user, database, or entry (aka doc).", false, true)
	, VERB_REPLICATE("r", "replicate","Replicate source database to target database.", false, true)
	, VERB_SHOW("s", "show","Show things in database server, use with flags -u, -d, -e.", false, true)
	, VERB_TRUNCATE("t", "truncate","Truncate (delete) all docs (entries) from database.", false, true)
	, QUIT("q", "quit", "Quit the interactive command shell.", false, true);
	  
	  private String shortFlag;
	  private String longFlag;
	  private String description;
	  private boolean hasArg = false;
	  private boolean argOptional = false;
	  private boolean forInteractive = false;
	  private String fs = "-";
	  private String fl = "--";
	  
	  private CliDbCommands(
			  String shortFlag
			  , String longFlag
			  , String description
			  , boolean hasArg
			  , boolean forInteractive
			  ) {
		  this.shortFlag = shortFlag;
		  this.longFlag = longFlag;
		  this.description = description;
		  this.hasArg = hasArg;
		  this.forInteractive = forInteractive;
	  }

	public String getShortFlag() {
		if (shortFlag.length() > 1) {
			return fl+shortFlag;
		} else {
			return fs+shortFlag;
		}
	}

	public String getShort() {
		return shortFlag;
	}
	public void setShortFlag(String shortFlag) {
		this.shortFlag = shortFlag;
	}

	public String getLongFlag() {
		return fl+longFlag;
	}
	
	public String getLong() {
		return longFlag;
	}

	public void setLongFlag(String longFlag) {
		this.longFlag = longFlag;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean hasArg() {
		return hasArg;
	}

	public void setHasArg(boolean hasArgs) {
		this.hasArg = hasArgs;
	}

	public boolean isForInteractive() {
		return forInteractive;
	}

	public void setForInteractive(boolean forInteractive) {
		this.forInteractive = forInteractive;
	}

}
