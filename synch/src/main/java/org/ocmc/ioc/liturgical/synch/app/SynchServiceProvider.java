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

	public static void main( String[] args ) {
    	try {
    		Properties prop = new Properties();
    		InputStream input = null;
        	String ws_usr = args[0];
        	String ws_pwd = args[1];
        	githubToken = args[2];
        	SynchManager synchManager = null;
        	
    		try {
    			logger.info("logger info enabled = " + logger.isInfoEnabled());
    			logger.info("logger warn enabled = " + logger.isWarnEnabled());
    			logger.info("logger trace enabled = " + logger.isTraceEnabled());
    			logger.info("logger debug enabled = " + logger.isDebugEnabled());
    			logger.debug("If you see this, logger.debug is working");
    			logger.info("ioc-liturgical-synch version: " + Constants.VERSION);
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
    	        	messagingToken = args[3];
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
    				);
    				
    				if (testGitSynch) {
    					logger.info("Using test ares repos to test git synch");
    					logger.info("The docs in the synch db will use the label " + Constants.LABEL_GITHUB_TEST_REPO);
    					synchManager.setGithubRepos(GITHUB_TEST_ARES_URLS.toPOJO());
    					synchManager.setUseTestRepos(true);
    					if (deleteTestGitRepos) {
    						logger.info("Deleting test repos");
        					synchManager.deleteTestRepos();
    					}
    				}
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
