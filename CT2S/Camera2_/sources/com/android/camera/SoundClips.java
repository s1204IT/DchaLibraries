package com.android.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.SoundPool;
import com.android.camera.debug.Log;
import com.android.camera.util.ApiHelper;
import com.android.camera2.R;

public class SoundClips {
    public static final int FOCUS_COMPLETE = 0;
    public static final int SHUTTER_CLICK = 3;
    public static final int START_VIDEO_RECORDING = 1;
    public static final int STOP_VIDEO_RECORDING = 2;

    public interface Player {
        void play(int i);

        void release();
    }

    public static Player getPlayer(Context context) {
        return ApiHelper.HAS_MEDIA_ACTION_SOUND ? new MediaActionSoundPlayer() : new SoundPoolPlayer(context);
    }

    public static int getAudioTypeForSoundPool() {
        return ApiHelper.getIntFieldIfExists(AudioManager.class, "STREAM_SYSTEM_ENFORCED", null, 2);
    }

    @TargetApi(16)
    private static class MediaActionSoundPlayer implements Player {
        private static final Log.Tag TAG = new Log.Tag("MediaActSndPlayer");
        private MediaActionSound mSound = new MediaActionSound();

        @Override
        public void release() {
            if (this.mSound != null) {
                this.mSound.release();
                this.mSound = null;
            }
        }

        public MediaActionSoundPlayer() {
            this.mSound.load(2);
            this.mSound.load(3);
            this.mSound.load(1);
            this.mSound.load(0);
        }

        @Override
        public synchronized void play(int action) {
            switch (action) {
                case 0:
                    this.mSound.play(1);
                    break;
                case 1:
                    this.mSound.play(2);
                    break;
                case 2:
                    this.mSound.play(3);
                    break;
                case 3:
                    this.mSound.play(0);
                    break;
                default:
                    Log.w(TAG, "Unrecognized action:" + action);
                    break;
            }
        }
    }

    private static class SoundPoolPlayer implements Player, SoundPool.OnLoadCompleteListener {
        private static final int ID_NOT_LOADED = 0;
        private static final int NUM_SOUND_STREAMS = 1;
        private Context mContext;
        private final boolean[] mSoundIDReady;
        private final int[] mSoundIDs;
        private static final Log.Tag TAG = new Log.Tag("SoundPoolPlayer");
        private static final int[] SOUND_RES = {R.raw.focus_complete, R.raw.video_record, R.raw.video_record, R.raw.shutter};
        private final int[] mSoundRes = {0, 1, 2, 3};
        private int mSoundIDToPlay = 0;
        private SoundPool mSoundPool = new SoundPool(1, SoundClips.getAudioTypeForSoundPool(), 0);

        public SoundPoolPlayer(Context context) {
            this.mContext = context;
            this.mSoundPool.setOnLoadCompleteListener(this);
            this.mSoundIDs = new int[SOUND_RES.length];
            this.mSoundIDReady = new boolean[SOUND_RES.length];
            for (int i = 0; i < SOUND_RES.length; i++) {
                this.mSoundIDs[i] = this.mSoundPool.load(this.mContext, SOUND_RES[i], 1);
                this.mSoundIDReady[i] = false;
            }
        }

        @Override
        public synchronized void release() {
            if (this.mSoundPool != null) {
                this.mSoundPool.release();
                this.mSoundPool = null;
            }
        }

        @Override
        public synchronized void play(int action) {
            if (action >= 0) {
                if (action >= this.mSoundRes.length) {
                    Log.e(TAG, "Resource ID not found for action:" + action + " in play().");
                } else {
                    int index = this.mSoundRes[action];
                    if (this.mSoundIDs[index] == 0) {
                        this.mSoundIDs[index] = this.mSoundPool.load(this.mContext, SOUND_RES[index], 1);
                        this.mSoundIDToPlay = this.mSoundIDs[index];
                    } else if (!this.mSoundIDReady[index]) {
                        this.mSoundIDToPlay = this.mSoundIDs[index];
                    } else {
                        this.mSoundPool.play(this.mSoundIDs[index], 1.0f, 1.0f, 0, 0, 1.0f);
                    }
                }
            }
        }

        @Override
        public void onLoadComplete(SoundPool pool, int soundID, int status) {
            if (status != 0) {
                Log.e(TAG, "loading sound tracks failed (status=" + status + ")");
                for (int i = 0; i < this.mSoundIDs.length; i++) {
                    if (this.mSoundIDs[i] == soundID) {
                        this.mSoundIDs[i] = 0;
                        return;
                    }
                }
                return;
            }
            int i2 = 0;
            while (true) {
                if (i2 >= this.mSoundIDs.length) {
                    break;
                }
                if (this.mSoundIDs[i2] != soundID) {
                    i2++;
                } else {
                    this.mSoundIDReady[i2] = true;
                    break;
                }
            }
            if (soundID == this.mSoundIDToPlay) {
                this.mSoundIDToPlay = 0;
                this.mSoundPool.play(soundID, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }
}
