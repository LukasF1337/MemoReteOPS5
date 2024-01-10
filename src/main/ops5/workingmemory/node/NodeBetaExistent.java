package ops5.workingmemory.node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import ops5.ModelRete;
import ops5.workingmemory.data.Memoization;
import ops5.workingmemory.data.ReteEntity;
import ops5.workingmemory.data.ValueListCompare;
import ops5.workingmemory.data.Value;
import ops5.workingmemory.data.condition.ConditionPredicate;
import utils.SpecialBiMultiMap;

public final class NodeBetaExistent extends NodeBeta {

	final ValueListCompare wrapperLeft; // fast lookups of ImmutableList<Value<?>>
	final ValueListCompare wrapperRight; // fast lookups of ImmutableList<Value<?>>
	final HashMap<ReteEntity, ReteEntity> keyToProcessedReteEntity; // <key (right side), processedReteEntity>
	final Memoization memoizationLeft; // track ImmutableList<Value<?>> to ReteEntity's associations
	final Memoization memoizationRight; // track ImmutableList<Value<?>> to ReteEntity's associations
	// Remember all matches of ReteEntities.
	final SpecialBiMultiMap<ImmutableList<Value<?>>, ImmutableList<Value<?>>> reteEntityManyToManyLookup;

	public NodeBetaExistent(Optional<ModelRete> modelRete, Optional<NodeBeta> previousLeftBetaNode,
			Optional<NodeAlpha> previousRightAlphaNode) {
		super(modelRete, previousLeftBetaNode, previousRightAlphaNode);

		if (this.previousLeftBetaNode == null) {
			// 1 prev node
			this.wrapperLeft = null;
			this.wrapperRight = null;
			this.reteEntityManyToManyLookup = null;
			this.keyToProcessedReteEntity = new HashMap<>();
			this.memoizationLeft = null;
			this.memoizationRight = null;
		} else {
			// 2 prev nodes
			ImmutableMap<String, ConditionPredicate> atomVarToPredicateForWrapper = constructAtomVarToPredicate();
			if (this.atomVarsIntersection.size() > 0) {
				this.reteEntityManyToManyLookup = new SpecialBiMultiMap<>();
				this.wrapperLeft = new ValueListCompare(this, atomVarToPredicateForWrapper, Position.LEFTBETA);
				this.wrapperRight = new ValueListCompare(this, atomVarToPredicateForWrapper, Position.RIGHTALPHA);
			} else {
				this.reteEntityManyToManyLookup = null;
				this.wrapperLeft = null;
				this.wrapperRight = null;
			}
			this.keyToProcessedReteEntity = null;
			this.memoizationLeft = new Memoization(this.atomVarsIntersection);
			this.memoizationRight = new Memoization(this.atomVarsIntersection);
		}
	}

