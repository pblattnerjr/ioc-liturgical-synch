package org.ocmc.ioc.liturgical.synch.alwb.gateway.loaders;

import java.io.File;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.ocmc.ioc.liturgical.schemas.constants.Constants;
import org.ocmc.ioc.liturgical.schemas.constants.HTTP_RESPONSE_CODES;
import org.ocmc.ioc.liturgical.schemas.constants.STATUS;
import org.ocmc.ioc.liturgical.schemas.models.ModelHelpers;
import org.ocmc.ioc.liturgical.schemas.models.db.docs.ontology.TextLiturgical;
import org.ocmc.ioc.liturgical.schemas.models.supers.LTKDb;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepo;
import org.ocmc.ioc.liturgical.schemas.models.synch.GithubRepositories;
import org.ocmc.ioc.liturgical.schemas.models.ws.response.RequestStatus;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.LibraryFileProxy;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.LibraryLine;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.LibraryProxyManager;
import org.ocmc.ioc.liturgical.synch.alwb.gateway.ares.LibraryUtils;
import org.ocmc.ioc.liturgical.synch.exceptions.DbException;
import org.ocmc.ioc.liturgical.synch.git.JGitUtils;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to load a Neo4j Database from Ares files.
 * 
 * If the boolean useResolvedValues is set to true:
 * 
 * 1. If a value is a key, it will use the key to look up the value and use it instead.
 * 2. If the domain of the lefthand key is different than the domain of the
 *     righthand key (the key as value), it will ignore it.  The reason is that
 *     we don't want to mix domains.
 *     
 *   If the boolean useResolvedValues is set to false,
 *   the node will not have a value property.  Instead,
 *   it will have a relationship of type VALUE_FROM that points to the
 *   value to use for that node.
 *   
 *   TODO: study these and consider batching the transactions:
 *   https://github.com/neo4j/neo4j/issues/7228
 *   https://gist.github.com/nandosola/ebe2ced123e05a79e238edd6ec81fee5
 * 
 * @author mac002
 *
 */
public class AresToNeo4j {
	private static final Logger logger = LoggerFactory
			.getLogger(AresToNeo4j.class);

	private static List<String> constraints = new ArrayList<String>();

	private static void pullFromGitHub(String rootPath) {
		
	}
	
	/**
	 * Program to load a neo4j database from ares files.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {

		String user = args[0];
		String pwd = args[1];
		String url = args[2];

		boolean updateDatabaseNodes = false; 
		boolean updateDatabaseRelationships = false; 
		boolean useResolvedValues = false; // if true, will be used as read-only database
		// and, if true, there won't be any relationships between nodes
		
		boolean includeComment = true;
		boolean inspectLine = true; // if true, you can set a breakpoint for
										// when it matches idOfLineToInspect
		String idOfLineToInspect = "en_uk_lash~me.m01.d01~meHO.note1";
		String idSeparator = "~";
		Pattern punctPattern = Pattern.compile("[˙·,.;!?(){}\\[\\]<>%]"); // punctuation 
		
		int valuesWithText = 0;
		int valuesWithIds = 0;
		int valuesWithNothing = 0;
		int pointsToSelf = 0;
		
		// Load the ares
		LibraryProxyManager libProxyManager;
		String alwbPath = "/Users/mac002/Git/alwb-repositories/ages"; // ages only
//		String alwbPath = "/Users/mac002/Git/alwb-repositories/kenya"; // kenya only
		List<String> domainsToProcess = new ArrayList<String>();
		/**
		 * Add each domain that you want to process
		 */
		domainsToProcess.add("en_US_andronache");
		domainsToProcess.add("en_US_barrett");
		domainsToProcess.add("en_US_boyer");
		domainsToProcess.add("en_US_constantinides");
		domainsToProcess.add("en_US_dedes");
		domainsToProcess.add("en_US_goa");
		domainsToProcess.add("en_US_holycross");
		domainsToProcess.add("en_UK_lash");
		domainsToProcess.add("en_US_oca");
		domainsToProcess.add("en_US_public");
		domainsToProcess.add("en_US_repass");
		domainsToProcess.add("en_US_unknown");
		domainsToProcess.add("gr_GR_cog");
		
