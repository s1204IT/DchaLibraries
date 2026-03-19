package java.net;

import libcore.util.BasicLruCache;

class AddressCache {
    private static final int MAX_ENTRIES = 16;
    private static final long TTL_NANOS = 2000000000;
    private final BasicLruCache<AddressCacheKey, AddressCacheEntry> cache = new BasicLruCache<>(16);

    AddressCache() {
    }

    static class AddressCacheKey {
        private final String mHostname;
        private final int mNetId;

        AddressCacheKey(String str, int i) {
            this.mHostname = str;
            this.mNetId = i;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AddressCacheKey)) {
                return false;
            }
            AddressCacheKey addressCacheKey = (AddressCacheKey) obj;
            return this.mHostname.equals(addressCacheKey.mHostname) && this.mNetId == addressCacheKey.mNetId;
        }

        public int hashCode() {
            return (31 * (527 + this.mNetId)) + this.mHostname.hashCode();
        }
    }

    static class AddressCacheEntry {
        final long expiryNanos = System.nanoTime() + AddressCache.TTL_NANOS;
        final Object value;

        AddressCacheEntry(Object obj) {
            this.value = obj;
        }
    }

    public void clear() {
        this.cache.evictAll();
    }

    public Object get(String str, int i) {
        AddressCacheEntry addressCacheEntry = this.cache.get(new AddressCacheKey(str, i));
        if (addressCacheEntry != null && addressCacheEntry.expiryNanos >= System.nanoTime()) {
            return addressCacheEntry.value;
        }
        return null;
    }

    public void put(String str, int i, InetAddress[] inetAddressArr) {
        this.cache.put(new AddressCacheKey(str, i), new AddressCacheEntry(inetAddressArr));
    }

    public void putUnknownHost(String str, int i, String str2) {
        this.cache.put(new AddressCacheKey(str, i), new AddressCacheEntry(str2));
    }
}
