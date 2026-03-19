package android.support.v4.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.GuardedBy;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.util.ObjectsCompat;
import android.util.Log;

@VisibleForTesting(otherwise = 3)
@RestrictTo({RestrictTo.Scope.LIBRARY})
public class AudioFocusHandler {
    private static final boolean DEBUG = false;
    private static final String TAG = "AudioFocusHandler";
    private final AudioFocusHandlerImpl mImpl;

    interface AudioFocusHandlerImpl {
        void close();

        boolean onPauseRequested();

        boolean onPlayRequested();

        void onPlayerStateChanged(int i);

        void sendIntent(Intent intent);
    }

    AudioFocusHandler(Context context, MediaSession2 session) {
        this.mImpl = new AudioFocusHandlerImplBase(context, session);
    }

    public boolean onPlayRequested() {
        return this.mImpl.onPlayRequested();
    }

    public boolean onPauseRequested() {
        return this.mImpl.onPauseRequested();
    }

    public void onPlayerStateChanged(int playerState) {
        this.mImpl.onPlayerStateChanged(playerState);
    }

    public void close() {
        this.mImpl.close();
    }

    public void sendIntent(Intent intent) {
        this.mImpl.sendIntent(intent);
    }

    private static class AudioFocusHandlerImplBase implements AudioFocusHandlerImpl {
        private static final float VOLUME_DUCK_FACTOR = 0.2f;

        @GuardedBy("mLock")
        private AudioAttributesCompat mAudioAttributes;
        private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener;
        private final AudioManager mAudioManager;
        private final BroadcastReceiver mBecomingNoisyIntentReceiver;

        @GuardedBy("mLock")
        private boolean mHasAudioFocus;

        @GuardedBy("mLock")
        private boolean mHasRegisteredReceiver;
        private final IntentFilter mIntentFilter = new IntentFilter("android.media.AUDIO_BECOMING_NOISY");
        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private boolean mResumeWhenAudioFocusGain;
        private final MediaSession2 mSession;

        AudioFocusHandlerImplBase(Context context, MediaSession2 session) {
            this.mBecomingNoisyIntentReceiver = new NoisyIntentReceiver();
            this.mAudioFocusListener = new AudioFocusListener();
            this.mSession = session;
            this.mAudioManager = (AudioManager) context.getSystemService("audio");
        }

        private void updateAudioAttributesIfNeeded() {
            AudioAttributesCompat attributes;
            if (this.mSession.getVolumeProvider() != null) {
                attributes = null;
            } else {
                BaseMediaPlayer player = this.mSession.getPlayer();
                attributes = player == null ? null : player.getAudioAttributes();
            }
            synchronized (this.mLock) {
                if (ObjectsCompat.equals(attributes, this.mAudioAttributes)) {
                    return;
                }
                this.mAudioAttributes = attributes;
                if (this.mHasAudioFocus) {
                    this.mHasAudioFocus = requestAudioFocusLocked();
                    if (!this.mHasAudioFocus) {
                        Log.w(AudioFocusHandler.TAG, "Failed to regain audio focus.");
                    }
                }
            }
        }

        @Override
        public boolean onPlayRequested() {
            updateAudioAttributesIfNeeded();
            synchronized (this.mLock) {
                if (!requestAudioFocusLocked()) {
                    return false;
                }
                return true;
            }
        }

        @Override
        public boolean onPauseRequested() {
            synchronized (this.mLock) {
                this.mResumeWhenAudioFocusGain = false;
            }
            return true;
        }

        @Override
        public void onPlayerStateChanged(int playerState) {
            synchronized (this.mLock) {
                switch (playerState) {
                    case 0:
                        abandonAudioFocusLocked();
                        break;
                    case 1:
                        updateAudioAttributesIfNeeded();
                        unregisterReceiverLocked();
                        break;
                    case 2:
                        updateAudioAttributesIfNeeded();
                        registerReceiverLocked();
                        break;
                    case 3:
                        abandonAudioFocusLocked();
                        unregisterReceiverLocked();
                        break;
                }
            }
        }

        @Override
        public void close() {
            synchronized (this.mLock) {
                unregisterReceiverLocked();
                abandonAudioFocusLocked();
            }
        }

        @Override
        public void sendIntent(Intent intent) {
            this.mBecomingNoisyIntentReceiver.onReceive(this.mSession.getContext(), intent);
        }

        @GuardedBy("mLock")
        private boolean requestAudioFocusLocked() {
            int focusGain = convertAudioAttributesToFocusGainLocked();
            if (focusGain == 0) {
                return true;
            }
            int audioFocusRequestResult = this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, this.mAudioAttributes.getVolumeControlStream(), focusGain);
            if (audioFocusRequestResult == 1) {
                this.mHasAudioFocus = true;
            } else {
                Log.w(AudioFocusHandler.TAG, "requestAudioFocus(" + focusGain + ") failed (return=" + audioFocusRequestResult + ") playback wouldn't start.");
                this.mHasAudioFocus = false;
            }
            this.mResumeWhenAudioFocusGain = false;
            return this.mHasAudioFocus;
        }

