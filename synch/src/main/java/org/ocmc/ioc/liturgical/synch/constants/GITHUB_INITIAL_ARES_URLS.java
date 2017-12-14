package org.ocmc.ioc.liturgical.synch.constants;

import org.apache.commons.io.FilenameUtils;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;

public enum GITHUB_INITIAL_ARES_URLS {
	EN_UK_LASH("https://github.com/AGES-Initiatives/ages-alwb-library-lash.git")
	, EN_US_ANDRONACHE("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-andronache.git")
	, EN_US_BARRETT("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-barrett.git")
	, EN_US_BOYER("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-boyer.git")
	, EN_US_CONSTANTINIDES("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-constantinides.git")
	, EN_US_DEDES("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-dedes.git")
	, EN_US_GOA("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-goa.git")
	, EN_US_HOLYCROSS("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-holycross.git")
	, EN_US_OCA("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-oca.git")
	, EN_US_PUBLIC("https://github.com/AGES-Initiatives/ages-alwb-library-client-enpublic.git")
	, EN_US_REPASS("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-repass.git")
	, EN_US_UNKNOWN("https://github.com/AGES-Initiatives/ages-alwb-library-en-us-unknown.git")
	, GR_GR_COG("https://github.com/AGES-Initiatives/ages-alwb-library-gr-gr-cog.git")
	, KIK_KE_OAG("https://github.com/AGES-Initiatives/oak-alwb-library-kik-ke-oak.git")
	, SWA_KE_OAK("https://github.com/AGES-Initiatives/oak-alwb-library-swh-ke-oak.git")
	;
	
	public String url = "";
	public String repoDirPath = "";

	private GITHUB_INITIAL_ARES_URLS(String url) {
		this.url = url;
		String fileName = FilenameUtils.getName(url);
		if (fileName.endsWith(".git")) {
			fileName = fileName.substring(0, fileName.length()-4);
		}
		this.repoDirPath = fileName;
	}
	
	public static GithubRepositories toPOJO() {
		GithubRepositories repos = new GithubRepositories();
		for (GITHUB_INITIAL_ARES_URLS item : GITHUB_INITIAL_ARES_URLS.values()) {
			GithubRepo repo = new GithubRepo(item.url);
			repos.addRepo(repo);
		}
		return repos;
	}

}
