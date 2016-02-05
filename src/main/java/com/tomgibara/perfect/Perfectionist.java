package com.tomgibara.perfect;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import com.tomgibara.hashing.Hash;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.streams.StreamSerializer;

public class Perfectionist<T> {

	private final PerfectDomain<T> domain;
	private final int maxSeedAttempts;
	private final Random random;
	
	Perfectionist(PerfectDomain<T> domain, int maxSeedAttempts, Random random) {
		this.domain = domain;
		this.maxSeedAttempts = maxSeedAttempts;
		this.random = random;
	}

	public Perfect<T> assumedPerfect(Hasher<T> hasher) {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		return new Perfect<>(hasher, domain, random);
	}
	
	public Optional<Perfect<T>> maybePerfect() {
		return maybePerfect(Hashing.objectHasher());
	}
	
	public Optional<Perfect<T>> maybePerfect(Hasher<T> hasher) {
		return domain.isPerfect(hasher) ? Optional.of(new Perfect<T>(hasher, domain, random)) : Optional.empty();
	}

	public Perfect<T> perfect(StreamSerializer<T> serializer) {
		if (serializer == null) throw new IllegalArgumentException("null serializer");
		return perfect(Hashing.murmur3Int(), serializer, s -> Hashing.murmur3Int(s.intValue()).hasher(serializer));
	}

	public Perfect<T> perfect(Hash hash, StreamSerializer<T> serializer) {
		if (hash == null) throw new IllegalArgumentException("null hash");
		if (serializer == null) throw new IllegalArgumentException("null serializer");
		return perfect(hash, serializer, s -> hash.seeded(serializer, s));
	}

	// package scoped
	
	// four poss. :
	//  default hasher
	//  supplied hasher
	//  default hash + specified serializer
	//  specified hash + specified serializer
	
	// private utility methods
	
	private Perfect<T> perfect(Hash hash, StreamSerializer<T> serializer, Function<Long, Hasher<T>> seeded) {
		for (int i = 0; i < maxSeedAttempts; i++) {
			// after two attempts, doubt the serializer
			if (i == 2 && !domain.isInjective(serializer)) throw new PerfectionException("serializer not injective");
			Hasher<T> hasher = i == 0 ?  hash.hasher(serializer) : seeded.apply((random.nextLong()));
			if (domain.isPerfect(hasher)) return new Perfect<>(hasher, domain, random);
		}
		throw new PerfectionException("unable to find hash function after " + maxSeedAttempts);
	}
	
	
}
