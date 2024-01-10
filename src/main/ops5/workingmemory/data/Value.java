package ops5.workingmemory.data;

import java.util.EnumMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import ops5.parser.StringHelpers;

public final record Value<T>(ValueType attributeType, T attributeValue) implements Comparable<Value<T>> {

	// in a future Java version one could use a value class instead of a record.
	public Value {
		if (attributeValue instanceof String str && attributeType == ValueType.STRING //
				&& '<' != str.charAt(0) //
				&& '>' != str.charAt(str.length() - 1)) {
			// legitimate value
		} else if (attributeValue instanceof String str && attributeType == ValueType.ATOMVARIABLE //
				&& '<' == str.charAt(0) //
				&& '>' == str.charAt(str.length() - 1)) {
			if (str.contains("^")) {
				throw new IllegalStateException("\"^\" not allowed inside atomvariable name " + str);
			}
			// legitimate value
		} else if (attributeValue instanceof Integer && attributeType == ValueType.INTEGER) {
			// legitimate value
		} else if (attributeValue instanceof Double && attributeType == ValueType.DOUBLE) {
			// legitimate value
		} else if (attributeValue == null && attributeType == ValueType.NIL) {
			// legitimate value, needed for valueMap in NodeBeta's
			// FIXME implications, it was not allowed to use NIL before.
		} else {
			// impossible value
			throw new NoSuchElementException("Type: " + attributeType + " Value: " + attributeValue);
		}
	}

	@Override
	public String toString() {
		return Objects.toString(this.attributeValue, "");
	}

	public Value(Optional<ValueType> attributeType, Optional<T> attributeValue) {
		this(attributeType.orElseThrow(), attributeValue.orElse(null));
	}

//	/**
//	 * Create NIL value. To create Values with data use ValueBuilder.
//	 */
//	public Value() {
//		this(ValueType.NIL, null);
//	}

	private static Value<?> convertNilToNull(Value<?> val) {
		if (val != null && val.equals(new Value<>(ValueType.NIL, null))) {
			val = null;
		}
		return val;
	}

	private enum NumPos {
		first, second;
	}

	public static boolean equals(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		final boolean res;
		if (v1 == null && v2 == null) {
			res = true;
		} else if (v1 == null || v2 == null) {
			res = false;
		} else {
			res = v1.equals(v2);
		}
		return res;
	}

	// Integer 5 must not equal float 5.0!! Value and Type must be equal according
	// to OPS5.
//	@Override
//	public boolean equals(Object other);

	public static boolean notequals(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		final boolean res;
		if (v1 == null && v2 == null) {
			res = false;
		} else if (v1 == null || v2 == null) {
			res = true;
		} else {
			res = !v1.equals(v2);
		}
		return res;
	}

	// /**
	// * Create NIL value. To create Values with data use ValueBuilder.
	// */
	// public Value() {
	// this(ValueType.NIL, null);
	// }

