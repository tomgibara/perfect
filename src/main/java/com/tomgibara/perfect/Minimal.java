package com.tomgibara.perfect;

import java.util.Collection;
import java.util.Optional;

import com.tomgibara.hashing.Hasher;
import com.tomgibara.permute.Permutation;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;

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
	
	public Hasher<T> getHasher() {
		return hasher;
	}
	
	public PerfectDomain<T> getDomain() {
		return domain;
	}

	public Permutation getPermutation() {
		if (permutation == null) {
			populate();
		}
		return permutation;
	}
	
	public Store<T> getStore() {
		if (store == null) {
			Optional<Class<T>> optionalType = domain.getType();
			Storage<T> storage = optionalType.isPresent() ? Storage.typed(optionalType.get(),false) : Storage.generic(false);
			store = storage.newStore(domain.getValues().size());
			populate();
			store = store.immutableView();
		}
		return store;
	}
	
	// methods
	
	public MinimalSet<T> newSet() {
		return new MinimalSet<>(this);
	}

	public <V> Maps<V> withStorage(Storage<V> storage) {
		if (storage == null) throw new IllegalArgumentException("null storage");
		return new Maps<>(storage);
	}
	
	public <V> Maps<V> withGenericStorage() {
		return new Maps<>(Storage.generic(false));
	}
	
	public <V> Maps<V> withNullableGenericStorage() {
		return new Maps<>(Storage.generic(true));
	}
	
	public <V> Maps<V> withTypedStorage(Class<V> type) {
		if (type == null) throw new IllegalArgumentException("null type");
		return new Maps<>(Storage.typed(type, false));
	}
	
	public <V> Maps<V> withNullableTypedStorage(Class<V> type) {
		if (type == null) throw new IllegalArgumentException("null type");
		return new Maps<>(Storage.typed(type, true));
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
	
	public final class Maps<V> {

		final Storage<V> storage;

		Maps(Storage<V> storage) {
			this.storage = storage;
		}

		public MinimalMap<T,V> newMap() {
			Store<T> store = getStore();
			return new MinimalMap<>(hasher, store, storage.newStore(store.size()));
		}

	}

}
