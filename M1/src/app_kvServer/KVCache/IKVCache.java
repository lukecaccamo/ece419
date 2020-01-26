package app_kvServer.KVCache;

public interface IKVCache {
    String get(String key);
    void put(String key, String value);
    void clear();
}
