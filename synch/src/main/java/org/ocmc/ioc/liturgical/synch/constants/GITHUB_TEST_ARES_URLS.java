package org.ocmc.ioc.liturgical.synch.constants;

import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

public enum GITHUB_TEST_ARES_URLS {
//	EN_US_COLBURN("https://github.com/mcolburn/synch-test.git")
	EN_US_ANDRONACHE("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-andronache.git")
	;
	
	public String account = "";
	public String repoName = "";
	public String url = "";
	public String repoDirPath = "";

	private GITHUB_TEST_ARES_URLS(String url) {
		this.url = url;
		try {
			String [] parts = url.split("/");
			account = parts[parts.length-2];
			repoName = parts[parts.length-1];
			repoName = repoName.substring(0, repoName.length()-4);
			this.repoDirPath = repoName;
		} catch (Exception e) {
			ErrorUtils.reportAnyErrors(GITHUB_INITIAL_ARES_URLS.class.getName() + " " + e.getMessage());
		}
	}
	
	public static GithubRepositories toPOJO() {
		GithubRepositories repos = new GithubRepositories();
		for (GITHUB_TEST_ARES_URLS item : GITHUB_TEST_ARES_URLS.values()) {
			GithubRepo repo = new GithubRepo(item.account, item.repoName);
			repo.setUrl(item.url);
			repos.addRepo(repo);
		}
		return repos;
	}
}
