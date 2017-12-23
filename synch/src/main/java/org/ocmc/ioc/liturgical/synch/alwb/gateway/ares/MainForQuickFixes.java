package org.ocmc.ioc.liturgical.synch.alwb.gateway.ares;

/**
 * This is class with a main, so it can be run by developers
 * to do various tasks.  It is not run from the TMS via the end user.
 * 
 * @author mac002
 *
 */
public class MainForQuickFixes {

	public static void main(String[] args) {
		String from = "/Staging/In/From/en_US_constantinides/Books-Collections/Heirmologion/Heirmoi/he.h.m1_en_US_constantinides.ares";
		String into = "";
		LibraryUtils.merge(from, into);
	}

}
