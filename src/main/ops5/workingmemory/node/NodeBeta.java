package ops5.workingmemory.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets.SetView;

import ops5.ModelRete;
import ops5.workingmemory.data.BetaTuple;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.ValueRestriction;
import ops5.workingmemory.data.condition.ConditionFilter;
import ops5.workingmemory.data.condition.ConditionPredicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Node for list of combinations
 *
 */
public abstract class NodeBeta extends Node {

	public enum Position {
		LEFTBETA, RIGHTALPHA
		// left side beta node, right side alpha node
	}

	final NodeBeta previousLeftBetaNode;
	final NodeAlpha previousRightAlphaNode;
	final private Integer hashCode;
	final Integer numberOfPrevNodes;
	final int nodeBetaSpecificityScore; // the more previous NodeBeta's, the higher the score
	final int nodeAlphaSpecificityScore; // the more previous NadeAlpha's, the higher the score

	// join potentially multiple atomvars for filtering
	final ImmutableSet<String> atomVarsIntersection;
	// remember which atom variables have which predicates ("=,<,>,..."). If atomVar
	// intersects, one side must have predicate "=".
	final ImmutableMap<String, ConditionPredicate> atomVarToPredicate;
	// remember the location of variable values (the previous node that is the
	// originator of the variable)
	final public ImmutableMap<String, Node> atomVariableNodes;
	final public ImmutableMap<String, Node> elemVariableNodes;

	public NodeBeta(Optional<ModelRete> modelRete, Optional<NodeBeta> previousLeftBetaNode,
			Optional<NodeAlpha> previousRightAlphaNode) {
		super(modelRete);

		this.previousLeftBetaNode = previousLeftBetaNode.orElse(null);
		this.previousRightAlphaNode = previousRightAlphaNode.orElseThrow();
		this.hashCode = Objects.hash(this.previousLeftBetaNode, this.previousRightAlphaNode);
		if (this.previousLeftBetaNode == null) {
			this.numberOfPrevNodes = 1;
		} else {
			this.numberOfPrevNodes = 2;
		}

		int sumBeta = 0;
		int sumAlpha = 0;
		for (Node node : this.getPreviousNodes()) {
			if (node instanceof NodeBeta) {
				sumBeta += ((NodeBeta) node).nodeBetaSpecificityScore;
				sumAlpha += ((NodeBeta) node).nodeAlphaSpecificityScore;
			} else if (node instanceof NodeAlpha || node instanceof NodeRoot) {
				sumBeta += 1;
				sumAlpha += ((NodeAlpha) node).nodeAlphaSpecificityScore;
			}
		}
		nodeBetaSpecificityScore = sumBeta;
		nodeAlphaSpecificityScore = sumAlpha;

		ImmutableSet.Builder<String> atomVarsIntersectionBuilder = ImmutableSet.builder();
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<String, Node> atomVariableNodesBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<String, Node> elemVariableNodesBuilder = ImmutableMap.builder();

		setVarRefs(atomVarsIntersectionBuilder, atomVarToPredicateBuilder, atomVariableNodesBuilder,
				elemVariableNodesBuilder);

		this.atomVarsIntersection = atomVarsIntersectionBuilder.build();
		this.atomVarToPredicate = atomVarToPredicateBuilder.build();
		this.atomVariableNodes = atomVariableNodesBuilder.build();
		this.elemVariableNodes = elemVariableNodesBuilder.build();
	}

