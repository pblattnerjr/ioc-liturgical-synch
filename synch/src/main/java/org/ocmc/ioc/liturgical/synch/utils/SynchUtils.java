package org.ocmc.ioc.liturgical.synch.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.ocmc.ioc.liturgical.schemas.constants.VISIBILITY;
import org.ocmc.ioc.liturgical.schemas.exceptions.BadIdException;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresPushTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresPushTransaction.TYPES;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.synch.git.GitUtils;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffEntry;
import org.ocmc.ioc.liturgical.synch.git.models.GitDiffLibraryLine;
import org.ocmc.ioc.liturgical.synch.git.models.SynchData;
import org.ocmc.ioc.liturgical.synch.git.models.SynchStatus;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.ocmc.ioc.liturgical.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SynchUtils {
	private static final Logger logger = LoggerFactory.getLogger(SynchUtils.class);

	/**
	 * 
	 * @param githubRepo the repo info
	 * @param printDetails true if want details printed
	 * @return transactions for all the changes that occurred in a Git repository.

	 */
	public static SynchData getRepoSynchData(
			GithubRepo githubRepo
			, boolean printDetails
			) {
		SynchData result = new SynchData(githubRepo);
		SynchStatus synchStatus = new SynchStatus();
		Map<String,GitDiffLibraryLine> createMap = new TreeMap<String,GitDiffLibraryLine>();
		Map<String,GitDiffLibraryLine> updateMap = new TreeMap<String,GitDiffLibraryLine>();
		Map<String,GitDiffLibraryLine> deleteMap = new TreeMap<String,GitDiffLibraryLine>();
		Map<String,GitDiffLibraryLine> renameMap = new TreeMap<String,GitDiffLibraryLine>();
		Map<String,GitDiffLibraryLine> unknownMap = new TreeMap<String,GitDiffLibraryLine>();
		List<String> deletedFileList = new ArrayList<String>();
		List<AresPushTransaction> transactions = new ArrayList<AresPushTransaction>();

		List<GitDiffEntry> entries = null;
		if (githubRepo.getLastSynchCommitId() == null || githubRepo.getLastSynchCommitId().length() == 0) {
			entries = GitUtils.compareEmptyTree(githubRepo.lastFetchLocalPath);
		} else {
			entries = GitUtils.compare(githubRepo.getLastFetchLocalPath(), githubRepo.getLastSynchCommitId());
		}
		for (GitDiffEntry entry : entries) {
			for (String filename : entry.getDeletedFiles()) {
				String [] parts = getAresFileParts(filename);
				if (parts != null) {
					if (parts.length == 2) {
						String libraryTopic = "--- " + parts[1].toLowerCase() + "~" + parts[0];
						if (! deletedFileList.contains(libraryTopic)) {
							deletedFileList.add(libraryTopic);
						}
					} else {
						deletedFileList.add(filename);
					}
				}
			}
	    	for (Entry<String, List<GitDiffLibraryLine>> keySet : entry.getChangeIterator()) {
	    		try {
		    		List<GitDiffLibraryLine> list = keySet.getValue();
		    		switch (list.size()) {
		    		case (1): {
		    			GitDiffLibraryLine first = (GitDiffLibraryLine) list.get(0);
		    			if (first.isPlus()) {
		    				if (first.hasRenamedKey()) {
			    				renameMap.put(first.getKey(), first);
		    				} else {
			    				createMap.put(first.getKey(), first);
		    				}
		    			} else {
		    				deleteMap.put(first.getKey(), first);
		    			}
		    			break;
		    		}
		    		case (2): {
		    			GitDiffLibraryLine first = (GitDiffLibraryLine) list.get(0);
		    			GitDiffLibraryLine second = (GitDiffLibraryLine) list.get(1);
		    			if (first.isPlus()) {
		    				if (second.isPlus()) {
			    				unknownMap.put(first.getKey(), first);
			    				unknownMap.put(second.getKey(), second);
		    				} else {
			    				unknownMap.put(first.getKey(), first);
			    				unknownMap.put(second.getKey(), second);
		    				}
		    			} else {
		    				if (second.isPlus()) {
			    				updateMap.put(second.getKey(), second);
		    				} else {
			    				unknownMap.put(first.getKey(), first);
			    				unknownMap.put(second.getKey(), second);
		    				}
		    			}
		    			break;
		    		}
		    		default: {
		    			logger.error("No changes in change set");
		    		}
		    		}
		    		if (printDetails) {
		    			for (GitDiffLibraryLine line : keySet.getValue()) {
		    				System.out.println(line.toSummary());
		    			}
		    		}
	    		} catch (Exception e) {
	    			ErrorUtils.report(logger, e);
	    		}
	    	}
	    	// remove false deletes.  A false delete exists because a rename is a delete followed by an add
	    	for (GitDiffLibraryLine line : renameMap.values()) {
	    		if (deleteMap.containsKey(line.getRenameKeyFrom())) {
	    			deleteMap.remove(line.getRenameKeyFrom());
	    		}
	    	}
		}
		synchStatus.setCreates(createMap.size());
		synchStatus.setRenames(renameMap.size());
		synchStatus.setUpdates(updateMap.size());
		synchStatus.setDeletes(deleteMap.size());
		synchStatus.setFileDeletes(deletedFileList.size());

		// Convert the information into a set of AresTransactions.
		String hostName = GeneralUtils.getHostName();
		String macAddress = GeneralUtils.getMacAddress();

		int counter = 0;
		
		for (GitDiffLibraryLine line : createMap.values()) {
			counter++;
			transactions.add(getAresPushTransaction(
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
			transactions.add(getAresPushTransaction(
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
			transactions.add(getAresPushTransaction(
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
					getAresPushTransaction(
							line
							, hostName
							, macAddress
							, TYPES.DELETE_KEY_VALUE
							, counter
							)
					);
		}

		for (AresPushTransaction trans : transactions) {
			if (printDetails) {
				trans.setPrettyPrint(true);
				System.out.println(trans.toJsonString());
			}
		}
		if (printDetails) {
			logger.info("Synch summary: " + synchStatus.toSummary());
		}
		result.setStatus(synchStatus);
		result.setTransactions(transactions);
		return result;
	}

	private static AresPushTransaction getAresPushTransaction(
			GitDiffLibraryLine line
			, String hostName
			, String macAddress
			, TYPES type
			, int counter
			) {
		AresPushTransaction trans = null;
		try {
			trans = new AresPushTransaction(
					hostName
					, macAddress
					, line.getTimestamp() + GeneralUtils.padNumber("s", 4, counter)
					);
			trans.setFromLibrary(line.getRenamedFromLibrary().length() > 0 ? line.getRenamedFromLibrary() : line.getDomain());
			trans.setFromTopic(line.getRenamedFromTopic().length() > 0 ? line.getRenamedFromTopic() : line.getTopic());
			trans.setFromKey(line.getRenamedFromKey().length() > 0 ? line.getRenamedFromKey() : line.getKey());
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
	
	/**
	 * For a given ares filename, return the prefix and domain parts
	 * @param file - ares filename
	 * @return array with prefix in [0] and domain in [1]
	 */
	private static String[] getAresFileParts(
			String file
			, String domainDelimiter
			, boolean countryToLowerCase
			, boolean dropFileExtension
			) {
		String [] theParts;
		String [] result;
		try {
			theParts = file.split("_");
			result = new String[2];
			if (theParts.length ==4) {
				result[0] = theParts[0];
				result[1] = (theParts[1] + domainDelimiter + theParts[2] + domainDelimiter);
				theParts[3] = theParts[3].replace(".tsf", "");
				if (countryToLowerCase) {
					theParts[3] = theParts[3].toLowerCase();
				}
				if (dropFileExtension) {
					theParts[3] = theParts[3].split("\\.")[0];
				}
				result[1] = result[1]  + theParts[3];
			} else {
				result = null;
			}
		} catch (Exception e) {
			result = null;
		}
		return result;
	}
	
	private static String[] getAresFileParts(String file) {
		return getAresFileParts(file, "_", false, true);
	}


}
