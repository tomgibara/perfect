package com.tomgibara.perfect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map.Entry;

import org.junit.Test;

public class MinimalMapTest {

	@Test
	public void testMap() {
		Minimal<String> animals = Perfect.over("ostrich", "dog", "snail", "centipede").usingDefaults().maybePerfect().get().minimized();
		Minimal<String>.Maps<Integer> counts = animals.withNullableTypedStorage(int.class);
		MinimalMap<String, Integer> legs = counts.newMap();
		assertNull( legs.get("ostrich") );
		legs.put("ostrich", 2);
		assertEquals((Integer)2, legs.get("ostrich"));
		try {
			legs.put("whippet", 3);
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		legs.put("dog", 3);
		for (Entry<String,Integer> entry : legs.entrySet()) {
			switch (entry.getKey()) {
			case "dog" : entry.setValue(4); continue;
			case "ostrich": continue;
			default: fail();
			}
		}
		assertEquals((Integer) 4, legs.get("dog"));
		assertEquals(2, legs.keySet().size());
		legs.remove("dog");
		assertEquals(1, legs.size());
		assertNull(legs.get("dog"));
		legs.remove("whippet");
		legs.putAll(Collections.singletonMap("snail", 1));
		assertEquals((Integer) 1, legs.get("snail"));
		assertEquals(2, legs.size());
	}
	
}
