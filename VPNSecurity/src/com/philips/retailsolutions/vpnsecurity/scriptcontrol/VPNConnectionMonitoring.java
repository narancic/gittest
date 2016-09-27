package com.philips.retailsolutions.vpnsecurity.scriptcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallSetReportMessage;
import com.philips.retailsolutions.vpnsecurity.common.CommonConstants;
import com.philips.retailsolutions.vpnsecurity.messaging.CommandQueue;

public class VPNConnectionMonitoring implements Runnable {

	private static Logger log = LoggerFactory
			.getLogger(VPNConnectionMonitoring.class);
	private boolean shouldRun;
	private VPNConnectionHandling connHandling;
	private CommandQueue commandQueue;

	public VPNConnectionMonitoring(VPNConnectionHandling connHandling,
			CommandQueue commandQueue) {
		this.connHandling = connHandling;
		this.commandQueue = commandQueue;
	}

	@Override
	public void run() {
		while (shouldRun) {

			VPNFirewallSetReportMessage msg = this.connHandling
					.checkVPNConnections();
			
			if ((msg != null) && (msg.getUserIp().size() > 0) && (commandQueue != null)
					&& (commandQueue.getResponseSender() != null)) {
				commandQueue.getResponseSender().sendMessage(msg,
						VPNFirewallSetReportMessage.class);
			}
			try {
				Thread.sleep(CommonConstants.MONITOR_THREAD_PERIOD);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("VPNConnectionMonitoring thread ended.");

	}

	public boolean isShouldRun() {
		return shouldRun;
	}

	public void setShouldRun(boolean shouldRun) {
		this.shouldRun = shouldRun;
	}

}
