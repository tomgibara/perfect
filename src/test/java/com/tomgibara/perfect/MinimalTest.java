package com.tomgibara.perfect;

import java.util.Arrays;
import java.util.function.IntFunction;

import org.junit.Assert;
import org.junit.Test;

import com.tomgibara.permute.Permutation;
import com.tomgibara.permute.Permute;

public class MinimalTest {

	@Test
	public void testPermutation() {
		String[] animals = { "Dog", "Cat", "Horse", "Goat", "Llama" };
		Minimal<String> minimal = Perfect.over(animals).usingDefaults().maybePerfect().get().minimized();
		Permutation perm = minimal.getPermutation();
		Object[] permuted = Permute.objects(animals.clone()).apply(perm).permuted();
		IntFunction<Object> image = i -> permuted[minimal.getHasher().hash(animals[i]).intValue()];
		for (int i = 0; i < animals.length; i++) {
			Assert.assertEquals(animals[i], image.apply(i));
		}
	}
}
