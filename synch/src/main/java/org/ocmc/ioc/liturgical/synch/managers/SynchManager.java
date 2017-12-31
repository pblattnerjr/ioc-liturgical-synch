package org.ocmc.ioc.liturgical.synch.managers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Config.ConfigBuilder;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.summary.Notification;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.ocmc.ioc.liturgical.schemas.constants.DATA_SOURCES;
import org.ocmc.ioc.liturgical.schemas.constants.HTTP_RESPONSE_CODES;
import org.ocmc.ioc.liturgical.schemas.constants.RELATIONSHIP_TYPES;
import org.ocmc.ioc.liturgical.schemas.constants.STATUS;
import org.ocmc.ioc.liturgical.schemas.id.managers.IdManager;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.RequestStatus;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.ResultJsonObjectArray;
import org.ocmc.ioc.liturgical.synch.app.SynchServiceProvider;
import org.ocmc.ioc.liturgical.synch.constants.Constants;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_INITIAL_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.constants.GITHUB_TEST_ARES_URLS;
import org.ocmc.ioc.liturgical.synch.exceptions.DbException;
import org.ocmc.ioc.liturgical.synch.git.AresTransTuple;
import org.ocmc.ioc.liturgical.schemas.models.ModelHelpers;
import org.ocmc.ioc.liturgical.schemas.models.db.docs.ontology.TextLiturgical;
import org.ocmc.ioc.liturgical.schemas.models.synch.AresTransaction;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.schemas.models.synch.Transaction;
import org.ocmc.ioc.liturgical.utils.CypherUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;

/**
 * Provides methods to query the database for transactions,
 * and to record a transaction.
 * TODO: add check on requestor.
 * @author mac002
 *
 */
public class SynchManager {
	private static final Logger logger = LoggerFactory.getLogger(SynchManager.class);
	private static final String DELETED_LABEL_PREFIX = org.ocmc.ioc.liturgical.schemas.constants.Constants.DELETED_LABEL_PREFIX;
	private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	private JsonParser parser = new JsonParser();
	private static final String returnClause = "return properties(doc) order by doc.key";
	private String synchDomain = "";
	private String synchPort = "";
	private String username = "";
	private String password = "";
	private Driver synchDriver = null;
	private GithubRepositories githubRepos = new GithubRepositories();
	private boolean synchEnabled = false;
	private boolean synchConnectionOK = false;
	private boolean useTestRepos = false;
	private String synchInfoLabel = Constants.LABEL_GITHUB_REPO;
	private String synchTransLabel = Constants.LABEL_SYNCH_TRANS;
	private String synchAresTransLabel = Constants.LABEL_SYNCH_ARES_TRANS;
	private boolean debug = false;
	public boolean testGitSynch = false;
	public boolean deleteTestGitRepos = false;

	public SynchManager(
			  String synchDomain
			  , String synchPort
			  , String username
			  , String password
			 ,	boolean testGitSynch
			 ,	boolean deleteTestGitRepos
			  ) {
		  this.testGitSynch = testGitSynch;
		  this.setUseTestRepos(testGitSynch);
		  this.deleteTestGitRepos = deleteTestGitRepos;
		  this.synchDomain = synchDomain;
		  this.synchPort = synchPort;
		  this.username = username;
		  this.password = password;
		  synchEnabled = (synchDomain != null && synchDomain.length() > 0);
		  if (synchEnabled) {
			  initializeSynchDriver();
		  }
	}
	
