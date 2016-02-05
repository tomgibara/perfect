package com.tomgibara.perfect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class MinimalSetTest {

	@Test
	public void testSet() {
		MinimalSet<String> mammals = Perfect.over("cat", "dog", "cow", "horse").usingDefaults().maybePerfect().get().minimized().newSet();
		mammals.fill();
		assertEquals(4, mammals.size());
		assertTrue(mammals.contains("dog"));
		assertFalse(mammals.contains("ant"));
		Set<String> set = new HashSet<String>();
		for (String mammal : mammals) {
			set.add(mammal);
		}
		assertEquals(4, set.size());
		assertEquals(mammals, set);
		assertEquals(set, mammals);
		assertEquals(mammals.hashCode(), set.hashCode());
		for (Iterator<String> i = mammals.iterator(); i.hasNext(); ) {
			String mammal = i.next();
			if (mammal.equals("dog")) {
				i.remove();
			}
		}
		assertEquals(3, mammals.size());
		assertFalse(mammals.contains("dog"));
		mammals.remove("cow");
		assertEquals(2, mammals.size());
		assertFalse(mammals.contains("cow"));
		mammals.remove("ant");
		assertEquals(2, mammals.size());
		try {
			mammals.add("ant");
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
	}

	public void testSetMutability() {
		MinimalSet<String> set = Perfect.over("black", "white").usingDefaults().maybePerfect().get().minimized().newSet();
		MinimalSet<String> imm = set.immutableView();
		assertTrue(imm.isEmpty());
		set.add("black");
		assertFalse(imm.isEmpty());
		try {
			imm.add("white");
			fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
		set.add("white");
		assertTrue(set.isFull());
		MinimalSet<String> cpy = set.mutableCopy();
		cpy.clear();
		assertTrue(set.isFull());
	}


}
