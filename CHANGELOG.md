# NEXT
    - Added `@Caching(enabled = false)` annotation. Used to disable cache for some `@GET` calls.
    - `@Caching(enabled = true)` annotation can be now used to cach `@POST`, `@PUT` and `@DELETE`, disabled by default.

# 1.1.1
    - Added `clone()` method to `CachedCall` interface to avoid extra casting.
    - Implemented new tests and fixed old ones.

# 1.1
    - Added `remove` method for invalidate a cached call.