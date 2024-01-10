package ops5.parser;

import java.util.LinkedList;

public final class StringHelpers {

	private StringHelpers() {
		// This class should never be instantiated. It provides only static methods.
		throw new IllegalStateException();
	}

	/**
	 * Remove leftmost '(' and rightmost ')'
	 *
	 * @throws Exception
	 */
	public static String removeOuterParenthesis(String str) throws Exception {
		str = str.trim();
		String res;
		if (str.charAt(0) == '(' && str.charAt(str.length() - 1) == ')') {
			res = str.substring(1, str.length() - 1);
		} else {
			throw new Exception("Can't remove outer \"(\" and \")\" from:\n" + str);
		}

		return res;
	}

	/**
	 * Remove leftmost '{' and rightmost '}'
	 *
	 * @throws Exception
	 */
	public static String removeOuterCurlyParenthesis(String str) throws Exception {
		str = str.trim();
		String res;
		if (str.charAt(0) == '{' && str.charAt(str.length() - 1) == '}') {
			res = str.substring(1, str.length() - 1);
		} else {
			throw new Exception("Can't remove outer \"{\" and \"}\" from:\n" + str);
		}

		return res;
	}

	/**
	 * String to separate into Substrings each contained by startString and
	 * stopString while ignoring startString and stopString combinations inside such
	 * a substring and also all ignores[]. It also asserts that
	 * numberOf(startString) == numberOf(stopString) and that the number of opening
	 * and closing ignore pairs match. It skips over '|' pairs, not considering
	 * anything inside.
	 *
	 * @param str          String to parse
	 * @param startString  e.g. "("
	 * @param stopString   e.g. ")"
	 * @param startIndizes each index where one startString begins
	 * @param ignores      startSring and stopString combinations to ignore e.g. {
	 *                     {"{","}"}, {"[","]"} }
	 * @return List of Substrings surrounded by startString and stopString
	 * @throws Exception numberOf(startString) != numberOf(stopString)
	 */
	public static LinkedList<String> substringBetween(String str, String startString, String stopString,
			LinkedList<Integer> startIndizes, String ignores[][]) throws Exception {
		LinkedList<String> result = new LinkedList<>();
		int numberOfOpenParenthesis = 0;
		int i_start = 0, i_end = 0;
		int numOfIgnorePairs = ignores.length;
		int[] ignorePairCounts = new int[numOfIgnorePairs]; // zero initialized

		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '|') { // skip over constant expressions: |gdfgs |
				i++;
				for (; str.charAt(i) != '|'; i++) {
				}
				i++;
			}

			int currentPair = 0;
			for (String ignore[] : ignores) {
				if (ignore[0].equals("|")) {
					throw new Exception("ignoring \'|\' is not allowed, this is an error in the implementation");
				}
				if (str.substring(i, i + ignore[0].length()).equals(ignore[0])) { // "("
					ignorePairCounts[currentPair]++;
				} else if (str.substring(i, i + ignore[1].length()).equals(ignore[1])) { // ")"
					ignorePairCounts[currentPair]--;
					if (ignorePairCounts[currentPair] < 0) {
						throw new Exception(
								"too many closing \"" + ignore[1] + "\":\n" + str.substring(0, i + 1) + "<<<<");
					}
				}
				currentPair++;
			}

			int sumOfOpenIgnores = 0;
			for (int count : ignorePairCounts) {
				sumOfOpenIgnores += count;
			}

