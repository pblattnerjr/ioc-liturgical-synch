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

public class GithubServiceUtility {

//	static String account = "mcolburn";
//	static String repo = "synch-test";
	static String account = "AGES-Initiatives"; // "mcolburn";
	static String repo = "ages-alwb-library-en-us-dedes"; // "ages-alwb-library-gr-gr-cog"; // "synch-test";
	static String token = "";
	
	static GithubService utils;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		token = System.getenv("token");
		utils = new GithubService(token, account, repo);
	}

	@Test
	public void testGetCommitsAsList() {
		String fromCommitId = "38e30e4c18259472f0ff453dff2ce00fd781a17c";
		String toCommitId = "255679e3cf8c26bfbff4546326ed018858e4d8ca";
		
		ResultJsonObjectArray getResult = utils.compareCommits(	
				fromCommitId
				, toCommitId
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
