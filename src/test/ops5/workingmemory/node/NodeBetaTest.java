package ops5.workingmemory.node;

import java.util.HashSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import ops5.Controller;
import ops5.workingmemory.data.condition.ConditionPredicate;
import ops5.workingmemory.node.Node;
import ops5.workingmemory.node.NodeBeta;
import ops5.workingmemory.node.NodeBeta.Position;

public class NodeBetaTest {

	private Controller ctrlDemo, ctrlAuto, ctrlConflict, ctrlMajor;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		ctrlDemo = new Controller("src/test/ops5/Ops5Programs/demo.ops");
//		ctrlAuto = new Controller("src/test/Ops5Programs/auto.ops");
//		ctrlConflict = new Controller("src/test/Ops5Programs/conflict.ops");
//		ctrlMajor = new Controller("src/test/Ops5Programs/major.ops");
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test_NodeBeta_00() throws Exception {
		HashSet<Node> nodes = new HashSet<>(ctrlDemo.modelRete.nodes);
		for (Node node : nodes) {
			if (node instanceof NodeBeta) {
				NodeBeta nodeBeta = (NodeBeta) node;
				assert (nodeBeta.atomVariableNodes.keySet().equals(nodeBeta.atomVarToPredicate.keySet()))
						: nodeBeta.atomVariableNodes.toString() + nodeBeta.atomVarToPredicate.toString() + "";
				Boolean hasTwo = false;
				ImmutableMap<String, ConditionPredicate> left = null;
				if (nodeBeta.previousLeftBetaNode != null) {
					left = nodeBeta.previousLeftBetaNode.atomVarToPredicate;
					hasTwo = true;
				}
				ImmutableMap<String, ConditionPredicate> right = nodeBeta.previousRightAlphaNode.atomVarToPredicate;
				if (hasTwo) {
					for (String atomVar : nodeBeta.atomVarsIntersection) {
						assert (left != null && left.get(atomVar).equals(ConditionPredicate.EQUAL));
					}
					// TODO check atomVarToPredicate
				} else {
					assert (right.equals(nodeBeta.atomVarToPredicate));
				}
			}
		}
	}

}
