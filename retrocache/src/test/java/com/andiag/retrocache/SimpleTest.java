package com.andiag.retrocache;

import com.andiag.retrocache.interfaces.CachedCall;
import com.andiag.retrocache.utils.MainThreadExecutor;
import com.andiag.retrocache.utils.MockCachingSystem;
import com.andiag.retrocache.utils.ToStringConverterFactory;
import com.andiag.retrocache.utils.Utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Created by IagoCanalejas on 10/01/2017.
 */

public class SimpleTest {
    /***
     * Builds a Retrofit SmartCache factory without Android executor
     */
    private CallAdapter.Factory buildSmartCacheFactory() {
        return new CachedCallFactory(mMockCachingSystem, new MainThreadExecutor());
    }

    @Rule
    public final MockWebServer mServer = new MockWebServer();
    private Retrofit mRetrofit;
    private MockCachingSystem mMockCachingSystem = new MockCachingSystem();

    @Before
    public void setUp() {
        mRetrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
    }

    @Test
    public void simpleCachedCall() {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        mServer.enqueue(resp);

        DemoService demoService = mRetrofit.create(DemoService.class);
        CachedCall<String> call = demoService.getHome();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                assertEquals(response.body(), "VERY_BASIC_BODY");
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().method(), call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
    }

    @Test
    public void removedCall() throws Exception {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        mServer.enqueue(resp);

        DemoService demoService = mRetrofit.create(DemoService.class);
        CachedCall<String> call = demoService.getHome();

        final CountDownLatch latch = new CountDownLatch(1); // Make main thread wait call to end.
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                assertEquals(response.body(), "VERY_BASIC_BODY");
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().method(), call.request().url())));
                latch.countDown();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
        latch.await(1, TimeUnit.SECONDS);
        call.remove();
        assertNull(mMockCachingSystem.get(Utils.urlToKey(
                call.request().method(), call.request().url())));
    }

    @Test
    public void refreshCall() throws Exception {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        mServer.enqueue(resp);

        DemoService demoService = mRetrofit.create(DemoService.class);
        CachedCall<String> call = demoService.getHome();
        CachedCall<String> call2Refresh = call.clone();

        final CountDownLatch latch = new CountDownLatch(1); // Make main thread wait call to end.
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                assertEquals(response.body(), "VERY_BASIC_BODY");
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().method(), call.request().url())));
                latch.countDown();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
        latch.await(1, TimeUnit.SECONDS);

        resp = new MockResponse().setBody("VERY_BASIC_REFRESHED_BODY");
        mServer.enqueue(resp);

        call2Refresh.refresh(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                assertEquals(response.body(), "VERY_BASIC_REFRESHED_BODY");
                assertNotNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().method(), call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
    }

    interface DemoService {

        @GET("/")
        CachedCall<String> getHome();

    }
}
