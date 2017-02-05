Android RetroCache
=================
[![API](https://img.shields.io/badge/API-12%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=12)
[![Build Status](https://travis-ci.org/iagocanalejas/retrocache.svg?branch=master)](https://travis-ci.org/iagocanalejas/retrocache)
[![](https://jitpack.io/v/iagocanalejas/retrocache.svg)](https://jitpack.io/#iagocanalejas/retrocache)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RetroCache-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/5064)

# Description
This library provide an easy way for configure retrofit with a 2 layer cache (RAM and Disk).
To see more details about the cache used visit [DualCache](https://github.com/iagocanalejas/dualcache)

This allow you to improve the data usage of your apps.

# Setup

- Ensure you can pull artifacts from JitPack :
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```
- And add to your module gradle file :
```gradle
android {
    packagingOptions {
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/NOTICE'
    }
}

dependencies {
    compile 'com.github.iagocanalejas:retrocache:<VERSION>'
}
```

# Usage

1. In your Service Api Interface change all `Call<T>` return types for `CachedCall<T>`
    ```java
    public interface ApiService {

        @GET("/")
        CachedCall<MyObject> getResource();

    }
    ```

2. Build your cache. Library include some shortcuts for this tasks but you can always build a full configured cache using  [DualCache](https://github.com/iagocanalejas/dualcache). Just remember cache **key** type must be `String` and **entry** type must be `byte[]`

    - Basic cache using only RAM.
    ```java
       DualCache<String, byte[]> mCache = RetroCache.getRamCache(APP_VERSION);
    ```

    - Basic cache using both, Disk and Ram, layers.
    ```java
       DualCache<String, byte[]> mCache = RetroCache.getDualCache(context, APP_VERSION);
    ```

    - Basic cache using both, Disk and Ram, layers and setting a life time for entries.
    ```java
       DualCache<String, byte[]> mCache = RetroCache.getVolatileCache(context, APP_VERSION);
    ```

    - You can also get a non configured Builder.
    ```java
       Builder<String, byte[]> builder = RetroCache.getBuilder(APP_VERSION);
    ```

    For see default values in this caches take a look at [RetroCache](retrocache/src/main/java/com/andiag/retrocache/cache/RetroCache.java)

    **Important**
    - APP_VERSION is a static integer. When APP_VERSION changes all caches are automatically cleared. It's recommended to use BuildConfig.VERSION_CODE as BuildConfig.VERSION_CODE

3. Add the cache to your Retrofit service.

    ```java
    retrofitBuilder.addCallAdapterFactory(new CachedCallFactory(mCache));
    ```

4. Use it as normal retrofit. Just remember to use `CachedCall`. All retrofit methods are included, and you can also use methods included in `Included` section.

# Included
In addition to normal retrofit usage you can also call `refresh(callback)` to avoid looking in the cache or `remove()` to invalidate a cached call.

    ```java
    CachedCall<MyObject> call = ...
    call.refresh(new Callback<MyObject>() {
       ...
    });
    call.remove();
    ```

You can also tell RetroCache when a call should be cached using `@Caching(active = true/false)`.

    ```java
    public interface ApiService {

        @Caching(active = false)
        @GET("/")
        CachedCall<MyObject> getResource();

    }
    ```

**This feature is by default enabled to `@GET` and disabled for the rest of methods.**

# Pull Requests
I welcome and encourage all pull requests. Here are some basic rules to follow to ensure timely addition of your request:
  1. Match coding style (braces, spacing, etc.) This is best achieved using CMD+Option+L (on Mac) or Ctrl+Alt+L on Windows to reformat code with Android Studio defaults.
  2. Pull Request must pass all tests with `gradlew check` for styling, and `gradlew test` for unit tests.
  2. If its a feature, bugfix, or anything please only change code to what you specify.
  3. Please keep PR titles easy to read and descriptive of changes, this will make them easier to merge.
  4. Pull requests _must_ be made against `develop` branch. Any other branch (unless specified by the maintainers) will get rejected.
  5. Have fun!


# Maintained By
[IagoCanalejas](https://github.com/iagocanalejas) ([@iagocanalejas](https://twitter.com/Iagocanalejas))

[Andy](https://github.com/andy135) ([@ANDYear21](https://twitter.com/ANDYear21))

License
=======

    Copyright 2016 IagoCanalejas.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
