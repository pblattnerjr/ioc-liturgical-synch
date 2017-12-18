package org.ocmc.ioc.liturgical.synch.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLEngineResult.Status;

import org.apache.commons.io.FilenameUtils;
import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresPushTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.RequestStatus;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.ResultJsonObjectArray;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_INITIAL_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.git.GithubService;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffLibraryLine;
import org.ocmc.ioc.liturgical.synch.git.models.github.Commit;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitDetails;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFile;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFileChanged;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFileRenamed;
import org.ocmc.ioc.liturgical.synch.managers.SynchManager;
import org.ocmc.ioc.liturgical.synch.utils.SynchUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.ocmc.ioc.liturgical.utils.FileUtils;

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

	SynchManager synchManager = null;
	GithubService githubService = null;
	boolean printpretty = false;
	String token = "synchrepos";
	boolean debug = false;
	
	public SynchGitPushTask (
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
	
	public SynchGitPushTask (
			SynchManager synchManager
			, String token
			, boolean printpretty
			, boolean debug
			) {
		this.synchManager = synchManager;
		this.printpretty = printpretty;
		this.token = token;
		this.debug = debug;
		githubService = new GithubService(
				token
				, GITHUB_INITIAL_ARES_URLS.GR_GR_COG.account
				, GITHUB_INITIAL_ARES_URLS.GR_GR_COG.repoName
				);
	}
	
	@Override
	public void run() {
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
						logger.info("fCommitId = " + repo.lastFetchCommitId);
						logger.info("sCommitId = " + repo.lastSynchCommitId);
					}
					if (
							(repo.lastFetchCommitId.length() > 0 && repo.lastSynchCommitId.length() > 0)
						&& (repo.lastSynchCommitId.equals(repo.lastFetchCommitId)))
					{
						this.logMessage(repo.getName(), "is current");
					} else {
 						githubService = new GithubService(
								token
								, repo.getAccount()
								, repo.getName()
								);
 						CommitDetails masterCommit = githubService.getMasterCommit();
						if (repo.lastSynchCommitId.length() == 0) {
							this.logMessage(repo.getName(), "first time to synch");
							// TODO: handle initial synch.  Either we assume we always start with a db that has been loaded once or we assume it is empty
						} else {
							this.logMessage(repo.getName(), "will be synched");
							ResultJsonObjectArray result = 
									githubService.compareCommits(
											repo.lastSynchCommitId
											, repo.lastFetchCommitId
											);
							if (result.status.code == 200) {
								JsonArray values = result.getFirstObject().get("files").getAsJsonArray();
								for (JsonElement value : values) {
									RequestStatus status = this.processFileCommit(
											value
											, masterCommit.commit.committer.name
											, masterCommit.commit.committer.date
											);
								}
							} else {
								ErrorUtils.error(logger, result.getQuery());
								ErrorUtils.error(logger, result.status.getCode() + " " + result.getStatus().getDeveloperMessage());
							}
						}
						repo.setLastSynchCommitId(repo.lastFetchCommitId);
						repo.setLastSynchTime(Instant.now().toString());
					}
					synchManager.updateGitRepoSynchInfo(repo);
				}
				int limitEnd = githubService.GetRemainingLimit();
				logger.info("Github calls by synch task = " + (limitStart - limitEnd) + ". Available: " + limitEnd);
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
	
	public RequestStatus processFileCommit(JsonElement json
			, String committerName
			, String committerDate
			) {
		RequestStatus status = new RequestStatus();
		CommitFile abstractFile = new CommitFile();
		CommitFileChanged commitFile = new CommitFileChanged();
		CommitFileRenamed renamedFile = new CommitFileRenamed();
		String filenameFrom = "";
		String filenameTo = "";
		String [] filenameFromParts = new String[2];
		String [] filenameToParts = new String[2];
			try {
				abstractFile = AbstractModel.gson.fromJson(json, CommitFile.class);
				filenameTo = FilenameUtils.getBaseName(abstractFile.getFilename());
				filenameToParts = FileUtils.getAresFileParts(filenameTo);
				if (abstractFile.fileWasRenamed()) {
					renamedFile = AbstractModel.gson.fromJson(json, CommitFileRenamed.class);
					filenameFrom = FilenameUtils.getBaseName(renamedFile.getPrevious_filename());
					filenameFromParts = FileUtils.getAresFileParts(filenameFrom);
				} else {
					commitFile = AbstractModel.gson.fromJson(json, CommitFileChanged.class);
					filenameFrom = filenameTo;
					filenameFromParts = filenameToParts;
				}
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}

		// TODO: convert commitFile info into a list of AresPushTransactions
		List<AresPushTransaction> aresList = new ArrayList<AresPushTransaction>();
		switch (commitFile.status) { // this is the status of the file itself
		case ("added"): {
			aresList = SynchUtils.handleCommitFileAdded(
					filenameFromParts
					, filenameToParts
					, commitFile
					, committerName
					, committerDate
					);
			break;
		}
		case ("modified"): {
			aresList = SynchUtils.handleCommitFileModified(
					filenameFromParts
					, filenameToParts
					, commitFile
					, committerName
					, committerDate
					);
			break;
		}
		case ("removed"): {
			aresList = SynchUtils.handleCommitFileRemoved(
					filenameFromParts
					, filenameToParts
					, commitFile
					, committerName
					, committerDate
					);
			break;
		}
		case ("renamed"): {
			aresList = SynchUtils.handleCommitFileRenamed(
					filenameFromParts
					, filenameToParts
					, renamedFile
					, committerName
					, committerDate
					);
			break;
		}
		default: {
			// TODO: handle as an error
			break;
		}
		}
		for (AresPushTransaction ares : aresList) {
			ares.setPrettyPrint(true);
			System.out.println(ares.toJsonString());
			// TODO remove the print lines from above and uncomment the line below
//			status = processAresPushTransaction(aresTransaction);
		}
		return status;
	}
	
	
}
