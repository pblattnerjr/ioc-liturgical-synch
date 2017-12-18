package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

/**
 * This is an item in a json array returned from:
 * https://api.github.com/repos/:owner/:repo/git/trees/master?recursive=1
 * @author mac002
 *
 */
public class GitFileInfo extends AbstractModel {
  @Expose public String	path = ""; // e.g. "net.ages.liturgical.workbench.library_gr_GR_cog/Properties/client_gr_GR_cog.ares",
  @Expose public String	mode = ""; // e.g. "100644",
  @Expose public String	type = ""; // e.g. "blob"
  @Expose public String	sha = ""; // e.g.  "3cc97fbf090b73c589ae58d9b6b0ea307d4d2363",
  @Expose public int	size = 0; // e.g. 15534
  @Expose public String	url = ""; // e.g. "https://api.github.com/repos/AGES-Initiatives/ages-alwb-library-gr-gr-cog/git/blobs/3cc97fbf090b73c589ae58d9b6b0ea307d4d2363"

  public GitFileInfo() {
		super();
	}

public String getPath() {
	return path;
}

public void setPath(String path) {
	this.path = path;
}

public String getMode() {
	return mode;
}

public void setMode(String mode) {
	this.mode = mode;
}

public String getType() {
	return type;
}

public void setType(String type) {
	this.type = type;
}

public String getSha() {
	return sha;
}

public void setSha(String sha) {
	this.sha = sha;
}

public int getSize() {
	return size;
}

public void setSize(int size) {
	this.size = size;
}

public String getUrl() {
	return url;
}

public void setUrl(String url) {
	this.url = url;
}

}
