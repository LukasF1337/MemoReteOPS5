package ops5.workingmemory.data;

import java.util.Objects;
import java.util.Optional;

import ops5.workingmemory.data.condition.ConditionPredicate;

/**
 * @param value     a concrete integer, float or string
 * @param predicate =, >, <>, <, ...
 *
 */
public final record ValueRestriction<T>(Value<T> value, ConditionPredicate predicate) {

	public ValueRestriction {
		// value is allowed to be null, a null value represents OPS5's NIL
		Objects.requireNonNull(predicate);
	}

	public ValueRestriction(Value<T> value, Optional<ConditionPredicate> predicate) {
		this(value, predicate.orElseThrow());
	}
}
