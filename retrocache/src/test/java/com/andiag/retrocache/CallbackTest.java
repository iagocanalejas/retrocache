package com.andiag.retrocache;

import android.support.annotation.NonNull;

import com.andiag.retrocache.interfaces.CachedCall;
import com.andiag.retrocache.utils.MockCachingSystem;
import com.andiag.retrocache.utils.ToStringConverterFactory;
import com.andiag.retrocache.utils.Utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Created by IagoCanalejas on 10/01/2017.
 */

public class CallbackTest {
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
                assertNotNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
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
                assertNotNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
                latch.countDown();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
        latch.await(1, TimeUnit.SECONDS);
        call.remove();
        assertNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
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
                assertNotNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
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
                assertNotNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                fail("Failure executing the request: " + t.getMessage());
            }
        });
    }

    @Test
    public void testConcurrentAccess() {
        List<Thread> threads = new ArrayList<>();

        /* Set up the mock webserver */
        MockResponse resp = new MockResponse().setBody("VERY_BASIC_BODY");

        for (int i = 0; i < 10; i++) {
            threads.add(createWorkerThread(mRetrofit.create(DemoService.class), resp, new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    assertEquals(response.body(), "VERY_BASIC_BODY");
                    assertNotNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    fail("Failure executing the request: " + t.getMessage());
                }
            }));
        }
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertFalse("test", false);
        mMockCachingSystem.clear();
    }

    private Thread createWorkerThread(final DemoService service, final MockResponse resp, final Callback<String> callback) {
        return new Thread() {
            int sMaxNumberOfRun = 1000;
            final CachedCall<String> call = service.getHomeById(getId());

            @Override
            public void run() {
                try {
                    int numberOfRun = 0;
                    while (numberOfRun++ < sMaxNumberOfRun) {
                        Thread.sleep((long) (Math.random() * 2));
                        mServer.enqueue(resp.clone());
                        double choice = Math.random();
                        if (choice < 0.4) {
                            call.clone().enqueue(callback);
                        } else if (choice < 0.7) {
                            call.clone().refresh(callback);
                        } else if (choice < 1) {
                            call.clone().remove();
                            assertNull(mMockCachingSystem.get(Utils.urlToKey(call.request().url())));
                        } else {
                            // do nothing
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
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

        @GET("/{id}")
        CachedCall<String> getHomeById(@Path("id") long id);
    }
}
