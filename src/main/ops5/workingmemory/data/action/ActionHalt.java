package ops5.workingmemory.data.action;

import java.math.BigInteger;

import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.node.NodeTermination;

public final class ActionHalt implements Action {

	@Override
	public Boolean executeAction(ReteEntity reteEntity, NodeTermination from, BigInteger time) throws Exception {
		// return false because this action stops execution
		return false;
	}

	@Override
	public String getElemVar() {
		return null;
	}
}
