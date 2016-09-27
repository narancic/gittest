package com.philips.retailsolutions.vpnsecurity.scriptcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallRuleMessage;
import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallSetReportMessage;
import com.philips.retailsolutions.experiencemanager.server.javabeans.VPNFirewallSetReportMessage.FirewallSetResult;
import com.philips.retailsolutions.vpnsecurity.common.CommonConstants;

public class VPNConnectionHandling {

	private class userVPNData {

		private String ipV4Address;
		private Integer connectionFailure;

		public userVPNData(String ipAddress) {
			ipV4Address = ipAddress;
			connectionFailure = 0;
		}

		public String getIpV4Address() {
			return ipV4Address;
		}

		public Integer getConnectionFailure() {
			return connectionFailure;
		}

		public void connectionSuccessful() {
			this.connectionFailure = 0;
		}

		public void connectionFailure() {
			this.connectionFailure++;
			log.info("Connection problems with " + this.ipV4Address + " counter " + this.connectionFailure);
		}
	}

	private static Logger log = LoggerFactory.getLogger(VPNConnectionHandling.class);
	private List<String> netCatPorts;
	private HashMap<String, String> rulesMap; // Key user VPN IP, value EP VPN
												// IP
	private HashMap<String, userVPNData> usersMap; // Key user VPN IP, value
													// User
													// IPv4 IP and connection
													// failure counter
	// IP
	private Semaphore operationSync;

	public VPNConnectionHandling() {
		netCatPorts = new ArrayList<String>();
		rulesMap = new HashMap<String, String>();
		usersMap = new HashMap<String, userVPNData>();
		operationSync = new Semaphore(1);
	}

	public VPNConnectionHandling(List<String> ports) {
		netCatPorts = ports;
		rulesMap = new HashMap<String, String>();
		usersMap = new HashMap<String, userVPNData>();
		operationSync = new Semaphore(1);
	}

	public Map<String, FirewallSetResult> handleVPNConnectionEvent(VPNFirewallRuleMessage message) {
		Map<String, FirewallSetResult> ret_value = new HashMap<String, FirewallSetResult>();
		try {
			operationSync.acquire();
			log.info("Event: " + message.getEventType());
			log.info("RM gateway IP: " + message.getRmgwIP());
			if (message.getUserIps() != null) {
				String users = "";
				for (String ip : message.getUserIps()) {
					users += ip + " ";
				}
				log.info("Requested for users: " + users);
			}
			// Search for User IP on management ports for openVPN server
			for (String ip : message.getUserIps()) {
				boolean status = false;
				String userVPNIP = null;
				for (String port : netCatPorts) {
					if (userVPNIP == null) {
						// Try to get user VPN IP
						String vpnIp = ScriptExecution.getUserVpnAddress(port, ip);
						if ((vpnIp != null) && (validIP(vpnIp))) {
							log.info("User VPN IP " + vpnIp);
							userVPNIP = vpnIp;
						}
					}
				}
				if (userVPNIP != null) {
					if (message.getEventType().equals(VPNFirewallRuleMessage.EventType.VPN_CONNECTED)) {
						// Enable forwarding
						status = ScriptExecution.enableForwarding(message.getRmgwIP(), userVPNIP);
						if (status == true) {
							rulesMap.put(userVPNIP, message.getRmgwIP());
							usersMap.put(userVPNIP, new userVPNData(ip));
						}
					} else if (message.getEventType().equals(VPNFirewallRuleMessage.EventType.VPN_DISCONNECTED)) {
						// Disable forwarding
						status = ScriptExecution.disableForwarding(message.getRmgwIP(), userVPNIP);
						if (status == true) {
							rulesMap.remove(userVPNIP);
							usersMap.remove(userVPNIP);
						}
					}
				}
				ret_value.put(ip, status == true ? FirewallSetResult.OK : FirewallSetResult.FAILURE);
			}
			operationSync.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ret_value;
	}

	public VPNFirewallSetReportMessage checkVPNConnections() {
		VPNFirewallSetReportMessage report = null;
		try {
			operationSync.acquire();

			report = new VPNFirewallSetReportMessage();
			Map<String, FirewallSetResult> retMap = new HashMap<String, FirewallSetResult>();
			report.setRuleType(VPNFirewallSetReportMessage.RuleType.PC_VPN_DISCONNECTED);

			List<String> vpnIps = ScriptExecution.getAllConectedVPNUsers(netCatPorts);

			for (String key : rulesMap.keySet()) {
				log.info("Key from rules map " + key + " value " + rulesMap.get(key));
				if ((!vpnIps.contains(key) && (usersMap.get(key) != null))) {
					usersMap.get(key).connectionFailure();
				} else {
					usersMap.get(key).connectionSuccessful();
				}
			}

			Iterator<Map.Entry<String, userVPNData>> iter = usersMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, userVPNData> entry = iter.next();
				if ((entry.getValue() != null) && (entry.getValue().getConnectionFailure() > CommonConstants.CONNECTION_LOSS_LIMIT )) {
					ScriptExecution.disableForwarding(rulesMap.get(entry.getKey()), entry.getKey());
					retMap.put(entry.getValue().ipV4Address, FirewallSetResult.OK);
					rulesMap.remove(entry.getKey());
					iter.remove();
				}
			}

			report.setUserIp(retMap);
			operationSync.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		return report;
	}

	private boolean validIP(String ip) {
		if (ip == null || ip.isEmpty())
			return false;
		ip = ip.trim();
		if ((ip.length() < 6) & (ip.length() > 15))
			return false;

		try {
			Pattern pattern = Pattern
					.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
			Matcher matcher = pattern.matcher(ip);
			return matcher.matches();
		} catch (PatternSyntaxException ex) {
			return false;
		}
	}

}
