package com.tomgibara.perfect;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import com.tomgibara.collect.Equivalence;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Store;

// note array note does not make defensive copies of collections
public final class Perfect<T> {

	//TODO need constructions with type
	public static <T> PerfectDomain<T> over(Iterable<? extends T> values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<>(new AdaptedCollection<>(values), null);
	}

	public static <T> PerfectDomain<T> over(T... values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<T>(Arrays.asList(values), (Class<T>) values.getClass().getComponentType());
	}

	public static <T> PerfectDomain<T> over(Collection<? extends T> values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<>(values, null);
	}

	public static <T> PerfectDomain<T> over(Store<T> values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<>(values.asList(), values.valueType());
	}

	private static class AdaptedCollection<E> extends AbstractCollection<E> {

		private final Iterable<E> iterable;
		private int size = -1;
		
		AdaptedCollection(Iterable<E> iterable) {
			this.iterable = iterable;
		}
		
		public Iterator<E> iterator() {
			return iterable.iterator();
		}
		
		public int size() {
			if (size < 0) {
				int count = 0;
				for (E value : iterable) count ++;
				size = count;
			}
			return size;
		}

	}

	private final Hasher<T> hasher;
	private final PerfectDomain<T> domain;
	private final Random random;
	private Equivalence<T> equ = null;
	
	Perfect(Hasher<T> hasher, PerfectDomain<T> domain, Random random) {
		this.hasher = hasher;
		this.domain = domain;
		this.random = random;
	}

	public Hasher<T> getHasher() {
		return hasher;
	}
	
	//TODO need to make this available on Minimal too
	public Equivalence<T> getEquivalence() {
		if (equ == null) {
			equ = new Equivalence<T>() {
				@Override public boolean isEquivalent(T e1, T e2) { return e1 == e2 || hasher.intHashValue(e1) == hasher.intHashValue(e2); };
				@Override public Hasher<T> getHasher() { return hasher; }
			};
		}
		return equ;
	}
	
	public PerfectDomain<T> getDomain() {
		return domain;
	}
	
	public Minimal<T> minimized() {
		return createMinimized(100, 1.15);
	}

	public Minimal<T> minimizedWithBMZ(int maxAttempts, double c) {
		if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts not positive");
		if (c < 1.0) throw new IllegalArgumentException("c less than 1.0");
		return createMinimized(maxAttempts, c);
	}

	private Minimal<T> createMinimized(int maxAttempts, double c) {
		Hasher<T> h = new BMZ<T>(hasher, maxAttempts, c, random).create(domain.getValues());
		return new Minimal<>(h, domain);
	}
}
