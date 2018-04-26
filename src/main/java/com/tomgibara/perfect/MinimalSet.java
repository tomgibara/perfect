package com.tomgibara.perfect;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.Bits;
import com.tomgibara.fundament.Mutability;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Store;

/**
 * A set implementation that pre-allocates a bit field to record membership of
 * keys from a hash domain. Sets of this nature are created via the
 * {@link Minimal#newSet()} method. The set cannot contain elements outside of
 * the hash domain.
 *
 * @author Tom Gibara
 *
 * @param <E>
 *            the type of elements stored in the set; that of the minimal hash
 *            domain
 */
public class MinimalSet<E> extends AbstractSet<E> implements Mutability<MinimalSet<E>> {

	private final Hasher<E> hasher;
	private final Store<E> store;
	private final BitStore bits;

	MinimalSet(Minimal<E> minimal) {
		hasher = minimal.getHasher();
		store = minimal.getStore();
		bits = Bits.store(store.size());
	}

	private MinimalSet(Hasher<E> hasher, Store<E> store, BitStore bits) {
		this.hasher = hasher;
		this.store = store;
		this.bits = bits;
	}

	// methods

	/**
	 * Adds all elements of the hash domain to the set.
	 */
	public void fill() {
		bits.fill();
	}

	/**
	 * Whether the set contains all elements of the domain.
	 *
	 * @return true if the set contains all possible elements, false otherwise
	 */
	public boolean isFull() {
		return bits.ones().isAll();
	}

	// mutability

	@Override
	public boolean isMutable() {
		return bits.isMutable();
	}

	@Override
	public MinimalSet<E> mutable() {
		return isMutable() ? this : mutableCopy();
	}

	@Override
	public MinimalSet<E> immutable() {
		return isMutable() ? immutableView() : this;
	}

	@Override
	public MinimalSet<E> mutableCopy() {
		return new MinimalSet<>(hasher, store, bits.mutableCopy());
	}

	@Override
	public MinimalSet<E> immutableCopy() {
		return new MinimalSet<>(hasher, store, bits.immutableCopy());
	}

	@Override
	public MinimalSet<E> immutableView() {
		return new MinimalSet<>(hasher, store, bits.immutable());
	}

	// set

	@Override
	public int size() {
		return bits.ones().count();
	}

	@Override
	public boolean contains(Object o) {
		int i = indexOf(o);
		return i != -1 && bits.getBit(i);
	}

	@Override
	public boolean remove(Object o) {
		int i = indexOf(o);
		return i != -1 && bits.getThenSetBit(i, false);
	}

	@Override
	public void clear() {
		bits.clear();
	}

	@Override
	public boolean isEmpty() {
		return bits.zeros().isAll();
	}

	@Override
	public boolean add(E e) {
		int i = validIndex(hasher.intHashValue(e), e);
		return i != -1 && !bits.getThenSetBit(i, true);
	}

	@Override
	public Object[] toArray() {
		int length = size();
		Object[] array = new Object[length];
		populateArray(array, length);
		return array;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		int length = size();
		Object[] array;
		if (a.length >= length) {
			array = a;
		} else if (a.getClass().getComponentType() == String.class) {
			array = new String[length];
		} else {
			array = new Object[length];
		}
		populateArray(array, length);
		return (T[]) array;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private final BitStore.Positions positions = bits.ones().positions();
			@Override public boolean hasNext() { return positions.hasNext(); }
			@Override public E next() { return store.get(positions.next()); }
			@Override public void remove() { positions.remove(); }
		};
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		boolean modified = false;
		for (Positions ps = bits.ones().positions(); ps.hasNext(); ) {
			int p = ps.nextPosition();
			E e = store.get(p);
			if (filter.test(e)) {
				modified = bits.getThenSetBit(p, false) || modified;
			}

		}
		return modified;
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		for (Positions ps = bits.ones().positions(); ps.hasNext(); ) {
			action.accept(store.get(ps.nextPosition()));
		}
	}

	// private utility methods

	private int indexOf(Object o) {
		if (!store.isSettable(o)) return -1;
		@SuppressWarnings("unchecked")
		E e = (E) o;
		int i;
		try {
			i = hasher.intHashValue(e);
		} catch (IllegalArgumentException ex) {
			return -1;
		}
		return validIndex(i, e);
	}

	private void populateArray(Object[] array, int length) {
		for (Positions ps = bits.ones().positions(); ps.nextIndex() < length && ps.hasNext(); ) {
			// order important here, nextIndex first because nextPosition advances
			int i = ps.nextIndex();
			int p = ps.nextPosition();
			array[i] = store.get(p);
		}
	}

	private int validIndex(int i, E e) {
		return i >= 0 && i < bits.size() && store.get(i).equals(e) ? i : -1;
	}
}
