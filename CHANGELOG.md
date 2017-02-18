# 1.2
    - Renaming CachedCall<T> into Cached<T>
    - Make RequestBuilder and ResponseUtils inner classes of CachedCall
    - Implemented new tests matching Retrofit.
    - Sync calls now are also cached

# 1.1.1
    - Added `clone()` method to `CachedCall` interface to avoid extra casting.
    - Implemented new tests and fixed old ones.

# 1.1
    - Added `remove` method for invalidate a cached call.