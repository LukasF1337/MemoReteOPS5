package emo.ifs.ecar;

public class Timer extends Thread {

	protected int time;

	public Timer(int time) {
		super();
		this.time = time;
	}

	public void run() {
		try {
			sleep(time);
		} catch (InterruptedException ign) {
		}
	}

	public void waitFor() {
		try {
			start();
			join();
		} catch (InterruptedException ign) {
		}
	}
}
