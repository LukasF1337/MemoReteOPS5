package emo.ifs.ecar;

public class ECarDefines {

	public static final String DX1 = "DX1";
	public static final String DX2 = "DX2";
	public static final String DX3 = "DX3";
	public static final String DX4 = "DX4";
	public static final String DX5 = "DX5";
	public static final String AT1 = "AT1";
	public static final String AT2 = "AT2";
	public static final String AT3 = "AT3";
	public static final String PB1 = "PB1";
	public static final String PB2 = "PB2";
	public static final String SIM_DX1 = "SIM_DX1";
	public static final String SIM_DX2 = "SIM_DX2";
	public static final String SIM_DX3 = "SIM_DX3";
	public static final String SIM_DX4 = "SIM_DX4";
	public static final String SIM_DX5 = "SIM_DX5";
	public static final String SIM_AT1 = "SIM_AT1";
	public static final String SIM_AT2 = "SIM_AT2";
	public static final String SIM_AT3 = "SIM_AT3";
	public static final String SIM_PB1 = "SIM_PB1";
	public static final String SIM_PB2 = "SIM_PB2";
	public static final String JSIM_DX1 = "JSIM_DX1";
	public static final String JSIM_DX2 = "JSIM_DX2";
	public static final String JSIM_DX3 = "JSIM_DX3";
	public static final String JSIM_DX4 = "JSIM_DX4";
	public static final String JSIM_DX5 = "JSIM_DX5";
	public static final String JSIM_AT1 = "JSIM_AT1";
	public static final String JSIM_AT2 = "JSIM_AT2";
	public static final String JSIM_AT3 = "JSIM_AT3";
	public static final String JSIM_PB1 = "JSIM_PB1";
	public static final String JSIM_PB2 = "JSIM_PB2";

	public static final int SERVER_CONTROL_PORT = 9210;
	public static final int ROB_PORT = 9220;
	public static final int SIM_DX1_PORT = 9221;
	public static final int SIM_DX2_PORT = 9222;
	public static final int SIM_DX3_PORT = 9223;
	public static final int SIM_DX4_PORT = 9224;
	public static final int SIM_DX5_PORT = 9225;
	public static final int SIM_AT1_PORT = 9226;
	public static final int SIM_AT2_PORT = 9227;
	public static final int SIM_AT3_PORT = 9228;
	public static final int SIM_PB1_PORT = 9229;
	public static final int SIM_PB2_PORT = 9230;

	public static final int SIM_DX1_ROBSIM_PORT = 9241;
	public static final int SIM_DX2_ROBSIM_PORT = 9242;
	public static final int SIM_DX3_ROBSIM_PORT = 9243;
	public static final int SIM_DX4_ROBSIM_PORT = 9244;
	public static final int SIM_DX5_ROBSIM_PORT = 9245;
	public static final int SIM_AT1_ROBSIM_PORT = 9246;
	public static final int SIM_AT2_ROBSIM_PORT = 9247;
	public static final int SIM_AT3_ROBSIM_PORT = 9248;
	public static final int SIM_PB1_ROBSIM_PORT = 9249;
	public static final int SIM_PB2_ROBSIM_PORT = 9250;

	public static final int SIM_DX1_IMAGE_SERVER_PORT = 9261;
	public static final int SIM_DX2_IMAGE_SERVER_PORT = 9262;
	public static final int SIM_DX3_IMAGE_SERVER_PORT = 9263;
	public static final int SIM_DX4_IMAGE_SERVER_PORT = 9264;
	public static final int SIM_DX5_IMAGE_SERVER_PORT = 9265;
	public static final int SIM_AT1_IMAGE_SERVER_PORT = 9266;
	public static final int SIM_AT2_IMAGE_SERVER_PORT = 9267;
	public static final int SIM_AT3_IMAGE_SERVER_PORT = 9268;
	public static final int SIM_PB1_IMAGE_SERVER_PORT = 9269;
	public static final int SIM_PB2_IMAGE_SERVER_PORT = 9270;

	public static final int JSIM_DX1_PORT = 9281;
	public static final int JSIM_DX2_PORT = 9282;
	public static final int JSIM_DX3_PORT = 9283;
	public static final int JSIM_DX4_PORT = 9284;
	public static final int JSIM_DX5_PORT = 9285;
	public static final int JSIM_AT1_PORT = 9286;
	public static final int JSIM_AT2_PORT = 9287;
	public static final int JSIM_AT3_PORT = 9288;
	public static final int JSIM_PB1_PORT = 9289;
	public static final int JSIM_PB2_PORT = 9290;

	public static final String SEP = ",";
	public static final String POSQUIT = "0";
	public static final String NEGQUIT = "-1";

	public static final int CONNECT = 1;

	public static final int MOVE = 10;
	public static final int TURN = 11;
	public static final int SPEED = 12;
	public static final int ROTATE = 13;

	public static final int SONAR = 20;
	public static final int LASER = 21;
	public static final int POSE = 22;
	public static final int VOLT = 23;
	public static final int ALLSONAR = 24;
	public static final int VEL = 25;

