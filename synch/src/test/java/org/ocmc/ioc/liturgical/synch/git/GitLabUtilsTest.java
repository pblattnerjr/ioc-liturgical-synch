package org.ocmc.ioc.liturgical.synch.git;

import static org.junit.Assert.*;

import java.util.List;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.junit.BeforeClass;
import org.junit.Test;
public class GitLabUtilsTest {

	static String token = "";
	static GitLabUtils utils = null;
	static String url = "https://gitlab.liml.org";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		token = System.getenv("token");
		utils = new GitLabUtils(url, token);
	}
	
	@Test
	public void test() {
		try {
//			utils.gitLabApi.sudo("mcolburn");
			List<Project> projects = utils.gitLabApi.getProjectApi().getProjects();
			for (Project p : projects) {
				System.out.println(p.getName());
			}
			assertTrue(projects.size() > 0);
		} catch (GitLabApiException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
