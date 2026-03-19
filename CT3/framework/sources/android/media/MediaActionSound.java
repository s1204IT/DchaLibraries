package android.media;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

public class MediaActionSound {
    public static final int FOCUS_COMPLETE = 1;
    private static final int NUM_MEDIA_SOUND_STREAMS = 1;
    public static final int SHUTTER_CLICK = 0;
    private static final String[] SOUND_FILES = {"/system/media/audio/ui/camera_click.ogg", "/system/media/audio/ui/camera_focus.ogg", "/system/media/audio/ui/VideoRecord.ogg", "/system/media/audio/ui/VideoStop.ogg"};
    public static final int START_VIDEO_RECORDING = 2;
    private static final int STATE_LOADED = 3;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADING_PLAY_REQUESTED = 2;
    private static final int STATE_NOT_LOADED = 0;
    public static final int STOP_VIDEO_RECORDING = 3;
    private static final String TAG = "MediaActionSound";
    private SoundPool.OnLoadCompleteListener mLoadCompleteListener = new SoundPool.OnLoadCompleteListener() {
        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            for (SoundState sound : MediaActionSound.this.mSounds) {
                if (sound.id == sampleId) {
                    int playSoundId = 0;
                    synchronized (sound) {
                        if (status != 0) {
                            sound.state = 0;
                            sound.id = 0;
                            Log.e(MediaActionSound.TAG, "OnLoadCompleteListener() error: " + status + " loading sound: " + sound.name);
                            return;
                        }
                        switch (sound.state) {
                            case 1:
                                sound.state = 3;
                                break;
                            case 2:
                                playSoundId = sound.id;
                                sound.state = 3;
                                break;
                            default:
                                Log.e(MediaActionSound.TAG, "OnLoadCompleteListener() called in wrong state: " + sound.state + " for sound: " + sound.name);
                                break;
                        }
                        if (playSoundId != 0) {
                            soundPool.play(playSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
                            return;
                        }
                        return;
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

        public SoundState(int name) {
            this.name = name;
        }
    }

    public MediaActionSound() {
        this.mSoundPool.setOnLoadCompleteListener(this.mLoadCompleteListener);
        this.mSounds = new SoundState[SOUND_FILES.length];
        for (int i = 0; i < this.mSounds.length; i++) {
            this.mSounds[i] = new SoundState(i);
        }
    }

    private int loadSound(SoundState sound) {
        int id = this.mSoundPool.load(SOUND_FILES[sound.name], 1);
        if (id > 0) {
            sound.state = 1;
            sound.id = id;
        }
        return id;
    }

    public void load(int soundName) {
        if (soundName < 0 || soundName >= SOUND_FILES.length) {
            throw new RuntimeException("Unknown sound requested: " + soundName);
        }
        SoundState sound = this.mSounds[soundName];
        synchronized (sound) {
            switch (sound.state) {
                case 0:
                    if (loadSound(sound) <= 0) {
                        Log.e(TAG, "load() error loading sound: " + soundName);
                    }
                    break;
                default:
                    Log.e(TAG, "load() called in wrong state: " + sound + " for sound: " + soundName);
                    break;
            }
        }
    }

    public void play(int soundName) {
        if (soundName < 0 || soundName >= SOUND_FILES.length) {
            throw new RuntimeException("Unknown sound requested: " + soundName);
        }
        SoundState sound = this.mSounds[soundName];
        synchronized (sound) {
            switch (sound.state) {
                case 0:
                    loadSound(sound);
                    if (loadSound(sound) <= 0) {
                        Log.e(TAG, "play() error loading sound: " + soundName);
                        break;
                    }
                case 1:
                    sound.state = 2;
                    break;
                case 2:
                default:
                    Log.e(TAG, "play() called in wrong state: " + sound.state + " for sound: " + soundName);
                    break;
                case 3:
                    this.mSoundPool.play(sound.id, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
            }
        }
    }

    public void release() {
        if (this.mSoundPool == null) {
            return;
        }
        for (SoundState sound : this.mSounds) {
            synchronized (sound) {
                sound.state = 0;
                sound.id = 0;
            }
        }
        this.mSoundPool.release();
        this.mSoundPool = null;
    }
}
