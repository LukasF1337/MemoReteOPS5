package ops5.workingmemory.data;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

/**
 * Track already processed ReteEntities for reuse (and speed). It maintains a
 * 1:n mapping from one ImmutableList&#60;Value> to one or multiple
 * ReteEntity's. This enables each value combination (ImmutableList&#60;Value>)
 * needing to be matched only once, instead of needing to match each ReteEntity.
 */
public class Memoization {
	final private HashMap<ImmutableList<Value<?>>, Multiset<ReteEntity>> memoization;
	final private ImmutableSet<String> atomVariables;

	public Memoization(ImmutableSet<String> atomVariables) {
		memoization = new HashMap<>();
		this.atomVariables = atomVariables;
	}

	/**
	 * 
	 * @param valuesMap
	 * @return number of occurrences after add
	 */
	public int add(ImmutableMap<String, Value<?>> valuesMap, ReteEntity reteEntity) {
		assert (checkSameOrdering(valuesMap, reteEntity));
		final ImmutableList<Value<?>> values = ImmutableList.copyOf(valuesMap.values());
		Multiset<ReteEntity> entry = memoization.get(values);
		if (entry == null) {
			memoization.put(values, HashMultiset.create());
			entry = memoization.get(values);
		}
		entry.add(reteEntity);
		return entry.size();
	}

	/**
	 * 
	 * @return number of occurrences after remove
	 */
	public int remove(ImmutableMap<String, Value<?>> valuesMap, ReteEntity reteEntity) {
		assert (checkSameOrdering(valuesMap, reteEntity));
		ImmutableList<Value<?>> valuesList = ImmutableList.copyOf(valuesMap.values());
		Multiset<ReteEntity> entry = memoization.get(valuesList);
		if (entry != null) {
			entry.remove(reteEntity);
			if (entry.isEmpty()) {
				memoization.remove(valuesList); // to prevent memory leak
			}
		} else {
			throw new IllegalStateException();
		}
		return entry.size();
	}

	/**
	 * 
	 * @param valuesKey
	 * @return modifiable view. Changes will be reflected on original.
	 */
	public Multiset<ReteEntity> get(ImmutableList<Value<?>> valuesKey) {
		return this.memoization.get(valuesKey);
	}

	/**
	 * @param valuesMatches
	 * @return all ReteEntity that correspond to one of the given ValueLists
	 */
	public ImmutableMultiset<ReteEntity> getCorresponding(Set<ImmutableList<Value<?>>> valuesMatches) {
		assert (valuesMatches != null);
		ImmutableMultiset.Builder<ReteEntity> res = ImmutableMultiset.builder();
		for (ImmutableList<Value<?>> valuesMatch : valuesMatches) {
			Multiset<ReteEntity> partRes = this.memoization.get(valuesMatch);
			if (partRes != null) {
				res.addAll(partRes);
			} else {
				// sanity checks:
				Set<ImmutableList<Value<?>>> tmp = new HashSet<>();
				tmp.add(ImmutableList.of()); // [[]]
				if (!(valuesMatch.equals(ImmutableList.of()) && valuesMatches.equals(tmp))) {
					throw new IllegalStateException("There must be corresponding ReteEntity's for each valuelist given."
							+ " Otherwise State is erroneous.");
				}
			}
		}
		return res.build();
	}

	/**
	 * For testing and debugging only.
	 */
	private boolean checkSameOrdering(ImmutableMap<String, Value<?>> valuesMap, ReteEntity reteEntity) {
		ImmutableList.Builder<Value<?>> tmpListBuilder = ImmutableList.builder();
		for (String atomVar : this.atomVariables) {
			tmpListBuilder.add(reteEntity.getValue(atomVar));
		}
		ImmutableList<Value<?>> tmpList = tmpListBuilder.build();
		return tmpList.equals(ImmutableList.copyOf(valuesMap.values()));
	}
}
