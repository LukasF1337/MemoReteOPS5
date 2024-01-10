package ops5.workingmemory.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.UnmodifiableIterator;

import ops5.workingmemory.data.condition.ConditionPredicate;
import ops5.workingmemory.node.NodeBeta;
import ops5.workingmemory.node.NodeBeta.Position;
import utils.MathHelpers;

// TODO: FIXME: correct this. I need correct comparison; some unit tests
/**
 * Wrap ValueList's by a List of specified String atomVar's and Predicates
 * (>,>=,<>,=,...). This allows sorting and hashing based on a values for each
 * atomVar. This helps in lookup for faster matching in NodeBeta's if there are
 * one or more atom variables present, instead of needing to test for each
 * potential match.
 */
public final class ValueListCompare {
	// each String in the following variables is an Atom Variable.
	final ImmutableList<String> atomVarsEqual;
	final ImmutableList<String> atomVarsEnum;
	final ImmutableList<String> atomVarsTree;
	final ImmutableList<String> atomVarsUnequal;
	final ImmutableList<Integer> equalsIndices; // indices for reteEntitiesLookupEqual
	final HashMap<ImmutableList<Value<?>>, HashSet<ImmutableList<Value<?>>>> //
	reteEntitiesLookupEqual; // merged lookup of all "="
	final ImmutableMap<String, EnumMap<ValueType, HashSet<ImmutableList<Value<?>>>>> //
	reteEntitiesLookupEnum; // separate lookups of all enum, equal ValueType
	final ImmutableMap<String, TreeLookup> //
	reteEntitiesLookupTree; // separate lookups for all of ">,>=,<,<="
	final ImmutableMap<String, UnequalLookup> //
	reteEntitiesLookupUnequal; // separate lookups for "<>" which means unequal
	final ImmutableMap<String, ConditionPredicate> atomVarToPredicate;
	final ImmutableMap<String, MapType> mapTypes; // Order is preserved by ImmutableMap.
	final Boolean reverseTree;
	final Position pos;

	private enum MapType {
		EQUAL, ENUM, TREE, UNEQUAL
	}

