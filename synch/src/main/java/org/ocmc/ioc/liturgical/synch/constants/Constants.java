package org.ocmc.ioc.liturgical.synch.constants;

import java.util.concurrent.TimeUnit;

public class Constants {
	public static final String VERSION = "1.0-SNAPSHOT"; // should match most recent jar
	public static final int boltDriverConnectionTimeout = 30;
	public static final TimeUnit boltDriverConnectionTimeoutUnits = TimeUnit.SECONDS;
	public static final String LIBRARY_SYNCH = "en_sys_synch";
	public static final String TOPIC_SYNCH = "cypher";
	public static final String TOPIC_SYNCH_LOG = "synch";
	public static final String KEY_SYNCH_LOG = "log";
	public static final String ID_DELIMITER = "~";
	public static final String ID_SPLITTER = "~";
	public static final String PIPE_SPLITTER = "\\|";
	public static final String ALT_ID_DELIMITER = "~"; // TODO reconcile pipe vs tilde
	public static final String DOMAIN_DELIMITER = "_";
	public static final String DOMAIN_SPLITTER = "_";
	
	public static final String LABEL_GITHUB_REPO = "GithubRepo";
	public static final String LABEL_GITHUB_TEST_REPO = "GithubTestRepo";
	public static final String LABEL_SYNCH_TRANS = "Transaction";
	public static final String LABEL_SYNCH_TEST_TRANS = "TestTransaction";
	
	public static final String GithubRepositoriesLibraryTopic = 	
			Constants.LIBRARY_SYNCH
			+ "~" +  org.ocmc.ioc.liturgical.schemas.constants.Constants.GITHUB
			+ "~" ;
}
