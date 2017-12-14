package org.ocmc.ioc.liturgical.synch.constants;

import org.apache.commons.io.FilenameUtils;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;

public enum GITHUB_TEST_ARES_URLS {
	EN_US_COLBURN("https://github.com/mcolburn/synch-test.git")
	;
	
	public String url = "";
	public String repoDirPath = "";

	private GITHUB_TEST_ARES_URLS(String url) {
		this.url = url;
		String fileName = FilenameUtils.getName(url);
		if (fileName.endsWith(".git")) {
			fileName = fileName.substring(0, fileName.length()-4);
		}
		this.repoDirPath = fileName;
	}
	
	public static GithubRepositories toPOJO() {
		GithubRepositories repos = new GithubRepositories();
		for (GITHUB_TEST_ARES_URLS item : GITHUB_TEST_ARES_URLS.values()) {
			GithubRepo repo = new GithubRepo(item.url);
			repos.addRepo(repo);
		}
		return repos;
	}
}
