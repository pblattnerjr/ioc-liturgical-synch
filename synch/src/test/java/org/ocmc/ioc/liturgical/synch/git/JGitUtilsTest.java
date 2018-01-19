package org.ocmc.ioc.liturgical.synch.git;

import static org.junit.Assert.*;

import org.junit.Test;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;

public class JGitUtilsTest {

	@Test
	public void test() {
		String alwbPath = "/Users/mac002/Git/alwb-repositories/ages"; // ages only
		GithubRepositories repos = JGitUtils.getRepositories(alwbPath);
		for (GithubRepo repo : repos.getRepos()) {
			repo.setPrettyPrint(true);
			System.out.println(repo.toJsonString());
		}
		fail("Not yet implemented");
	}

}
