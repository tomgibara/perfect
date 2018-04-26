package com.tomgibara.perfect;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.BitVector;
import com.tomgibara.bits.Operation;
import com.tomgibara.hashing.HashCode;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;
import com.tomgibara.streams.StreamSerializer;

// BMZ implementation based on:
// http://remis-thoughts.blogspot.co.uk/2012/03/perfect-hashes-in-java-given-set-of-m.html

final class BMZ<E> {

	// statics

	private static final Storage<Long> storage = StoreType.of(long.class).settingNullToDefault().storage();

	// static helper methods

	private static int a(long ab) {
		return (int) (ab >> 32);
	}

	private static int b(long ab) {
		return (int) ab;
	}

	private static long ab(int a, int b) {
		return (long) a << 32 | 0xffffffffL & b;
	}

	// fields

	private final Random random;
	private final Hasher<E> hasher;
	private final int maxTries;
	private final double c;

	// constructors

	BMZ(Hasher<E> hasher, int maxTries, double c, Random random) {
		this.hasher = hasher;
		this.maxTries = maxTries;
		this.c = c;
		this.random = random;
	}

	Hasher<E> create(Collection<? extends E> elements) {
		long max = (long) Math.ceil(c * elements.size());
		if (max > Integer.MAX_VALUE) throw new IllegalArgumentException("elements too large");
		int[] g = new int[(int) max];

		for (int tries = 0; tries < maxTries; tries++) {
			int seed1 = random.nextInt();
			int seed2 = random.nextInt();
			BMZHasher<E> bmz = new BMZHasher<>(hasher, seed1, seed2, g, elements.size());

			Graph graph = bmz.computeGraph(elements);
			if (graph == null) continue; // duplicate edge detected

			boolean assigned = graph.newAssigner(g).assignIntegersToVertices();
			if (!assigned) continue; // failed to assign to critical vertices

			return bmz;
		}
		throw new PerfectionException("failed to find minimal hash");
	}


	// inner classes

	private static class BMZHasher<E> implements Hasher<E> {

		private static final StreamSerializer<Integer> ser =  (i, w) -> w.writeInt(i);

		private final Hasher<E> hasher;
		private final int[] g;
		private final HashSize size;

		private final Hasher<Integer> hasher1;
		private final Hasher<Integer> hasher2;

		BMZHasher(Hasher<E> hasher, int seed1, int seed2, int[] g, int size) {
			this.hasher = hasher;
			this.g = g;
			this.size = HashSize.fromInt(size);
			HashSize vertices = HashSize.fromInt(g.length);
			hasher1 = Hashing.murmur3Int(seed1).hasher(ser).sized(vertices);
			hasher2 = Hashing.murmur3Int(seed2).hasher(ser).sized(vertices);
		}

		public HashSize getSize() {
			return size;
		}

		public HashCode hash(E e) throws IllegalArgumentException {
			long ab = computeEdge(e);
			int hash = g[a(ab)] + g[b(ab)];
			return HashCode.fromInt(hash);
		}

		// returns null if the graph cannot be computed
		Graph computeGraph(Collection<? extends E> elements) {
			Graph graph = new Graph(g.length, elements.size());
			int index = 0;
			for (E element : elements) {
				if (!graph.setEdge(index++, computeEdge(element))) return null;
			}
			return graph;
		}

		private long computeEdge(E e) {
			int n = g.length;
			int hc = hasher.intHashValue(e);
			int h1 = hasher1.intHashValue(hc);
			int h2 = hasher2.intHashValue(hc);
			// this is necessary to avoid loops in the graph
			if (h1 == h2) h2 = (h2 == n - 1) ? 0 : h2 + 1;
			return ab(h1, h2);
		}

	}

	private static final class Graph {

		// the number of vertices
		final int n;
		// the number of edges
		final int m;
		// the two vertices are packed into a long
		final Store<Long> edges;
		// indexed by vertex, holds list of vertices that vertex is connected to
		final List<Integer>[] adjacencyList;

		@SuppressWarnings("unchecked")
		Graph(int n, int m) {
			assert(m <= n);
			this.n = n;
			this.m = m;
			edges = storage.newStore(m);
			adjacencyList = new List[n];
		}

		// true if the edge was added, false if it was a duplicate
		boolean setEdge(int index, long e) {
			int a = a(e);
			int b = b(e);
			if (getAdjacencyList(a).contains(b)) return false;
			edges.set(index, e);
			getAdjacencyList(a).add(b);
			getAdjacencyList(b).add(a);
			return true;
		}

		Assigner newAssigner(int[] g) {
			return new Assigner(g);
		}

		// private utility methods

		private List<Integer> getAdjacencyList(int forVertex) {
			List<Integer> ret = adjacencyList[forVertex];
			return ret == null ? (adjacencyList[forVertex] = new LinkedList<>()) : ret;
		}

		// inner inner classes

		private class Assigner {

			// values assigned to each of the edges (has length m)
			private final int[] g;
			// those nodes that can't be linearized
			//(ie. have degree greater than 2 or are in cycles)
			private final BitVector criticalNodes;
			// records the edges that have been assigned a value
			private final BitVector assignedEdges;

			Assigner(int[] g) {
				this.g = g;
				assert(g.length == n);
				assignedEdges = new BitVector(m);
				criticalNodes = findCriticalNodes();
			}

