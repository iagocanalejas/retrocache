package com.andiag.retrocache.cache;

import com.iagocanalejas.dualcache.interfaces.Cache;

import java.util.HashMap;

/**
 * Created by IagoCanalejas on 10/01/2017.
 */
public class MockCachingSystem implements Cache<String, byte[]> {
    private HashMap<String, byte[]> cachedResponses = new HashMap<>();

    @Override
    public boolean contains(String key) {
        return cachedResponses.containsKey(key);
    }

    @Override
    public byte[] get(String key) {
        return cachedResponses.get(key);
    }

    @Override
    public byte[] put(String key, byte[] value) {
        return cachedResponses.put(key, value);
    }

    @Override
    public int size() {
        return cachedResponses.size();
    }

    @Override
    public byte[] remove(String key) {
        byte[] previous = cachedResponses.get(key);
        cachedResponses.remove(key);
        return previous;
    }

    @Override
    public void clear() {
        cachedResponses = new HashMap<>();
    }
}
