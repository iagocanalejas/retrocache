package com.andiag.retrocache;

import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Created by Canalejas on 22/02/2017.
 */

final class CachedCallAdapter<T> implements CallAdapter<T, Cached<T>> {

    private final Type mReturnType;
    private final Executor mExecutor;
    private final Annotation[] mAnnotations;
    private final Retrofit mRetrofit;
    private final Cache<String, byte[]> mCachingSystem;

    CachedCallAdapter(Executor executor, Type returnType, Annotation[] annotations,
                      Retrofit retrofit, Cache<String, byte[]> cachingSystem) {
        this.mExecutor = executor;
        this.mAnnotations = annotations;
        this.mRetrofit = retrofit;
        this.mCachingSystem = cachingSystem;
        this.mReturnType = returnType;
    }

    @Override
    public Type responseType() {
        return ((ParameterizedType) mReturnType).getActualTypeArguments()[0];
    }

    @Override
    public Cached<T> adapt(Call<T> call) {
        return new CachedCall<>(mExecutor, call, responseType(), mAnnotations, mRetrofit, mCachingSystem);
    }
}
