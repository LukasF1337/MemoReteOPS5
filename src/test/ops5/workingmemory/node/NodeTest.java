package ops5.workingmemory.node;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ops5.Controller;
import ops5.ModelRete;
import ops5.workingmemory.node.Node;

public class NodeTest {

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
	void test_Node_00() throws Exception {

	}

}
