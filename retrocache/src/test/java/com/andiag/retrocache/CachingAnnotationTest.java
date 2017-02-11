package com.andiag.retrocache;

import com.andiag.retrocache.utils.MainThreadExecutor;
import com.andiag.retrocache.utils.MockCachingSystem;
import com.andiag.retrocache.utils.ToStringConverterFactory;

import org.junit.Before;
import org.junit.Rule;

import okhttp3.mockwebserver.MockWebServer;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

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

    interface DemoService {


    }

}
