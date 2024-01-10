package ops5;

import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;

import ops5.Controller;
import ops5.workingmemory.data.condition.ConditionPredicate;
import ops5.workingmemory.node.Node;
import ops5.workingmemory.node.NodeBeta;
import ops5.workingmemory.node.NodeBeta.Position;

public class IntegrationTests {

	private Controller ctrl;
	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

	private void assertEqualNumbers(List<Number> numbersList1, String strNumbers2) {
		HashMultiset<Number> numbers1 = HashMultiset.create();
		numbers1.addAll(numbersList1);
		String[] strNumbers2array = strNumbers2.trim().split(" ");
		Multiset<Number> numbers2 = HashMultiset.create();
		for (String str : strNumbers2array) {
			if (str.equals("")) {
				continue;
			}
			Number num;
			try {
				num = Integer.parseInt(str);
			} catch (Exception e) {
				num = Double.parseDouble(str);
			}
			numbers2.add(num);
		}
		assertEquals(numbers1, numbers2);
	}

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		outputStreamCaptor.reset();
		System.setOut(new PrintStream(outputStreamCaptor));
		ctrl = new Controller("src/test/ops5/Ops5Programs/varietyProgram.ops");
	}

	@AfterEach
	void tearDown() throws Exception {
		System.setOut(standardOut);
		outputStreamCaptor.reset();
	}

	@Test
	void test_SameFactMultipleOccurenceAdd_00() throws Exception {
		ctrl.addKnowledge("(p same " + //
				"(number ^value 2.2) " + //
				"-->" + //
				" (make number ^value 4235)" + //
				" (make number ^value 4235)" + //
				" (make number ^value 4235))" //
				+ "(p other " + //
				"(number ^value 4235 ^value <num1>) " + //
				"-->" + //
				" (write <num1> | |))" //
		);
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(4235, 4235, 4235), outputStreamCaptor.toString());
	}

	@Test
	void test_SameFactMultipleOccurenceAdd_01() throws Exception {
		ctrl.addKnowledge("(p same " + //
				"(number ^value 2.2) " + //
				"-->" + //
				" (make number ^value 4235 ^value2 4235)" + //
				" (make number ^value 4235 ^value2 4235)" + //
				" (make number ^value 4235 ^value2 4235))" //
				+ "(p other " + //
				"(number ^value <num1>) " + //
				"(number ^value > <num1> ^value 4235) " + //
				"-->" + //
				" (write <num1> | |))" //
		);
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(//
				1, 2, 3, 3, 3, 4, 1.1, 2.2, 3.3, // first 4235
				1, 2, 3, 3, 3, 4, 1.1, 2.2, 3.3, // second 4235
				1, 2, 3, 3, 3, 4, 1.1, 2.2, 3.3 // third 4235
		), outputStreamCaptor.toString());
	}

	@Test
	void test_SameFactMultipleOccurenceRemove_00() throws Exception {
	}

	@Test
	void test_SameFactMultipleOccurenceRemove_01() throws Exception {
	}

	@Test
	void test_Equal_00() throws Exception {
	}

	@Test
	void test_Unequal_00() throws Exception {
	}

	@Test
	void test_BetaNonExistent_00() throws Exception {
		ctrl.addKnowledge("(p not " + //
				"-(number ^value 4563453542) " + //
				"-->" + //
				" (write |1456745 |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(1456745), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_00() throws Exception {
		ctrl.addKnowledge("(p smallerequal " + //
				"(number ^value <num1> ^value <= 3) " + //
				"-->" + //
				" (write <num1> | |))"); // numbers smaller equal than 3
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(1, 1.1, 2, 2.2, 3, 3, 3), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_01() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value < 3) " + //
				"-->" + //
				" (write <num1> | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(1, 1.1, 2, 2.2), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_02() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value < 1.2) " + // num1 = [1, 1.1]
				"(number ^value > <num1> ^value <num2>) " + // num2 > num1
				"-->" + //
				" (write <num2> | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				1.1, 2, 2.2, 3, 3, 3, 3.3, 4, // > 1
				2, 2.2, 3, 3, 3, 3.3, 4), // > 1.1
				outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_03() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3) " + //
				"(number ^value > <num1> ^value <num2>) " + //
				"-->" + //
				" (write <num2> | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3.3, 4, // > 3
				3.3, 4, // > 3
				3.3, 4, // > 3
				4 // > 3.3
					// > 4
		), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_04() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value < 3.2) " + //
				"(number ^value > <num1> ^value <num2>) " + //
				"-->" + //
				" (write <num2> | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3.3, 4, 3.3, 4, 3.3, 4), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_05() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value < 3.2) " + // num1 < 3.2
				"(number ^value > <num1> ^value <num2>) " + // num2 > num1
				"(number ^value <num2> ^value 4) " + // num2 == 4
				"-->" + //
				" (write <num2> | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				4, 4, 4 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_06() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value < 3.2) " + // 3,3,3
				"(number ^value2 <num2> ^value2 >= 2 ^value2 < 3.2) " + // 3,2,2.5
				"(number ^value2 <num1>) " + // num1==num2;
				"-->" + //
				" (write <num1> | |))"); // 3 times "3" times 3 times 1 = 9 * "3"
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3, 3, 3, 3, 3, 3, 3, 3, 3 // 9 * "3"
		), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_07() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value < 3.3) " + // 3,3,3
				"(number ^value2 <num2> ^value2 >= 2 ^value2 < 3.2) " + // 3,2,2.5
				"(number ^value2 <num1>) " + // num1==num2;
				"-->" + //
				" (write <num1> | |))"); // 3 times "3" times 3 times 1 = 9 * "3"
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3, 3, 3, 3, 3, 3, 3, 3, 3 // 9 * "3"
		), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_08() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value <= 3.3) " + // 3,3,3,3.3
				"(number ^value2 <num2> ^value2 >= 2 ^value2 < 3.2) " + // 3,2,2.5
				"(number ^value2 <num1>) " + // num1==num2; 3 only remaining value
				"-->" + //
				" (write <num1> | |))"); // "3,3,3" times 3 times 1
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3, 3, 3, 3, 3, 3, 3, 3, 3 // 9 * "3"
		), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaExistentGreaterSmallerAndSoOn_09() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value <= 4) " + // num1 = 3,3,3,3.3,4
				"(number ^value2 <num2> ^value2 >= 2 ^value2 < 4.2) " + // num2 = 3,2,2.5,3.5,4
				"(number ^value2 <num1>) " + // num1==num2; "3,4" only remaining possible values
				"-->" + //
				" (write <num1> | |))"); // "3,3,3,4" * 5 * 1
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3, 3, 3, 4, 3, 3, 3, 4, 3, 3, 3, 4, 3, 3, 3, 4, 3, 3, 3, 4 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaNoneistentGreaterSmallerAndSoOn_00() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value >= 3 ^value <= 4) " + // num1 = 3,3,3,3.3,4
				"-(number ^value2 <num2> ^value2 < 1) " + // no matches
				"-->" + //
				" (write <num1> | |))"); // num1 = 3,3,3,3.3,4
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				3, 3, 3, 3.3, 4), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaNoneistentGreaterSmallerAndSoOn_01() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"-(number ^value2 < 1) " + // no matches
				"-->" + //
				" (write |1  |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				1), outputStreamCaptor.toString());
	}

	@Test
	void test_BetaNoneistentGreaterSmallerAndSoOn_02() throws Exception {
		ctrl.addKnowledge("(p removeNumber " + //
				"{<number1> (number ^value <num1> ^value >= 3 ^value <= 4)} " + // num1 = 3,3,3,3.3,4
				"-(number ^value2 <num2> ^value2 < 1) " + // no matches
				"-->" + //
				"(remove <number1>))" + //

				"(p removedcheck1 " + //
				"-(number ^value 3)" + // no matches
				"-->" + //
				" (write |546456 |))" + //
				"(p removedcheck2 " + //
				"-(number ^value 3.3)" + // no matches
				"-->" + //
				" (write |546456 |))" + //
				"(p removedcheck3 " + //
				"-(number ^value 4)" + // no matches
				"-->" + //
				" (write |546456 |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				546456, 546456, 546456), outputStreamCaptor.toString());
	}

	@Test
	void test_AtomVar_00() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1>) " + //
				"-->" + //
				" (write <num1> | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				4 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_00() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 3 + <num1>) | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				7 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_01() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 3 - <num1>) | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				-1 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_02() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 3 // <num1>) | |))"); // 3/4 = 0, because integer div
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				0 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_03() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 3.1 // <num1>) | |))"); // 3.1/4 = 0.775, because double div
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				0.775 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_04() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 2354321 \\\\ <num1>) | |))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				1 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_05() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE <num1> * 2)))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				8 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_06() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE (<num1> + 2) + 2)))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				8 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_07() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE (<num1> + 2) // 2 * (<num1> + 2))))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				18 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_08() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 1+(1+(1+(1+((1+(1+1))+1)+1))))))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				9 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_09() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 1+(1+(1+(1+((1+(1.0+1))+1)+1))))))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				9.0 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_FunctionCompute_10() throws Exception {
		ctrl.addKnowledge("(p smaller " + //
				"(number ^value <num1> ^value2 <num1> ^value <> NIL) " + //
				"-->" + // num1==4
				" (write (COMPUTE 1+(1+(1+(1+((1+(4231.05436654+1))+1)+2))))))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList( //
				4240.05436654 //
		), outputStreamCaptor.toString());
	}

	@Test
	void test_ActionMake_00() throws Exception {
		ctrl.addKnowledge("(p regsedfg " + //
				"{<num1> (number ^value 1) }" + //
				"-->" + //
				" (make number ^value 43125324))" //
				+ "(p print " + //
				"(number ^value 43125324)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(43125324), //
				outputStreamCaptor.toString());

	}

	@Test
	void test_ActionModify_00() throws Exception {
		ctrl.addKnowledge("(p regsedfg " + //
				"{<num1> (number ^value 1) }" + //
				"-->" + //
				" (modify <num1> ^value 43125324))" //
				+ "(p print " + //
				"(number ^value 43125324)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(43125324), //
				outputStreamCaptor.toString());
	}

	@Test
	void test_ActionModify_01() throws Exception {
		ctrl.addKnowledge("(p regsedfg " + //
				"{<num1> (number ^value 1) }" + //
				"-->" + //
				" (modify <num1> ^value 43125324))" //
				+ "(p first " + //
				"{<num1>(number ^value 43125324)}" + //
				"-->" + //
				" (modify <num1> ^value 12351))" //
				+ "(p second " + //
				"(number ^value 12351)" + //
				"-->" + //
				" (write |12351|))" //
		);
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(12351), //
				outputStreamCaptor.toString());
	}

	@Test
	void test_ActionWrite_00() throws Exception {
		ctrl.addKnowledge("(p print " + //
				"(number ^value <num1>) " + //
				"-->" + //
				" (write <num1> | | ))");
		ctrl.run(true);
		assertEqualNumbers(Arrays.asList(3, 4, 2.2, 3, 1.1, 2, 1, 3, 3.3), //
				outputStreamCaptor.toString());
	}

	@Test
	void test_ActionRemove_00() throws Exception {
		ctrl.addKnowledge("(p rm " + //
				"{<num> (number) }" + //
				"-->" + //
				" (remove <num>))" + //
				"(p testRemoved " + //
				"-(number)" + //
				"-->" + //
				" (write |successful remove|))");
		ctrl.run(true);
		assertEquals("successful remove", //
				outputStreamCaptor.toString());
	}

	@Test
	void test_ActionHalt_00() throws Exception {
		ctrl.addKnowledge("(p rm " + //
				"{<num> (number) }" + //
				"-->" + //
				" (remove <num>)" + //
				"(halt))" + //
				"(p testRemoved " + //
				"-(number)" + //
				"-->" + //
				"(write |should not be printed|)" //
				+ ")");
		ctrl.run(true);
		assertEquals("", //
				outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_00() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new))");
		ctrl.run(true);
		assertEquals("MockClassActionCall()", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_01() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new ^str test))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_02() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new ^str 5))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_03() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new ^str 7.0))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_04() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new ^str * test1 test2))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_05() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new ^str * 1 2))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_05_2() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new"//
				+ " ^str * 1 2 ^integr 7))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_05_3() throws Exception {
		ctrl.addKnowledge("(p prepare -(uninitialized)" //
				+ "-->"//
				+ "(make uninitialized ^value 1 ^value2 2 ^value3 7))"//
				+ "(p calls " + //
				"(uninitialized ^value <atom1> ^value2 <atom2> ^value3 <atom3>) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new"//
				+ " ^str * <atom1> <atom2> ^integr <atom3>))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_05_4() throws Exception {
		ctrl.addKnowledge("(p prepare"//
				+ "-(uninitialized)"//
				+ "-->"//
				+ "(make uninitialized ^value 1 ^value2 2 ^value3 8.0))"//
				+ "(p calls " + //
				"(uninitialized ^value <atom1> ^value2 <atom2> ^value3 <atom3>) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new "//
				+ "^str * <atom1> <atom2> ^integr <atom3>))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_06() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new ^str * 1.0 2.0))");
		ctrl.run(true);
		assertEquals("", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_07() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"-(number ^value 543674568776) " + //
				"-->" + //
				" (call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str * 1.0 2.0 " //
				+ "=> make uninitialized ^value 43125324 ^value2 (compute 1+3)))" //
				+ "(p print " + //
				"(uninitialized ^value 43125324 ^value2 4)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_08() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 3) } " + //
				"-->" + //
				"(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str * 1.0 2.0 " //
				+ "=> modify <elemvar1> ^value 43125324 ^value2 (compute 1+3)))" //
				+ "(p print " + //
				"(number ^value 43125324 ^value2 4)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_09() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 3) } " + //
				"-->" + //
				"(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str test) " //
				+ "(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.test " //
				+ "=> " //
				+ "modify <elemvar1> ^value <?>))" //
				+ "(p print " + //
				"(number ^value success ^value2 3)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_10() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 3) } " + //
				"-->" + //
				"(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str test) " //
				+ "(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.test2 "
				+ "^list * test1 test2" //
				+ "=> " //
				+ "modify <elemvar1> ^value <?>))" //
				+ "(p print " + //
				"(number ^value test1 ^value2 test2)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_10_1() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 3) } " + //
				"-->" + //
				"(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str test) " //
				+ "(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.test2 "
				+ "^list * test1 test2" //
				+ "=> " //
				+ "modify <elemvar1> ^value2 <?>))" //
				+ "(p print " + //
				"(number ^value 1 ^value2 test1 ^value3 test2)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_10_2() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 3) } " + //
				"-->" + //
				"(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str test) " //
				+ "(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.test2 "
				+ "^list * test1 test2" //
				+ "=> " //
				+ "modify <elemvar1> ^value3 <?>))" //
				+ "(p print " + //
				"(number ^value 1 ^value2 3 ^value3 test1)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}

	@Test
	void test_ActionCall_10_3() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 <atom25>) } " + //
				"-->" + //
				"(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str test) " //
				+ "(call callzo ops5.workingmemory.data.action.MockClassActionCallTest.test2 "
				+ "^list * test1 test2" //
				+ "=> " //
				+ "modify <elemvar1> ^value3 <?> ^value <atom25>))" //
				+ "(p print " + //
				"(number ^value 3 ^value2 3 ^value3 test1)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}
	
	@Test
	void test_ActionCall_10_4() throws Exception {
		ctrl.addKnowledge("(p calls " + //
				"{ <elemvar1> (number ^value 1 ^value2 <atom25>) } " + //
				"-->" + //
				"(call <atom25> ops5.workingmemory.data.action.MockClassActionCallTest.new" //
				+ " ^str test) " //
				+ "(call <atom25> ops5.workingmemory.data.action.MockClassActionCallTest.test2 "
				+ "^list * test1 test2" //
				+ "=> " //
				+ "modify <elemvar1> ^value3 <?> ^value <atom25>))" //
				+ "(p print " + //
				"(number ^value 3 ^value2 3 ^value3 test1)" + //
				"-->" + //
				" (write |43125324|))" //
		);
		ctrl.run(true);
		assertEquals("43125324", outputStreamCaptor.toString());
	}
	
