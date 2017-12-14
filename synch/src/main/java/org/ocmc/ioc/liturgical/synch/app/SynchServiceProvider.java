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
import org.ocmc.ioc.liturgical.synch.tasks.SynchGitPushTask;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
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

	public static boolean synchEnabled = false; // can be overridden by serviceProvider.config
	public static String synchDomain = "";  // can be overridden by serviceProvider.config
	public static String synchBoltPort = "";  // can be overridden by serviceProvider.config
	public static String synchDomainWithPort = "";
	public static boolean testGitSynch = false;

	public static String repoBase = "synchrepos";
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
        	SynchManager synchManager = null;
        	
    		try {
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
    			
    			synchEnabled = toBoolean(synchEnabled, prop.getProperty("synch_enabled"));
    			logger.info("synch_enabled: " + synchEnabled);

    			if (synchEnabled) {

    				testGitSynch = toBoolean(testGitSynch, prop.getProperty("testGitSynch"));
        			logger.info("testGitSynch: " + testGitSynch);

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
    				
					if (synchManager.getGithubRepos() == null) {
						logger.info("This database has never been synched from the Git repos");
	    				if (testGitSynch) {
	    					logger.info("Using test ares repos to test git synch");
	    					synchManager.setGithubRepos(GITHUB_TEST_ARES_URLS.toPOJO());
	    				} else {
	    					logger.info("Using initial ares repos for git synch");
	    					synchManager.setGithubRepos(GITHUB_INITIAL_ARES_URLS.toPOJO());
	    				}
					}
					if (debug) {
						synchManager.printGithubReposInfo();
					}
    			} else {
    				synchDomainWithPort = "";
    			}
    			
    			if (synchEnabled) {
					executorService.scheduleAtFixedRate(
							new SynchGitPushTask(
									synchManager
									, repoBase
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
    		logger.error("You failed to pass in one or more of: username, password, synch domain, or synch port");
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

}