	protected ImmutableMap<String, ConditionPredicate> constructAtomVarToPredicate() {
		ImmutableMap<String, ConditionPredicate> atomVarToPredicateLeft = this.previousLeftBetaNode.atomVarToPredicate;
		ImmutableMap<String, ConditionPredicate> atomVarToPredicateRight = this.previousRightAlphaNode.atomVarToPredicate;
		SetView<String> atomVarIntersect = Sets.intersection(atomVarToPredicateLeft.keySet(),
				atomVarToPredicateRight.keySet());
		assert (this.atomVarsIntersection.equals(ImmutableSet.copyOf(atomVarIntersect)));
		ImmutableMap.Builder<String, ConditionPredicate> resBuilder = ImmutableMap.builder();
		for (String atomVar : atomVarIntersect) {
			final ConditionPredicate predicate;
			final ConditionPredicate predicateLeft = atomVarToPredicateLeft.get(atomVar);
			final ConditionPredicate predicateRight = atomVarToPredicateRight.get(atomVar);
			if (!predicateLeft.equals(ConditionPredicate.EQUAL)) {
				throw new IllegalStateException();
			}
			switch (predicateRight) {
			case EQUAL:
			case SAMETYPE:
			case BIGGER:
			case BIGGERQUAL:
			case SMALLER:
			case SMALLEREQUAL:
			case UNEQUAL:
				predicate = predicateRight;
				break;
			default:
				throw new IllegalStateException();
			}
			resBuilder.put(atomVar, predicate);
		}
		return resBuilder.build();
	}

	/**
	 * Only check previous nodes and existence for equality.
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj.getClass().equals(this.getClass())) {
			return false;
		}
		if (this.hashCode() != obj.hashCode()) {
			return false;
		}
		NodeBeta other = (NodeBeta) obj;
		return Objects.equals(this.previousLeftBetaNode, other.previousLeftBetaNode)
				&& Objects.equals(this.previousRightAlphaNode, other.previousRightAlphaNode);
	}

	@Override
	public final int hashCode() {
		return this.hashCode;
	}

	public NodeBeta getPreviousLeftBetaNode() {
		return previousLeftBetaNode;
	}

	public NodeAlpha getPreviousRightAlphaNode() {
		return previousRightAlphaNode;
	}

	@Override
	public void appendNode(Node other) {
		nextNodes.add(other);
	}

	/**
	 * Set atom and element variable References for lookup upwards the tree.
	 */
	private void setVarRefs(ImmutableSet.Builder<String> atomVarsIntersectionBuilder,
			ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder,
			ImmutableMap.Builder<String, Node> atomVariableNodesBuilder,
			ImmutableMap.Builder<String, Node> elemVariableNodesBuilder) {
		setAtomVarRefs(atomVarsIntersectionBuilder, atomVarToPredicateBuilder, atomVariableNodesBuilder);
		setElemVarRefs(elemVariableNodesBuilder);
	}

	/**
	 * Populate atomVariableNodesBuilder, atomVarsIntersectionBuilder and
	 * atomVarToPredicateBuilder.
	 */
	private void setAtomVarRefs(ImmutableSet.Builder<String> atomVarsIntersectionBuilder,
			ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder,
			ImmutableMap.Builder<String, Node> atomVariableNodesBuilder) {

		Set<String> atomVarsIntersection = new HashSet<>();
		Map<String, ConditionPredicate> atomVarToPredicate = new HashMap<>();
		Map<String, Node> atomVariableNodes = new HashMap<>();

		// remember NodeAlpha references
		final NodeAlpha nodeAlpha = this.previousRightAlphaNode;
		for (String atomVar : nodeAlpha.atomVariableNodes.keySet()) {
			atomVarToPredicate.put(atomVar, nodeAlpha.getRestriction(atomVar));
			atomVariableNodes.put(atomVar, nodeAlpha);
		}
		// remember NodeBeta references
		if (this.previousLeftBetaNode != null) {
			final NodeBeta nodeBeta = this.previousLeftBetaNode;
			for (String atomVar : nodeBeta.atomVariableNodes.keySet()) {
				if (nodeAlpha.atomVariableNodes.containsKey(atomVar)) {
					// intersect
					if (!nodeBeta.atomVarToPredicate.get(atomVar).equals(ConditionPredicate.EQUAL)) {
						throw new RuntimeException(
								"Atom Variables need to be assigned first, before they can be compared.");
					}
					atomVarsIntersection.add(atomVar);
					atomVarToPredicate.put(atomVar, ConditionPredicate.EQUAL);
				} else {
					// remainder after intersect, unique atomvars to nodeBeta
					atomVarToPredicate.put(atomVar, nodeBeta.getRestriction(atomVar));
				}
				atomVariableNodes.put(atomVar, nodeBeta);
			}
		}

		atomVarsIntersectionBuilder.addAll(atomVarsIntersection);
		atomVarToPredicateBuilder.putAll(atomVarToPredicate);
		atomVariableNodesBuilder.putAll(atomVariableNodes);
	}

