package app_kvServer.KVCache;

import java.util.LinkedHashMap;
import java.util.Map;

public class FIFOCache implements IKVCache {

    private int cacheSize;
    private LinkedHashMap<String, String> cache;

    public FIFOCache(int cacheSize){
        this.cacheSize = cacheSize;
        cache = new LinkedHashMap<String, String>(cacheSize, (float) 1, false){
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest){
                return size() > cacheSize;
            }
        };
    }

    @Override
    public String get(String key) {
        return cache.get(key);
    }

    @Override
    public void put(String key, String value) {
        cache.put(key, value);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
