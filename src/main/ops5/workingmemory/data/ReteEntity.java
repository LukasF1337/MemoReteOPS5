package ops5.workingmemory.data;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import ops5.workingmemory.node.*;
import ops5.workingmemory.node.NodeBeta.Position;

/**
 * Rete Entity is either referencing a fact or beta tuple. Rete Entities save
 * the state of the network digestion of facts and fact combinations (beta
 * tuples). Each Rete Entity is located on a distinct node. Rete Entities
 * propagating, being added or removed, trough the network comprise the rete
 * algorithm. The content of a rete entity is never changed. Instead, if a fact
 * is modified, the old fact and its sucessor rete entitys are removed and the
 * new fact is propagated anew trough the network. Propagation starts on a root
 * node.
 * 
 * @param node                 Node that this ReteEntity resides on
 * @param entity               can be a Fact or BetaTuple
 * @param creationTime         timestamp of when this node has been created
 *                             according to time counter of model rete. This
 *                             counter is unrelated to real time, it only tracks
 *                             which ReteEntity was created before or after
 *                             another.
 * @param creationTimeMostLeft similar timestamp like creationTime, but it
 *                             tracks the creation time of the most left
 *                             reteEntity of the most left alpha node only.
 * 
 */
public final record ReteEntity(Node node, Object entity, Integer hash, BigInteger creationTime,
		BigInteger creationTimeMostLeft) {
	// record is an immutable class that implements hashCode(), equals() and
	// toString() by default.

	public ReteEntity

	{
		assert (node != null && hash == 0 && creationTime != null && creationTimeMostLeft != null
				&& creationTime != null);
		if (entity == null || entity instanceof Fact || entity instanceof BetaTuple) {
			// valid object
		} else {
			throw new IllegalArgumentException();
		}
		hash = Objects.hashCode(entity);
	}

	public ReteEntity(Node node, BigInteger creationTime) {
		this(node, null, 0, creationTime, creationTime);
	}

	public ReteEntity(Node node, Fact fact, BigInteger creationTime) {
		this(node, (Object) fact, 0, creationTime, creationTime);
	}

	public ReteEntity(Node node, BetaTuple betaTuple, BigInteger creationTime, BigInteger creationTimeMostLeft) {
		this(node, (Object) betaTuple, 0, creationTime, creationTimeMostLeft);
	}

	public ReteEntity(Node node, Position pos, ReteEntity re, BigInteger creationTime) {
		this(node, new BetaTuple(new EnumMap<Position, ReteEntity>( //
				Map.of(pos, re) //
		)), creationTime, creationTime);
	}

	public ReteEntity(Node node, ReteEntity reLeft, ReteEntity reRight) {
		this(node, new BetaTuple(new EnumMap<Position, ReteEntity>( //
				Map.of(Position.LEFTBETA, reLeft, Position.RIGHTALPHA, reRight) //
		)), reLeft.creationTime.max(reRight.creationTime), reLeft.creationTimeMostLeft);
		// take max() of both creation times to set new creation time. Take max instead
		// of min because the time stamp is incremented with time passing and the bigger
		// value represents a more recent time.
	}

	@Override
	public int hashCode() {
		// cache hashCode(), because otherwise we would have a recursive call of
		// hashCode() each time this.hashCode() is called.
		assert (hash.equals(Objects.hashCode(entity)))
				: "Hashcode of entity isn't allowed to change. Fatal implementation error.";
		return hash;
	}

	/**
	 * equals() disregards time for comparison, it only compares the packaged
	 * entity.
	 */
	@Override
	public boolean equals(Object other) {
		boolean res = false;
		if (other instanceof ReteEntity otherReteEntity) {
			res = Objects.equals(otherReteEntity.entity, entity);
		}
		return res;
	}

	public <T> Value<T> getValue(String atomVar) {
		ReteEntity currentReteEntity = this;
		while (true) {
			Node currentNode = currentReteEntity.node();
			if (currentNode instanceof NodeBeta nodeBeta) {
				Node originNode = nodeBeta.atomVariableNodes.get(atomVar);
				BetaTuple tuple = (BetaTuple) currentReteEntity.entity();
				final Position pos;
				if (originNode instanceof NodeBeta) {
					pos = Position.LEFTBETA;
				} else {
					pos = Position.RIGHTALPHA;
				}
				currentReteEntity = tuple.betaTuple().get(pos);
			} else if (currentNode instanceof NodeAlpha nodeAlpha) {
				return (Value<T>) nodeAlpha.getValueRestriction(atomVar, currentReteEntity).value();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	public Fact getElemvarFact(String elemVar) {
		ReteEntity currentReteEntity = this; // iterator for this loop
		while (true) {
			Node currentNode = currentReteEntity.node();
			if (currentNode instanceof NodeBeta nodeBeta) {
				Node originNode = nodeBeta.elemVariableNodes.get(elemVar);
				BetaTuple tuple = (BetaTuple) currentReteEntity.entity();
				final Position pos;
				if (originNode instanceof NodeBeta) {
					pos = Position.LEFTBETA;
				} else {
					pos = Position.RIGHTALPHA;
				}
				currentReteEntity = tuple.betaTuple().get(pos);
			} else if (currentNode instanceof NodeAlpha nodeAlpha) {
				Object entity = currentReteEntity.entity();
				if (entity instanceof Fact fact) {
					return fact;
				} else {
					throw new IllegalStateException();
				}
			} else if (currentNode instanceof NodeRoot nodeRoot) {
				Object entity = currentReteEntity.entity();
				if (entity instanceof Fact fact) {
					return fact;
				} else {
					throw new IllegalStateException();
				}
			} else {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public String toString() {
		if (this.entity != null) {
			return this.entity().toString();
		} else {
			return "";
		}
	}

	public String toStringWithTime() {
		return this.toString() + " creationTimeMostLeft: " + this.creationTimeMostLeft + ", creationTime: "
				+ this.creationTime;
	}
}