	public static final int OPEN = 100;
	public static final int CLOSE = 101;
	public static final int UP = 102;
	public static final int DOWN = 103;

	public static final int[][] SONAR_AT = { { 145, 130, 90 }, { 185, 115, 50 }, { 220, 80, 30 }, { 240, 25, 10 },
			{ 240, -25, -10 }, { 220, -80, -30 }, { 185, -115, -50 }, { 145, -130, -90 }, { -145, -130, -90 },
			{ -185, -115, -130 }, { -220, -80, -150 }, { -240, -25, -170 }, { -240, 25, 170 }, { -220, 80, 150 },
			{ -185, 115, 130 }, { -145, 130, 90 } };

	public static final int[][] SONAR_DX = { { 115, 130, 90 }, { 155, 115, 50 }, { 190, 80, 30 }, { 210, 25, 10 },
			{ 210, -25, -10 }, { 190, -80, -30 }, { 155, -115, -50 }, { 115, -130, -90 }, { -115, -130, -90 },
			{ -155, -115, -130 }, { -190, -80, -150 }, { -210, -25, -170 }, { -210, 25, 170 }, { -190, 80, 150 },
			{ -155, 115, 130 }, { -115, 130, 90 } };

	public static final int[] LASER_AT = { 160, 7, 0 };
	public static final int[] LASER_DX = { 18, 0, 0 };

	public static int getPort(String eCarName) {
		if (eCarName == null)
			return -1;
		else if (eCarName.equals(SIM_DX1))
			return SIM_DX1_PORT;
		else if (eCarName.equals(SIM_DX2))
			return SIM_DX2_PORT;
		else if (eCarName.equals(SIM_DX3))
			return SIM_DX3_PORT;
		else if (eCarName.equals(SIM_DX4))
			return SIM_DX4_PORT;
		else if (eCarName.equals(SIM_AT1))
			return SIM_AT1_PORT;
		else if (eCarName.equals(SIM_AT2))
			return SIM_AT2_PORT;
		else if (eCarName.equals(SIM_AT3))
			return SIM_AT3_PORT;
		else if (eCarName.equals(SIM_PB1))
			return SIM_PB1_PORT;
		else if (eCarName.equals(SIM_PB2))
			return SIM_PB2_PORT;
		else if (eCarName.equals(JSIM_DX1))
			return JSIM_DX1_PORT;
		else if (eCarName.equals(JSIM_DX2))
			return JSIM_DX2_PORT;
		else if (eCarName.equals(JSIM_DX3))
			return JSIM_DX3_PORT;
		else if (eCarName.equals(JSIM_DX4))
			return JSIM_DX4_PORT;
		else if (eCarName.equals(JSIM_AT1))
			return JSIM_AT1_PORT;
		else if (eCarName.equals(JSIM_AT2))
			return JSIM_AT2_PORT;
		else if (eCarName.equals(JSIM_AT3))
			return JSIM_AT3_PORT;
		else if (eCarName.equals(JSIM_PB1))
			return JSIM_PB1_PORT;
		else if (eCarName.equals(JSIM_PB2))
			return JSIM_PB2_PORT;
		else if (eCarName.equals(DX1) || eCarName.equals(DX2) || eCarName.equals(DX3) || eCarName.equals(DX4)
				|| eCarName.equals(DX5) || eCarName.equals(AT1) || eCarName.equals(AT2) || eCarName.equals(AT3)
				|| eCarName.equals(PB1) || eCarName.equals(PB2))
			return ROB_PORT;
		else
			return -1;
	}

	public static int getRobSimPort(String eCarName) {
		if (eCarName == null)
			return -1;
		else if (eCarName.equals(SIM_DX1))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_DX2))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_DX3))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_DX4))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_AT1))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_AT2))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_AT3))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_PB1))
			return SIM_DX1_ROBSIM_PORT;
		else if (eCarName.equals(SIM_PB2))
			return SIM_DX1_ROBSIM_PORT;
		else
			return -1;
	}

	public static int getImageServerPort(String eCarName) {
		if (eCarName == null)
			return -1;
		else if (eCarName.equals(SIM_DX1))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_DX2))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_DX3))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_DX4))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_AT1))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_AT2))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_AT3))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_PB1))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else if (eCarName.equals(SIM_PB2))
			return SIM_DX1_IMAGE_SERVER_PORT;
		else
			return -1;
	}

	public static String getServerName(String eCarName) {
		if (eCarName == null)
			return "";
		else if (eCarName.equals(DX1) || eCarName.equals(DX2) || eCarName.equals(DX3) || eCarName.equals(DX4)
				|| eCarName.equals(DX5) || eCarName.equals(AT1) || eCarName.equals(AT2) || eCarName.equals(AT3)
				|| eCarName.equals(PB1) || eCarName.equals(PB2))
			return eCarName;
		else
			return "localhost";
	}

	public static final String JWORLD = "JWorld";

}
