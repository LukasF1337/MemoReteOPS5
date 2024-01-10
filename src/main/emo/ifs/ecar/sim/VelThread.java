package emo.ifs.ecar.sim;

public class VelThread extends Thread {

	protected static final int ZYKLUSZEIT = 200;
	protected SimDialog simDialog;
	protected int transVel, rotVel;

	public VelThread(SimDialog simDialog) {
		super();
		this.simDialog = simDialog;
	}

	public void run() {
		while (true) {
			double nTh = simDialog.getRobTh() + rotVel * ZYKLUSZEIT / 1000;
			if (nTh > 180)
				nTh = -360 + nTh;
			if (nTh < -180)
				nTh = 360 + nTh;
			simDialog.setRobTh(nTh);
			simDialog.setRobX(simDialog.getRobX()
					+ transVel * ZYKLUSZEIT / 1000 * Math.cos(Math.toRadians(simDialog.getRobTh())));
			simDialog.setRobY(simDialog.getRobY()
					+ transVel * ZYKLUSZEIT / 1000 * Math.sin(Math.toRadians(simDialog.getRobTh())));
			try {
				sleep(ZYKLUSZEIT);
			} catch (InterruptedException ign) {
			}
		}
	}

	public void setTransVel(int transVel) {
		this.transVel = transVel;
	}

	public void setRotVel(int rotVel) {
		this.rotVel = rotVel;
	}

	public int getTransVel() {
		return transVel;
	}

	public int getRotVel() {
		return rotVel;
	}

}
