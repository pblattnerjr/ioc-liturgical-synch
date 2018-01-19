package org.ocmc.ioc.liturgical.synch.git;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.junit.BeforeClass;
import org.junit.Test;

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
