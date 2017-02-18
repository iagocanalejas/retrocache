package com.andiag.retrocache;

import android.support.annotation.NonNull;
import android.util.Log;

import com.iagocanalejas.dualcache.hashing.Hashing;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by IagoCanalejas on 09/01/2017.
 * Handles the {@link Cached} requests
 */
final class CachedCallAdapter<T> implements Cached<T> {
    private final Executor mExecutor;
    private final Call<T> mCall;
    private final Type mResponseType;
    private final Annotation[] mAnnotations;
    private final Retrofit mRetrofit;
    private final Cache<String, byte[]> mCachingSystem;
    private final Request mRequest;
    private final boolean mCachingActive;

    private boolean mExecuted;
    private boolean mCanceled;

    CachedCallAdapter(Executor executor, Call<T> call, Type responseType, Annotation[] annotations,
                      Retrofit retrofit, Cache<String, byte[]> cachingSystem) {
        this.mExecutor = executor;
        this.mCall = call;
        this.mResponseType = responseType;
        this.mAnnotations = annotations;
        this.mRetrofit = retrofit;
        this.mCachingSystem = cachingSystem;
        this.mRequest = RequestBuilder.build(call);
        mCachingActive = mRequest != null && mRequest.method().equals("GET");

        mExecuted = mCall.isExecuted();
        mCanceled = mCall.isCanceled();
    }

