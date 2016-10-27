package com.tomgibara.perfect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.IntStream;

import org.junit.Test;

import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreNullity;

public class PerfectionTest {

	@Test
	public void testCompact() {
		Hasher<Integer> h = Hashing.<Integer>objectHasher().sized(HashSize.SHORT_SIZE);

		{
			IntStream stream = IntStream.range(0, 1 << PerfectDomain.COMPACT_BIT_CUTOFF);
			Iterable<Integer> iterable = stream::iterator;
			assertTrue( Perfect.over(iterable).isPerfect(h) );
		}
		
		{
			IntStream stream = IntStream.rangeClosed(0, 1 << PerfectDomain.COMPACT_BIT_CUTOFF);
			Iterable<Integer> iterable = stream::iterator;
			assertFalse( Perfect.over(iterable).isPerfect(h) );
		}
	}
	
	@Test
	public void testLarge() {
		Hasher<Long> h = Hashing.<Long>objectHasher();
		StoreNullity<Long> nullity = StoreNullity.settingNullToValue(0L);

		{
			int size = 1000000;
			Store<Long> store = Storage.typed(long.class, nullity).newStore(size);
			for (int i = 0; i < size; i++) {
				store.set(i, (long) i);
			}
			assertTrue( Perfect.over(store.asList()).isPerfect(h) );
		}

		{
			int size = 100000;
			Store<Long> store = Storage.typed(long.class, nullity).newStore(size);
			for (int i = 0; i < size; i++) {
				store.set(i, (long) i);
			}
			store.set(size - 1, 0x1000000000L);
			assertFalse( Perfect.over(store.asList()).isPerfect(h) );
		}
	}
}
