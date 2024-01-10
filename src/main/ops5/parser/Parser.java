package ops5.parser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ops5.Strategy;
import ops5.workingmemory.data.*;
import ops5.workingmemory.data.action.*;
import ops5.workingmemory.data.condition.*;

/**
 * The class Parser handles the parsing of *.ops files to create the working
 * memory elements including literals, facts and production rules for OPS5.
 */
public class Parser {
	/**
	 * case insensitive Unicode pattern for matching
	 */
	final public static Integer patternFlags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
	// Define "classes", each literal contains a list of facts
	final public Map<String, Literal> literals = new LinkedHashMap<>();
	// production rule: IF conditions apply THEN execute actions
	final public Map<String, ProductionRule> productionRules = new LinkedHashMap<>();

	public Strategy strategy = Strategy.LEX;

	/**
	 * Parses OPS5 file and safes it into working memory
	 *
	 * @param filepath Path to OPS5 source code
	 * @throws Exception Error opening the file or error parsing the file
	 */
	public void parseOps5File(String filepath) throws Exception {
		StringBuilder str = new StringBuilder();

		FileReader fr = new FileReader(filepath);
		int r = 0;
		while ((r = fr.read()) != -1) {
			str.append((char) r);
		}
		fr.close();

		parseOps5String(str.toString());
//		this.printAllLiterals();
//		this.printAllProductionRules();
	}

	// Compile patterns only once:
	final static Pattern patternLiteralize = Pattern.compile("^\\(literalize\\s(.*)", patternFlags);
	final static Pattern patternStartup = Pattern.compile("^\\(startup\\s(.*)", patternFlags);
	final static Pattern patternProductionRule = Pattern.compile("^\\(p\\s(.*)", patternFlags);
	final static Pattern patternStrategy = Pattern.compile("^\\(strategy\\s(.*)", patternFlags);
	final static Pattern patternVectorAttribute = Pattern.compile("^\\(VECTOR-ATTRIBUTE\\s(.*)", patternFlags);

	/**
	 * Parses multiple line OPS5 string and safes it into working memory
	 *
	 * @param str String to be parsed and included into working memory
	 * @throws Exception Error in parsing
	 */
	public void parseOps5String(String str) throws Exception {

		// Remove Comments: after ';' remove word, whitespace, tab and special
		// characters except for '\n' until newline and place "\n" there
		// Between \\Q and \\E is automatic escaping of characters
		str = str.replaceAll(";(([\\w\\s\\t\\Q!\"#$§%&'()*+,-./:;<=>?@[\\]^_`{|}~\\E&&[^\n]])*)\n", "\n");
		// TODO the following is wrong for anything between | |
		str = str.replaceAll("[\\t\\n\\r]", " "); // remove tab, newline, carriage-return
		str = str.replaceAll("\\s+", " "); // condense to single whitespace
		str = str.replaceAll("\\(\\s", "\\("); // from "( " to "("
		str = str.replaceAll("\\s\\)", "\\)"); // from " )" to ")"
		str = str.replaceAll("\\{\\s", "\\{"); // from "{ " to "{"
		str = str.replaceAll("\\s\\}", "\\}"); // from " }" to "}"

		LinkedList<String> res = new LinkedList<>();
		res = StringHelpers.substringBetween(str, "(", ")");

		final Matcher matcher = patternLiteralize.matcher("");

		for (String s : res) {
			if (matcher.reset(s).usePattern(patternLiteralize).matches()) { // (literalize ...)
				parseLiteral(s);
			} else if (matcher.reset().usePattern(patternStartup).matches()) { // (startup ...)
				parseStartup(s);
			} else if (matcher.reset().usePattern(patternProductionRule).matches()) { // (p ...)
				parseProductionRule(s);
			} else if (matcher.reset().usePattern(patternStrategy).matches()) { // (strategy ...)
				parseStrategy(s);
			} else if (matcher.reset().usePattern(patternVectorAttribute).matches()) { // (VECTOR-ATTRIBUTE ...)
				throw new Exception("can't parse vector-attribute in this OPS5 implementation:\n" + s);
			} else {
				throw new Exception("can't parse:\n" + s);
			}

		}
	}