	  public void initializeSynchDriver() {
		  // in case the database is not yet initialized, we will wait 
		  // try again for maxTries, after waiting for 
		  int maxTries = 5;
		  int tries = 0;
		  long waitPeriod = 15000; // 1000 = 1 second
		  while (tries <= maxTries) {
			  String synchBoltUrl = "bolt://" + this.synchDomain + ":" + this.synchPort;
			  logger.info("Using " + synchBoltUrl + " for external synch database...");
			  try {
				  // getting periodic TLS connection closed errors.  Setting connectionTimeout to see if solves problem.
				  ConfigBuilder cb = Config.build();
				  cb.withConnectionTimeout(
						  Constants.boltDriverConnectionTimeout
						  , Constants.boltDriverConnectionTimeoutUnits
						  );
				  Config config = cb.toConfig();
				  synchDriver = GraphDatabase.driver(synchBoltUrl, AuthTokens.basic(username, password), config);
				  testSynchConnection();
				  if (this.synchConnectionOK) {
					  if (this.testGitSynch) {
							if (this.deleteTestGitRepos) {
								  this.deleteTestRepos();
							}
			  				this.setGithubRepos(GITHUB_TEST_ARES_URLS.toPOJO());
					  } else {
		  					this.setGithubRepos(GITHUB_INITIAL_ARES_URLS.toPOJO());
					  }
					  this.githubRepos = this.getGithubSynchInfo();
				 }
			  } catch (org.neo4j.driver.v1.exceptions.ServiceUnavailableException u) {
				  this.synchConnectionOK = false;
				  logger.error("Can't connect to the Neo4j SYNCH database.");
				  logger.error(u.getMessage());
			  } catch (Exception e) {
				  this.synchConnectionOK = false;
				  ErrorUtils.report(logger, e);
			  }
			  if (this.synchConnectionOK) {
				  break;
			  } else {
				  try {
					  logger.info("Will retry db connection in " + waitPeriod / 1000 + " seconds...");
						Thread.sleep(waitPeriod); 
					} catch (InterruptedException e) {
						ErrorUtils.report(logger, e);
					}
				  tries++;
			  }
		  }
		  if (! this.synchConnectionOK) {
			  SynchServiceProvider.sendMessage("SynchManager cannot connect to synch database at " + this.synchDomain + ":" + this.synchPort);
		  }
	  }
	  
	  private ResultJsonObjectArray testSynchConnection() {
		  ResultJsonObjectArray result = getResultObjectForQuery("match (n) return count(n) limit 1");
		  if (result.getValueCount() > 0) {
			  logger.info("Connection to Neo4j SYNCH database is OK and encrypted.");
			  if (! this.synchDriver.isEncrypted()) {
				  logger.error("Connection to Neo4j SYNCH database is NOT encrypted!");
				  this.synchConnectionOK = false;
			  } else {
				  this.synchConnectionOK = true;
			  }
		  } else {
			  this.synchConnectionOK = false;
			  logger.error("Can't connect to the Neo4j SYNCH database.");
			  logger.error(result.getStatus().getUserMessage());
		  }
		  return result;
	  }

	  public ResultJsonObjectArray getResultObjectForQuery(
			  String query
			  ) {
			ResultJsonObjectArray result = new ResultJsonObjectArray(true);
			try (org.neo4j.driver.v1.Session session = synchDriver.session()) {
				StatementResult neoResult = session.run(query);
				while (neoResult.hasNext()) {
					org.neo4j.driver.v1.Record record = neoResult.next();
						JsonObject o = parser.parse(gson.toJson(record.asMap())).getAsJsonObject();
						if (o.has("properties(link)")) {
							o = parser.parse(gson.toJson(record.get("properties(link)").asMap())).getAsJsonObject();
						} else if (o.has("properties(doc)")) {
							o = parser.parse(gson.toJson(record.get("properties(doc)").asMap())).getAsJsonObject();
						}
						result.addValue(o);
				}
			} catch (Exception e) {
				result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
				result.setStatusMessage(e.getMessage());
			}
			return result;
	}

	  
	public ResultJsonObjectArray getForQuery(String query) {
			return getResultObjectForQuery(query);
	}

	/**
	 * Gets transactions whose timestamp is greater than or equal to
	 * the parameter 'since'.  The results are ordered ascending based
	 * on the timestamp.  So, the last value in the results array will be
	 * the most recent transaction.
	 * @param requestor person making the request
	 * @param since the time
	 * @return transactions whose timestamp is greater than or equal to the parameter 'since'
	 */
	public ResultJsonObjectArray getTransactionsSince(
			String requestor
			, String since
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(") where doc.key > '");
			sb.append(since);
			sb.append("' ");
			sb.append(returnClause);
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}
	
