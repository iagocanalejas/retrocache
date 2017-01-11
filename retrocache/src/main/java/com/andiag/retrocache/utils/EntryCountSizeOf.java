package com.andiag.retrocache.utils;

import com.iagocanalejas.dualcache.interfaces.SizeOf;

/**
 * Created by Iagocanalejas on 11/01/2017.
 * Count each entry as 1
 */
public class EntryCountSizeOf implements SizeOf<byte[]> {

    /**
     * Count each entry as 1 {@link SizeOf}.
     */
    @Override
    public int sizeOf(byte[] bytes) {
        return 1;
    }

}