	/**
	 * 
	 * @param sourceNode         The node that this ReteEntityWrapper resides on,
	 *                           which is always either NodeBetaExistent or
	 *                           NodeBetaNonexistent.
	 * @param atomVarToPredicate
	 * @param pos                side that is represented by this wrapper
	 */
	public ValueListCompare(NodeBeta sourceNode, ImmutableMap<String, ConditionPredicate> atomVarToPredicate,
			Position pos) {
		assert (sourceNode != null);
		assert (sourceNode.getPreviousNodes().size() == 2);

		ImmutableList.Builder<String> atomVarsEqualBuilder = ImmutableList.builder();
		ImmutableList.Builder<String> atomVarsEnumBuilder = ImmutableList.builder();
		ImmutableList.Builder<String> atomVarsTreeBuilder = ImmutableList.builder();
		ImmutableList.Builder<String> atomVarsUnequalBuilder = ImmutableList.builder();
		ImmutableList.Builder<Integer> equalsIndicesBuilder = ImmutableList.builder();
		ImmutableMap.Builder<String, EnumMap<ValueType, HashSet<ImmutableList<Value<?>>>>> //
		reteEntitiesLookupEnumBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<String, TreeLookup> //
		reteEntitiesLookupTreeBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<String, UnequalLookup> //
		reteEntitiesLookupUnequalBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<String, MapType> mapTypeBuilder = //
				ImmutableMap.builder(); // also acts like a LinkedHashmap preserving iteration order

		mapTypeBuilder.put("", MapType.EQUAL);
		int valueListIndex = 0;
		for (Map.Entry<String, ConditionPredicate> entry : atomVarToPredicate.entrySet()) {
			String atomVar = entry.getKey();
			ConditionPredicate predicate = entry.getValue();
			switch (predicate) {
			case EQUAL:
				atomVarsEqualBuilder.add(atomVar);
				equalsIndicesBuilder.add(valueListIndex);
				mapTypeBuilder.put(atomVar, MapType.EQUAL);
				break;
			case SAMETYPE: // enum
				atomVarsEnumBuilder.add(atomVar);
				reteEntitiesLookupEnumBuilder.put(atomVar, new EnumMap<>(ValueType.class));
				mapTypeBuilder.put(atomVar, MapType.ENUM);
				break;
			case BIGGER: // tree
			case BIGGERQUAL: // tree
			case SMALLER: // tree
			case SMALLEREQUAL: // tree
				atomVarsTreeBuilder.add(atomVar);
				reteEntitiesLookupTreeBuilder.put(atomVar, new TreeLookup(valueListIndex));
				mapTypeBuilder.put(atomVar, MapType.TREE);
				break;
			case UNEQUAL:
				atomVarsUnequalBuilder.add(atomVar);
				reteEntitiesLookupUnequalBuilder.put(atomVar, new UnequalLookup(valueListIndex));
				// reteEntitiesLookupUnequalRepairBuilder.put(atomVar, new HashMap<>());
				mapTypeBuilder.put(atomVar, MapType.UNEQUAL);
				break;
			case RETURNTRUE:
			default:
				throw new IllegalStateException(predicate.toString());
			}
			valueListIndex++;
		}

		this.atomVarsEqual = atomVarsEqualBuilder.build();
		this.atomVarsEnum = atomVarsEnumBuilder.build();
		this.atomVarsTree = atomVarsTreeBuilder.build();
		this.atomVarsUnequal = atomVarsUnequalBuilder.build();
		this.equalsIndices = equalsIndicesBuilder.build();
		this.reteEntitiesLookupEqual = new HashMap<>();
		this.reteEntitiesLookupEnum = reteEntitiesLookupEnumBuilder.build();
		this.reteEntitiesLookupTree = reteEntitiesLookupTreeBuilder.build();
		this.reteEntitiesLookupUnequal = reteEntitiesLookupUnequalBuilder.build();
		// this.reteEntitiesLookupUnequalRepair =
		// reteEntitiesLookupUnequalRepairBuilder.build();
		this.mapTypes = mapTypeBuilder.build();
		this.atomVarToPredicate = atomVarToPredicate;
		this.reverseTree = pos.equals(Position.LEFTBETA) ? true : false;
		this.pos = pos;
	}

	private boolean assertSameOrder(ImmutableMap<String, Value<?>> valuesMap) {
		// asserts that the values insiude of valuesMap are in the same order as the
		// values of this.atomVarToPredicate. This is necessary so that the tuple of
		// values are always in the same order for comparison and equality checks.
		UnmodifiableIterator<String> it1 = valuesMap.keySet().iterator();
		UnmodifiableIterator<String> it2 = this.atomVarToPredicate.keySet().iterator();
		while (it1.hasNext() && it2.hasNext()) {
			String s1 = it1.next();
			String s2 = it2.next();
			if (!s1.equals(s2)) {
				return false;
			}
		}
		return !it1.hasNext() && !it2.hasNext();
	}

