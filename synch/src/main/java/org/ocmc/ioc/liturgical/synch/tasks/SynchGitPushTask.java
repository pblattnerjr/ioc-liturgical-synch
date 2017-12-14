package org.ocmc.ioc.liturgical.synch.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import org.ocmc.ioc.liturgical.schemas.models.synch.AresPushTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.synch.git.JGitUtils;
import org.ocmc.ioc.liturgical.synch.git.models.GitStatus;
import org.ocmc.ioc.liturgical.synch.git.models.SynchData;
import org.ocmc.ioc.liturgical.synch.managers.SynchManager;
import org.ocmc.ioc.liturgical.synch.utils.SynchUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

/**
 * Runs a task (separate thread) to read Github repositories,
 * convert changes to database transactions, 
 * and push the transactions to the synch server
 * .
 * @author mac002
 *
 */
public class SynchGitPushTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(SynchGitPushTask.class);
	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	private static final JsonParser parser = new JsonParser();

	SynchManager synchManager = null;
	boolean printpretty = false;
	String repoBase = "synchrepos";
	boolean debug = false;
	
	public SynchGitPushTask (
			SynchManager synchManager
			, String repoBase
			) {
		this.synchManager = synchManager;
		this.repoBase = repoBase;
	}
	
	public SynchGitPushTask (
			SynchManager synchManager
			, String repoBase
			, boolean printpretty
			, boolean debug
			) {
		this.synchManager = synchManager;
		this.printpretty = printpretty;
		this.repoBase = repoBase;
		this.debug = debug;
	}
	
	@Override
	public void run() {
		try {
			if (synchManager.synchConnectionOK()) {
				GitStatus status = JGitUtils.updateAresGithubRepos(
						synchManager.getGithubRepos()
						, this.repoBase
				);
				if (debug) {
					logger.info(status.toSummary());
				} else if (status.getCloned().size() > 0 || status.getUpdated().size() > 0){
					logger.info(status.toSummary());
				}
				// process the clones
				for (GithubRepo repo : status.getCloned()) {
					SynchData synchData = SynchUtils.getRepoSynchData(
							repo
							, true
							);
					for (AresPushTransaction ares : synchData.getTransactions()) {
						synchManager.processAresPushTransaction(ares);
//						ares.setPrettyPrint(true);
//						System.out.println(ares.toJsonString());
					}
				}
				// process the updates
				for (GithubRepo repo : status.getUpdated()) {
					SynchData synchData = SynchUtils.getRepoSynchData(
							repo
							, true
							);
					for (AresPushTransaction ares : synchData.getTransactions()) {
						synchManager.processAresPushTransaction(ares);
//						ares.setPrettyPrint(true);
//						System.out.println(ares.toJsonString());
					}
				}
			} else {
				logger.info("Synch Manager not available...");
				synchManager.initializeSynchDriver();
			}
		} catch (Exception e) { 
			ErrorUtils.report(logger, e);
		}
	}
	
}
