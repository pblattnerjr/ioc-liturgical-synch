package org.ocmc.ioc.liturgical.synch.git.models.github;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class CommitDate extends AbstractModel {

	@Expose public String dayName = "";
	@Expose public String month = "";
	@Expose public String day = "";
	@Expose public String time = "";
	@Expose public String year = "";
	
	public CommitDate() {
		super();
	}

	public String getDayName() {
		return dayName;
	}

	public void setDayName(String dayName) {
		this.dayName = dayName;
	}

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

}
