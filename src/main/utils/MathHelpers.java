package utils;

public final class MathHelpers {
	private MathHelpers() {
		// This class should never be instantiated. It provides only static methods.
		throw new IllegalStateException();
	}

	/**
	 * Fast log2() algorithm copied from x4u:
	 * https://stackoverflow.com/questions/3305059/how-do-you-calculate-log-base-2-in-java-for-integers
	 */
	public static int binlog(int bits) // returns 0 for bits=0
	{
		int log = 0;
		if ((bits & 0xffff0000) != 0) {
			bits >>>= 16;
			log = 16;
		}
		if (bits >= 256) {
			bits >>>= 8;
			log += 8;
		}
		if (bits >= 16) {
			bits >>>= 4;
			log += 4;
		}
		if (bits >= 4) {
			bits >>>= 2;
			log += 2;
		}
		return log + (bits >>> 1);
	}
}
