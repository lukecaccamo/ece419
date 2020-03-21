package shared.hashring;

import ecs.ECSNode;
import java.util.*;

public class HashRing {
    public TreeMap<String, ECSNode> hashRing;

    public HashRing() {
        this.hashRing = new TreeMap<String, ECSNode>(new HashComparator());
    }

    public String toString() {
        return this.hashRing.values().toString();
    }

    public void clear() {
        this.hashRing.clear();
    }

    public TreeMap<String, ECSNode> getHashRing() {
        return hashRing;
    }

    public void setHashRing(TreeMap<String, ECSNode> hashRing) {
        this.hashRing = hashRing;
    }

    public void addServer(String hashIndex, ECSNode server){
        hashRing.put(hashIndex, server);
    }

    public void removeServer(String hashIndex){
        hashRing.remove(hashIndex);
    }

    public ECSNode getSucc(String hashIndex){
        String succKey = hashRing.higherKey(hashIndex);
        if (succKey == null){
            String firstIndex = hashRing.firstKey();
            if (hashIndex == firstIndex){
                return null;
            } else {
                return hashRing.get(firstIndex);
            }
        }
        return hashRing.get(succKey);
    }

    public ECSNode getPred(String hashIndex){
        String predKey = hashRing.lowerKey(hashIndex);
        if (predKey == null){
            String lastIndex = hashRing.lastKey();
            if (hashIndex == lastIndex){
                return null;
            } else {
                return hashRing.get(lastIndex);
            }
        }
        return hashRing.get(predKey);
    }

    public ECSNode serverLookup(String hashIndex){
        String key = hashRing.ceilingKey(hashIndex);
        if (key == null){
            key = hashRing.firstKey();
            if(key == null){
                return null;
            }
        }
        return hashRing.get(key);
    }

    public ECSNode getServer(String hashIndex){
        return hashRing.get(hashIndex);
    }

    public boolean inServer(String keyHash, String serverHash){
        String potentialServer = hashRing.ceilingKey(keyHash);
        if (potentialServer == null){
            potentialServer = hashRing.firstKey();
        }
        return potentialServer.compareTo(serverHash) == 0;
    }
}