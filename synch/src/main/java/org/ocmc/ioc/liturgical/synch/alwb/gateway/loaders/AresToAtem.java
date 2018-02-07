package org.ocmc.ioc.liturgical.synch.alwb.gateway.loaders;

import java.io.File;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	
	public static void main(String[] args) {

		// Load the ares
		LibraryProxyManager libProxyManager;
		String alwbPath = "/Users/mac002/git/ages/ares/dcs/ages-alwb-library-gr-gr-cog/net.ages.liturgical.workbench.library_gr_GR_cog/Books-Collections";
		String pathOut = "/Users/mac002/temp/";
		File out = new File(pathOut);
		out.mkdirs();
		List<String> domainsToProcess = new ArrayList<String>();
		domainsToProcess.add("gr_GR_cog");
		System.out.println("Loading Ares files...");
		libProxyManager = new LibraryProxyManager(alwbPath);
		libProxyManager.loadAllLibraryFiles(domainsToProcess);
		for (LibraryFileProxy fileProxy : libProxyManager.getLoadedFiles().values()) {
			String resource = fileProxy.getResourceName(); // actors_gr_GR_cog
			if (resource.startsWith("me.") || resource.startsWith("tr.") ) {
				System.out.println(resource);
				String [] parts = resource.split("_");
				String resourceTopic = parts[0];
				parts = resourceTopic.split("\\.");
				if (resource.startsWith("me.")) {
					out = new File(pathOut + "/" + parts[0] + "/" + parts[1] + "/" + parts[2]);
				} else {
					out = new File(pathOut + "/" + parts[0] + "/" + parts[1]);
					
				}
				out.mkdirs();
				StringBuffer fileLines = new StringBuffer();
				fileLines.append("Template ");
				fileLines.append(resourceTopic);
				fileLines.append("\n\nStatus Final\n\n");
				List<String> topics = new ArrayList<String>();
				StringBuffer sids = new StringBuffer();
				for (Entry<String, String> entry : fileProxy.linesByLineNbr.entrySet()) {
					LibraryLine line = fileProxy.getLibraryLine(entry.getValue());
					String key = line.getKey();
					if (line.isSimpleKeyValue || line.valueIsKey && (! key.contains("Resource_Whose_Name"))) {
						String tag = "Para sid ";
						String endTag = " End-Para";
						if (
								key.startsWith("meVE")
								 ||	key.startsWith("meCO")
								 ||	key.startsWith("meMA")
								 ||	key.startsWith("meH1")
								 ||	key.startsWith("meH3")
								 ||	key.startsWith("meH6")
								 ||	key.startsWith("meH9")
								 ||	key.startsWith("meSV")
								 ||	key.startsWith("trSV")
								 ||	key.startsWith("peSV")
								) {
							if (key.endsWith(".poet")
									) {
								tag = "Title<inr>sid ";
								endTag = " End-Title";
							} else if (key.endsWith(".ode")) {
								tag = "Title<Tdesig> sid ";
								endTag = " End-Title";
							} else if (key.endsWith(".mode")) {
								tag = "Title<Tmode> sid ";
								endTag = " End-Title";
							} else if (key.endsWith(".melody")) {
								tag = "Title<Tmelody> sid ";
								endTag = " End-Title";
							} else if (key.endsWith(".text")) {
								tag = "Hymn sid ";
								endTag = " End-Hymn\n";
							} else {
								tag = "Para sid ";
								endTag = " End-Para";
							}
						}
						// need to handle valuesIsKey
						if (! topics.contains(line.getTopic())) {
							topics.add(line.getTopic());
						}
						sids.append(tag);
						sids.append(line.getKey());
						sids.append(endTag);
						sids.append("\n");
					} else {
						sids.append("\n");
					}
				}
				for (String topic : topics) {
					fileLines.append("import ");
					fileLines.append(topic);
					fileLines.append("_gr_GR_cog.*\n");
				}
				fileLines.append("\n");
				fileLines.append(sids.toString());
				fileLines.append("\nEnd-Template");
				out = new File(out.getAbsolutePath() + "/" + resource + ".atem");
				org.ocmc.ioc.liturgical.utils.FileUtils.writeFile(out.getAbsolutePath(), fileLines.toString());
			}
		}
		System.out.println("Done...");
	}

}

