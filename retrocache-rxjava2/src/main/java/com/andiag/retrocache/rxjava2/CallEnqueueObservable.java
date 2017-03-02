/*
 * Copyright (C) 2016 Jake Wharton
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

import com.andiag.commons.CacheUtils;
import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

final class CallEnqueueObservable<T> extends Observable<Response<T>> {
    private final Retrofit mRetrofit;
    private final Cache<String, byte[]> mCachingSystem;
    private final Call<T> mOriginalCall;

    private final Type mResponseType;
    private final Annotation[] mAnnotations;

    private final boolean mCachingActive;

    CallEnqueueObservable(Cache<String, byte[]> cachingSystem, Call<T> originalCall, Type responseType, Annotation[] annotations, Retrofit retrofit) {
        this.mOriginalCall = originalCall;
        this.mCachingSystem = cachingSystem;
        this.mAnnotations = annotations;
        this.mResponseType = responseType;
        this.mRetrofit = retrofit;

        this.mCachingActive = mOriginalCall.request() != null && mOriginalCall.request().method().equals("GET");
    }

    @Override
    protected void subscribeActual(Observer<? super Response<T>> observer) {
        if (mCachingActive && mCachingSystem.contains(CacheUtils.urlToKey(mOriginalCall.request().url()))) {
            byte[] data = mCachingSystem.get(CacheUtils.urlToKey(mOriginalCall.request().url()));
            if (data != null) {
                final T convertedData = CacheUtils.bytesToResponse(mRetrofit, mResponseType, mAnnotations, data);
                observer.onNext(Response.success(convertedData));
                observer.onComplete();
            }
            return;
        }
        // Since Call is a one-shot type, clone it for each new observer.
        Call<T> call = mOriginalCall.clone();
        CallCallback<T> callback = new CallCallback<>(call, observer, mCachingSystem, mResponseType, mAnnotations, mRetrofit, mCachingActive);
        observer.onSubscribe(callback);
        call.enqueue(callback);
    }

    private static final class CallCallback<T> implements Disposable, Callback<T> {
        private final Call<?> call;
        private final Observer<? super Response<T>> observer;
        private final Cache<String, byte[]> mCachingSystem;
        private final boolean mCachingActive;
        private final Retrofit mRetrofit;
        private final Type mResponseType;
        private final Annotation[] mAnnotations;
        boolean terminated = false;

        CallCallback(Call<?> call, Observer<? super Response<T>> observer, Cache<String, byte[]> cachingSystem, Type responseType,
                     Annotation[] annotations, Retrofit retrofit, boolean cachingActive) {

            this.call = call;
            this.observer = observer;
            this.mCachingSystem = cachingSystem;
            this.mAnnotations = annotations;
            this.mResponseType = responseType;
            this.mRetrofit = retrofit;
            this.mCachingActive = cachingActive;
        }

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            if (call.isCanceled()) return;

            if (mCachingActive) {
                mCachingSystem.put(
                        CacheUtils.urlToKey(call.request().url()), CacheUtils.responseToBytes(mRetrofit, response, mResponseType, mAnnotations));
            }

            try {
                observer.onNext(response);

                if (!call.isCanceled()) {
                    terminated = true;
                    observer.onComplete();
                }
            } catch (Throwable t) {
                if (terminated) {
                    RxJavaPlugins.onError(t);
                } else if (!call.isCanceled()) {
                    try {
                        observer.onError(t);
                    } catch (Throwable inner) {
                        Exceptions.throwIfFatal(inner);
                        RxJavaPlugins.onError(new CompositeException(t, inner));
                    }
                }
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            if (call.isCanceled()) return;

            try {
                observer.onError(t);
            } catch (Throwable inner) {
                Exceptions.throwIfFatal(inner);
                RxJavaPlugins.onError(new CompositeException(t, inner));
            }
        }

        @Override
        public void dispose() {
            call.cancel();
        }

        @Override
        public boolean isDisposed() {
            return call.isCanceled();
        }
    }
}
