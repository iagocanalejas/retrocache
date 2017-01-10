package com.andiag.retrocache;

import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Canalejas on 09/01/2017.
 */
class ResponseUtils {

    @SuppressWarnings("unchecked")
    static <T> byte[] responseToBytes(Retrofit retrofit, T data, Type dataType,
                                      Annotation[] annotations) {
        for (Converter.Factory factory : retrofit.converterFactories()) {
            if (factory == null) {
                continue;
            }
            Converter<T, RequestBody> converter =
                    (Converter<T, RequestBody>) factory.requestBodyConverter(
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
    static <T> T bytesToResponse(Retrofit retrofit, Type dataType, Annotation[] annotations,
                                 byte[] data) {
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

    static String urlToKey(HttpUrl url) {
        return sha1(url.toString(), Charset.defaultCharset());
    }

    //region SHA1 Converter
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfByte = (b >>> 4) & 0x0F;
            int twoHalf = 0;
            do {
                buf.append((0 <= halfByte) && (halfByte <= 9)
                        ? (char) ('0' + halfByte)
                        : (char) ('a' + (halfByte - 10)));
                halfByte = b & 0x0F;
            } while (twoHalf++ < 1);
        }
        return buf.toString();
    }


    private static String sha1(String text, Charset charset) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] textBytes = text.getBytes(charset);
            md.update(textBytes, 0, textBytes.length);
            return convertToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    //endregion

}
