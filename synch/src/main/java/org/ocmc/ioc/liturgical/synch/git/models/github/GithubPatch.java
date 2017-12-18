package org.ocmc.ioc.liturgical.synch.git.models.github;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;
import org.ocmc.ioc.liturgical.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;

public class GithubPatch extends AbstractModel {
	
	private static final Logger logger = LoggerFactory.getLogger(GithubPatch.class);
	
	@Expose public String fromSha = "";
	@Expose public CommitDate commitDate = new CommitDate();
	@Expose public String fromEmail = "";
	@Expose public String fromDate = "";
	@Expose public String subject = "";
	@Expose public List<String> lines = new ArrayList<String>();
	private String patch = "";
	
	public GithubPatch(String patch) {
		super();
		this.patch = patch;
		this.parsePatch();
	}
	
	private void parsePatch() {
		try {
			String [] lines = this.patch.split("\n");
			for (String line : lines) {
				if (line.length() > 0) {
					if (line.startsWith("From ")) {
						String [] parts = line.trim().split(" ");
						if (parts.length == 7) {
							this.fromSha = parts[1];
							this.commitDate.setDayName(parts[2]);
							this.commitDate.setMonth(parts[3]);
							this.commitDate.setDay(parts[4]);
							this.commitDate.setTime(parts[5]);
							this.commitDate.setYear(parts[6]);
						}
					} else if (line.startsWith("From: ")) {
						this.fromEmail = line.substring(6);
					} else if (line.startsWith("Date: ")) {
						this.fromDate = line.substring(6);
					} else if (line.startsWith("Subject: ")) {
						this.fromDate = line.substring(9);
					} else { 
						this.lines.add(line);
					}
				}
			}

		} catch (Exception e) {
			ErrorUtils.report(logger, e);
		}
	}

}
