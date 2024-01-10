package ops5.workingmemory.node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import ops5.ModelRete;
import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Literal;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.ValueRestriction;
import ops5.workingmemory.data.condition.ConditionPredicate;

/**
 * Entrance nodes where facts initially enter the rete network
 *
 */
public final class NodeRoot extends Node {
	// literal specifies the kind of facts that enter this Node
	// literal also has a list of all facts entered
	final public Literal literal;

	public NodeRoot(Optional<ModelRete> modelRete, Optional<Literal> literal) {
		super(modelRete);
		this.literal = literal.orElseThrow();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NodeRoot)) {
			return false;
		}
		NodeRoot other = (NodeRoot) obj;
		return Objects.equals(literal, other.literal);
	}

	@Override
	public int hashCode() {
		return Objects.hash(literal);
	}

	@Override
	public void appendNode(Node other) {
		nextNodes.add(other);
	}

	@Override
	public void propagateAddedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		this.processedEntities.add(reteEntity);
		this.newAddEntities.add(reteEntity);
		this.modelRete.get().addNodeForPropagation(this);
	}

	@Override
	public void propagateAddedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			this.propagateAddedEntity(reteEntity, from, time);
		}
	}

	@Override
	public void propageteRemovedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		if (!this.processedEntities.contains(reteEntity)) {
			throw new IllegalStateException("Tried removing nonexistent ReteEntity, implementation error.");
		}
		this.processedEntities.remove(reteEntity);
		this.newRemoveEntities.add(reteEntity);
		this.modelRete.get().addNodeForPropagation(this);
	}

	@Override
	public void propagateRemovedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			this.propageteRemovedEntity(reteEntity, from, time);
		}
	}

	public void addFact(Fact fact, BigInteger time) {
		ReteEntity re = new ReteEntity(this, fact, time);
		this.addNewEntitiy(re);
	}

	public void removeFact(Fact fact) {
		ReteEntity re = new ReteEntity(this, fact, BigInteger.ZERO);
		this.removeNewEntitiy(re);
	}

	public void addNewEntities(ArrayList<ReteEntity> entities) {
		this.processedEntities.addAll(entities);
		this.newAddEntities.addAll(entities);
		this.modelRete.get().addNodeForPropagation(this);
	}

	public void addNewEntitiy(ReteEntity entity) {
		this.processedEntities.add(entity);
		this.newAddEntities.add(entity);
		this.modelRete.get().addNodeForPropagation(this);
	}

	public void removeNewEntities(ArrayList<ReteEntity> entities) {
		if (this.processedEntities.containsAll(entities)) {
			throw new IllegalStateException();
		}
		this.processedEntities.removeAll(entities);
		this.newRemoveEntities.addAll(entities);
		this.modelRete.get().addNodeForPropagation(this);
	}

	public void removeNewEntitiy(ReteEntity entity) {
		if (!this.processedEntities.contains(entity)) {
			throw new IllegalStateException();
		}
		this.processedEntities.remove(entity);
		this.newRemoveEntities.add(entity);
		this.modelRete.get().addNodeForPropagation(this);
	}

	@Override
	public <T> ValueRestriction<T> getValueRestriction(String atomVarName, ReteEntity reteEntity) {
		throw new RuntimeException("Can never getAtomVariableValue() from a root node."
				+ " It should always find the atom Variable in the Alpha nodes."
				+ "This means that the Entries in atomVariableNodes are incorrect.");
	}

	@Override
	public LinkedList<Node> getPreviousNodes() {
		return new LinkedList<>();
	}

	@Override
	public void unlink() {
		this.nextNodes.clear();
	}

	@Override
	public ConditionPredicate getRestriction(String atomVar) {
		throw new IllegalStateException();
	}

	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("Literal " + this.literal.name() + " ");
		strBuilder.append(this.literal.attributeNames() + "\n");
		Integer i = 0;
		for(ReteEntity processedEntity : super.processedEntities) {
			strBuilder.append(i + ". " + processedEntity.toString() + "\n");
			i++;
		}
		return strBuilder.toString();
	}
}