    /**
     * Try to find {@link Request} on cache.
     *
     * @param callback {@link Callback} to handle {@link Callback#onResponse} result.
     * @return True if found on cache. False otherwise.
     */
    private boolean cacheLoad(final Callback<T> callback) {
        byte[] data = mCachingSystem.get(
                ResponseUtils.urlToKey(request().url()));
        if (data != null) {
            final T convertedData = ResponseUtils.bytesToResponse(
                    mRetrofit, mResponseType, mAnnotations, data);

            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onResponse(mCall, Response.success(convertedData));
                }
            });
            return true;
        }
        return false;
    }

    /**
     * Enqueue trying to resolve it with a server {@link Request}.
     *
     * @param callback  {@link Callback} to handle {@link Callback#onResponse} result.
     * @param isRefresh Mark if cache should be deleted.
     */
    private void networkLoad(final Callback<T> callback, final boolean isRefresh) {
        mCall.enqueue(new Callback<T>() {
            @Override
            public void onResponse(final Call<T> call, final Response<T> response) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            // Add response to cache
                            mCachingSystem.put(
                                    ResponseUtils.urlToKey(mCall.request().url()),
                                    ResponseUtils.responseToBytes(mRetrofit, response.body(),
                                            responseType(), mAnnotations));
                        }
                        if (!response.isSuccessful() && isRefresh) {
                            // If we are refreshing remove cache entry
                            mCachingSystem.remove(
                                    ResponseUtils.urlToKey(mCall.request().url()));
                        }
                        callback.onResponse(call, response);
                    }
                });
            }

            @Override
            public void onFailure(final Call<T> call, final Throwable t) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isRefresh) {
                            // If we are refreshing remove cache entry
                            mCachingSystem.remove(ResponseUtils.urlToKey(mCall.request().url()));
                        }
                        callback.onFailure(call, t);
                    }
                });
            }
        });
    }

    private void delegate(final Callback<T> callback) {
        mCall.enqueue(new Callback<T>() {
            @Override
            public void onResponse(final Call<T> call, final Response<T> response) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(call, response);
                    }
                });
            }

            @Override
            public void onFailure(final Call<T> call, final Throwable t) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(call, t);
                    }
                });
            }
        });
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        if (callback == null) {
            throw new NullPointerException("callback == null");
        }
        if (mExecuted || mCall.isExecuted()) {
            throw new IllegalStateException("Already executed.");
        }

        mExecuted = true;
        if (mCachingActive) {
            // Look in cache if we are in a GET method
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!cacheLoad(callback)) {
                        networkLoad(callback, false);
                    }
                }
            }).start();
            return;
        }
        delegate(callback);
    }

    @Override
    public void refresh(final Callback<T> callback) {
        if (callback == null) {
            throw new NullPointerException("callback == null");
        }
        if (mExecuted || mCall.isExecuted()) {
            throw new IllegalStateException("Already executed.");
        }

        mExecuted = true;
        if (mCachingActive) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    networkLoad(callback, true);
                }
            }).start();
            return;
        }
        delegate(callback);
    }

    @Override
    public Type responseType() {
        return mResponseType;
    }

    @Override
    public Request request() {
        return mRequest.newBuilder().build();
    }

    @Override
    public Cached<T> clone() {
        return new CachedCallAdapter<>(mExecutor, mCall.clone(), responseType(),
                mAnnotations, mRetrofit, mCachingSystem);
    }

    @Override
    public Response<T> execute() throws IOException {
        if (mExecuted || mCall.isExecuted()) {
            throw new IllegalStateException("Already executed.");
        }

        mExecuted = true;
        if (mCachingActive) {
            byte[] data = mCachingSystem.get(ResponseUtils.urlToKey(mCall.request().url()));
            if (data == null) { // Response is not cached
                Response<T> response = mCall.execute();
                if (response.isSuccessful()) {
                    mCachingSystem.put(
                            ResponseUtils.urlToKey(mCall.request().url()),
                            ResponseUtils.responseToBytes(mRetrofit, response.body(),
                                    responseType(), mAnnotations));
                }
                return response;
            }
            // Response is cached
            final T convertedData = ResponseUtils.bytesToResponse(
                    mRetrofit, mResponseType, mAnnotations, data);
            return Response.success(convertedData);
        }
        return mCall.execute();
    }

    @Override
    public void remove() {
        mCachingSystem.remove(ResponseUtils.urlToKey(mCall.request().url()));
    }

    @Override
    public void cancel() {
        this.mCanceled = true;
        mCall.cancel();
    }

    @Override
    public boolean isCanceled() {
        return mCall.isCanceled() || mCanceled;
    }

    @Override
    public boolean isExecuted() {
        return mCall.isExecuted() || mExecuted;
    }

    /**
     * Created by IagoCanalejas on 02/02/2017.
     * Required because {@link retrofit2.RequestBuilder} is final and package local
     */
    private static final class RequestBuilder {

        private static Object[] getCallArgs(Call call)
                throws NoSuchFieldException, IllegalAccessException {
            Field argsField = call.getClass().getDeclaredField("args");
            argsField.setAccessible(true);
            return (Object[]) argsField.get(call);
        }

        private static Object getRequestFactory(Call call)
                throws IllegalAccessException, NoSuchFieldException {
            Field serviceMethodField = call.getClass().getDeclaredField("serviceMethod");
            serviceMethodField.setAccessible(true);
            return serviceMethodField.get(call);
        }

        static Request build(Call call) {
            try {
                Object requestFactory = getRequestFactory(call);
                Method createMethod = requestFactory.getClass()
                        .getDeclaredMethod("toRequest", Object[].class);

                createMethod.setAccessible(true);

                return (Request) createMethod.invoke(
                        requestFactory, new Object[]{getCallArgs(call)});
            } catch (Exception exc) {
                return null;
            }
        }

    }

    /**
     * Created by IagoCanalejas on 09/01/2017.
     */
    private static final class ResponseUtils {

        @SuppressWarnings("unchecked")
        static <T> byte[] responseToBytes(Retrofit retrofit, T data, Type dataType,
                                          Annotation[] annotations) {
            if (data == null) {
                return null;
            }

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

        /**
         * Hash the url concat the REST method used to work as cache key.
         *
         * @param url requested
         * @return hashed cache key
         */
        static String urlToKey(@NonNull HttpUrl url) {
            return Hashing.sha1(url.toString(), Charset.defaultCharset());
        }

    }
}