	/**
	 * 
	 * @param valuesMap a list of values that are to be matched individually. For
	 *                  example (1,3,7) and the predicates (=,>,<=>), meaning
	 *                  (equal, greater than, same type). getMatches() then returns
	 *                  all value tuples that satisfy the three constraints: (=1,
	 *                  >3, same type as 7 meaning Integer Type). This class uses
	 *                  acceleration structures like HashMap, TreeSet, etc. for
	 *                  faster lookup.
	 * @return HashSet of Tuples of Values
	 *         (HashSet&#60;ImmutableList&#60;Value&#60;?>>>) which satisfy all
	 *         constraints. Returns a defensive copy.
	 */
	@SuppressWarnings("unchecked")
	public HashSet<ImmutableList<Value<?>>> getMatches(ImmutableMap<String, Value<?>> valuesMap) {
		assert (assertSameOrder(valuesMap));
		assert (valuesMap.size() > 0);
		for (String atomVar : this.atomVarsTree) {
			if (!Value.isNumber(valuesMap.get(atomVar))) {
				// only numbers can be compared and only numbers will return matches. For
				// example null<7 is not a match, while 0<7 is a match.
				return new HashSet<>();
			}
		}
		Map<String, Object> lookups = new LinkedHashMap<String, Object>();
		if (atomVarsEqual.size() > 0) {
			HashSet<ImmutableList<Value<?>>> equalMatches = getMatchesEqual(valuesMap);
			lookups.put("", equalMatches);
		}
		for (String atomVar : this.atomVarsEnum) {
			HashSet<ImmutableList<Value<?>>> enumMatches = getMatchesEnum(valuesMap, atomVar);
			lookups.put(atomVar, enumMatches);
		}
		for (String atomVar : this.atomVarsTree) {
			TreeLookup treeMatches = getMatchesTree(valuesMap, atomVar);
			lookups.put(atomVar, treeMatches);
		}
		for (String atomVar : this.atomVarsUnequal) { // FIXME
			UnequalLookup unequalMatches = getMatchesUnequal(valuesMap, atomVar);
			lookups.put(atomVar, unequalMatches);
		}

		ArrayList<Map.Entry<String, Object>> lookupsList = new ArrayList<Map.Entry<String, Object>>(lookups.entrySet());

		// sort in ascending order by set.size()
		Collections.sort(lookupsList, mapComparatorAscending);

		HashSet<ImmutableList<Value<?>>> res = null;
		for (Entry<String, Object> e : lookupsList) {
			String atomVar = e.getKey();
			MapType mapType = this.mapTypes.get(atomVar);
			Object collection = e.getValue();
			// res.size() <= collection.size()

			if (res == null) {
				// assign res
				if (collection instanceof UnequalLookup col) { // FIXME
					res = col.getValuesCopy(); // create a defensive copy
				} else if (collection instanceof TreeLookup col) {
					// make a defensive copy, because it will likely be modified later (loop or
					// return)
					res = col.getValuesCopy();
				} else if (collection instanceof HashSet<?> col) {
					// make a defensive copy, because it will likely be modified later (loop or
					// return)
					res = new HashSet<>((HashSet<ImmutableList<Value<?>>>) col);
				} else {
					throw new IllegalStateException();
				}
				continue;
			}

			switch (mapType) {
			case EQUAL:
			case ENUM:
				assert (collection instanceof HashSet<?>);
				final HashSet<ImmutableList<Value<?>>> sett = (HashSet<ImmutableList<Value<?>>>) collection;
				// sett.contains() is O(1)
				res.retainAll(sett); // O(res.size())
				break;
			case TREE:
				assert (collection instanceof TreeLookup);
				TreeLookup set = (TreeLookup) collection;
				final int resSize = res.size(); // O(1)
				final int setKeysSize = set.uniqueKeysSize(); // O( log2(set.uniqueKeysSize()) )
				final int setSize = set.size(); // O(1)
				final int log2SetKeysSize = MathHelpers.binlog(setKeysSize); // will only be 0, if resSize is also 0
				// Choose the cheaper operation
				// https://stackoverflow.com/questions/14379515/computational-complexity-of-treeset-methods-in-java
				if (resSize * log2SetKeysSize < resSize + setSize) {
					// manual comparison is faster
					// O( res.size() * log2(set.uniqueKeysSize()) )
					// SortedSet.contains() and then HashSet.contains()
					res.removeIf(resEntry -> !set.contains(resEntry));
				} else {
					// HashSet comparison is faster
					// O( res.size() + set.uniqueKeysSize() )
					// below Sets.retainAll() will use HashSet.contains()
					res.retainAll(set.getValuesCopy());
				}
				break;
			case UNEQUAL:
				// O(res.size())
				assert (collection instanceof UnequalLookup);
				UnequalLookup unequal = (UnequalLookup) collection;
				// retain all that are unequal:
				res.removeIf(resEntry -> !unequal.contains(resEntry));
				break;
			default:
				throw new IllegalStateException();
			}
		}
		// repair all LookupUnequal:
		this.atomVarsUnequal.forEach(atomVar -> this.reteEntitiesLookupUnequal.get(atomVar).repair());
		// repair all LookupTree:
		this.atomVarsTree.forEach(atomVar -> this.reteEntitiesLookupTree.get(atomVar).repair());
		if (res == null) {
			throw new IllegalStateException();
		}
		return ((HashSet<ImmutableList<Value<?>>>) res); // returns a defensive copy
	}

