package com.andiag.retrocache.cache;

import android.content.Context;

import com.iagocanalejas.dualcache.DualCache;
import com.iagocanalejas.dualcache.interfaces.Parser;
import com.iagocanalejas.dualcache.interfaces.SizeOf;

import java.nio.charset.Charset;

/**
 * Created by IagoCanalejas on 09/01/2017.
 * Gives the basic configurations for set a cache to retrofit calls.
 */
public final class RetrofitCache {

    private static final String CACHE_NAME = "dualcache_retrofit";
    public static final int REASONABLE_DISK_SIZE = 1024 * 1024; // 1 MB
    public static final int REASONABLE_MEM_ENTRIES = 50; // 50 entries

    /**
     * Count each entry as 1 {@link SizeOf}.
     */
    public static final SizeOf<byte[]> ENTRY_COUNT_SIZE_OF = new SizeOf<byte[]>() {
        @Override
        public int sizeOf(byte[] object) {
            return 1;
        }
    };

    /**
     * Count each entry as byte[].length {@link SizeOf}.
     */
    public static final SizeOf<byte[]> BYTES_LENGTH_SIZE_OF = new SizeOf<byte[]>() {
        @Override
        public int sizeOf(byte[] object) {
            return object.length;
        }
    };

    /**
     * Basic parser that encode and decode byte[] matching default charset {@link Parser}.
     */
    public static final Parser<byte[]> BYTE_ARRAY_PARSER = new Parser<byte[]>() {
        @Override
        public byte[] fromString(String data) {
            return data.getBytes(Charset.defaultCharset());
        }

        @Override
        public String toString(byte[] object) {
            return new String(object, Charset.defaultCharset());
        }
    };

    /**
     * Return a no disk cache.
     *
     * @param appVersion used to invalidate the cache.
     * @return {@link DualCache}.
     */
    public static DualCache<byte[]> getRamCache(int appVersion) {
        return new DualCache.Builder<byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, ENTRY_COUNT_SIZE_OF)
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
    public static DualCache<byte[]> getDualCache(Context context, int appVersion) {
        return new DualCache.Builder<byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, ENTRY_COUNT_SIZE_OF)
                .useSerializerInDisk(REASONABLE_DISK_SIZE, true, BYTE_ARRAY_PARSER, context)
                .build();
    }

    /**
     * Generate a non-configure builder for the cache.
     *
     * @param appVersion used to invalidate the cache.
     * @return {@link DualCache}.
     */
    public static DualCache.Builder<byte[]> getBuilder(int appVersion) {
        return new DualCache.Builder<>(CACHE_NAME, appVersion);
    }

}
