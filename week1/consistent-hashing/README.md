# Consistent Hashing Quick Explainer

This project is a small Java implementation of consistent hashing. The goal is
to show how keys can be assigned to nodes in a way that avoids reshuffling every
key whenever a node is added or removed.

## Mental Model

Think of the hash space as a circle, usually called the hash ring.

1. Every node is hashed onto the ring.
2. Every key is also hashed onto the same ring.
3. To find the owner of a key, move clockwise from the key's hash until you hit
   the next node.
4. That node owns the key.

If the search reaches the end of the sorted hash values, it wraps back to the
first value. That wraparound is what makes the sorted map behave like a circle.

## Why Consistent Hashing Helps

With a simple `hash(key) % numberOfNodes` strategy, adding or removing one node
changes the modulo value. That causes most keys to move.

With consistent hashing, only keys near the changed node move:

- When a node is removed, only keys owned by that node need to be reassigned.
- When a node is added, only keys that now fall into the new node's ring ranges
  move to it.

This is useful for distributed caches, sharded storage, load balancers, and
systems where moving data is expensive.

## Virtual Nodes

If each physical node appeared only once on the ring, key distribution could be
uneven. One node might accidentally own a very large section of the ring.

This implementation gives each physical node many virtual nodes:

```text
node 1_v_node_0
node 1_v_node_1
node 1_v_node_2
...
```

Each virtual node is hashed separately. All of those virtual positions still map
back to the same physical node. This spreads each physical node around the ring
and usually gives a more even key distribution.

## Code Map

- `HashFunction`: interface for converting a string into an integer hash.
- `MD5HashFunction`: stable hash implementation used by the demo.
- `Node`: physical node plus its list of virtual node names.
- `ConsistentHashRing`: stores `hash position -> Node` in a `TreeMap`.
- `findNodeForKey`: hashes a key and finds the next clockwise node.
- `keyToNodeMap`: demo-only map that records current key assignments.
- `addNode` / `removeNode`: update the ring and remap affected keys.
- `printKeyDistribution`: shows how many keys ended up on each node.

## Demo Flow

The `main` method does this:

1. Creates an MD5 hash function.
2. Creates a consistent hash ring with `1000` virtual nodes per physical node.
3. Creates five physical nodes.
4. Adds those nodes to the ring.
5. Generates sample keys.
6. Assigns every key to a node.
7. Prints the initial key distribution.
8. Removes `node 2` and analyzes how many keys moved.
9. Adds `node 6` and analyzes how many keys moved to the new node.
10. Prints the final distribution.

## Important Detail

The `TreeMap` is the key data structure:

```java
hashRing.ceilingEntry(keyHash)
```

This finds the first ring position greater than or equal to the key's hash. If
there is no such position, the code uses:

```java
hashRing.firstEntry()
```

That is the wraparound behavior.

## Running

From this directory:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="week1.consistentHashing"
```

If the `exec:java` command is not available, run it from your IDE by selecting
the `main` method in `src/main/java/week1/consistentHashing.java`.