// TODO FIXME:
//	@Test
//	void test_ActionCall_10_5() throws Exception {
//		ctrl.addKnowledge("(p calls " + //
//				"{ <elemvar1> (number ^value 1 ^value2 <atom25>) } " + //
//				"-->" + //
//				"(call (compute 7 + <atom25>) ops5.workingmemory.data.action.MockClassActionCallTest.new" //
//				+ " ^str test) " //
//				+ "(call (compute 4 + <atom25> + 3) ops5.workingmemory.data.action.MockClassActionCallTest.test2 "
//				+ "^list * test1 test2" //
//				+ "=> " //
//				+ "modify <elemvar1> ^value3 <?> ^value <atom25>))" //
//				+ "(p print " + //
//				"(number ^value 3 ^value2 3 ^value3 test1)" + //
//				"-->" + //
//				" (write |43125324|))" //
//		);
//		ctrl.run(true);
//		assertEquals("43125324", outputStreamCaptor.toString());
//	}

	@Test
	void test_ActionCall_ECar_1() throws Exception {
		ctrl.addKnowledge("(p print " + //
				"-(number ^value 25456345167) " + //
				"-->" + //
				" (call Jrobo1 emo.ifs.ecar.ECar.new ^eCarName Jrobo1)" + //
				" (call Jrobo1 emo.ifs.ecar.ECar.connect))");
		ctrl.run(true);
	}
}