			if (sumOfOpenIgnores != 0) {
				// no operation if we are currently between ignores
			} else if (str.substring(i, i + startString.length()).equals(startString)) { // "("
				numberOfOpenParenthesis++;
				if (numberOfOpenParenthesis == 1) {
					i_start = i; // remember start index of "("
				}
			} else if (str.substring(i, i + stopString.length()).equals(stopString)) { // ")"
				numberOfOpenParenthesis--;
				if (numberOfOpenParenthesis < 0) {
					throw new Exception(
							"too many closing \"" + stopString + "\":\n" + str.substring(0, i + 1) + "<<<<");
				}
				if (numberOfOpenParenthesis == 0) {
					i_end = i;
					result.add(str.substring(i_start, i_end + 1));
					startIndizes.add(i_start);
				}
			}
		}
		if (numberOfOpenParenthesis != 0) {
			throw new Exception("\"" + startString + "\" never got closed:\n>>>> " + str.substring(i_start));
		}
		int sumOfOpenIgnores = 0;
		for (int count : ignorePairCounts) {
			sumOfOpenIgnores += count;
		}
		if (sumOfOpenIgnores != 0) {
			StringBuilder error = new StringBuilder("{");
			for (String ignore[] : ignores) {
				error.append("{\'").append(ignore[0]).append("\',\'").append(ignore[1]).append("\'}, ");
			}
			error.append("}");
			throw new Exception("Too many open of one of:\n" + "\t"
					+ error.append("\nin String:\n").append("\t").append(str).toString());
		}
		return result;
	}

	/**
	 *
	 * @param str          String to separate into Substrings each contained by
	 *                     startString and stopString while ignoring startString and
	 *                     stopString combinations inside such a substring. It also
	 *                     assures that numberOf(startString) ==
	 *                     numberOf(stopString)
	 * @param startString  e.g. "("
	 * @param stopString   e.g. ")"
	 * @param startIndizes each index where one startString begins
	 * @return List of Substrings surrounded by startString and stopString
	 * @throws Exception numberOf(startString) != numberOf(stopString)
	 */
	public static LinkedList<String> substringBetween(String str, String startString, String stopString,
			LinkedList<Integer> startIndizes) throws Exception {
		LinkedList<String> result = new LinkedList<>();
		int numberOfOpenParenthesis = 0;
		int i_start = 0, i_end = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '|') { // skip over constant expressions: |gdfgs |
				i++;
				for (; str.charAt(i) != '|'; i++) {
				}
				i++;
			}

			if (str.substring(i, i + startString.length()).equals(startString)) { // "("
				numberOfOpenParenthesis++;
				if (numberOfOpenParenthesis == 1) {
					i_start = i; // remember start index of "("
				}
			}
			if (str.substring(i, i + stopString.length()).equals(stopString)) { // ")"
				numberOfOpenParenthesis--;
				if (numberOfOpenParenthesis < 0) {
					throw new Exception(
							"too many closing \"" + stopString + "\":\n" + str.substring(0, i + 1) + "<<<<");
				}
				if (numberOfOpenParenthesis == 0) {
					i_end = i;
					result.add(str.substring(i_start, i_end + 1));
					startIndizes.add(i_start);
				}
			}
		}
		if (numberOfOpenParenthesis != 0) {
			throw new Exception("\"" + startString + "\" never got closed:\n>>>> " + str.substring(i_start));
		}
		return result;
	}

	/**
	 *
	 * @param str         String to separate into Substrings each contained by
	 *                    startString and stopString while ignoring startString and
	 *                    stopString combinations inside such a substring. It also
	 *                    assures that numberOf(startString) == numberOf(stopString)
	 * @param startString e.g. "("
	 * @param stopString  e.g. ")"
	 * @return List of Substrings surrounded by startString and stopString
	 * @throws Exception numberOf(startString) != numberOf(stopString)
	 */
	public static LinkedList<String> substringBetween(String str, String startString, String stopString)
			throws Exception {
		LinkedList<String> result = new LinkedList<>();
		int numberOfOpenParenthesis = 0;
		int i_start = 0, i_end = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '|') { // skip over constant expressions: |gdfgs |
				i++;
				for (; str.charAt(i) != '|'; i++) {
				}
				i++;
			}

			if (str.substring(i, i + startString.length()).equals(startString)) { // "("
				numberOfOpenParenthesis++;
				if (numberOfOpenParenthesis == 1) {
					i_start = i; // remember start index of "("
				}
			}
			if (str.substring(i, i + stopString.length()).equals(stopString)) { // ")"
				numberOfOpenParenthesis--;
				if (numberOfOpenParenthesis < 0) {
					throw new Exception(
							"too many closing \"" + stopString + "\":\n" + str.substring(0, i + 1) + "<<<<");
				}
				if (numberOfOpenParenthesis == 0) {
					i_end = i;
					result.add(str.substring(i_start, i_end + 1));
				}
			}
		}
		if (numberOfOpenParenthesis != 0) {
			throw new Exception("\"" + startString + "\" never got closed:\n>>>> " + str.substring(i_start));
		}
		return result;
	}

	public static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static boolean isDouble(String str) {
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Remove leftmost '|' and rightmost '|' if possible.
	 *
	 * @throws Exception
	 */
	public static String removeOuterSeparators(String str) throws Exception {
		str = str.trim();
		String res;
		if (str.charAt(0) == '|' || str.charAt(str.length() - 1) == '|') {
			if (str.charAt(0) == '|' && str.charAt(str.length() - 1) == '|') {
				res = str.substring(1, str.length() - 1);
			} else {
				throw new Exception("Can't remove outer \"|\" and \"|\" from:\n" + str);
			}
		} else {
			res = str;
		}
		return res;
	}

}