		// ages scripture
		domainsToProcess.add("en_UK_kjv");
		domainsToProcess.add("en_US_eob");
		domainsToProcess.add("en_US_kjv");
		domainsToProcess.add("en_US_net");
		domainsToProcess.add("en_US_nkjv");
		domainsToProcess.add("en_US_rsv");
		domainsToProcess.add("en_US_saas");
		
		// Kenya
		domainsToProcess.add("kik_KE_oak");
		domainsToProcess.add("swh_KE_oak");
		
		// added by Meg
		domainsToProcess.add("fra_FR_oaf");
		domainsToProcess.add("spa_GT_odg");

		/**
		 * Prepare a list to hold the create Queries
		 */
		List<TextLiturgical> textsToCreate = new ArrayList<TextLiturgical>();

		/**
		 * Prepare a List to hold queries we will run to create relationships
		 * between nodes where one node's value points to another node.
		 */
		List<String> redirectQueries = new ArrayList<String>();

		// Update the local repos
		GithubRepositories repos = JGitUtils.getRepositories(alwbPath);
		Map<String, GithubRepo> repoMap = new TreeMap<String, GithubRepo>();
		for (GithubRepo repo : repos.getRepos()) {
			File f = new File(repo.localRepoPath);
			repoMap.put(f.getParent(), repo);
		}

		/**
		 * Now read in all the ares files...
		 */
		System.out.println("Loading Ares files...");
		libProxyManager = new LibraryProxyManager(alwbPath);
		libProxyManager.loadAllLibraryFiles(domainsToProcess);

