package com.andiag.retrocache;

import com.andiag.retrocache.annotations.Caching;
import com.andiag.retrocache.interfaces.CachedCall;
import com.andiag.retrocache.utils.MainThreadExecutor;
import com.andiag.retrocache.utils.MockCachingSystem;
import com.andiag.retrocache.utils.ToStringConverterFactory;
import com.andiag.retrocache.utils.Utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Created by Canalejas on 11/02/2017.
 */

public class CachingAnnotationTest {

    @Rule
    public final MockWebServer mServer = new MockWebServer();
    private Retrofit mRetrofit;
    private MockCachingSystem mMockCachingSystem = new MockCachingSystem();

    /***
     * Builds a Retrofit SmartCache factory without Android executor
     */
    private CallAdapter.Factory buildSmartCacheFactory() {
        return new CachedCallFactory(mMockCachingSystem, new MainThreadExecutor());
    }

    @Before
    public void setUp() {
        mRetrofit = new Retrofit.Builder()
                .baseUrl(mServer.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
    }

    @Test
    public void cachedPost() {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        mServer.enqueue(resp);

        DemoService demoService = mRetrofit.create(DemoService.class);
        CachedCall<String> call = demoService.postHome("SIMPLE_BODY");

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
    public void disabledCache() {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        mServer.enqueue(resp);

        DemoService demoService = mRetrofit.create(DemoService.class);
        CachedCall<String> call = demoService.getHome();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                assertEquals(response.body(), "VERY_BASIC_BODY");
                assertNull(mMockCachingSystem.get(
                        Utils.urlToKey(call.request().method(), call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
    }

    interface DemoService {

        @Caching
        @POST("/")
        CachedCall<String> postHome(@Body String body);

        @Caching(enabled = false)
        @GET("/")
        CachedCall<String> getHome();

    }

}
