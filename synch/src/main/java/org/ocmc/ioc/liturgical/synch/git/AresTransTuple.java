package org.ocmc.ioc.liturgical.synch.git;

import java.time.Instant;

import org.ocmc.ioc.liturgical.schemas.models.synch.AresTransaction;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds two AresTransaction instances, one for from Git, one from DB
 * @author mac002
 *
 */
public class AresTransTuple {
	private static final Logger logger = LoggerFactory.getLogger(AresTransTuple.class);

	private AresTransaction fromGit = null;
	private AresTransaction fromDb = null;
	
	public boolean gitIsNewer() {
		boolean result = false;
		try {
			Instant gitTime = Instant.parse(fromGit.whenTransactionRecordedInThisDatabase);
			Instant dbTime = Instant.parse(fromDb.whenTransactionRecordedInThisDatabase);
			result = gitTime.isAfter(dbTime);
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public boolean valuesAndCommentsSame() {
		return fromGit.toValue.equals(fromDb.toValue) 
				&& fromGit.toComment.equals(fromDb.toComment);
	}
	public AresTransaction getFromGit() {
		return fromGit;
	}
	public void setFromGit(AresTransaction fromGit) {
		this.fromGit = fromGit;
	}
	public AresTransaction getFromDb() {
		return fromDb;
	}
	public void setFromDb(AresTransaction fromDb) {
		this.fromDb = fromDb;
	}

}
