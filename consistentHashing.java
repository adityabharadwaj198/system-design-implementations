import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*; 

public class consistentHashing {

    public interface HashFunction {
        int hash(String key); 
    }

    public class simpleHashFunction implements HashFunction {
        public int hash (String key) {
            return key.hashCode();
        }
    }

    public class MD5HashFunction implements HashFunction {
            public int hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(key.getBytes());
            return Math.abs(ByteBuffer.wrap(hashBytes).getInt());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
    }

        

    public class Node {
        String id; 
        List<String> virtualNodes; 

        public Node (String id, int virtualNodeCount) {
            this.id = id;
            this.virtualNodes = new ArrayList<>();
            for (int i=0; i<virtualNodeCount; i++) {
                virtualNodes.add(id + "_v_node_" + Integer.toString(i));
            }
        }
    }

    public class ConsistentHashRing {
        TreeMap<Integer, Node> hashRing; // actual ring. 
        HashFunction hashFunction;
        private int virtualNodeCount; 

        public ConsistentHashRing(HashFunction hf, int virtualNodeCount) {
            this.hashRing = new TreeMap<>();
            this.hashFunction = hf;
            this.virtualNodeCount = virtualNodeCount;
        }

        public void addNode (Node node) {
            for (int i=0; i<this.virtualNodeCount; i++) {
                hashRing.put(hashFunction.hash(node.virtualNodes.get(i)), node);
            }
        }

        public void removeNode (Node node) {
            for (int i=0; i<node.virtualNodes.size(); i++) {
                if (hashRing.containsKey(hashFunction.hash(node.virtualNodes.get(i)))) {
                    hashRing.remove(hashFunction.hash(node.virtualNodes.get(i)));
                }
            }
        }

        public Node findNodeForKey(String key) {
            int keyHash = hashFunction.hash(key);
            if (hashRing.ceilingEntry(keyHash) == null) { // if there's no higher value, return the first value to 
            // signify that the ring is circular. 
                return hashRing.firstEntry().getValue();
            }
            return hashRing.ceilingEntry(keyHash).getValue();
        } 
    }    

    Map<String, Node> keyToNodeMap = new HashMap<>();

    public void mapKeyToNode(ConsistentHashRing chr, String key) {
        Node node = chr.findNodeForKey(key);
        keyToNodeMap.put(key, node);
    }

    public void remapKeysToNewNode(ConsistentHashRing chr, List<String> keys) {
        for (String k: keys) {
            Node node = chr.findNodeForKey(k);
            keyToNodeMap.put(k, node);
        }
    }

    public void removeNode(ConsistentHashRing chr, Node node) {
        chr.removeNode(node);
        List<String> keysOnNode = findKeysOnANode(node);
        System.out.println("Remapped " + keysOnNode.size() + " keys");
        for (String s: keysOnNode) {
            remapKeysToNewNode(chr, keysOnNode);
        }
    }

    public void addNode(ConsistentHashRing chr, Node node) {
        chr.addNode(node);
        int numberOfRemappedKeys = 0;
        for (String k: keyToNodeMap.keySet()) {
            if (chr.findNodeForKey(k) != keyToNodeMap.get(k)) {
                System.out.println("Will need to remap this key");
                numberOfRemappedKeys++;
                keyToNodeMap.put(k, chr.findNodeForKey(k));
            }
        }
        if (numberOfRemappedKeys > 0)
            System.out.println("Remapped " + numberOfRemappedKeys + " keys");
    }

    public List<String> findKeysOnANode(Node n) {
        List<String> answer = new ArrayList<>();
        for (String s: keyToNodeMap.keySet()) {
            if (keyToNodeMap.get(s) == n) {
                answer.add(s);
            }
        }
        return answer;
    }

    // private static List<String> generateSampleKeys(int count) {
    //     List<String> keys = new ArrayList<>();
    //     for (int i = 0; i < count; i++) {
    //         keys.add("key_" + UUID.randomUUID());
    //     }

