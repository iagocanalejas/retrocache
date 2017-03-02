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

import java.io.IOException;

import retrofit2.Response;

/**
 * The result of executing an HTTP request.
 */
public final class Result<T> {
    private final Response<T> mResponse;
    private final Throwable mError;

    private Result(Response<T> response, Throwable error) {
        this.mResponse = response;
        this.mError = error;
    }

    public static <T> Result<T> error(Throwable error) {
        if (error == null) {
            throw new NullPointerException("mError == null");
        }
        return new Result<>(null, error);
    }

    public static <T> Result<T> response(Response<T> response) {
        if (response == null) {
            throw new NullPointerException("mResponse == null");
        }
        return new Result<>(response, null);
    }

    /**
     * The mResponse received from executing an HTTP request. Only present when {@link #isError()} is
     * false, null otherwise.
     */
    public Response<T> response() {
        return mResponse;
    }

    /**
     * The mError experienced while attempting to execute an HTTP request. Only present when {@link
     * #isError()} is true, null otherwise.
     * <p>
     * If the mError is an {@link IOException} then there was a problem with the transport to the
     * remote server. Any other exception type indicates an unexpected failure and should be
     * considered fatal (configuration mError, programming mError, etc.).
     */
    public Throwable error() {
        return mError;
    }

    /**
     * {@code true} if the request resulted in an mError. See {@link #error()} for the cause.
     */
    public boolean isError() {
        return mError != null;
    }
}
