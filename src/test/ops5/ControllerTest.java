package ops5;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ops5.Controller;

class ControllerTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test_Controller_00() throws Exception {
		// fail("Not yet implemented");
	}

	@Test
	void test_Controller_01() throws Exception {
		//Controller ctrl = new Controller("src/test/ops5/Ops5Programs/demo.ops");
	}

	@Test
	void test_Controller_02() throws Exception {
		//Controller ctrl = new Controller("src/test/ops5/Ops5Programs/auto.ops");
	}

	@Test
	void test_Controller_03() throws Exception {
		//Controller ctrl = new Controller("src/test/ops5/Ops5Programs/conflict.ops");
	}

	@Test
	void test_Controller_04() throws Exception {
		//Controller ctrl = new Controller("src/test/ops5/Ops5Programs/major.ops");
	}

	@Test
	void test_Controller_05() throws Exception {
		Controller ctrl = new Controller("src/test/ops5/Ops5Programs/mluewbs2.ops");
		ctrl.run(true);
	}

	@Test
	void test_Controller_06() throws Exception {
		//Controller ctrl = new Controller("src/test/ops5/Ops5Programs/reactor.ops");
	}

	@Test
	void test_Controller_07() throws Exception {
		//Controller ctrl = new Controller("src/test/ops5/Ops5Programs/fail1.ops");
	}

}
