package emo.ifs.ecar;

import java.net.*;

import emo.ifs.ecar.sim.*;

import java.io.*;

public class ECarClient {

	protected String eCarName;
	protected boolean hasLaser;
	protected Socket sock;
	protected PrintWriter socketSenden;
	protected BufferedReader socketEmpfangen;
	protected boolean connected;
	protected Process server;

	public ECarClient(String eCarName) {
		this.eCarName = eCarName;
		connected = false;
		hasLaser = false;
	}

	public void addLaser() {
		hasLaser = true;
	}

	public void startServer() {
		try {
			if (ECarDefines.getServerName(eCarName).equals("localhost")) {
				if (eCarName.startsWith("J")) {
					new SimThread(eCarName);
				} else {
					String cmd = "cmd /c " + "start /MIN c:\\programme\\eCar\\bin\\ECServer.exe " + eCarName + " "
							+ hasLaser;
					server = Runtime.getRuntime().exec(cmd);
				}
			} else {
				sock = new Socket(ECarDefines.getServerName(eCarName), ECarDefines.SERVER_CONTROL_PORT);
				socketSenden = new PrintWriter(sock.getOutputStream(), true);
				socketEmpfangen = new BufferedReader(new java.io.InputStreamReader(sock.getInputStream()));
				connected = true;
				String cmd = eCarName + ECarDefines.SEP;
				if (hasLaser)
					cmd += true;
				else
					cmd += false;
				socketSenden.println(cmd);
			}

		} catch (Exception e) {
			connected = false;
			System.out.println("Error starting server for " + eCarName + ":" + ECarDefines.getPort(eCarName));
		}
	}

	public boolean connect() {
		try {
			disconnect();
			sock = new Socket(ECarDefines.getServerName(eCarName), ECarDefines.getPort(eCarName));
			socketSenden = new PrintWriter(sock.getOutputStream(), true);
			socketEmpfangen = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			connected = true;
		} catch (Exception e) {
			connected = false;
			System.out.println("Error connecting eCar " + eCarName + ":" + ECarDefines.getPort(eCarName));
		}
		return connected;
	}

	public void disconnect() {
		if (connected) {
			try {
				sock.close();
			} catch (Exception ign) {
			}
			connected = false;
		}
	}

	public synchronized String exec(String cmd) {
		if (connected) {
			try {
				socketSenden.println(cmd);
				String res = "";
				do {
					res = socketEmpfangen.readLine();
				} while (res.length() == 0);
				return res;
			} catch (Exception e) {
				System.out.println("Error executing command: " + cmd);
				disconnect();
				connected = false;
				return null;
			}
		} else {
			System.out.println("Error executing command: " + cmd);
			return null;
		}
	}

	public String getECarName() {
		return eCarName;
	}

	public boolean isConnected() {
		return connected;
	}

}
