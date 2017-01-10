package com.andiag.retrocache.utils;

import com.iagocanalejas.dualcache.interfaces.Cache;

import java.util.HashMap;

/**
 * Created by IagoCanalejas on 10/01/2017.
 */
public class MockCachingSystem implements Cache<String, byte[]> {
    private HashMap<String, byte[]> cachedResponses = new HashMap<>();

    @Override
    public boolean contains(String key) {
        return false;
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
        return 0;
    }

    @Override
    public byte[] remove(String key) {
        return new byte[0];
    }

    @Override
    public void clear() {

    }
}
