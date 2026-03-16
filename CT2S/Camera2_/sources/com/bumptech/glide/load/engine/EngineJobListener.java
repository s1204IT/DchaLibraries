package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;

public interface EngineJobListener {
    void onEngineJobCancelled(Key key);

    void onEngineJobComplete(Key key, Resource resource);
}
