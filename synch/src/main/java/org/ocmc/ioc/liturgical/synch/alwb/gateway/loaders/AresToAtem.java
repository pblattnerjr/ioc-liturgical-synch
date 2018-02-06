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


public class AresToAtem {
	private static final Logger logger = LoggerFactory
			.getLogger(AresToAtem.class);

	private static List<String> constraints = new ArrayList<String>();

	private static void pullFromGitHub(String rootPath) {
		
	}
	
	public static void main(String[] args) {

		boolean updateDatabaseNodes = true; 
		boolean useResolvedValues = true; // if true, will be used as read-only database
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
		String alwbPath = "/Users/mac002/git/ages/ares/dcs";
		List<String> domainsToProcess = new ArrayList<String>();
		domainsToProcess.add("gr_GR_cog");
		System.out.println("Loading Ares files...");
		libProxyManager = new LibraryProxyManager(alwbPath);
		libProxyManager.loadAllLibraryFiles(domainsToProcess);
		for (LibraryFileProxy fileProxy : libProxyManager.getLoadedFiles().values()) {
			String resource = fileProxy.getResourceName(); // actors_gr_GR_cog
			if (resource.startsWith("me.")) {
				StringBuffer fileLines = new StringBuffer();
				List<String> topics = new ArrayList<String>();
				StringBuffer sids = new StringBuffer();
				for (LibraryLine line : fileProxy.getValues()) {
					if (line.isSimpleKeyValue || line.valueIsKey) {
						// need to handle valuesIsKey
						if (! topics.contains(line.getTopic())) {
							topics.add(line.getTopic());
						}
						sids.append("Para sid ");
						sids.append(line.getKey());
						sids.append(" End-para");
						sids.append("\n");
					}
				}
				for (String topic : topics) {
					fileLines.append("import ");
					fileLines.append(topic);
					fileLines.append("_gr_GR_cog.*;\n");
				}
				fileLines.append(sids.toString());
				System.out.println(fileLines.toString());
			}
		}
	}

}

