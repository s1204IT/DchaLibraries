package com.android.keyguard;

import android.graphics.drawable.Drawable;
import java.util.HashMap;

public class MultiUserAvatarCache {
    private static MultiUserAvatarCache sInstance;
    private final HashMap<Integer, Drawable> mCache = new HashMap<>();

    private MultiUserAvatarCache() {
    }

    public static MultiUserAvatarCache getInstance() {
        if (sInstance == null) {
            sInstance = new MultiUserAvatarCache();
        }
        return sInstance;
    }

    public void clear(int userId) {
        this.mCache.remove(Integer.valueOf(userId));
    }

    public Drawable get(int userId) {
        return this.mCache.get(Integer.valueOf(userId));
    }

    public void put(int userId, Drawable image) {
        this.mCache.put(Integer.valueOf(userId), image);
    }
}
