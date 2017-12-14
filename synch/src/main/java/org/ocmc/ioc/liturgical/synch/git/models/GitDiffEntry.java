package org.ocmc.ioc.liturgical.synch.git.models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.ocmc.ioc.liturgical.synch.git.JGitUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitDiffEntry {
	private static final Logger logger = LoggerFactory.getLogger(GitDiffEntry.class);
	private DiffEntry entry = null;
    private RevCommit commit = null;
    private String timestamp = "";
    private String oldTopic = "";
    private String newTopic = "";
    private String oldLibrary = "";
    private String newLibrary = "";
    private String diffs = "";
    private List<String> deletedFiles = new ArrayList<String>();
    private Map<String, List<GitDiffLibraryLine>> changeMap = new TreeMap<String,List<GitDiffLibraryLine>>();

	public GitDiffEntry (RevCommit commit, DiffEntry entry) {
		this.commit = commit;
		this.entry = entry;
		this.setTopicsAndLibraries();
		this.timestamp = JGitUtils.gitTimeToTimestamp(this.commit.getCommitTime());
	}
	
	private void setTopicsAndLibraries() {
		try {
			String path = this.entry.getOldPath();
			if (path.length() == 0) {
				path = this.entry.getNewPath();
			}
			if (path.length() > 0) {
				List<String> libraryTopic = this.getLibraryTopic(this.entry.getOldPath());
				if (libraryTopic.size() == 2) {
						this.oldLibrary = libraryTopic.get(0);
						this.oldTopic = libraryTopic.get(1);
				}
				libraryTopic = this.getLibraryTopic(this.entry.getNewPath());
				if (libraryTopic.size() == 2) {
					this.newLibrary = libraryTopic.get(0);
					this.newTopic = libraryTopic.get(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setNewTopicAndLibrary(String filename) {
		try {
			if (filename.length() > 0) {
				List<String> libraryTopic = this.getLibraryTopic(filename);
				if (libraryTopic.size() == 2) {
					this.newLibrary = libraryTopic.get(0);
					this.newTopic = libraryTopic.get(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setOldTopicAndLibrary(String filename) {
		try {
			if (filename.length() > 0) {
				List<String> libraryTopic = this.getLibraryTopic(filename);
				if (libraryTopic.size() == 2) {
					this.oldLibrary = libraryTopic.get(0);
					this.oldTopic = libraryTopic.get(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> getLibraryTopic(String path) {
		List<String> result = new ArrayList<String>();
		try {
			File file = new File(path);
			String filename = file.getName();
			StringBuffer sb  = new StringBuffer();
			String [] parts = filename.split("_");
			if (parts.length == 4) { // all ok
				sb.append(parts[1]);
				sb.append("_");
				sb.append(parts[2]);
				sb.append("_");
				String [] subparts = parts[3].split("\\.");
				sb.append(subparts[0]);
				result.add(sb.toString());
				result.add(parts[0]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public Set<Entry<String, List<GitDiffLibraryLine>>> getChangeIterator() {
		return this.changeMap.entrySet();
	}

	public DiffEntry getEntry() {
		return entry;
	}

	public void setEntry(DiffEntry entry) {
		this.entry = entry;
	}

	public RevCommit getCommit() {
		return commit;
	}

	public void setCommit(RevCommit commit) {
		this.commit = commit;
	}

	public String getOldTopic() {
		return oldTopic;
	}

	public void setOldTopic(String oldTopic) {
		this.oldTopic = oldTopic;
	}

	public String getNewTopic() {
		return newTopic;
	}

	public void setNewTopic(String newTopic) {
		this.newTopic = newTopic;
	}

	public String getOldLibrary() {
		return oldLibrary;
	}

	public void setOldLibrary(String oldLibrary) {
		this.oldLibrary = oldLibrary;
	}

	public String getNewLibrary() {
		return newLibrary;
	}

	public void setNewLibrary(String newLibrary) {
		this.newLibrary = newLibrary;
	}

	public String getDiffs() {
		return diffs;
	}

	public int setDiffs(String diffs) {
		this.diffs = diffs;
	    return this.processDiffs();
	}
	
	private int processDiffs() {
		String [] diffEntries = this.diffs.split("\n");
		int count = 0;
		for (String diff : diffEntries) {
			count++;
			String line = "";
			GitDiffLibraryLine libLine = null;
			if (diff.startsWith("+++") || diff.startsWith("---")) {
				if (diff.startsWith("+++")) {
					// TODO
				} else {
					deletedFiles.add(diff.substring(6));
				}
			} else if (diff.startsWith("++") || diff.startsWith("--")) {
				if (diff.startsWith("+++ a/")) {
					System.out.println(diff);
				} else if (diff.startsWith("+++ b/")) {
					if (this.newLibrary.length() == 0) {
						this.setNewTopicAndLibrary(diff.substring(6));
					}
				}
			} else if (diff.startsWith("+") || diff.startsWith("-")) {
				line = diff.substring(1);
				try {
					libLine = new GitDiffLibraryLine(Integer.toString(count),line);
					libLine.setTimestamp(timestamp);
					libLine.setPlus(diff.startsWith("+"));
					if (libLine.hasCommentAfterValue) {
						if (libLine.getComment().contains("~")) {
							String [] parts = libLine.getComment().split("~");
							if (parts.length > 1) {
								libLine.setRenameKeyFrom(parts[1]);
							}
						}
					}
					if (libLine.isPlus()) {
						libLine.setDomain(this.newLibrary.toLowerCase());
						libLine.setTopic(this.newTopic);
					} else {
						libLine.setDomain(this.oldLibrary.toLowerCase());
						libLine.setTopic(this.oldTopic);
					}
				} catch (Exception e) {
					ErrorUtils.report(logger, e);
					libLine = null;
				}
			} else if (diff.startsWith("@@")){
				// ignore but if want in the future, this is info about the lines affected
			} else if (diff.startsWith("diff --git a/")){ // this will give you the two files being compared
				String [] parts = diff.split("diff --git a/");
				parts = parts[1].split(" b/");
				if (parts[0].contains("_")) {
					this.setOldTopicAndLibrary(parts[0]);
					this.setNewTopicAndLibrary(parts[1]);
				}
			}
			if (libLine != null && libLine.isSimpleKeyValue) {
				List<GitDiffLibraryLine> theSet = new ArrayList<GitDiffLibraryLine>();
				if (this.changeMap.containsKey(libLine.getKey())) {
					theSet = this.changeMap.get(libLine.getKey());
				}
				theSet.add(libLine);
				this.changeMap.put(libLine.getKey(), theSet);
			}
		}
		return this.changeMap.size();
	}
	

	public Map<String, List<GitDiffLibraryLine>> getChangeMap() {
		return changeMap;
	}

	public void setChangeMap(Map<String, List<GitDiffLibraryLine>> changeMap) {
		this.changeMap = changeMap;
	}

	public List<String> getDeletedFiles() {
		return deletedFiles;
	}

	public void setDeletedFiles(List<String> deletedFiles) {
		this.deletedFiles = deletedFiles;
	}
}
