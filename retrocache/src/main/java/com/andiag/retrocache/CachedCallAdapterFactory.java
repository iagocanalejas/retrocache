package com.andiag.retrocache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class CachedCallAdapterFactory extends CallAdapter.Factory {
    private final Cache<String, byte[]> mCachingSystem;
    private final Executor mAsyncExecutor;

    public CachedCallAdapterFactory(@NonNull Context context, int appVersion) {
        this.mCachingSystem = RetroCache.getDualCache(context, appVersion);
        this.mAsyncExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                new Handler(Looper.getMainLooper()).post(command);
            }
        };
    }

    public CachedCallAdapterFactory(@NonNull Cache<String, byte[]> cachingSystem) {
        this.mCachingSystem = cachingSystem;
        this.mAsyncExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                new Handler(Looper.getMainLooper()).post(command);
            }
        };
    }

    public CachedCallAdapterFactory(@NonNull Cache<String, byte[]> cachingSystem,
                                    @Nullable Executor executor) {
        this.mCachingSystem = cachingSystem;
        this.mAsyncExecutor = executor;
    }

    @Override
    public CallAdapter<Cached<?>> get(final Type returnType, final Annotation[] annotations,
                                      final Retrofit retrofit) {

        TypeToken<?> token = TypeToken.get(returnType);
        if (token.getRawType() != Cached.class) {
            return null;
        }

        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "CachedCall must have generic type (e.g., CachedCall<ResponseBody>)");
        }

        return new CallAdapter<Cached<?>>() {
            @Override
            public Type responseType() {
                return ((ParameterizedType) returnType).getActualTypeArguments()[0];
            }

            @Override
            public <R> Cached<R> adapt(Call<R> call) {
                return new CachedCallAdapter<>(mAsyncExecutor, call, responseType(), annotations,
                        retrofit, mCachingSystem);
            }
        };
    }

}
