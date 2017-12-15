package org.ocmc.ioc.liturgical.synch.git;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommitCompare;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffEntry;
import org.ocmc.ioc.liturgical.synch.git.models.GitStatus;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utilities for accessing Github repositories.  
 * Note that the Github api (api.github.com) has rate limits:
 * 60 requests per hour per account for unauthenticated requests
 * 5000 requests per hour per account for authenticated requests
 * 
 * @author mac002
 *
 */
public class GithubApiClient {
	private static final Logger logger = LoggerFactory.getLogger(GithubApiClient.class);
	private RepositoryService repoService = null;
	private DataService dataService = null;
	private CommitService commitService = null;
	private GitHubClient client = new GitHubClient();
	private String oldCommit = "722455bfbae840ec66c12709847b49f04cf238d0";

	public GithubApiClient (
			String oAuth
			) {
		GitHubClient client = new GitHubClient();
		client.setOAuth2Token(oAuth);
		repoService = new RepositoryService(client);
		dataService = new DataService(client);
		commitService = new CommitService(client);
	}

	public void process(GithubRepositories repos) {
		for (GithubRepo repo : repos.getRepos()) {
			try {
				  Repository repository = repoService.getRepository(
						  repo.getAccount()
						  , repo.getName()
						  );
			      RepositoryCommitCompare repoCompare = this.compare(repository, oldCommit);
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}
		}
	}
	
	public RepositoryCommitCompare compare(Repository repository, String baseCommit) {
		RepositoryCommitCompare result = null;
		try {
			  Reference master = dataService.getReference(repository, "heads/master");
			  Reference lastCommit = dataService.getReference(repository, baseCommit);
		      RepositoryCommitCompare repoCompare = commitService.compare(repository, lastCommit.getObject().getSha(), master.getObject().getSha());
		} catch (Exception e ) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public String getHeadCommitId(String account, String repoName) {
		String result = "";
		try {
			  Repository repository = repoService.getRepository(account, repoName);
			  Reference master = dataService.getReference(repository, "heads/master");
		      Commit baseCommit = dataService.getCommit(repository, master.getObject().getSha());			
			  result = baseCommit.getSha();
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public GitStatus updateAresGithubRepos(GithubRepositories repos) {
		GitStatus status = new GitStatus();
		return status;
	}
	
	public List<GitDiffEntry> compare(String repo, String lastCommitId) {
		List<GitDiffEntry> result = new ArrayList<GitDiffEntry>();
		return result;
	}

	public List<GitDiffEntry> compareEmptyTree(String repoPath) {
		List<GitDiffEntry> result = new ArrayList<GitDiffEntry>();
		return result;
	}

}
