package emo.ifs.ecar.sim;

import java.io.*;
import java.net.*;
import java.util.*;

import emo.ifs.ecar.ECarDefines;

public class SimThread extends Thread {

	protected static final int MOVE_VEL = 200;
	protected static final int ROT_VEL = 15;
	protected static final double SLOW_FACTOR = 0.8;

	protected static final int MAXRANGE_LASER = 5000;
	protected static final int MAX_DISTANCES = 180;

	protected SimDialog simDialog;
	protected String eCarName;
	protected VelThread velThread;
	protected double volt;

	public SimThread(String eCarName) {
		super();
		simDialog = new SimDialog(eCarName);
		this.eCarName = eCarName;
		volt = 12.5;
		start();
	}

	public void run() {
		runTCP();
		// sonarTest();
		// velTest();
	}

	public void runTCP() {
		velThread = new VelThread(simDialog);
		velThread.start();
		ServerSocket welcomeSocket = null;
		Socket connectionSocket = null;
		try {
			welcomeSocket = new ServerSocket(ECarDefines.getPort(eCarName));
			connectionSocket = welcomeSocket.accept();
			DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

			while (true) {
				String request = inFromClient.readLine();
				StringTokenizer tok = new StringTokenizer(request, ECarDefines.SEP);
				String sCommand = tok.nextToken();
				int command = Integer.parseInt(sCommand);
				int param1 = 0;
				if (tok.hasMoreElements()) {
					String sParam1 = tok.nextToken();
					param1 = Integer.parseInt(sParam1);
				}
				switch (command) {
				case ECarDefines.MOVE:
					// move(param1);
					outToClient.writeBytes(ECarDefines.POSQUIT + "\n");
					break;

				case ECarDefines.TURN:
					// turn(param1);
					outToClient.writeBytes(ECarDefines.POSQUIT + "\n");
					break;

				case ECarDefines.SONAR:
					int dist = getSonarRange(param1);
					outToClient.writeBytes("" + dist + "\n");
					break;

				case ECarDefines.SPEED:
					speed(param1);
					outToClient.writeBytes(ECarDefines.POSQUIT + "\n");
					break;

				case ECarDefines.ROTATE:
					rotate(param1);
					outToClient.writeBytes(ECarDefines.POSQUIT + "\n");
					break;

				case ECarDefines.ALLSONAR:
					String sonarRanges = getSonarRanges();
					outToClient.writeBytes(sonarRanges + "\n");
					break;

				case ECarDefines.LASER:
					String laserRanges = getLaserRanges();
					outToClient.writeBytes(laserRanges + "\n");
					break;

				case ECarDefines.POSE:
					String pose = getPose();
					outToClient.writeBytes(pose + "\n");
					break;

				case ECarDefines.VOLT:
					String volt = getVolt();
					outToClient.writeBytes(volt + "\n");
					break;

				case ECarDefines.VEL:
					String vel = getVel();
					outToClient.writeBytes(vel + "\n");
					break;
				default:
					throw new IllegalStateException();

				}
			}
		} catch (Exception e) {
			try {
				if (connectionSocket != null)
					connectionSocket.close();
				if (welcomeSocket != null)
					welcomeSocket.close();
			} catch (IOException ign) {
				System.out.println("Connection closed\n");
			}
		}
	}

	protected void speed(int transVel) {
		velThread.setTransVel(transVel);
	}

	protected void rotate(int rotVel) {
		velThread.setRotVel(rotVel);
	}

	protected int getSonarRange(int num) {
		int res = 0;
		double sonarPosX = simDialog.getRobX() + ECarDefines.SONAR_DX[num][0];
		double sonarPosY = simDialog.getRobY() + ECarDefines.SONAR_DX[num][1];
		double sonarPosTh = simDialog.getRobTh() + ECarDefines.SONAR_DX[num][2];
		if (sonarPosTh > 0 && sonarPosTh <= 90) {
			double distY = Math.abs(simDialog.getDistLeft(sonarPosY) / Math.sin(Math.toRadians(sonarPosTh)));
			double distX = Math.abs(simDialog.getDistTop(sonarPosX) / Math.cos(Math.toRadians(sonarPosTh)));
			res = (int) distX;
			if (distX > distY)
				res = (int) distY;
		} else if (sonarPosTh > 90 && sonarPosTh <= 180) {
			double distY = Math.abs(simDialog.getDistLeft(sonarPosY) / Math.sin(Math.toRadians(180 - sonarPosTh)));
			double distX = Math.abs(simDialog.getDistBottom(sonarPosX) / Math.cos(Math.toRadians(180 - sonarPosTh)));
			res = (int) distX;
			if (distX > distY)
				res = (int) distY;
		} else if (sonarPosTh >= -180 && sonarPosTh <= -90) {
			double distY = Math.abs(simDialog.getDistRight(sonarPosY) / Math.sin(Math.toRadians(180 + sonarPosTh)));
			double distX = Math.abs(simDialog.getDistBottom(sonarPosX) / Math.cos(Math.toRadians(180 + sonarPosTh)));
			res = (int) distX;
			if (distX > distY)
				res = (int) distY;
		} else if (sonarPosTh > -90 && sonarPosTh < 0) {
			double distY = Math.abs(simDialog.getDistRight(sonarPosY) / Math.sin(Math.toRadians(-sonarPosTh)));
			double distX = Math.abs(simDialog.getDistTop(sonarPosX) / Math.cos(Math.toRadians(-sonarPosTh)));
			res = (int) distX;
			if (distX > distY)
				res = (int) distY;
		}
		if (res > 0)
			return res;
		else
			return 0;
	}

