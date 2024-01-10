package ops5.workingmemory.data;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.*;

import ops5.workingmemory.data.action.Action;
import ops5.workingmemory.data.action.ActionModify;
import ops5.workingmemory.data.condition.Condition;

import java.util.LinkedHashSet;

/**
 * If a list of conditions applies fire a list of actions. ISBN-13:
 * 978-3-528-04498-5 S.37
 * 
 * @param name             name of production rule
 * @param conditions       LHS left-hand-side conditions
 * @param actions          RHS right-hand-side actions
 * @param atomVariables    variables and their values: {(&#60;variableName&#62,
 *                         value),...} book: 5.1.3.1
 * @param elementVariables variables that save an element and their values and
 *                         the associated literals:
 *                         {(&#60;elementVariableName&#62;, value),...} are
 *                         either set or are NIL. book: 5.1.3.2
 *
 */
public final record ProductionRule( //
		String name, //
		ImmutableList<Condition> conditions, //
		ImmutableList<Action> actions, //
		ImmutableSet<String> atomVariables, //
		ImmutableMap<String, Literal> elemvarToLiteral //
) {
	public ProductionRule {
		Objects.nonNull(name);
		Objects.nonNull(conditions);
		Objects.nonNull(actions);
		Objects.nonNull(atomVariables);
		Objects.nonNull(elemvarToLiteral);
		LinkedList<String> elemVarsTest = new LinkedList<>();
		// make sure Elementvariables are used only once for program correctness.
		for (Action action : actions) {
			String elemVar = action.getElemVar();
			if (elemVar != null) {
				Boolean success = elemVarsTest.add(elemVar);
				if (!success) {
					throw new IllegalArgumentException(
							"Elementvariables can only be used once. Multiple use of elementvariable \"" + elemVar
									+ "\" detected.");
				}
			}
		}
	}

	public ProductionRule( //
			Optional<String> name, //
			Optional<ArrayList<Condition>> conditions, //
			Optional<ArrayList<Action>> actions, //
			Optional<Set<String>> atomVariables, //
			Optional<ImmutableMap<String, Literal>> elemvarToLiteral//
	) {
		this( //
				name.orElseThrow(), //
				ImmutableList.copyOf(conditions.orElseThrow()), //
				ImmutableList.copyOf(actions.orElseThrow()), //
				ImmutableSet.copyOf(atomVariables.orElseThrow()), //
				ImmutableMap.copyOf(elemvarToLiteral.orElseThrow()));
	}
}