	/**
	 * Parse a Literal in the form: (literalize pose name isscheduled)
	 *
	 * @param str String containing the Literal statement
	 */
	private void parseLiteral(String str) throws Exception {
		str = StringHelpers.removeOuterParenthesis(str);
		final String[] s = str.split(" ");
		if (s.length >= 2) { // a literal has at least a name
			String litName;
			ImmutableSet.Builder<String> attrNamesBuilder = new Builder<String>();
			litName = s[1];
			for (int i = 2; i < s.length; i++) {
				attrNamesBuilder.add(s[i]);
			}
			Literal literal = new Literal(Optional.of(litName), Optional.of(attrNamesBuilder.build()));
			literals.put(literal.name(), literal);
		} else {
			throw new Exception("can't parse literal:\n" + str);
		}
	}

	/**
	 * Parse a startup statement: (startup (make robot ^name at1 ^type Pioneer-AT))
	 *
	 * @param str The string that contains the startup statement
	 */
	private void parseStartup(String str) throws Exception {
		str = StringHelpers.removeOuterParenthesis(str);
		LinkedList<String> makes = StringHelpers.substringBetween(str, "(", ")");
		for (String make : makes) {
			parseMakeAndCreateFact(make);
		}

	}

	private void printAllLiterals() {
		final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		System.out.println("======== Literals ========");
		System.out.println("==========================");
		for (Literal literal : literals.values()) {
			System.out.println(gson.toJson(literal));
			System.out.println("==========================");
		}
	}

	private void printAllProductionRules() {
		final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		System.out.println("==== Production Rules ====");
		System.out.println("==========================");
		for (ProductionRule productionRule : productionRules.values()) {
			System.out.println(gson.toJson(productionRule));
			System.out.println("==========================");
		}
	}

	/**
	 * Parse the make line and create fact
	 *
	 * @param str String of the form "(make robot ^name r2d2 ^size 7)"
	 * @throws Exception
	 */
	private void parseMakeAndCreateFact(String str) throws Exception {
		str = StringHelpers.removeOuterParenthesis(str);

		Literal literal = null;
		final ImmutableMap.Builder<String, Value<?>> tmpValues = ImmutableMap.builder();
		final String[] attributes = str.split("\\^");
		// get name of fact:
		final String[] prefix = attributes[0].split(" ");
		if (prefix.length == 2 && prefix[0].toLowerCase().equals("make")) { // if "make robot"
			String name = prefix[1];
			literal = literals.get(name);
			if (literal == null) {
				throw new Exception("Can't find literal \"" + name + "\" for fact:\n" + str);
			}
		} else {
			throw new Exception("Can't parse:\n" + prefix.length + "\nas part of of:\n" + str);
		}

		// get attributes of fact:
		for (int i = 1; i < attributes.length; i++) {
			String[] tmp = attributes[i].split(" ", 2);
			String name = tmp[0];
			String valueString = tmp[1].trim();
			valueString = StringHelpers.removeOuterSeparators(valueString);
			Value<?> newValue = new Value.Builder().setValue(Optional.of(valueString)).build();
			tmpValues.put(name, newValue);
		}
		final Fact fact = new Fact(Optional.of(literal), Optional.of(tmpValues.buildOrThrow()));
		literal.facts().add(fact);
	}

