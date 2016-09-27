package com.philips.retailsolutions.vpnsecurity;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.philips.retailsolutions.vpnsecurity.messaging.MessageCenter;
import com.philips.retailsolutions.vpnsecurity.scriptcontrol.ScriptExecution;
import com.philips.retailsolutions.vpnsecurity.scriptcontrol.VPNConnectionHandling;

/**
 * Entry point - main class.
 */
public class VPNSecurity {

	private static Logger log = LoggerFactory.getLogger(VPNSecurity.class);

	private static MessageCenter messageCenter;

	/**
	 * Entry point in the application - create needed resources and start
	 * threads: controllers and listeners
	 * 
	 * @param args
	 *            - input parameters
	 * @throws InterruptedException
	 *             - on interrupt
	 * @throws JMSException
	 *             - on failure to create JMS resources
	 * @throws NamingException
	 *             - on failure to create remote connection
	 */
	public static void main(String[] args) throws InterruptedException, NamingException, JMSException {

		if (args.length > 0) {
			List<String> ports = new ArrayList<String>();
			for (String string : args) {
				ports.add(string);
			}

			// Set blocking FORWARD firewall rule
			ScriptExecution.createBlockAllFirewallRule();
			VPNConnectionHandling connHandling = new VPNConnectionHandling(ports);
			startApplicationTasks(connHandling);
			/* Catch CTRL+C to release the resources */
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						// Clear all FORWARD firewall rules
						ScriptExecution.clearAllForwardRules();
						messageCenter.close();
						log.info("Main thread finished");
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			log.error("Please provide command line args!");
		}
	}

	/**
	 * Create/start threads to listen for messages
	 * 
	 * @throws JMSException
	 *             - on failure to create JMS resources
	 * @throws NamingException
	 *             - on failure to create remote connection
	 */
	private static void startApplicationTasks(VPNConnectionHandling connHandling) throws NamingException, JMSException {
		messageCenter = MessageCenter.instance();
		messageCenter.establishConnections(connHandling);
	}

}
