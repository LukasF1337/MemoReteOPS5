package ops5.workingmemory.data;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import ops5.Strategy;

/**
 * Conflict set for ordered rule firing. It implements retirement of fired rules
 * and differences between LEX/MEA rule firing order. This class is needed,
 * because a normal TreeMultiset&#60;ReteEntityWrapper> can not remove similar
 * ReteEntityWrapper with only changed creation times (because those are used
 * for ordering). This class solves that shortcoming (using the variable
 * conflictSetTracker).
 */
public class ConflictSet {

	private Strategy strategy = Strategy.LEX; // default rule firing strategy in OPS 5 is LEX
	// ReteEntity's that could fire rules in this recognize-act cycle
	private TreeMultiset<ReteEntityWrapper> conflictSet;
	// enable remove from conflictSet even if creation times of ReteEntity are
	// different. Each of its treeSetValue must maintain the same order (LEX or MEA)
	// as in conflictSet.
	private HashMap<ReteEntityWrapper, TreeMultiset<ReteEntityWrapper>> conflictSetTracker;
	// ReteEntity's that already fired
	private final HashMultiset<ReteEntityWrapper> conflictSetFired = HashMultiset.create();

	public ConflictSet() {
		conflictSet = TreeMultiset.create(Strategy.makeComparator(strategy));
		conflictSetTracker = new HashMap<>();
	}

	public void changeStrategy(Strategy strategy) {
		this.strategy = strategy;
		if (strategy == null) {
			throw new IllegalStateException("Strategy for rule firing order required");
		}
		if (strategy.equals(this.strategy)) {
			return;
		}

		TreeMultiset<ReteEntityWrapper> conflictSetBuilder = //
				TreeMultiset.create(Strategy.makeComparator(strategy));
		conflictSetBuilder.addAll(conflictSet);
		this.conflictSet = conflictSetBuilder;

		for (Entry<ReteEntityWrapper, TreeMultiset<ReteEntityWrapper>> e : this.conflictSetTracker.entrySet()) {
			TreeMultiset<ReteEntityWrapper> newSet = TreeMultiset.create(Strategy.makeComparator(strategy));
			newSet.addAll(e.getValue());
			e.setValue(newSet);
		}

		// conflictSetFired remains the same
	}

	public void addToConflictSet(ReteEntityWrapper reteEntityWrapper) {
		this.conflictSet.add(reteEntityWrapper);
		if (!this.conflictSetTracker.containsKey(reteEntityWrapper)) {
			TreeMultiset<ReteEntityWrapper> newSet = TreeMultiset.create(Strategy.makeComparator(strategy));
			this.conflictSetTracker.put(reteEntityWrapper, newSet);
		}
		boolean correct = this.conflictSetTracker.get(reteEntityWrapper).add(reteEntityWrapper);
		if (!correct) {
			throw new IllegalStateException("conflictSetTracker implementation error.");
		}
	}

	public void removeFromConflictSet(ReteEntityWrapper reteEntityWrapper) {
		// clear out fired matches first
		if (!this.conflictSetFired.remove(reteEntityWrapper)) {
			// obtain true actualReteEntityWrapper from the conflictSetTracker
			if (this.conflictSetTracker.containsKey(reteEntityWrapper)) {
				ReteEntityWrapper actualReteEntityWrapper = removeFromConflictSetTracker(reteEntityWrapper);
				// remove true actualReteEntityWrapper instead of reteEntityWrapper which has
				// wrong creation times
				if (!this.conflictSet.remove(actualReteEntityWrapper)) {
					throw new IllegalStateException("Could not remove nonexistent, implementation error:" + //
							actualReteEntityWrapper.toString());
				}
			} else {
				throw new IllegalStateException("conflictSetTracker maintained incorrectly");
			}
		}
	}

	/**
	 * 
	 * @param reteEntityWrapper
	 * @return actualReteEntityWrapper, which is really stored in conflictSet
	 *         because it has the same creation times
	 */
	private ReteEntityWrapper removeFromConflictSetTracker(ReteEntityWrapper reteEntityWrapper) {
		TreeMultiset<ReteEntityWrapper> chosenSet = conflictSetTracker.get(reteEntityWrapper);
		Multiset.Entry<ReteEntityWrapper> entry = chosenSet.firstEntry();
		ReteEntityWrapper actualReteEntityWrapper = entry.getElement();
		chosenSet.remove(actualReteEntityWrapper);
		if (chosenSet.size() == 0) {
			conflictSetTracker.remove(reteEntityWrapper);
		}
		return actualReteEntityWrapper;
	}

	/**
	 * @return false if halt was called or conflict set is empty. Otherwise return
	 *         true and fire match.
	 */
	public boolean fireMatch(BigInteger time) throws Exception {
		final Boolean res;
		if (this.conflictSet.size() > 0) {
			ReteEntityWrapper match = this.conflictSet.firstEntry().getElement();
			res = match.fire(time);
			this.conflictSet.remove(match);
			ReteEntityWrapper actualReteEntityWrapper = removeFromConflictSetTracker(match);
			this.conflictSetFired.add(actualReteEntityWrapper);
		} else {
			res = false;
		}
		return res;
	}

	@Override
	public String toString() {
		final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation()
				.disableHtmlEscaping().create();
		StringBuilder strBuilder = new StringBuilder();
		Integer i = 0;
		for (ReteEntityWrapper conflict : this.conflictSet) {
			strBuilder.append(i.toString() + ". " + conflict.toString() + "\n");
			i++;
		}
		return strBuilder.toString();
	}
}
