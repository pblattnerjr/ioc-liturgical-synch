package org.ocmc.ioc.liturgical.synch.git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffEntry;
import org.ocmc.ioc.liturgical.synch.git.models.GitStatus;
import org.ocmc.ioc.liturgical.utils.ApacheFileUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utilities for accessing Git repositories.  
 * Note that the Github api (api.github.com) has rate limits:
 * 60 requests per hour per account for unauthenticated requests
 * 5000 requests per hour per account for authenticated.
 * 
 * 
 * @author mac002
 *
 */
public class JGitUtils {
	private static final Logger logger = LoggerFactory.getLogger(JGitUtils.class);
	private static String emptyInitialTreeObjectId = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

	/**
	 * Finds Git directories that have a .synch file
	 * @param root path to the directory from which to start the search
	 * @return list of paths to git repositories that have a .synch file.
	 */
	public static List<String> getGitDirectories(File root) {
		List<String> result = new ArrayList<String>();
		for (String fileName : root.list()) {
			try {
				File file = new File(root.getCanonicalFile() + "/" + fileName);
				if (file.isDirectory()) {
					if (file.getName().equals(".git")) {
						File synchFile = new File(file.getParent() + "/.synch");
						if (synchFile.exists()) {
							result.add(file.getCanonicalPath());
						}
						return result;
					} else {
						result = searchSubDirectories(file, result);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public static List<String> searchSubDirectories(File root, List<String> resultList) {
		for (String fileName : root.list()) {
			try {
				File file = new File(root.getCanonicalFile() + "/" + fileName);
				if (file.isDirectory()) {
					if (file.getName().equals(".git")) {
						File synchFile = new File(file.getParent() + "/.synch");
						if (synchFile.exists()) {
							resultList.add(file.getCanonicalPath());
						}
						return resultList;
					} else {
						resultList = searchSubDirectories(file, resultList);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return resultList;
	}
	
	
	/**
	 * 1. Need GithubRepositories.
	 * 2. Pass it to JGitUtils.updateAresGithubRepos.
	 * 3. For each local repo, load it into the database
	 */

	
	
	public static GitStatus updateAresGithubRepos(
			GithubRepositories repos
			, String baseDir
			) {
		GitStatus status = new GitStatus();
		File repoDir = null;
		for (GithubRepo repo : repos.getRepos()) {
			try {
				Path path = Paths.get(baseDir, repo.name);
				repoDir = path.toFile();
				if (repoDir.exists()) {
					pullRepository(repo, path.toString());
					if (repo.lastGitToDbSynchCommitId.equals(repo.lastGitToDbFetchCommitId)) {
						status.addUnchanged(repo);
					} else {
						status.addUpdated(repo);
					}
				} else {
					cloneRepository(repo, baseDir);
					status.addCloned(repo);
				}
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
				status.addError(repo);
			}
		}
		return status;
	}
	
	public static GithubRepo pullRepository(GithubRepo repo, String repoPath) {
		Git git = null;
		try {
		    Repository repository = new FileRepository(repoPath.endsWith("git") ? repoPath : repoPath + "/.git");
		    git = new Git(repository);
		    git.pull().call();
		    setRepoFetchInfo(repo, repository.getDirectory().getPath());
		    git.clean().setCleanDirectories(true).call();
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		if (git != null) {
			git.close();
		}
		return repo;
	}

	public static GithubRepo cloneRepository(GithubRepo repo, String baseDir) {
		try {
			String fileName = FilenameUtils.getName(repo.url);
			if (fileName.endsWith(".git")) {
				fileName = fileName.substring(0, fileName.length()-4);
			}
			Path path = Paths.get(baseDir, fileName);
			File repoDir = path.toFile();
			Git git = Git.cloneRepository()
	                .setURI(repo.url)
	                .setDirectory(repoDir)
	                .setCloneAllBranches(true)
	                .call();
			git.branchCreate().setName("master")
					.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
						.setStartPoint("origin/master").setForce(true).call();
			StoredConfig config = git.getRepository().getConfig();
			config.setString("remote", "origin", "url", repo.url);
			config.save();
			git.close();
		    setRepoFetchInfo(repo, git.getRepository().getDirectory().getPath());
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return repo;
	}
	
	private static void printGitRepo(GithubRepo repo) {
		repo.setPrettyPrint(true);
		System.out.println(repo.toJsonString());
	}
	
	/**
	 * This issues a git reset --hard origin/master and git pull origin
	 * @param repoPath path to the repository
	 */
	public static void resetAndPullLatest(String repoPath) {
		Git git = null;
		try {
		    Repository repository = new FileRepository(repoPath.endsWith("git") ? repoPath : repoPath + "/.git");
		    git = new Git(repository);
		    git.reset().setMode(ResetType.HARD).setRef("origin/master").call();
		    git.pull().setRemote("origin");
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		if (git != null) {
			git.close();
		}
	}
	// git diff 4b825dc642cb6eb9a060e54bf8d69288fbee4904 HEAD
	
	public static List<GitDiffEntry> compare(String repoPath, String lastCommitId) {
		List<GitDiffEntry> result = new ArrayList<GitDiffEntry>();
		try {
		    Repository repository = new FileRepository(repoPath.endsWith("git") ? repoPath : repoPath + "/.git");
            ObjectId currentHeadTree = repository.resolve("HEAD^{tree}");
 	        ObjectId lastCommit = repository.resolve(lastCommitId);
	        RevWalk revWalk = new RevWalk (repository);
	        RevCommit commit = revWalk.parseCommit(lastCommit);
            ObjectId oldHeadTree = commit.getTree().getId();
            // Prepare means to show actual diffs
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter ( out );
            // prepare the two iterators to compute the diff between
    		try (ObjectReader reader = repository.newObjectReader()) {
        		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        		oldTreeIter.reset(reader, oldHeadTree);
        		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        		newTreeIter.reset(reader, currentHeadTree);
        		// finally get the list of changed files
        		try (Git git = new Git(repository)) {
        			df.setRepository(git.getRepository());
                    List<DiffEntry> diffs= git.diff()
            		                    .setNewTree(newTreeIter)
            		                    .setOldTree(oldTreeIter)
            		                    .call();
                    for (DiffEntry entry : diffs) {
                    	df.format(entry);
                    	String diffText = out.toString("UTF-8");
                    	out.reset();
                    	GitDiffEntry myEntry = new GitDiffEntry(commit, entry);
                    	int diffCount = myEntry.setDiffs(diffText);
                    	if (diffCount > 0) {
                        	result.add(myEntry);
                    	}
                    }
                    // now see what files changed
                    diffs= git.diff()
		                    .setShowNameAndStatusOnly(true)
		                    .call();
		        for (DiffEntry entry : diffs) {
		        	df.format(entry);
		        	String diffText = out.toString("UTF-8");
		        	out.reset();
		        	GitDiffEntry myEntry = new GitDiffEntry(commit, entry);
		        	int diffCount = myEntry.setDiffs(diffText);
		        	if (diffCount > 0) {
		            	result.add(myEntry);
		        	}
		        }
        		}
    		}
    		df.close();
    		revWalk.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static List<GitDiffEntry> compareEmptyTree(String repoPath) {
		List<GitDiffEntry> result = new ArrayList<GitDiffEntry>();
		try {
		    Repository repository = new FileRepository(repoPath.endsWith("git") ? repoPath : repoPath + "/.git");
            ObjectId currentHeadTree = repository.resolve("HEAD^{tree}");
	        RevWalk revWalk = new RevWalk (repository);
	        ObjectId headCommitId = JGitUtils.getHeadCommitId(repoPath);
	        RevCommit commit = revWalk.parseCommit(headCommitId);
            // Prepare means to show actual diffs
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter ( out );
            // prepare the two iterators to compute the diff between
    		try (ObjectReader reader = repository.newObjectReader()) {
    			AbstractTreeIterator oldTreeIter = new EmptyTreeIterator();
        		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        		newTreeIter.reset(reader, currentHeadTree);
        		// finally get the list of changed files
        		try (Git git = new Git(repository)) {
        			df.setRepository(git.getRepository());
                    List<DiffEntry> diffs= git.diff()
            		                    .setNewTree(newTreeIter)
            		                    .setOldTree(oldTreeIter)
            		                    .call();
                    for (DiffEntry entry : diffs) {
                    	df.format(entry);
                    	String diffText = out.toString("UTF-8");
                    	out.reset();
                    	GitDiffEntry myEntry = new GitDiffEntry(commit, entry);
                    	int diffCount = myEntry.setDiffs(diffText);
                    	if (diffCount > 0) {
                        	result.add(myEntry);
                    	}
                    }
        		}
    		}
    		revWalk.close();
    		df.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Status getStatus(String repoPath) {
		Status result = null;
		Git git = null;
		try {
		     Repository repository = new FileRepository(repoPath.endsWith("git") ? repoPath : repoPath + "/.git");
		     git = new Git(repository);
		     result = git.status().call();
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		} 
		if (git != null) {
			git.close();
		}
		return result;
	}
	
	public static Set<String> getAdded(String repoPath) {
		Set<String> result = null;
		Status status = getStatus(repoPath);
		result = status.getAdded();
		return result;
	}
	
	public static String gitTimeToTimestamp(int git) {
		String result = "";
		Instant instant = Instant.ofEpochSecond(git);
		result = instant.toString();
		return result;
	}
	
	public static String dumpDiffEntry(DiffEntry entry, DiffFormatter df) {
		String result = "";
	    System.out.println(MessageFormat.format("({0} {1} {2}", entry.getChangeType().name(), entry.getNewMode().getBits(), entry.getNewPath()));
		return result;
	}
		
	public static RevCommit getRevCommit(String repPath, String targetCommitId) {
		RevCommit result = null;
		Repository repository;
		Git git = null;
		try {
			repository = new FileRepository(repPath.endsWith("git") ? repPath : repPath + "/.git");
			git = new Git(repository);
			String treeName = "refs/heads/master"; // tag or branch
			for (RevCommit commit : git.log().add(repository.resolve(treeName)).call()) {
				if (commit.getName().equals(targetCommitId)) {
					result = commit;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (git != null) {
			git.close();
		}
		return result;
	}
	
	public static boolean getMasterCommits(String repPath) {
		Repository repository;
		Git git = null;
		try {
			repository = new FileRepository(repPath.endsWith("git") ? repPath : repPath + "/.git");
			git = new Git(repository);
			String treeName = "refs/heads/master"; // tag or branch
			ObjectId id = repository.resolve(treeName);
			LogCommand logCommand = git.log().add(id);
			for (RevCommit commit : logCommand.call()) {
				String commitId = commit.getName();
				String commitTreeId = commit.getTree().getId().getName();
				int commitTime = commit.getCommitTime();
			    Date date = new Date((long) commitTime * 1000);
			    String dateString = date.toString();
			    if (dateString.contains("2017")) {
				    System.out.println(
				    		commitId 
				    		+ " : " 
				    		+ commitTime 
				    		+ " : " 
				    		+ dateString 
				    		+ " : " 
				    		+ commitTreeId
				    		);
			    }
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		if (git != null) {
			git.close();
		}
		return true;
	}
	
	public static String getHeadCommitName(String repoPath) {
		String result = "";
		ObjectId id = getHeadCommitId(repoPath);
		if (id != null) {
			result = id.getName();
		}
		return result;
	}
	
	private static void setRepoFetchInfo(GithubRepo repo, String path) {
		repo.setLastGitToDbFetchTime(Instant.now().toString());
		repo.setLastGitToDbFetchCommitId(getHeadCommitName(path));
		repo.setLocalRepoPath(path);
	}
	
	public static GithubRepositories getRepositories(String path) {
		GithubRepositories githubRepos = new GithubRepositories();
		try {
			for (File f :  ApacheFileUtils.listFilesAndDirs(
					new File(path)
					, new NotFileFilter(TrueFileFilter.INSTANCE)
					, DirectoryFileFilter.DIRECTORY)) {
				if (f.getName().startsWith(".git")) {
					Repository repository;
					try {
						repository = new FileRepository(f.getAbsoluteFile());
						String url = repository.getConfig().getString( "remote", "origin", "url" );
						String [] parts = url.split("/");
						String account = parts[parts.length-2];
						String repoName = parts[parts.length-1];
						repoName = repoName.substring(0, repoName.length()-4);
						GithubRepo repo = new GithubRepo(account, repoName);
						repo.setUrl(url);
						repo.setLocalRepoPath(f.getAbsolutePath());
						githubRepos.addRepo(repo);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			updateAresGithubRepos(githubRepos, path);
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return githubRepos;
	}

	public static ObjectId getHeadCommitId(String repPath) {
        ObjectId result = null;
		Repository repository;
		try {
			repository = new FileRepository(repPath.endsWith("git") ? repPath : repPath + "/.git");
            result = repository.resolve("HEAD^{commit}");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static ObjectId getHeadTreeId(String repPath) {
        ObjectId result = null;
		Repository repository;
		try {
			repository = new FileRepository(repPath.endsWith("git") ? repPath : repPath + "/.git");
            result = repository.resolve("HEAD^{tree}");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static String getEmptyTreeId(String repPath) {
        String result = null;
		Repository repository;
		try {
			repository = new FileRepository(repPath.endsWith("git") ? repPath : repPath + "/.git");
            result = repository.resolve(emptyInitialTreeObjectId).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
