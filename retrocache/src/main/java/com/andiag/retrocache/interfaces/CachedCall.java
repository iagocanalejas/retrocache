package com.andiag.retrocache.interfaces;


import java.lang.reflect.Type;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by IagoCanalejas on 09/01/2017.
 * Interface replacing {@link retrofit2.Call} for Caching {@link Request}.
 */
public interface CachedCall<T> extends Call<T>, Cloneable {

    /**
     * Asynchronously send the request ignoring the cache and notify {@code callback} of its
     * response or if an error occurred talking to the server, creating the request,
     * or processing the response.
     */
    void refresh(Callback<T> callback);

    /**
     * Returns a runtime {@link Type} that corresponds to the response type specified in your
     * service.
     */
    Type responseType();

    /**
     * Remove a cached call if exists
     */
    void remove();

}
