package ops5.workingmemory.node;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableMultiset;

import ops5.ModelRete;
import ops5.workingmemory.data.*;
import ops5.workingmemory.data.condition.ConditionFilter;
import ops5.workingmemory.data.condition.ConditionPredicate;
import ops5.workingmemory.node.NodeBeta.Position;

/**
 * One Alpha Node filters for one Condition. It takes all Facts (which are
 * packed in ReteEnitity's) from the previous node and applies the
 * ConditionFilter filter on it. Each fact either passes or doesn't pass trough
 * the filter. This resembles an alpha Node in the classical rete network.
 *
 */
public final class NodeAlpha extends Node {

	final public WeakReference<Node> previousNode;
	final public ConditionFilter filter; // The ConditionFilter that filters facts for this alpha node
	final public Boolean exists; // test for existance or non existance
	final private Integer hashCode;
	final private boolean atomVarCheck; // this node re-references an atomvar, this means we have to filter based on
										// atomvar value (p (rectangle ^width <size> ^heigth <size>)...)
	final private String atomVarString; // for example: "<theCity>" enclosed by greater than and less than symbols
	final private boolean atomVarNewUnique; // true if we have a new atomvar that appears the first time

	// remember the location of variable values (the previous node that is the
	// originator of the variable)
	final public ImmutableMap<String, Node> atomVariableNodes;
	final public ImmutableMap<String, ConditionPredicate> atomVarToPredicate; // e.g.: {"<speed>" => ">", ...}
	final public ImmutableSet<String> atomVarAssignedWithConcreteValue; // Set of atom variables that have assigned a
																		// concrete value ("=")
	final public String elemVariableString; // if !=null we have an elemvar (element variable)
	final public int nodeAlphaSpecificityScore; // the more previous NadeAlpha's, the higher the score