	private HashSet<ImmutableList<Value<?>>> getMatchesEqual(ImmutableMap<String, Value<?>> valuesMap) {
		// O(1)
		ImmutableList.Builder<Value<?>> valuesBuilder = new ImmutableList.Builder<>();
		for (String atomVar : this.atomVarsEqual) {
			valuesBuilder.add(valuesMap.get(atomVar));
		}
		ImmutableList<Value<?>> values = valuesBuilder.build();
		HashSet<ImmutableList<Value<?>>> match = this.reteEntitiesLookupEqual.get(values);
		if (match == null) {
			match = HashSet.newHashSet(0); // empty HashSet, because no matches
		}
		return match;
	}

	private HashSet<ImmutableList<Value<?>>> getMatchesEnum(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1)
		final Map<ValueType, HashSet<ImmutableList<Value<?>>>> enumMap = this.reteEntitiesLookupEnum.get(atomVar);
		final ValueType type = valuesMap.get(atomVar).attributeType();
		return enumMap.get(type);
	}

	private TreeLookup getMatchesTree(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1), but calling res.size() will be O(log2(treesize))
		assert (Value.isNumber(valuesMap.get(atomVar))); // only numbers allowed, otherwise should be dealt with earlier
															// in code.
		final ImmutableList<Value<?>> values = ImmutableList.copyOf(valuesMap.values());
		ConditionPredicate predicate = this.atomVarToPredicate.get(atomVar);
		final TreeLookup treeSet = this.reteEntitiesLookupTree.get(atomVar);
		// SortedSet<Value<?>> keys = (SortedSet<Value<?>>) treeSet.keySet();
		final SortedSet<ImmutableList<Value<?>>> res;
		if (reverseTree) {
			switch (predicate) {
			case BIGGER: // ">"
				predicate = ConditionPredicate.SMALLER;
				break;
			case BIGGERQUAL: // ">="
				predicate = ConditionPredicate.SMALLEREQUAL;
				break;
			case SMALLER: // "<"
				predicate = ConditionPredicate.BIGGER;
				break;
			case SMALLEREQUAL: // "<="
				predicate = ConditionPredicate.BIGGERQUAL;
				break;
			default:
				throw new IllegalStateException();
			}
		}
		switch (predicate) {
		case BIGGER: // ">"
			treeSet.tailSet(values, false);
			// FIXME TODO mask key on BIGGER and SMALLER.
			break;
		case BIGGERQUAL: // ">="
			treeSet.tailSet(values, true);
			break;
		case SMALLER: // "<"
			treeSet.headSet(values, false);
			break;
		case SMALLEREQUAL: // "<="
			treeSet.headSet(values, true);
			break;
		default:
			throw new IllegalStateException();
		}
		return treeSet;
	}

	/**
	 * Value -> ValueList lookup for each unequal.
	 */
	private class UnequalLookup {
		private final HashMap<Value<?>, HashSet<ImmutableList<Value<?>>>> lookupUnequal = new HashMap<>();
		private Integer size = 0; // track size
		private final Integer valueListIndex; // position of the relevant Value in the ValueList
		private boolean masked = false;
		private Value<?> repairValue = null; // temporal holder of repair Value associated with below repairData,
		private HashSet<ImmutableList<Value<?>>> repairData = null; // temporal holder of repair data, populated once
																	// for each

		public UnequalLookup(Integer valueListIndex) {
			this.valueListIndex = valueListIndex;
		}

		public boolean contains(ImmutableList<Value<?>> valueList) {
			HashSet<ImmutableList<Value<?>>> subset = this.lookupUnequal.get(valueList.get(valueListIndex));
			if (subset == null) {
				return false;
			} else {
				return subset.contains(valueList);
			}
		}

		public void add(ImmutableList<Value<?>> valueList) {
			// O(1)
			this.size++;
			assert (valueList != null);
			final Value<?> val = valueList.get(valueListIndex);
			HashSet<ImmutableList<Value<?>>> tmpSet = lookupUnequal.get(val);
			if (tmpSet == null) {
				lookupUnequal.put(val, HashSet.newHashSet(0));
				tmpSet = lookupUnequal.get(val);
			}
			tmpSet.add(valueList);
		}

		public void remove(ImmutableList<Value<?>> valueList) {
			// O(1)
			this.size--;
			final Value<?> val = valueList.get(valueListIndex);
			final HashSet<ImmutableList<Value<?>>> subset = lookupUnequal.get(val);
			final boolean success = subset.remove(valueList);
			if (subset.size() == 0) {
				lookupUnequal.remove(val);
			}
			if (!success) {
				throw new IllegalStateException();
			}
		}

		/**
		 * Mask specific Value val from the internal Map. After that the unequal lookup
		 * can be done. Last a call to repair() is necessary. Repeat.
		 * 
		 */
		public void maskValue(Value<?> val) {
			if (this.masked) {
				throw new IllegalStateException();
			}
			this.masked = true;
			this.repairValue = val;
			this.repairData = this.lookupUnequal.remove(val); // null allowed
			this.size -= this.repairData != null ? this.repairData.size() : 0;
		}

		public void repair() {
			assert (this.masked);
			this.size += this.repairData != null ? this.repairData.size() : 0;
			if (repairData != null) {
				this.lookupUnequal.put(repairValue, repairData);
			}
			this.repairValue = null;
			this.repairData = null;
			this.masked = false;
		}

		/**
		 * 
		 * @return defensive copy of all ImmutableList&#60;Value&#60;?>> ValueList's
		 */
		public HashSet<ImmutableList<Value<?>>> getValuesCopy() {
			// O(lookupUnequal.size())
			final Set<Entry<Value<?>, HashSet<ImmutableList<Value<?>>>>> entrySet = this.lookupUnequal.entrySet();
			final HashSet<ImmutableList<Value<?>>> res = HashSet.newHashSet(entrySet.size());
			for (Entry<Value<?>, HashSet<ImmutableList<Value<?>>>> e : entrySet) {
				HashSet<ImmutableList<Value<?>>> part = e.getValue();
				res.addAll(part);
			}
			return res;
		}

		public Integer size() {
			return size;
		}
	}

	/**
	 * Value -> ValueList lookup for each <,<=,>,>=.
	 */
	private class TreeLookup {
		private SetMultimap<Value<?>, ImmutableList<Value<?>>> lookupTree = //
				MultimapBuilder.treeKeys().hashSetValues().build();
		private final Integer valueListIndex; // position of the relevant Value in the ValueList
		private boolean masked = false;
		private Value<?> repairValue = null; // temporal holder of repair Value associated with below repairData,
		private Set<ImmutableList<Value<?>>> repairData = null; // temporal holder of repair data, populated once
																// for each
		private SortedMap<Value<?>, Set<ImmutableList<Value<?>>>> result = null;

		public TreeLookup(Integer valueListIndex) {
			this.valueListIndex = valueListIndex;
		}

		public void headSet(ImmutableList<Value<?>> values, boolean inclusive) {
			Value<?> val = values.get(valueListIndex);
			this.maskValue(val, inclusive);
			lookupTree.get(val);
			result = ((SortedMap) this.lookupTree.asMap()).headMap(val);
		}

		public void tailSet(ImmutableList<Value<?>> values, boolean inclusive) {
			Value<?> val = values.get(valueListIndex);
			this.maskValue(val, inclusive);
			result = ((SortedMap) this.lookupTree.asMap()).tailMap(val);
		}

		public boolean contains(ImmutableList<Value<?>> valueList) {
			Set<ImmutableList<Value<?>>> subset = result.get(valueList.get(valueListIndex));
			if (subset == null) {
				return false;
			} else {
				return subset.contains(valueList);
			}
		}

		public void add(ImmutableList<Value<?>> valueList) {
			assert (valueList != null && !this.masked);
			final Value<?> val = valueList.get(valueListIndex);
			final boolean correct = lookupTree.put(val, valueList);
			if (!correct) {
				throw new IllegalStateException();
			}
		}

		public void remove(ImmutableList<Value<?>> valueList) {
			assert (valueList != null && !this.masked);
			final Value<?> val = valueList.get(valueListIndex);
			final boolean correct = lookupTree.remove(val, valueList);
			if (!correct) {
				throw new IllegalStateException();
			}
		}

		/**
		 * Mask specific Value val from the internal Map. After that the unequal lookup
		 * can be done. Last a call to repair() is necessary. Repeat.
		 * 
		 * @param inclusive
		 * 
		 */
		private void maskValue(Value<?> val, boolean inclusive) {
			if (this.masked) {
				throw new IllegalStateException();
			}
			this.masked = true;
			this.repairValue = val;
			if (inclusive) {
				this.repairData = null;
			} else {
				this.repairData = (Set<ImmutableList<Value<?>>>) this.lookupTree.removeAll(val); // null allowed
			}
		}

		public void repair() {
			assert (this.masked);
			if (this.repairData != null) {
				this.lookupTree.putAll(this.repairValue, this.repairData);
			}
			this.result = null;
			this.repairValue = null;
			this.repairData = null;
			this.masked = false;
		}

		/**
		 * 
		 * @return defensive copy of all ImmutableList&#60;Value&#60;?>> ValueList's
		 */
		public HashSet<ImmutableList<Value<?>>> getValuesCopy() {
			// O(lookupTree.size())
			assert(result != null);
			final Collection<Set<ImmutableList<Value<?>>>> values = this.result.values();
			
			final HashSet<ImmutableList<Value<?>>> res = HashSet.newHashSet(values.size());
			result.values().forEach(hashset -> res.addAll(hashset));
			return res;
		}

		public int uniqueKeysSize() {
			return this.lookupTree.keySet().size();
		}

		public int size() {
			return this.lookupTree.size();
		}
	}

	private UnequalLookup getMatchesUnequal(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1)
		// this method does lookupUnequal.maskValue() and therefore necessitates a later
		// repair by lookupUnequal.repair() after the Lookup.
		Value<?> value = valuesMap.get(atomVar);
		UnequalLookup lookupUnequal = this.reteEntitiesLookupUnequal.get(atomVar);
		lookupUnequal.maskValue(value);
		return lookupUnequal;
	}

	public void addAll(ArrayList<ImmutableMap<String, Value<?>>> valuesMapList) {
		for (ImmutableMap<String, Value<?>> valuesMap : valuesMapList) {
			this.add(valuesMap);
		}
	}

	public void add(ImmutableMap<String, Value<?>> valuesMap) {
		assert (assertSameOrder(valuesMap));
		if (this.atomVarsEqual.size() > 0) {
			addEqualEntity(valuesMap);
		}
		for (String atomVar : this.atomVarsEnum) {
			addEnumEnitity(valuesMap, atomVar);
		}
		for (String atomVar : this.atomVarsTree) {
			addTreeEntity(valuesMap, atomVar);
		}
		for (String atomVar : this.atomVarsUnequal) {
			addUnequalEntity(valuesMap, atomVar);
		}
	}

	private void addEqualEntity(ImmutableMap<String, Value<?>> valuesMap) {
		// O(1)
		assert (this.atomVarsEqual.size() > 0);
		ImmutableList.Builder<Value<?>> valuesBuilder = new ImmutableList.Builder<>();
		for (String atomVar : this.atomVarsEqual) {
			valuesBuilder.add(valuesMap.get(atomVar));
		}
		ImmutableList<Value<?>> values = valuesBuilder.build();
		HashSet<ImmutableList<Value<?>>> valsFromKey = this.reteEntitiesLookupEqual.get(values);
		if (valsFromKey == null) {
			valsFromKey = HashSet.newHashSet(1);
			reteEntitiesLookupEqual.put(values, valsFromKey);
		}
		boolean checkTrue = valsFromKey.add(ImmutableList.copyOf(valuesMap.values()));
		assert (checkTrue); // only unique adds allowed
	}

	private void addEnumEnitity(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1)
		final Value<?> value = valuesMap.get(atomVar);
		final ValueType type = value.attributeType();
		final EnumMap<ValueType, HashSet<ImmutableList<Value<?>>>> enumMap = //
				this.reteEntitiesLookupEnum.get(atomVar);
		if (!enumMap.containsKey(type)) {
			enumMap.put(type, HashSet.newHashSet(1));
		}
		enumMap.get(type).add(ImmutableList.copyOf(valuesMap.values()));
	}

	private void addTreeEntity(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(log2(fullTreeSize))
		if (!Value.isNumber(valuesMap.get(atomVar))) {
			return; // only numbers can be compared and only numbers will return matches. For
					// example null<7 is not a match, while 0<7 is a match.
		}
		final TreeLookup treeSet = this.reteEntitiesLookupTree.get(atomVar);
		treeSet.add(ImmutableList.copyOf(valuesMap.values()));
	}

	private void addUnequalEntity(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1)
		final UnequalLookup unequalLookup = this.reteEntitiesLookupUnequal.get(atomVar);
		unequalLookup.add(ImmutableList.copyOf(valuesMap.values()));
	}

	public void removeAll(Collection<ImmutableMap<String, Value<?>>> valuesMapList) {
		for (ImmutableMap<String, Value<?>> values : valuesMapList) {
			this.remove(values);
		}
	}

	public void remove(ImmutableMap<String, Value<?>> valuesMap) {
		assert (assertSameOrder(valuesMap));
		if (this.atomVarsEqual.size() > 0) {
			removeEqualEntity(valuesMap);
		}
		for (String atomVar : this.atomVarsEnum) {
			removeEnumEntity(valuesMap, atomVar);
		}
		for (String atomVar : this.atomVarsTree) {
			removeTreeEntity(valuesMap, atomVar);
		}
		for (String atomVar : this.atomVarsUnequal) {
			removeUnequalEntity(valuesMap, atomVar);
		}
	}

	private void removeEqualEntity(ImmutableMap<String, Value<?>> valuesMap) {
		// O(1)
		assert (this.atomVarsEqual.size() > 0);
		ImmutableList.Builder<Value<?>> valuesBuilder = new ImmutableList.Builder<>();
		for (String atomVar : this.atomVarsEqual) {
			valuesBuilder.add(valuesMap.get(atomVar));
		}
		ImmutableList<Value<?>> values = valuesBuilder.build();
		final boolean res = this.reteEntitiesLookupEqual.get(values).remove(ImmutableList.copyOf(valuesMap.values()));
		if (!res) {
			throw new IllegalStateException();
		}
		if (this.reteEntitiesLookupEqual.get(values).size() == 0) {
			this.reteEntitiesLookupEqual.remove(values);
		}
	}

	private void removeEnumEntity(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1)
		final EnumMap<ValueType, HashSet<ImmutableList<Value<?>>>> enumMap = this.reteEntitiesLookupEnum.get(atomVar);
		final Value<?> value = valuesMap.get(atomVar);
		final ValueType type = value.attributeType();
		final boolean res = enumMap.get(type).remove(ImmutableList.copyOf(valuesMap.values()));
		if (!res) {
			throw new IllegalStateException();
		}
	}

	private void removeTreeEntity(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(log2(fullTreeSize))
		if (!Value.isNumber(valuesMap.get(atomVar))) {
			return; // only numbers can be compared and only numbers will return matches. For
					// example null<7 is not a match, while 0<7 is a match.
		}
		final TreeLookup treeSet = this.reteEntitiesLookupTree.get(atomVar);
		treeSet.remove(ImmutableList.copyOf(valuesMap.values()));
	}

	private void removeUnequalEntity(ImmutableMap<String, Value<?>> valuesMap, String atomVar) {
		// O(1)
		UnequalLookup unequalLookup = this.reteEntitiesLookupUnequal.get(atomVar);
		unequalLookup.remove(ImmutableList.copyOf(valuesMap.values()));
	}

