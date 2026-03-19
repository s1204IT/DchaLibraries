package com.android.gallery3d.app;

import java.util.HashMap;

public class TransitionStore {
    private HashMap<Object, Object> mStorage = new HashMap<>();

    public void put(Object obj, Object obj2) {
        this.mStorage.put(obj, obj2);
    }

    public <T> void putIfNotPresent(Object obj, T t) {
        this.mStorage.put(obj, get(obj, t));
    }

    public <T> T get(Object obj) {
        return (T) this.mStorage.get(obj);
    }

    public <T> T get(Object obj, T t) {
        T t2 = (T) this.mStorage.get(obj);
        return t2 == null ? t : t2;
    }

    public void clear() {
        this.mStorage.clear();
    }
}
