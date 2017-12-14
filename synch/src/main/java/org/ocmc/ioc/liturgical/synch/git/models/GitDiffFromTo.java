package org.ocmc.ioc.liturgical.synch.git.models;

import java.util.ArrayList;
import java.util.List;

public class GitDiffFromTo {
	public enum TYPE {
		ADD
		, UPDATE
		, DELETE
		, UNKNOWN
	}
	public enum SIGN {
		MINUS
		, MINUS_MINUS
		, PLUS
		, PLUS_PLUS
		, OTHER
	}
	private String from = "";
	private String to = "";
	private String fromKey = "";
	private String toKey = "";
	private SIGN fromSign = SIGN.OTHER;
	private String fromText = "";
	private String toText = "";
	private SIGN toSign = SIGN.OTHER;
	private TYPE type = TYPE.UNKNOWN;
	
	public GitDiffFromTo(String from, String to) {
		this.from = from;
		this.to = to;
		this.process();
	}
	
	public TYPE getType() {
		return this.type;
	}

	private void process() {
		List<String> keyValue = null;
		StringBuffer typeTest = new StringBuffer();
		if (this.from.length() > 0) {
			keyValue = this.keyValue(this.from);
			if (keyValue.size() == 2) {
				this.fromKey = keyValue.get(0);
				this.fromText = keyValue.get(1);
				if (this.from.startsWith("++")) {
					this.fromSign = SIGN.PLUS_PLUS;
					typeTest.append("++");
				} else if (this.from.startsWith("+")) {
					this.fromSign = SIGN.PLUS;
					typeTest.append("+");
				} else if (this.from.startsWith("--")) {
					this.fromSign = SIGN.MINUS_MINUS;
					typeTest.append("--");
				} else if (this.from.startsWith("-")) {
					this.fromSign = SIGN.MINUS;
					typeTest.append("-");
				}
			}
		}
		typeTest.append(":");
		if (this.to.length() > 0) {
			keyValue = this.keyValue(this.to);
			if (keyValue.size() == 2) {
				this.toKey = keyValue.get(0);
				this.toText = keyValue.get(1);
				if (this.to.startsWith("++")) {
					this.toSign = SIGN.PLUS_PLUS;
					typeTest.append("++");
				} else if (this.to.startsWith("+")) {
					this.toSign = SIGN.PLUS;
					typeTest.append("+");
				} else if (this.to.startsWith("--")) {
					this.toSign = SIGN.MINUS_MINUS;
					typeTest.append("--");
				} else if (this.to.startsWith("-")) {
					this.toSign = SIGN.MINUS;
					typeTest.append("-");
				}
			}
		}
		switch (typeTest.toString()) {
		case (":++"): {
			break;
		}
		case (":+"): {
			this.type = TYPE.ADD;
			break;
		}
		case ("++:++"): {
			break;
		}
		case ("++:--"): {
			break;
		}
		case ("+:+"): {
			break;
		}
		case ("+:-"): {
			break;
		}
		case ("--:--"): {
			break;
		}
		case ("--:++"): {
			break;
		}
		case ("-:-"): {
			this.type = TYPE.DELETE;
			break;
		}
		case ("-:+"): {
			this.type = TYPE.UPDATE;
			break;
		}
		}
	}
	
	private List<String> keyValue(String change) {
		List<String> result = new ArrayList<String>();
		try {
			String [] parts = change.split(" = ");
			if (parts.length == 2) {
				result.add(parts[0].substring(1).trim());
				result.add(parts[1].trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFromKey() {
		return fromKey;
	}

	public void setFromKey(String fromKey) {
		this.fromKey = fromKey;
	}

	public String getToKey() {
		return toKey;
	}

	public void setToKey(String toKey) {
		this.toKey = toKey;
	}

	public String getFromText() {
		return fromText;
	}

	public void setFromText(String fromText) {
		this.fromText = fromText;
	}

	public String getToText() {
		return toText;
	}

	public void setToText(String toText) {
		this.toText = toText;
	}
	
}
