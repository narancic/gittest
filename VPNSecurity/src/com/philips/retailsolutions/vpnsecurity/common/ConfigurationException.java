/***************************************************************************
 *
 *
 *  Copyright (c) 2009 Philips Lighting BV
 *  Building EEAp 019, PO Box 80020, 5600JM, Eindhoven, The Netherlands
 *
 *  All Rights Reserved
 *
 *  P R O P R I E T A R Y    &    C O N F I D E N T I A L
 *
 *  -----------------------------------------------------
 *  http://www.Philips.com
 *  -----------------------------------------------------
 *
 * \file ConfigurationException.java
 * \brief
 * Custom exception thrown when there are problems with ExperienceManagerReporting configuration file
 * 
 * Created on 27 Nov 2009
 *
 * @Author Dejan Stefanovic
 * \notes
 * 
 * \history
 * 27 Nov 2009 Initial version
 */

package com.philips.retailsolutions.vpnsecurity.common;

/**
 * Custom exception thrown when there are problems with configuration file
 */
@SuppressWarnings("serial")
public class ConfigurationException extends Exception {
	private StackTraceElement[] stackTrace = null;
	
	/**
	 * Class constructor
	 * Creates exception with stub message
	 */
	public ConfigurationException() {
		super("Configuration file invalid or may not be found.");
	}
	
	/**
	 * Class constructor
	 * Creates exception with provided message
	 * @param msg Message to shown to whom ever catches this exception
	 */
	public ConfigurationException(String msg) {
		super(msg);
	}
		
	/**
	 * Class constructor
	 * Creates exception with provided message
	 * @param msg Message to shown to whom ever catches this exception
     * @param stackTrace stacke trace 
	 */
	public ConfigurationException(String msg, StackTraceElement[] stackTrace) {
		super(msg);
			
		if (stackTrace != null && stackTrace.length > 0) {
			this.stackTrace = new StackTraceElement [stackTrace.length];
			for (int i=0;i<stackTrace.length;i++) {
				this.stackTrace[i] = stackTrace[i];
			}
		}
	}

	/**
	 * Retrieves stack trace (if provided)
	 * @return
	 */
	public StackTraceElement[] getCustomStackTrace() {
		return stackTrace;
	}
}
