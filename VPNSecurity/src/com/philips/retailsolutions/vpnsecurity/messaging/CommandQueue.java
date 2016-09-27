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
 * @file ReportQueue.java
 * @brief
 *  
 * Created on 17.08.2012.
 *
 * @author Dejan Stefanovic
 * 
 * 
 * @history
 * 17.08.2012. Initial version
 * 
 */

package com.philips.retailsolutions.vpnsecurity.messaging;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallRuleMessage;
import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallSetReportMessage;
import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallSetReportMessage.FirewallSetResult;
import com.philips.retailsolutions.vpnsecurity.scriptcontrol.VPNConnectionHandling;
import com.philips.retailsolutions.vpnsecurity.scriptcontrol.VPNConnectionMonitoring;

/**
 * This runnable listens to start queue for incoming messages and processes
 * them.
 */
public class CommandQueue implements Runnable {
	private static Logger log = LoggerFactory.getLogger(CommandQueue.class);
	private MessageConsumer consumer;
	private VPNConnectionHandling connHandling;
	private MessageSender responseSender = null;
	private static VPNConnectionMonitoring connMonitoringThread;
	/**
	 * Constructor - set controller and message consumer
	 * 
	 * @param ctrl
	 *            - controller to set
	 * @param msgConsumer
	 *            - consumer to set
	 */
	public CommandQueue(MessageConsumer msgConsumer,
			VPNConnectionHandling connHandling) {
		this.consumer = msgConsumer;
		this.connHandling = connHandling;
	}

	/**
	 * Set message consumer
	 * 
	 * @param msgConsumer
	 */
	public void setMessageConsumer(MessageConsumer msgConsumer) {
		consumer = msgConsumer;
	}

	public void setConnHandling(VPNConnectionHandling connHandling) {
		this.connHandling = connHandling;
	}

	public void setResponseSender(MessageSender responseSender) {
		this.responseSender = responseSender;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		connMonitoringThread= new VPNConnectionMonitoring(connHandling,this);
		startConnectionMonitoringThread();
		while (true) {
			Message msg;
			VPNFirewallRuleMessage message = null;
			try {
				// receive JMS from EM
				msg = consumer.receive();
				ObjectMessage om = (ObjectMessage) msg;
				message = (VPNFirewallRuleMessage) om.getObject();

				log.info("Received a message from server: .........");

				VPNFirewallSetReportMessage report = new VPNFirewallSetReportMessage();
				Map<String, FirewallSetResult> retMap = null;
				report.setGatewayIP(message.getRmgwIP());

				if (message.getEventType().equals(
						VPNFirewallRuleMessage.EventType.VPN_CONNECTED)) {
					report.setRuleType(VPNFirewallSetReportMessage.RuleType.SET_RULE);
				} else {
					report.setRuleType(VPNFirewallSetReportMessage.RuleType.DELETE_RULE);
				}
				retMap = connHandling.handleVPNConnectionEvent(message);
				if (retMap != null) {
					for (String ip : retMap.keySet()) {
						log.info("Firewall rules for " + ip + " status "
								+ retMap.get(ip));

					}
				}
				report.setUserIp(retMap);
				responseSender.sendMessage(report,
						VPNFirewallSetReportMessage.class);
			} catch (JMSException e) {
				stopConnectionMonitoringThread();
				e.printStackTrace();
			}
		}
	}
	/**
	 * Create/start threads that checks if any VPN client with firewall rule is disconnected
	 *
	 *@param connHandling - Firewall logic context
	 */
	private void startConnectionMonitoringThread() {
		//Enable thread execution
		connMonitoringThread.setShouldRun(true);
       	Thread taskThread = new Thread(connMonitoringThread);
       	taskThread.start();
	}
	
	/**
	 * Stops thread that checks if any VPN client with firewall rule is disconnected
	 */
	private static void stopConnectionMonitoringThread() {
		//Disable thread execution
		connMonitoringThread.setShouldRun(false);
	}

	public MessageSender getResponseSender() {
		return responseSender;
	}
	
	
}
