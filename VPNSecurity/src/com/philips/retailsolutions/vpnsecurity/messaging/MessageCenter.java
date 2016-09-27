/***************************************************************************
 *
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
 * @file MessageCenter.java
 * @brief
 * This class creates connections with JMS server manages with message consumers/senders.
 *  
 * Created on 31.08.2012.
 *
 * @author Dejan Stefanovic
 * 
 * 
 * @history
 * 31.08.2012. Initial version
 * 
 */

package com.philips.retailsolutions.vpnsecurity.messaging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.philips.retailsolutions.vpnsecurity.common.CommonConstants;
import com.philips.retailsolutions.vpnsecurity.common.LookupRemoteEjbUtil;
import com.philips.retailsolutions.vpnsecurity.scriptcontrol.VPNConnectionHandling;
import com.philips.retailsolutions.vpnsecurity.scriptcontrol.VPNConnectionMonitoring;

/**
 * This class creates connections with JMS server and manages with message consumers/senders.
 * Also periodically checks if the connection is alive, if not tries to re-establish the
 * connection.
 */
public class MessageCenter implements ExceptionListener {
	private static final long CONNECTION_REPAIR_TIMEOUT = 60*1000;

	private static Logger log = LoggerFactory.getLogger(MessageCenter.class);
	
	/* JMS connection */
    private static Connection connection = null;
    /* JMS session */
    private static Session session = null;
    
    /* Message consumers */
    private static MessageConsumer commandConsumer;
	
    CommandQueue commandQueue;

    MessageSender responseSender;
	
	boolean connected = false;
	boolean stopped = false;
	
	private static MessageCenter mc;
	
	private ConnectionRepair repair;
    
	private VPNConnectionHandling connHandling;
	/**
	 * Private (hidden) constructor
	 */
    private MessageCenter() {
    	repair = new ConnectionRepair();
		Thread t = new Thread(repair);
		t.start();
    }
    
    /**
     * Get instance of the class
     * @return - class instance
     */
    public static MessageCenter instance() {
    	if (mc==null) {
    		mc = new MessageCenter();    		
    	}
    	return mc;
    }
	
	/**
	 * Create JMS resources (connection, queue, session, consumers)
	 * @throws NamingException - on context creation failure
	 * @throws JMSException - on JMS resources creation failure
	 */
	private void createJMSConnection() throws NamingException, JMSException {
    	log.info("JMS connection init...");
        		
		/* create connection */
		if (connection == null) {
			log.info("Creating a new connection...");
			TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName(), LookupRemoteEjbUtil.getJMSConnectionParams()); 
			ConnectionFactory cf = (ConnectionFactory) HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
			connection = cf.createConnection();
		}
		
		/* create session */
		if (session == null) {
			log.info("Creating a new session...");
	        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		}
    	
		connection.setExceptionListener(this);
		
        connection.start();
        log.info("JMS connection created");
	}
	
	/**
	 * Create message consumers for incoming JMS
	 * @throws NamingException - on context creation failure
	 * @throws JMSException - on JMS resources creation failure
	 */
	public void createConsumers(VPNConnectionHandling connHandling) throws NamingException, JMSException {
		InitialContext initialContext = LookupRemoteEjbUtil.getInitialContext();
		log.info("Initial context created");
		
		/* create queues */
		Queue vpnQueue = (Queue)initialContext.lookup("/queue/" + CommonConstants.VPN_COMMAND_QUEUE);
		
		/* create message consumers */
		log.info("Creating consumers...");
        commandConsumer = session.createConsumer(vpnQueue);
       	log.info("Consumers created");
        
        commandQueue = new CommandQueue(commandConsumer,connHandling);
       	Thread taskThread = new Thread(commandQueue);
       	taskThread.start();
       	log.info("Command queue started");
	}
	
	/**
	 * Create message publishers
	 * @throws NamingException - on context creation failure
	 * @throws JMSException - on JMS resources creation failure
	 */
	public void createPublishers() throws NamingException, JMSException {
		log.info("Creating pubishers...");
		responseSender = new MessageSender(this, CommonConstants.VPN_COMMAND_RESPONSE_QUEUE); 
		log.info("Created pubishers...");
	}
	
	/**
	 * Establish connection, consumers and publishers
	 * @return success info
	 */
	public boolean establishConnections(VPNConnectionHandling connHandling)  {
		try {
			this.connHandling=connHandling;
			createJMSConnection();
			createConsumers(connHandling);
			createPublishers();
			commandQueue.setMessageConsumer(commandConsumer);
			commandQueue.setConnHandling(connHandling);
			commandQueue.setResponseSender(responseSender);
	        connected = true;
			return true;
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
	 */
	@Override
	public void onException(JMSException arg0) {
		connected = false;
		connection = null;
		session = null;
		establishConnections(this.connHandling);
	}

	/**
	 * Get JMS connection
	 * @return
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Get JMS session
	 * @return
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * Close all connection before shutting down.
	 * @throws JMSException
	 */
	public void close() throws JMSException {
		if (session != null) {
			session.close();
		}
		if (connection != null) {
			connection.close();
		}
		stopped = true;
	}

	/**
	 * Thread that periodically check if the connection towards ExperienceManager is present.
	 * If the connection has been lost, it tries to re-establish.
	 */
	private class ConnectionRepair implements Runnable {
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while (!stopped) {
				try {
					Thread.sleep(CONNECTION_REPAIR_TIMEOUT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!connected) {
					log.info("Connection has been lost. Trying to re-establish...");
					connection = null;
					session = null;
					boolean succ = establishConnections(connHandling);
					if (succ) {
						log.info("Connection has been restored!");
					}
				}
			}			
		}
	}
}