		/**
		 * Now create a node for each line of each ares file...
		 * 
		 */
		System.out.println("Preparing queries to create nodes and relationships...");
		// We are going to create a node for every line that has a value or is a
		// redirect.
		// We are also going to batch up queries to create VALUE_FROM
		// relationships from
		// nodes whose value is a redirect and the node it redirects to.
		// If the node that is pointed to does not exist, it will not be
		// created.
		int count = 0;
		String repoKey = "";
		try (Driver driver = GraphDatabase.driver("bolt://" + url, AuthTokens.basic(user, pwd))) {
			try (Session session = driver.session()) {
				for (LibraryFileProxy fileProxy : libProxyManager.getLoadedFiles().values()) {
					String fileDomain = fileProxy.getDomain().toLowerCase();
					String fileTopic = fileProxy.getTopic();
					repoKey = "";
					for (String s : repoMap.keySet()) {
						if (fileProxy.getResourcePath().startsWith(s)) {
							GithubRepo r = repoMap.get(s);
							r.setStatus(STATUS.READY_TO_RELEASE);
							repoMap.put(s,  r);
							repoKey = s;
						}
					}
					for (LibraryLine line : fileProxy.getValues()) {
						boolean addToDb = true;
						if (inspectLine) {
							if (fileDomain.equals("en_us_public")
									&& fileTopic.equals("actors")
									&& line.getKey().equals("Candidate_Sponsor")
									) {
								System.out.print("");
							}
						}
						if (line.isSimpleKeyValue || line.isRedirect()) {
							if (useResolvedValues) { // determine whether we will use this with a normalized database
								if (line.getValue() == null || line.getValue().trim().length() < 1) {
									addToDb = false;
								}
							}
							TextLiturgical theNode = new TextLiturgical(
									line.getDomain().toLowerCase()
									, line.getTopic()
									, line.getKey()
									);
							theNode.setSeq(
									fileDomain 
									+ idSeparator 
									+ fileTopic 
									+ idSeparator 
									+ line.getLineNbr()
									);
							if (line.isSimpleKeyValue()) {
								String value = Normalizer.normalize(LibraryUtils.escapeQuotes(line.getValue()),Normalizer.Form.NFC);
								theNode.setValue(value);
								if (includeComment) {
									if (line.hasCommentAfterValue) {
										String comment = line.getComment();
										if (comment != null && comment.length() > 0) {
											theNode.setComment(LibraryUtils.escapeQuotes(comment));
										}
									}
								}
							} else if (line.isRedirect()) {
								String comment = line.getComment();
								if (comment != null && comment.length() > 0) {
									theNode.setComment(LibraryUtils.escapeQuotes(comment));
								}
								String redirectDomain = line.getDomain();
								String redirectTopic = line.getTopic();
								String redirectKey = line.getKey();
								String[] parts = line.getValue().split("_");
								LibraryLine redirectLine = null;
								int partsLength = parts.length;
								switch (partsLength) {
								case 1: {
									valuesWithIds++;
									redirectKey = line.getValue();
									break;
								}
								case 2: {
									valuesWithIds++;
									redirectKey = line.getValue();
									break;
								}
								case 3: {
									valuesWithIds++;
									redirectKey = line.getValue();
									break;
								}
								default: {
									valuesWithIds++;
									redirectTopic = parts[0];
									String redirectRealm = parts[3].substring(0, parts[3].indexOf("."));
									redirectDomain = parts[1] + "_" + parts[2] + "_" + redirectRealm;
									StringBuffer sb = new StringBuffer();
									sb.append(parts[3].substring(parts[3].indexOf(".") + 1, parts[3].length()));
									for (int i = 4; i < partsLength; i++) {
										sb.append("_");
										sb.append(parts[i]);
									}
									redirectKey = sb.toString();
									break;
								}
								}
								redirectLine = libProxyManager
										.getLine(redirectTopic + "_" + redirectDomain, redirectKey);
								if (redirectLine == null) {
									// this value points to a key that does
									// not exist.
									// TODO decide how to handle. For now,
									// we go ahead and create the node,
									// but it won't point to an existing
									// node.
									// You can find these bad nodes using:
									// match (l:LitText) where not
									// exists(l.value) and not
									// (l)-[:VALUE_FROM]->() return l.id,
									// l.value
									System.out.println("Redirect key does not exist: " + line.getTopic() + "_" + line.getDomain() + ".ares: " + line.getLine() + " if exists in Github, did you forget to sudo pullAll.sh?");
									if (useResolvedValues) {
										addToDb = false;
									}
								}
								redirectDomain = redirectDomain.toLowerCase();
								String redirectId = redirectDomain + Constants.ID_DELIMITER + redirectTopic + Constants.ID_DELIMITER + redirectKey;
//								if (parts.length == 5) {
//									redirectId = redirectId + "_" + parts[4];
//								}
								
								if (theNode.getId().equals(redirectId)) {
									System.out.println("Points to self: " + line.getLine());
								} else {
									theNode.setRedirectId(redirectId);
									String nodeRedirect = getRedirectCypherTo(
											theNode.getId()
											, theNode.getOntologyTopic().label
											, redirectId
											);
									if (useResolvedValues) {
										if (redirectLine == null 
												|| redirectLine.getValue() == null 
												|| redirectLine.getValue().length() < 1
												|| (! redirectLine.getDomain().toLowerCase().startsWith(line.getDomain().toLowerCase()))
												) {
											addToDb = false;
										} else {
											addToDb = true;
											String value = Normalizer.normalize(LibraryUtils.escapeQuotes(redirectLine.getValue()),Normalizer.Form.NFC);
											theNode.setValue(value);
										}
									} else {
										redirectQueries.add(nodeRedirect);
									}
								}
								}
							if (addToDb) {
								theNode.setCreatedWhen(Instant.now().toString());
								theNode.setModifiedWhen(theNode.getCreatedWhen());
								theNode.setCreatedBy("wsadmin");
								theNode.setModifiedBy("wsadmin");
								textsToCreate.add(theNode);
							}
						}
					}
				}
				if (repoKey.length() > 0) {
					GithubRepo r = repoMap.get(repoKey);
					r.setStatus(STATUS.RELEASED);
					repoMap.put(repoKey, r);
					try {
						recordPull(session, r);
					} catch (DbException e) {
						ErrorUtils.report(logger, e);
					}
				}
				System.out.println(textsToCreate.size() + " create queries prepared...");
				if (updateDatabaseNodes) {
					System.out.println("Creating " + textsToCreate.size() + " nodes...");
					for (TextLiturgical text : textsToCreate) {
						try {
							if (updateDatabaseNodes) {
								RequestStatus result = insert(session, text);
								count++;
							}
						} catch (Exception e) {
							System.out.println(e.getStackTrace());
						}
					}
				}

				/**
				 * Now create the relationships between nodes where a node's
				 * value is redirected to another node.
				 */
				int relCount = 0;
				System.out
						.println("Merging VALUE_FROM relationships for " + redirectQueries.size() + " nodes...");
				for (String query : redirectQueries) {
					try {
						if (updateDatabaseRelationships) {
							StatementResult result = session.run(query);
							count = result.consume().counters().relationshipsCreated();
							if (count == 0) {
								System.out.println("Did not create relationship: " + query);
							} else {
								relCount++;
							}
						}
					} catch (Exception e) {
						System.out.println(e.getStackTrace());
					}
				}

				System.out.println("Done. Created " + count + " nodes and " + relCount + " relationships...");
			} // try session
		} // try driver
	}
	
	/**
	 * 
	 * @param id the id
	 * @param label the label
	 * @param redirectId the id to redirect to
	 * @return a Cypher query to create a POINTS_TO relationship from this node to the node of the redirectId
	 */
	public static String getRedirectCypherTo(
			String id
			, String label
			, String redirectId
			) {
		if (id.equals(redirectId)) {
			System.out.print("oh no");
		}
		return "MATCH (d:" + label + " {id:'" + id + "'}), (t:"
				+ label + " {id:'" + redirectId + "'}) CREATE (d)-[:VALUE_FROM]->(t) return d";
	}
	
	public static RequestStatus insert(Session session, LTKDb doc) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		setIdConstraints(session, doc.fetchOntologyLabelsList());
		String query = "create (n:" + doc.fetchOntologyLabels() + ") set n = {props} return n";
		try {
			Map<String,Object> props = ModelHelpers.getAsPropertiesMap(doc);
				StatementResult neoResult = session.run(query, props);
				count = neoResult.consume().counters().nodesCreated();
				if (count > 0) {
			    	result.setCode(HTTP_RESPONSE_CODES.CREATED.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.CREATED.message + ": created " + doc.getId());
				} else {
			    	result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message + "  " + doc.getId());
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

	public static RequestStatus recordPull(Session session, GithubRepo doc) throws DbException {
		RequestStatus result = new RequestStatus();
		int count = 0;
		List<String> labels = new ArrayList<String>();
		labels.add("Root");
		labels.add("GithubRepo");
		setIdConstraints(session, labels);
		String query = "create (n:Root:GithubRepo" + ") set n = {props} return n";
		try {
			Map<String,Object> props = ModelHelpers.getAsPropertiesMap(doc);
				StatementResult neoResult = session.run(query, props);
				count = neoResult.consume().counters().nodesCreated();
				if (count > 0) {
			    	result.setCode(HTTP_RESPONSE_CODES.CREATED.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.CREATED.message + ": created " + doc.getId());
				} else {
			    	result.setCode(HTTP_RESPONSE_CODES.BAD_REQUEST.code);
			    	result.setMessage(HTTP_RESPONSE_CODES.BAD_REQUEST.message + "  " + doc.getId());
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

	public static void setIdConstraints(Session session, List<String> labels) {
		try {
			for (String label : labels) {
				if (! AresToNeo4j.constraints.contains(label)) {
					setIdConstraint(session, label); 
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * The purpose of this method is to ensure that any node with 
	 * an id property has a unique constraint so that duplicate IDs 
	 * are not allowed.
	 * @param label
	 * @return
	 */
	private static StatementResult setIdConstraint(Session session, String label) {
		StatementResult neoResult = null;
			String query = "create constraint on (p:" + label + ") assert p.id is unique"; 
			try  {
				neoResult = session.run(query);
				AresToNeo4j.constraints.add(label);
			} catch (Exception e) {
				ErrorUtils.report(logger, e);
			}
		return neoResult;
	}
	
	private static List<File> gitRepos(String path) {
		List<File> result = new ArrayList<File>();
		for (File f :  org.ocmc.ioc.liturgical.utils.FileUtils.getFilesFromSubdirectories(path, ".git")) {
			result.add(f);
		}
		return result;
	}

}

