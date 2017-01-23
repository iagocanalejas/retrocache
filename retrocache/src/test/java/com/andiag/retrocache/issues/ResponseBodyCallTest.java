package com.andiag.retrocache.issues;

import com.andiag.retrocache.CachedCallFactory;
import com.andiag.retrocache.interfaces.CachedCall;
import com.andiag.retrocache.utils.MainThreadExecutor;
import com.andiag.retrocache.utils.MockCachingSystem;

import org.junit.Rule;
import org.junit.Test;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.CallAdapter;
import retrofit2.http.GET;

/**
 * Created by Iago on 12/01/2017.
 */
public class ResponseBodyCallTest {

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


    }

    interface DemoService {
        @GET("/")
        CachedCall<ResponseBody> getBody();
    }

}
