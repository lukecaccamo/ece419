package app_kvServer.KVCache;

import java.util.*;

public class LFUCache implements IKVCache{

    private int cacheSize;
    private int min = -1;

    // map and minheap
    private HashMap<String, String> cache;
    private HashMap<String, Integer> counts;
    // ignores duplicates, but keeps access order
    // <count, list of keys>
    private HashMap<Integer, LinkedHashSet<String>> countToKeys;

    public LFUCache(int cacheSize){
        this.cacheSize = cacheSize;
        cache = new HashMap<>();
        counts = new HashMap<>();
        countToKeys = new HashMap<>();
        // init to empty list of keys
        // lowest freq possible is 1
        countToKeys.put(1, new LinkedHashSet<String>());
    }

    @Override
    public String get(String key) {
        if (cache.containsKey(key)){
            // frequency
            int count = counts.get(key);
            // put just updates this since key already exists
            counts.put(key, count + 1);
            // list of keys that have this count
            // remove this specific key from that list
            // add this specific key to the next list
            countToKeys.get(count).remove(key);

            if (countToKeys.get(count).size() == 0 && count == min){
                min++;
            }
            if (countToKeys.containsKey(count + 1)){
                LinkedHashSet<String> newList = new LinkedHashSet<>();
                countToKeys.put(count + 1, newList);
            }
            countToKeys.get(count + 1).add(key);

            return cache.get(key);
        } else {
            return null;
        }
    }

    @Override
    public void put(String key, String value) {
        if (cacheSize <= 0){
            return;
        }
        if (value.equals("null")){
            cache.remove(key);
            int currCount = counts.get(key);
            counts.remove(key);
            countToKeys.get(currCount).remove(key);
        } else {
            // just updating, no need to evict
            if (cache.containsKey(key)){
                cache.put(key, value);
                get(key); //update the frequency
                return;
            } else {
                // if it doesn't exist, evict least frequent IF we are at max
                if (cache.size() >= cacheSize) {
                    // get one key from countToKeys[min] and remove from cache
                    String evictedKey = countToKeys.get(min).iterator().next();
                    cache.remove(evictedKey);
                    counts.remove(evictedKey);
                }
                // new key, add to countToKeys[1]
                cache.put(key, value);
                counts.put(key, 1);
                // this new entry is now the new min
                min = 1;
                countToKeys.get(1).add(key);
            }
        }
    }

    @Override
    public void clear() {
        cache.clear();
        counts.clear();
        countToKeys.clear();
        countToKeys.put(1, new LinkedHashSet<String>());
    }
}
