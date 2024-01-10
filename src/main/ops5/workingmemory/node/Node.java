package ops5.workingmemory.node;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import ops5.ModelRete;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.ValueRestriction;
import ops5.workingmemory.data.condition.ConditionPredicate;

/**
 * Node in the rete network can be root, alpha, beta or termination node. The
 * network of these nodes comprise the Rete Network.
 *
 */
public abstract class Node {
	// Rete Model that owns this node
	public final WeakReference<ModelRete> modelRete;

	final Set<Node> nextNodes; // previous nodes can be found in subclasses to Node

	// rete entities that are to be added through propagation in the network for all
	// successive nodes, also called '+' token.
	final protected LinkedHashMultiset<ReteEntity> newAddEntities = LinkedHashMultiset.create();
	// rete entities that are to be removed through propagation in the network for
	// all successive nodes, also called '-' token.
	final protected LinkedHashMultiset<ReteEntity> newRemoveEntities = LinkedHashMultiset.create();
	// rete entities that are already filtered and propagated through the network.
	// Every content change in processedEntities entities must be reflected in
	// newAddEntities or newRemoveEntities
	final protected LinkedHashMultiset<ReteEntity> processedEntities = LinkedHashMultiset.create();

	Node(Optional<ModelRete> modelRete) {
		this.modelRete = new WeakReference<ModelRete>(modelRete.orElseThrow());
		if (this instanceof NodeRoot || this instanceof NodeAlpha || this instanceof NodeBeta) {
			this.nextNodes = new LinkedHashSet<>();
		} else if (this instanceof NodeTermination) {
			this.nextNodes = (new ImmutableSet.Builder<Node>()).build();
		} else {
			throw new IllegalStateException("Unknown node type");
		}
	}

	@Override
	public abstract boolean equals(Object other);

	@Override
	public abstract int hashCode();

	/**
	 * get the Value (e.g. '15') and Restriction (e.g. '>') of the atomvariable for
	 * the given rete entity by going upwards the tree recursively.
	 * 
	 * @param <T>
	 * 
	 * @param atomVarName
	 * @param reteEntity
	 * @return
	 */
	public abstract <T> ValueRestriction<T> getValueRestriction(String atomVarName, ReteEntity reteEntity);

	/**
	 * For a given atomVar name get the ConditionPredicate "=,<,<=,>,>=,...". The
	 * further down this Node is, the more likely it is that the atomVariable has
	 * concrete Value assigned, meaning that the returned ConditionPredicate is "=".
	 * 
	 */
	public abstract ConditionPredicate getRestriction(String atomVar);

	/**
	 * get the Value (e.g. '12') of the atomvariable for the given rete entity by
	 * going upwards the tree recursively.
	 * 
	 */
	public <T> Value<T> getValue(String atomVarName, ReteEntity reteEntity) {
		return reteEntity.getValue(atomVarName);
	}

	public abstract void appendNode(Node other); // append node after this node

	public Set<Node> getNextNodes() {
		return this.nextNodes;
	}

	public abstract LinkedList<Node> getPreviousNodes();

	public LinkedList<Node> getAdjacentNodes() {
		LinkedList<Node> result = new LinkedList<>();
		result.addAll(this.getPreviousNodes());
		result.addAll(this.getNextNodes());
		return result;
	}

	/**
	 * Propagate added entity to this node. The "from" Node is the source of the
	 * added Entity.
	 */
	public abstract void propagateAddedEntity(ReteEntity reteEntity, Node from, BigInteger time);

	/**
	 * Propagate added entities to this node. The "from" Node is the source of the
	 * added Entity.
	 */
	public abstract void propagateAddedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time);

	/**
	 * Propagate removed entity to this node. The "from" Node is the source of the
	 * removed Entity.
	 */
	public abstract void propageteRemovedEntity(ReteEntity reteEntity, Node from, BigInteger time);

	/**
	 * Propagate removed entities to this node. The "from" Node is the source of the
	 * added Entity.
	 */
	public abstract void propagateRemovedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from,
			BigInteger time);

	public void clearNewEntitiesIntersection() {
		ImmutableMultiset<ReteEntity> intersect = ImmutableMultiset
				.copyOf(Multisets.intersection(newAddEntities, newRemoveEntities)); // necessary copy, because
																					// .removeAll() below also modifies
																					// the intersect
		this.newAddEntities.removeAll(intersect);
		this.newRemoveEntities.removeAll(intersect);
	}

	public ImmutableMultiset<ReteEntity> popNewAddEntities() {
		final ImmutableMultiset<ReteEntity> res = ImmutableMultiset.copyOf(this.newAddEntities);
		this.newAddEntities.clear();
		return res;
	}

	public ImmutableMultiset<ReteEntity> popNewRemoveEntities() {
		final ImmutableMultiset<ReteEntity> res = ImmutableMultiset.copyOf(this.newRemoveEntities);
		this.newRemoveEntities.clear();
		return res;
	}

	/**
	 * link from this node to other. This node must be closer to root node than
	 * other node.
	 */
	public void link(Node other) {
		assert (other != null);
		this.appendNode(other);
	}

	/**
	 * doubly unlink this node from all previous and all next nodes
	 */
	public abstract void unlink();

	public final void deleteNextNode(Node n) {
		if (this.nextNodes.remove(n)) {
		} else {
			throw new RuntimeException("Can not delete nonexistent next Node " + n);
		}
	}
}
