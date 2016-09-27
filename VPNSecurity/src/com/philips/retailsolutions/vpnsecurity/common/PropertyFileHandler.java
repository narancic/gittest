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
 * \file PropertyFileHandler.java
 * \brief
 * Base class for property fetching.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for property fetching from property file.
 */
public class PropertyFileHandler {
	
	private Logger logger = LoggerFactory.getLogger(PropertyFileHandler.class);
	private Properties defaultProps = null;
	private String file = null;
	
	/**
	 * Opens property file in a constructor
	 * @param file property file path
	 */
	public PropertyFileHandler(String file) throws ConfigurationException {
		if (file != null) {
			openPropertyFile(file);
		}
	}

	/**
	 * Opens provided property file.
	 * @param file property file path
	 * @throws ConfigurationException
	 */
	public final void openPropertyFile(String file) throws ConfigurationException {
		this.file = file;
		try {
			defaultProps = new Properties();
			URL url = getFileURL(file);
			InputStream fileInputStream;
		
			fileInputStream = url.openStream();
	        defaultProps.load(fileInputStream);
	        fileInputStream.close();
		} catch (Exception e) {
			logger.error("Failed to open property file: " + file);
			throw(new ConfigurationException(e.getMessage(), e.getStackTrace()));
		}
        logger.info("Openned and loaded property file: " + file);
	}
	
	/**
	 * Updates property file with new values
	 * @return true on success/false otherwise
	 */
	public final synchronized boolean updatePropertyFile() {
		if (file == null) {
			logger.info("Updating application property file failed. Missing file path...");
			return false;
		}
	    
	    FileOutputStream fileOutputStream;
		try {
			File configFile = new File(file);
			fileOutputStream = new FileOutputStream(configFile);
	        defaultProps.store(fileOutputStream, null);
	        fileOutputStream.flush();
	        fileOutputStream.close();
	        return true;
		} catch (IOException e) {
			logger.error("Updating " + file + " property file failed. " + e.getMessage());
			return false;
		}
	}
	
    /**
     * Get URL of the property file
     * @param path file path
     * @return URL path to the file to be used
     */
	public final URL getFileURL(String path) {
		URL filePath = null;
        try {
           	File convert = new File(path);
            filePath = convert.toURI().toURL();  
        } catch (MalformedURLException e) {    
        }
        return filePath;
    }
	
	/**
	 * Get specified property from file as string value
	 * @param propertyName Name of the property to get
	 * @return Property read from file
	 */
	public final String getPropertyAsString(String propertyName) {
		return defaultProps.getProperty(propertyName);
	}
	
	/**
	 * Get specified property from file as integer value
	 * @param propertyName Name of the property to get
	 * @return Property read from file as integer
	 */
	public final Integer getPropertyAsInteger(String propertyName) {
		try {
			String val = defaultProps.getProperty(propertyName);
			return Integer.valueOf(val);
		} catch(Exception e) {
			return null;
		}
	}
	
	/**
	 * Get specified property from file as Long value
	 * @param propertyName Name of the property to get
	 * @return Property read from file as long
	 */
	public final Long getPropertyAsLong(String propertyName) {
		try {
			String val = defaultProps.getProperty(propertyName);
			return Long.valueOf(val);
		} catch(Exception e) {
			return null;
		}
	}

	/**
	 * Set specified property to configuration file as integer value
	 * @param propertyName Name of the property to get
	 * @param value property value (as Integer)
	 * @return true on success / false otherwise
	 */
	public final boolean setPropertyAsInteger(String propertyName, int value) {
		String strValue = "" + value;
		defaultProps.setProperty(propertyName, strValue);
		return updatePropertyFile();
	}
	
	/**
	 * Set specified property to configuration file as string value
	 * @param propertyName Name of the property to get
	 * @param value property value (as String)
	 * @return true on success / false otherwise
	 */
	public final boolean setPropertyAsString(String propertyName, String value) {
		defaultProps.setProperty(propertyName, value);
		return updatePropertyFile();
	}
}
