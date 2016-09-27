/***************************************************************************
 *
 *  Copyright (c) 2012 Philips Lighting BV
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
 * @file CommonConstants.java
 *
 * @brief
 *  
 * Created on 17.08.2012.
 *
 * @author Dragan Narancic
 * 
 * @history
 * 17.08.2012. Initial version
 * 
 */
package com.philips.retailsolutions.vpnsecurity.common;

/**
 * Constants used in the VPN
 */
public final class CommonConstants {

	/**
	 * Hidden default constructor
	 */
	private CommonConstants() {
	}

	/**
	 * Path to the log4j configuration file
	 */
	public static String LOG_PROPERTIES_PATH = "/etc/VPNSecurity/cfg/log4j.properties";
	/**
	 * Path to the property file
	 */
	public static final String PROPERTY_FILE_PATH = "/etc/VPNSecurity/cfg/vpnsecurity.properties";

	/**
	 * JMS Connection factory
	 */
	public static final String CF_LOOKUP_NAME = "/ConnectionFactory";
	/**
	 * Name of the queue to receive messages for starting reports
	 */
	public static final String VPN_COMMAND_QUEUE = "vpnFirewallQueue";
	
	/**
	 * Name of the queue to receive messages for starting reports
	 */
	public static final String VPN_COMMAND_RESPONSE_QUEUE = "vpnFirewallResponseQueue";

	/**
	 * Default value for max allowed number of parallel processing
	 */
	public static final int MAX_PARALLEL_PROCESSING_DEFAULT = 5;

	/**
	 * Default prefix for composed log name
	 */
	public static final String LOG_FILE_PREFIX = "ExperiencePlayer_log_";

	/**
	 * Waiting period between checks for disconnected VPN users
	 */

	public static final long MONITOR_THREAD_PERIOD = 5000;

	/**
	 * Limit of connection losses before connection is broken
	 * 
	 */
	public static final int CONNECTION_LOSS_LIMIT = 10;
}
