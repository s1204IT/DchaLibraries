package com.android.server.accounts;

import android.accounts.Account;
import android.util.LruCache;
import android.util.Pair;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class TokenCache {
    private static final int MAX_CACHE_CHARS = 64000;
    private TokenLruCache mCachedTokens = new TokenLruCache();

    TokenCache() {
    }

    private static class Value {
        public final long expiryEpochMillis;
        public final String token;

        public Value(String str, long j) {
            this.token = str;
            this.expiryEpochMillis = j;
        }
    }

    private static class Key {
        public final Account account;
        public final String packageName;
        public final byte[] sigDigest;
        public final String tokenType;

        public Key(Account account, String str, String str2, byte[] bArr) {
            this.account = account;
            this.tokenType = str;
            this.packageName = str2;
            this.sigDigest = bArr;
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return Objects.equals(this.account, key.account) && Objects.equals(this.packageName, key.packageName) && Objects.equals(this.tokenType, key.tokenType) && Arrays.equals(this.sigDigest, key.sigDigest);
        }

        public int hashCode() {
            return ((this.account.hashCode() ^ this.packageName.hashCode()) ^ this.tokenType.hashCode()) ^ Arrays.hashCode(this.sigDigest);
        }
    }

    private static class TokenLruCache extends LruCache<Key, Value> {
        private HashMap<Account, Evictor> mAccountEvictors;
        private HashMap<Pair<String, String>, Evictor> mTokenEvictors;

        private class Evictor {
            private final List<Key> mKeys = new ArrayList();

            public Evictor() {
            }

            public void add(Key key) {
                this.mKeys.add(key);
            }

            public void evict() {
                Iterator<Key> it = this.mKeys.iterator();
                while (it.hasNext()) {
                    TokenLruCache.this.remove(it.next());
                }
            }
        }

        public TokenLruCache() {
            super(TokenCache.MAX_CACHE_CHARS);
            this.mTokenEvictors = new HashMap<>();
            this.mAccountEvictors = new HashMap<>();
        }

        @Override
        protected int sizeOf(Key key, Value value) {
            return value.token.length();
        }

        @Override
        protected void entryRemoved(boolean z, Key key, Value value, Value value2) {
            Evictor evictorRemove;
            if (value != null && value2 == null && (evictorRemove = this.mTokenEvictors.remove(value.token)) != null) {
                evictorRemove.evict();
            }
        }

        public void putToken(Key key, Value value) {
            Evictor evictor = this.mTokenEvictors.get(value.token);
            if (evictor == null) {
                evictor = new Evictor();
            }
            evictor.add(key);
            this.mTokenEvictors.put(new Pair<>(key.account.type, value.token), evictor);
            Evictor evictor2 = this.mAccountEvictors.get(key.account);
            if (evictor2 == null) {
                evictor2 = new Evictor();
            }
            evictor2.add(key);
            this.mAccountEvictors.put(key.account, evictor);
            put(key, value);
        }

        public void evict(String str, String str2) {
            Evictor evictor = this.mTokenEvictors.get(new Pair(str, str2));
            if (evictor != null) {
                evictor.evict();
            }
        }

        public void evict(Account account) {
            Evictor evictor = this.mAccountEvictors.get(account);
            if (evictor != null) {
                evictor.evict();
            }
        }
    }

    public void put(Account account, String str, String str2, String str3, byte[] bArr, long j) {
        Preconditions.checkNotNull(account);
        if (str == null || System.currentTimeMillis() > j) {
            return;
        }
        this.mCachedTokens.putToken(new Key(account, str2, str3, bArr), new Value(str, j));
    }

    public void remove(String str, String str2) {
        this.mCachedTokens.evict(str, str2);
    }

    public void remove(Account account) {
        this.mCachedTokens.evict(account);
    }

    public String get(Account account, String str, String str2, byte[] bArr) {
        Value value = this.mCachedTokens.get(new Key(account, str, str2, bArr));
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (value != null && jCurrentTimeMillis < value.expiryEpochMillis) {
            return value.token;
        }
        if (value != null) {
            remove(account.type, value.token);
            return null;
        }
        return null;
    }
}
