package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public class OriginalEngineKey implements Key {
    private String id;

    public OriginalEngineKey(String id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OriginalEngineKey)) {
            return false;
        }
        OriginalEngineKey that = (OriginalEngineKey) o;
        return this.id.equals(that.id);
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        messageDigest.update(this.id.getBytes("UTF-8"));
    }
}
