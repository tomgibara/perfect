package com.tomgibara.perfect;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.Bits;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.permute.Permutation;
import com.tomgibara.storage.Store;

public class PerfectTest {

	@Test
	public void testSmall() {
		String[] words = { "Tom", "Astrid", "Joy", "Magnus", "Horse", "Cow", "Crow", "Spoon" };
		Minimal<String> minimal = Perfect.over(words).usingDefaults().maybePerfect().get().minimized();
		Hasher<String> hasher = minimal.getHasher();
		assertEquals(words.length, hasher.getSize().asInt());
		Set<Integer> ints = Bits.store(words.length).ones().asSet();
		for (String word : words) {
			if (!ints.add(hasher.intHashValue(word))) fail();
		}
	}

	@Test
	public void testBig() {
		Random r = new Random(0L);
		List<String> list = largeList(1000, 100000, r);

		Minimal<String> minimal = Perfect.over(list).usingDefaults().maybePerfect().get().minimized();
		minimal.getPermutation();
		Hasher<String> hasher = minimal.getHasher();
		BitMatches ones = Bits.store(list.size()).ones();
		Set<Integer> ints = ones.asSet();
		for (String word : list) {
			if (!ints.add(hasher.intHashValue(word))) fail();
		}
		assertTrue(ones.isAll());
	}

	@Test
	public void testStringHashing() {
		assertFalse( Perfect.over("FB", "Ea").isPerfect(Hashing.objectHasher()) );
		assertTrue( Perfect.over("FB", "Ea").isInjective((s,w) -> w.writeChars(s)) );
		assertTrue( Perfect.over("FB", "Ea").isInjective((s,w) -> w.writeChar(s.charAt(0))) );
		assertFalse( Perfect.over("Ant", "Bear", "Aardvark").isInjective((s,w) -> w.writeChar(s.charAt(0))) );

		Minimal<String> minimal = Perfect.over("FB", "Ea").usingDefaults().perfect((s,w) -> w.writeChars(s)).minimized();
		minimal.getPermutation();
		Hasher<String> hasher = minimal.getHasher();
		int ha = hasher.intHashValue("FB");
		int hb = hasher.intHashValue("Ea");
		assertTrue(ha == 0 || ha == 1);
		assertTrue(hb == 0 || hb == 1);
		assertFalse(ha == hb);
	}

	@Test
	public void testLargePerfect() {
		for (long seed = 0; seed < 10; seed++) {
			Random r = new Random(seed);
			List<String> list = largeList(10000, 100000000, r);
			assertTrue( Perfect.over(list).usingDefaults().maybePerfect().isPresent() );
		}

	}

	@Test
	public void testDomainCons() {
		PerfectDomain<Number> domain = Perfect.over(Integer.MIN_VALUE, Integer.MAX_VALUE);
		assertEquals(Number.class, domain.getType().get());
	}

	@Test
	public void testMinimalStorageStoreFirst() {
		testMinimalStorage(true);
	}

	@Test
	public void testMinimalStoragePermFirst() {
		testMinimalStorage(false);
	}

	private void testMinimalStorage(boolean storeFirst) {
		String[] words = { "Alice", "Bob", "Eve" };
		Perfect<String> perfect = Perfect.over(words).using(3, new Random(0L)).perfect((s, w) -> w.writeChars(s));
		Minimal<String> minimal = perfect.minimized();
		Hasher<String> mh = minimal.getHasher();
		Store<String> store;
		Permutation perm;
		if (storeFirst) {
			store = minimal.getStore().mutableCopy();
			perm = minimal.getPermutation();
		} else {
			perm = minimal.getPermutation();
			store = minimal.getStore().mutableCopy();
		}
		int size = words.length;
		for (int i = 0; i < size; i++) {
			assertEquals(i, mh.intHashValue(store.get(i)));
		}

		perm.inverse().permute(store);
		assertEquals(asList(words), store.asList());
	}

	@Test
	public void testSizes() {
		Random r = new Random(0L);
		List<String> large = largeList(1000, 1000000, r);
		for (int i = 4; i <= large.size(); i++) {
			List<String> list = new ArrayList<>(large.subList(0, i));
			Collections.shuffle(list, r);
			Minimal<String> minimal;
			try {
				minimal = Perfect.over(list).using(3, new SecureRandom()).perfect((s, w) -> w.writeChars(s)).minimizedWithBMZ(40, 1.15);
			} catch (PerfectionException e) {
				fail(i + " failed");
				return;
			}
			confirmMinimal(minimal, list);
		}
	}

	private List<String> largeList(int size, int range, Random r) {
		String[] strs = new String[size];
		for (int i = 0; i < strs.length; i++) {
			strs[i] = Integer.toString(r.nextInt(range), 10);
		}
		return asList(strs).stream().sorted().distinct().collect(Collectors.toList());
	}

	private void confirmMinimal(Minimal<String> minimal, List<String> list) {
		SortedSet<Integer> set = Bits.store(list.size()).zeros().asSet();
		Hasher<String> hasher = minimal.getHasher();
		for (String str : list) {
			int h = hasher.intHashValue(str);
			if (!set.remove(h)) Assert.fail("duplicate hash value: " + h);
		}
		assertTrue(set.isEmpty());
	}
}