	public RequestStatus deleteTransaction(String id) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		StringBuffer sb = new StringBuffer();
		sb.append("match (doc:");
		sb.append(this.synchTransLabel);
		sb.append(") where doc.id = '");
		sb.append(id);
		sb.append("' delete doc return count(doc)");
		try (org.neo4j.driver.v1.Session session = synchDriver.session()) {
			StatementResult neoResult = session.run(sb.toString());
			count = neoResult.consume().counters().nodesDeleted();
			if (count > 0) {
		    	result.setCode(HTTP_RESPONSE_CODES.OK.code);
		    	result.setMessage(HTTP_RESPONSE_CODES.OK.message + ": deleted " + id);
			} else {
		    	result.setCode(HTTP_RESPONSE_CODES.NOT_FOUND.code);
		    	result.setMessage(HTTP_RESPONSE_CODES.NOT_FOUND.message + " " + id);
			}
		} catch (Exception e){
			result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			result.setDeveloperMessage(e.getMessage());
		}
		return result;
	}

	public RequestStatus deleteTestRepos() {
		RequestStatus status = new RequestStatus();
		try {
			status = this.deleteWhereStartsWith(
					Constants.GithubRepositoriesLibraryTopic
					, Constants.LABEL_GITHUB_TEST_REPO
					);
			logger.info("Deleted test repos...");
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return status;
	}
	
	public RequestStatus deleteWhereStartsWith(String id, String label) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		String query = 
				"match (doc:%s) where doc.id starts with '%s' delete doc return count(doc)";
		query = String.format(query, label, id);
		try (org.neo4j.driver.v1.Session session = synchDriver.session()) {
			StatementResult neoResult = session.run(query);
			count = neoResult.consume().counters().nodesDeleted();
			if (count > 0) {
		    	result.setCode(HTTP_RESPONSE_CODES.OK.code);
		    	result.setMessage(HTTP_RESPONSE_CODES.OK.message + ": deleted " + id);
			} else {
		    	result.setCode(HTTP_RESPONSE_CODES.NOT_FOUND.code);
		    	result.setMessage(HTTP_RESPONSE_CODES.NOT_FOUND.message + " " + id);
			}
		} catch (Exception e){
			result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			result.setDeveloperMessage(e.getMessage());
		}
		return result;
	}

	/**
	 * Gets the transaction for the specified timestamp.
	 * @param requestor who is asking
	 * @param timestamp the time to use for the search
	 * @return the transaction for the specified timestamp
	 */
	public ResultJsonObjectArray getTransactionByTimestamp(
			String requestor
			, String timestamp
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(") where doc.key = '");
			sb.append(timestamp);
			sb.append("' ");
			sb.append(" return properties(doc)");
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	/**
	 * Gets transactions whose timestamp is greater than or equal to
	 * the parameter 'since'.  The results are ordered ascending based
	 * on the timestamp.  So, the last value in the results array will be
	 * the most recent transaction.
	 * @param requestor person making the request
	 * @param library library to use
	 * @param since when
	 * @return transactions whose timestamp is greater than or equal to the parameter 'since'.
	 */
	public ResultJsonObjectArray getTransactionsForLibrarySince(
			String requestor
			, String library
			, String since
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(") where doc.key > '");
			sb.append(since);
			sb.append("'" );
			sb.append(this.andLibrary(library));
			sb.append(returnClause);
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public GithubRepositories getGithubSynchInfo() {
		GithubRepositories repos = new GithubRepositories();
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			String query = "match (doc:%s) where doc.id starts with '%s' return properties(doc)"; 
			query = String.format(query, this.synchInfoLabel, Constants.GithubRepositoriesLibraryTopic);
			result.setQuery(query);
			result = getForQuery(query);
			if (result.valueCount == 0) {
				if (this.useTestRepos) {
					repos = GITHUB_TEST_ARES_URLS.toPOJO();
				} else {
					repos = GITHUB_INITIAL_ARES_URLS.toPOJO();
				}
				this.createGitSynchInfo(repos);
			} else {
				for (JsonObject value : result.getValues()) {
					GithubRepo repo = gson.fromJson(value, GithubRepo.class);
					repos.addRepo(repo);
				}
			}
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return repos;
	}
	
	public void initializeTestRepos() {
		
	}
	
	/**
	 * Gets all transactions whose status = RELEASED and whose
	 * timestamp is greater than or equal to the 'since' parameter
	 * @param requestor person making the request
	 * @param since when
	 * @return matching transactions
	 */
	public ResultJsonObjectArray getReleasedTransactionsSince(
			String requestor
			, String since
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(") where doc.key > '");
			sb.append(since);
			sb.append("' ");
			sb.append("and doc.status = '");
			sb.append(STATUS.RELEASED.keyname);
			sb.append("' ");
			sb.append(returnClause);
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	/**
	 * Gets all transactions whose status = RELEASED and whose
	 * timestamp is greater than or equal to the 'since' parameter
	 * @param requestor person making the request
	 * @param library the library
	 * @param since when
	 * @return matching transactions
	 */
	public ResultJsonObjectArray getReleasedTransactionsForLibrarySince(
			String requestor
			, String library
			, String since
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(") where doc.key > '");
			sb.append(since);
			sb.append("' and doc.status = '");
			sb.append(STATUS.RELEASED.keyname);
			sb.append("' ");
			sb.append(this.andLibrary(library));
			sb.append(returnClause);
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	/**
	 * Returns the most recent transaction
	 * @param requestor person making the request
	 * @return the most recent transaction
	 */
	public ResultJsonObjectArray getMostRecentTransaction(String requestor) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(")");
			sb.append(returnClause);
			sb.append(" descending limit 1");
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	/**
	 * Returns the most recent transaction for the specified library
	 * @param requestor person making the request
	 * @param library whose transaction is wanted
	 * @return the most recent transaction for the specified library
	 */
	public ResultJsonObjectArray getMostRecentTransactionForLibrary(
			String requestor
			, String library
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(")");
			sb.append(whereLibrary(library));
			sb.append(returnClause);
			sb.append(" descending limit 1");
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	/**
	 * Returns the most recent transaction for the specified Id
	 * @param requestor person making the request
	 * @param id - the Id of a doc.  This is stored in the transaction as its topic.
	 * @return the most recent transaction for the specified Id
	 */
	public ResultJsonObjectArray getMostRecentTransactionForId(
			String requestor
			, String id
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(")  ");
			sb.append(this.whereTopicIsId(id));
			sb.append("' ");
			sb.append(returnClause);
			sb.append(" descending limit 1");
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public ResultJsonObjectArray getTransactionsForId(
			String requestor
			, String id
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:");
			sb.append(this.synchTransLabel);
			sb.append(")  ");
			sb.append(this.whereTopicIsId(id));
			sb.append(returnClause);
			sb.append(" ascending");
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	private String andLibrary(String library) {
		StringBuffer result = new StringBuffer();
		result.append("and doc.id starts with '");
		result.append(Constants.LIBRARY_SYNCH);
		result.append(Constants.ID_DELIMITER);
		result.append(library);
		result.append(Constants.ID_DELIMITER);
		result.append("' ");
		return result.toString();
	}

	private String whereLibrary(String library) {
		StringBuffer result = new StringBuffer();
		result.append("where doc.id starts with '");
		result.append(Constants.LIBRARY_SYNCH);
		result.append(Constants.ID_DELIMITER);
		result.append(library);
		result.append(Constants.ID_DELIMITER);
		result.append("' ");
		return result.toString();
	}

	private String whereTopicIsId(String id) {
		StringBuffer result = new StringBuffer();
		result.append("where doc.id starts with '");
		result.append(Constants.LIBRARY_SYNCH);
		result.append(Constants.ID_DELIMITER);
		result.append(id);
		result.append(Constants.ID_DELIMITER);
		result.append("' ");
		return result.toString();
	}

	public boolean synchEnabled() {
		return synchEnabled;
	}

	public boolean synchConnectionOK() {
		return synchConnectionOK;
	}
	
	/**
	 * The purpose of this method is to ensure that any node with 
	 * an id property has a unique constraint so that duplicate IDs 
	 * are not allowed.
	 * @param label for the constraint
	 * @return result
	 */
	public StatementResult setIdConstraint(String label) {
		StatementResult neoResult = null;
		String query = "create constraint on (p:%s) assert p.id is unique"; 
		query = String.format(query, label);
		try (org.neo4j.driver.v1.Session session = synchDriver.session()) {
			neoResult = session.run(query);
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return neoResult;
	}

	public RequestStatus recordTransactions (List<Transaction> list) {
		RequestStatus result = new RequestStatus();
		try {
			for (Transaction trans : list) {
				 result = this.recordTransaction(trans);
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return result;
	}

	public RequestStatus recordTransaction(Transaction doc) throws DbException {
			RequestStatus result = new RequestStatus();
			int count = 0;
			StringBuffer sb = new StringBuffer();


			setIdConstraint(this.synchTransLabel);
			sb.append("create (doc:");
			sb.append(this.synchTransLabel);
			sb.append(") set doc = {props} return doc");
			try (org.neo4j.driver.v1.Session session = this.synchDriver.session()) {
				Map<String,Object> props = ModelHelpers.getAsPropertiesMap(doc);
				StatementResult neoResult = session.run(sb.toString(), props);
				count = neoResult.consume().counters().nodesCreated();
				if (count > 0) {
			    	result.setCode(HTTP_RESPONSE_CODES.CREATED.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.CREATED.message + ": created " + doc.whenTransactionRecordedInThisDatabase);
				} else {
			    	result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message + "  " + doc.whenTransactionRecordedInThisDatabase );
				}
			} catch (Exception e){
				if (e.getMessage().contains("already exists")) {
					result.setCode(HTTP_RESPONSE_CODES.CONFLICT.code);
					result.setDeveloperMessage(HTTP_RESPONSE_CODES.CONFLICT.message);
				} else {
					result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
					result.setDeveloperMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
				}
				result.setUserMessage(e.getMessage());
			}
			return result;
	}

	public synchronized RequestStatus createGitSynchInfo(GithubRepositories doc) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		logger.info("Creating GitSynchInfo for respositories using label " + this.synchInfoLabel);
		setIdConstraint(this.synchInfoLabel);
		String query = "create (doc:%s) set doc = {props} return doc";
		query = String.format(query, this.synchInfoLabel);
		for (GithubRepo repo : doc.getRepos()) {
			ResultSummary summary = null;
			try (org.neo4j.driver.v1.Session session = this.synchDriver.session()) {
				Map<String,Object> props = ModelHelpers.getAsPropertiesMap(repo);
				StatementResult neoResult = session.run(query, props);
				summary = neoResult.consume();
				for (Notification n : summary.notifications()) {
					logger.info(n.code() + " " + n.description());
				}
				count = summary.counters().nodesCreated();
				if (count > 0) {
					logger.info("Created GitSynchInfo for " + repo.getName());
			    	result.setCode(HTTP_RESPONSE_CODES.CREATED.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.CREATED.message);
				} else {
					logger.error("Could not create GitSynchInfo doc for " + repo.getName());
			    	result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			    	result.setMessage(summary.notifications().toString());
				}
			} catch (Exception e){
				ErrorUtils.report(logger, e);
				if (e.getMessage().contains("already exists")) {
					result.setCode(HTTP_RESPONSE_CODES.CONFLICT.code);
					result.setDeveloperMessage(HTTP_RESPONSE_CODES.CONFLICT.message);
				} else {
					result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
					result.setDeveloperMessage(e.getMessage());
				}
				result.setUserMessage(e.getMessage());
			}
		}
		return result;
	}

	public RequestStatus saveAresTransaction(AresTransaction doc) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		setIdConstraint(this.synchAresTransLabel);
		String query = "create (doc:%s) set doc = {props} return doc";
		query = String.format(query, this.synchAresTransLabel);
		ResultSummary summary = null;
		try (org.neo4j.driver.v1.Session session = this.synchDriver.session()) {
			Map<String,Object> props = ModelHelpers.getAsPropertiesMap(doc);
			StatementResult neoResult = session.run(query, props);
			summary = neoResult.summary();
			count = summary.counters().nodesCreated();
			if (count > 0) {
		    	result.setCode(HTTP_RESPONSE_CODES.CREATED.code);
		    	result.setMessage(HTTP_RESPONSE_CODES.CREATED.message);
			} else {
		    	result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
		    	result.setMessage(summary.notifications().toString());
			}
		} catch (Exception e){
			if (e.getMessage().contains("already exists")) {
				result.setCode(HTTP_RESPONSE_CODES.CONFLICT.code);
				result.setDeveloperMessage(HTTP_RESPONSE_CODES.CONFLICT.message);
			} else {
				result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
				result.setDeveloperMessage(e.getMessage());
			}
			result.setUserMessage(e.getMessage());
		}
		return result;
	}

	public RequestStatus updateGitSynchInfo(GithubRepositories doc) throws DbException {
			RequestStatus result = new RequestStatus();
			for (GithubRepo repo : doc.getRepos()) {
				result = updateGitRepoSynchInfo(repo);
			}
			return result;
	}

		public RequestStatus updateGitRepoSynchInfo(GithubRepo repo) throws DbException {
			RequestStatus result = new RequestStatus();
			int count = 0;
			String query = "match (doc:%s) where doc.id = '%s' set doc = {props} return doc";
			query = String.format(query, this.synchInfoLabel, repo.getId());
			try (org.neo4j.driver.v1.Session session = this.synchDriver.session()) {
				Map<String,Object> props = ModelHelpers.getAsPropertiesMap(repo);
				StatementResult neoResult = session.run(query, props);
				count = neoResult.consume().counters().propertiesSet();
				if (count > 0) {
			    	result.setCode(HTTP_RESPONSE_CODES.OK.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.OK.message);
				} else {
			    	result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
				}
			} catch (Exception e){
				if (e.getMessage().contains("already exists")) {
					result.setCode(HTTP_RESPONSE_CODES.CONFLICT.code);
					result.setDeveloperMessage(HTTP_RESPONSE_CODES.CONFLICT.message);
				} else {
					result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
					result.setDeveloperMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
				}
				result.setUserMessage(e.getMessage());
			}
			return result;
	}

	public GithubRepositories getGithubRepos() {
		return githubRepos;
	}

	public void setGithubRepos(GithubRepositories githubRepos) {
		this.githubRepos = githubRepos;
	}
	
	public void printGithubReposInfo() {
		if (this.githubRepos != null) {
			this.githubRepos.setPrettyPrint(true);
			logger.info("Github Repos Synch Info:\n" + this.githubRepos.toJsonString());
		}
	}
	
	/**
	 * TODO: this is where we will write the code to combine the processing
	 * of both ares pushes and pulls.
	 * @param fromGit  transactions from Git
	 * @param fromDb transactions from the Database
	 * @return the status of the request
	 */
	public RequestStatus processAres (
			List<AresTransaction> fromGit
			, List<AresTransaction> fromDb
			) {
		RequestStatus status = new RequestStatus();
		/**
		 * The map below will contain keys that are library~topic
		 * and a value that is a tuple of both ares to db and db to ares transactions.
		 * This allows us to compare the two transactions and decide how to apply them.
		 */
		Map<String,AresTransTuple> map = new TreeMap<String,AresTransTuple>();
		for (AresTransaction ares : fromGit) {
			IdManager idManager = new IdManager(ares.toLibrary, ares.toTopic, ares.toKey);
			AresTransTuple tuple = new AresTransTuple();
			tuple.setFromGit(ares);
			map.put(idManager.getId(), tuple);
		}
		for (AresTransaction ares : fromDb) {
			IdManager idManager = new IdManager(ares.toLibrary, ares.toTopic, ares.toKey);
			AresTransTuple tuple = new AresTransTuple();
			if (map.containsKey(idManager.getId())) {
				tuple = map.get(idManager.getId());
			}
			tuple.setFromDb(ares);
			map.put(idManager.getId(), tuple);
		}
		for (AresTransTuple tuple : map.values()) {
			if (! tuple.valuesAndCommentsSame()) {
				if (tuple.gitIsNewer()) {
					// update db ???
//					transaction.setAresTransId(ares.getId());
//					this.recordTransaction(transaction);
//					this.saveAresTransaction(ares); // for future reference in case there are questions
				} else {
					// update git ???
					// TODO: save AresTrans
				}
			}
		}
		return status;
	}

	/**
	 * This method will create a Transaction from the AresTransaction
	 * and record it in the synch database.  
	 * 
	 * Because we do not know at this point whether a doc already exists,
	 * it is necessary to use MERGE instead of CREATE and to add
	 * ON CREATE and ON MATCH clauses.
	 * 
	 * @param ares transaction to be pushed
	 * @return the status of the request
	 */
	public RequestStatus processAresTransaction(AresTransaction ares) {
		RequestStatus status = new RequestStatus();
		try {
			String query = null; // if remains null, no transaction will be recorded
			TextLiturgical doc = new TextLiturgical(
					ares.toLibrary
					, ares.toTopic
					, ares.toKey
					);
			doc.setValue(ares.toValue);
			doc.setComment(ares.toComment);
			doc.setCreatedBy(ares.getRequestingUser());
			doc.setCreatedWhen(Instant.now().toString());
			doc.setModifiedBy(ares.getRequestingUser());
			doc.setModifiedWhen(doc.getCreatedWhen());
			doc.setDataSource(DATA_SOURCES.GITHUB);
			List<String> queries = new ArrayList<String>();

			switch (ares.type) {
			case ADD_KEY_VALUE:
				query = this.createMergeNodeQuery(doc);
				queries.add(query);
				break;
			case CHANGE_OF_KEY:
				queries.addAll(this.createRenameKeyQueries(doc));
				break;
			case CHANGE_OF_LIBRARY: 
				// fall through to CHANGE_OF_TOPIC
			case CHANGE_OF_TOPIC:
				this.createLibraryTopicRenameTransactions(
					ares.fromLibrary
					, ares.fromTopic
					, ares.toLibrary
					, ares.toTopic
					, ares.getRequestingUser()
				);
				break;
			case CHANGE_OF_VALUE: // treat same as ADD_KEY_VALUE
				query = this.createMergeNodeQuery(doc);
				queries.add(query);
				break;
			case DELETE_KEY_VALUE:
				// TODO: soft delete of relationships and linked nodes
				queries.addAll(this.createDeleteKeyValueQueries(doc));
				break;
			case DELETE_TOPIC: // should appear as DELETE_KEY_VALUE instead
				logger.error("DELETE TOPIC - unexpected ares transaction: " + ares.toJsonString());
				break;
			case UNKNOWN: // fall through
			default:
				logger.error("unknown ares transaction: " + ares.toJsonString());
				break;
			}
			if (queries.size() > 0) {
				for (String theQuery : queries) {
					Transaction transaction = new Transaction(
							theQuery
							, doc
							, ares.requestingServer
					);
					transaction.setFromAres(true);
					transaction.setRequestingMac(ares.requestingMac);
					transaction.setPrettyPrint(true);
					if (this.debug) {
						System.out.println(transaction.toJsonString());
					}
					transaction.setAresTransId(ares.getId());
					this.recordTransaction(transaction);
					this.saveAresTransaction(ares); // for future reference in case there are questions
				}
			}
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return status;
	}
	
	/**
	 * Rather than actually delete a doc, we will rename all its labels by
	 * prefixing them with DELETED_
	 * @param doc the doc for which to create the queries
	 * @return list of queries to rename each label for this doc to one that has DELETE_ as its prefix
	 */
	private List<String> createDeleteKeyValueQueries(TextLiturgical doc) {
		List<String> result = new ArrayList<String>();
		for (String label : doc.fetchOntologyLabelsList()) {
			StringBuffer sb = new StringBuffer();
			sb.append("match (n:Root) where n.id = '");
			sb.append(doc.getId());
			sb.append("' ");
			sb.append(
					CypherUtils.getLabelRenameClause(
							"n"
							, label
							, DELETED_LABEL_PREFIX + label
							)
					);
			result.add(sb.toString());
		}
		// now add the queries to rename the relationships
		result.addAll(this.createLiturgicalRelationshipDeleteQueries(doc.getId()));
		return result;
	}

	private List<String> createRenameKeyQueries(TextLiturgical doc) {
		List<String> result = new ArrayList<String>();
		return result;
	}

	private String createMergeNodeQuery(
			TextLiturgical doc
			) {
		StringBuffer sb = new StringBuffer();
		sb.append("MERGE (n:");
		sb.append(doc.fetchOntologyLabels());
		sb.append(" {id: '");
		sb.append(doc.id);
		sb.append("'}) ");
		sb.append("ON CREATE SET n = {props} ");
		sb.append("ON MATCH SET n.value = $props.value, n.comment = $props.comment, n.modifiedBy = $props.modifiedBy, n.modifiedWhen = $props.modifiedWhen, n.dataSource = $props.dataSource ");
		sb.append("return n");
		return sb.toString();
	}
	
	/**
	 * An ID has three parts: library~topic~key.
	 * Sometimes the value of library or topic or key
	 * is another node's ID.  In such a case, the library
	 * or topic or key is termed 'complex'.
	 * 
	 * This method creates a transaction to cover each of four cases:
	 * 1. The ID parts are all simple.
	 * 2. The library part is complex.
	 * 3. The topic part is complex.
	 * 4. The key part is complex.
	 * 
	 * We do not know whether all four cases are needed, but we
	 * will create and eventually execute them just in case...
	 * 
	 * @param fromLibrary library from
	 * @param fromTopic topic from
	 * @param toLibrary library to
	 * @param toTopic topic to
	 * @param committerName name of the committer
	 */
	public void createLibraryTopicRenameTransactions(
			String fromLibrary
			, String fromTopic
			, String toLibrary
			, String toTopic
			, String committerName
			) {
		String combinedFrom = "";
		String combinedTo = "";
		try {
			String query = "";
			if (fromTopic.equals(toTopic)) { // only the library changed
				// create a transaction for when the ID is not complex
				query = CypherUtils.getQueryToGloballyReplaceLibrary(fromLibrary, toLibrary);
				combinedFrom = fromLibrary + "~";
				combinedTo = toLibrary + "~";
			} else {
				// create a transaction for when the ID is not complex
				query = CypherUtils.getQueryToReplaceLibraryAndTopic(
						fromLibrary
						, toLibrary
						, fromTopic
						, toTopic
						);
				combinedFrom = fromLibrary + "~" + fromTopic + "~" ;
				combinedTo = toLibrary + "~" + toTopic + "~" ;
			}
			Transaction trans = new Transaction(
					query
					, committerName
					);
			trans.setFromAres(true);
			this.recordTransaction(trans);
			// create a transaction for when the library is complex
			String complexLibrary = CypherUtils.getQueryToGloballyReplaceLibrary(
					combinedFrom
					, combinedTo
					);
			trans = new Transaction(
					complexLibrary
					, committerName
					);
			trans.setFromAres(true);
			this.recordTransaction(trans);
			// create a transaction for when the topic is complex
			String complexTopic = CypherUtils.getQueryToGloballyReplaceTopic(
					combinedFrom
					, combinedTo
					);
			trans = new Transaction(
					complexTopic
					, committerName
					);
			trans.setFromAres(true);
			this.recordTransaction(trans);
			// create a transaction for when the key is complex
			String complexKey = CypherUtils.getQueryToGloballyReplaceKey(
					combinedFrom
					, combinedTo
					);
			trans = new Transaction(
					complexKey
					, committerName
					);
			trans.setFromAres(true);
			this.recordTransaction(trans);
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
	}
	
	/**
	 * When a Liturgical doc is deleted, we do a 'soft delete' by simply
	 * renaming its labels.  But, a liturgical doc can also have relationships
	 * to other docs.  So, we need to do a 'soft delete' of the relationships.
	 * A soft delete renames the relationship type by prefixing the DELETED_LABEL_PREFIX.
	 * @param docId the id of the Liturgical doc
	 * @return a list of queries to be run to soft delete the relationships to the doc
	 */
	public List<String> createLiturgicalRelationshipDeleteQueries(
			String docId
			) {
		List<String> result = new ArrayList<String>();
		for (RELATIONSHIP_TYPES type : org.ocmc.ioc.liturgical.schemas.constants.RELATIONSHIP_TYPES.filterByLiturgicalRelations()) {
			String query = CypherUtils.getQueryToRenameRelationshipType(
					org.ocmc.ioc.liturgical.schemas.constants.TOPICS.TEXT_LITURGICAL.label
					, docId
					, type.name()
					, DELETED_LABEL_PREFIX + type.name()
					);
			result.add(query);
		}
		return result;
	}

	public String getSynchDomain() {
		return synchDomain;
	}

	public void setSynchDomain(String synchDomain) {
		this.synchDomain = synchDomain;
	}

	public String getSynchPort() {
		return synchPort;
	}

	public void setSynchPort(String synchPort) {
		this.synchPort = synchPort;
	}

	public Driver getSynchDriver() {
		return synchDriver;
	}

	public void setSynchDriver(Driver synchDriver) {
		this.synchDriver = synchDriver;
	}

	public boolean isSynchEnabled() {
		return synchEnabled;
	}

	public void setSynchEnabled(boolean synchEnabled) {
		this.synchEnabled = synchEnabled;
	}

	public boolean isSynchConnectionOK() {
		return synchConnectionOK;
	}

	public void setSynchConnectionOK(boolean synchConnectionOK) {
		this.synchConnectionOK = synchConnectionOK;
	}

	public boolean isUseTestRepos() {
		return useTestRepos;
	}

	public void setUseTestRepos(boolean useTestRepos) {
		this.useTestRepos = useTestRepos;
		if (useTestRepos) {
			this.synchInfoLabel = Constants.LABEL_GITHUB_TEST_REPO;
			this.synchTransLabel = Constants.LABEL_SYNCH_TEST_TRANS;
			this.synchAresTransLabel = Constants.LABEL_SYNCH_TEST_ARES_TRANS;
		} else {
			this.synchInfoLabel = Constants.LABEL_GITHUB_REPO;
			this.synchTransLabel = Constants.LABEL_SYNCH_TRANS;
			this.synchAresTransLabel = Constants.LABEL_SYNCH_ARES_TRANS;
		}
	}

	public String getSynchInfoLabel() {
		return synchInfoLabel;
	}

	public void setSynchInfoLabel(String synchInfoLabel) {
		this.synchInfoLabel = synchInfoLabel;
		this.useTestRepos = synchInfoLabel.equals(Constants.LABEL_GITHUB_TEST_REPO);
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getSynchTransLabel() {
		return synchTransLabel;
	}

	public void setSynchTransLabel(String synchTransLabel) {
		this.synchTransLabel = synchTransLabel;
	}

	public String getSynchAresTransLabel() {
		return synchAresTransLabel;
	}

	public void setSynchAresTransLabel(String synchAresTransLabel) {
		this.synchAresTransLabel = synchAresTransLabel;
	}

}