	protected String getSonarRanges() {
		String res = "";
		for (int i = 0; i < 16; i++) {
			res += getSonarRange(i);
			if (i < 15)
				res += ",";
		}
		return res;
	}

	protected String getLaserRanges() {
		String res = "";
		double robPosX = simDialog.getRobX();
		double robPosY = simDialog.getRobY();
		for (int i = -180; i < 180; i++) {
			int range = 0;
			if (i > 0 && i <= 90) {
				double distY = Math.abs(simDialog.getDistLeft(robPosY) / Math.sin(Math.toRadians(i)));
				double distX = Math.abs(simDialog.getDistTop(robPosX) / Math.cos(Math.toRadians(i)));
				range = (int) distX;
				if (distX > distY)
					range = (int) distY;
			} else if (i > 90 && i <= 180) {
				double distY = Math.abs(simDialog.getDistLeft(robPosY) / Math.sin(Math.toRadians(180 - i)));
				double distX = Math.abs(simDialog.getDistBottom(robPosX) / Math.cos(Math.toRadians(180 - i)));
				range = (int) distX;
				if (distX > distY)
					range = (int) distY;
			} else if (i >= -180 && i <= -90) {
				double distY = Math.abs(simDialog.getDistRight(robPosY) / Math.sin(Math.toRadians(180 + i)));
				double distX = Math.abs(simDialog.getDistBottom(robPosX) / Math.cos(Math.toRadians(180 + i)));
				range = (int) distX;
				if (distX > distY)
					range = (int) distY;
			} else if (i > -90 && i < 0) {
				double distY = Math.abs(simDialog.getDistRight(robPosY) / Math.sin(Math.toRadians(-i)));
				double distX = Math.abs(simDialog.getDistTop(robPosX) / Math.cos(Math.toRadians(-i)));
				range = (int) distX;
				if (distX > distY)
					range = (int) distY;
			}
			if (range < 0)
				range = 0;
			res += range;
			if (i < 179)
				res += ",";
		}
		return res;
	}

	protected String getPose() {
		String res = "" + simDialog.getRobX() + "," + simDialog.getRobY() + "," + simDialog.getRobTh();
		return res;
	}

	protected String getVolt() {
		volt /= 0.9999999;
		return "" + volt;
	}

	protected String getVel() {
		String res = "" + velThread.getTransVel() + "," + velThread.getRotVel();
		return res;
	}

	protected void velTest() {
		velThread = new VelThread(simDialog);
		velThread.start();
		velThread.setTransVel(200);
		velThread.setRotVel(10);
		velThread.setTransVel(-200);
		velThread.setRotVel(10);
		velThread.setRotVel(0);
		velThread.setTransVel(0);
	}

	protected void posTest() {
		simDialog.setRobTh(0);
		simDialog.setRobTh(90);
		simDialog.setRobTh(180);
		simDialog.setRobTh(-180);
		simDialog.setRobTh(-90);

		for (int i = -3; i < 4; i++) {
			simDialog.setRobX(i * 1000);
			for (int j = -3; j < 4; j++) {
				simDialog.setRobY(j * 1000);
				for (int k = -180; k <= 180; k += 90) {
					simDialog.setRobTh(k);
					try {
						sleep(1000);
					} catch (InterruptedException ign) {
					}

				}
			}
		}

	}

	protected void sonarTest() {
		velThread = new VelThread(simDialog);
		velThread.start();
		velThread.setTransVel(200);
//		simDialog.setRobTh(60);
//		simDialog.setRobX(-2500);
		while (true) {
			System.out.print("SonarRing:");
			for (int i = 0; i < 16; i++) {
				int dist = getSonarRange(i);
				System.out.print(" " + dist);
			}
			System.out.println();
			try {
				sleep(1000);
			} catch (InterruptedException ign) {
			}
		}
	}

}
