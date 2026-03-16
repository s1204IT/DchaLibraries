package com.android.camera;

import android.content.Context;
import android.media.SoundPool;
import android.util.SparseIntArray;

public class SoundPlayer {
    private final Context mAppContext;
    private final SparseIntArray mResourceToSoundId = new SparseIntArray();
    private final SoundPool mSoundPool;

    public SoundPlayer(Context appContext) {
        this.mAppContext = appContext;
        int audioType = SoundClips.getAudioTypeForSoundPool();
        this.mSoundPool = new SoundPool(1, audioType, 0);
    }

    public void loadSound(int resourceId) {
        int soundId = this.mSoundPool.load(this.mAppContext, resourceId, 1);
        this.mResourceToSoundId.put(resourceId, soundId);
    }

    public void play(int resourceId, float volume) {
        Integer soundId = Integer.valueOf(this.mResourceToSoundId.get(resourceId));
        if (soundId == null) {
            throw new IllegalStateException("Sound not loaded. Must call #loadSound first.");
        }
        this.mSoundPool.play(soundId.intValue(), volume, volume, 0, 0, 1.0f);
    }

    public void unloadSound(int resourceId) {
        Integer soundId = Integer.valueOf(this.mResourceToSoundId.get(resourceId));
        if (soundId == null) {
            throw new IllegalStateException("Sound not loaded. Must call #loadSound first.");
        }
        this.mSoundPool.unload(soundId.intValue());
    }

    public void release() {
        this.mSoundPool.release();
    }
}
