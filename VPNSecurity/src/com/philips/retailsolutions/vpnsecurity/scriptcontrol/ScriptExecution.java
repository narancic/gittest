package com.philips.retailsolutions.vpnsecurity.scriptcontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptExecution {

	private static Logger log = LoggerFactory.getLogger(ScriptExecution.class);

	private static final String netCatGetUserIP = "echo \"status 3\" | netcat -q 1 -t localhost %s | grep CLIENT_LIST | grep %s | cut -f 4";
	private static final String rejectRuleCommand = "iptables -A FORWARD -j REJECT";
	private static final String enableFirewallRuleCommand1 = "iptables -I FORWARD -s %s -d %s -j ACCEPT";
	private static final String enableFirewallRuleCommand2 = "iptables -I FORWARD -d %s -s %s -j ACCEPT";
	private static final String disableFirewallRuleCommand1 = "iptables -D FORWARD -s %s -d %s -j ACCEPT";
	private static final String disableFirewallRuleCommand2 = "iptables -D FORWARD -d %s -s %s -j ACCEPT";
	private static final String clearForwardRules1 = "iptables -F";
	private static final String clearForwardRules2 = "iptables -X";
	private static final String netCatAllIPs = "echo \"status 3\" | netcat -q 1 -t localhost %s | grep CLIENT_LIST | cut -f 4";

	public ScriptExecution() {
	}

	public static String getUserVpnAddress(String port, String userIPAddress) {
		log.info("getUserVpnAddress - port: " + port + ", userIPAddress: " + userIPAddress);
		
		String cmd = String.format(netCatGetUserIP, port, userIPAddress);
		
		log.info("getUserVpnAddress - command: " + cmd);
		
		Process process = null;
		String response = "";
		String error = "";
		try {
			process = Runtime.getRuntime().exec(
					new String[] { "/bin/sh", "-c", cmd });
			process.waitFor();

			BufferedReader stdIn = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String line = null;

			while ((line = stdError.readLine()) != null) {
				line += "\n";
				error += line;
			}
			while ((line = stdIn.readLine()) != null) {
				response += line;
			}
			if (error.length() > 0) {
				log.info("getUserVpnAddress Error : " + error);
				response = null;
			} else {
				log.info("getUserVpnAddress Response : " + response);
			}
		} catch (InterruptedException ignore) {
			log.info("getUserVpnAddress exception " + ignore.toString());
			ignore.printStackTrace();
			return null;
		} catch (IOException e) {
			log.info("getUserVpnAddress exception " + e.toString());
			e.printStackTrace();
			return null;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return response;
	}

	public static boolean createBlockAllFirewallRule() {
		String cmd = rejectRuleCommand;
		boolean ret_value = false;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			process.waitFor();

			BufferedReader stdIn = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));

			String line = null;
			String response = "";
			String error = "";
			while ((line = stdIn.readLine()) != null) {
				line += "\n";
				response += line;
			}

			while ((line = stdError.readLine()) != null) {
				line += "\n";
				error += line;
			}
			if (error.length() > 0) {
				log.info("createBlockAllFirewallRule Error : " + error);
			} else {
				log.info("createBlockAllFirewallRule Response : " + response);
				ret_value = true;
			}
		} catch (InterruptedException ignore) {
			return ret_value;
		} catch (IOException e) {
			return ret_value;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return ret_value;
	}

	public static boolean enableForwarding(String epVPNAddress,
			String userVPNAddress) {
		log.info("Enabling forwarding for : " + epVPNAddress + " and "
				+ userVPNAddress);
		boolean ret_value = false;
		String cmd1 = String.format(enableFirewallRuleCommand1, epVPNAddress,
				userVPNAddress);
		String cmd2 = String.format(enableFirewallRuleCommand2, epVPNAddress,
				userVPNAddress);
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd1);
			process.waitFor();

			BufferedReader err = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String line = null;
			String error = "";
			while ((line = err.readLine()) != null) {
				line += "\n";
				error += line;
			}
			if (error.length() > 0) {
				log.info("enableForwarding 1 Error : " + error);
			} else {
				ret_value = true;
			}
		} catch (InterruptedException ignore) {
			log.error("enableForwarding 1 error :" + ignore.toString());
			return ret_value;
		} catch (IOException e) {
			log.error("enableForwarding 1 error :" + e.toString());
			return ret_value;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		try {
			process = Runtime.getRuntime().exec(cmd2);
			process.waitFor();

			BufferedReader err = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String line = null;
			String error = "";
			while ((line = err.readLine()) != null) {
				line += "\n";
				error += line;
			}
			if (error.length() > 0) {
				log.info("enableForwarding 2 Error : " + error);
			} else {
				ret_value = true;
			}
		} catch (InterruptedException ignore) {
			log.error("enableForwarding 2 error :" + ignore.toString());
			return ret_value;
		} catch (IOException e) {
			log.error("enableForwarding 2 error :" + e.toString());
			return ret_value;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return ret_value;
	}

	public static boolean disableForwarding(String epVPNAddress,
			String userVPNAddress) {
		log.info("Disabling forwarding for : " + epVPNAddress + " and "
				+ userVPNAddress);
		boolean ret_value = false;
		String cmd1 = String.format(disableFirewallRuleCommand1, epVPNAddress,
				userVPNAddress);
		String cmd2 = String.format(disableFirewallRuleCommand2, epVPNAddress,
				userVPNAddress);
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd1);
			process.waitFor();

			BufferedReader err = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String line = null;
			String error = "";
			while ((line = err.readLine()) != null) {
				line += "\n";
				error += line;
			}
			if (error.length() > 0) {
				log.info("disableForwarding 1 Error : " + error);
			} else {
				ret_value = true;
			}
		} catch (InterruptedException ignore) {
			log.error("disableForwarding 1 error :" + ignore.toString());
			return ret_value;
		} catch (IOException e) {
			log.error("disableForwarding 1 error :" + e.toString());
			return ret_value;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		try {
			process = Runtime.getRuntime().exec(cmd2);
			process.waitFor();

			BufferedReader err = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String line = null;
			String error = "";
			while ((line = err.readLine()) != null) {
				line += "\n";
				error += line;
			}
			if (error.length() > 0) {
				log.info("disableForwarding 2 Error : " + error);
			} else {
				ret_value = true;
			}
		} catch (InterruptedException ignore) {
			log.error("disableForwarding 2 error :" + ignore.toString());
			return ret_value;
		} catch (IOException e) {
			log.error("disableForwarding 2 error :" + e.toString());
			return ret_value;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return ret_value;
	}

	public static void clearAllForwardRules() {
		log.info("Clearing forward rules for");
		String cmd1 = clearForwardRules1;
		String cmd2 = clearForwardRules2;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd1);
			process.waitFor();

			BufferedReader err = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String line = null;
			String error = "";
			while ((line = err.readLine()) != null) {
				line += "\n";
				error += line;
			}
			if (error.length() > 0) {
				log.info("disableForwarding 1 Error : " + error);
			}
		} catch (InterruptedException ignore) {
			log.error("clearAllForwardRules 1 error :" + ignore.toString());
		} catch (IOException e) {
			log.error("clearAllForwardRules 1 error :" + e.toString());
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		try {
			process = Runtime.getRuntime().exec(cmd2);
			process.waitFor();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;
			String response = "";
			String error = "";
			while ((line = in.readLine()) != null) {
				line += "\n";
				response += line;
			}
			log.info("clearAllForwardRules 2 Response : " + response);
			log.info("clearAllForwardRules 2 Error : " + error);
		} catch (InterruptedException ignore) {
			log.error("clearAllForwardRules 2 error :" + ignore.toString());
		} catch (IOException e) {
			log.error("clearAllForwardRules 2 error :" + e.toString());
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	public static List<String> getAllConectedVPNUsers(List<String> ports) {
		List<String> retList = new ArrayList<String>();
		for (String port : ports) {
			String cmd = String.format(netCatAllIPs, port);
			Process process = null;
			try {
				process = Runtime.getRuntime().exec(
						new String[] { "/bin/sh", "-c", cmd });
				process.waitFor();

				BufferedReader in = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				String line = null;
				while ((line = in.readLine()) != null) {
					retList.add(line);
				}
			} catch (InterruptedException ignore) {
				log.error("getAllConectedVPNUsers error :" + ignore.toString());
			} catch (IOException e) {
				log.error("getAllConectedVPNUsers error :" + e.toString());
			} finally {
				if (process != null) {
					process.destroy();
				}
			}
		}
		return retList;
	}
}