	public static boolean isNumber(Value<?> val) {
		if (val != null && (val.attributeType == ValueType.DOUBLE || val.attributeType == ValueType.INTEGER)) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean comparable(Value<?> v1, Value<?> v2) {
		if (isNumber(v1) && isNumber(v2)) {
			return true;
		} else {
			return false;
		}
	}

	// Casting int to double should keep comparison accurate, because double has a
	// 52bit mantissa (fraction) while int has 32bits overall maximum.
	private static void getDoubles(Object first, Object second, EnumMap<NumPos, Double> numbers) {
		numbers.put(NumPos.first, ((Number) first).doubleValue());
		numbers.put(NumPos.second, ((Number) second).doubleValue());
	}

	public static boolean isBiggerThan(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		if (v1 == null || v2 == null) {
			return false;
		}
		forbidAtomvarComparison(v1, v2);
		boolean res = comparable(v1, v2);
		final EnumMap<NumPos, Double> numbers = new EnumMap<>(NumPos.class);
		getDoubles(v1.attributeValue, v2.attributeValue, numbers);
		res = res && numbers.get(NumPos.first) > numbers.get(NumPos.second);
		return res;
	}

	public static boolean isBiggerEqualThan(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		if (v1 == null || v2 == null) {
			return false;
		}
		forbidAtomvarComparison(v1, v2);
		boolean res = comparable(v1, v2);
		final EnumMap<NumPos, Double> numbers = new EnumMap<>(NumPos.class);
		getDoubles(v1.attributeValue, v2.attributeValue, numbers);
		res = res && numbers.get(NumPos.first) >= numbers.get(NumPos.second);
		return res;
	}

	public static boolean isSmallerThan(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		if (v1 == null || v2 == null) {
			return false;
		}
		forbidAtomvarComparison(v1, v2);
		boolean res = comparable(v1, v2);
		final EnumMap<NumPos, Double> numbers = new EnumMap<>(NumPos.class);
		getDoubles(v1.attributeValue, v2.attributeValue, numbers);
		res = res && numbers.get(NumPos.first) < numbers.get(NumPos.second);
		return res;
	}

	public static boolean isSmallerEqualThan(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		if (v1 == null || v2 == null) {
			return false;
		}
		forbidAtomvarComparison(v1, v2);
		boolean res = comparable(v1, v2);
		final EnumMap<NumPos, Double> numbers = new EnumMap<>(NumPos.class);
		getDoubles(v1.attributeValue, v2.attributeValue, numbers);
		res = res && numbers.get(NumPos.first) <= numbers.get(NumPos.second);
		return res;
	}

	public static boolean isSameType(Value<?> v1, Value<?> v2) {
		v1 = convertNilToNull(v1);
		v2 = convertNilToNull(v2);
		final boolean res;
		if (v1 == null && v2 == null) {
			res = true;
		} else if (v1 == null || v2 == null) {
			// I wanted to use XOR "^" instead of "||", but eclipse complains
			res = false;
		} else {
			forbidAtomvarComparison(v1, v2);
			if (v1.attributeType == v2.attributeType) {
				res = true;
			} else {
				res = false;
			}
		}
		return res;
	}

	private static void forbidAtomvarComparison(Value<?> v1, Value<?> v2) {
		if (v1.attributeType == ValueType.ATOMVARIABLE || v2.attributeType == ValueType.ATOMVARIABLE) {
			throw new IllegalStateException();
		}
	}

	public Value<T> copy() throws Exception {
		ValueType attributeType;
		T attributeValue;

		// Copy attribute value type:
		attributeType = this.attributeType;
		// Copy attribute value:
		if (attributeType == ValueType.INTEGER) {
			attributeValue = this.attributeValue; // working copy (doesn't get changed if original value changes)
		} else if (attributeType == ValueType.DOUBLE) {
			attributeValue = this.attributeValue; // working copy
		} else if (attributeType == ValueType.STRING) {
			attributeValue = this.attributeValue; // Copy reference to immutable String
		} else if (attributeType == ValueType.ATOMVARIABLE) {
			attributeValue = this.attributeValue; // Copy reference to immutable String of atom variable
		} else {
			throw new Exception("Unimplemented Attribute Type:" + this);
		}
		return new Value<T>(Optional.of(attributeType), Optional.of(attributeValue));
	}

	public int compareToHelper(Value<T> v2) {
		final int res;
		if (comparable(this, v2)) {
			if (isBiggerThan(this, v2)) {
				res = 1;
			} else if (this.equals(v2)) {
				res = 0;
			} else { // this.isSmallerThan(o)
				res = -1;
			}
		} else if (this.attributeType.equals(ValueType.STRING) && v2.attributeType.equals(ValueType.STRING)) {
			res = ((String) this.attributeValue).compareTo(((String) v2.attributeValue));
		} else if (this.equals(v2)) {
			res = 0;
		} else {
			throw new IllegalArgumentException("Comparing Values of any type that is not numeric is disallowed");
		}
		return res;
	}

	@Override
	public int compareTo(Value v2) {
		final int res;
		res = this.compareToHelper(v2);
		return res;
	}

	@Override
	public int hashCode() {
		final int result;
		if (this.attributeValue instanceof Number num) {
			// every Number gets the hashCode of its Double value, because hashCode of 5
			// (Integer) and 5.0 (Float) must be equal for Lookups.
			result = Objects.hashCode(num.doubleValue());
		} else {
			result = Objects.hashCode(this.attributeType);
		}
		return result;
	}

	public final static class Builder {
		private ValueType attributeType;
		private Object attributeValue;

		/**
		 * Create NIL value, which requires a non NIL setValue() before it can be built.
		 */
		public Builder() {
			this.attributeType = ValueType.NIL;
			this.attributeValue = null;
		}

		public Builder setValue(Optional<String> valueString) throws Exception {

			String str = valueString.orElseThrow().trim();

			final ValueType type;
			if (str.charAt(0) == '<' && str.charAt(str.length() - 1) == '>') {
				type = ValueType.ATOMVARIABLE;
			} else if (str.equals("NIL")) {
				type = ValueType.NIL;
			} else if (StringHelpers.isInteger(str)) {
				type = ValueType.INTEGER;
			} else if (StringHelpers.isDouble(str)) {
				type = ValueType.DOUBLE;
			} else if (str.contains("<") || str.contains(">")) {
				throw new IllegalArgumentException("Strings are not allowed to contain ’<’ or ’>’: " + str
						+ "\n These characters are reserved for atom and element variables.");
			} else if (str.contains("|")) {
				throw new IllegalStateException("Strings are not allowed to contain ’|’ inside the string: " + str
						+ "\n This should never happen. Implementation error.");
			} else {
				type = ValueType.STRING;
			}
			this.attributeType = type;

			final Object val;
			switch (type) {
			case INTEGER:
				val = Integer.parseInt(str);
				break;
			case DOUBLE:
				val = Double.parseDouble(str);
				break;
			case STRING:
				str = str.trim();
				str = StringHelpers.removeOuterSeparators(str);
				val = str;
				break;
			case ATOMVARIABLE:
				val = str;
				break;
			case NIL:
			default:
				val = null;
				break;
			}
			this.attributeValue = val;

			return this;
		}

		public Value<?> build() {
			final Value<?> res;
			if (this.attributeType == ValueType.NIL && this.attributeValue == null) {
				res = null;
			} else {
				res = new Value<Object>(this.attributeType, this.attributeValue);
			}
			return res;
		}
	}
}
