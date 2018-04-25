package com.tomgibara.perfect;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.tomgibara.collect.AbstractMapEntry;
import com.tomgibara.fundament.Mutability;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Store;

/**
 * A map implementation that pre-allocates value storage for keys over a minimal
 * hash domain. Maps of this nature are created via the
 * {@link Minimal.Maps#newMap()} method.
 *
 * @author Tom Gibara
 *
 * @param <K>
 *            the type of keys stored in the map; that of the minimal hash
 *            domain
 * @param <V>
 *            the type of values stored in the map
 * @see #Minimal.Maps
 */
public class MinimalMap<K,V> extends AbstractMap<K, V> implements Mutability<MinimalMap<K,V>> {

	// fields
	
	//TODO rename
	private final Store<K> strings;
	private final Hasher<K> hasher;
	private final Store<V> store;
	
	private Entries entries = null;
	private Keys keys = null;
	private Values values = null;
	
	// constructors
	
	MinimalMap(Hasher<K> hasher, Store<K> strings, Store<V> store) {
		this.hasher = hasher;
		this.strings = strings;
		this.store = store;
	}
	
	// mutability
	
	@Override
	public boolean isMutable() {
		return store.isMutable();
	}
	
	@Override
	public MinimalMap<K,V> mutableCopy() {
		return new MinimalMap<>(hasher, strings, store.mutableCopy());
	}
	
	@Override
	public MinimalMap<K,V> immutableCopy() {
		return new MinimalMap<>(hasher, strings, store.immutableCopy());
	}
	
	@Override
	public MinimalMap<K,V> immutableView() {
		return new MinimalMap<>(hasher, strings, store.immutable());
	}
	
	@Override
	public MinimalMap<K,V> mutable() {
		return isMutable() ? this : mutableCopy();
	}
	
	@Override
	public MinimalMap<K,V> immutable() {
		return isMutable() ? immutableView() : this;
	}
	
	// map
	
	@Override
	public int size() {
		return store.count();
	}
	
	@Override
	public void clear() {
		store.clear();
	}
	
	@Override
	public boolean containsKey(Object key) {
		int i = indexOf(key);
		return i != -1 && !store.isNull(i);
	}
	
	@Override
	public boolean containsValue(Object value) {
		return indexOfValue(value) != -1;
	}
	
	@Override
	public boolean isEmpty() {
		return store.count() == 0;
	}
	