	// @formatter:off
	/**
	 * Parse production rules of the form: <br>
	 * <pre>(p (condition 1)(condition 2)... --> (action 1)(action 2)...) </pre>
	 * including element variables (storing facts) and atom variables (storing float, integer or string)
	 *
	 * @param str
	 * @throws Exception
	 */
	// @formatter:on
	private void parseProductionRule(String str) throws Exception {
		str = StringHelpers.removeOuterParenthesis(str.trim());

		final String prodName;
		final ArrayList<Condition> prodConditions = new ArrayList<>();
		final ArrayList<Action> prodActions = new ArrayList<>();
		final Set<String> prodAtomVariables = new HashSet<>();
		final HashMap<String, Literal> prodElemVariableToLiteral = new HashMap<>();

		final String[] parts = str.split("-->");
		if (parts.length != 2) {
			throw new Exception("Can't parse production rule with too many or no parts:\n" + str);
		}
		final String ConditionFilter = parts[0]; // LHS
		final String actionPart = parts[1]; // RHS

		final String[] tmp;
		tmp = ConditionFilter.split(" ");
		if (tmp.length < 2)
			throw new Exception("Can't parse production rule name:\n" + str);
		prodName = tmp[1];
		if (productionRules.containsKey(prodName)) {
			throw new IllegalArgumentException("Same name for production rule not allowed: " + prodName);
		}
		// Conditions:
		final LinkedList<Integer> startIndizes = new LinkedList<>();
		final LinkedList<Integer> startIndizes2 = new LinkedList<>();
		final String[][] ignores = { { "{", "}" } };
		final LinkedList<String> conditionStrs = StringHelpers.substringBetween(ConditionFilter, "(", ")", startIndizes,
				ignores);
		final String[][] ignores2 = { { "(", ")" } };
		final LinkedList<String> curlyConditionStrs = StringHelpers.substringBetween(ConditionFilter, "{", "}",
				startIndizes2, ignores2);
		TreeMap<Integer, Condition> conditions = new TreeMap<>(); // remember order of conditions
		for (String conditionStr : conditionStrs) { // for each Condition
			Integer startIndex = startIndizes.removeFirst();
			Boolean exists;
			if (ConditionFilter.charAt(startIndex - 1) == '-' || ConditionFilter.charAt(startIndex - 2) == '-') {
				exists = false;
			} else {
				exists = true;
			}
			Condition condition = parseCondition(conditionStr, exists, prodAtomVariables);
			conditions.put(startIndex, condition);
		}
		for (String curlyConditionStr : curlyConditionStrs) { // for each ElementVariableCondition
			Integer startIndex = startIndizes2.removeFirst();
			Boolean exists;
			if (ConditionFilter.charAt(startIndex - 1) == '-' || ConditionFilter.charAt(startIndex - 2) == '-') {
				throw new Exception("Can't mark Condition with Element Variable as nonexistent:\n" + curlyConditionStr);
			} else {
				exists = true;
			}
			Condition condition = parseElementVariableCondition(curlyConditionStr, exists, prodAtomVariables,
					prodElemVariableToLiteral);
			conditions.put(startIndex, condition);
		}
		for (Entry<Integer, Condition> e : conditions.entrySet()) {
			prodConditions.add(e.getValue());
		}
		// Actions:
		final LinkedList<String> actionStrs = StringHelpers.substringBetween(actionPart, "(", ")", startIndizes);
		for (String actionStr : actionStrs) {
			Action action = parseAction(actionStr, prodAtomVariables, prodElemVariableToLiteral);
			prodActions.add(action);
		}
		// Production Rules:
		ProductionRule prodRule = new ProductionRule(Optional.of(prodName), Optional.of(prodConditions),
				Optional.of(prodActions), Optional.of(prodAtomVariables),
				Optional.of(ImmutableMap.copyOf(prodElemVariableToLiteral)));
		productionRules.put(prodRule.name(), prodRule);
	}

