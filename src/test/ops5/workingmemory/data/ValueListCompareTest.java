package ops5.workingmemory.data;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ops5.Controller;
import ops5.workingmemory.data.condition.ConditionPredicate;
import ops5.workingmemory.node.*;
import ops5.workingmemory.node.NodeBeta.Position;

public class ValueListCompareTest {

	private NodeBetaExistent mockNodeBeta;
	private ArrayList<ImmutableMap<String, Value<?>>> mockValueListData;

	public ValueListCompareTest() {
	}

	private ImmutableMap<String, Value<?>> makeMapForTest(ArrayList<String> atomvars, Object[] elements) {
		ImmutableMap.Builder<String, Value<?>> builder = ImmutableMap.builder();
		for (int i = 0; i < elements.length; i++) {
			Object obj = elements[i];
			String atomVar = atomvars.get(i);
			switch (obj) {
			case String str -> builder.put(atomVar, new Value(ValueType.STRING, str));
			case Integer integer -> builder.put(atomVar, new Value(ValueType.INTEGER, integer));
			case Double doublee -> builder.put(atomVar, new Value(ValueType.DOUBLE, doublee));
			default -> throw new IllegalStateException("");
			}
		}
		return builder.build();
	}

	private ImmutableList<Value<?>> listToValueList(Object[] elements) {
		ImmutableList.Builder<Value<?>> builder = ImmutableList.builder();
		for (int i = 0; i < elements.length; i++) {
			Object obj = elements[i];
			switch (obj) {
			case String str -> builder.add(new Value(ValueType.STRING, str));
			case Integer integer -> builder.add(new Value(ValueType.INTEGER, integer));
			case Double doublee -> builder.add(new Value(ValueType.DOUBLE, doublee));
			default -> throw new IllegalStateException("");
			}
		}
		return builder.build();
	}

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		// just get a mock NodeBeta with 2 previous nodes
		Controller ctrl = new Controller("./src/test/ops5/Ops5Programs/mluewbs2.ops");
		for (Node n : ctrl.modelRete.getAllNodes()) {
			if (n instanceof NodeBetaExistent nodeBeta) {
				if (nodeBeta.getPreviousNodes().size() == 2) {
					mockNodeBeta = nodeBeta;
				}
			}
		}
		mockValueListData = new ArrayList<>();
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test_equal_00() throws Exception {
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		atomVarToPredicateBuilder.put("<EQ1>", ConditionPredicate.EQUAL);

		ImmutableMap<String, ConditionPredicate> atomVarToPredicate = atomVarToPredicateBuilder.build();
		ArrayList<String> atomVars = new ArrayList<String>(atomVarToPredicate.keySet());
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 2, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 3, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 4, }));

		ValueListCompare valueListCompare = //
				new ValueListCompare(mockNodeBeta, atomVarToPredicate, Position.LEFTBETA);
		valueListCompare.addAll(mockValueListData);

		HashSet<ImmutableList<Value<?>>> res = valueListCompare
				.getMatches(makeMapForTest(atomVars, new Object[] { 2, }));
		assert (res.size() == 1);
		assert (res.contains(listToValueList(new Object[] { 2, })));
		assert (!res.contains(listToValueList(new Object[] { 1, })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 5, }));
		assert (res.size() == 0);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 4, }));
		assert (res.size() == 1);
		assert (res.contains(listToValueList(new Object[] { 4, })));
		assert (!res.contains(listToValueList(new Object[] { 2, })));
	}

	@Test
	void test_equal_01() throws Exception {
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		atomVarToPredicateBuilder.put("<EQ1>", ConditionPredicate.EQUAL);
		atomVarToPredicateBuilder.put("<EQ2>", ConditionPredicate.EQUAL);

		ImmutableMap<String, ConditionPredicate> atomVarToPredicate = atomVarToPredicateBuilder.build();
		ArrayList<String> atomVars = new ArrayList<String>(atomVarToPredicate.keySet());
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 7, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 2, 3, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 3, 4, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 4, 5, }));

		ValueListCompare valueListCompare = //
				new ValueListCompare(mockNodeBeta, atomVarToPredicate, Position.LEFTBETA);
		valueListCompare.addAll(mockValueListData);

		HashSet<ImmutableList<Value<?>>> res = valueListCompare
				.getMatches(makeMapForTest(atomVars, new Object[] { 2, 3 }));
		assert (res.size() == 1);
		assert (res.contains(listToValueList(new Object[] { 2, 3 })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 5, 3 }));
		assert (res.size() == 0);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 1, 1 }));
		assert (res.size() == 0);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 4, 5 }));
		assert (res.size() == 1);
		assert (res.contains(listToValueList(new Object[] { 4, 5 })));
	}

	@Test
	void test_unequal_00() throws Exception {
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		atomVarToPredicateBuilder.put("<UEQ1>", ConditionPredicate.UNEQUAL);

		ImmutableMap<String, ConditionPredicate> atomVarToPredicate = atomVarToPredicateBuilder.build();
		ArrayList<String> atomVars = new ArrayList<String>(atomVarToPredicate.keySet());
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 2, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 3, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 4, }));

		ValueListCompare valueListCompare = //
				new ValueListCompare(mockNodeBeta, atomVarToPredicate, Position.LEFTBETA);
		valueListCompare.addAll(mockValueListData);

		HashSet<ImmutableList<Value<?>>> res = valueListCompare
				.getMatches(makeMapForTest(atomVars, new Object[] { 2, }));
		assert (res.size() == 3);
		assert (!res.contains(listToValueList(new Object[] { 2, })));
		assert (res.contains(listToValueList(new Object[] { 1, })));
		assert (res.contains(listToValueList(new Object[] { 3, })));
		assert (res.contains(listToValueList(new Object[] { 4, })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 5, }));
		assert (res.size() == 4);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 4, }));
		assert (res.size() == 3);
		assert (!res.contains(listToValueList(new Object[] { 4, })));
		assert (res.contains(listToValueList(new Object[] { 1, })));
		assert (res.contains(listToValueList(new Object[] { 2, })));
		assert (res.contains(listToValueList(new Object[] { 3, })));
	}

	@Test
	void test_smaller_00() throws Exception {
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		atomVarToPredicateBuilder.put("<smaller>", ConditionPredicate.SMALLER);

		ImmutableMap<String, ConditionPredicate> atomVarToPredicate = atomVarToPredicateBuilder.build();
		ArrayList<String> atomVars = new ArrayList<String>(atomVarToPredicate.keySet());
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 2, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 3, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 4, }));

		ValueListCompare valueListCompare = //
				new ValueListCompare(mockNodeBeta, atomVarToPredicate, Position.LEFTBETA);
		valueListCompare.addAll(mockValueListData);

		HashSet<ImmutableList<Value<?>>> res = valueListCompare
				.getMatches(makeMapForTest(atomVars, new Object[] { 2, }));
		assert (res.size() == 2);
		assert (!res.contains(listToValueList(new Object[] { 2, })));
		assert (!res.contains(listToValueList(new Object[] { 1, })));
		assert (res.contains(listToValueList(new Object[] { 3, })));
		assert (res.contains(listToValueList(new Object[] { 4, })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 5, }));
		assert (res.size() == 0);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { -3, }));
		assert (res.size() == 4);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 4, }));
		assert (res.size() == 0);

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 1, }));
		assert (res.size() == 3);
		assert (res.contains(listToValueList(new Object[] { 4, })));
		assert (!res.contains(listToValueList(new Object[] { 1, })));
		assert (res.contains(listToValueList(new Object[] { 2, })));
		assert (res.contains(listToValueList(new Object[] { 3, })));
	}

	@Test
	void test_sametype_00() throws Exception {
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		atomVarToPredicateBuilder.put("<type>", ConditionPredicate.SAMETYPE);

		ImmutableMap<String, ConditionPredicate> atomVarToPredicate = atomVarToPredicateBuilder.build();
		ArrayList<String> atomVars = new ArrayList<String>(atomVarToPredicate.keySet());
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { "car", }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 3.0, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 17, }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { "city", }));

		ValueListCompare valueListCompare = //
				new ValueListCompare(mockNodeBeta, atomVarToPredicate, Position.LEFTBETA);
		valueListCompare.addAll(mockValueListData);

		HashSet<ImmutableList<Value<?>>> res = valueListCompare
				.getMatches(makeMapForTest(atomVars, new Object[] { 2, }));
		assert (res.size() == 2);
		assert (res.contains(listToValueList(new Object[] { 1, })));
		assert (res.contains(listToValueList(new Object[] { 17, })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { "star", }));
		assert (res.size() == 2);
		assert (res.contains(listToValueList(new Object[] { "car", })));
		assert (res.contains(listToValueList(new Object[] { "city", })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { -3.761, }));
		assert (res.size() == 1);
		assert (res.contains(listToValueList(new Object[] { 3.0, })));
	}

	@Test
	void test_mixed_00() throws Exception {
		ImmutableMap.Builder<String, ConditionPredicate> atomVarToPredicateBuilder = ImmutableMap.builder();
		atomVarToPredicateBuilder.put("<type>", ConditionPredicate.SAMETYPE);
		atomVarToPredicateBuilder.put("<size>", ConditionPredicate.BIGGER);
		atomVarToPredicateBuilder.put("<size2>", ConditionPredicate.SMALLEREQUAL);
		atomVarToPredicateBuilder.put("<eq>", ConditionPredicate.EQUAL);

		ImmutableMap<String, ConditionPredicate> atomVarToPredicate = atomVarToPredicateBuilder.build();
		ArrayList<String> atomVars = new ArrayList<String>(atomVarToPredicate.keySet());
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 3, 6345, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 5, 905, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 7, 60, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 9, 50, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 14, 50, 19 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 12, 16, 50, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 23, 49, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 33, 45, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 36, 30, 20 }));
		mockValueListData.add(makeMapForTest(atomVars, new Object[] { 1, 67, 14, 20 }));

		ValueListCompare valueListCompare = //
				new ValueListCompare(mockNodeBeta, atomVarToPredicate, Position.LEFTBETA);
		valueListCompare.addAll(mockValueListData);

		HashSet<ImmutableList<Value<?>>> res = valueListCompare
				.getMatches(makeMapForTest(atomVars, new Object[] { 123, 6, 50, 20 }));
		assert (res.size() == 2);
		assert (res.contains(listToValueList(new Object[] { 1, 3, 6345, 20 })));
		assert (res.contains(listToValueList(new Object[] { 1, 5, 905, 20 })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 4, 34, 45, 20 }));
		System.err.println(res.toString());
		assert (res.size() == 7);
		assert (res.contains(listToValueList(new Object[] { 1, 33, 45, 20 })));
		assert (res.contains(listToValueList(new Object[] { 1, 23, 49, 20 })));
		assert (res.contains(listToValueList(new Object[] { 12, 16, 50, 20 })));
		assert (res.contains(listToValueList(new Object[] { 1, 9, 50, 20 })));
		assert (res.contains(listToValueList(new Object[] { 1, 7, 60, 20 })));
		assert (res.contains(listToValueList(new Object[] { 1, 5, 905, 20 })));
		assert (res.contains(listToValueList(new Object[] { 1, 3, 6345, 20 })));

		res = valueListCompare.getMatches(makeMapForTest(atomVars, new Object[] { 4, 33, 45, 19 }));
		System.err.println(res.toString());
		assert (res.size() == 1);
		assert (res.contains(listToValueList(new Object[] { 1, 14, 50, 19 })));

	}
}
