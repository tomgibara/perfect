package com.tomgibara.perfect;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import com.tomgibara.hashing.Hash;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.streams.StreamSerializer;

/**
 * Perfectionists operate over a {@link PerfectDomain} to find perfect hashes
 * (instances of {@link Perfect}). Instances of this class are created from
 * {@link PerfectDomain} instances.
 *
 * @author Tom Gibara
 *
 * @param <T>
 *            the type of values in the domain
 * @see PerfectDomain#using(int, Random)
 * @see PerfectDomain#usingDefaults()
 */
public class Perfectionist<T> {

	private final PerfectDomain<T> domain;
	private final int maxSeedAttempts;
	private final Random random;

	Perfectionist(PerfectDomain<T> domain, int maxSeedAttempts, Random random) {
		this.domain = domain;
		this.maxSeedAttempts = maxSeedAttempts;
		this.random = random;
	}

	/**
	 * Creates a perfect hash by simply assuming that the supplied hasher is
	 * perfect over the domain. Calling this method in error is likely to cause
	 * malfunctions, but the method may be useful when reconstructing a perfect
	 * domain with a hasher that has already been confirmed to be perfect.
	 *
	 * @param hasher
	 *            a hasher that is known to be perfect over the domain
	 * @return the supplied hasher as a perfect hash
	 */
	public Perfect<T> assumedPerfect(Hasher<T> hasher) {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		return new Perfect<>(hasher, domain, random);
	}

	/**
	 * <p>
	 * Attempts to create a perfect hash from the {@code hashCode()} method
	 * implemented by the domain values. In many circumstances, there is a high
	 * probability that this is true, and using values' defined hash will often
	 * yield better performance.
	 *
	 * <p>
	 * The {@code hashCode()} method is efficiently tested for perfection over
	 * the entire domain. If and only if it fails to be perfect (ie. fails to
	 * distinguish every value from all other values) then the returned optional
	 * value will be empty.
	 *
	 * @return a perfect hash based on the {@code hashCode()} method, or empty
	 */
	public Optional<Perfect<T>> maybePerfect() {
		return maybePerfect(Hashing.objectHasher());
	}

	/**
	 * <p>
	 * Attempts to create a perfect hash from the supplied hasher. The hasher is
	 * efficiently tested for perfection over the entire domain.
	 *
	 * <p>
	 * The returned optional will be empty precisely when a hasher fails to be
	 * perfect (ie. fails to distinguish every value from all other values).
	 *
	 * @param hasher
	 *            a hasher that can operate over all domain values
	 * @return a perfect hash based on the supplied hasher, or empty
	 */
	public Optional<Perfect<T>> maybePerfect(Hasher<T> hasher) {
		return domain.isPerfect(hasher) ? Optional.of(new Perfect<>(hasher, domain, random)) : Optional.empty();
	}

	/**
	 * Attempts to create a perfect hash using from the byte-serialized form of
	 * the domain values. This object may try repeated times to generate such a
	 * hash through a randomized process. The number of attempts made, and the
	 * randomization are determined by the parameters used to construct this
	 * object.
	 *
	 * @param serializer
	 *            a serializer that can operate over all domain values
	 * @return a perfect hash over the domain values
	 * @throws PerfectionException
	 *             if a perfect hash could not be generated within the this
	 *             object's parameters
	 * @see PerfectDomain#using(int, Random)
	 */
	public Perfect<T> perfect(StreamSerializer<T> serializer) throws PerfectionException {
		if (serializer == null) throw new IllegalArgumentException("null serializer");
		return perfect(serializer, Hashing.murmur3Int(), s -> Hashing.murmur3Int(s.intValue()).hasher(serializer));
	}

	/**
	 * Attempts to create a perfect hash using from the byte-serialized form of
	 * the domain values using a specified hash. This object may try repeated
	 * times to generate such a hash through a randomized process. The number of
	 * attempts made, and the randomization are determined by the parameters
	 * used to construct this object.
	 *
	 * @param serializer
	 *            a serializer that can operate over all domain values
	 * @param hash
	 *            an explicit hash algorithm to use over the value
	 *            byte-serializations
	 * @return a perfect hash over the domain values
	 * @throws PerfectionException
	 *             if a perfect hash could not be generated within the this
	 *             object's parameters
	 * @see PerfectDomain#using(int, Random)
	 */
	public Perfect<T> perfect(StreamSerializer<T> serializer, Hash hash) throws PerfectionException {
		if (hash == null) throw new IllegalArgumentException("null hash");
		if (serializer == null) throw new IllegalArgumentException("null serializer");
		return perfect(serializer, hash, s -> hash.seeded(serializer, s));
	}

	// private utility methods

	private Perfect<T> perfect(StreamSerializer<T> serializer, Hash hash, Function<Long, Hasher<T>> seeded) {
		for (int i = 0; i < maxSeedAttempts; i++) {
			// after two attempts, doubt the serializer
			if (i == 2 && !domain.isInjective(serializer)) throw new PerfectionException("serializer not injective");
			Hasher<T> hasher = i == 0 ?  hash.hasher(serializer) : seeded.apply((random.nextLong()));
			if (domain.isPerfect(hasher)) return new Perfect<>(hasher, domain, random);
		}
		throw new PerfectionException("unable to find hash function after " + maxSeedAttempts);
	}


}
