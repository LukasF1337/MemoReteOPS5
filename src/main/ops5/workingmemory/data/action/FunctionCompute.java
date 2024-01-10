package ops5.workingmemory.data.action;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.*;

import ops5.parser.Parser;
import ops5.parser.StringHelpers;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.ValueType;

public class FunctionCompute {

	/*
	 * Original OPS5 compute is severely limited. S.51. Change that?
	 */

	// pattern for float and integer
	final private static Pattern numberPattern = Pattern.compile("^[0-9]*\\.?[0-9]+", Parser.patternFlags);
	final private ImmutableList<ComputeEntry<?>> computeLoad;

	private enum ComputeEntryType {
		UP(ImmutableList.class), // start with "(" containing another ImmutableList<ComputeEntry> until it closes
									// with ")"
		NUMBER(Number.class), // Number like "8"
		ATOMVARVALUE(String.class), // AtomvarValue like "<speed>"
		OPERATOR(Character.class); // Operator like '*'

		private final Class<?> clazz;

		ComputeEntryType(Class<?> clazz) {
			this.clazz = clazz;
		}

		public Class<?> getClazz() {
			return clazz;
		}
	}

	/**
	 * record ComputeEntry is there to store a tuple of (ComputeEntryType,
	 * computeEntry)
	 *
	 */
	private record ComputeEntry<T>(ComputeEntryType computeEntryType, T computeEntry) {
		public ComputeEntry {
			Objects.requireNonNull(computeEntryType);
			Objects.requireNonNull(computeEntry);
			switch (computeEntryType) {
			case ATOMVARVALUE:
				assert (((String) computeEntry).startsWith("<"));
				assert (((String) computeEntry).endsWith(">"));
				break;
			case NUMBER:
				if (computeEntry instanceof Value<?> val && (val.attributeType().equals(ValueType.INTEGER)
						|| val.attributeType().equals(ValueType.DOUBLE))) {
				} else {
					throw new IllegalStateException();
				}
				break;
			case OPERATOR:
				if (computeEntry instanceof Character c) {
					switch (c) {
					case '/': // divide
					case '\\': // modulo
					case '+': // plus
					case '-': // minus
					case '*': // multiply
						break;
					default:
						throw new IllegalArgumentException();
					}
				} else {
					throw new IllegalArgumentException();
				}
				break;
			case UP:
				assert (computeEntry instanceof ImmutableList);
				break;
			default:
				throw new IllegalStateException();
			}
		}
	}

	private class CalculatorTriplet {
		private Number first; // "2"
		private Character operator; // '+'
		private Number second; // "3.0"

		public void putNumber(Number num) {
			assert (num != null);
			if (first == null) {
				first = num;
			} else if (second == null) {
				second = num;
				first = this.calculate();
			} else {
				throw new IllegalArgumentException();
			}
		}

		public <U> void putNumber(Value<U> val) {
			assert (val != null);
			if (val.attributeType() == ValueType.DOUBLE || val.attributeType() == ValueType.INTEGER) {
				assert (val.attributeValue() instanceof Number);
				this.putNumber((Number) val.attributeValue());
			} else {
				throw new IllegalArgumentException("" + val);
			}
		}

		public void putOperator(Character charOp) {
			assert (charOp != null);
			if (operator == null) {
				operator = charOp;
			} else {
				throw new IllegalArgumentException();
			}
		}

		private Number calculate() {
			if (first == null || operator == null || second == null) {
				throw new IllegalArgumentException();
			}
			final Number res;
			if (first instanceof Integer iFirst && second instanceof Integer iSecond) {
				switch (operator) {
				case '/': // divide
					res = iFirst / iSecond;
					break;
				case '\\': // modulo
					res = iFirst % iSecond;
					break;
				case '+': // plus
					res = iFirst + iSecond;
					break;
				case '-': // minus
					res = iFirst - iSecond;
					break;
				case '*': // multiply
					res = iFirst * iSecond;
					break;
				default:
					throw new IllegalStateException();
				}
			} else {
				switch (operator) {
				case '/': // divide
					res = first.doubleValue() / second.doubleValue();
					break;
				case '\\': // modulo
					// should not work according to original OPS5, but why not make it work?
					res = first.doubleValue() % second.doubleValue();
					break;
				case '+': // plus
					res = first.doubleValue() + second.doubleValue();
					break;
				case '-': // minus
					res = first.doubleValue() - second.doubleValue();
					break;
				case '*': // multiply
					res = first.doubleValue() * second.doubleValue();
					break;
				default:
					throw new IllegalStateException();
				}
			}
			first = null;
			operator = null;
			second = null;
			return res;
		}

