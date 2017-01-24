package com.andiag.retrocache.utils;

import com.iagocanalejas.dualcache.hashing.Hashing;

import java.nio.charset.Charset;

import okhttp3.HttpUrl;

/**
 * Created by Iago on 23/01/2017.
 */
public class Utils {
    public static String urlToKey(HttpUrl url) {
        return Hashing.sha1(url.toString(), Charset.defaultCharset());
    }
}