	/**
	 * Populate elemVariableNodes
	 */
	private void setElemVarRefs(ImmutableMap.Builder<String, Node> elemVariableNodesBuilder) {
		// remember NodeAlpha references
		NodeAlpha nodeAlpha = this.previousRightAlphaNode;
		if (nodeAlpha.elemVariableString != null) {
			elemVariableNodesBuilder.put(nodeAlpha.elemVariableString, nodeAlpha);
		}
		// remember NodeBeta references
		if (this.previousLeftBetaNode != null) {
			NodeBeta nodeBeta = this.previousLeftBetaNode;
			for (String atomVar : nodeBeta.elemVariableNodes.keySet()) {
				elemVariableNodesBuilder.put(atomVar, nodeBeta);
			}
		}
	}

	@Override
	public <T> ValueRestriction<T> getValueRestriction(String atomVarName, ReteEntity reteEntity) {
		assert (atomVarName != null && reteEntity != null);
		Node originNode = this.atomVariableNodes.get(atomVarName);
		if (originNode == null) {
			throw new RuntimeException("atomVarName: " + atomVarName + " not found");
		}
		// Extract previous Rete Entity from BetaTuple
		BetaTuple betaTuple = (BetaTuple) reteEntity.entity();
		if (originNode instanceof NodeBeta) {
			reteEntity = betaTuple.betaTuple().get(Position.LEFTBETA);
		} else if (originNode instanceof NodeAlpha) {
			reteEntity = betaTuple.betaTuple().get(Position.RIGHTALPHA);
		} else {
			throw new RuntimeException(
					"atomVarName: " + atomVarName + " not found. Either AtomVariable locations have not "
							+ "been propagated properly or they are missing in the OPS 5 code.");
		}
		assert (reteEntity != null);
		// Recursive call towards the node containing the atom variable
		return originNode.getValueRestriction(atomVarName, reteEntity);
	}

	@Override
	public ConditionPredicate getRestriction(String atomVar) {
		return this.atomVarToPredicate.get(atomVar);
	}

	/**
	 * Match a concrete assigned value (transported via a value restriction)
	 * together with a value restriction acting as filter. Do not modify rete
	 * network state here, only see if they match.
	 * 
	 * @param entryLeftValueRestriction
	 * @param entryRightValueRestriction
	 * @param assignedSide               is the side (left or right) that has a
	 *                                   value assigned ("="). The other side then
	 *                                   must contain the restriction (which might
	 *                                   also be an assigned value)
	 */
	protected boolean match(ValueRestriction<?> entryLeftValueRestriction,
			ValueRestriction<?> entryRightValueRestriction, Position assignedSide) {
		Value<?> val;
		ValueRestriction<?> valRes;
		if (assignedSide == Position.LEFTBETA) {
			val = entryLeftValueRestriction.value();
			valRes = entryRightValueRestriction;
		} else if (assignedSide == Position.RIGHTALPHA) {
			val = entryRightValueRestriction.value();
			valRes = entryLeftValueRestriction;
		} else {
			throw new RuntimeException("Atom Variable needs to be assigned first, before it can be filtered.");
		}

		boolean match = ConditionFilter.match(valRes.value(), valRes.predicate(), val);
		return match;
	}

	@Override
	public LinkedList<Node> getPreviousNodes() {
		LinkedList<Node> res = new LinkedList<>();
		if (this.previousLeftBetaNode != null) {
			res.add(this.previousLeftBetaNode);
		}
		res.add(this.previousRightAlphaNode);
		return res;
	}

	@Override
	public void unlink() {
		// next
		this.nextNodes.clear();
	}
}