	/**
	 * Parse a condition which is assigned to an element variable
	 *
	 * @param str      {{@literal<}elementVariable{@literal>}(Condition)}
	 * @param exists
	 * @param prodRule
	 * @return
	 * @throws Exception
	 */
	private Condition parseElementVariableCondition(String str, Boolean exists, Set<String> atomVariables,
			HashMap<String, Literal> elementVariableToLiteral) throws Exception {
		str = StringHelpers.removeOuterCurlyParenthesis(str).trim();
		final String[][] ignores = { { "(", ")" } };
		final LinkedList<Integer> startIndizes = new LinkedList<>();
		final LinkedList<String> tmpElementVariables = StringHelpers.substringBetween(str, "<", ">", startIndizes,
				ignores);
		if (tmpElementVariables.size() != 1) {
			throw new Exception(
					"Expected 1 Element variable {<elemVar>(condition)} but got:\n" + elementVariableToLiteral);
		}
		final String elementVariable = tmpElementVariables.getFirst().trim();

		final String prefix = str.substring(0, startIndizes.getFirst());
		final String suffix = str.substring(startIndizes.getFirst() + elementVariable.length());
		str = prefix + suffix;

		final Condition tmpCond = parseCondition(str, exists, atomVariables);
		elementVariableToLiteral.put(elementVariable, tmpCond.literal());
		return new Condition(Optional.of(tmpCond.exists()), Optional.of(tmpCond.literal()),
				Optional.of(elementVariable), Optional.of(tmpCond.conditionFilters()));
	}

	/**
	 * Parse a condition
	 *
	 * @param str      (robot ^name {@literal<}ro1{@literal>} ^type Pioneer-AT)
	 * @param exists
	 * @param prodRule
	 * @return
	 * @throws Exception
	 */
	private Condition parseCondition(String str, Boolean exists, Set<String> atomVariables) throws Exception {
		str = StringHelpers.removeOuterParenthesis(str);
		final String[] parts = str.split("\\^");
		final String literalString = parts[0].trim();
		final Literal literal = literals.get(literalString);
		if (literal == null) {
			throw new Exception("Can't find Literal " + literalString + "\nfor Condition:\n" + str);
		}

		ImmutableList.Builder<ConditionFilter> conditionFiltersBuilder = new ImmutableList.Builder<>();

		for (int i = 1; i < parts.length; i++) {
			conditionFiltersBuilder.add(parseConditionFilter(parts[i], exists, atomVariables));
		}
		if (parts.length == 1 && exists == false) {
			// add condition part that filters for pure nonexistence
			ConditionFilter condFilter = new ConditionFilter(null, null, null, null);
			conditionFiltersBuilder.add(condFilter);
		}

		return new Condition(Optional.of(exists), Optional.of(literal), Optional.empty(),
				Optional.of(conditionFiltersBuilder.build()));
	}

	/**
	 * Parse a ConditionFilter e.g. "x
	 * {@literal<} {@literal<}atomVariable{@literal>}" containing an attribute name
	 * "x" a ConditionPredicate "{@literal<}" and a immediate value "154" or atom
	 * variable "{@literal<}atomVariable{@literal>}"
	 *
	 * @param str "x {@literal<} {@literal<}atomVariable{@literal>}"
	 * @throws Exception Parse error
	 */
	private ConditionFilter parseConditionFilter(String str, Boolean exists, Set<String> atomVariables)
			throws Exception {
		final String attribute;
		final ConditionPredicate predicate;

		String[] parts = str.split(" ", 2);
		attribute = parts[0];
		String attributeValueElement = parts[1].trim(); // "x < <atomVariable>"
		attributeValueElement = StringHelpers.removeOuterSeparators(attributeValueElement);
		int numOfParts = countConditionFilterParts(attributeValueElement);
		if (numOfParts == 1) {
			attributeValueElement = "= " + attributeValueElement;
		} else if (numOfParts > 2) {
			throw new Exception("can't parse:\n" + attributeValueElement + "\ninside:\n" + str);
		}

		parts = attributeValueElement.split(" ", 2);

		if (parts[0].equals("=")) {
			predicate = ConditionPredicate.EQUAL;
		} else if (parts[0].equals("<>")) {
			predicate = ConditionPredicate.UNEQUAL;
		} else if (parts[0].equals("<=>")) {
			predicate = ConditionPredicate.SAMETYPE;
		} else if (parts[0].equals("<")) {
			predicate = ConditionPredicate.SMALLER;
		} else if (parts[0].equals(">")) {
			predicate = ConditionPredicate.BIGGER;
		} else if (parts[0].equals("<=")) {
			predicate = ConditionPredicate.SMALLEREQUAL;
		} else if (parts[0].equals(">=")) {
			predicate = ConditionPredicate.BIGGERQUAL;
		} else {
			throw new Exception("Can't parse Condition Predicate:\n" + parts[0] + "\ninside:\n" + str);
		}

		Value<?> value = new Value.Builder().setValue(Optional.of(parts[1])).build();
		if (value != null && value.attributeType().equals(ValueType.ATOMVARIABLE)) {
			atomVariables.add(value.attributeValue().toString());
		}

		return new ConditionFilter(attribute, predicate, value, null);
	}