	@Override
	public void propagateAddedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			this.propagateAddedEntity(reteEntity, from, time);
		}
	}

	@Override
	public void propagateAddedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		assert (reteEntity != null && from != null);
		if (this.previousLeftBetaNode == null) {
			// 1 prev node
			ReteEntity reteEntityNew = new ReteEntity(this, Position.RIGHTALPHA, reteEntity, time);
			final ReteEntity tmp = this.keyToProcessedReteEntity.put(reteEntity, reteEntityNew);
			assert (tmp == null || tmp.equals(reteEntityNew));
			this.processedEntities.add(reteEntityNew);
			this.newAddEntities.add(reteEntityNew);
		} else {
			// 2 prev nodes
			final ImmutableMap.Builder<String, Value<?>> valuesMapBuilder = ImmutableMap.builder();
			for (String atomVar : this.atomVarsIntersection) {
				Value<?> val = reteEntity.getValue(atomVar);
				valuesMapBuilder.put(atomVar, val);
			}
			final ImmutableMap<String, Value<?>> valuesMap = valuesMapBuilder.build();
			final ImmutableList<Value<?>> valuesList = ImmutableList.copyOf(valuesMap.values());
			final Position pos;
			final int occurrences;
			if (from.equals(this.previousLeftBetaNode)) {
				pos = Position.LEFTBETA;
				occurrences = this.memoizationLeft.add(valuesMap, reteEntity);
			} else if (from.equals(this.previousRightAlphaNode)) {
				pos = Position.RIGHTALPHA;
				occurrences = this.memoizationRight.add(valuesMap, reteEntity);
			} else {
				throw new IllegalStateException();
			}
			final Set<ImmutableList<Value<?>>> valuesMatches;
			if (this.atomVarsIntersection.size() > 0) {
				if (occurrences == 1) {
					// new unique value combination, complex lookup needed
					if (pos == Position.LEFTBETA) {
						valuesMatches = this.wrapperRight.getMatches(valuesMap);
						this.wrapperLeft.add(valuesMap);
						this.reteEntityManyToManyLookup.putAll(valuesList, valuesMatches);
					} else { // pos == Position.RIGHTALPHA
						valuesMatches = this.wrapperLeft.getMatches(valuesMap);
						this.wrapperRight.add(valuesMap);
						this.reteEntityManyToManyLookup.putAll(valuesMatches, valuesList);
					}
				} else {
					assert (occurrences > 1);
					if (pos == Position.LEFTBETA) {
						valuesMatches = this.reteEntityManyToManyLookup.getValues(valuesList);
					} else { // pos == Position.RIGHTALPHA
						valuesMatches = this.reteEntityManyToManyLookup.getKeys(valuesList);
					}
				}
			} else { // this.atomVarsIntersection.size() == 0
				// just match with all possible by giving one empty ValueList:
				valuesMatches = HashSet.newHashSet(1);
				valuesMatches.add(ImmutableList.of());
			}
			// valuesMatches must have been successfully assigned by now
			assert (valuesMatches != null);
			final ImmutableMultiset<ReteEntity> actualMatches;
			if (pos == Position.LEFTBETA) {
				actualMatches = this.memoizationRight.getCorresponding(valuesMatches);
			} else { // pos == Position.RIGHTALPHA
				actualMatches = this.memoizationLeft.getCorresponding(valuesMatches);
			}

			for (Multiset.Entry<ReteEntity> e : actualMatches.entrySet()) {
				final ReteEntity actualMatch = e.getElement();
				final int count = e.getCount();
				final ReteEntity newReteEntity;
				if (pos == Position.LEFTBETA) {
					newReteEntity = new ReteEntity(this, reteEntity, actualMatch);
				} else { // pos == Position.RIGHTALPHA
					newReteEntity = new ReteEntity(this, actualMatch, reteEntity);
				}
				this.processedEntities.add(newReteEntity, count);
				this.newAddEntities.add(newReteEntity, count);
			}
		}
	}

	@Override
	public void propagateRemovedEntities(ImmutableMultiset<ReteEntity> reteEntities, Node from, BigInteger time) {
		for (ReteEntity reteEntity : reteEntities) {
			this.propageteRemovedEntity(reteEntity, from, time);
		}
	}

	@Override
	public void propageteRemovedEntity(ReteEntity reteEntity, Node from, BigInteger time) {
		assert (reteEntity != null && from != null);
		if (this.previousLeftBetaNode == null) {
			// 1 prev node
			ReteEntity reteEntityOld = this.keyToProcessedReteEntity.get(reteEntity);
			assert (reteEntityOld != null);
			this.newRemoveEntities.add(reteEntityOld);
			final int occurences = this.processedEntities.remove(reteEntityOld, 1) - 1;
			if (occurences == 0) {
				ReteEntity ret = this.keyToProcessedReteEntity.remove(reteEntity);
				assert (ret != null);
			}
		} else {
			// 2 prev nodes
			final ImmutableMap.Builder<String, Value<?>> valuesMapBuilder = ImmutableMap.builder();
			for (String atomVar : this.atomVarsIntersection) {
				Value<?> val = reteEntity.getValue(atomVar);
				valuesMapBuilder.put(atomVar, val);
			}
			final ImmutableMap<String, Value<?>> valuesMap = valuesMapBuilder.build();
			final ImmutableList<Value<?>> valuesList = ImmutableList.copyOf(valuesMap.values());

			final Position pos;
			if (from.equals(this.previousLeftBetaNode)) {
				pos = Position.LEFTBETA;
			} else if (from.equals(this.previousRightAlphaNode)) {
				pos = Position.RIGHTALPHA;
			} else {
				throw new IllegalStateException();
			}

			// get valuesMatches, Set of ValueList that match
			final HashSet<ImmutableList<Value<?>>> valuesMatches;
			if (reteEntityManyToManyLookup != null) {
				if (pos == Position.LEFTBETA) {
					// defensive copy needed for correctness
					valuesMatches = this.reteEntityManyToManyLookup.getValues(valuesList);
				} else { // pos == Position.RIGHTALPHA
					// defensive copy needed for correctness
					valuesMatches = this.reteEntityManyToManyLookup.getKeys(valuesList);
				}
			} else {
				// matches with all:
				valuesMatches = HashSet.newHashSet(1); // avoid resize()
				valuesMatches.add(ImmutableList.of());
			}
			assert (valuesMatches != null);

			// maintain lookup structures:
			if (pos == Position.LEFTBETA) {
				final int numOfSameValue = this.memoizationLeft.remove(valuesMap, reteEntity);
				if (numOfSameValue == 0) {
					if (reteEntityManyToManyLookup != null) {
						this.wrapperLeft.remove(valuesMap);
						this.reteEntityManyToManyLookup.removeKey(valuesList);
					}
				}
			} else { // pos == Position.RIGHTALPHA
				final int numOfSameValue = this.memoizationRight.remove(valuesMap, reteEntity);
				if (numOfSameValue == 0) {
					if (reteEntityManyToManyLookup != null) {
						this.wrapperRight.remove(valuesMap);
						this.reteEntityManyToManyLookup.removeValue(valuesList);
					}
				}
			}

			final ImmutableMultiset<ReteEntity> actualMatches;
			if (pos == Position.LEFTBETA) {
				actualMatches = this.memoizationRight.getCorresponding(valuesMatches);
			} else { // pos == Position.RIGHTALPHA
				actualMatches = this.memoizationLeft.getCorresponding(valuesMatches);
			}

			for (Multiset.Entry<ReteEntity> e : actualMatches.entrySet()) {
				final ReteEntity actualMatch = e.getElement();
				final int occurences = e.getCount();
				final ReteEntity newReteEntity;
				if (pos == Position.LEFTBETA) {
					newReteEntity = new ReteEntity(this, reteEntity, actualMatch);
				} else { // pos == Position.RIGHTALPHA
					newReteEntity = new ReteEntity(this, actualMatch, reteEntity);
				}
				final int res = this.processedEntities.remove(newReteEntity, occurences);
				this.newRemoveEntities.add(newReteEntity, occurences);
				assert (res >= occurences);
			}
		}
	}
}
