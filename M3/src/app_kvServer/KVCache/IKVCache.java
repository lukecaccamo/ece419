package app_kvServer.KVCache;

public interface IKVCache {
    boolean inCache(String key);
    String get(String key);
    void put(String key, String value);
    void clear();
}