	/**
	 * Count parts, for example 2 parts: "{@literal<} 5", 1 part: " 7"
	 *
	 * @param str like "> 3" or "= |a test string|"
	 * @return number of parts
	 */
	private int countConditionFilterParts(String str) {
		int numOfParts = 1;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '|') { // skip over constant expressions: |gdfgs |
				i++;
				for (; str.charAt(i) != '|'; i++) {
				}
				i++;
			}
			if (str.charAt(i) == ' ') {
				numOfParts++;
			}
		}
		return numOfParts;
	}

	/**
	 * Parse action of type make, modify, remove, write or call and append it to the
	 * production rule. Can not remove or modify by index e.g. (remove 2) won't
	 * work.
	 *
	 * @param str
	 * @throws Exception
	 */
	private Action parseAction(String str, Set<String> atomVariables, HashMap<String, Literal> elemVariableToLiteral)
			throws Exception {
		str = StringHelpers.removeOuterParenthesis(str);
		final Action action;
		final Pattern make = Pattern.compile("^make\\s(.*)", patternFlags);
		final Pattern modify = Pattern.compile("^modify\\s(.*)", patternFlags);
		final Pattern remove = Pattern.compile("^remove\\s(.*)", patternFlags);
		final Pattern write = Pattern.compile("^write\\s(.*)", patternFlags);
		final Pattern call = Pattern.compile("^call\\s(.*)", patternFlags);
		final Pattern callRobo = Pattern.compile("^callrobo\\s(.*)", patternFlags);
		final Pattern halt = Pattern.compile("^halt$", patternFlags);
		final Matcher matcher = make.matcher("");

		if (matcher.reset(str).usePattern(make).matches()) { // (make ...)
			action = new ActionMake(str, literals, atomVariables);
		} else if (matcher.reset().usePattern(modify).matches()) { // (modify ...)
			action = new ActionModify(str, atomVariables, elemVariableToLiteral);
		} else if (matcher.reset().usePattern(remove).matches()) { // (remove ...)
			action = new ActionRemove(str, elemVariableToLiteral);
		} else if (matcher.reset().usePattern(write).matches()) { // (write ...)
			action = new ActionWrite(str, atomVariables);
		} else if (matcher.reset().usePattern(call).matches()) { // (call ...)
			action = new ActionCall(str, literals, atomVariables, elemVariableToLiteral);
		} else if (matcher.reset().usePattern(halt).matches()) {
			action = new ActionHalt();
		} else {
			throw new Exception("Could not parse action:\n" + str);
		}
		return action;
	}

	private void parseStrategy(String str) throws Exception {
		str = StringHelpers.removeOuterParenthesis(str);
		String[] s = str.split(" ");
		if (s.length == 2) { // a literal has at least a name
			String strat = s[1];
			if (strat.toLowerCase().equals("lex")) {
				this.strategy = Strategy.LEX;
			} else if (strat.toLowerCase().equals("mea")) {
				this.strategy = Strategy.MEA;
			} else {
				throw new Exception("can't parse strategy:\n" + str);
			}
		} else {
			throw new Exception("can't parse strategy:\n" + str);
		}
	}

	public String parseOps5Model() {
		return "";
	}

	public void merge(Parser otherModel) {
	}

	public void unlinkFacts() {
		for (Entry<String, Literal> literalEntry : literals.entrySet()) {
			Literal literal = literalEntry.getValue();
			literal.facts().clear();
		}
	}

}