//	private static Comparator<ImmutableList<Value<?>>> makeComparator(final int valueListIndex) {
//		return new Comparator<ImmutableList<Value<?>>>() {
//			@Override
//			public int compare(ImmutableList<Value<?>> o1, ImmutableList<Value<?>> o2) {
//				// order the ValueList only according to specific Values residing on position
//				// valueListIndex
//				Value<?> v1 = o1.get(valueListIndex);
//				Value<?> v2 = o2.get(valueListIndex);
//				int res = v1.compareTo(v2);
//				if (res == 0) {
//					if (o1.equals(o2)) {
//						res = 0;
//					} else {
//						// fix for three times the value 50, this can err, because it doesnt find the
//						// last 50. This creates a total order. FIXME wrong, cant compare Strings for
//						// example!!
//						int numValues = o1.size();
//						for (int i = 0; i < numValues; i++) {
//							if (i == valueListIndex) {
//								continue;
//							}
//							v1 = o1.get(valueListIndex);
//							v2 = o2.get(valueListIndex);
//							res = v1.compareTo(v2);
//							if (res != 0) {
//								break;
//							}
//						}
//						if (res == 0) {
//							res = System.identityHashCode(o1) - System.identityHashCode(o2);
//						}
//					}
//				}
//				return res;
//			}
//		};
//	}

	private final Comparator<Map.Entry<String, Object>> mapComparatorAscending = new Comparator<Map.Entry<String, Object>>() {
		@Override
		public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
			final int size1, size2;
			final Object obj1 = o1.getValue();
			final Object obj2 = o2.getValue();
//			Types possible:
//			TreeLookup
//			HashSet<ImmutableList<Value<?>>> 
//			UnequalLookup

			if (obj1 instanceof TreeLookup object1) {
				size1 = object1.size();
			} else if (obj1 instanceof HashSet<?> object1) {
				size1 = object1.size();
			} else if (obj1 instanceof UnequalLookup object1) {
				size1 = object1.size();
			} else {
				throw new IllegalStateException();
			}
			if (obj2 instanceof TreeLookup object2) {
				size2 = object2.size();
			} else if (obj2 instanceof HashSet<?> object2) {
				size2 = object2.size();
			} else if (obj1 instanceof UnequalLookup object2) {
				size2 = object2.size();
			} else {
				throw new IllegalStateException();
			}
			// TODO? Weigh different sets differently?
			return size1 - size2;
		}
	};
}
