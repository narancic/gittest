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
 * \file MessageProducer.java
 * \brief
 * 
 * This class represents a message producer, which queues 
 * messages in the queue to be processed by MDBs.
 *  
 * Created on 1 Feb 2012
 *
 * @Author Dragan Narancic
 * \notes
 * 
 * \history
 * 1 Feb 2012 Initial version
 * 08 Mar 2012 Bug fix 1676 (internal) implemented
 */
package com.philips.retailsolutions.vpnsecurity.messaging;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.philips.retailsolutions.vpnsecurity.common.LookupRemoteEjbUtil;


/**
 * This class represents a message producer, which queues messages in the remote JMS queue
 * on external server (ExperienceManager) to be processed by servers' MDBs.
 */
public class MessageSender {

	private static final Long NO_ERROR = 0L;
	private static final Long ERROR_SEND_MESSAGE = 1L;
	/* JMS resources */
	private InitialContext initialContext = null;
	private Session session = null;
	private MessageProducer producer = null;
	/* Message */
	private Message message = null;
	private String qName;
	
	private MessageCenter messageCenter;
	
	private static Logger log = Logger.getLogger(MessageSender.class);
	
	/**
	 * Constructor
	 * @param mc - message center instance to set
	 * @param queueName - name of the queue to send the message
	 * @throws JMSException 
	 * @throws NamingException 
	 */
	public MessageSender(MessageCenter mc, String queueName) throws NamingException, JMSException {
		messageCenter = mc;
		qName = queueName;
		createConnection(queueName);
	}
	
	/**
	 * Create JMS resources (connection, queue, session, producer)
	 * @param queueName - name of the queue
	 * @throws NamingException - on failure to create initial context
	 * @throws JMSException - on failure to create JMS resources
	 */
	private void createConnection(String queueName) throws NamingException, JMSException {
		if (initialContext == null) {
			log.info("Creating a new initial context...");
	        // Step 1. Create an initial context to perform the JNDI lookup.
	        initialContext = LookupRemoteEjbUtil.getInitialContext();
		}
		// Perform a lookup on the queue
		Queue queue = (Queue)initialContext.lookup("/queue/" + queueName);
		session = messageCenter.getSession();
		
		// Create a JMS Message Producer
		producer = session.createProducer(queue);
	}
	
	/**
	 * Send the message to the queue
	 * @param messageBody - message body
	 * @param messageType - message type (String, Long, Integer...)
	 * @return - error code represented by @link ErrorCodes constants
	 */
	@SuppressWarnings("rawtypes")
	public Long sendMessage(Object messageBody, Class messageType) {
		Long errorCode = null;
		int retry = 0;
		while (retry < 2) {
			try {
		        message = createMessage(session, messageBody, messageType);	        
		        producer.send(message);
		        log.info("The message is queued to: " + qName);
		        errorCode = NO_ERROR;
		        retry = 2;
			} catch (JMSException e) {
				log.error(e.getMessage());
				errorCode = ERROR_SEND_MESSAGE;
				try {
					createConnection(qName);
				} catch (NamingException e1) {
					e1.printStackTrace();
				} catch (JMSException e1) {
					e1.printStackTrace();
				}
				retry++;
			} 
		}
		
		return errorCode;
	}
	
	/**
	 * Create message depending on message type
	 * @param session - session
	 * @param messageBody - message body
	 * @param messageClass - message class, must be serializable
	 * @return - created message
	 * @throws JMSException
	 */
	@SuppressWarnings("rawtypes")
	private static Message createMessage(Session session, Object messageBody, Class messageClass) throws JMSException {
		return session.createObjectMessage((Serializable)messageClass.cast(messageBody)); 
	}
	
}