		public Number getResult() {
			if (first == null || operator != null || second != null) {
				throw new IllegalStateException();
			}
			return first;
		}
	}

	/**
	 * Compute is a Function that computes strings like "(COMPUTE 3 + &#60atomVar4>
	 * * (1-7))". Elemvars are irrelevant for compute, but it uses Atomvars.
	 * Operations supported are: + plus,- minus, * multiply, // divide, \\ modulo
	 * (for integers). Evaluation left to right, meaning it disregards operator
	 * precedence for example "*" is not evaluated before "+" which is contrary to
	 * standard math. Respects parenthesis "()". If one Value is Float, all is
	 * Float, otherwise all is Integer (which is still not fully specified). Can not
	 * write "-&#60atomVar1>", must write "0-&#60atomVar1>" instead.
	 * 
	 * @param str      String of compute instruction to parse.
	 * @param atomvars that are allowed to use. If str contains a atomvar not in
	 *                 atomvars, throw error.
	 */
	public FunctionCompute(String str, Set<String> atomvars) throws Exception {
		if (!str.toUpperCase().startsWith("(COMPUTE ")) {
			throw new IllegalArgumentException("No legitimate compute function: " + str);
		}
		str = StringHelpers.removeOuterParenthesis(str);
		str = str.substring("COMPUTE ".length(), str.length());
		final Stack<LinkedList<ComputeEntry<?>>> builders = new Stack<>();
		LinkedList<ComputeEntry<?>> currentBuilder = null;
		final Stack<CalculatorTriplet> validators = new Stack<>();
		CalculatorTriplet currentValidator = null;
		final int length = str.length();
		for (int i = 0; i < length; i++) {
			if (currentBuilder == null) {
				currentBuilder = new LinkedList<>();
			}
			if (currentValidator == null) {
				currentValidator = new CalculatorTriplet();
			}
			char currentChar = str.charAt(i);
			switch (currentChar) {
			case ' ': // space
			case '	': // tab
			case '\n': // new line
				continue; // skip empty spaces
			case '(':
				assert (currentBuilder != null);
				builders.push(currentBuilder);
				validators.push(currentValidator);
				currentBuilder = null;
				currentValidator = null;
				break;
			case ')':
				assert (currentBuilder != null);
				final ImmutableList<ComputeEntry<?>> built = ImmutableList.copyOf(currentBuilder);
				Number numTmp = currentValidator.getResult(); // run validity test
				currentBuilder = builders.pop();
				currentValidator = validators.pop();
				ComputeEntry<?> newEntry = new ComputeEntry<Object>(ComputeEntryType.UP, built);
				currentBuilder.add(newEntry);
				currentValidator.putNumber(numTmp);
				break;
			case '<': // atomvar
				final int startIndex = i;
				while (currentChar != '>') {
					i++;
					currentChar = str.charAt(i);
				}
				final int endIndex = i + 1;
				String atomvar = str.substring(startIndex, endIndex);
				if (!atomvars.contains(atomvar)) {
					throw new IllegalArgumentException("Trying to use atomvar \"" + atomvar + " in compute: \"" + str
							+ "\"\n is impossible, because the atomvar was not specified in the production rule before.");
				}
				assert (atomvar.charAt(0) == '<' && atomvar.charAt(atomvar.length() - 1) == '>');
				newEntry = new ComputeEntry<Object>(ComputeEntryType.ATOMVARVALUE, atomvar);
				currentBuilder.add(newEntry);
				currentValidator.putNumber(1d);
				break;
			case '/': // divide
			case '\\': // modulo
				if (str.startsWith("//", i)) { // divide
				} else if (str.startsWith("\\\\", i)) { // modulo (for integers)
				} else {
					throw new IllegalStateException("Error parsing: " + str
							+ "\nMake sure you use // for divide and \\\\ for modulo operations");
				}
				i++;
			case '+': // plus
			case '-': // minus
			case '*': // multiply
				ComputeEntry<?> computeEntry = new ComputeEntry<Object>(ComputeEntryType.OPERATOR, currentChar);
				currentBuilder.add(computeEntry);
				currentValidator.putOperator('+');
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.': // Implicit 0.XYZ float number.
				final Matcher matcher = numberPattern.matcher(str.subSequence(i, str.length()));
				if (matcher.find()) {
					String numberString = matcher.group();
					i += numberString.length() - 1;
					// Parse the string as an integer
					Number number = null;
					try {
						number = Integer.parseInt(numberString);
					} catch (NumberFormatException e) {
						number = Double.parseDouble(numberString);
					}
					final Value<?> newVal;
					if (number instanceof Integer) {
						newVal = new Value<Object>(ValueType.INTEGER, number);
					} else if (number instanceof Double) {
						newVal = new Value<Object>(ValueType.DOUBLE, number);
					} else {
						throw new IllegalStateException();
					}
					computeEntry = new ComputeEntry<Object>(ComputeEntryType.NUMBER, newVal);
					currentBuilder.add(computeEntry);
					currentValidator.putNumber(1d);
				} else {
					throw new IllegalStateException(
							"Couldn't find Integer/Float in: " + str + "\nstarting with: \"" + currentChar + "\"");
				}
				break;
			default:
				throw new IllegalStateException("Error parsing: " + str + "On character \"" + currentChar + "\"");
			}
		}
		if (builders.size() != 0) {
			throw new IllegalArgumentException("No matching closing brackets on:\n" + str);
		}
		Objects.requireNonNull(currentValidator);
		// if pass, then compute function provided by user is valid
		currentValidator.getResult(); // run validity test
		assert (currentBuilder != null);
		computeLoad = ImmutableList.copyOf(currentBuilder);
	}

