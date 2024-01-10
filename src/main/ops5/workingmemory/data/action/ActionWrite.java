package ops5.workingmemory.data.action;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.node.NodeTermination;

public final class ActionWrite implements Action {

	final private ImmutableList<Object> writeContents;

	/**
	 * 
	 * @param str "write |car | &#60;carNr> | is bought by customer |
	 *            &#60;customerName> (CRLF) | for | (COMPUTE 3 + &#60atomVar4>) |oz
	 *            gold| (CRLF)"
	 */
	public ActionWrite(String str, Set<String> atomVariables) throws Exception {
		super();
		str = str.substring("write ".length());
		ArrayList<Object> writeContentsBuilder = new ArrayList<>();
		for (int i = 0; i < str.length(); i++) {
			Character c = str.charAt(i);
			switch (c) {
			case ' ':
			case '	':
			case '\n':
				continue; // skip spaces
			case '|':
				int oldi = i;
				i++;
				while (!Objects.equals(str.charAt(i), '|')) {
					i++;
				}
				String atomVar = str.substring(oldi, i + 1);
				writeContentsBuilder.add(atomVar);
				break;
			case '<':
				oldi = i;
				i++;
				while (!Objects.equals(str.charAt(i), '>')) {
					if (Objects.equals(str.charAt(i), '|')) {
						throw new IllegalArgumentException(str);
					}
					i++;
				}
				atomVar = str.substring(oldi, i + 1);
				if (!atomVariables.contains(atomVar)) {
					throw new IllegalArgumentException("Trying to use undefined atomVar: " + atomVar + "\n" + str);
				}
				writeContentsBuilder.add(atomVar);
				break;
			case '(': // (CRLF) or (COMPUTE ...)
				if (Objects.equals("(crlf)", str.substring(i, i + "(crlf)".length()).toLowerCase())) {
					writeContentsBuilder.add("\n");
					i += "(crlf)".length();
				} else if (Objects.equals("(compute ", str.substring(i, i + "(compute ".length()).toLowerCase())) {
					oldi = i;
					i++;
					int parenthesisCount = 1;
					while (parenthesisCount > 0) {
						if (i >= str.length()) {
							throw new IllegalArgumentException("Function compute misses closing brackets:" + str);
						}
						if (Objects.equals(str.charAt(i), '(')) {
							parenthesisCount++;
						} else if (Objects.equals(str.charAt(i), ')')) {
							parenthesisCount--;
						}
						i++;
					}
					FunctionCompute funcComp = new FunctionCompute(str.substring(oldi, i), atomVariables);
					writeContentsBuilder.add(funcComp);
				} else {
					throw new IllegalArgumentException(str);
				}
				break;
			default:
				throw new IllegalArgumentException(c.toString() + " " + str);
			}
		}
		writeContents = ImmutableList.copyOf(writeContentsBuilder);
	}

	@Override
	public Boolean executeAction(ReteEntity reteEntity, NodeTermination from, BigInteger time) throws Exception {
		StringBuilder resultStr = new StringBuilder();
		for (Object obj : writeContents) {
			if (obj instanceof String str) { // string literal, atomvar or crlf
				Character charFirst = str.charAt(0);
				switch (charFirst) {
				case '|':
					resultStr.append(str.substring(1, str.length() - 1));
					break;
				case '<':
					Value<?> value = reteEntity.getValue(str);
					resultStr.append(value.toString());
					break;
				case '\n':
					resultStr.append('\n');
					break;
				default:
					throw new IllegalStateException();
				}
			} else if (obj instanceof FunctionCompute compute) {
				Number number = compute.execute(reteEntity).orElse(null);
				if (number != null) {
					resultStr.append(number.toString());
				} else {
					resultStr.append("NIL");
				}
			}
		}
		System.out.print(resultStr);
		return true;
	}

	@Override
	public String getElemVar() {
		return null;
	}
}
