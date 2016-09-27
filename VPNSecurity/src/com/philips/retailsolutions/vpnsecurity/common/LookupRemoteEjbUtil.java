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
 * @file LookupRemoteEjbUtil.java
 *
 * @brief
 * Class which provides static utility methods to lookup for all remote EJB's that are used
 * in application. By using this class, lookups need to be updated on one place only.
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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.hornetq.core.remoting.impl.netty.TransportConstants;

/**
 * Class which provides static utility methods to lookup for all remote EJB's that are used
 * in application. By using this class, lookups need to be updated on one place only.
 */
public final class LookupRemoteEjbUtil {

	/**
	 * Naming factory for EJB/queue lookup
	 */
	public static final String CONTEXT = "org.jnp.interfaces.NamingContextFactory";
	/**
	 * URL package prefix for queue lookup
	 */
	public static final String URL_PKG_PREFIXES = "org.jboss.naming";	
	/**
	 * EM server remote host property name
	 */
	private static final String EM_SERVER_HOST = "EM_SERVER_HOST";
	/**
	 * EM server remote port property name
	 */
	private static final String EM_SERVER_PORT = "EM_SERVER_PORT";
	/**
	 * EM server default host value, used in the case when no defined host in property file
	 */
	private static final String EM_DEFAULT_HOST = "localhost";
	/**
	 * EM server default port value, used in the case when no defined port in property file
	 */
	private static final String EM_DEFAULT_PORT = "1099";
	/**
	 * EM server remote JBoss port property name
	 */
	private static final String EM_JBOSS_PORT = "EM_JBOSS_PORT";
	
	/**
	 * Hidden constructor
	 */
	private LookupRemoteEjbUtil() {}
	
	/**
	 * Look up for the remote EJB bean and return it 
	 * @return - bean found
	 * @throws NamingException - in case when unable to find bean
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <IF> IF lookupEJB(Class c, String remote) throws NamingException  {
		IF managerEJB;
		Context	initialContext = getInitialContext();
		Object objref = initialContext.lookup(remote);
		managerEJB = (IF) PortableRemoteObject.narrow(objref, c);
		return managerEJB;
	}
	
	/**
	 * Create and get initial context
	 * @return - created initial context
	 * @throws NamingException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static InitialContext getInitialContext() throws NamingException {
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT);
		env.put(Context.PROVIDER_URL, getLookupHost());
		env.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
		return new InitialContext(env);
	}
	
	/**
	 * Get map with JMS connection parameters
	 * @return - connection parameters
	 */
	public static Map<String, Object> getJMSConnectionParams() {
		Map<String, Object> connectionParams = new HashMap<String, Object>();
		connectionParams.put(TransportConstants.PORT_PROP_NAME, 5455);
		connectionParams.put(TransportConstants.HOST_PROP_NAME, getHostName());
		return connectionParams;
	}
	
	/**
	 * Get EM server host URL from property file
	 * @return - host URL  (http://HOST_NAME or https://HOST_NAME)
	 */
	public static String getHostURL() {
		try {
			PropertyFileHandler pfHandler = new PropertyFileHandler(CommonConstants.PROPERTY_FILE_PATH);
			String host = pfHandler.getPropertyAsString(EM_SERVER_HOST);
			if (host != null) {
				return host;
			}
			return "http://" + EM_DEFAULT_HOST;
		} catch (ConfigurationException e) {
			e.printStackTrace();
			/* return default values */
			return "http://" + EM_DEFAULT_HOST;
		}
	}
	
	/**
	 * Get EM server host name from property file
	 * @return - host name (without http, https) 
	 */
	public static String getHostName() {
		String host = getHostURL();
		if (host != null) {
			int i = host.indexOf("://");
			if (i > 0) {
				host = host.substring(i+3);
			}
			return host;
		} else {
			return EM_DEFAULT_HOST;
		}		
	}
	
	/**
	 * Get JBoss port from property file, to access the EM servlets
	 * @return - JBoss port from property file, if not defined return empty string
	 */
	public static String getJBossPort() {
		try {
			PropertyFileHandler pfHandler = new PropertyFileHandler(CommonConstants.PROPERTY_FILE_PATH);
			String port = pfHandler.getPropertyAsString(EM_JBOSS_PORT);
			if (port != null) {
				return ":" + port;
			}
			return "";
		} catch (ConfigurationException e) {
			e.printStackTrace();
			/* return default values */
			return "";
		}
	}
	
	/**
	 * Get EM server URL for JMS connection - "jnp://host_name:port" from property file
	 * @return - URL with host name and port in format: "jnp://EM_HOST_NAME:EM_PORT"
	 */
	private static String getLookupHost() {		
		try {
			PropertyFileHandler pfHandler = new PropertyFileHandler(CommonConstants.PROPERTY_FILE_PATH);
			String port = pfHandler.getPropertyAsString(EM_SERVER_PORT);
			String url = "jnp://" + getHostName();
			if (port != null  &&  port.length() > 0) {
				url += ":" + port;
			} else {
				url += ":" + EM_DEFAULT_PORT;
			}
			return url;
		} catch (ConfigurationException e) {
			e.printStackTrace();
			/* return default values */
			return "jnp://" + EM_DEFAULT_HOST + ":" + EM_DEFAULT_PORT;
		}
	}

	/**
	 * Get external upload servlet path from the property file
	 * @return External upload servlet URL
	 */
	public static String getExternalUploadServletPath() {
		try {
			PropertyFileHandler pfHandler = new PropertyFileHandler(CommonConstants.PROPERTY_FILE_PATH);
			return pfHandler.getPropertyAsString("EM_EXT_UPLOAD_SERVLET");
		} catch (ConfigurationException e) {
			e.printStackTrace();
			return null;
		}
	}
}
