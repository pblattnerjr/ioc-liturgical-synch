package org.ocmc.ioc.liturgical.synch.git;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.ResultJsonObjectArray;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitDetails;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFile;
import org.ocmc.ioc.liturgical.synch.git.models.github.RateLimit;
import org.ocmc.ioc.liturgical.synch.git.models.github.Refs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class GithubServiceTest {

	static String account = "mcolburn";
	static String repo = "synch-test";
//	static String account = "AGES-Initiatives"; // "mcolburn";
//	static String repo = "ages-alwb-library-gr-gr-cog"; // "synch-test";
	static String token = "";
	
	static GithubService utils;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		token = System.getenv("token");
		utils = new GithubService(token, account, repo);
	}

	
	@Test
	public void testGetRefs() {
		Refs refs = utils.getBranchRefs();
//		for (Ref ref : refs.getRefs()) {
//			ref.setPrettyPrint(true);
//			System.out.println(ref.toJsonString());
//		}
		assertTrue(refs.getRefs().size() > 0);
	}

	@Test
	public void testGetMasterSha() {
		String sha = utils.getMasterSha();
		assertTrue(sha.length() > 0);
	}

	@Test
	public void testGetCommits() {
		ResultJsonObjectArray result = utils.getCommits();
		assertTrue(result.valueCount > 0);
	}

	@Test
	public void testGetRateLimit() {
		RateLimit limit = utils.getRateLimit();
		assertTrue(limit.core.limit == 5000);
	}

	@Test
	public void testGetRemaingLimit() {
		int limit = utils.GetRemainingLimit();
		assertTrue(limit > 0);
	}

	@Test
	public void testGetCommitsAsList() {
		List<CommitDetails> result = utils.getCommitsAsList();
		assertTrue(result.size() > 0);
//		for (CommitDetails d : result) {
//			System.out.println(d.sha + " " + d.getCommit().committer.date);
//		}
		CommitDetails head = result.get(0);
		CommitDetails parent = result.get(result.size()-3);
		ResultJsonObjectArray getResult = utils.compareCommits(	
				parent.getSha()
				, head.getSha()
				);
		if (getResult.status.code == 200) {
			JsonArray values = getResult.getFirstObject().get("files").getAsJsonArray();
			CommitFile file = null;
			for (JsonElement value : values) {
				file = getResult.gson.fromJson(value.toString(), CommitFile.class);
				file.setPrettyPrint(true);
				System.out.println(file.toJsonString());
			}
			System.out.println(utils.getRawFile(file.raw_url));
		} else {
			System.out.println(getResult.getQuery());
			System.out.println(getResult.status.getCode() + " " + getResult.getStatus().getDeveloperMessage());
		}
		assertTrue(getResult.valueCount > 0);
	}
}
