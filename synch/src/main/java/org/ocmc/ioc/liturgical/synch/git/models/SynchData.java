package org.ocmc.ioc.liturgical.synch.git.models;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.synch.AresPushTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;

/**
 * Transitory information that holds the transactions to be pushed to the synch db
 * @author mac002
 *
 */
public class SynchData {
	private GithubRepo repo = null;
	private SynchStatus status = null;
	private List<AresPushTransaction> transactions = new ArrayList<AresPushTransaction>();

	public SynchData(GithubRepo repo) {
		this.repo = repo;
	}

	public GithubRepo getRepo() {
		return repo;
	}

	public void setRepo(GithubRepo repo) {
		this.repo = repo;
	}

	public List<AresPushTransaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<AresPushTransaction> transactions) {
		this.transactions = transactions;
	}

	public SynchStatus getStatus() {
		return status;
	}

	public void setStatus(SynchStatus status) {
		this.status = status;
	}
}
