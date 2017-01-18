package com.andiag.retrocache.cache;

import android.content.Context;
import android.support.annotation.NonNull;

import com.andiag.retrocache.utils.ByteArraySerializer;
import com.andiag.retrocache.utils.EntryCountSizeOf;
import com.iagocanalejas.dualcache.Builder;
import com.iagocanalejas.dualcache.DualCache;

/**
 * Created by IagoCanalejas on 09/01/2017.
 * Gives the basic configurations for set a cache to retrofit calls.
 */
public final class RetroCache {

    private static final String CACHE_NAME = "dualcache_retrofit";
    public static final int REASONABLE_DISK_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final int REASONABLE_MEM_ENTRIES = 50; // 50 entries
    public static final long REASONABLE_PERSISTENT_TIME = 60 * 60; // 1 hour

    /**
     * Return a no disk cache.
     *
     * @param appVersion used to invalidate the cache.
     * @return {@link DualCache}.
     */
    public static DualCache<String, byte[]> getRamCache(int appVersion) {
        return new Builder<String, byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, new EntryCountSizeOf())
                .noDisk()
                .build();
    }

    /**
     * Generate a basic cache.
     *
     * @param context    required for {@link com.jakewharton.disklrucache.DiskLruCache}.
     * @param appVersion used to invalidate the cache.
     * @return {@link DualCache}.
     */
    public static DualCache<String, byte[]> getDualCache(@NonNull Context context, int appVersion) {
        return new Builder<String, byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, new EntryCountSizeOf())
                .useSerializerInDisk(REASONABLE_DISK_SIZE, true, new ByteArraySerializer(), context)
                .build();
    }

    public static DualCache<String, byte[]> getVolatileCache(@NonNull Context context,
                                                             int appVersion) {

        return new Builder<String, byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, new EntryCountSizeOf())
                .useSerializerInDisk(REASONABLE_DISK_SIZE, true, new ByteArraySerializer(), context)
                .useVolatileCache(REASONABLE_PERSISTENT_TIME)
                .build();
    }

    /**
     * Generate a non-configure builder for the cache.
     *
     * @param appVersion used to invalidate the cache.
     * @return {@link DualCache}.
     */
    public static Builder<String, byte[]> getBuilder(int appVersion) {
        return new Builder<>(CACHE_NAME, appVersion);
    }

}
