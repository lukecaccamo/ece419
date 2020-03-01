package shared.hashring;

import ecs.IECSNode;
import shared.messages.KVAdminMessage;
import shared.messages.IKVAdminMessage.ActionType;
import java.util.*;

public class HashRing {
    // map start index to IECSNode
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

    // this will be called by ecs
    public void addServer(String hashIndex, IECSNode server){
        hashRing.put(hashIndex, server);
    }

    // this will be called by ecs
    // could probably have overloaded fn that takes in IECSNode objec instead
    public void removeServer(String hashIndex){
        hashRing.remove(hashIndex);
    }

    // this will be called by server, server has info on their own ip and port
    // maybe better to pass in server ip and port instead, better performance wise
    public IECSNode getSucc(String hashIndex){
        // find current one, need current server start index
        String succKey = hashRing.higherKey(hashIndex);
        if (succKey == null){
            // first or only
            String firstIndex = hashRing.firstKey();
            if (hashIndex == firstIndex){
                // first and succ is null, only server
                return null;
            } else {
                // last item, loop back to first server
                return hashRing.get(firstIndex);
            }
        }
        return hashRing.get(succKey);
    }

    public IECSNode getPred(String hashIndex){
        String predKey = hashRing.lowerKey(hashIndex);
        if (predKey == null){
            // last or only
            String lastIndex = hashRing.lastKey();
            if (hashIndex == lastIndex){
                // first and succ is null, only server
                return null;
            } else {
                // last item, loop back to first server
                return hashRing.get(lastIndex);
            }
        }
        return hashRing.get(predKey);
    }

    // client calls this
    public IECSNode serverLookup(String hashIndex){
        String key = hashRing.ceilingKey(hashIndex);
        if (key == null){
            // either wrap around to first server or no servers currently
            key = hashRing.firstKey();
            if(key == null){
                return null;
            }
        }
        return hashRing.get(key);
    }

    // server calls this to check
    public boolean inServer(String keyHash, String serverHash){
        String potentialServer = hashRing.ceilingKey(keyHash);
        if (potentialServer == null){
            // wrap around
            potentialServer = hashRing.firstKey();
        }
        return potentialServer == serverHash;
    }
}