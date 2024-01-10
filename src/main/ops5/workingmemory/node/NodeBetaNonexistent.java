package ops5.workingmemory.node;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import ops5.ModelRete;
import ops5.workingmemory.data.Memoization;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.ValueListCompare;
import ops5.workingmemory.data.condition.ConditionPredicate;
import utils.SpecialBiMultiMap;

/**
 * This is a beta node where the previousRightAlphaNode tests for non-existence.
 * The chain of Alpha nodes ending in previousRightAlphaNode is similar to this:
 * "- (city ^name &#60;theCity&#62;)"
 *
 */
public final class NodeBetaNonexistent extends NodeBeta {

	final ValueListCompare wrapperLeft; // stores ImmutableList<Value<?>> for quick lookups
	final ValueListCompare wrapperRight; // stores ImmutableList<Value<?>> for quick lookups
	// Counts are tracked inside of the memoizations:
	final Memoization memoizationLeft; // track ImmutableList<Value<?>> to ReteEntity's mapping
	final Memoization memoizationRight; // track ImmutableList<Value<?>> to ReteEntity's mapping
	// Remember all matches of values lists of left side to values lists of right
	// side and vice versa.
	final SpecialBiMultiMap<ImmutableList<Value<?>>, ImmutableList<Value<?>>> reteEntityManyToManyLookup;

	public NodeBetaNonexistent(Optional<ModelRete> modelRete, Optional<NodeBeta> previousLeftBetaNode,
			Optional<NodeAlpha> previousRightAlphaNode) {
		super(modelRete, previousLeftBetaNode, previousRightAlphaNode);
		this.memoizationRight = new Memoization(this.atomVarsIntersection);
		if (this.previousLeftBetaNode == null) {
			// 1 prev node
			this.memoizationLeft = null;
			this.wrapperLeft = null;
			this.wrapperRight = null;
			ReteEntity reteEntityNew = new ReteEntity(this, Objects.requireNonNull(super.modelRete.get().getTime()));
			this.processedEntities.add(reteEntityNew);
			this.newAddEntities.add(reteEntityNew);
			this.reteEntityManyToManyLookup = null;
		} else {
			// 2 prev nodes
			this.memoizationLeft = new Memoization(this.atomVarsIntersection);
			ImmutableMap<String, ConditionPredicate> atomVarToPredicateForLookup = constructAtomVarToPredicate();
			if (this.atomVarsIntersection.size() > 0) {
				this.reteEntityManyToManyLookup = new SpecialBiMultiMap<>();
				this.wrapperLeft = new ValueListCompare(this, atomVarToPredicateForLookup, Position.LEFTBETA);
				this.wrapperRight = new ValueListCompare(this, atomVarToPredicateForLookup, Position.RIGHTALPHA);
			} else {
				this.reteEntityManyToManyLookup = null;
				this.wrapperLeft = null;
				this.wrapperRight = null;
			}
		}
	}

