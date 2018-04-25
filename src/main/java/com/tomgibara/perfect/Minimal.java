package com.tomgibara.perfect;

import java.util.Collection;
import java.util.Optional;

import com.tomgibara.hashing.Hasher;
import com.tomgibara.permute.Permutation;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

/**
 * A minimal perfect hash.
 *
 * The hasher returned by this {@link #getHasher()} is guaranteed to be both
 * perfect over its domain and minimal. In this context, minimal means that for
 * a domain of size <i>n<i> the hash value is in the range [0,<i>n</i>].
 *
 * @author Tom Gibara
 *
 * @param <T>
 *            the type of values over which the minimal hash is defined
 */
public class Minimal<T> {

	private final Hasher<T> hasher;
	private final PerfectDomain<T> domain;
	private Permutation permutation = null;
	private Store<T> store = null;

	Minimal(Hasher<T> hasher, PerfectDomain<T> domain) {
		this.hasher = hasher;
		this.domain = domain;
	}

	// accessors

	/**
	 * A hash that is both perfect and minimal over its domain.
	 *
	 * @return a perfect hasher
	 */
	public Hasher<T> getHasher() {
		return hasher;
	}

	/**
	 * The domain over which the hash returned by {@link #getHasher()} is
	 * guaranteed to be minimal.
	 *
	 * @return the domain of values
	 */
	public PerfectDomain<T> getDomain() {
		return domain;
	}

	/**
	 * The permutation induced by this minimal hash. The returned permutation
	 * has the characteristic that n -> m iff the hash of the domain element at
	 * index n is m.
	 *
	 * @return the permutation of the domain induced by the hash.
	 */
	public Permutation getPermutation() {
		if (permutation == null) {
			populate();
		}
		return permutation;
	}

	/**
	 * The elements of the domain organized such that each elements index in the
	 * store is equal to its hash value.
	 *
	 * @return the domain elements ordered by hash value.
	 */
	public Store<T> getStore() {
		if (store == null) {
			Optional<Class<T>> optionalType = domain.getType();
			StoreType<T> storeType;
			if (optionalType.isPresent()) {
				Class<T> type = optionalType.get();
				storeType = StoreType.of(type).settingNullToDefault();
			} else {
				storeType = StoreType.generic();
			}
			store = storeType.storage().newStore(domain.getValues().size());
			populate();
			store = store.immutableView();
		}
		return store;
	}

	// methods

	/**
	 * <p>
	 * Creates a new empty set that uses a bit field to record membership. The
	 * sets created by this method each, at creation time, allocate a number of
	 * bits equal to the size of the element domain; they can only contain
	 * elements from the domain associated with this minimal hash.
	 *
	 * <p>
	 * Sets implemented in this fashion can be useful for recording large
	 * numbers of sets over the same modestly sized domain
	 *
	 * @return a bit field backed set
	 */
	public MinimalSet<T> newSet() {
		return new MinimalSet<>(this);
	}

	/**
	 * A means for creating maps mapping elements of the hash domain to the
	 * specified storage.
	 *
	 * @param storage
	 *            the storage to be used for map values
	 * @return maps that map domain keys to the specified storage
	 */
	public <V> Maps<V> withStorage(Storage<V> storage) {
		if (storage == null) throw new IllegalArgumentException("null storage");
		return new Maps<>(storage);
	}

	/**
	 * A means for creating maps mapping elements of the hash domain to
	 * objects.
	 *
	 * @return maps that map domain keys to generic storage
	 */
	public <V> Maps<V> withGenericStorage() {
		return new Maps<>(StoreType.<V>generic().storage());
	}

	/**
	 * A means for mapping elements of the hash domain to objects. In contrast
	 * to the {@link #withGenericStorage()} method allows a default value to be
	 * specified, to which all keys that have not been explicitly mapped to a
	 * different value, will be mapped. The sizes of all maps produced in this
	 * way are that of the domain; removing keys reassigns them the default
	 * value and leaves the map size unchanged.
	 *
	 * @param nullValue
	 *            the value automatically assigned to unmapped keys.
	 * @return maps that map the domain keys to generic storage
	 */
	public <V> Maps<V> withGenericStorage(V nullValue) {
		return new Maps<>(StoreType.<V>generic().settingNullToValue(nullValue).storage());
	}

	/**
	 * Provides for creating maps mapping elements of the hash domain to values
	 * of an explicit type. Note that the supplied type can may be primitive
	 * (eg. {@code int.class}) in which case key storage will be backed by
	 * primitive arrays. This can be important for reducing memory usage.
	 *
	 * @return maps that map domain keys to values of the given type
	 */
	public <V> Maps<V> withTypedStorage(Class<V> type) {
		return new Maps<>(StoreType.of(type).storage());
	}

	/**
	 * <p>
	 * Provides for creating maps mapping elements of the hash domain to values
	 * of an explicit type. In contrast to the {@link #withGenericStorage()}
	 * method allows a default value to be specified, to which all keys that
	 * have not been explicitly mapped to a different value, will be mapped. The
	 * sizes of all maps produced in this way are that of the domain; removing
	 * keys reassigns them the default value and leaves the map size unchanged.
	 * This has the possibility of further reducing the memory required to store
	 * primitive backed maps, since key membership in the map does not need to
	 * be recorded.
	 *
	 * <p>
	 * By way an example, maps of this nature can be useful in applications that
	 * need to count occurrences of domain elements; using a type of
	 * {@code int.class} with a default value of {@code 0} allows frequencies to
	 * be accumulated over a large domain using approximately 4 bytes per key
	 * and avoids all non-boxing allocations.
	 *
	 * @return maps that map domain keys to values of the given type
	 */
	public <V> Maps<V> withTypedStorage(Class<V> type, V nullValue) {
		return new Maps<>(StoreType.of(type).settingNullToValue(nullValue).storage());
	}

	// private utility methods
	
	private void populate() {
		Collection<? extends T> values = domain.getValues();
		int count = 0;
		if (permutation == null) {
			// if store has been assigned, it's a signal to populate it
			// otherwise only the permutation is required
			int[] order = new int[values.size()];
			for (T value : values) {
				int index = hasher.intHashValue(value);
				order[count++] = index;
				if (store != null) store.set(index, value);
			}
			permutation = Permutation.reorder(order);
		} else {
			// permutation has already been computed
			// so this call must be intended to populate store
			// we use the permutation to make this faster
			for (T value : values) {
				store.set(count++, value);
			}
			permutation.permute(store);
		}
	}

	// inner classes

	/**
	 * <p>
	 * A class that can create maps over the hash domain.
	 *
	 * <p>
	 * The characteristics of the maps generated by this class vary depending on
	 * the method used to construct its instance, but all pre-allocate backing
	 * storage to accommodate values for each possible key.
	 *
	 * @param <V>
	 *            the type of value to which keys are mapped.
	 * @see Minimal#withGenericStorage()
	 * @see Minimal#withGenericStorage(Object)
	 * @see Minimal#withStorage(Storage)
	 * @see Minimal#withTypedStorage(Class)
	 * @see Minimal#withTypedStorage(Class, Object)
	 */
	public final class Maps<V> {

		final Storage<V> storage;

		Maps(Storage<V> storage) {
			this.storage = storage;
		}

		/**
		 * Constructs a new map over hash domain.
		 *
		 * @return a new empty map
		 */
		public MinimalMap<T,V> newMap() {
			Store<T> store = getStore();
			return new MinimalMap<>(hasher, store, storage.newStore(store.size()));
		}

	}

}