	public NodeAlpha(Optional<ConditionFilter> condFilter, Optional<Boolean> exists, Optional<ModelRete> modelRete,
			Optional<Node> previousNode) throws Exception {
		super(modelRete);
		this.previousNode = new WeakReference<>(previousNode.orElseThrow());
		if (this.previousNode.get() instanceof NodeRoot) {
			nodeAlphaSpecificityScore = 1;
		} else if (this.previousNode.get() instanceof NodeAlpha alpha) {
			nodeAlphaSpecificityScore = (alpha.nodeAlphaSpecificityScore + 1);
		} else {
			throw new IllegalStateException();
		}
		filter = condFilter.orElse(null);
		this.exists = exists.orElseThrow();
		// setVarRefs(previousNode);
		hashCode = Objects.hash(exists, filter, previousNode.get());

		// atomVariableNodes:
		ImmutableMap.Builder<String, Node> atomVariableNodesBuilder = new Builder<String, Node>();
		Map<String, ConditionPredicate> atomVarToPredicateBuilder = new HashMap<String, ConditionPredicate>();
		if (previousNode.get() instanceof NodeAlpha) {
			// copy previous atomVariableNodes
			final NodeAlpha prevAlphaNode = (NodeAlpha) previousNode.get();
			for (Map.Entry<String, Node> entry : prevAlphaNode.atomVariableNodes.entrySet()) {
				String atomVar = entry.getKey();
				atomVariableNodesBuilder.put(atomVar, prevAlphaNode);
			}
			atomVarToPredicateBuilder.putAll(prevAlphaNode.atomVarToPredicate);
		} else if (previousNode.get() instanceof NodeRoot) {
			// NOOP
		} else {
			throw new IllegalStateException();
		}

		if (filter != null && filter.value() != null && filter.value().attributeType().equals(ValueType.ATOMVARIABLE)) {
			// this NodeAlpha filters for an atom variable. Depending on the previous nodes
			// this has a different effect
			ConditionPredicate predNew = filter.predicate();
			String atomVar = (String) filter.value().attributeValue();
			if (atomVarToPredicateBuilder.get(atomVar) != null) {
				assert (atomVarToPredicateBuilder.get(atomVar).equals(ConditionPredicate.EQUAL)
						|| predNew.equals(ConditionPredicate.EQUAL));
				atomVarToPredicateBuilder.put(atomVar, ConditionPredicate.EQUAL);
			} else {
				atomVarToPredicateBuilder.put(atomVar, predNew);
			}
			if (previousNode.get() instanceof NodeAlpha && ((NodeAlpha) previousNode.get()).atomVariableNodes
					.containsKey(filter.value().attributeValue())) {
				// one of the previous nodes already contains the same atom variable
				atomVarCheck = true;
				atomVarNewUnique = false;
			} else {
				// in the list of previous nodes up to this node, the atom variable appeared for
				// the first time on this node
				atomVarCheck = false;
				atomVarNewUnique = true;
			}
			atomVarString = (String) filter.value().attributeValue();
			atomVariableNodesBuilder.put(atomVarString, this);
		} else {
			// no new atom variable on this node. No work needed.
			atomVarCheck = false;
			atomVarNewUnique = false;
			atomVarString = null;
		}
		atomVariableNodes = atomVariableNodesBuilder.buildKeepingLast(); // keep most recent reference for quicker
																			// lookups
		atomVarToPredicate = ImmutableMap.copyOf(atomVarToPredicateBuilder);

		// atomVarAssignedWithConcreteValue:
		ImmutableSet.Builder<String> atomVarAssignedWithConcreteValueBuilder = ImmutableSet.builder();
		if (previousNode.get() instanceof NodeAlpha) {
			// remember previous assigned ones
			NodeAlpha prevAlphaNode = (NodeAlpha) previousNode.get();
			atomVarAssignedWithConcreteValueBuilder.addAll(prevAlphaNode.atomVarAssignedWithConcreteValue);
		}
		if (atomVarNewUnique) {

			assert (filter == null || filter.value().attributeType().equals(ValueType.ATOMVARIABLE));
			if (filter != null && filter.predicate().equals(ConditionPredicate.EQUAL)) {
				atomVarAssignedWithConcreteValueBuilder.add(atomVarString);
			}
		}
		atomVarAssignedWithConcreteValue = atomVarAssignedWithConcreteValueBuilder.build();

		// elemVariables
		String s = null;
		if (previousNode.get() instanceof NodeAlpha) {
			// copy previous
			s = ((NodeAlpha) previousNode.get()).elemVariableString;
		} else if (filter != null && filter.elementVariable() != null) {
			// get elemvar string from condition
			s = filter.elementVariable();
		}
		elemVariableString = s;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NodeAlpha other)) {
			return false;
		}
		if (this == other) {
			return true;
		}
		return Objects.equals(exists, other.exists) && Objects.equals(filter, other.filter)
				&& Objects.equals(previousNode.get(), other.previousNode.get());
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public void appendNode(Node other) {
		nextNodes.add(other);
	}

	@Override
	public void propagateAddedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		if (doesPropagate(reteEntity)) {
			// atomvar and elementvar values can be deduced from chosen facts, so they do
			// not need an explicit variable assignment
			ReteEntity newReteEntity = new ReteEntity(this, (Fact) reteEntity.entity(), time);
			processedEntities.add(newReteEntity);
			newAddEntities.add(newReteEntity);
		}
	}

	@Override
	public void propagateAddedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			propagateAddedEntity(reteEntity, from, time);
		}
	}

	@Override
	public void propageteRemovedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		if (doesPropagate(reteEntity)) {
			final ReteEntity newRemoveReteEntity = new ReteEntity(this, (Fact) reteEntity.entity(), time);
			if (processedEntities.remove(newRemoveReteEntity)) {
				newRemoveEntities.add(newRemoveReteEntity);
			} else {
				throw new IllegalStateException("Remove invalid: " + from.toString() + " " + reteEntity.toString());
			}
		}
	}

	@Override
	public void propagateRemovedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			propageteRemovedEntity(reteEntity, from, time);
		}
	}
	
	private boolean doesPropagate(ReteEntity reteEntity) {
		boolean propagate = false;
		if (atomVarCheck) {
			// check for things where the same ATOMVARIABLE repeats (p r1 (rectangle ^width
			// <size> ^heigth <size> ^weight < <size> ^velocity > <size>) --> ...)
			atomVariableNodes.get(atomVarString);
			Value<?> value1 = reteEntity.getValue(atomVarString);
			ValueRestriction<?> restr2 = getValueRestriction(atomVarString, reteEntity);

			if (previousNode.get() instanceof NodeAlpha) {
				propagate = ConditionFilter.match(restr2.value(), restr2.predicate(), value1);
			} else {
				throw new RuntimeException("First atom variable would need to be assigned"
						+ " a concrete value in order to be compared in alpha node: " + value1);
			}
		} else if (atomVarNewUnique || filter == null) {
			// always pass on cases like (p r1 (person ^size <size>) --> ...)
			// or if there is no filter
			propagate = true;
		} else {
			// filter in cases like (p r1 (person age >= 18) --> ...)
			Fact fact = (Fact) reteEntity.entity();
			propagate = filter.applyFilter(fact);
		}
		return propagate;
	}

	@Override
	public <T> ValueRestriction<T> getValueRestriction(String atomVarName, ReteEntity reteEntity) {
		ValueRestriction<T> result;
		if (filter != null && filter.value() != null && filter.value().attributeType() == ValueType.ATOMVARIABLE
				&& filter.value().attributeValue().equals(atomVarName)) {
			// this alpha node has the value of the atom variable
			Fact fact = (Fact) reteEntity.entity();

			Value<T> value = fact.getValue(filter.attribute());
			ConditionPredicate predicate = filter.predicate();
			result = new ValueRestriction<T>(value, Optional.of(predicate));
		} else {
			// some alpha node above has the value of the atom variable
			result = previousNode.get().getValueRestriction(atomVarName, reteEntity);
		}
		return result;
	}

	@Override
	public LinkedList<Node> getPreviousNodes() {
		LinkedList<Node> res = new LinkedList<>();
		if (previousNode.get() != null) {
			res.add(previousNode.get());
		}
		return res;
	}

	@Override
	public void unlink() {
		nextNodes.clear();
		if (previousNode != null) {
			previousNode.get().deleteNextNode(this);
		}
	}

	@Override
	public ConditionPredicate getRestriction(String atomVar) {
		return atomVarToPredicate.get(atomVar);
	}
}
