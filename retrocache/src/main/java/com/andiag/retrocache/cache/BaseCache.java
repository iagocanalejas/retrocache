package com.andiag.retrocache.cache;

import android.content.Context;

import com.iagocanalejas.dualcache.DualCache;
import com.iagocanalejas.dualcache.interfaces.Parser;
import com.iagocanalejas.dualcache.interfaces.SizeOf;

import java.nio.charset.Charset;

/**
 * Created by Canalejas on 09/01/2017.
 */

public class BaseCache {

    private static final String CACHE_NAME = "dualcache_retrofit";
    private static final int REASONABLE_DISK_SIZE = 1024 * 1024; // 1 MB
    private static final int REASONABLE_MEM_ENTRIES = 50; // 50 entries

    private static final SizeOf<byte[]> sSizeOf = new SizeOf<byte[]>() {
        @Override
        public int sizeOf(byte[] object) {
            return 1;
        }
    };

    private static final Parser<byte[]> sParser = new Parser<byte[]>() {
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
     * Count each entry as 1
     *
     * @return {@link SizeOf}
     */
    public static SizeOf<byte[]> getSizeOf() {
        return sSizeOf;
    }

    /**
     * Basic parser that encode and decode byte[] matching default charset
     *
     * @return {@link Parser}
     */
    public static Parser<byte[]> getParser() {
        return sParser;
    }

    /**
     * Return a no disk cache
     *
     * @param appVersion used to invalidate the cache
     * @return {@link DualCache}
     */
    public static DualCache<byte[]> getInstance(int appVersion) {
        return new DualCache.Builder<byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, sSizeOf)
                .noDisk()
                .build();
    }

    /**
     * Generate a basic cache
     *
     * @param context    required for {@link com.jakewharton.disklrucache.DiskLruCache}
     * @param appVersion used to invalidate the cache
     * @return {@link DualCache}
     */
    public static DualCache<byte[]> getInstance(Context context, int appVersion) {
        return new DualCache.Builder<byte[]>(CACHE_NAME, appVersion)
                .useReferenceInRam(REASONABLE_MEM_ENTRIES, sSizeOf)
                .useSerializerInDisk(REASONABLE_DISK_SIZE, true, sParser, context)
                .build();
    }

    public static DualCache.Builder<byte[]> getPreconfiguredBuilder(int appVersion) {
        return new DualCache.Builder<>(CACHE_NAME, appVersion);
    }

}
