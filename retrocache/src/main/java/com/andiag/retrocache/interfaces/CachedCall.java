package com.andiag.retrocache.interfaces;


import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.Request;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Canalejas on 09/01/2017.
 */
public interface CachedCall<T> extends Cloneable {
    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     */
    void enqueue(Callback<T> callback);

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     *
     * If we are in a GET operation ignores cache and send a new request.
     * In other operations send a new request.
     */
    void refresh(Callback<T> callback);

    /**
     * Returns a runtime {@link Type} that corresponds to the response type specified in your
     * service.
     */
    Type responseType();

    /**
     * Builds a new {@link Request} that is identical to the one that will be dispatched
     * when the {@link CachedCall} is executed/enqueued.
     */
    Request buildRequest();

    /**
     * Create a new, identical call to this one which can be enqueued or executed even if this call
     * has already been.
     */
    CachedCall<T> clone();

    /**
     * Synchronously send the request and return its response. NOTE: No smart caching allowed!
     *
     * @throws IOException      if a problem occurred talking to the server.
     * @throws RuntimeException (and subclasses) if an unexpected error occurs creating the request
     *                          or decoding the response.
     */
    Response<T> execute() throws IOException;

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    void cancel();
}
