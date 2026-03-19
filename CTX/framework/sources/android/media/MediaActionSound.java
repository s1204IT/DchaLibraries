package android.media;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

public class MediaActionSound {
    public static final int FOCUS_COMPLETE = 1;
    private static final int NUM_MEDIA_SOUND_STREAMS = 1;
    public static final int SHUTTER_CLICK = 0;
    private static final String[] SOUND_DIRS = {"/product/media/audio/ui/", "/system/media/audio/ui/"};
    private static final String[] SOUND_FILES = {"camera_click.ogg", "camera_focus.ogg", "VideoRecord.ogg", "VideoStop.ogg"};
    public static final int START_VIDEO_RECORDING = 2;
    private static final int STATE_LOADED = 3;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADING_PLAY_REQUESTED = 2;
    private static final int STATE_NOT_LOADED = 0;
    public static final int STOP_VIDEO_RECORDING = 3;
    private static final String TAG = "MediaActionSound";
    private SoundPool.OnLoadCompleteListener mLoadCompleteListener = new SoundPool.OnLoadCompleteListener() {
        @Override
        public void onLoadComplete(SoundPool soundPool, int i, int i2) {
            int i3 = 0;
            for (SoundState soundState : MediaActionSound.this.mSounds) {
                if (soundState.id == i) {
                    synchronized (soundState) {
                        try {
                            if (i2 != 0) {
                                soundState.state = 0;
                                soundState.id = 0;
                                Log.e(MediaActionSound.TAG, "OnLoadCompleteListener() error: " + i2 + " loading sound: " + soundState.name);
                                return;
                            }
                            switch (soundState.state) {
                                case 1:
                                    soundState.state = 3;
                                    break;
                                case 2:
                                    i3 = soundState.id;
                                    soundState.state = 3;
                                    break;
                                default:
                                    Log.e(MediaActionSound.TAG, "OnLoadCompleteListener() called in wrong state: " + soundState.state + " for sound: " + soundState.name);
                                    break;
                            }
                            int i4 = i3;
                            if (i4 != 0) {
                                soundPool.play(i4, 1.0f, 1.0f, 0, 0, 1.0f);
                                return;
                            }
                            return;
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                }
            }
        }
    };
    private SoundPool mSoundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setFlags(1).setContentType(4).build()).build();
    private SoundState[] mSounds;

    private class SoundState {
        public final int name;
        public int id = 0;
        public int state = 0;

        public SoundState(int i) {
            this.name = i;
        }
    }

    public MediaActionSound() {
        this.mSoundPool.setOnLoadCompleteListener(this.mLoadCompleteListener);
        this.mSounds = new SoundState[SOUND_FILES.length];
        for (int i = 0; i < this.mSounds.length; i++) {
            this.mSounds[i] = new SoundState(i);
        }
    }

    private int loadSound(SoundState soundState) {
        String str = SOUND_FILES[soundState.name];
        for (String str2 : SOUND_DIRS) {
            int iLoad = this.mSoundPool.load(str2 + str, 1);
            if (iLoad > 0) {
                soundState.state = 1;
                soundState.id = iLoad;
                return iLoad;
            }
        }
        return 0;
    }

    public void load(int i) {
        if (i < 0 || i >= SOUND_FILES.length) {
            throw new RuntimeException("Unknown sound requested: " + i);
        }
        SoundState soundState = this.mSounds[i];
        synchronized (soundState) {
            if (soundState.state == 0) {
                if (loadSound(soundState) <= 0) {
                    Log.e(TAG, "load() error loading sound: " + i);
                }
            } else {
                Log.e(TAG, "load() called in wrong state: " + soundState + " for sound: " + i);
            }
        }
    }

    public void play(int i) {
        if (i < 0 || i >= SOUND_FILES.length) {
            throw new RuntimeException("Unknown sound requested: " + i);
        }
        SoundState soundState = this.mSounds[i];
        synchronized (soundState) {
            int i2 = soundState.state;
            if (i2 != 3) {
                switch (i2) {
                    case 0:
                        loadSound(soundState);
                        if (loadSound(soundState) <= 0) {
                            Log.e(TAG, "play() error loading sound: " + i);
                            break;
                        }
                    case 1:
                        soundState.state = 2;
                        break;
                    default:
                        Log.e(TAG, "play() called in wrong state: " + soundState.state + " for sound: " + i);
                        break;
                }
            } else {
                this.mSoundPool.play(soundState.id, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    public void release() {
        if (this.mSoundPool != null) {
            for (SoundState soundState : this.mSounds) {
                synchronized (soundState) {
                    soundState.state = 0;
                    soundState.id = 0;
                }
            }
            this.mSoundPool.release();
            this.mSoundPool = null;
        }
    }
}
