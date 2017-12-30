package org.ocmc.ioc.liturgical.synch.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ocmc.ioc.liturgical.synch.constants.Constants;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_INITIAL_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_TEST_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.managers.SynchManager;
import org.ocmc.ioc.liturgical.synch.tasks.SynchGitAndDbTask;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.ocmc.ioc.liturgical.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the synch service
 *
 */
public class SynchServiceProvider {
	private static final Logger logger = LoggerFactory.getLogger(SynchServiceProvider.class);

	private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	public static boolean debug = false;

	public static boolean messagingEnabled = true; // can be overridden by serviceProvider.config
	public static boolean synchEnabled = false; // can be overridden by serviceProvider.config
	public static String synchDomain = "";  // can be overridden by serviceProvider.config
	public static String synchBoltPort = "";  // can be overridden by serviceProvider.config
	public static String synchDomainWithPort = "";
	public static boolean testGitSynch = false;
	public static boolean deleteTestGitRepos = false;

	public static String repoBase = "synchrepos";
	
	private static String githubToken = "";
	private static String messagingToken = "";

	/**
	 * If the property is null, the method returns
	 * back the value of var, otherwise it checks
	 * prop to see if it starts with "true".  If so
	 * it returns true, else false.  
	 * 
	 * The var is passed in so that if the config file lacks 
	 * the specified property, the default value gets used.
	 * @param var the variable
	 * @param prop the property
	 * @return true if so
	 */
	public static boolean toBoolean(boolean var, String prop) {
		if (prop == null) {
			return var;
		} else {
			return prop.startsWith("true");
		}
	}

	/**
	 * The main method must be provided the database admin username and password
	 * and a valid token for Github and one for Slack.  These can be passed via 
	 * command line arguments, or if using docker-compose, they are passed 
	 * through the setting of environment key-values.
	 * 
	 * @param args [0] db admin username [1] db admin password [2]  github token [3] slack token for synch channel
	 */
	public static void main( String[] args ) {
    	try {
    		Properties prop = new Properties();
    		InputStream input = null;
    		
    		String ws_usr = System.getenv("WS_USR");
    		if (ws_usr == null) {
    			ws_usr = args[0];
    		}
    		String ws_pwd = System.getenv("WS_PWD");
    		if (ws_pwd == null) {
    			ws_pwd = args[1];
    		}
    		githubToken = System.getenv("GITHUB_TOKEN");
    		if (githubToken == null) {
            	githubToken = args[2];
    		}
        	SynchManager synchManager = null;
        	
    		try {
    			logger.info("ioc-liturgical-synch version: " + Constants.VERSION);
    			logger.info("logger info enabled = " + logger.isInfoEnabled());
    			logger.info("logger warn enabled = " + logger.isWarnEnabled());
    			logger.info("logger trace enabled = " + logger.isTraceEnabled());
    			logger.info("logger debug enabled = " + logger.isDebugEnabled());
    			logger.debug("If you see this, logger.debug is working");
    			SynchServiceProvider.class.getClassLoader();
    			String location = getLocation();
    			logger.info("Jar is executing from: " + location);
    			File configFile = new File(location+"/resources/app.config");
    			logger.info("app.config exists: " + configFile.exists() + " - " + configFile.getPath());
    			if (configFile.exists()) {
    				input = new FileInputStream(new File(location+"/resources/app.config"));
    			} else {
    				logger.info("Trying to load config from bundle");
    				input = SynchServiceProvider.class.getClassLoader().getResourceAsStream("app.config");
    			}
        		prop.load(input);
    			
    			debug = toBoolean(debug, prop.getProperty("debug"));
    			logger.info("debug: " + debug);
    			
    			if (debug) {
    				try {
    				} catch (Exception e) {
    					ErrorUtils.report(logger, e);
    				}
    			}
    			
    			synchEnabled = toBoolean(synchEnabled, prop.getProperty("synch_enabled"));
    			logger.info("synch_enabled: " + synchEnabled);

    			messagingEnabled = toBoolean(messagingEnabled, prop.getProperty("messaging_enabled"));
    			logger.info("messaging_enabled: " + messagingEnabled);
    			
    			if (messagingEnabled) {
    				try {
    					messagingToken = System.getenv("MESSAGING_TOKEN");
        	    		if (messagingToken == null) {
            	        	messagingToken = args[3];
        	    		}
    				} catch (Exception e) {
    					logger.info("main args [3] missing parameter for messaging token");
    					throw e;
    				}
    			}

    			if (synchEnabled) {

    				testGitSynch = toBoolean(testGitSynch, prop.getProperty("testGitSynch"));
        			logger.info("testGitSynch: " + testGitSynch);

    				deleteTestGitRepos = toBoolean(deleteTestGitRepos, prop.getProperty("deleteTestGitRepos"));
        			logger.info("deleteTestGitRepos: " + deleteTestGitRepos);

        			repoBase = prop.getProperty("git_repoBase");
    				logger.info("git_repoBase: " + repoBase );

    				synchDomain = prop.getProperty("synch_domain");
    				logger.info("synch_domain: " + synchDomain );

    				synchBoltPort = prop.getProperty("synch_bolt_port");
    				logger.info("synch_bolt_port: " + synchBoltPort );
    				synchDomainWithPort = "bolt://" + synchDomain + ":" + synchBoltPort;
    				
    				synchManager = new SynchManager(
    						synchDomain
    						, synchBoltPort
    						, ws_usr
    						, ws_pwd
    						, testGitSynch
    						, deleteTestGitRepos
    				);
    			} else {
    				synchDomainWithPort = "";
    			}
    			
    			if (synchEnabled) {
					executorService.scheduleAtFixedRate(
							new SynchGitAndDbTask(
									synchManager
									, githubToken
									, debug
									, debug
									)
							, 10
							, 10
							, TimeUnit.SECONDS
							);

    			}

    		} catch (Exception e) {
    			ErrorUtils.report(logger, e);
    		}
    	} catch (ArrayIndexOutOfBoundsException arrayError) {
    		logger.error("You failed to pass in one or more of: username, password, github token, slack token");
    	} catch (Exception e) {
    		ErrorUtils.report(logger, e);
    	}
    }
	
	  public static String getLocation() {
		  try {
			return new File(SynchServiceProvider.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
		} catch (URISyntaxException e) {
			ErrorUtils.report(logger, e);
			return null;
		}
	  }
	  
	  public static String sendMessage(String message) {
		  String response = "";
		  MessageUtils.sendMessage(messagingToken, message);
		  return response;
	  }

}
