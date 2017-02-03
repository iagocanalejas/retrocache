package com.andiag.retrocache.utils;

import com.iagocanalejas.dualcache.interfaces.Serializer;

import java.nio.charset.Charset;

/**
 * Created by IagoCanalejas on 12/01/2017.
 * Basic parser that encode and decode byte[] matching default charset
 */
public class ByteArraySerializer implements Serializer<byte[]> {

    @Override
    public byte[] fromString(String string) {
        return string.getBytes(Charset.defaultCharset());
    }

    @Override
    public String toString(byte[] bytes) {
        return new String(bytes, Charset.defaultCharset());
    }

}
