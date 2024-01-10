package ops5.workingmemory.data.condition;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.ValueType;

/**
 * @param attribute;       // the attribute tested e.g. color of a car
 * @param predicate;       // >, <>, <, ...
 * @param value;           // the value tested, e.g. a literal like "red" or an
 *                         AtomVariable Object &#60atomVariable>
 * @param elementVariable; // the element variable or null if there is none
 */
public final record ConditionFilter( //
		String attribute, //
		ConditionPredicate predicate, //
		Value<?> value, //
		String elementVariable) {

	public ConditionFilter {
		if (attribute == null && predicate == null && value == null) {
			// create an all accepting Filter
			predicate = ConditionPredicate.RETURNTRUE;
			value = null;
		} else if (attribute != null && predicate != null) {
			// value is allowed to be null
			// create a normal actual Filter
		} else {
			throw new IllegalArgumentException();
		}
		Objects.nonNull(predicate);
	}

	/**
	 * apply the filter on fact. If the fact fulfills the filter constraint return
	 * true else false. This is used for filtering in alpha nodes.
	 * 
	 * @param fact
	 * @return
	 */
	public boolean applyFilter(Fact fact) {
		Value<?> v2 = fact.getValue(attribute);
		return match(v2, this.predicate, this.value);
	}

	public static boolean match(Value<?> v1, ConditionPredicate predicate, Value<?> v2) {
		boolean res = false;
		switch (predicate) {
		case EQUAL:
			res = Value.equals(v1, v2);
			break;
		case UNEQUAL:
			res = Value.notequals(v1, v2);
			break;
		case SAMETYPE:
			res = Value.isSameType(v1, v2);
			break;
		case SMALLER:
			res = Value.isSmallerThan(v1, v2);
			break;
		case BIGGER:
			res = Value.isBiggerThan(v1, v2);
			break;
		case SMALLEREQUAL:
			res = Value.isSmallerEqualThan(v1, v2);
			break;
		case BIGGERQUAL:
			res = Value.isBiggerEqualThan(v1, v2);
			break;
		case RETURNTRUE:
			// in cases like (p shift-context (city) -->...)
			res = true;
			break;
		default:
			throw new IllegalArgumentException("" + predicate);
		}
		return res;
	}
}
