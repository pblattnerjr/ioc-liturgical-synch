package org.ocmc.ioc.liturgical.synch.alwb.gateway.utils;

/**
 * Simple class to hold two values, e.g. current and previous, or whatever
 * @author mac002
 *
 */
public class Tuple {
	String left;
	String right;
	
	public Tuple() {
		
	}
	
	public Tuple(String left, String right) {
		this.left = left;
		this.right = right;
	}
	
	public String getLeft() {
		return left;
	}
	public void setLeft(String left) {
		this.left = left;
	}
	public String getRight() {
		return right;
	}
	public void setRight(String right) {
		this.right = right;
	}
	
	/**
	 * Compares the values of the left and right string
	 * @return true if the values are the same
	 */
	public boolean sameValue() {
		return (left.compareTo(right) == 0);
	}
	
}
