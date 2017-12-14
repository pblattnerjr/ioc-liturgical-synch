package org.ocmc.ioc.liturgical.synch.git;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
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
	private RepositoryService service = null;
	private DataService dataService = null;
	private GitHubClient client = new GitHubClient();

	public GithubApiClient (
			String oAuth
			) {
		GitHubClient client = new GitHubClient();
		client.setOAuth2Token(oAuth);
		service = new RepositoryService(client);
		dataService = new DataService(client);
	}

	public void process(GithubRepositories repos) {
		for (GithubRepo repo : repos.getRepos()) {
			try {
				  Repository repository = service.getRepository(
						  repo.getAccount()
						  , repo.getName()
						  );
				  Reference master = dataService.getReference(repository, "heads/master");
			      Commit baseCommit = dataService.getCommit(repository, master.getObject().getSha());
			      System.out.println(repository.getName() + " " + baseCommit.getAuthor().getName() + " " + baseCommit.getAuthor().getDate() + " "+ baseCommit.getSha());
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}
		}
	}
	
	public String getHeadCommitId(String account, String repoName) {
		String result = "";
		try {
			  Repository repository = service.getRepository(account, repoName);
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
