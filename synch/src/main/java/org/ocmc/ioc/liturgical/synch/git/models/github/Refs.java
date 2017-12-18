package org.ocmc.ioc.liturgical.synch.git.models.github;

import java.util.ArrayList;
import java.util.List;

import org.ocmc.ioc.liturgical.schemas.models.supers.AbstractModel;

import com.google.gson.annotations.Expose;

public class Refs extends AbstractModel {
	
	@Expose List<Ref> refs = new ArrayList<Ref>();
	
	public Refs() {
		super();
	}

	public List<Ref> getRefs() {
		return refs;
	}

	public void setRefs(List<Ref> refs) {
		this.refs = refs;
	}
	
	public void addRef(Ref ref) {
		this.refs.add(ref);
	}
	
	public Ref getMaster() {
		Ref result = new Ref();
		for (Ref ref : this.refs) {
			if (ref.getRef().equals("refs/heads/master")) {
				result = ref;
				break;
			}
		}
		return result;
	}
	
	public String getMasterSha() {
		Ref master = this.getMaster();
		if (master != null) {
			return this.getMaster().getObject().getSha();
		} else {
			return "";
		}
	}
}
