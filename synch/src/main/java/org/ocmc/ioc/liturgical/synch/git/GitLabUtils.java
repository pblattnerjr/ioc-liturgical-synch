package org.ocmc.ioc.liturgical.synch.git;

import org.gitlab4j.api.GitLabApi;

public class GitLabUtils {
	public GitLabApi gitLabApi = null;
	
	public GitLabUtils (String url, String token) {
		this.gitLabApi = new GitLabApi(url, token);
	}

	public GitLabApi getGitLabApi() {
		return gitLabApi;
	}

	public void setGitLabApi(GitLabApi gitLabApi) {
		this.gitLabApi = gitLabApi;
	}
}
