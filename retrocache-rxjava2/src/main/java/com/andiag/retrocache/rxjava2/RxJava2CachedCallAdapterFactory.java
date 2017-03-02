/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.andiag.retrocache.rxjava2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.andiag.commons.RetroCache;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;


public final class RxJava2CachedCallAdapterFactory extends CallAdapter.Factory {
    private final Cache<String, byte[]> mCachingSystem;
    private final Scheduler mScheduler;
    private final boolean mAsync;

    private RxJava2CachedCallAdapterFactory(@NonNull Cache<String, byte[]> cachingSystem, @Nullable Scheduler scheduler, boolean async) {
        this.mCachingSystem = cachingSystem;
        this.mScheduler = scheduler;
        this.mAsync = async;
    }

    public static RxJava2CachedCallAdapterFactory create(@NonNull Context context, int appVersion) {
        return new RxJava2CachedCallAdapterFactory(RetroCache.getDualCache(context, appVersion), null, false);
    }

    public static RxJava2CachedCallAdapterFactory createAsync(@NonNull Context context, int appVersion) {
        return new RxJava2CachedCallAdapterFactory(RetroCache.getDualCache(context, appVersion), null, true);
    }

    public static RxJava2CachedCallAdapterFactory createWithScheduler(@NonNull Context context, int appVersion, Scheduler scheduler) {
        if (scheduler == null) throw new NullPointerException("mScheduler == null");
        return new RxJava2CachedCallAdapterFactory(RetroCache.getDualCache(context, appVersion), scheduler, false);
    }

    public static RxJava2CachedCallAdapterFactory create(@NonNull Cache<String, byte[]> cachingSystem) {
        return new RxJava2CachedCallAdapterFactory(cachingSystem, null, false);
    }

    public static RxJava2CachedCallAdapterFactory createAsync(@NonNull Cache<String, byte[]> cachingSystem) {
        return new RxJava2CachedCallAdapterFactory(cachingSystem, null, true);
    }

    public static RxJava2CachedCallAdapterFactory createWithScheduler(@NonNull Cache<String, byte[]> cachingSystem, Scheduler scheduler) {
        if (scheduler == null) throw new NullPointerException("mScheduler == null");
        return new RxJava2CachedCallAdapterFactory(cachingSystem, scheduler, false);
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);

        if (rawType == Completable.class) {
            // Completable is not parameterized (which is what the rest of this method deals with) so it
            // can only be created with a single configuration.
            return new RxJava2CachedCallAdapter(
                    mCachingSystem, Void.class, mScheduler, retrofit, annotations, mAsync, false, true, false, false, false, true);
        }

        boolean isFlowable = rawType == Flowable.class;
        boolean isSingle = rawType == Single.class;
        boolean isMaybe = rawType == Maybe.class;
        if (rawType != Observable.class && !isFlowable && !isSingle && !isMaybe) {
            return null;
        }

        boolean isResult = false;
        boolean isBody = false;
        Type responseType;
        if (!(returnType instanceof ParameterizedType)) {
            String name = isFlowable ? "Flowable"
                    : isSingle ? "Single"
                    : isMaybe ? "Maybe" : "Observable";
            throw new IllegalStateException(name + " return type must be parameterized" + " as " + name + "<Foo> or " + name + "<? extends Foo>");
        }

        Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
        Class<?> rawObservableType = getRawType(observableType);
        if (rawObservableType == Response.class) {
            if (!(observableType instanceof ParameterizedType)) {
                throw new IllegalStateException("Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
            }
            responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
        } else if (rawObservableType == Result.class) {
            if (!(observableType instanceof ParameterizedType)) {
                throw new IllegalStateException("Result must be parameterized" + " as Result<Foo> or Result<? extends Foo>");
            }
            responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
            isResult = true;
        } else {
            responseType = observableType;
            isBody = true;
        }

        return new RxJava2CachedCallAdapter(
                mCachingSystem, responseType, mScheduler, retrofit, annotations, mAsync, isResult, isBody, isFlowable, isSingle, isMaybe, false);
    }
}
