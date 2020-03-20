package shared.hashring;

import ecs.IECSNode;
import java.util.*;

public class HashRing {
    public TreeMap<String, IECSNode> hashRing;

    public HashRing() {
        this.hashRing = new TreeMap<String, IECSNode>(new HashComparator());
    }

    public void clear() {
        this.hashRing.clear();
    }

    public TreeMap<String, IECSNode> getHashRing() {
        return hashRing;
    }

    public void setHashRing(TreeMap<String, IECSNode> hashRing) {
        this.hashRing = hashRing;
    }

    public void addServer(String hashIndex, IECSNode server){
        hashRing.put(hashIndex, server);
    }

    public void removeServer(String hashIndex){
        hashRing.remove(hashIndex);
    }

    public IECSNode getSucc(String hashIndex){
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

    public IECSNode getPred(String hashIndex){
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

    public IECSNode serverLookup(String hashIndex){
        String key = hashRing.ceilingKey(hashIndex);
        if (key == null){
            key = hashRing.firstKey();
            if(key == null){
                return null;
            }
        }
        return hashRing.get(key);
    }

    public IECSNode getServer(String hashIndex){
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