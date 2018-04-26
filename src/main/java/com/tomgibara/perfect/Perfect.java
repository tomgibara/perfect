package com.tomgibara.perfect;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Store;

/**
 * <p>
 * This class serves as both the entry-point for generating perfect hashes, and
 * as their embodiment.
 *
 * <p>
 * The hash returned by {@link #getHasher()} is guaranteed to be perfect over
 * the elements of {@link #getDomain()}, meaning that the hash function is
 * injective; every value in the domain will be mapped to a different hash
 * value.
 *
 * @author Tom Gibara
 *
 * @param <T>
 *            the type of values over which the perfect hash is defined
 */
public final class Perfect<T> extends DomainHash<T> {

	/**
	 * Creates a domain of values over which a perfect hash can be defined from
	 * a supplied iterable. This method can be used to create hashes over very
	 * large collections without retaining any significant proportion of the
	 * values concurrently in memory. Note that the iterable <i>must</i> return
	 * a consistent set of objects on each call.
	 *
	 * @param values
	 *            iterable access to the domain values
	 * @return a domain over the supplied values
	 */
	public static <T> PerfectDomain<T> over(Iterable<? extends T> values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<>(new AdaptedCollection<>(values), null);
	}

	/**
	 * Creates a domain of values over which a perfect hash can be defined from
	 * a supplied iterable. This method behaves identically to
	 * {@link #over(Iterable)} with the exception that it records an explicit
	 * type for the elements of the domain. This explicit type can result in
	 * reduced memory usage for domains over primitive types (eg. using
	 * long.class) and may yield greater type safety.
	 *
	 * @param values
	 *            iterable access to the domain values
	 * @param type
	 *            an explicit type for the domain values
	 * @return a domain over the supplied values
	 */
	public static <T> PerfectDomain<T> over(Iterable<? extends T> values, Class<T> type) {
		if (values == null) throw new IllegalArgumentException("null values");
		if (type == null) throw new IllegalArgumentException("null type");
		return new PerfectDomain<>(new AdaptedCollection<>(values), type);
	}

	/**
	 * Creates a domain of values over which a perfect hash can be defined from
	 * a supplied array. Note that, to reduce memory usage for large domains,
	 * the supplied array is <i>not</i> copied; the array should not be modified
	 * after being supplied to this method.
	 *
	 * @param values
	 *            an array of the domain values
	 * @return a domain over the supplied values
	 */

	public static <T> PerfectDomain<T> over(T... values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<T>(Arrays.asList(values), (Class<T>) values.getClass().getComponentType());
	}

	/**
	 * Creates a domain of values over which a perfect hash can be defined from
	 * a supplied collection. Note that, to reduce memory usage for large
	 * domains, the supplied collection is <i>not</i> copied; the collection
	 * should not be modified after being supplied to this method.
	 *
	 * @param values
	 *            an collection of the domain values
	 * @return a domain over the supplied values
	 */

	public static <T> PerfectDomain<T> over(Collection<? extends T> values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<>(values, null);
	}

	/**
	 * Creates a domain of values over which a perfect hash can be defined from
	 * a supplied store. Null store values are ignored and are not considered
	 * elements of the domain. Note that, to reduce memory usage for large
	 * domains, the supplied store is <i>not</i> copied; the store should not be
	 * modified after being supplied to this method.
	 *
	 * @param values
	 *            an collection of the domain values
	 * @return a domain over the supplied values
	 */

	public static <T> PerfectDomain<T> over(Store<T> values) {
		if (values == null) throw new IllegalArgumentException("null values");
		return new PerfectDomain<>(new AdaptedCollection<>(values), values.type().valueType());
	}

	private static class AdaptedCollection<E> extends AbstractCollection<E> {

		private final Iterable<E> iterable;
		private int size;

		AdaptedCollection(Iterable<E> iterable) {
			this.iterable = iterable;
			size = -1;
		}

		AdaptedCollection(Store<E> store) {
			iterable = store;
			size = store.count();
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

	private final Random random;
	
	Perfect(Hasher<T> hasher, PerfectDomain<T> domain, Random random) {
		super(hasher, domain);
		this.random = random;
	}

	/**
	 * Attempts to derive a minimal perfect hash from this perfect hash using
	 * default settings. The attempt may fail with a perfection exception if
	 * finding a hash proves too difficult to find.
	 *
	 * @return a minimal perfect hash over the same domain
	 * @throws PerfectionException
	 *             if the minimization algorithm failed to create a suitable
	 *             hash within a predefined number of attempts
	 */
	public Minimal<T> minimized() throws PerfectionException {
		return createMinimized(100, 1.15);
	}

	/**
	 * <p>
	 * Attempts to derive a minimal perfect hash from this perfect hash using
	 * default the BMZ algorithm. Each pass of the algorithm operates over a
	 * randomized bipartite graph and has a chance of failure. The attempt may
	 * fail with a perfection exception if the maximum number of attempts is
	 * exceeded.
	 *
	 * <p>
	 * A {@code c} parameter limits the memory required to define the minimal
	 * hash to {@code 4*m*c} bytes, where {@code m} is the number of elements in
	 * the domain. The value of {@code c} cannot be less than {@code 1/0};
	 * increasing the value increases the probability that any single pass of
	 * the algorithm will succeed at the expense of requiring more memory to
	 * define the hash. A value {@code 1.15} is recommended by the paper's
	 * original authors.
	 *
	 * @param maxAttempts
	 *            the greatest number of attempts that the algorithm should make
	 *            to produce a minimal hash
	 * @param c
	 *            a multiple, not less than 1.0, that limits the memory used to
	 *            define the resulting hash
	 * @return a minimal perfect hash over the same domain
	 * @throws PerfectionException
	 *             if the minimization algorithm failed to create a suitable
	 *             hash within the specified number of attempts
	 */
	public Minimal<T> minimizedWithBMZ(int maxAttempts, double c) throws PerfectionException {
		if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts not positive");
		if (c < 1.0) throw new IllegalArgumentException("c less than 1.0");
		return createMinimized(maxAttempts, c);
	}

	private Minimal<T> createMinimized(int maxAttempts, double c) throws PerfectionException {
		Hasher<T> h = new BMZ<T>(hasher, maxAttempts, c, random).create(domain.getValues());
		return new Minimal<>(h, domain);
	}
}
