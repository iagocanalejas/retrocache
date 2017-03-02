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

import com.iagocanalejas.dualcache.interfaces.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

final class RxJava2CachedCallAdapter<R> implements CallAdapter<R, Object> {
    private final Cache<String, byte[]> mCachingSystem;
    private final Type mResponseType;
    private final Scheduler mScheduler;
    private final boolean mAsync;
    private final boolean mResult;
    private final boolean mBody;
    private final boolean mFlowable;
    private final boolean mSingle;
    private final boolean mMaybe;
    private final boolean mCompletable;
    private final Retrofit mRetrofit;
    private final Annotation[] mAnnotations;

    RxJava2CachedCallAdapter(Cache<String, byte[]> cachingSystem, Type responseType, Scheduler scheduler, Retrofit retrofit, Annotation[] annotations,
                             boolean mAsync, boolean mResult, boolean mBody, boolean mFlowable, boolean mSingle, boolean mMaybe,
                             boolean mCompletable) {

        this.mCachingSystem = cachingSystem;
        this.mResponseType = responseType;
        this.mScheduler = scheduler;
        this.mRetrofit = retrofit;
        this.mAnnotations = annotations;
        this.mAsync = mAsync;
        this.mResult = mResult;
        this.mBody = mBody;
        this.mFlowable = mFlowable;
        this.mSingle = mSingle;
        this.mMaybe = mMaybe;
        this.mCompletable = mCompletable;
    }

    @Override
    public Type responseType() {
        return mResponseType;
    }

    @Override
    public Object adapt(Call<R> call) {
        Observable<Response<R>> responseObservable = mAsync
                ? new CallEnqueueObservable<>(mCachingSystem, call, mResponseType, mAnnotations, mRetrofit)
                : new CallExecuteObservable<>(mCachingSystem, call, mResponseType, mAnnotations, mRetrofit);

        Observable<?> observable;
        if (mResult) {
            observable = new ResultObservable<>(responseObservable);
        } else if (mBody) {
            observable = new BodyObservable<>(responseObservable);
        } else {
            observable = responseObservable;
        }

        if (mScheduler != null) {
            observable = observable.subscribeOn(mScheduler);
        }

        if (mFlowable) {
            return observable.toFlowable(BackpressureStrategy.LATEST);
        }
        if (mSingle) {
            return observable.singleOrError();
        }
        if (mMaybe) {
            return observable.singleElement();
        }
        if (mCompletable) {
            return observable.ignoreElements();
        }
        return observable;
    }
}
