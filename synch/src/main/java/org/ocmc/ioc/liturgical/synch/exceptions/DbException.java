package org.ocmc.ioc.liturgical.synch.exceptions;

/***
 * Acts as a wrapper to an underlying database exception, e.g. SQL Exception
 * @author mac002
 *
 */
public class DbException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DbException(String message) {
        super(message);
    }

    public DbException(String message, Throwable throwable) {
        super(message, throwable);
    }

}