package app_kvServer.KVCache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache implements IKVCache{

    private int maxSize;
    private LinkedHashMap<String, String> cache;

    public LRUCache(int cacheSize){
        this.maxSize = cacheSize;
        cache = new LinkedHashMap<String, String>(cacheSize, (float) 1, true){
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest){
                return size() > maxSize;
            }
        };
    }

    @Override
    public boolean inCache(String key){
        return cache.containsKey(key);
    }

    @Override
    public String get(String key) {
        return cache.get(key);
    }

    @Override
    public void put(String key, String value) {
        if (value.equals("null")){
            cache.remove(key);
        } else {
            cache.put(key, value);
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