        @GuardedBy("mLock")
        private void abandonAudioFocusLocked() {
            if (!this.mHasAudioFocus) {
                return;
            }
            this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
            this.mHasAudioFocus = false;
            this.mResumeWhenAudioFocusGain = false;
        }

        @GuardedBy("mLock")
        private void registerReceiverLocked() {
            if (this.mHasRegisteredReceiver) {
                return;
            }
            this.mSession.getContext().registerReceiver(this.mBecomingNoisyIntentReceiver, this.mIntentFilter);
            this.mHasRegisteredReceiver = true;
        }

        @GuardedBy("mLock")
        private void unregisterReceiverLocked() {
            if (!this.mHasRegisteredReceiver) {
                return;
            }
            this.mSession.getContext().unregisterReceiver(this.mBecomingNoisyIntentReceiver);
            this.mHasRegisteredReceiver = false;
        }

        @GuardedBy("mLock")
        private int convertAudioAttributesToFocusGainLocked() {
            AudioAttributesCompat audioAttributesCompat = this.mAudioAttributes;
            if (audioAttributesCompat == null) {
                return 0;
            }
            switch (audioAttributesCompat.getUsage()) {
            }
            return 0;
        }

        private class NoisyIntentReceiver extends BroadcastReceiver {
            private NoisyIntentReceiver() {
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                BaseMediaPlayer player;
                synchronized (AudioFocusHandlerImplBase.this.mLock) {
                    if (AudioFocusHandlerImplBase.this.mHasRegisteredReceiver) {
                        if ("android.media.AUDIO_BECOMING_NOISY".equals(intent.getAction())) {
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                if (AudioFocusHandlerImplBase.this.mAudioAttributes == null) {
                                    return;
                                }
                                int usage = AudioFocusHandlerImplBase.this.mAudioAttributes.getUsage();
                                if (usage == 1) {
                                    AudioFocusHandlerImplBase.this.mSession.pause();
                                } else if (usage == 14 && (player = AudioFocusHandlerImplBase.this.mSession.getPlayer()) != null) {
                                    player.setPlayerVolume(player.getPlayerVolume() * AudioFocusHandlerImplBase.VOLUME_DUCK_FACTOR);
                                }
                            }
                        }
                    }
                }
            }
        }

        private class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
            private float mPlayerDuckingVolume;
            private float mPlayerVolumeBeforeDucking;

            private AudioFocusListener() {
            }

            @Override
            public void onAudioFocusChange(int focusGain) {
                if (focusGain != 1) {
                    switch (focusGain) {
                        case FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR:
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                if (AudioFocusHandlerImplBase.this.mAudioAttributes != null) {
                                    if (AudioFocusHandlerImplBase.this.mAudioAttributes.getContentType() == 1) {
                                        AudioFocusHandlerImplBase.this.mSession.pause();
                                    } else {
                                        BaseMediaPlayer player = AudioFocusHandlerImplBase.this.mSession.getPlayer();
                                        if (player != null) {
                                            float currentVolume = player.getPlayerVolume();
                                            float duckingVolume = AudioFocusHandlerImplBase.VOLUME_DUCK_FACTOR * currentVolume;
                                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                                this.mPlayerVolumeBeforeDucking = currentVolume;
                                                this.mPlayerDuckingVolume = duckingVolume;
                                                break;
                                            }
                                            player.setPlayerVolume(duckingVolume);
                                        }
                                    }
                                    return;
                                }
                                return;
                            }
                        case -2:
                            AudioFocusHandlerImplBase.this.mSession.pause();
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                AudioFocusHandlerImplBase.this.mResumeWhenAudioFocusGain = true;
                                break;
                            }
                            return;
                        case -1:
                            AudioFocusHandlerImplBase.this.mSession.pause();
                            synchronized (AudioFocusHandlerImplBase.this.mLock) {
                                AudioFocusHandlerImplBase.this.mResumeWhenAudioFocusGain = false;
                                break;
                            }
                            return;
                        default:
                            return;
                    }
                }
                if (AudioFocusHandlerImplBase.this.mSession.getPlayerState() == 1) {
                    synchronized (AudioFocusHandlerImplBase.this.mLock) {
                        if (AudioFocusHandlerImplBase.this.mResumeWhenAudioFocusGain) {
                            AudioFocusHandlerImplBase.this.mSession.play();
                        }
                    }
                    return;
                }
                BaseMediaPlayer player2 = AudioFocusHandlerImplBase.this.mSession.getPlayer();
                if (player2 != null) {
                    float currentVolume2 = player2.getPlayerVolume();
                    synchronized (AudioFocusHandlerImplBase.this.mLock) {
                        if (currentVolume2 == this.mPlayerDuckingVolume) {
                            float volumeBeforeDucking = this.mPlayerVolumeBeforeDucking;
                            player2.setPlayerVolume(volumeBeforeDucking);
                        }
                    }
                }
            }
        }
    }
}
