package emo.ifs.ecar.sim;

import java.awt.*;

import javax.swing.*;

public class SimDialog extends JFrame {

	protected static final long serialVersionUID = 0;

	protected static final int ROWS = 700;
	protected static final int COLS = 700;
	protected static final int ROWS_OFFSET = 50;
	protected static final int COLS_OFFSET = 50;
	protected static final int RADIUS = 50;

	protected double robX, robY, robTh;

	public SimDialog(String ECarName) {
		super();
		// Frame erzeugen
		setSize(ROWS + 4 * ROWS_OFFSET, COLS + 4 * COLS_OFFSET);
		setTitle("Ecar Simulator " + ECarName);
		setFont(new Font("Times", Font.PLAIN, 12));
		setBackground(Color.lightGray);
		setRobX(0);
		setRobY(0);
		setRobTh(0);
		setVisible(true);
	}

	public void paint(Graphics g) {
		int rpos = ROWS / 2 - (int) (robX / 10) + ROWS_OFFSET + RADIUS / 2;
		int cpos = COLS / 2 - (int) (robY / 10) + COLS_OFFSET + RADIUS / 2;
		double thpos = robTh;
//		g.clearRect(0, 0, ROWS + 2*ROWS_OFFSET, COLS+2*COLS_OFFSET);
		g.clearRect(0, 0, ROWS + 4 * ROWS_OFFSET, COLS + 4 * COLS_OFFSET);
		g.drawRect(ROWS_OFFSET, COLS_OFFSET, ROWS + 2 * ROWS_OFFSET, COLS + 2 * COLS_OFFSET);
		g.drawOval(cpos, rpos, RADIUS, RADIUS);
		int startR = rpos + RADIUS / 2;
		int startC = cpos + RADIUS / 2;
		int endR = rpos + RADIUS / 2 - (int) (RADIUS / 2 * Math.cos(Math.toRadians(thpos)));
		int endC = cpos + RADIUS / 2 - (int) (RADIUS / 2 * Math.sin(Math.toRadians(thpos)));
		// System.out.println("RobLine " + thpos + ": " + startX + " " + startY + " " +
		// endX + " " + endY);
		g.drawLine(startC, startR, endC, endR);
	}

	public synchronized void setRobX(double robX) {
		if (Math.abs(robX) / 10 - RADIUS / 2 <= ROWS / 2) {
			this.robX = robX;
			repaint();
		}
	}

	public synchronized void setRobY(double robY) {
		if (Math.abs(robY) / 10 - RADIUS / 2 <= COLS / 2) {
			this.robY = robY;
			repaint();
		}
	}

	public synchronized void setRobTh(double robTh) {
		this.robTh = robTh;
		repaint();
	}

	public double getRobX() {
		return robX;
	}

	public double getRobY() {
		return robY;
	}

	public double getRobTh() {
		return robTh;
	}

	public double getDistLeft(double pos) {
		return (COLS / 2 - pos / 10) * 10;
	}

	public double getDistTop(double pos) {
		return (ROWS / 2 - pos / 10) * 10;
	}

	public double getDistBottom(double pos) {
		return (ROWS / 2 + pos / 10) * 10;
	}

	public double getDistRight(double pos) {
		return (COLS / 2 + pos / 10) * 10;
	}

}