	@Override
	public void propagateAddedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity re : reteEntities) {
			propagateAddedEntity(re, from, time);
		}
	}

	@Override
	public void propagateAddedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		assert (from != null);
		assert (reteEntity != null
				&& (reteEntity.node() instanceof NodeAlpha || reteEntity.node() instanceof NodeBeta));

		if (this.previousLeftBetaNode == null) {
			// 1 previous node
			final int occurences = memoizationRight.add(ImmutableMap.of(), reteEntity);
			if (occurences == 1) {
				// (L,R): (1,0) -> (1,1)
				assert (this.processedEntities.size() == 1);
				ReteEntity oldReteEntitity = this.processedEntities.iterator().next();
				this.processedEntities.remove(oldReteEntitity);
				this.newRemoveEntities.add(oldReteEntitity);
			}
		} else {
			// 2 previous nodes
			final ImmutableMap.Builder<String, Value<?>> valuesMapBuilder = ImmutableMap.builder();
			for (String atomVar : this.atomVarsIntersection) {
				Value<?> val = reteEntity.getValue(atomVar);
				valuesMapBuilder.put(atomVar, val);
			}
			final ImmutableMap<String, Value<?>> valuesMap = valuesMapBuilder.build();
			final ImmutableList<Value<?>> valuesList = ImmutableList.copyOf(valuesMap.values());

			final Position pos;
			final int currentOccurences;
			if (from.equals(this.previousLeftBetaNode)) {
				pos = Position.LEFTBETA;
				currentOccurences = this.memoizationLeft.add(valuesMap, reteEntity);
			} else if (from.equals(this.previousRightAlphaNode)) {
				pos = Position.RIGHTALPHA;
				currentOccurences = this.memoizationRight.add(valuesMap, reteEntity);
			} else {
				throw new IllegalStateException();
			}

			if (currentOccurences == 1) {
				// first ReteEntity of this value list to appear
				addUniqueOccurence(pos, valuesMap, valuesList, reteEntity, time);
			} else { // currentOccurences > 1
				// O(1) lookup is possible
				if (pos.equals(Position.LEFTBETA)) {
					final int numMatches;
					if (this.reteEntityManyToManyLookup != null) {
						numMatches = reteEntityManyToManyLookup.getValues(valuesList).size();
						// if the real numMatches is >=1, then this numMatches value is also at least
						// >=1, so this is sufficient for the >0 check
					} else {
						final Multiset<ReteEntity> tmpResult = memoizationRight.get(ImmutableList.of());
						if (tmpResult != null) {
							numMatches = tmpResult.size();
						} else {
							numMatches = 0;
						}
					}
					if (numMatches > 0) {
						// (L,R): (>0,n) -> (>1,n); n is a non-negative integer
						// NOOP
					} else {
						// numMatches == 0
						// (L,R): (>0,0) -> (>1,0);
						final ReteEntity processedEntity = new ReteEntity(this, Position.LEFTBETA, reteEntity, time);
						this.processedEntities.add(processedEntity);
						this.newAddEntities.add(processedEntity);
					}
				} else { // pos == Position.RIGHTALPHA
					// (L,R): (n,>0) -> (n,>1); n is a non-negative integer
					// NOOP
				}
			}
		}
	}

	/**
	 * A new unique value list contained in valuesMap has appeared on the side
	 * Position pos. Depending on the side of pos, the value list will have a
	 * different effect on this NodeBetaNonexistent and its propagation.
	 * 
	 */
	private void addUniqueOccurence(Position pos, ImmutableMap<String, Value<?>> valuesMap,
			ImmutableList<Value<?>> valuesList, ReteEntity reteEntity, BigInteger time) {
		final Set<ImmutableList<Value<?>>> sideMatched; // ValuesList that matched on side opposite to pos
		if (this.atomVarsIntersection.size() > 0) {
			if (pos.equals(Position.LEFTBETA)) {
				sideMatched = this.wrapperRight.getMatches(valuesMap);
				this.wrapperLeft.add(valuesMap);
			} else { // pos == Position.RIGHTALPHA
				sideMatched = this.wrapperLeft.getMatches(valuesMap);
				this.wrapperRight.add(valuesMap);
			}
		} else { // this.atomVarsIntersection.size() == 0
			// just match with all possible by giving one empty ValueList:
			sideMatched = HashSet.newHashSet(1); // avoid resize()
			sideMatched.add(ImmutableList.of()); // =[[]]
			assert (sideMatched.contains(ImmutableList.of()));
		}

		if (pos.equals(Position.LEFTBETA)) {
			// (L,R): (0,n) -> (1,n); n is an integer
			if (this.atomVarsIntersection.size() > 0) {
				reteEntityManyToManyLookup.putAll(valuesList, sideMatched);
			}
			final ImmutableMultiset<ReteEntity> actualMatches = memoizationRight.getCorresponding(sideMatched);
			if (actualMatches.size() == 0) {
				// (L,R): (0,0) -> (1,0);
				ReteEntity processedEntity = new ReteEntity(this, Position.LEFTBETA, reteEntity, time);
				this.processedEntities.add(processedEntity);
				this.newAddEntities.add(processedEntity);
			}
		} else { // pos == Position.RIGHTALPHA
			// (L,R): (n,0) -> (n,1); n is a non-negative integer
			final Set<ImmutableList<Value<?>>> changed; // ValueLists that changed: (n,0) -> (n,1);
			if (reteEntityManyToManyLookup != null) {
				assert (this.atomVarsIntersection.size() > 0);
				changed = reteEntityManyToManyLookup.putAllAndReturnDifference(sideMatched, valuesList); // put(n,1)
			} else {
				assert (this.atomVarsIntersection.size() == 0);
				changed = HashSet.newHashSet(1);
				changed.add(ImmutableList.of());
				// changed is [[]]
			}
			for (Entry<ReteEntity> change : this.memoizationLeft.getCorresponding(changed).entrySet()) {
				// for each n that changed (L,R): (1,0) -> (1,1)
				final ReteEntity reteEntityChanged = change.getElement();
				final int reteEntityChangedCount = change.getCount();
				final ReteEntity processedEntity = //
						new ReteEntity(this, Position.LEFTBETA, reteEntityChanged, time);
				final int countBefore = this.processedEntities.remove(processedEntity, reteEntityChangedCount);
				this.newRemoveEntities.add(processedEntity, reteEntityChangedCount);
				if (!(countBefore >= reteEntityChangedCount)) {
					throw new IllegalStateException();
				}
			}
		}
	}

	@Override
	public void propagateRemovedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity re : reteEntities) {
			propageteRemovedEntity(re, from, time);
		}
	}

	@Override
	public void propageteRemovedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		assert (from != null);
		assert (reteEntity != null);

		final ImmutableMap.Builder<String, Value<?>> valuesMapBuilder = ImmutableMap.builder();
		for (String atomVar : this.atomVarsIntersection) {
			Value<?> val = reteEntity.getValue(atomVar);
			valuesMapBuilder.put(atomVar, val);
		}
		final ImmutableMap<String, Value<?>> valuesMap = valuesMapBuilder.build();
		final ImmutableList<Value<?>> valuesList = ImmutableList.copyOf(valuesMap.values());

		if (this.previousLeftBetaNode == null) {
			// 1 previous node
			final int size = this.memoizationRight.remove(ImmutableMap.of(), reteEntity);
			if (size == 0) {
				// (L,R): (1,1) -> (1,0)
				assert (this.processedEntities.size() == 0);
				ReteEntity reteEntityNew = new ReteEntity(this, time);
				this.processedEntities.add(reteEntityNew);
				this.newAddEntities.add(reteEntityNew);
			} else {
				// (L,R): (1,>1) -> (1,>0)
				// NOOP
			}
		} else {
			// 2 previous nodes
			final Position pos;
			final int currentOccurences;
			if (from.equals(this.previousLeftBetaNode)) {
				pos = Position.LEFTBETA;
				currentOccurences = this.memoizationLeft.remove(valuesMap, reteEntity);
			} else if (from.equals(this.previousRightAlphaNode)) {
				pos = Position.RIGHTALPHA;
				currentOccurences = this.memoizationRight.remove(valuesMap, reteEntity);
			} else {
				throw new IllegalStateException();
			}
			assert (currentOccurences >= 0);
			if (currentOccurences == 0) {
				// this ReteEntity was the last of one kind of value list
				removeUniqueOccurence(pos, valuesMap, valuesList, reteEntity, time);
			} else { // currentOccurences > 0
				if (pos.equals(Position.LEFTBETA)) {
					final int numMatches;
					if (this.reteEntityManyToManyLookup != null) {
						numMatches = reteEntityManyToManyLookup.getValues(valuesList).size();
						// if the real numMatches >= 1, then this numMatches value is also >=1, so this
						// is sufficient for the >0 check
					} else {
						final Multiset<ReteEntity> tmpResult = memoizationRight.get(ImmutableList.of());
						if (tmpResult != null) {
							numMatches = tmpResult.size();
						} else {
							numMatches = 0;
						}
					}
					if (numMatches > 0) {
						// (L,R): (>1,n) -> (>0,n); n>0
						// NOOP
					} else {
						// numMatches == 0
						// (L,R): (>1,0) -> (>0,0);
						final ReteEntity processedEntity = new ReteEntity(this, Position.LEFTBETA, reteEntity, time);
						boolean success = this.processedEntities.remove(processedEntity);
						this.newRemoveEntities.add(processedEntity);
						if (!success) {
							throw new IllegalStateException();
						}
					}
				} else { // pos.equals(Position.RIGHTALPHA)
					// (L,R): (n,>1) -> (n,>0); n is a non-negative integer
					// NOOP
				}
			}
		}
	}

	/**
	 * The number of ReteEntities in a ValueList has gone from 1 to 0. Depending on
	 * the side of pos, the removal will have a different effect on this
	 * NodeBetaNonexistent and its propagation.
	 * 
	 */
	private void removeUniqueOccurence(Position pos, ImmutableMap<String, Value<?>> valuesMap,
			ImmutableList<Value<?>> valuesList, ReteEntity reteEntity, BigInteger time) {

		if (pos.equals(Position.LEFTBETA)) {
			// (L,R): (1,n) -> (0,n); n is a non-negative integer
			final int numMatches;
			if (this.reteEntityManyToManyLookup != null) {
				this.wrapperLeft.remove(valuesMap);
				numMatches = reteEntityManyToManyLookup.getValues(valuesList).size();
				this.reteEntityManyToManyLookup.removeKey(valuesList);
			} else {
				Multiset<ReteEntity> tmp = this.memoizationRight.get(ImmutableList.of());
				if (tmp != null) {
					numMatches = tmp.size();
				} else {
					numMatches = 0;
				}
			}
			if (numMatches == 0) {
				// (L,R): (1,0) -> (0,0);
				ReteEntity re = new ReteEntity(this, Position.LEFTBETA, reteEntity, time);
				boolean success = this.processedEntities.remove(re);
				this.newRemoveEntities.add(re);
				if (!success) {
					throw new IllegalStateException();
				}
			} else {
				// (L,R): (1,>0) -> (0,>0);
				// NOOP
			}
		} else { // pos == Position.RIGHTALPHA
			// (L,R): (n,1) -> (n,0); n is a non-negative integer
			final HashSet<ImmutableList<Value<?>>> removedKeys;
			if (this.reteEntityManyToManyLookup != null) {
				this.wrapperRight.remove(valuesMap);
				removedKeys = this.reteEntityManyToManyLookup.removeValue(valuesList);
			} else {
				removedKeys = HashSet.newHashSet(1);
				removedKeys.add(ImmutableList.of());
			}
			// removedKeys are the keys for which is true: (L,R): (1,1) -> (1,0)
			for (Entry<ReteEntity> entry : //
			this.memoizationLeft.getCorresponding(removedKeys).entrySet()) {
				int count = entry.getCount();
				ReteEntity newMatch = entry.getElement();
				// each removed key is a new match for propagation
				ReteEntity re = new ReteEntity(this, Position.LEFTBETA, newMatch, time);
				this.processedEntities.add(re, count);
				this.newAddEntities.add(re, count);
			}
		}
	}
}
