package ops5;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ops5.parser.Parser;
import ops5.workingmemory.data.ConflictSet;
import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Literal;
import ops5.workingmemory.data.ProductionRule;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.ReteEntityWrapper;
import ops5.workingmemory.data.condition.Condition;
import ops5.workingmemory.data.condition.ConditionFilter;
import ops5.workingmemory.node.Node;
import ops5.workingmemory.node.NodeAlpha;
import ops5.workingmemory.node.NodeBeta;
import ops5.workingmemory.node.NodeBetaExistent;
import ops5.workingmemory.node.NodeBetaNonexistent;
import ops5.workingmemory.node.NodeRoot;
import ops5.workingmemory.node.NodeTermination;

import utils.UniqueQueue;

/**
 * This class implements the Rete Network for matching facts with production
 * rules.
 *
 */
public class ModelRete {
	// Entry point for facts into rete network
	final private Map<String, NodeRoot> rootNodes = new LinkedHashMap<>();
	// rules fire in terminationNodes
	final private Map<String, NodeTermination> terminationNodes = new LinkedHashMap<>();
	// nodes that have new reteEntities that still need to be filtered and processed
	final private UniqueQueue<Node> nodesWithChangedEntities = new UniqueQueue<Node>();
	final public HashSet<Node> nodes = new HashSet<>();

	private BigInteger time = Objects.requireNonNull(BigInteger.ONE);
	// ReteEntity's that could fire rules in this recognize-act cycle
	private final ConflictSet conflictSet = new ConflictSet();

	public String showOps5ModelRete() {
		return "";
	}

	public void addNodeForPropagation(Node node) {
		this.nodesWithChangedEntities.add(node);
	}

	/**
	 * Take over facts, rules and strategy from the parser and integrate them into
	 * the rete network
	 * 
	 * @param parser model containing facts, literals and production rules
	 * @throws Exception
	 */
	public void addKnowledge(Parser parser) throws Exception {
		addRootNodes(parser);
		addRules(parser);
		addStrategy(parser);
	}

	private void addRules(Parser parser) throws Exception {
		for (Entry<String, ProductionRule> productionRuleEntity : parser.productionRules.entrySet()) {
			// for each production rule:
			final String ruleName = productionRuleEntity.getKey();
			final ProductionRule prodRule = productionRuleEntity.getValue();
			if (terminationNodes.containsKey(ruleName)) {
				throw new IllegalArgumentException(ruleName + " already declared");
			}
			NodeBeta betaNode = null;
			for (Condition condition : prodRule.conditions()) {
				// for each condition of the production rule:
				String condName = condition.literal().name();
				NodeRoot root = rootNodes.get(condName);

				Node previous = root;
				// create sequence of alpha nodes for the condition
				boolean atLeastOneConditionPart = false; // sanity check
				for (ConditionFilter condPart : condition.conditionFilters()) {
					// for each condPart of the condition
					atLeastOneConditionPart = true;
					// create or reuse a NodeAlpha
					previous = addNodeAlpha(previous, condPart, condition.exists());
					nodes.add(previous);
				}
				if (atLeastOneConditionPart == false) {
					previous = addNodeAlpha(previous, null, condition.exists());
					nodes.add(previous);
				}
				assert (previous instanceof NodeAlpha);
				// create or reuse NodeBeta
				betaNode = addNodeBeta(betaNode, (NodeAlpha) previous); // connect alpha to beta node
				nodes.add(betaNode);
			}
			if (betaNode == null) {
				throw new RuntimeException(
						"Need at least one condition on a production rule. Implementation (Java) Error.");
			}
			final NodeTermination termNode = new NodeTermination(Optional.of(prodRule), Optional.of(this),
					Optional.of(betaNode));
			terminationNodes.put(ruleName, termNode);
			nodes.add(termNode);
		}
	}

	public void addFact(ReteEntity newReteEntity, NodeRoot nodeRoot) {
		nodeRoot.addNewEntitiy(newReteEntity);
	}

