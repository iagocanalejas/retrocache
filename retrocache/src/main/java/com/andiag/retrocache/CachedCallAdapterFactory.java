package com.andiag.retrocache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.reflect.TypeToken;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class CachedCallAdapterFactory extends CallAdapter.Factory {
    private final Cache<String, byte[]> mCachingSystem;
    private final Executor mAsyncExecutor;

    private CachedCallAdapterFactory(@NonNull Cache<String, byte[]> cachingSystem, @Nullable Executor executor) {
        this.mCachingSystem = cachingSystem;
        this.mAsyncExecutor = executor;
    }

    public static CachedCallAdapterFactory create(@NonNull Context context, int appVersion) {
        return new CachedCallAdapterFactory(RetroCache.getDualCache(context, appVersion), new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                new Handler(Looper.getMainLooper()).post(command);
            }
        });
    }

    public static CachedCallAdapterFactory create(@NonNull Cache<String, byte[]> cachingSystem) {
        return new CachedCallAdapterFactory(cachingSystem, new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                new Handler(Looper.getMainLooper()).post(command);
            }
        });
    }

    public static CachedCallAdapterFactory createWithExecutor(@NonNull Cache<String, byte[]> cachingSystem, @NonNull Executor executor) {
        return new CachedCallAdapterFactory(cachingSystem, executor);
    }

    @Override
    public CallAdapter<Cached<?>, ?> get(final Type returnType, final Annotation[] annotations, final Retrofit retrofit) {

        TypeToken<?> token = TypeToken.of(returnType);
        if (token.getRawType() != Cached.class) {
            return null;
        }

        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException("Cached<?> must have generic type (e.g., Cached<ResponseBody>)");
        }

        return new CachedCallAdapter<>(mAsyncExecutor, returnType, annotations, retrofit, mCachingSystem);

    }

}
