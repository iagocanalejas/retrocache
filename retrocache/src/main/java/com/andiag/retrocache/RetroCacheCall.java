package com.andiag.retrocache;

import com.andiag.retrocache.annotations.Caching;
import com.andiag.retrocache.interfaces.CachedCall;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by IagoCanalejas on 09/01/2017.
 * Handles the {@link CachedCall} requests
 */
class RetroCacheCall<T> implements CachedCall<T> {
    private final Executor mExecutor;
    private final Call<T> mCall;
    private final Type mResponseType;
    private final Annotation[] mAnnotations;
    private final Retrofit mRetrofit;
    private final Cache<String, byte[]> mCachingSystem;
    private final Request mRequest;

    RetroCacheCall(Executor executor, Call<T> call, Type responseType, Annotation[] annotations,
                   Retrofit retrofit, Cache<String, byte[]> cachingSystem) {
        this.mExecutor = executor;
        this.mCall = call;
        this.mResponseType = responseType;
        this.mAnnotations = annotations;
        this.mRetrofit = retrofit;
        this.mCachingSystem = cachingSystem;
        this.mRequest = RequestBuilder.build(call);
    }

    /**
     * Find caching value using {@link Caching} annotation in the call annotations.
     *
     * @param method REST
     * @return annotation value if exist.
     * If not, true for {@link retrofit2.http.GET}, false otherwise.
     */
    private boolean isCachingActive(String method) {
        for (Annotation annotation : mAnnotations) {
            if (annotation instanceof Caching) {
                return ((Caching) annotation).enabled();
            }
        }
        return method.equals("GET");
    }

    /**
     * Try to find {@link Request} on cache.
     *
     * @param callback {@link Callback} to handle {@link Callback#onResponse} result.
     * @return True if found on cache. False otherwise.
     */
    private boolean getFromCache(final Callback<T> callback) {
        byte[] data = mCachingSystem.get(
                ResponseUtils.urlToKey(request().method(), request().url()));
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
    private void getFromServer(final Callback<T> callback, final boolean isRefresh) {
        mCall.enqueue(new Callback<T>() {
            @Override
            public void onResponse(final Call<T> call, final Response<T> response) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            // Add response to cache
                            mCachingSystem.put(
                                    ResponseUtils.urlToKey(
                                            call.request().method(),
                                            response.raw().request().url()),
                                    ResponseUtils.responseToBytes(mRetrofit, response.body(),
                                            responseType(), mAnnotations));
                        }
                        if (!response.isSuccessful() && isRefresh) {
                            // If we are refreshing remove cache entry
                            mCachingSystem.remove(
                                    ResponseUtils.urlToKey(
                                            call.request().method(),
                                            response.raw().request().url()));
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
                            mCachingSystem.remove(ResponseUtils.urlToKey(call.request().method(),
                                    call.request().url()));
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
    public boolean isExecuted() {
        return mCall.isExecuted();
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        if (isCachingActive(request().method())) {
            // Look in cache if we are in a GET method
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!getFromCache(callback)) {
                        getFromServer(callback, false);
                    }
                }
            }).start();
            return;
        }
        delegate(callback);
    }

    @Override
    public void refresh(final Callback<T> callback) {
        if (isCachingActive(request().method())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getFromServer(callback, true);
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
    public CachedCall<T> clone() {
        return new RetroCacheCall<>(mExecutor, mCall.clone(), responseType(),
                mAnnotations, mRetrofit, mCachingSystem);
    }

    @Override
    public Response<T> execute() throws IOException {
        return mCall.execute();
    }

    @Override
    public void remove() {
        mCachingSystem.remove(ResponseUtils.urlToKey(
                mCall.request().method(), mCall.request().url()));
    }

    @Override
    public void cancel() {
        mCall.cancel();
    }

    @Override
    public boolean isCanceled() {
        return mCall.isCanceled();
    }
}
