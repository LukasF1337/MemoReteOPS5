package ops5.workingmemory.data;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * @param name           name of the literal, for example "car". Quite similar
 *                       to C structs or Java classes (without methods).
 * @param attributeNames attributes of the literal. For example
 *                       {"color","numberOfWheels", ...}. Although it is a Set,
 *                       ImmutableSet preserves the insertion order.
 * @param facts          incarnations/instances of the literal. Like objects in
 *                       java. Facts are mutable.
 */
public final record Literal(String name, ImmutableSet<String> attributeNames, ArrayList<Fact> facts)
		implements Cloneable {

	public Literal {
		Objects.requireNonNull(name);
		Objects.requireNonNull(attributeNames);
		Objects.requireNonNull(facts);
	}

	public Literal(Optional<String> name, Optional<ImmutableSet<String>> attributeNames) {
		this(name.orElseThrow(), attributeNames.orElseThrow(), new ArrayList<>());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Literal other = (Literal) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public Literal clone() {
		return new Literal(Optional.of(name), Optional.of(attributeNames));
	}
}
