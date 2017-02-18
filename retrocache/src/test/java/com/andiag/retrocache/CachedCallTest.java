package com.andiag.retrocache;

import com.andiag.retrocache.cache.MainThreadExecutor;
import com.andiag.retrocache.cache.MockCachingSystem;
import com.andiag.retrocache.cache.ToStringConverterFactory;
import com.andiag.retrocache.cache.Utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertNull;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CachedCallTest {

    @Rule
    public final MockWebServer mServer = new MockWebServer();
    private MockCachingSystem mMockCachingSystem = new MockCachingSystem();

    interface Service {
        @GET("/")
        Cached<String> getString();

        @GET("/")
        Cached<ResponseBody> getBody();

        @GET("/")
        @Streaming
        Cached<ResponseBody> getStreamingBody();

    }

    private CallAdapter.Factory buildSmartCacheFactory() {
        return new CachedCallFactory(mMockCachingSystem, new MainThreadExecutor());
    }

    @Test
    public void http200Sync() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        Response<String> response = example.getString().execute();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");
    }

    @Test
    public void http200Async() throws InterruptedException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        example.getString().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                latch.countDown();
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        Response<String> response = responseRef.get();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");
    }

    @Test
    public void http404Sync() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

        Response<String> response = example.getString().execute();
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(404);
        assertThat(response.errorBody().string()).isEqualTo("Hi");
    }

    @Test
    public void http404Async() throws InterruptedException, IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

        final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        example.getString().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                latch.countDown();
                assertNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        Response<String> response = responseRef.get();
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(404);
        assertThat(response.errorBody().string()).isEqualTo("Hi");
    }

    @Test
    public void transportProblemSync() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        Cached<String> call = example.getString();
        try {
            call.execute();
            fail();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void transportProblemAsync() throws InterruptedException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        final AtomicReference<Throwable> failureRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        example.getString().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                throw new AssertionError();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                failureRef.set(t);
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        Throwable failure = failureRef.get();
        assertThat(failure).isInstanceOf(IOException.class);
    }

    @Test
    public void conversionProblemIncomingMaskedByConverterIsUnwrapped() throws IOException {
        // MWS has no way to trigger IOExceptions during the response body so use an interceptor.
        OkHttpClient client = new OkHttpClient.Builder() //
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        okhttp3.Response response = chain.proceed(chain.request());
                        ResponseBody body = response.body();
                        BufferedSource source = Okio.buffer(new ForwardingSource(body.source()) {
                            @Override
                            public long read(Buffer sink, long byteCount) throws IOException {
                                throw new IOException("cause");
                            }
                        });
                        body = ResponseBody.create(
                                body.contentType(), body.contentLength(), source);
                        return response.newBuilder().body(body).build();
                    }
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .client(client)
                .addConverterFactory(new ToStringConverterFactory() {
                    @Override
                    public Converter<ResponseBody, ?> responseBodyConverter(
                            Type type,
                            Annotation[] annotations,
                            Retrofit retrofit) {

                        return new Converter<ResponseBody, String>() {
                            @Override
                            public String convert(ResponseBody value) throws IOException {
                                try {
                                    return value.string();
                                } catch (IOException e) {
                                    throw new RuntimeException("wrapper", e);
                                }
                            }
                        };
                    }
                })
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        Cached<String> call = example.getString();
        try {
            call.execute();
            fail();
        } catch (IOException e) {
            assertThat(e).hasMessage("cause");
        }
    }

    @Test
    public void http204SkipsConverter() throws IOException {
        final Converter<ResponseBody, String> converter =
                spy(new Converter<ResponseBody, String>() {
                    @Override
                    public String convert(ResponseBody value) throws IOException {
                        return value.string();
                    }
                });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory() {
                    @Override
                    public Converter<ResponseBody, ?> responseBodyConverter(
                            Type type,
                            Annotation[] annotations,
                            Retrofit retrofit) {

                        return converter;
                    }
                })
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setStatus("HTTP/1.1 204 Nothin"));

        Response<String> response = example.getString().execute();
        assertThat(response.code()).isEqualTo(204);
        assertThat(response.body()).isNull();
        verifyNoMoreInteractions(converter);
    }

    @Test
    public void http205SkipsConverter() throws IOException {
        final Converter<ResponseBody, String> converter = spy(
                new Converter<ResponseBody, String>() {
                    @Override
                    public String convert(ResponseBody value) throws IOException {
                        return value.string();
                    }
                });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory() {
                    @Override
                    public Converter<ResponseBody, ?> responseBodyConverter(
                            Type type,
                            Annotation[] annotations,
                            Retrofit retrofit) {

                        return converter;
                    }
                })
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setStatus("HTTP/1.1 205 Nothin"));

        Response<String> response = example.getString().execute();
        assertThat(response.code()).isEqualTo(205);
        assertThat(response.body()).isNull();
        verifyNoMoreInteractions(converter);
    }

    @Test
    public void executeCallOnce() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);
        mServer.enqueue(new MockResponse());
        Cached<String> call = example.getString();
        call.execute();
        try {
            call.execute();
            fail();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Already executed.");
        }
    }

    @Test
    public void successfulRequestResponseWhenMimeTypeMissing() throws Exception {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi").removeHeader("Content-Type"));

        Response<String> response = example.getString().execute();
        assertThat(response.body()).isEqualTo("Hi");
    }

    @Test
    public void responseBody() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("1234"));

        Response<ResponseBody> response = example.getBody().execute();
        assertThat(response.body().string()).isEqualTo("1234");
    }

    @Test
    public void responseBodyBuffers() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse()
                .setBody("1234")
                .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

        Call<ResponseBody> buffered = example.getBody();
        // When buffering we will detect all socket problems before returning the Response.
        try {
            buffered.execute();
            fail();
        } catch (IOException e) {
            assertThat(e).hasMessage("unexpected end of stream");
        }
    }

    @Test
    public void responseBodyStreams() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse()
                .setBody("1234")
                .setSocketPolicy(DISCONNECT_DURING_RESPONSE_BODY));

        Response<ResponseBody> response = example.getStreamingBody().execute();

        ResponseBody streamedBody = response.body();
        // When streaming we only detect socket problems as the ResponseBody is read.
        try {
            streamedBody.string();
            fail();
        } catch (IOException e) {
            assertThat(e).hasMessage("unexpected end of stream");
        }
    }

    @Test
    public void rawResponseContentTypeAndLengthButNoSource() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi").addHeader("Content-Type", "text/greeting"));

        Response<String> response = example.getString().execute();
        assertThat(response.body()).isEqualTo("Hi");
        ResponseBody rawBody = response.raw().body();
        assertThat(rawBody.contentLength()).isEqualTo(2);
        assertThat(rawBody.contentType().toString()).isEqualTo("text/greeting");
        try {
            rawBody.source();
            fail();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Cannot read raw response body of a converted body.");
        }
    }

    @Test
    public void emptyResponse() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("").addHeader("Content-Type", "text/stringy"));

        Response<String> response = example.getString().execute();
        assertThat(response.body()).isEqualTo("");
        ResponseBody rawBody = response.raw().body();
        assertThat(rawBody.contentLength()).isEqualTo(0);
        assertThat(rawBody.contentType().toString()).isEqualTo("text/stringy");
    }

    @Test
    public void reportsExecutedSync() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        Cached<String> call = example.getString();
        assertThat(call.isExecuted()).isFalse();

        call.execute();
        assertThat(call.isExecuted()).isTrue();
    }

    @Test
    public void reportsExecutedAsync() throws InterruptedException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        Cached<String> call = example.getString();
        assertThat(call.isExecuted()).isFalse();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
            }
        });
        assertThat(call.isExecuted()).isTrue();
    }

    @Test
    public void cancelBeforeExecute() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service service = retrofit.create(Service.class);
        Cached<String> call = service.getString();

        call.cancel();
        assertThat(call.isCanceled()).isTrue();

        try {
            call.execute();
            fail();
        } catch (IOException e) {
            assertThat(e).hasMessage("Canceled");
        }
    }

    @Test
    public void cancelBeforeEnqueue() throws Exception {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service service = retrofit.create(Service.class);
        Cached<String> call = service.getString();

        call.cancel();
        assertThat(call.isCanceled()).isTrue();

        final AtomicReference<Throwable> failureRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                throw new AssertionError();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                failureRef.set(t);
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, SECONDS));
        assertThat(failureRef.get()).hasMessage("Canceled");
    }

    @Test
    public void cloningExecutedRequestDoesNotCopyState() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service service = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));
        mServer.enqueue(new MockResponse().setBody("Hello"));

        Cached<String> call = service.getString();
        assertThat(call.execute().body()).isEqualTo("Hi");

        Cached<String> cloned = call.clone();
        assertThat(cloned.execute().body()).isEqualTo("Hello");
    }

    @Test
    public void cancelRequest() throws InterruptedException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service service = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        Cached<String> call = service.getString();

        final AtomicReference<Throwable> failureRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                throw new AssertionError();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                failureRef.set(t);
                latch.countDown();
            }
        });

        call.cancel();
        assertThat(call.isCanceled()).isTrue();

        assertTrue(latch.await(10, SECONDS));
        assertThat(failureRef.get()).isInstanceOf(IOException.class).hasMessage("Canceled");
    }

    @Test
    public void removeCall() throws Exception {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        final CountDownLatch latch = new CountDownLatch(1);
        Cached<String> call = example.getString();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                latch.countDown();
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        call.remove();
        Assert.assertNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
    }

    @Test
    public void refreshCall() throws Exception {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Cached<String> call = example.getString();
        Cached<String> call2Refresh = example.getString();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                latch.countDown();
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        Response<String> response = responseRef.get();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");

        final CountDownLatch refreshLatch = new CountDownLatch(1);
        mServer.enqueue(new MockResponse().setBody("Refreshed Hi"));

        call2Refresh.refresh(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                refreshLatch.countDown();
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(refreshLatch.await(10, SECONDS));

        response = responseRef.get();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Refreshed Hi");
    }

    @Test
    public void refresh404Call() throws Exception {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        Service example = retrofit.create(Service.class);

        mServer.enqueue(new MockResponse().setBody("Hi"));

        final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Cached<String> call = example.getString();
        Cached<String> call2Refresh = call.clone();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                latch.countDown();
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        Response<String> response = responseRef.get();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo("Hi");

        final CountDownLatch refreshLatch = new CountDownLatch(1);
        mServer.enqueue(new MockResponse().setResponseCode(404).setBody("Refreshed Hi"));

        call2Refresh.refresh(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                refreshLatch.countDown();
                assertNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });
        assertTrue(refreshLatch.await(10, SECONDS));

        Response<String> refreshedResponse = responseRef.get();
        assertThat(refreshedResponse.isSuccessful()).isFalse();
        assertThat(refreshedResponse.code()).isEqualTo(404);
        assertThat(refreshedResponse.errorBody().string()).isEqualTo("Refreshed Hi");
    }

}