			boolean assignIntegersToVertices() {
				if (!assignIntegersToCriticalVertices()) return false;
				assignIntegersToNonCriticalVertices();
				return true;
			}

			private BitVector findCriticalNodes() {
				// calculate node degrees...
				int[] degrees = new int[n];
				for (Long edge : edges.asList()) {
					degrees[a(edge)] ++;
					degrees[b(edge)] ++;
				}

				// ...and trim the chains...
				List<Integer> degreeOne = new LinkedList<>();
				for (int i = 0; i < n; ++i) {
					if (degrees[i] == 1) degreeOne.add(i);
				}
				while (!degreeOne.isEmpty()) {
					int v = degreeOne.remove(0);
					degrees[v] --;
					for (int adjacent : adjacencyList[v]) {
						if (--degrees[adjacent] == 1) degreeOne.add(adjacent);
					}
				}

				// ...and return a bitmap of critical vertices
				BitVector ret = new BitVector(n);
				for (int i = 0; i < n; ++i) if (degrees[i] > 1) ret.setBit(i, true);
				return ret;
			}

			/** @returns false if we couldn't assign the integers */
			private boolean assignIntegersToCriticalVertices() {
				int x = 0;
				List<Integer> toProcess = new LinkedList<>();
				BitVector assignedNodes = new BitVector(g.length);
				while (!assignedNodes.equals(criticalNodes)) {
					BitStore unprocessed = Operation.AND.stores(criticalNodes, assignedNodes.flipped());
					toProcess.add(unprocessed.ones().first()); // start at the lowest unassigned critical vertex
					// assign another "tree" of vertices - not all critical ones are necessarily connected!
					x = processCriticalNodes(toProcess, x, assignedNodes);
					if(x < 0) return false; // x is overloaded as a failure signal
				}
				return true;
			}

			/** process a single "tree" of connected critical nodes, rooted at the vertex in toProcess */
			private int processCriticalNodes(List<Integer> toProcess, int x, BitVector assignedNodes) {
				while(!toProcess.isEmpty()) {
					int v = toProcess.remove(0);
					if(v < 0 || assignedNodes.getBit(v)) continue; // there are no critical nodes || already done this vertex
					if(adjacencyList[v] != null) {
						x = getXThatSatifies(adjacencyList[v], x, assignedNodes);
						for (int adjacent : adjacencyList[v]) {
							if(!assignedNodes.getBit(adjacent) && criticalNodes.getBit(adjacent) && v!= adjacent) {
								// give this one an integer, & note we shouldn't have loops - except if there is one key
								toProcess.add(adjacent);
							}
							if(assignedNodes.getBit(adjacent)) {
								int edgeXtoAdjacent = x + g[adjacent]; // if x is ok, then this edge is now taken
								if(edgeXtoAdjacent >= edges.size()) return -1; // this edge is too big! we're only assigning between 0 & m-1
								assignedEdges.setBit(edgeXtoAdjacent, true);
							}
						}
					}
					g[v] = x; assignedNodes.setBit(v, true); // assign candidate x to g
					++x; // next v needs a new candidate x
				}
				return x; // will use this as a candidate for other "trees" of critical vertices
			}

			private void assignIntegersToNonCriticalVertices() {
				List<Integer> toProcess = new LinkedList<>( criticalNodes.ones().asSet() );
				BitVector visited = criticalNodes.clone();
				processNonCriticalNodes(toProcess, visited); // process the critical nodes
				// we've done everything reachable from the critical nodes - but
				// what about isolated chains?
				for (Positions positions = visited.zeros().positions(); positions.hasNext(); ) {
					toProcess.add(positions.nextPosition());
					processNonCriticalNodes(toProcess, visited);
				}
			}

			/** process everything in the list and all vertices reachable from it */
			private void processNonCriticalNodes(List<Integer> toProcess, BitVector visited) {
				BitMatches zeros = assignedEdges.zeros();
				int nextEdge = zeros.first();
				while(!toProcess.isEmpty()) {
					int v = toProcess.remove(0);
					if(v < 0) continue; // there are no critical nodes
					if(adjacencyList[v] != null) {
						for(int adjacent : adjacencyList[v]) {
							if(!visited.getBit(adjacent) && v != adjacent) { // shouldn't have loops - only if one key
								// we must give it a value
								g[adjacent] = nextEdge - g[v]; // i.e. g[v] + g[a] = edge as needed
								toProcess.add(adjacent);
								assignedEdges.setBit(nextEdge, true);
								nextEdge = zeros.next(nextEdge + 1);
							}
						}
					}
					visited.setBit(v, true);
				}
			}

			private int getXThatSatifies(List<Integer> adjacencyList, int x, BitVector assignedNodes) {
				for(int adjacent : adjacencyList) {
					if (assignedNodes.getBit(adjacent) /*only covers critical nodes*/) {
						int index = g[adjacent] + x;
						if (index >= 0 && index < assignedEdges.size() && assignedEdges.getBit(index)) {
							// if we assign x to v, then the edge between v & and 'adjacent' will
							// be a duplicate - so our hash code won't be perfect! Try again with a new x:
							return getXThatSatifies(adjacencyList, x + 1, assignedNodes);
						}
					}
				}
				return x; // this one satisfies all edges
			}

		}

	}

}