	@Override
	public V get(Object key) {
		int i = indexOf(key);
		return i == -1 ? null : store.get(i);
	}
	
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		int i = indexOf(key);
		if (i == -1) return defaultValue;
		V value = store.get(i);
		return value == null ? defaultValue : value;
	}
	
	@Override
	public V remove(Object key) {
		int i = indexOf(key);
		if (i == -1) return null;
		V value = store.get(i);
		if (value != null) store.set(i, null);
		return value;
	}
	
	@Override
	public boolean remove(Object key, Object value) {
		if (value == null) return false;
		int i = indexOf(key);
		if (i == -1) return false;
		V previous = store.get(i);
		if (previous == null || !previous.equals(value)) return false;
		store.set(i, null);
		return true;
	}
	
	@Override
	public V put(K key, V value) {
		if (value == null) throw new IllegalArgumentException("null value");
		int i = checkedIndexOf(key);
		return store.set(i, value);
	}

	@Override
	public V putIfAbsent(K key, V value) {
		if (value == null) throw new IllegalArgumentException("null value");
		int i = checkedIndexOf(key);
		V previous = store.get(i);
		if (previous != null) return previous;
		store.set(i, value);
		return null;
	}

	@Override
	public V replace(K key, V value) {
		if (value == null) throw new IllegalArgumentException("null value");
		int i = checkedIndexOf(key);
		V previous = store.get(i);
		if (previous == null) return null;
		store.set(i, value);
		return previous;
	}
	
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		if (newValue == null) throw new IllegalArgumentException("null newValue");
		int i = checkedIndexOf(key);
		V previous = store.get(i);
		if (previous == null || !previous.equals(oldValue)) return false;
		store.set(i, newValue);
		return true;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return entries == null ? entries = new Entries() : entries;
	}
	
	@Override
	public Set<K> keySet() {
		return keys == null ? keys = new Keys() : keys;
	}
	
	@Override
	public Collection<V> values() {
		return values == null ? values = new Values() : values;
	}

	private int indexOf(Object o) {
		if (!strings.isSettable(o)) return -1;
		K k = (K) o;
		//TODO no way to make this more efficient yet
		int i;
		try {
			i = hasher.intHashValue(k);
		} catch (IllegalArgumentException e) {
			return -1;
		}
		//TODO do we want to require equality
		return strings.get(i).equals(k) ? i : -1;
	}
	
	private int checkedIndexOf(K k) {
		int i = hasher.intHashValue(k);
		//TODO do we want to require equality
		if (!strings.get(i).equals(k)) throw new IllegalArgumentException("invalid token");
		return i;
	}
	
	private int indexOfValue(Object value) {
		if (value == null) return -1;
		int size = strings.size();
		for (int i = 0; i < size; i++) {
			V candidate = store.get(i);
			if (candidate != null && candidate.equals(value)) return i;
		}
		return -1;
	}

	private final class Keys extends AbstractSet<K> {
		
		@Override
		public int size() {
			return store.count();
		}
		
		@Override
		public boolean isEmpty() {
			return store.count() != 0;
		}
		
		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}
		
		@Override
		public void clear() {
			store.clear();
		}
		
		@Override
		public boolean remove(Object o) {
			int i = indexOf(o);
			if (i == -1) return false;
			if (store.isNull(i)) return false;
			store.set(i, null);
			return true;
		}

		@Override
		public Iterator<K> iterator() {
			return store.transformedIterator((i,v) -> strings.get(i));
		}
	}
	
	private final class Values extends AbstractCollection<V> {

		@Override
		public int size() {
			return store.count();
		}
		
		@Override
		public boolean isEmpty() {
			return store.count() == 0;
		}
		
		@Override
		public void clear() {
			store.clear();
		}
		
		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}
		
		@Override
		public boolean remove(Object o) {
			int i = indexOfValue(o);
			if (i == -1) return false;
			store.set(i, null);
			return true;
		}

		@Override
		public Iterator<V> iterator() {
			return store.iterator();
		}

	}
	
	private final class Entries extends AbstractSet<Entry<K, V>> {
		
		@Override
		public int size() {
			return store.count();
		}
		
		@Override
		public boolean isEmpty() {
			return store.count() == 0;
		}
		
		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Entry)) return false;
			Entry<?,?> e = (Entry<?,?>) o;
			Object k = e.getKey();
			int i = indexOf(k);
			if (i == -1) return false;
			V v = store.get(i);
			if (v == null) return false;
			return v.equals(e.getValue());
		}
		
		@Override
		public boolean remove(Object o) {
			if (!(o instanceof Entry)) return false;
			Entry<?,?> e = (Entry<?,?>) o;
			Object k = e.getKey();
			int i = indexOf(k);
			if (i == -1) return false;
			V v = store.get(i);
			if (v == null) return false;
			if (!v.equals(e.getValue())) return false;
			store.set(i, null);
			return true;
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			//TODO could just iterate over positions?
			return store.transformedIterator((i,k) -> new MinimalEntry(i));
		}
		
		@Override
		public void clear() {
			store.clear();
		}
		
	}
	
	final private class MinimalEntry extends AbstractMapEntry<K, V> {
		
		private final int index;
		
		MinimalEntry(int index) {
			this.index = index;
		}
		
		@Override
		public K getKey() {
			return strings.get(index);
		}
		
		@Override
		public V getValue() {
			return store.get(index);
		}
		
		@Override
		public V setValue(V value) {
			if (value == null) throw new IllegalArgumentException("null value");
			return store.set(index, value);
		}

	}

}
