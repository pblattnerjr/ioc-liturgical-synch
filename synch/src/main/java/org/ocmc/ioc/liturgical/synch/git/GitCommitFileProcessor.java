package org.ocmc.ioc.liturgical.synch.git;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.ocmc.ioc.liturgical.schemas.constants.Constants;
import org.ocmc.ioc.liturgical.schemas.constants.VISIBILITY;
import org.ocmc.ioc.liturgical.schemas.exceptions.BadIdException;
import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresTransaction.SOURCES;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresTransaction.TYPES;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffTuple;
import org.ocmc.ioc.liturgical.synch.git.models.SynchStatus;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffLibraryLine;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFile;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFileChanged;
import org.ocmc.ioc.liturgical.synch.git.models.github.CommitFileRenamed;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.ocmc.ioc.liturgical.utils.FileUtils;
import org.ocmc.ioc.liturgical.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * Takes the json response from Github regarding the diff between two commits.
 * and converts it to a list of AresPushTransaction.  These can be used
 * elsewhere to convert them to database transactions and send them
 * to the synch server. 
 * @author mac002
 *
 */
public class GitCommitFileProcessor {
	private static final Logger logger = LoggerFactory.getLogger(GitCommitFileProcessor.class);
	public enum STATUSES {ADDED, MODIFIED, REMOVED, RENAMED, UNKNOWN};
	private Map<String,GitDiffLibraryLine> createMap = new TreeMap<String,GitDiffLibraryLine>();
	private Map<String,GitDiffLibraryLine> updateMap = new TreeMap<String,GitDiffLibraryLine>();
	private Map<String,GitDiffLibraryLine> deleteMap = new TreeMap<String,GitDiffLibraryLine>();
	private Map<String,GitDiffLibraryLine> renameMap = new TreeMap<String,GitDiffLibraryLine>();
	private Map<String,GitDiffLibraryLine> unknownMap = new TreeMap<String,GitDiffLibraryLine>();
	private List<AresTransaction> transactions = new ArrayList<AresTransaction>();
	private CommitFileChanged commitFile = null;
	private CommitFileRenamed renamedFile = null;
	private String fromCommitId = "";
	private String toCommitId = "";
	private String committerName = "";
	private String committerDate = "";
	private String filenameFrom = "";
	private String filenameTo = "";
	private String libraryFrom = "";
	private String topicFrom = "";
	private String libraryTo = "";
	private String topicTo = "";
	private STATUSES status = STATUSES.UNKNOWN;
	private List<GitDiffLibraryLine> gitDiffLibraryLines = new ArrayList<GitDiffLibraryLine>();
	private final String hostName = GeneralUtils.getHostName();
	private final String macAddress = GeneralUtils.getMacAddress();

	
	/**
	 * Constructor.
	 * 
	 * Git reports four statuses for a file: added, modified, removed, or renamed.
	 * The Json information for these four statuses fall into two categories:
	 *      those with a patch and those without.
	 * The one without a patch occurs when the file has been renamed.
	 * In correspondance with this, there are two subclasses of CommitFile:
	 * CommitFileChanged and CommitFileRenamed.  
	 * The CommitFileChanged has a patch and handles the Git statuses
	 * of added, modified, and removed.
	 * 
	 * @param json from Github that contains the diff between two revisions
	 * @param committerName name of person who made the commit
	 * @param committerDate date of the commit
	 */
	public GitCommitFileProcessor(
			JsonElement json
			, String committerName
			, String committerDate
			, String fromCommitId
			, String toCommitId
			) {
		this.committerName = committerName;
		this.committerDate = committerDate;
		this.fromCommitId = fromCommitId;
		this.toCommitId = toCommitId;
		CommitFile abstractFile = new CommitFile();
		try {
			abstractFile = AbstractModel.gson.fromJson(json, CommitFile.class);
			String [] filenameFromParts = new String[2];
			String [] filenameToParts = new String[2];
			this.filenameTo = FilenameUtils.getBaseName(abstractFile.getFilename());
			filenameToParts = FileUtils.getAresFileParts(
					filenameTo
					, Constants.DOMAIN_DELIMITER
					);
			this.libraryTo = filenameToParts[1];
			this.topicTo = filenameToParts[0];
			if (abstractFile.fileWasRenamed()) {
				this.renamedFile = AbstractModel.gson.fromJson(json, CommitFileRenamed.class);
				this.filenameFrom = FilenameUtils.getBaseName(renamedFile.getPrevious_filename());
				filenameFromParts = FileUtils.getAresFileParts(filenameFrom, Constants.DOMAIN_DELIMITER);
				this.libraryFrom = filenameFromParts[1];
				this.topicFrom = filenameFromParts[0];
			} else {
				this.commitFile = AbstractModel.gson.fromJson(json, CommitFileChanged.class);
				if (this.commitFile.getStatus().startsWith("add")) {
					this.filenameFrom = "";
				} else {
					this.filenameFrom = filenameTo;
					this.libraryFrom = filenameToParts[1];
					this.topicFrom = filenameToParts[0];
				}
			}
			switch (abstractFile.status) { // this is the status of the file itself
			case ("added"): {
				this.status = STATUSES.ADDED;
				break;
			}
			case ("modified"): {
				this.status = STATUSES.MODIFIED;
				break;
			}
			case ("removed"): {
				this.status = STATUSES.REMOVED;
				break;
			}
			case ("renamed"): {
				this.status = STATUSES.RENAMED;
				break;
			}
			default: {
				break;
			}
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
	
	}
	
	public List<AresTransaction> process() {
		if (this.status == STATUSES.RENAMED) {
			
		} else {
			this.processPatchWithFromTo();
			this.processMaps();
		}
		return this.transactions;
	}

	public Map<String, GitDiffLibraryLine> getCreateMap() {
		return createMap;
	}

	public void setCreateMap(Map<String, GitDiffLibraryLine> createMap) {
		this.createMap = createMap;
	}

	public Map<String, GitDiffLibraryLine> getUpdateMap() {
		return updateMap;
	}

	public void setUpdateMap(Map<String, GitDiffLibraryLine> updateMap) {
		this.updateMap = updateMap;
	}

	public Map<String, GitDiffLibraryLine> getDeleteMap() {
		return deleteMap;
	}

	public void setDeleteMap(Map<String, GitDiffLibraryLine> deleteMap) {
		this.deleteMap = deleteMap;
	}

	public Map<String, GitDiffLibraryLine> getRenameMap() {
		return renameMap;
	}

	public void setRenameMap(Map<String, GitDiffLibraryLine> renameMap) {
		this.renameMap = renameMap;
	}

	public Map<String, GitDiffLibraryLine> getUnknownMap() {
		return unknownMap;
	}

	public void setUnknownMap(Map<String, GitDiffLibraryLine> unknownMap) {
		this.unknownMap = unknownMap;
	}

	public List<AresTransaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<AresTransaction> transactions) {
		this.transactions = transactions;
	}

	public CommitFileChanged getCommitFile() {
		return commitFile;
	}

	public void setCommitFile(CommitFileChanged commitFile) {
		this.commitFile = commitFile;
	}

	public CommitFileRenamed getRenamedFile() {
		return renamedFile;
	}

	public void setRenamedFile(CommitFileRenamed renamedFile) {
		this.renamedFile = renamedFile;
	}

	public String getCommitterName() {
		return committerName;
	}

	public void setCommitterName(String committerName) {
		this.committerName = committerName;
	}

	public String getCommitterDate() {
		return committerDate;
	}

	public void setCommitterDate(String committerDate) {
		this.committerDate = committerDate;
	}

	public String getFilenameFrom() {
		return filenameFrom;
	}

	public void setFilenameFrom(String filenameFrom) {
		this.filenameFrom = filenameFrom;
	}

	public String getFilenameTo() {
		return filenameTo;
	}

	public void setFilenameTo(String filenameTo) {
		this.filenameTo = filenameTo;
	}

	public void printGitDiffLibraryLines() {
		if (this.commitFile != null) {
			System.out.println(this.commitFile.getPatch());
		}
		for (GitDiffLibraryLine line : this.gitDiffLibraryLines) {
			line.setPrettyPrint(true);
			System.out.println(line.toJsonString());
		}
	}
	
	/**
	 * Processes the patch. The end result will be
	 * that the various maps are populated.
	 */
	private void processPatchWithFromTo() {
		String [] diffEntries = this.commitFile.getPatch().split("\n");
		Map<String,GitDiffTuple> map = new TreeMap<String,GitDiffTuple>();
		int count = 0;
		for (String diff : diffEntries) {
			count++;
			String line = "";
			GitDiffLibraryLine libLine = null;
			if (diff.length() > 1 && (diff.startsWith("+") || diff.startsWith("-"))) {
				if (diff.startsWith("+A_Resource_Whose_Name =") || diff.startsWith("-A_Resource_Whose_Name =")) {
					// ignore
				} else {
					line = diff.substring(1);
					try {
						libLine = new GitDiffLibraryLine(Integer.toString(count),line);
						if (libLine.hasError) {
							logger.info(libLine.domain + "~" + libLine.topic + "~" + libLine.key + " ares line has error");
						} else {
							libLine.setTimestamp(committerDate);
							libLine.setWho(committerName);
							libLine.setPlus(diff.startsWith("+"));
							if (libLine.hasCommentAfterValue) {
								if (libLine.getComment().contains("~")) {
									String [] parts = libLine.getComment().split("~");
									if (parts.length > 1) {
										libLine.setRenameKeyFrom(parts[1]);
									}
								}
							}
							GitDiffTuple tuple = new GitDiffTuple();
							if (map.containsKey(libLine.getKey())) {
								tuple = map.get(libLine.getKey());
							}
							if (libLine.isPlus()) {
								libLine.setDomain(this.libraryTo.toLowerCase());
								libLine.setTopic(this.topicTo);
								tuple.setPlus(libLine);
							} else {
								libLine.setDomain(this.libraryFrom.toLowerCase());
								libLine.setTopic(this.topicFrom);
								libLine.setFromKey(libLine.getKey());
								libLine.setFromTopic(libLine.getTopic());
								libLine.setFromLibrary(libLine.getDomain());
								libLine.setFromValue(libLine.getValue());
								tuple.setMinus(libLine);
							}
							map.put(libLine.getKey(), tuple);
						}
					} catch (Exception e) {
						ErrorUtils.report(logger, e);
						libLine = null;
					}
				}
			}
		}
		/**
		 * At this point, tuples that 
		 * have minus only - could be a delete or a rename
		 * have plus only - could be an add or a rename
		 * have both - are value changes
		 * 
		 */
		List<GitDiffTuple> updateList = new ArrayList<GitDiffTuple>();
		List<String> deleteList = new ArrayList<String>();
		
		for (GitDiffTuple tuple : map.values()) {
			if (tuple.getType() == GitDiffTuple.TYPES.RENAME) {
				String fromKey = tuple.getPlus().fromKey;
				if (map.containsKey(fromKey)) {
					GitDiffLibraryLine minus = map.get(fromKey).getMinus();
					tuple.setMinus(minus);
					GitDiffLibraryLine plus = tuple.getPlus();
					plus.setFromLibrary(minus.getDomain());
					plus.setFromTopic(minus.getTopic());
					plus.setFromKey(minus.getKey());
					plus.setFromValue(minus.getValue());
					plus.setFromComment(minus.getComment());
					tuple.setPlus(plus);
					updateList.add(tuple);
					deleteList.add(fromKey);
				}
			}
		}
		// get rid of the false deletes
		for (String key : deleteList) {
			map.remove(key);
		}
		for (GitDiffTuple tuple : updateList) {
			map.put(tuple.getPlus().getKey(), tuple);
		}
		for (GitDiffTuple tuple : map.values()) {
			GitDiffLibraryLine line = null;
			switch (tuple.getType()) {
			case CHANGE:
				line = tuple.getPlus();
				GitDiffLibraryLine minus = tuple.getMinus();
				line.setStatus(GitDiffLibraryLine.STATUSES.MODIFIED_VALUE);
				line.setFromLibrary(minus.getDomain());
				line.setFromTopic(minus.getTopic());
				line.setFromKey(minus.getKey());
				line.setFromValue(minus.getValue());
				line.setFromComment(minus.getComment());
				this.updateMap.put(line.getKey(), line);
				break;
			case MINUS_SINGLETON:
				line = tuple.getMinus();
				line.setStatus(GitDiffLibraryLine.STATUSES.REMOVED_KEY_VALUE);
				this.deleteMap.put(line.getKey(), line);
				break;
			case PLUS_SINGLETON:
				line = tuple.getPlus();
				line.setStatus(GitDiffLibraryLine.STATUSES.ADDED_KEY_VALUE);
				this.createMap.put(line.getKey(), line);
				break;
			case RENAME:
				line = tuple.getPlus();
				line.setStatus(GitDiffLibraryLine.STATUSES.RENAMED_KEY);
				this.renameMap.put(line.getKey(), line);
				break;
			case UNKNOWN:
				try {
					StringBuffer sbMessage = new StringBuffer();
					sbMessage.append("Could not classify diff ");
					if (tuple.getMinus() != null) {
						sbMessage.append(" minus = ");
						GitDiffLibraryLine less = tuple.getMinus();
						sbMessage .append(less.getFromLibrary());
						sbMessage .append(".");
						sbMessage .append(less.getFromTopic());
						sbMessage .append(".");
						sbMessage .append(less.getFromKey());
					}
					if (tuple.getPlus() != null) {
						GitDiffLibraryLine more = tuple.getPlus();
						sbMessage .append(more.getFromLibrary());
						sbMessage .append(".");
						sbMessage .append(more.getFromTopic());
						sbMessage .append(".");
						sbMessage .append(more.getFromKey());
					}
					ErrorUtils.error(logger, sbMessage.toString());
				} catch (Exception e) {
					ErrorUtils.error(logger, "Could not classify diff ");
				}
				break;
			}
		}
		this.dumpMaps();
	}
	
	private void processMaps() {

		// Convert the information into a set of AresTransactions.
		int counter = 0;
		
		for (GitDiffLibraryLine line : createMap.values()) {
			counter++;
			transactions.add(getAresToDbTransaction(
					line
					, hostName
					, macAddress
					, TYPES.ADD_KEY_VALUE
					, counter
					)
					);
		}
		for (GitDiffLibraryLine line : renameMap.values()) {
			counter++;
			transactions.add(getAresToDbTransaction(
					line
					, hostName
					, macAddress
					, TYPES.CHANGE_OF_KEY
					, counter
					)
				);
		}
		for (GitDiffLibraryLine line : updateMap.values()) {
			counter++;
			transactions.add(getAresToDbTransaction(
					line
					, hostName
					, macAddress
					, TYPES.CHANGE_OF_VALUE
					, counter
					)
			);
		}
		for (GitDiffLibraryLine line : deleteMap.values()) {
			counter++;
			transactions.add(
					getAresToDbTransaction(
							line
							, hostName
							, macAddress
							, TYPES.DELETE_KEY_VALUE
							, counter
							)
					);
		}

	}

	private AresTransaction getAresToDbTransaction(
			GitDiffLibraryLine line
			, String hostName
			, String macAddress
			, TYPES type
			, int counter
			) {
		AresTransaction trans = null;
		try {
			trans = new AresTransaction(
					hostName
					, macAddress
					, line.getTimestamp() + GeneralUtils.padNumber("s", 4, counter)
					);
			if (line.getKey().equals("EnTiKamino.notmetered")) {
				System.out.print("");
			}
			trans.setFromCommitId(this.fromCommitId);
			trans.setToCommitId(this.toCommitId);
			trans.setSource(SOURCES.GIT);
			trans.setRequestingUser(line.getWho());
			trans.setFromLibrary(line.getFromLibrary());
			trans.setFromTopic(line.getFromTopic());
			trans.setFromKey(line.getFromKey());
			trans.setFromValue(line.getFromValue());
			trans.setToLibrary(line.getDomain());
			trans.setToKey(line.getKey());
			trans.setToTopic(line.getTopic());
			trans.setToValue(line.getValue());
			trans.setToComment(line.getComment());
			trans.setType(type);
			trans.setVisibility(VISIBILITY.PRIVATE);
		} catch (BadIdException e) {
			ErrorUtils.report(logger, e);
		}
		return trans;
	}

	public AresTransaction getAresToDBTransactionForFileRename() {
		AresTransaction trans = null;
		try {
			trans = new AresTransaction(
					hostName
					, macAddress
					, committerDate + GeneralUtils.padNumber("s", 4, 1)
					);
			trans.setSource(SOURCES.GIT);
			trans.setFromCommitId(this.fromCommitId);
			trans.setToCommitId(this.toCommitId);
			trans.setRequestingUser(committerName);
			trans.setFromLibrary(libraryFrom);
			trans.setFromTopic(topicFrom);
			trans.setToLibrary(libraryTo);
			trans.setToTopic(topicTo);
			trans.setType(TYPES.CHANGE_OF_LIBRARY);
			trans.setVisibility(VISIBILITY.PRIVATE);
		} catch (BadIdException e) {
			ErrorUtils.report(logger, e);
		}
		return trans;
	}

	private void dumpMaps() {
		System.out.println("Create Map");
		for (Entry<String, GitDiffLibraryLine> line : this.createMap.entrySet()) {
			System.out.println("\t" + line.getKey() + " " + line.getValue().getStatus().name());
		}
		System.out.println("Delete Map");
		for (Entry<String, GitDiffLibraryLine> line : this.deleteMap.entrySet()) {
			System.out.println("\t" + line.getKey() + " " + line.getValue().getStatus().name());
		}
		System.out.println("Rename Map");
		for (Entry<String, GitDiffLibraryLine> line : this.renameMap.entrySet()) {
			System.out.println(
					"\t" 
					+ line.getKey() 
					+ " " 
					+ line.getValue().getStatus().name()
					+ " "
					+ line.getValue().getFromKey() 
					+ "."
	                + line.getValue().getFromValue()
					+ " => "
					+ line.getValue().getKey() 
					+ "."
	                + line.getValue().getValue()
					);
		}
		System.out.println("Update Map");
		for (Entry<String, GitDiffLibraryLine> line : this.updateMap.entrySet()) {
			System.out.println(
					"\t" + 
					line.getKey() 
					+ " " 
					+ line.getValue().getStatus().name()
					+ " "
					+ line.getValue().getFromValue()
					+ " => "
					+ line.getValue().getValue()
					);
		}
		System.out.println("Unknown Map");
		for (Entry<String, GitDiffLibraryLine> line : this.unknownMap.entrySet()) {
			System.out.println("\t" + line.getKey() + " " + line.getValue().getStatus().name());
		}
	}

	public String getLibraryFrom() {
		return libraryFrom;
	}

	public void setLibraryFrom(String libraryFrom) {
		this.libraryFrom = libraryFrom;
	}

	public String getTopicFrom() {
		return topicFrom;
	}

	public void setTopicFrom(String topicFrom) {
		this.topicFrom = topicFrom;
	}

	public String getLibraryTo() {
		return libraryTo;
	}

	public void setLibraryTo(String libraryTo) {
		this.libraryTo = libraryTo;
	}

	public String getTopicTo() {
		return topicTo;
	}

	public void setTopicTo(String topicTo) {
		this.topicTo = topicTo;
	}

	public STATUSES getStatus() {
		return status;
	}

	public void setStatus(STATUSES status) {
		this.status = status;
	}

	public List<GitDiffLibraryLine> getGitDiffLibraryLines() {
		return gitDiffLibraryLines;
	}

	public void setGitDiffLibraryLines(List<GitDiffLibraryLine> gitDiffLibraryLines) {
		this.gitDiffLibraryLines = gitDiffLibraryLines;
	}
}
