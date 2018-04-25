package com.tomgibara.perfect;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.tomgibara.collect.Equivalence;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamSerializer;
import com.tomgibara.streams.Streams;

/**
 * Instances of this class define the values over which a perfect hash may be
 * defined.
 *
 * @author Tom Gibara
 *
 * @param <T>
 *            the type of values in the domain
 */
public class PerfectDomain<T> {

	// statics
	
	private static final int MAX_SEED_ATTEMPTS = 3;
	static final int COMPACT_BIT_CUTOFF = 16;

	// fields
	
	private final Collection<? extends T> values;
	private final Class<T> type;

	// constructors
	
	PerfectDomain(Collection<? extends T> values, Class<T> type) {
		this.values = values;
		this.type = type;
	}

	// accessors

	/**
	 * A collection of the values in the domain.
	 *
	 * @return the domain values
	 */

	public Collection<? extends T> getValues() {
		return values;
	}

	/**
	 * The type of values in the domain, if known.
	 *
	 * @return the explicit type of values in the domain, or empty if unknown
	 */
	public Optional<Class<T>> getType() {
		return Optional.ofNullable(type);
	}

	// methods

	/**
	 * <p>
	 * Determines whether the specified hasher is perfect over this domain. A
	 * hasher is perfect over this domain if every value yields a distinct hash
	 * value.
	 *
	 * <p>
	 * For large domains, this method requires memory proportional to the log of
	 * the domain size.
	 *
	 * @param hasher
	 *            the hasher to be tested for perfection
	 * @return true if the hasher is perfect over this domain, false otherwise
	 */

	public boolean isPerfect(Hasher<T> hasher) {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		HashSize size = hasher.getSize();
		// we can do compact test using a bitmap if the hash size is small enough
		if (size.getBits() <= COMPACT_BIT_CUTOFF) {
			BitStore store = Bits.store(size.asInt());
			for (T value : values) {
				if (store.getThenSetBit(hasher.intHashValue(value), true)) {
					return false;
				}
			}
			return true;
		}
		// fall back to a uniqueness check - still highly memory efficient
		Iterable<BigInteger> iterable = () -> new HashIterator<T>(values.iterator(), hasher);
		int sizeEstimate = (11 + (size.getBits() + 31 >> 5)) << 2;
		int itemCount = values.size();
		UniquenessChecker<BigInteger> checker = new UniquenessChecker<BigInteger>(itemCount, sizeEstimate, Equivalence.equality(), BigInteger.class);
		return checker.check(iterable);
	}

	/**
	 * <p>
	 * Determines whether the specified serializer is injective over this
	 * domain. In other words, that the serializer produces a different stream
	 * of bytes for each element of the domain. Serializers for which this is
	 * not true are not candidates for establishing a perfect hash since some
	 * domain elements become indistinguishable.
	 *
	 * <p>
	 * For large domains, this method requires memory proportional to the log of
	 * the domain size.
	 *
	 * @param serializer
	 *            the serializer to be evaulated
	 * @return true if the serializer can distinguish all elements of this
	 *         domain
	 */

	public boolean isInjective(StreamSerializer<T> serializer) {
		if (serializer == null) throw new IllegalArgumentException("null serializer");
		Iterable<byte[]> iterable = () -> new BytesIterator<T>(values.iterator(), serializer);
		int sizeEstimate = 50;
		int itemCount = values.size();
		UniquenessChecker<byte[]> checker = new UniquenessChecker<byte[]>(itemCount, sizeEstimate, Equivalence.bytes(), byte[].class);
		return checker.check(iterable);
	}

	/**
	 * An object that can attempt to generate perfect hashes over this domain.
	 * This method uses default parameters that can be overridden by using the
	 * alternative {@link #using(int, Random)} method.
	 *
	 * @return an object for generating perfect hashes
	 */

	public Perfectionist<T> usingDefaults() {
		return new Perfectionist<T>(this, MAX_SEED_ATTEMPTS, new Random());
	}

	/**
	 * An object that can attempt to generate perfect hashes over this domain.
	 * Perfect hashes are generated via a probablistic process that may in some
	 * cases (where the domain is large) fail to yield a perfect hash. The
	 * {@code maxSeedAttempts} parameter limits the number of times that
	 * perfection will be attempted to guard against unbounded execution times.
	 * The {@code random} parameter provides a source of randomness that the
	 * algorithm will use.
	 *
	 * @param maxSeedAttempts
	 *            the maximum number of attempts that will be made to produce a
	 *            perfect hash
	 * @param random
	 *            a source of randomness for generating perfect hashes
	 * @return an object for generating perfect hashes over this domain
	 */

	public Perfectionist<T> using(int maxSeedAttempts, Random random) {
		return new Perfectionist<T>(this, maxSeedAttempts, random);
	}

	// inner classes

	private static class HashIterator<E> implements Iterator<BigInteger> {

		private final Iterator<? extends E> iterator;
		private final Hasher<E> hasher;

		HashIterator(Iterator<? extends E> iterator, Hasher<E> hasher) {
			this.iterator = iterator;
			this.hasher = hasher;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public BigInteger next() {
			E value = iterator.next();
			BigInteger big = hasher.bigHashValue(value);
			if (big.intValue() == 3155236) System.out.println(value);
			return big;
		}

	}

	private static class BytesIterator<E> implements Iterator<byte[]> {

		private final Iterator<? extends E> iterator;
		private final StreamSerializer<E> serializer;

		BytesIterator(Iterator<? extends E> iterator, StreamSerializer<E> serializer) {
			this.iterator = iterator;
			this.serializer = serializer;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public byte[] next() {
			StreamBytes bytes = Streams.bytes();
			serializer.serialize(iterator.next(), bytes.writeStream());
			return bytes.bytes();
		}

	}

}
