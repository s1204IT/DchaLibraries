package com.mediatek.camera.common.sound;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.SparseIntArray;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.HashMap;

public class SoundPlayer implements SoundPool.OnLoadCompleteListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SoundPlayer.class.getSimpleName());
    private final Context mAppContext;
    private float mVolume;
    private final SparseIntArray mResourceToSoundId = new SparseIntArray();
    private final HashMap<Integer, Boolean> mSoundIDReadyMap = new HashMap<>();
    private final SoundPool mSoundPool = new SoundPool(1, getAudioTypeForSoundPool(), 0);
    private int mSoundIDToPlay = 0;

    public SoundPlayer(Context context) {
        this.mAppContext = context;
        this.mSoundPool.setOnLoadCompleteListener(this);
    }

    private void unloadSound(int i) {
        Integer numValueOf = Integer.valueOf(this.mResourceToSoundId.get(i));
        if (numValueOf == null) {
            throw new IllegalStateException("Sound not loaded. Must call #loadSound first.");
        }
        this.mSoundPool.unload(numValueOf.intValue());
    }

    public void unloadSound() {
        int size = this.mResourceToSoundId.size();
        for (int i = 0; i < size; i++) {
            unloadSound(this.mResourceToSoundId.keyAt(i));
        }
        this.mResourceToSoundId.clear();
    }

    public void release() {
        this.mSoundPool.release();
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int i, int i2) {
        if (i2 != 0) {
            LogHelper.e(TAG, "onLoadComplete : " + i + " load failed , status is " + i2);
            return;
        }
        LogHelper.d(TAG, "onLoadComplete : " + i + " load success");
        this.mSoundIDReadyMap.put(Integer.valueOf(i), true);
        if (i == this.mSoundIDToPlay) {
            this.mSoundIDToPlay = 0;
            this.mSoundPool.play(i, this.mVolume, this.mVolume, 0, 0, 1.0f);
        }
    }

    private int getAudioTypeForSoundPool() {
        return getIntFieldIfExists(AudioManager.class, "STREAM_SYSTEM_ENFORCED", null, 2);
    }

    private int getIntFieldIfExists(Class<?> cls, String str, Class<?> cls2, int i) {
        try {
            return cls.getDeclaredField(str).getInt(cls2);
        } catch (IllegalAccessException e) {
            return i;
        } catch (NoSuchFieldException e2) {
            return i;
        }
    }
}
