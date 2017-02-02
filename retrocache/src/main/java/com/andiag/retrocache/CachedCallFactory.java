package com.andiag.retrocache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.andiag.retrocache.cache.RetroCache;
import com.andiag.retrocache.interfaces.CachedCall;
import com.google.gson.reflect.TypeToken;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class CachedCallFactory extends CallAdapter.Factory {
    private final Cache<String, byte[]> mCachingSystem;
    private final Executor mAsyncExecutor;

    public CachedCallFactory(@NonNull Context context, int appVersion) {
        this.mCachingSystem = RetroCache.getDualCache(context, appVersion);
        this.mAsyncExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                new Handler(Looper.getMainLooper()).post(command);
            }
        };
    }

    public CachedCallFactory(@NonNull Cache<String, byte[]> cachingSystem) {
        this.mCachingSystem = cachingSystem;
        this.mAsyncExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                new Handler(Looper.getMainLooper()).post(command);
            }
        };
    }

    public CachedCallFactory(@NonNull Cache<String, byte[]> cachingSystem,
                             @Nullable Executor executor) {
        this.mCachingSystem = cachingSystem;
        this.mAsyncExecutor = executor;
    }

    @Override
    public CallAdapter<CachedCall<?>> get(final Type returnType, final Annotation[] annotations,
                                          final Retrofit retrofit) {

        TypeToken<?> token = TypeToken.get(returnType);
        if (token.getRawType() != CachedCall.class) {
            return null;
        }

        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "CachedCall must have generic type (e.g., CachedCall<ResponseBody>)");
        }

        return new CallAdapter<CachedCall<?>>() {
            @Override
            public Type responseType() {
                return ((ParameterizedType) returnType).getActualTypeArguments()[0];
            }

            @Override
            public <R> CachedCall<R> adapt(Call<R> call) {
                return new RetroCacheCall<>(mAsyncExecutor, call, responseType(), annotations,
                        retrofit, mCachingSystem);
            }
        };
    }

}
