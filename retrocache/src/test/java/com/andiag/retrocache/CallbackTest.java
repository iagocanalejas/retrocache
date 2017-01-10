package com.andiag.retrocache;

import android.support.annotation.NonNull;

import com.andiag.retrocache.interfaces.CachedCall;
import com.andiag.retrocache.utils.MockCachingSystem;
import com.andiag.retrocache.utils.ToStringConverterFactory;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Canalejas on 10/01/2017.
 */

public class CallbackTest {
    /***
     * Builds a Retrofit SmartCache factory without Android executor
     */
    private CallAdapter.Factory buildSmartCacheFactory() {
        return new CachedCallFactory(new MockCachingSystem(), new MainThreadExecutor());
    }

    @Rule
    public final MockWebServer server = new MockWebServer();

    @Test
    public void simpleCall() throws InterruptedException {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        server.enqueue(resp);

        Retrofit r = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        DemoService demoService = r.create(DemoService.class);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
        demoService.getHome().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                latch.countDown();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
                fail("Failure executing the request");
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(responseRef.get().body(), "VERY_BASIC_BODY");

    }

    @Test
    public void cachedCall() throws Exception {
        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");
        server.enqueue(resp);

        Retrofit r = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(new ToStringConverterFactory())
                .addCallAdapterFactory(buildSmartCacheFactory())
                .build();
        DemoService demoService = r.create(DemoService.class);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Response<String>> responseRef = new AtomicReference<>();
        demoService.getHome().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                responseRef.set(response);
                latch.countDown();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(responseRef.get().body(), "VERY_BASIC_BODY");

        final CountDownLatch latch2 = new CountDownLatch(1);
        final AtomicReference<Response<String>> response2Ref = new AtomicReference<>();
        demoService.getHome().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                latch2.countDown();
                if (latch2.getCount() == 0) { // the cache hit one.
                    response2Ref.set(response);
                } else { // the network one.
                    assertEquals(response.body(), response2Ref.get().body());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        server.shutdown();
    }

    private static class MainThreadExecutor implements Executor {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    }

    interface DemoService {
        @GET("/")
        CachedCall<String> getHome();
    }
}
