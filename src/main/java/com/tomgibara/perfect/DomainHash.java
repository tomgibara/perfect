package com.tomgibara.perfect;

import com.tomgibara.collect.Equivalence;
import com.tomgibara.hashing.Hasher;

/**
 * A hash over a specific domain of possible values. This class serves as the
 * base class of {@link Perfect} and {@link Minimal} hashes. The hash is not
 * necessarily defined outside of this domain, and any declared properties may
 * not hold.
 *
 * @author Tom Gibara
 *
 * @param <T>
 *            the type of values over which the hash is defined
 */
public class DomainHash<T> {

	final Hasher<T> hasher;
	final PerfectDomain<T> domain;
	Equivalence<T> equ = null;

	DomainHash(Hasher<T> hasher, PerfectDomain<T> domain) {
		this.hasher = hasher;
		this.domain = domain;
	}

	/**
	 * A hash that is perfect over its domain.
	 *
	 * @return a perfect hasher
	 */
	public Hasher<T> getHasher() {
		return hasher;
	}

	/**
	 * The domain over which the hash returned by {@link #getHasher()} is
	 * guaranteed to be perfect.
	 *
	 * @return the domain of values
	 */
	public PerfectDomain<T> getDomain() {
		return domain;
	}

	/**
	 * An equivalence relation derived from the perfect hash. This provides an
	 * definition of equivalence over the values in the domain that is
	 * guaranteed to be consistent with the perfect hash; two values being
	 * considered equal if their hash codes are equal.
	 *
	 * @return an equivalence relation consistent with the perfect hash
	 */
	public Equivalence<T> getEquivalence() {
		if (equ == null) {
			equ = new Equivalence<T>() {
				@Override public boolean isEquivalent(T e1, T e2) { return e1 == e2 || hasher.intHashValue(e1) == hasher.intHashValue(e2); };
				@Override public Hasher<T> getHasher() { return hasher; }
			};
		}
		return equ;
	}

}
