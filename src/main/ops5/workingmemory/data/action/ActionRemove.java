package ops5.workingmemory.data.action;

import java.math.BigInteger;
import java.util.HashMap;

import ops5.ModelRete;
import ops5.parser.StringHelpers;
import ops5.workingmemory.data.Fact;
import ops5.workingmemory.data.Literal;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.node.NodeRoot;
import ops5.workingmemory.node.NodeTermination;

public final class ActionRemove implements Action {
	final private String elemVar;
	final private String literalName;

	/**
	 * 
	 * @param str "(remove &#60;count>)"
	 */
	public ActionRemove(String str, HashMap<String, Literal> elemvarToLiteral) throws Exception {
		super();
		String[] parts = str.split(" ");
		String prefix = parts[0];
		if (!prefix.equalsIgnoreCase("remove") || parts.length != 2) {
			throw new IllegalArgumentException(str);
		}
		String elemVar = parts[1];
		if (!elemvarToLiteral.containsKey(elemVar)) {
			throw new IllegalArgumentException("elemVar undefined: " + elemVar
					+ "\nNote that reference by index is not allowed in this OPS5 implementation.");
		}
		this.elemVar = elemVar;
		this.literalName = elemvarToLiteral.get(elemVar).name();
	}

	@Override
	public Boolean executeAction(ReteEntity reteEntity, NodeTermination from, BigInteger time) {
		Fact fact = reteEntity.getElemvarFact(elemVar);
		ModelRete modelRete = from.modelRete.get();
		NodeRoot nodeRoot = modelRete.getRootNode(literalName);
		nodeRoot.removeFact(fact); // remove fact
		return true;
	}

	@Override
	public String getElemVar() {
		return this.elemVar;
	}
}
