package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class Person extends AbstractModel {
    @Expose public String gists_url = ""; // "https://api.github.com/users/mcolburn/gists{/gist_id}",
    @Expose public String repos_url = ""; //"https://api.github.com/users/mcolburn/repos",
    @Expose public String following_url = ""; //"https://api.github.com/users/mcolburn/following{/other_user}",
    @Expose public String starred_url = ""; //"https://api.github.com/users/mcolburn/starred{/owner}{/repo}",
    @Expose public String login = ""; //"mcolburn",
    @Expose public String followers_url = ""; //"https://api.github.com/users/mcolburn/followers",
    @Expose public String type = ""; //User",
    @Expose public String url = ""; //https://api.github.com/users/mcolburn",
    @Expose public String subscriptions_url = ""; //"https://api.github.com/users/mcolburn/subscriptions",
    @Expose public String received_events_url = ""; //"https://api.github.com/users/mcolburn/received_events",
    @Expose public String avatar_url = ""; //"https://avatars3.githubusercontent.com/u/1334217?v=4",
    @Expose public String events_url = ""; //"https://api.github.com/users/mcolburn/events{/privacy}",
    @Expose public String html_url = ""; //"https://github.com/mcolburn",
    @Expose public String site_admin = ""; //false,
    @Expose public String id = ""; // 1334217,
    @Expose public String gravatar_id = ""; //"",
    @Expose public String organizations_url = ""; // "https://api.github.com/users/mcolburn/orgs"

	public Person() {
		super();
	}

	public String getGists_url() {
		return gists_url;
	}

	public void setGists_url(String gists_url) {
		this.gists_url = gists_url;
	}

	public String getRepos_url() {
		return repos_url;
	}

	public void setRepos_url(String repos_url) {
		this.repos_url = repos_url;
	}

	public String getFollowing_url() {
		return following_url;
	}

	public void setFollowing_url(String following_url) {
		this.following_url = following_url;
	}

	public String getStarred_url() {
		return starred_url;
	}

	public void setStarred_url(String starred_url) {
		this.starred_url = starred_url;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getFollowers_url() {
		return followers_url;
	}

	public void setFollowers_url(String followers_url) {
		this.followers_url = followers_url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSubscriptions_url() {
		return subscriptions_url;
	}

	public void setSubscriptions_url(String subscriptions_url) {
		this.subscriptions_url = subscriptions_url;
	}

	public String getReceived_events_url() {
		return received_events_url;
	}

	public void setReceived_events_url(String received_events_url) {
		this.received_events_url = received_events_url;
	}

	public String getAvatar_url() {
		return avatar_url;
	}

	public void setAvatar_url(String avatar_url) {
		this.avatar_url = avatar_url;
	}

	public String getEvents_url() {
		return events_url;
	}

	public void setEvents_url(String events_url) {
		this.events_url = events_url;
	}

	public String getHtml_url() {
		return html_url;
	}

	public void setHtml_url(String html_url) {
		this.html_url = html_url;
	}

	public String getSite_admin() {
		return site_admin;
	}

	public void setSite_admin(String site_admin) {
		this.site_admin = site_admin;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGravatar_id() {
		return gravatar_id;
	}

	public void setGravatar_id(String gravatar_id) {
		this.gravatar_id = gravatar_id;
	}

	public String getOrganizations_url() {
		return organizations_url;
	}

	public void setOrganizations_url(String organizations_url) {
		this.organizations_url = organizations_url;
	}

}
