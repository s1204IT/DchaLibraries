package com.android.server.telecom;

import android.util.ArrayMap;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Map;

@VisibleForTesting
public class CallIdMapper {
    private ICallInfo mCallInfo;
    private final BiMap<String, Call> mCalls = new BiMap<>();

    public interface ICallInfo {
        String getCallId(Call call);
    }

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

    public CallIdMapper(ICallInfo iCallInfo) {
        this.mCallInfo = iCallInfo;
    }

    void addCall(Call call, String str) {
        if (call == null) {
            return;
        }
        this.mCalls.put(str, call);
    }

    void addCall(Call call) {
        addCall(call, this.mCallInfo.getCallId(call));
    }

    void removeCall(Call call) {
        if (call == null) {
            return;
        }
        this.mCalls.removeValue(call);
    }

    void removeCall(String str) {
        this.mCalls.remove(str);
    }

    String getCallId(Call call) {
        if (call == null || this.mCalls.getKey(call) == null) {
            return null;
        }
        return this.mCallInfo.getCallId(call);
    }

    Call getCall(Object obj) {
        String str;
        if (obj instanceof String) {
            str = (String) obj;
        } else {
            str = null;
        }
        return this.mCalls.getValue(str);
    }

    void clear() {
        this.mCalls.clear();
    }
}
