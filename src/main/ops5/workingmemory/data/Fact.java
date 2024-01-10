package ops5.workingmemory.data;

import java.util.Optional;
import java.util.LinkedHashMap;
import com.google.common.collect.ImmutableMap;

/**
 * @param referenceLiteral the "class" of this instance of fact
 * @param values           maps attribute names to their values
 *
 */
public final record Fact(ImmutableMap<String, Value<?>> attributeValues) {

	public Fact {
	}

	public Fact(Optional<Literal> literal, Optional<ImmutableMap<String, Value<?>>> values) {
		this(values.orElseThrow());
		if (literal.get().attributeNames().containsAll(values.get().keySet())) {
		} else {
			throw new RuntimeException("Literal " + literal + "\n does not contain all values of:" + values);
		}
	}

	public <T> Value<T> getValue(String attributeName) {
		Value<T> val = (Value<T>) this.attributeValues.get(attributeName);
		if (val == null) {
			val = new Value<>(ValueType.NIL, null);
		}
		return val;
	}

	@Override
	public String toString() {
		return "Fact[" + this.attributeValues().toString() + "]";
	}

	public Fact copy() {
		// correct because ImmutableMap, String and Value are completely immutable
		// (Integer and Float are immutable as well).
		return this;
	}
}
