package com.andiag.commons;

import android.support.annotation.NonNull;
import android.util.Log;

import com.iagocanalejas.dualcache.hashing.Hashing;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Canalejas on 22/02/2017.
 */

public final class CacheUtils {

    @SuppressWarnings("unchecked")
    public static <T> byte[] responseToBytes(Retrofit retrofit, T data, Type dataType, Annotation[] annotations) {
        if (data == null) {
            return null;
        }

        for (Converter.Factory factory : retrofit.converterFactories()) {
            if (factory == null) {
                continue;
            }
            Converter<T, RequestBody> converter;
            converter = (Converter<T, RequestBody>) factory.requestBodyConverter(
                    dataType, annotations, null, retrofit);

            if (converter != null) {
                Buffer buff = new Buffer();
                try {
                    converter.convert(data).writeTo(buff);
                } catch (IOException ioException) {
                    continue;
                }

                return buff.readByteArray();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T bytesToResponse(Retrofit retrofit, Type dataType, Annotation[] annotations, byte[] data) {
        for (Converter.Factory factory : retrofit.converterFactories()) {
            if (factory == null) {
                continue;
            }
            Converter<ResponseBody, T> converter =
                    (Converter<ResponseBody, T>) factory.responseBodyConverter(
                            dataType, annotations, retrofit);

            if (converter != null) {
                try {
                    return converter.convert(ResponseBody.create(null, data));
                } catch (IOException | NullPointerException exc) {
                    Log.e("CachedCall", "", exc);
                }
            }
        }

        return null;
    }

    /**
     * Hash the url concat the REST method used to work as cache key.
     *
     * @param url requested
     * @return hashed cache key
     */
    public static String urlToKey(@NonNull HttpUrl url) {
        return Hashing.sha1(url.toString(), Charset.defaultCharset());
    }

}
