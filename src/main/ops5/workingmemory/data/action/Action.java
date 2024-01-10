package ops5.workingmemory.data.action;

import java.lang.ref.WeakReference;
import java.math.BigInteger;

import ops5.ModelRete;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.node.NodeTermination;

/**
 * Each Action has an ActionType and can be executed by calling executeAction
 * 
 *
 */

public interface Action {

//	public static enum ActionType { // 5.2.1
//		// Incomplete List of possible actions
//		CALL, HALT, MAKE, MODIFY, REMOVE, WRITE
//	}

	public abstract Boolean executeAction(ReteEntity reteEntity, NodeTermination from, BigInteger time)
			throws Exception;

	public abstract String getElemVar();

}
