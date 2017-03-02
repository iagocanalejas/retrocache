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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

final class CallExecuteObservable<T> extends Observable<Response<T>> {
    private final Retrofit mRetrofit;
    private final Cache<String, byte[]> mCachingSystem;
    private final Call<T> mOriginalCall;

    private final Type mResponseType;
    private final Annotation[] mAnnotations;

    private final boolean mCachingActive;

    CallExecuteObservable(Cache<String, byte[]> cachingSystem, Call<T> originalCall, Type responseType, Annotation[] annotations, Retrofit retrofit) {
        this.mOriginalCall = originalCall;
        this.mCachingSystem = cachingSystem;
        this.mAnnotations = annotations;
        this.mResponseType = responseType;
        this.mRetrofit = retrofit;

        this.mCachingActive = mOriginalCall.request() != null && mOriginalCall.request().method().equals("GET");
    }


    private Response<T> getResponse(Call<T> call) throws IOException {
        if (mCachingActive && mCachingSystem.contains(CacheUtils.urlToKey(mOriginalCall.request().url()))) {
            byte[] data = mCachingSystem.get(CacheUtils.urlToKey(mOriginalCall.request().url()));
            if (data != null) {
                final T convertedData = CacheUtils.bytesToResponse(mRetrofit, mResponseType, mAnnotations, data);
                return Response.success(convertedData);
            }
        }
        return call.execute();
    }


    @Override
    protected void subscribeActual(Observer<? super Response<T>> observer) {
        // Since Call is a one-shot type, clone it for each new observer.
        Call<T> call = mOriginalCall.clone();
        observer.onSubscribe(new CallDisposable(call));

        boolean terminated = false;
        try {
            Response<T> response = getResponse(call);
            if (mCachingActive && !mCachingSystem.contains(CacheUtils.urlToKey(mOriginalCall.request().url()))) {
                mCachingSystem.put(
                        CacheUtils.urlToKey(call.request().url()), CacheUtils.responseToBytes(mRetrofit, response, mResponseType, mAnnotations));
            }
            if (!call.isCanceled()) {
                observer.onNext(response);
            }
            if (!call.isCanceled()) {
                terminated = true;
                observer.onComplete();
            }
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
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

    private static final class CallDisposable implements Disposable {
        private final Call<?> call;

        CallDisposable(Call<?> call) {
            this.call = call;
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
