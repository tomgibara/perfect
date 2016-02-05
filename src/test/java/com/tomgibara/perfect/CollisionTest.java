package com.tomgibara.perfect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;

public class CollisionTest {

	public static void main(String[] args) {
		Hasher<Integer> hasher = Hashing.murmur3Int().hasher((i, s) -> s.writeInt(i));
		hasher = hasher.sized(HashSize.fromBitLength(31));
		int count = 1 << 8;
		int[] hashes = new int[count];
		Random r = new Random(0L);
		for (int i = 0; i < count; i++) {
			hashes[i] = hasher.intHashValue(r.nextInt());
		}
//		Set<Integer> set = new HashSet<Integer>();
//		for (int hash : hashes) {
//			set.add(hash);
//		}
//		System.out.println(count - set.size());
		Arrays.sort(hashes);
		int[] uniques = new int[count];
		for (int i = 0; i < hashes.length - 1; i++) {
			int h1 = hashes[i];
			int h2 = hashes[i + 1];
			int d = h1 ^ h2;
			int c = Integer.numberOfLeadingZeros(d);
			System.out.println(c);
			int m = -1 >> (c + 1);
			int u = h1 & m;
			uniques[i] = u;
		}
		
		int[] errors = new int[count];
		for (int i = 0; i < count; i++) {
			long x = (1L << 24) * i / (count - 1);
			errors[i] = uniques[i] - (int) x;
		}

		for (int i = 0; i < count; i++) {
			System.out.println(i + "," + uniques[i] + "," + errors[i]);
		}

	}
	
}
