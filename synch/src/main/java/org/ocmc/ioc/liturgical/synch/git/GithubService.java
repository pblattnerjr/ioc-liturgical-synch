package org.ocmc.ioc.liturgical.synch.git;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.constants.HTTP_RESPONSE_CODES;
import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.ResultJsonObjectArray;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_MEDIA_TYPES;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitDetails;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFile;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFileChanged;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFileRenamed;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFiles;
import org.ocmc.ioc.liturgical.synch.git.models.github.GithubPatch;
import org.ocmc.ioc.liturgical.synch.git.models.github.RateLimit;
import org.ocmc.ioc.liturgical.synch.git.models.github.Ref;
import org.ocmc.ioc.liturgical.synch.git.models.github.Refs;
import org.ocmc.ioc.liturgical.synch.git.models.github.Resources;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class GithubService {
	private static final Logger logger = LoggerFactory.getLogger(GithubService.class);
	private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static JsonParser parser = new JsonParser();
	
	public static final String urlDomain = "https://api.github.com/repos/%s/%s/";
	public static final String urlCommits = urlDomain + "commits";
	public static final String urlCommitMaster = urlDomain + "commits/master";
	public static final String urlCompare = urlDomain + "compare/%s...%s";
	public static final String urlComparePatch = urlCompare + ".patch";
	public static final String urlCompareDiff = urlCompare + ".diff";
	public static final String urlCompareLastAndHead = urlDomain +"compare/HEAD^...HEAD";
	public static final String urlCompareWithHead = urlDomain +"compare/%s...HEAD";
	public static final String urlRefs = urlDomain + "git/refs";
	
	private String token = "";
	private String account = "";
	private String repo = "";

	public GithubService(
			String token
			, String account
			, String repo
			) {
		this.token = token;
		this.account = account;
		this.repo = repo;
	}

	public ResultJsonObjectArray getCommits() {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		String url = String.format(urlCommits, this.account, this.repo);
		try {
			result = get(url, CommitDetails.class);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(e.getMessage());
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public List<CommitDetails> getCommitsAsList() {
		List<CommitDetails> result = new ArrayList<CommitDetails>();
		ResultJsonObjectArray getResult = new ResultJsonObjectArray(true);
		String url = String.format(urlCommits, this.account, this.repo);
		try {
			getResult = get(url, CommitDetails.class);
			JsonArray jsonArray = getResult.getValuesAsJsonArray();
			for (JsonElement e : jsonArray) {
				CommitDetails c = gson.fromJson(e.toString(), CommitDetails.class);
				result.add(c);
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public ResultJsonObjectArray compareCommitsAsPatch(
			String shaFrom
			, String shaTo
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		String url = String.format(
				urlComparePatch
				, this.account
				, this.repo
				, shaFrom
				, shaTo
				);
		try {
			ResultJsonObjectArray getResult = getAsString(url);
			if (getResult.valueCount == 1) {
				JsonObject o = getResult.getFirstObject();
				String node = o.get("node").getAsString();
				String [] patches = node.split("From ");
				for (String group : patches) {
					if (group.length() > 0) {
						GithubPatch patch = new GithubPatch(
								"From "
								+ group
								);
						result.addValue(patch.toJsonObject());
					}
				}
			} else {
				result.setStatus(getResult.getStatus());
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public ResultJsonObjectArray compareCommits(
			String shaFrom
			, String shaTo
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		String url = String.format(
				urlCompare
				, this.account
				, this.repo
				, shaFrom
				, shaTo
				);
		try {
			ResultJsonObjectArray getResult = getAsJson(url);
			if (getResult.valueCount == 1) {
				JsonObject o = getResult.getFirstObject();
				JsonObject node = o.get("node").getAsJsonObject();
				JsonArray files = node.get("files").getAsJsonArray();
				CommitFiles commitFiles = new CommitFiles();
				for (JsonElement e : files) {
					CommitFile file = gson.fromJson(e, CommitFile.class);
					CommitFileChanged commitFile = new CommitFileChanged();
					CommitFileRenamed renamedFile = new CommitFileRenamed();
					if (file.fileWasRenamed()) {
						renamedFile = AbstractModel.gson.fromJson(e, CommitFileRenamed.class);
						commitFiles.addFile(renamedFile);
					} else {
						commitFile = AbstractModel.gson.fromJson(e, CommitFileChanged.class);
						commitFiles.addFile(commitFile);
					}
				}
				result.addValue(commitFiles.toJsonObject());
			} else {
				result.setStatus(getResult.getStatus());
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public String getRawFile(String url) {
		String result = "";
		try {
			HttpResponse<String> responseGet = Unirest.get(url)
					.basicAuth(token, "x-oauth-basic")
			        .header("content-type", GITHUB_MEDIA_TYPES.RAW.name)
			        .asString();
			result = responseGet.getBody();
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public int GetRemainingLimit() {
		return getRateLimit().core.remaining;
	}

	public RateLimit getRateLimit() {
		RateLimit result = new RateLimit();
		try {
			ResultJsonObjectArray requestResult = getAsJson("https://api.github.com/rate_limit");
			if (requestResult.valueCount > 0) {
				JsonObject node = requestResult.getFirstObject();
				Resources resources = gson.fromJson(node.get("node").getAsJsonObject(), Resources.class);
				result = resources.resources;
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public ResultJsonObjectArray get(
			String url
			, Class<? extends AbstractModel> model
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		result.setQuery(url);
		try {
			ResultJsonObjectArray getResult = getAsJson(url);
			if (getResult.status.getCode() == 200) {
				JsonArray responseArray = getResult.getFirstObject().get("node").getAsJsonArray();
				for (JsonElement e : responseArray) {
					AbstractModel details = gson.fromJson(
							e.getAsJsonObject()
							, model
						);
					result.addValue(details.toJsonObject());
				}
			} else {
				result.setStatus(getResult.getStatus());
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public ResultJsonObjectArray getAsJson(
			String url
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		result.setQuery(url);
		HttpResponse<JsonNode> responseGet = null;
		JsonObject o = new JsonObject();
		JsonElement je = null;
		JsonNode node = null;
		try {
			responseGet = Unirest.get(url)
					.basicAuth(this.token, "x-oauth-basic")
			        .header("content-type", GITHUB_MEDIA_TYPES.JSON.name)
			        .asJson();
			if (responseGet.getStatus() == 200) {
				node = responseGet.getBody();
				je = parser.parse(node.toString());
				o.add("node", je);
				result.addValue(o);
			} else {
				result.getStatus().code = responseGet.getStatus();
				result.getStatus().setMessage(responseGet.getStatusText());
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public ResultJsonObjectArray getAsString(
			String url
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		result.setQuery(url);
		try {
			HttpResponse<String> responseGet = Unirest.get(url)
					.basicAuth(this.token, "x-oauth-basic")
			        .header("content-type", "*/*")
			        .asString();
			if (responseGet.getStatus() == 200) {
				JsonObject o = new JsonObject();
				String body = responseGet.getBody();
				o.addProperty("node", body);
				result.addValue(o);
			} else {
				result.getStatus().code = responseGet.getStatus();
				result.getStatus().setMessage(responseGet.getStatusText());
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public ResultJsonObjectArray getAsText(
			String url
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(true);
		result.setQuery(url);
		try {
			HttpResponse<InputStream> responseGet = Unirest.get(url)
					.basicAuth(this.token, "x-oauth-basic")
			        .header("content-type", "*/*")
			        .asBinary();
			if (responseGet.getStatus() == 200) {
				JsonObject o = new JsonObject();
				String body = responseGet.getBody().toString();
				o.addProperty("node", body);
				result.addValue(o);
			} else {
				result.getStatus().code = responseGet.getStatus();
				result.getStatus().setMessage(responseGet.getStatusText());
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public GithubRepositories updateStatuses(GithubRepositories repos) {
		String currentAccount = this.account;
		String currentRepo = this.repo;
		List<GithubRepo> list = new ArrayList<GithubRepo>();
		for (GithubRepo repo : repos.getRepos()) {
			try {
				this.account = repo.getAccount();
				if (this.account.startsWith("test/")) {
					this.account = this.account.substring(5);
				}
				this.repo = repo.getName();
				String masterSha = this.getMasterSha();
				repo.setLastFetchCommitId(masterSha);
				repo.setLastFetchTime(Instant.now().toString());
				list.add(repo);
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}
		}
		repos.setRepos(list);
		this.account = currentAccount;
		this.repo = currentRepo;
		return repos;
	}
	
	public String getMasterSha() {
		String result = "";
		try {
			result = getBranchRefs().getMasterSha();
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public CommitDetails getMasterCommit() {
		CommitDetails result = new CommitDetails();
		String url = String.format(
				urlCommitMaster
				, this.account
				, this.repo
				);
		try {
			ResultJsonObjectArray getResult = getAsJson(url);
			if (getResult.valueCount == 1) {
				JsonObject o = getResult.getFirstObject();
				JsonObject node = o.get("node").getAsJsonObject();
				result = AbstractModel.gson.fromJson(node, CommitDetails.class);
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	/**
	 * 
	 * @return the ref to the head of each branch
	 */
	public Refs getBranchRefs() {
		Refs refs = new Refs();
		String url = String.format(
				urlRefs
				, this.account
				, this.repo
				);
		try {
			ResultJsonObjectArray getResult = getAsJson(url);
			if (getResult.valueCount == 1) {
				JsonObject o = getResult.getFirstObject();
				JsonArray node = o.get("node").getAsJsonArray();
				for (JsonElement e : node) {
					Ref ref = gson.fromJson(e.toString(), Ref.class);
					refs.addRef(ref);
				}
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return refs;
	}
		


}