	public void removeFact(ReteEntity newReteEntity, NodeRoot nodeRoot) {
		nodeRoot.removeNewEntitiy(newReteEntity);
	}

	/**
	 * Add all missing Literals from parser.literals to rootNodes. Add all new
	 * Facts, but do not propagate them yet. Create a new rete Entity for each such
	 * fact.
	 * 
	 * @param parser
	 */
	private void addRootNodes(Parser parser) {
		for (Entry<String, Literal> literalEntry : parser.literals.entrySet()) {
			final String literalName = literalEntry.getValue().name();
			if (!(rootNodes.containsKey(literalName))) {
				final Literal literal = literalEntry.getValue().clone();
				final NodeRoot root = new NodeRoot(Optional.of(this), Optional.of(literal));
				rootNodes.put(literalName, root);
				nodes.add(root);
			}
			final ArrayList<ReteEntity> newEntitys = new ArrayList<>();
			for (Fact newFact : literalEntry.getValue().facts()) {
				ReteEntity entity = new ReteEntity(rootNodes.get(literalName), newFact, time);
				newEntitys.add(entity);
			}
			rootNodes.get(literalName).addNewEntities(newEntitys);
			addNodeForPropagation(rootNodes.get(literalName)); // mark node for propagation
		}
	}

	/**
	 * Append one alpha node either to the root or to another alpha node. Return a
	 * reference the new (or duplicate) Node.
	 * 
	 * @param previous
	 * @param condPart Condition Part associated with the alpha (filter) node
	 * @return
	 * @throws Exception
	 */
	private NodeAlpha addNodeAlpha(Node previous, ConditionFilter condPart, Boolean exists) throws Exception {

		NodeAlpha newAlphaNode = new NodeAlpha(Optional.ofNullable(condPart), Optional.of(exists), Optional.of(this),
				Optional.of(previous));
		boolean duplicateFound = false;
		for (Node node : previous.getNextNodes()) {
			if (node.equals(newAlphaNode)) {
				newAlphaNode = (NodeAlpha) node; // reuse old alpha node
				duplicateFound = true;
				break; // found equal duplicate AlphaNode
			}
		}
		if (duplicateFound == false) { // link new alpha node into rete network
			previous.link(newAlphaNode);
		}
		return newAlphaNode;
	}

	/**
	 * link, and if necessary create, beta node linked to 1 alpha node and 0 or 1
	 * already existing beta nodes. If an equal beta node already exists, reuse it
	 * instead for efficiency.
	 * 
	 * @param currentBetaNode beta node currently being worked on, with 0, 1 or 2
	 *                        (full) connections
	 * @param newNodeAlpha    is root or alpha node
	 * @return
	 * @throws Exception
	 */
	private NodeBeta addNodeBeta(NodeBeta currentBetaNode, NodeAlpha newNodeAlpha) throws Exception {
		assert (newNodeAlpha != null);
		final boolean existing = newNodeAlpha.exists;
		final boolean created;
		if (currentBetaNode == null) {
			// There is no previous beta node
			if (existing) {
				currentBetaNode = new NodeBetaExistent(Optional.of(this), Optional.empty(), Optional.of(newNodeAlpha));
			} else {
				currentBetaNode = new NodeBetaNonexistent(Optional.of(this), Optional.empty(),
						Optional.of(newNodeAlpha));
			}
			created = true;
			// currentBetaNode.previousRightAlphaNode.nextnodes still doesn't contain
			// currentBetaNode
		} else {
			created = false;
		}
		NodeBeta betaNode;
		// search already existing beta nodes below newNodeAlpha
		Set<NodeBeta> oldBetaNodes = new HashSet<>();
		for (Node alternativeNode : newNodeAlpha.getNextNodes()) {
			if (alternativeNode instanceof NodeBeta) {
				oldBetaNodes.add((NodeBeta) alternativeNode);
			}
		}
		if (!created) {
			// create new beta node and chain it
			if (existing) {
				betaNode = new NodeBetaExistent(Optional.of(this), Optional.of(currentBetaNode),
						Optional.of(newNodeAlpha));
			} else {
				betaNode = new NodeBetaNonexistent(Optional.of(this), Optional.of(currentBetaNode),
						Optional.of(newNodeAlpha));
			}
			currentBetaNode.link(betaNode);
		} else {
			// else beta node has space, so use old beta node
			betaNode = currentBetaNode;
		}
		newNodeAlpha.link(betaNode);

		boolean madeNewBetaNode = true;
		// search for equal old beta node and if equal, reuse it
		for (NodeBeta oldBetaNode : oldBetaNodes) {
			if (oldBetaNode != betaNode && oldBetaNode.equals(betaNode)) {
				betaNode.unlink();
				betaNode = oldBetaNode;
				madeNewBetaNode = false;
				break;
			}
		}
		if (madeNewBetaNode) {
			// betaNode.propagateAllPreviousProcessedToThisNode();
		}
		return betaNode;
	}

