package ops5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import ops5.ModelRete;
import ops5.parser.Parser;
import ops5.workingmemory.node.Node;


public class ModelReteTest {

	private ArrayList<ModelRete> models = new ArrayList<>();

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		ArrayList<String> files = new ArrayList<>();
		files.add("src/test/ops5/Ops5Programs/demo.ops");
		for (String file : files) {
			Parser parser = new Parser();
			ModelRete model = new ModelRete();
			parser.parseOps5File(file);
			model.addKnowledge(parser);
			models.add(model);
		}
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test_ModelRete_Node() throws Exception {
		for (ModelRete model : models) {
			for (Node node : model.nodes) {
				testNodeLinking(node);
			}
			testNodeCompleteness(model.nodes.iterator().next(), model.nodes);
		}
	}

	void testNodeLinking(Node node) throws Exception {
		for (Node nextNode : node.getNextNodes()) {
			if (!nextNode.getPreviousNodes().contains(node)) {
				throw new IllegalStateException(node.toString() + nextNode.toString());
			}
		}
		for (Node prevNode : node.getPreviousNodes()) {
			if (!prevNode.getNextNodes().contains(node)) {
				throw new IllegalStateException(node.toString() + prevNode.toString());
			}
		}
	}

	void testNodeCompleteness(Node node, Set<Node> nodes) {
		// node traversed must equal nodes
		Set<Node> traversed = new HashSet<>();
		Set<Node> watchlist = new HashSet<>();
		watchlist.add(node);
		while (!watchlist.isEmpty()) {
			Set<Node> adjacent = new HashSet<>();
			Node currNode = watchlist.iterator().next();
			adjacent.addAll(currNode.getAdjacentNodes());
			adjacent.removeAll(traversed); // meet each node only once
			watchlist.addAll(adjacent);
			watchlist.remove(currNode);
			traversed.add(currNode);
		}
		SetView<Node> diff = Sets.difference(traversed, nodes);
		assert (traversed.equals(nodes))
				: "nodes either contains too many or not enough nodes. It should contain all traversed.\nDifference: "
						+ diff;
	}
}
