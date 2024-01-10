package ops5.workingmemory.data;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;

import ops5.workingmemory.data.action.Action;
import ops5.workingmemory.node.NodeTermination;

/**
 * Wrap ReteEntitiy's for being able to order them according to rule firing
 * strategy LEX or MEA.
 */
public record ReteEntityWrapper(NodeTermination nodeTerm, ReteEntity reteEntity)
		implements Comparable<ReteEntityWrapper> {

	public boolean fire(BigInteger time) throws Exception {
		Boolean res = true;
		// execute all actions
		for (Action action : nodeTerm.ruleToFire.actions()) {
			boolean tmpRes = action.executeAction(reteEntity, nodeTerm, time);
			res = res && tmpRes;
		}
		return res;
	}

	@Override
	public int compareTo(ReteEntityWrapper o) {
		final int res;
		if (this.equals(o)) {
			res = 0;
		} else {
			res = System.identityHashCode(this) - System.identityHashCode(o);
		}
		return res;
	}

	@Override
	public String toString() {
		return "rule " + this.nodeTerm.toString() + ": " + this.reteEntity.toString();
	}

//	@Override
//	public boolean equals(Object other) {
//		boolean res = false;
//		if (other instanceof ReteEntityWrapper) {
//			ReteEntityWrapper otherEnt = (ReteEntityWrapper) other;
//			res = res && Objects.equals(this.nodeTerm(), otherEnt.nodeTerm());
//			res = res && Objects.equals(this.reteEntity().entity(), otherEnt.reteEntity().entity());
//			res = res && Objects.equals(this.reteEntity().creationTime(), otherEnt.reteEntity().creationTime());
//			res = res && Objects.equals(this.reteEntity().creationTimeMostLeft(),
//					otherEnt.reteEntity().creationTimeMostLeft());
//		}
//		return res;
//	}
}
