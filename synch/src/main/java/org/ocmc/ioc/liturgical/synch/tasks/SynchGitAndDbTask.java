package org.ocmc.ioc.liturgical.synch.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.synch.AresTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.ResultJsonObjectArray;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_INITIAL_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.git.GitCommitFileProcessor;
import org.ocmc.ioc.liturgical.synch.git.GithubService;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitDetails;
import org.ocmc.ioc.liturgical.synch.managers.SynchManager;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

/**
 * Runs a task (separate thread) to read Github repositories,
 * convert changes to database transactions, 
 * and push the transactions to the synch server.
 * 
 * The transactions currently are only for updating the database from git.
 * TODO: update Github from the database.
 * @author mac002
 *
 */
public class SynchGitAndDbTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(SynchGitAndDbTask.class);

	SynchManager synchManager = null;
	GithubService githubService = null;
	boolean printpretty = false;
	String token = "synchrepos";
	boolean debug = false;
	boolean overRideLastFetchCommitId = true;
	String overRideSynchCommitId = "03a4a2dbbc6edd41eff817fd15c3c66b39aa04ad";
	String overRideFetchCommitId = "f497d10eeb045a998b1cbff3414e765930b0a096";
	
	public SynchGitAndDbTask (
			SynchManager synchManager
			, String token
			) {
		this.synchManager = synchManager;
		this.token = token;
		githubService = new GithubService(
				token
				, GITHUB_INITIAL_ARES_URLS.GR_GR_COG.account
				, GITHUB_INITIAL_ARES_URLS.GR_GR_COG.repoName
				);
	}
	
	public SynchGitAndDbTask (
			SynchManager synchManager
			, String token
			, boolean printpretty
			, boolean debug
			) {
		this.synchManager = synchManager;
		this.printpretty = printpretty;
		this.token = token;
		this.debug = debug;
		this.synchManager.setDebug(debug);
		githubService = new GithubService(
				token
				, GITHUB_INITIAL_ARES_URLS.GR_GR_COG.account
				, GITHUB_INITIAL_ARES_URLS.GR_GR_COG.repoName
				);
	}
	
	@Override
	public void run() {
		/**
		 * TODO:
		 * 1. Create the AresPullTransactions using data from Github
		 * 2. Create a map of the push and pulls using the LTK as the key.
		 * 3. Process the map, using the timestamp of the Ares trans.  
		 * 
		 * At the moment, this code is only handling ares to db.
		 */
		this.doSynch();
	}
	
	private synchronized void doSynch() {
		try {
			int limitStart = githubService.GetRemainingLimit();
			if (synchManager.synchConnectionOK()) {
				GithubRepositories repos = githubService.updateStatuses(
						synchManager.getGithubSynchInfo()
						);
				
				for (GithubRepo repo : repos.getRepos()) {
					if (this.debug) {
						if (overRideLastFetchCommitId) {
							repo.setLastGitToDbSynchCommitId(overRideSynchCommitId);
							repo.setLastGitToDbFetchCommitId(overRideFetchCommitId);
						}
						logger.info("fCommitId = " + repo.lastGitToDbFetchCommitId);
						logger.info("sCommitId = " + repo.lastGitToDbSynchCommitId);
					}
					if (
							(repo.lastGitToDbFetchCommitId.length() > 0 && repo.lastGitToDbSynchCommitId.length() > 0)
						&& (repo.lastGitToDbSynchCommitId.equals(repo.lastGitToDbFetchCommitId))
						){
						this.logMessage(repo.getName(), "is current");
					} else {
 						githubService = new GithubService(
								token
								, repo.getAccount()
								, repo.getName()
								);
 						CommitDetails masterCommit = githubService.getMasterCommit();
						if (repo.lastGitToDbSynchCommitId.length() == 0) {
								this.logMessage(repo.getName(), "first time to synch");
							// TODO: handle initial synch.  Either we assume we always start with a db that has been loaded once or we assume it is empty
						} else {
							this.logMessage(repo.getName(), "will be synched");
							/**
							 * The ResultJsonObjectArray values will be JsonObjects
							 * made from the subclasses of the Java class CommitFile.
							 */
							ResultJsonObjectArray result = 
									githubService.compareCommits(
											repo.lastGitToDbSynchCommitId
											, repo.lastGitToDbFetchCommitId
											);
							if (result.status.code == 200) {
								JsonArray values = result.getFirstObject().get("files").getAsJsonArray();
								for (JsonElement value : values) { // these are Json made from the CommitFile class
									GitCommitFileProcessor fileProcessor = new GitCommitFileProcessor(
											value
											, masterCommit.commit.committer.name
											, masterCommit.commit.committer.date
											, repo.lastGitToDbSynchCommitId
											, repo.lastGitToDbFetchCommitId
											);
									fileProcessor.printGitDiffLibraryLines();
									List<AresTransaction> aresList = new ArrayList<AresTransaction>();
									if (fileProcessor.getStatus() == 
											GitCommitFileProcessor.STATUSES.RENAMED) {
										aresList.add(
												fileProcessor.getAresToDBTransactionForFileRename()
										);
									} else {
										aresList = fileProcessor.process();
									}
									for (AresTransaction ares : aresList) {
										if (debug && printpretty) {
											ares.setPrettyPrint(true);
											System.out.println(ares.toJsonString());
										}
										synchManager.processAresTransaction(ares);
									}
								}
							} else {
								ErrorUtils.error(logger, result.getQuery());
								ErrorUtils.error(logger, result.status.getCode() + " " + result.getStatus().getDeveloperMessage());
							}
						}
						repo.setLastGitToDbSynchCommitId(repo.lastGitToDbFetchCommitId);
						repo.setLastGitToDbSynchTime(Instant.now().toString());
					}
					synchManager.updateGitRepoSynchInfo(repo);
				}
				int limitEnd = githubService.GetRemainingLimit();
				if (debug) {
					logger.info("Github calls by synch task = " + (limitStart - limitEnd) + ". Available: " + limitEnd);
				}
			} else {
				ErrorUtils.warn(logger,"Synch Manager not available...");
				synchManager.initializeSynchDriver();
			}
		} catch (Exception e) { 
			ErrorUtils.report(logger, e);
		}
	}
	
	private  void logMessage(String repoName, String message) {
		if (this.debug) {
			try {
		 		logger.info(
						repoName + " " 
								+ message 
								+ ". Limit = " 
								+ githubService.GetRemainingLimit()
					);
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}
		}
	}
		
	
}
