package ops5.workingmemory.node;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;

import ops5.ModelRete;
import ops5.workingmemory.data.ProductionRule;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.ReteEntityWrapper;
import ops5.workingmemory.data.ValueRestriction;
import ops5.workingmemory.data.condition.ConditionPredicate;

/**
 * node to execute rule
 * 
 * @author admin_t
 *
 */
public final class NodeTermination extends Node {

	// remember the location of variable values (the previous node that is the
	// originator of the variable)
	final public ProductionRule ruleToFire;
	final public NodeBeta prevNode; // Previous NodeBeta node
	final public int nodeBetaSpecificityScore; // the more previous NodeBeta's, the higher the score
	final public int nodeAlphaSpecificityScore; // the more previous NadeAlpha's, the higher the score

	public NodeTermination(Optional<ProductionRule> ruleToFire, Optional<ModelRete> modelRete,
			Optional<NodeBeta> previousNode) {
		super(modelRete);
		this.ruleToFire = ruleToFire.orElseThrow();
		this.prevNode = previousNode.orElseThrow();
		this.prevNode.link(this);
		this.nodeBetaSpecificityScore = prevNode.nodeBetaSpecificityScore;
		this.nodeAlphaSpecificityScore = prevNode.nodeAlphaSpecificityScore;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ruleToFire);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeTermination other = (NodeTermination) obj;
		return Objects.equals(ruleToFire, other.ruleToFire);
	}

	@Override
	public void appendNode(Node other) {
		throw new RuntimeException("Can not append node to NodeTermination.");
	}

	@Override
	public void propagateAddedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		this.processedEntities.add(reteEntity); // reuse reteEntity's
		// propagate into conflict set:
		this.modelRete.get().addToConflictSet(new ReteEntityWrapper(this, reteEntity));
	}

	@Override
	public void propagateAddedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			this.propagateAddedEntity(reteEntity, from, time);
		}
	}

	@Override
	public void propageteRemovedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		// reuse reteEntity's
		if (!this.processedEntities.remove(reteEntity)) {
			throw new IllegalStateException();
		}
		// propagate into conflict set:
		this.modelRete.get().removeFromConflictSet(new ReteEntityWrapper(this, reteEntity));
	}

	@Override
	public void propagateRemovedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			this.propageteRemovedEntity(reteEntity, from, time);
		}
	}

	@Override
	public <T> ValueRestriction<T> getValueRestriction(String atomVarName, ReteEntity reteEntity) {
		return this.prevNode.getValueRestriction(atomVarName, reteEntity);
	}

	@Override
	public LinkedList<Node> getPreviousNodes() {
		LinkedList<Node> res = new LinkedList<>();
		res.add(this.prevNode);
		return res;
	}

	@Override
	public void unlink() {
		this.prevNode.deleteNextNode(this);
	}

	@Override
	public ConditionPredicate getRestriction(String atomVar) {
		return this.prevNode.getRestriction(atomVar);
	}

	@Override
	public String toString() {
		return this.ruleToFire.name();
	}
}
