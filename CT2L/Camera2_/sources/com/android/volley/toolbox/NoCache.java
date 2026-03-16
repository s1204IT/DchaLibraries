package com.android.volley.toolbox;

import com.android.volley.Cache;

public class NoCache implements Cache {
    @Override
    public void clear() {
    }

    @Override
    public Cache.Entry get(String key) {
        return null;
    }

    @Override
    public void put(String key, Cache.Entry entry) {
    }

    @Override
    public void invalidate(String key, boolean fullExpire) {
    }

    @Override
    public void remove(String key) {
    }

    @Override
    public void initialize() {
    }
}
