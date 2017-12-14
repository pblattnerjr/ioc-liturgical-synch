package org.ocmc.ioc.liturgical.synch;

import org.ocmc.ioc.liturgical.synch.git.GithubApiClient;

public class EgitGithubTest {

	public static void main(String[] args) {
		GithubApiClient myClient = new GithubApiClient(args[0]);
		System.out.println(myClient.getHeadCommitId("mcolburn", "synch-test"));
//		GitHubClient client = new GitHubClient();
//		client.setOAuth2Token(args[0]);
//		RepositoryService service = new RepositoryService(client);
//		DataService dataService = new DataService(client);
//		try {
//			 Repository repository = service.getRepository("mcolburn", "synch-test");
//			  Reference master = dataService.getReference(repository, "heads/master");
//		      Commit baseCommit = dataService.getCommit(repository, master.getObject().getSha());			
//			  System.out.println(repository.getName() + " " + baseCommit.getAuthor().getName() + " " + baseCommit.getAuthor().getDate() + " "+ baseCommit.getSha());
//
//			  for (Repository repo : service.getOrgRepositories("AGES-Initiatives")) {
//				  master = dataService.getReference(repo, "heads/master");
//			      baseCommit = dataService.getCommit(repo, master.getObject().getSha());			
//				  System.out.println(repo.getName() + " " + baseCommit.getAuthor().getName() + " " + baseCommit.getAuthor().getDate() + " "+ baseCommit.getSha());
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

}