    //     return keys;
    // }

    private static void printKeyDistribution(consistentHashing ch) {
        // Count keys per node
        Map<String, Integer> nodeKeyCounts = new HashMap<>();
        
        for (Map.Entry<String, consistentHashing.Node> entry : 
             ch.keyToNodeMap.entrySet()) {
            String nodeId = entry.getValue().id;
            nodeKeyCounts.put(
                nodeId, 
                nodeKeyCounts.getOrDefault(nodeId, 0) + 1
            );
        }
        
        // Print distribution
        System.out.println("Key Distribution:");
        for (Map.Entry<String, Integer> entry : nodeKeyCounts.entrySet()) {
            System.out.printf("%s: %d keys (%.2f%%)%n", 
                entry.getKey(), 
                entry.getValue(), 
                (entry.getValue() * 100.0 / ch.keyToNodeMap.size())
            );
        }
    }
        // Generate more diverse keys
        private static List<String> generateSampleKeys(int count) {
            List<String> keys = new ArrayList<>();
            Random random = new Random();
            
            // Generate diverse keys
            for (int i = 0; i < count; i++) {
                // Mix of different key types
                switch(random.nextInt(4)) {
                    case 0: 
                        keys.add("user_" + i);
                    case 1:
                        keys.add("product_" + UUID.randomUUID());
                    case 2:
                        keys.add("order_" + System.currentTimeMillis() + "_" + i);
                    case 3:
                        keys.add(UUID.randomUUID().toString());
                }
            }
            return keys;
        }
    
        private static Map<String, Integer> getKeyDistribution(consistentHashing ch) {
            Map<String, Integer> nodeKeyCounts = new HashMap<>();
            
            for (Map.Entry<String, consistentHashing.Node> entry : 
                 ch.keyToNodeMap.entrySet()) {
                String nodeId = entry.getValue().id;
                nodeKeyCounts.put(
                    nodeId, 
                    nodeKeyCounts.getOrDefault(nodeId, 0) + 1
                );
            }
            return nodeKeyCounts;
        }
    
            // Print detailed distribution
            private static void printDetailedKeyDistribution(consistentHashing ch) {
                Map<String, Integer> nodeKeyCounts = getKeyDistribution(ch);
                
                System.out.println("Key Distribution:");
                int totalKeys = ch.keyToNodeMap.size();
                
                for (Map.Entry<String, Integer> entry : nodeKeyCounts.entrySet()) {
                    System.out.printf("%s: %d keys (%.2f%%)%n", 
                        entry.getKey(), 
                        entry.getValue(), 
                        (entry.getValue() * 100.0 / totalKeys)
                    );
                }
            }
        
                // Analyze key movement
        private static void analyzeKeyMovement(
            Map<String, Integer> beforeRemoval, 
            Map<String, Integer> afterRemoval,
            List<String> allKeys,
            Node removedNode
        ) {
            System.out.println("\nDetailed Redistribution Analysis:");
            
            // Print before and after distribution
            System.out.println("Before Removal:");
            beforeRemoval.forEach((node, count) -> 
                System.out.printf("%s: %d keys%n", node, count));
            
            System.out.println("\nAfter Removal:");
            afterRemoval.forEach((node, count) -> 
                System.out.printf("%s: %d keys%n", node, count));
            
            // Percentage of keys moved
            int totalKeys = allKeys.size();
            int expectedMovedKeys = beforeRemoval.get(removedNode.id);
            
            System.out.printf("\nTotal Keys: %d%n", totalKeys);
            System.out.printf("Keys on Removed Node: %d%n", expectedMovedKeys);
            System.out.printf("Percentage of Keys Moved: %.2f%%%n", 
                (expectedMovedKeys * 100.0 / totalKeys)
            );
        }
    