	private void addStrategy(Parser parser) {
		this.conflictSet.changeStrategy(parser.strategy);
	}

	/**
	 * Propagate all Entities (facts or combination tuples). This loop does the main
	 * work of the rete algorithm. It creates all possible matches and propagates
	 * trough the rete network.
	 */
	private void propagateEntities() {
		// use nodesWithNewEntities like a queue
		while (!nodesWithChangedEntities.isEmpty()) {
			final Node node = nodesWithChangedEntities.remove();
			node.clearNewEntitiesIntersection();
			final ImmutableMultiset<ReteEntity> newAddEntities = node.popNewAddEntities();
			final ImmutableMultiset<ReteEntity> newRemoveEntities = node.popNewRemoveEntities();
			for (Node nextNode : node.getNextNodes()) {
				nextNode.propagateAddedEntities(newAddEntities, node, time);
				nextNode.propagateRemovedEntities(newRemoveEntities, node, time);
				addNodeForPropagation(nextNode);
			}
		}
		time = time.add(BigInteger.ONE); // time++
	}

	public void addToConflictSet(ReteEntityWrapper reteEntityWrapper) {
		this.conflictSet.addToConflictSet(reteEntityWrapper);
	}

	public void removeFromConflictSet(ReteEntityWrapper reteEntityWrapper) {
		this.conflictSet.removeFromConflictSet(reteEntityWrapper);
	}

	public boolean executeStep() throws Exception {
		this.propagateEntities();
		return this.conflictSet.fireMatch(this.time);
	}

	public NodeRoot getRootNode(String name) {
		return this.rootNodes.get(name);
	}

	public HashSet<Node> getAllNodes() {
		return nodes;
	}

	public BigInteger getTime() {
		return time;
	}

	public String printConflictSet() {
		return this.conflictSet.toString();
	}
	
	public void setWatchLevel(Integer watchLevel) {
		this.conflictSet.setWatchlevel(watchLevel);
	}

	private HashSet<Node> expandNode(Node node) {
		HashSet<Node> visitedNodes = new HashSet<Node>();
		HashSet<Node> newNodes = new HashSet<Node>();
		Node currentNode;
		newNodes.add(node);
		while (!newNodes.isEmpty()) {
			currentNode = newNodes.iterator().next();
			for (Node n : node.getPreviousNodes()) {
				if (!visitedNodes.contains(n)) {
					newNodes.add(n);
				}
			}
			for (Node n : node.getPreviousNodes()) {
				if (!visitedNodes.contains(n)) {
					newNodes.add(n);
				}
			}
			visitedNodes.add(node);
			newNodes.remove(currentNode);
		}
		return visitedNodes;
	}

	public String printWorkingMemory() {
		StringBuilder strBuilder = new StringBuilder();
		for (Entry<String, NodeRoot> e : this.rootNodes.entrySet()) {
			strBuilder.append("==========\n");
			strBuilder.append(e.getValue().toString());
		}
		strBuilder.append("==========");
		return strBuilder.toString();
	}
}
