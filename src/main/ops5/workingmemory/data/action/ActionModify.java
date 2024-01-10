package ops5.workingmemory.data.action;

import java.math.BigInteger;
import java.util.*;

import com.google.common.collect.ImmutableMap;

import ops5.ModelRete;
import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Literal;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.node.NodeRoot;
import ops5.workingmemory.node.NodeTermination;

public final class ActionModify implements Action {
	final private String literalName; // Literal to be modified
	final private String elemVar; // Element variable
	final private ImmutableMap<String, Value<?>> attributeToValue; // ^city -> Washington
	final private ImmutableMap<String, String> attributeToAtomvar; // ^city -> <the_city>
	final private ImmutableMap<String, FunctionCompute> attributeToCompute; // ^value -> (compute <val> + 1)

	/**
	 * @param str (modify &#60;elemVar> ^name R2-D2 ^size &#60;sizeVar> ^value
	 *            (compute &#60;val> + 1))
	 */
	public ActionModify(String str, Set<String> atomVariables, HashMap<String, Literal> elemvarToLiteral)
			throws Exception {
		super();
		String[] attributes = str.split("\\^");
		// get name and literal of fact:
		String[] prefix = attributes[0].split(" ");
		if (prefix.length == 2 && prefix[0].toLowerCase().equals("modify")) { // if "modify robot"
			elemVar = prefix[1];
			literalName = elemvarToLiteral.get(elemVar).name();
		} else {
			throw new IllegalArgumentException("Can't parse:\n" + prefix.length + "\nas part of of:\n" + str);
		}
		ImmutableMap.Builder<String, Value<?>> attributeToValueBuilder = new ImmutableMap.Builder<>();
		ImmutableMap.Builder<String, String> attributeToAtomvarBuilder = new ImmutableMap.Builder<>();
		ImmutableMap.Builder<String, FunctionCompute> attributeToComputeBuilder = new ImmutableMap.Builder<>();
		// get attributes of fact:
		for (int i = 1; i < attributes.length; i++) {
			String[] tmp = attributes[i].split(" ", 2);
			String namee = tmp[0]; // could I check if this attribute name actually is part of the literal?
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
		Fact fact = reteEntity.getElemvarFact(elemVar);
		ImmutableMap.Builder<String, Value<?>> builder = ImmutableMap.builder();
		builder.putAll(fact.attributeValues());
		builder.putAll(attributeToValue);
		for (ImmutableMap.Entry<String, String> e : attributeToAtomvar.entrySet()) {
			String attribute = e.getKey();
			Value<?> value = reteEntity.getValue(e.getValue());
			builder.put(attribute, value);
		}
		for (ImmutableMap.Entry<String, FunctionCompute> e : attributeToCompute.entrySet()) {
			String attribute = e.getKey();
			FunctionCompute comp = e.getValue();
			Value<?> value = comp.executeAndGetValue(reteEntity).orElse(null);
			if (value != null) {
				builder.put(attribute, value);
			}
		}

		Fact newFact = new Fact(builder.buildKeepingLast());
		ModelRete modelRete = from.modelRete.get();
		if (modelRete == null) {
			throw new IllegalStateException("Outrageous");
		}
		NodeRoot nodeRoot = modelRete.getRootNode(literalName);
		nodeRoot.removeFact(fact); // remove old fact
		nodeRoot.addFact(newFact, time); // add new modified fact
		return true; // no halt called, return true;
	}

	@Override
	public String getElemVar() {
		return this.elemVar;
	}
}
