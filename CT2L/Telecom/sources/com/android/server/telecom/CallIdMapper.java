package com.android.server.telecom;

import android.util.ArrayMap;
import java.util.Map;

class CallIdMapper {
    private static int sIdCount;
    private final String mCallIdPrefix;
    private final BiMap<String, Call> mCalls = new BiMap<>();

    static class BiMap<K, V> {
        private Map<K, V> mPrimaryMap = new ArrayMap();
        private Map<V, K> mSecondaryMap = new ArrayMap();

        BiMap() {
        }

        public boolean put(K k, V v) {
            if (k == null || v == null || this.mPrimaryMap.containsKey(k) || this.mSecondaryMap.containsKey(v)) {
                return false;
            }
            this.mPrimaryMap.put(k, v);
            this.mSecondaryMap.put(v, k);
            return true;
        }

        public boolean remove(K k) {
            if (k == null || !this.mPrimaryMap.containsKey(k)) {
                return false;
            }
            V value = getValue(k);
            this.mPrimaryMap.remove(k);
            this.mSecondaryMap.remove(value);
            return true;
        }

        public boolean removeValue(V v) {
            if (v == null) {
                return false;
            }
            return remove(getKey(v));
        }

        public V getValue(K k) {
            return this.mPrimaryMap.get(k);
        }

        public K getKey(V v) {
            return this.mSecondaryMap.get(v);
        }

        public void clear() {
            this.mPrimaryMap.clear();
            this.mSecondaryMap.clear();
        }
    }

    CallIdMapper(String str) {
        ThreadUtil.checkOnMainThread();
        this.mCallIdPrefix = str + "@";
    }

    void addCall(Call call, String str) {
        if (call != null) {
            ThreadUtil.checkOnMainThread();
            this.mCalls.put(str, call);
        }
    }

    void addCall(Call call) {
        ThreadUtil.checkOnMainThread();
        addCall(call, getNewId());
    }

    void removeCall(Call call) {
        if (call != null) {
            ThreadUtil.checkOnMainThread();
            this.mCalls.removeValue(call);
        }
    }

    void removeCall(String str) {
        ThreadUtil.checkOnMainThread();
        this.mCalls.remove(str);
    }

    String getCallId(Call call) {
        if (call == null) {
            return null;
        }
        ThreadUtil.checkOnMainThread();
        return this.mCalls.getKey(call);
    }

    Call getCall(Object obj) {
        ThreadUtil.checkOnMainThread();
        String str = obj instanceof String ? (String) obj : null;
        if (isValidCallId(str) || isValidConferenceId(str)) {
            return this.mCalls.getValue(str);
        }
        return null;
    }

    void clear() {
        this.mCalls.clear();
    }

    boolean isValidCallId(String str) {
        return str != null && str.startsWith(this.mCallIdPrefix);
    }

    boolean isValidConferenceId(String str) {
        return str != null;
    }

    String getNewId() {
        sIdCount++;
        return this.mCallIdPrefix + sIdCount;
    }
}
