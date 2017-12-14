package org.ocmc.ioc.liturgical.synch.managers;

import java.util.Map;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Config.ConfigBuilder;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.ocmc.ioc.liturgical.schemas.constants.HTTP_RESPONSE_CODES;
import org.ocmc.ioc.liturgical.schemas.constants.STATUS;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.RequestStatus;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.ResultJsonObjectArray;
import org.ocmc.ioc.liturgical.synch.constants.Constants;
import org.ocmc.ioc.liturgical.synch.exceptions.DbException;
import org.ocmc.ioc.liturgical.schemas.models.ModelHelpers;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.schemas.models.synch.Transaction;
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
	private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	private JsonParser parser = new JsonParser();
	private static final String returnClause = "return properties(doc) order by doc.key";
	private String synchDomain = "";
	private String synchPort = "";
	private boolean synchEnabled = false;
	private String username = "";
	private String password = "";
	private Driver synchDriver = null;
	private boolean synchConnectionOK = false;
	private GithubRepositories githubRepos = new GithubRepositories();
	
	public SynchManager(
			  String synchDomain
			  , String synchPort
			  , String username
			  , String password
			  ) {
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
			sb.append("match (doc:Transaction) where doc.key > '");
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
		String query = 
				"match (doc:Transaction) where doc.id = \"" 
				+ id 
		        + "\" delete doc return count(doc)";
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
			sb.append("match (doc:Transaction) where doc.key = '");
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
			sb.append("match (doc:Transaction) where doc.key > '");
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
		GithubRepositories repos = null;
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:GithubTransaction return properties(doc)"); 
			String query = sb.toString();
			result.setQuery(query);
			result = getForQuery(query);
			if (result.valueCount == 1) {
				repos = gson.fromJson(result.getFirstObjectValueAsString(), GithubRepositories.class);
			}
		} catch (Exception e) {
			result.setStatusCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			result.setStatusMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message);
			ErrorUtils.report(logger, e);
		}
		return repos;
	}
	/**
	 * Gets all transactions whose status = RELEASED and whose
	 * timestamp is greater than or equal to the 'since' parameter
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
			sb.append("match (doc:Transaction) where doc.key > '");
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
			sb.append("match (doc:Transaction) where doc.key > '");
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
	 * @return the most recent transaction
	 */
	public ResultJsonObjectArray getMostRecentTransaction(String requestor) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:Transaction  ");
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
	 * @return the most recent transaction for the specified library
	 */
	public ResultJsonObjectArray getMostRecentTransactionForLibrary(
			String requestor
			, String library
			) {
		ResultJsonObjectArray result = new ResultJsonObjectArray(false); // true means PrettyPrint the json
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("match (doc:Transaction  ");
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
			sb.append("match (doc:Transaction  ");
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
			sb.append("match (doc:Transaction)  ");
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
		String query = "create constraint on (p:Transaction) assert p.id is unique"; 
		try (org.neo4j.driver.v1.Session session = synchDriver.session()) {
			neoResult = session.run(query);
		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
		return neoResult;
	}

	public RequestStatus recordTransaction(Transaction doc) throws DbException {
			RequestStatus result = new RequestStatus();
			int count = 0;
			setIdConstraint("Transaction");
			String query = "create (doc:Transaction) set doc = {props} return doc";
			try (org.neo4j.driver.v1.Session session = this.synchDriver.session()) {
				Map<String,Object> props = ModelHelpers.getAsPropertiesMap(doc);
				StatementResult neoResult = session.run(query, props);
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

	public RequestStatus createGitSynchInfo(GithubRepositories doc) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		setIdConstraint("GithubRepositories");
		String query = "create (doc:GithubRepositories) set doc = {props} return doc";
		try (org.neo4j.driver.v1.Session session = this.synchDriver.session()) {
			Map<String,Object> props = ModelHelpers.getAsPropertiesMap(doc);
			StatementResult neoResult = session.run(query, props);
			count = neoResult.consume().counters().nodesCreated();
			if (count > 0) {
		    	result.setCode(HTTP_RESPONSE_CODES.CREATED.code);
		    	result.setMessage(HTTP_RESPONSE_CODES.CREATED.message);
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

}
