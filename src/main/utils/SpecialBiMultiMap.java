package utils;
// BiMultiMap is copied from Anver Sadhat https://stackoverflow.com/a/39846050 and modified by Lukas Frank

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.MultimapBuilder;

/*
 * Map representing a many to many relationship. It can be queried either by key or by value.
 */
public final class SpecialBiMultiMap<K, V> {

	private final SetMultimap<K, V> keyToValues = MultimapBuilder.hashKeys().hashSetValues().build();
	private final SetMultimap<V, K> valueToKeys = MultimapBuilder.hashKeys().hashSetValues().build();

	public void put(K key, V value) {
		assert (checkConsistency());
		Collection<V> oldValue = keyToValues.get(key);
		if (oldValue.contains(value) == false) {
			keyToValues.put(key, value);
			valueToKeys.put(value, key);
		}
		assert (checkConsistency());
	}

	/**
	 * 
	 * @param keys  keys that are already digested
	 * @param value new value
	 * @return all keys that now have at least one value, but had 0 before.
	 */
	public HashSet<K> putAllAndReturnDifference(Set<K> keys, V value) {
		assert (checkConsistency());
		assert (value != null);
		HashSet<K> res = new HashSet<>();
		valueToKeys.putAll(value, keys);
		for (K key : keys) {
			boolean check = keyToValues.put(key, value);
			assert (check);
			if (keyToValues.get(key).size() == 1) {
				// (L,R): (1,0) -> (1,1)
				check = res.add(key);
				assert (check);
			}
		}
		assert (checkConsistency());
		return res;
	}

	/**
	 * 
	 * @param keys  keys that are already digested
	 * @param value new value
	 * @return all keys that now have at least one value, but had 0 before.
	 */
	public void putAll(Set<K> keys, V value) {
		assert (checkConsistency());
		assert (value != null);
		valueToKeys.putAll(value, keys);
		for (K key : keys) {
			keyToValues.put(key, value);
		}
		assert (checkConsistency());
	}

	/**
	 * 
	 * @param key    new key
	 * @param values values that are already digested
	 */
	public void putAll(K key, Set<V> values) {
		assert (checkConsistency());
		assert (key != null);
		keyToValues.putAll(key, values);
		for (V value : values) {
			valueToKeys.put(value, key);
		}
		assert (checkConsistency());
	}

	public void getSizeOneEntries(V value) {

	}

	/**
	 * 
	 * @param key
	 * @return defensive copy
	 */
	public HashSet<V> getValues(K key) {
		assert (checkConsistency());
		return new HashSet<>(keyToValues.get(key));
	}

	/**
	 * 
	 * @param key
	 * @return defensive copy
	 */
	public HashSet<K> getKeys(V value) {
		assert (checkConsistency());
		return new HashSet<>(valueToKeys.get(value));
	}

	public void remove(K key, V value) {
		assert (checkConsistency());
		keyToValues.remove(key, value);
		valueToKeys.remove(value, key);
		assert (checkConsistency());
	}

	/**
	 * Remove a key and return a Collection of values that have been removed as
	 * consequence.
	 */
	public HashSet<V> removeKey(K key) {
		assert (checkConsistency());
		Set<V> values = keyToValues.removeAll(key);
		HashSet<V> removedValues = new HashSet<>(); // values that were completely removed
		for (V value : values) {
			Collection<K> keys = valueToKeys.get(value);
			keys.remove(key);
			if (keys.isEmpty()) {
				boolean success = removedValues.add(value);
				assert (success);
			}
		}
		assert (checkConsistency());
		return removedValues;
	}

//	/**
//	 * Remove a Collection of keys and return a Collection of values that have been
//	 * removed as consequence.
//	 */
//	public Collection<V> removeKeys(Collection<K> keys) {
//		assert (checkConsistency());
//		Collection<V> values = new ArrayList<>();
//		Collection<V> removedValues = new ArrayList<>(); // values that were completely removed
//		for (K key : keys) {
//			values.addAll(keyToValues.removeAll(key));
//		}
//		for (V value : values) {
//			Collection<K> keyss = valueToKeys.get(value);
//			keyss.removeAll(keys);
//			if (keyss.isEmpty()) {
//				removedValues.add(value);
//			}
//		}
//		assert (checkConsistency());
//		return removedValues;
//	}

	/**
	 * Remove a value and return a Collection of keys that have been removed as
	 * consequence.
	 * 
	 * @return removed keys
	 */
	public HashSet<K> removeValue(V value) {
		assert (checkConsistency());
		Set<K> keys = valueToKeys.removeAll(value);
		HashSet<K> removedKeys = new HashSet<>(); // keys that were completely removed
		for (K key : keys) {
			Collection<V> values = keyToValues.get(key);
			values.remove(value);
			if (values.isEmpty()) {
				boolean success = removedKeys.add(key);
				assert (success);
			}
		}
		assert (checkConsistency());
		return removedKeys;
	}

//	/**
//	 * Remove a Collection of values and return a Collection of keys that have been
//	 * removed as consequence.
//	 */
//	public Collection<K> removeValues(Collection<V> values) {
//		assert (checkConsistency());
//		Collection<K> keys = new ArrayList<>();
//		Collection<K> removedKeys = new ArrayList<>(); // keys that were completely removed
//		for (V value : values) {
//			keys.addAll(valueToKeys.removeAll(value));
//		}
//		for (K key : keys) {
//			Collection<V> valuess = keyToValues.get(key);
//			valuess.removeAll(values);
//			if (valuess.isEmpty()) {
//				removedKeys.add(key);
//			}
//		}
//		assert (checkConsistency());
//		return removedKeys;
//	}

	public void clear() {
		assert (checkConsistency());
		keyToValues.clear();
		valueToKeys.clear();
	}

	@Override
	public String toString() {
		return "BiMultiMap [keyToValues=" + keyToValues + ", valueToKeys=" + valueToKeys + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(keyToValues, valueToKeys);
	}

	@Override
	public boolean equals(Object obj) {
		assert (checkConsistency());
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SpecialBiMultiMap)) {
			return false;
		}
		SpecialBiMultiMap<?, ?> other = (SpecialBiMultiMap<?, ?>) obj;
		return Objects.equals(keyToValues, other.keyToValues) && Objects.equals(valueToKeys, other.valueToKeys);
	}

	private boolean checkConsistency() {
		boolean res = true;
		Collection<Map.Entry<K, V>> kTOv = this.keyToValues.entries();
		for (Map.Entry<K, V> e : kTOv) {
			res = res && this.valueToKeys.containsEntry(e.getValue(), e.getKey());
		}
		Collection<Map.Entry<V, K>> vTOk = this.valueToKeys.entries();
		for (Map.Entry<V, K> e : vTOk) {
			res = res && this.keyToValues.containsEntry(e.getValue(), e.getKey());
		}
		return res;
	}
}