    public static void main (String[] args) {
        consistentHashing ch = new consistentHashing(); 
        HashFunction hf = ch.new MD5HashFunction(); // create hash function. 

        ConsistentHashRing chr = ch.new ConsistentHashRing(hf, 1000); // set virtual node count to 1000, This will ensure that every node get's a 1000 virtual nodes to ensure 
        // better key distribution. Since there will be a 1000 virtual node corresponding to each node, you can be assured that the node (ex: Node1) will be distributed almost evenly all across the hash ring. 
        // This will ensure that the keys are distributed evenly. 

        // Observation. If you change the virtual node count to 100, 
        /* 
        * Key Distribution:
            node 1: 5101 keys (20.41%)
            node 2: 4080 keys (16.33%)
            node 3: 4598 keys (18.40%)
            node 4: 5440 keys (21.77%)
            node 5: 5772 keys (23.10%)

            --- Detailed Node Removal Analysis ---

            --- Removing a node ---
            Remapped 4080 keys

            Detailed Redistribution Analysis:
            Before Removal:
            node 1: 5101 keys
            node 2: 4080 keys
            node 3: 4598 keys
            node 4: 5440 keys
            node 5: 5772 keys

            After Removal:
            node 1: 5998 keys
            node 3: 5617 keys
            node 4: 6105 keys
            node 5: 7271 keys

            Total Keys: 24991
            Keys on Removed Node: 4080
            Percentage of Keys Moved: 16.33%
            Key Distribution:
            node 1: 5998 keys (24.00%)
            node 3: 5617 keys (22.48%)
            node 4: 6105 keys (24.43%)
            node 5: 7271 keys (29.09%)
         */

         /* results with virtual node count = 1000. Notice that this is almost evenly distributed across all nodes. 
            Key Distribution:
            node 1: 5064 keys (20.20%)
            node 2: 4796 keys (19.13%)
            node 3: 4802 keys (19.16%)
            node 4: 5170 keys (20.63%)
            node 5: 5234 keys (20.88%)

            --- Detailed Node Removal Analysis ---

            --- Removing a node ---
            Remapped 4796 keys

            Detailed Redistribution Analysis:
            Before Removal:
            node 1: 5064 keys
            node 2: 4796 keys
            node 3: 4802 keys
            node 4: 5170 keys
            node 5: 5234 keys

            After Removal:
            node 1: 6412 keys
            node 3: 6065 keys
            node 4: 6220 keys
            node 5: 6369 keys

            Total Keys: 25066
            Keys on Removed Node: 4796
            Percentage of Keys Moved: 19.13%
            Key Distribution:
            node 1: 6412 keys (25.58%)
            node 3: 6065 keys (24.20%)
            node 4: 6220 keys (24.81%)
            node 5: 6369 keys (25.41%)
          */
        Node n1 = ch.new Node("node 1", 1000);
        Node n2 = ch.new Node("node 2", 1000);
        Node n3 = ch.new Node("node 3", 1000);
        Node n4 = ch.new Node("node 4", 1000);
        Node n5 = ch.new Node("node 5", 1000);

        ch.addNode(chr, n1);
        ch.addNode(chr, n2);
        ch.addNode(chr, n3);
        ch.addNode(chr, n4);
        ch.addNode(chr, n5);

        List<String> sampleKeys = generateSampleKeys(10000);
        for (String key : sampleKeys) {
            ch.mapKeyToNode(chr, key);
        }
        // Print initial key distribution
        printKeyDistribution(ch);

        System.out.println("\n--- Detailed Node Removal Analysis ---");
        // Track keys before removal
        Map<String, Integer> beforeRemoval = getKeyDistribution(ch);


        System.out.println("\n--- Removing a node ---");
        ch.removeNode(chr, n2);

        // Track keys after removal
        Map<String, Integer> afterRemoval = getKeyDistribution(ch);

                
        // Detailed analysis of key movement
        analyzeKeyMovement(beforeRemoval, afterRemoval, sampleKeys, n2);

        printKeyDistribution(ch);

    }

        


}
