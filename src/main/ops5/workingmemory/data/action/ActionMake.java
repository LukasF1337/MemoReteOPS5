package ops5.workingmemory.data.action;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import ops5.ModelRete;
import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Literal;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.node.NodeRoot;
import ops5.workingmemory.node.NodeTermination;

public final class ActionMake implements Action {
	final private Literal literal;
	final private ImmutableMap<String, Value<?>> attributeToValue; // ^city -> Washington
	final private ImmutableMap<String, String> attributeToAtomvar; // ^city -> <the_city>
	final private ImmutableMap<String, FunctionCompute> attributeToCompute; // ^value -> (compute <val> + 1)

	public ActionMake(String str, Map<String, Literal> literals, Set<String> atomVariables) throws Exception {
		super();
		String[] attributes = str.split("\\^");
		// get name and literal of fact:
		String[] prefix = attributes[0].split(" ");
		if (prefix.length == 2 && prefix[0].toLowerCase().equals("make")) { // if "make robot"
			String name = prefix[1];
			literal = literals.get(name);
			if (literal == null) {
				throw new Exception("Can't find literal \"" + name + "\" for fact:\n" + str);
			}
		} else {
			throw new Exception("Can't parse:\n" + prefix.length + "\nas part of of:\n" + str);
		}
		ImmutableMap.Builder<String, Value<?>> attributeToValueBuilder = new ImmutableMap.Builder<>();
		ImmutableMap.Builder<String, String> attributeToAtomvarBuilder = new ImmutableMap.Builder<>();
		ImmutableMap.Builder<String, FunctionCompute> attributeToComputeBuilder = new ImmutableMap.Builder<>();
		// get attributes of fact:
		for (int i = 1; i < attributes.length; i++) {
			String[] tmp = attributes[i].split(" ", 2);
			String namee = tmp[0];
			if (!literal.attributeNames().contains(namee)) {
				throw new IllegalArgumentException(
						"Trying to make Fact with attribute: " + namee + "\nThat does not exist in Literal:" + literal);
			}
			String valueString = tmp[1].trim();
			if (valueString.charAt(0) == '<' && valueString.charAt(valueString.length() - 1) == '>') {
				attributeToAtomvarBuilder.put(namee, valueString);
			} else if (valueString.charAt(0) == '(' && valueString.charAt(valueString.length() - 1) == ')') {
				attributeToComputeBuilder.put(namee, new FunctionCompute(valueString, atomVariables));
			} else {
				Value<?> newValue = new Value.Builder().setValue(Optional.of(valueString)).build();
				attributeToValueBuilder.put(namee, newValue);
			}
		}
		attributeToValue = attributeToValueBuilder.build();
		attributeToAtomvar = attributeToAtomvarBuilder.build();
		attributeToCompute = attributeToComputeBuilder.build();
	}

	@Override
	public Boolean executeAction(ReteEntity reteEntity, NodeTermination from, BigInteger time) throws Exception {
		ImmutableMap.Builder<String, Value<?>> values = new ImmutableMap.Builder<>();
		values.putAll(attributeToValue);
		for (Map.Entry<String, String> e : attributeToAtomvar.entrySet()) {
			String attr = e.getKey();
			String atomVarString = e.getValue();
			Value<?> value = from.getValue(atomVarString, reteEntity);
			values.put(attr, value);
		}
		for (Map.Entry<String, FunctionCompute> e : attributeToCompute.entrySet()) {
			String attr = e.getKey();
			FunctionCompute compute = e.getValue();
			Value<?> value = compute.executeAndGetValue(reteEntity).orElse(null);
			if (value != null) {
				values.put(attr, value);
			}
		}
		final Fact newFact = new Fact(values.build());
		ModelRete modelRete = from.modelRete.get();
		NodeRoot nodeRoot = modelRete.getRootNode(literal.name());
		nodeRoot.addFact(newFact, time);
		return true;
	}

	@Override
	public String getElemVar() {
		return null;
	}
}
