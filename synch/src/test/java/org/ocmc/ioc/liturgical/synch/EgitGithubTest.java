package org.ocmc.ioc.liturgical.synch;

import org.ocmc.ioc.liturgical.synch.constants.GITHUB_INITIAL_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.git.GithubApiClient;

public class EgitGithubTest {

	public static void main(String[] args) {
		GithubApiClient myClient = new GithubApiClient(args[0]);
		myClient.process(GITHUB_INITIAL_ARES_URLS.toPOJO());
	}

}