	/**
	 * Execute the (COMPUTE ...) function
	 */
	public Optional<Number> execute(ReteEntity reteEntity) throws Exception {
		final Stack<UnmodifiableIterator<ComputeEntry<?>>> stackIter = new Stack<>();
		UnmodifiableIterator<ComputeEntry<?>> currentIter = this.computeLoad.iterator();
		final Stack<CalculatorTriplet> stackCalculators = new Stack<>();
		CalculatorTriplet currentCalculator = new CalculatorTriplet();

		while (stackIter.size() > 0 || currentIter.hasNext()) {
			if (!currentIter.hasNext()) {
				// case DOWN:
				Number tmpResult = currentCalculator.getResult();
				currentCalculator = stackCalculators.pop();
				currentCalculator.putNumber(tmpResult);
				currentIter = stackIter.pop();
				continue;
			}
			ComputeEntry<?> currentEntry = currentIter.next();
			switch (currentEntry.computeEntryType) {
			case UP:
				stackCalculators.add(currentCalculator);
				stackIter.add(currentIter);
				currentCalculator = new CalculatorTriplet();
				currentIter = ((ImmutableList<ComputeEntry<?>>) currentEntry.computeEntry()).iterator();
				break;
			case NUMBER:
				Number num = (Number) ((Value<?>) currentEntry.computeEntry).attributeValue();
				currentCalculator.putNumber(num);
				break;
			case ATOMVARVALUE:
				String atomVar = (String) currentEntry.computeEntry;
				Value<?> val = reteEntity.getValue(atomVar);
				if (val == null || val.attributeType().equals(ValueType.NIL)
						|| !(val.attributeType().equals(ValueType.DOUBLE)
								|| val.attributeType().equals(ValueType.INTEGER))) {
					return Optional.empty();
				}
				currentCalculator.putNumber(val);
				break;
			case OPERATOR:
				Character chara = (Character) currentEntry.computeEntry;
				currentCalculator.putOperator(chara);
				break;
			default:
				throw new IllegalStateException();
			}
		}
		return Optional.of(currentCalculator.getResult());
	}

	/**
	 * Execute the (COMPUTE ...) function
	 */
	public Optional<Value<?>> executeAndGetValue(ReteEntity reteEntity) throws Exception {
		Number newNumber = this.execute(reteEntity).get();
		if (newNumber == null) {
			return Optional.empty();
		}
		final Value<?> resultValue;
		if (newNumber instanceof Integer) {
			resultValue = new Value<Integer>(ValueType.INTEGER, newNumber.intValue());
		} else if (newNumber instanceof Double) {
			resultValue = new Value<Double>(ValueType.DOUBLE, newNumber.doubleValue());
		} else {
			throw new IllegalStateException();
		}
		return Optional.of(resultValue);
	}
}
