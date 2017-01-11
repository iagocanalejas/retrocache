package com.andiag.retrocache.utils;

import com.iagocanalejas.dualcache.interfaces.SizeOf;

/**
 * Created by Iagocanalejas on 12/01/2017.
 * Count each entry as byte[].length
 */
public class BytesLengthSizeOf implements SizeOf<byte[]> {

    /**
     * Count each entry as byte[].length {@link SizeOf}.
     */
    @Override
    public int sizeOf(byte[] bytes) {
        return bytes.length;
    }

}
