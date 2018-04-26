package com.tomgibara.perfect;

import java.util.Set;

import com.tomgibara.bloom.Bloom;
import com.tomgibara.bloom.BloomConfig;
import com.tomgibara.bloom.BloomSet;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.Equivalence;

class UniquenessChecker<T> {

	private static final double LOG_2 = Math.log(2);
	private static final int BLOOM_MIN_SIZE = 256;

	private final BloomConfig<T> config;
	private final Sets<T> sets;

	UniquenessChecker(long expectedObjectCount, double averageObjectSizeInBytes, Equivalence<T> equ, Class<T> type) {
		if (expectedObjectCount < 0) throw new IllegalArgumentException("null expectedObjectCount");
		if (averageObjectSizeInBytes <= 0.0) throw new IllegalArgumentException("non-positive averageObjectSizeInBytes");
		if (equ == null) throw new IllegalArgumentException("null equ");

		double bitsPerObject = 8.0 * averageObjectSizeInBytes;
		double optimalBloomSize = expectedObjectCount * Math.log( bitsPerObject * LOG_2 * LOG_2 ) / LOG_2;
		int bloomSize = Math.max(BLOOM_MIN_SIZE, (int) Math.min(Integer.MAX_VALUE, optimalBloomSize));
		int hashCount = Math.max(1, Math.round( (float) LOG_2 * bloomSize / expectedObjectCount) );
		config = new BloomConfig<>(bloomSize, equ.getHasher().ints(), hashCount);

		sets = (type == null ? Collect.<T>sets() : Collect.setsOf(type)).underEquivalence(equ);
	}

	UniquenessChecker(long expectedObjectCount, double averageObjectSizeInBytes) {
		this(expectedObjectCount, averageObjectSizeInBytes, Equivalence.equality(), null);
	}

	boolean check(Iterable<T> iterable) {
		Set<T> candidates = sets.newSet();
		{ // first pass
			BloomSet<T> filter = Bloom.withConfig(config).newSet();
			// first pass: any elements which are dupes in the bloom filter are recorded as candidate
			// any candidate that occurs twice must be a dupe
			for (T value : iterable) {
				if (!filter.add(value) && !candidates.add(value)) {
					return false;
				}
			}
		}
		{ // second pass
			Set<T> witnesses = sets.newSet();
			// second pass: every candidate is recorded again as a witness
			// any witness that occurs twice is a dupe
			for (T value : iterable) {
				if (candidates.contains(value) && !witnesses.add(value)) {
					return false;
				}
			}
		}
		return true;
	}

}
