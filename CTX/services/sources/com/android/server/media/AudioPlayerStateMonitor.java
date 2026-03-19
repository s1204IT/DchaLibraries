package com.android.server.media;

import android.content.Context;
import android.media.AudioPlaybackConfiguration;
import android.media.IAudioService;
import android.media.IPlaybackConfigDispatcher;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AudioPlayerStateMonitor extends IPlaybackConfigDispatcher.Stub {
    private static boolean DEBUG = MediaSessionService.DEBUG;
    private static String TAG = "AudioPlayerStateMonitor";
    private static AudioPlayerStateMonitor sInstance = new AudioPlayerStateMonitor();

    @GuardedBy("mLock")
    private boolean mRegisteredToAudioService;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final Map<OnAudioPlayerActiveStateChangedListener, MessageHandler> mListenerMap = new ArrayMap();

    @GuardedBy("mLock")
    private final Set<Integer> mActiveAudioUids = new ArraySet();

    @GuardedBy("mLock")
    private ArrayMap<Integer, AudioPlaybackConfiguration> mPrevActiveAudioPlaybackConfigs = new ArrayMap<>();

    @GuardedBy("mLock")
    private final IntArray mSortedAudioPlaybackClientUids = new IntArray();

    interface OnAudioPlayerActiveStateChangedListener {
        void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z);
    }

    private static final class MessageHandler extends Handler {
        private static final int MSG_AUDIO_PLAYER_ACTIVE_STATE_CHANGED = 1;
        private final OnAudioPlayerActiveStateChangedListener mListener;

        MessageHandler(Looper looper, OnAudioPlayerActiveStateChangedListener onAudioPlayerActiveStateChangedListener) {
            super(looper);
            this.mListener = onAudioPlayerActiveStateChangedListener;
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                this.mListener.onAudioPlayerActiveStateChanged((AudioPlaybackConfiguration) message.obj, message.arg1 != 0);
            }
        }

        void sendAudioPlayerActiveStateChangedMessage(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
            obtainMessage(1, z ? 1 : 0, 0, audioPlaybackConfiguration).sendToTarget();
        }
    }

    static AudioPlayerStateMonitor getInstance() {
        return sInstance;
    }

    private AudioPlayerStateMonitor() {
    }

    public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> list, boolean z) {
        if (z) {
            Binder.flushPendingCommands();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
        } finally {
        }
        synchronized (this.mLock) {
            this.mActiveAudioUids.clear();
            ArrayMap<Integer, AudioPlaybackConfiguration> arrayMap = new ArrayMap<>();
            for (AudioPlaybackConfiguration audioPlaybackConfiguration : list) {
                if (audioPlaybackConfiguration.isActive()) {
                    this.mActiveAudioUids.add(Integer.valueOf(audioPlaybackConfiguration.getClientUid()));
                    arrayMap.put(Integer.valueOf(audioPlaybackConfiguration.getPlayerInterfaceId()), audioPlaybackConfiguration);
                }
            }
            for (int i = 0; i < arrayMap.size(); i++) {
                AudioPlaybackConfiguration audioPlaybackConfigurationValueAt = arrayMap.valueAt(i);
                int clientUid = audioPlaybackConfigurationValueAt.getClientUid();
                if (!this.mPrevActiveAudioPlaybackConfigs.containsKey(Integer.valueOf(audioPlaybackConfigurationValueAt.getPlayerInterfaceId()))) {
                    if (DEBUG) {
                        Log.d(TAG, "Found a new active media playback. " + AudioPlaybackConfiguration.toLogFriendlyString(audioPlaybackConfigurationValueAt));
                    }
                    int iIndexOf = this.mSortedAudioPlaybackClientUids.indexOf(clientUid);
                    if (iIndexOf != 0) {
                        if (iIndexOf > 0) {
                            this.mSortedAudioPlaybackClientUids.remove(iIndexOf);
                        }
                        this.mSortedAudioPlaybackClientUids.add(0, clientUid);
                    }
                }
            }
            Iterator<AudioPlaybackConfiguration> it = list.iterator();
            while (true) {
                boolean z2 = true;
                if (!it.hasNext()) {
                    break;
                }
                AudioPlaybackConfiguration next = it.next();
                if (this.mPrevActiveAudioPlaybackConfigs.remove(Integer.valueOf(next.getPlayerInterfaceId())) == null) {
                    z2 = false;
                }
                if (z2 != next.isActive()) {
                    sendAudioPlayerActiveStateChangedMessageLocked(next, false);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
            Iterator<AudioPlaybackConfiguration> it2 = this.mPrevActiveAudioPlaybackConfigs.values().iterator();
            while (it2.hasNext()) {
                sendAudioPlayerActiveStateChangedMessageLocked(it2.next(), true);
            }
            this.mPrevActiveAudioPlaybackConfigs = arrayMap;
        }
    }

    public void registerListener(OnAudioPlayerActiveStateChangedListener onAudioPlayerActiveStateChangedListener, Handler handler) {
        synchronized (this.mLock) {
            this.mListenerMap.put(onAudioPlayerActiveStateChangedListener, new MessageHandler(handler == null ? Looper.myLooper() : handler.getLooper(), onAudioPlayerActiveStateChangedListener));
        }
    }

    public void unregisterListener(OnAudioPlayerActiveStateChangedListener onAudioPlayerActiveStateChangedListener) {
        synchronized (this.mLock) {
            this.mListenerMap.remove(onAudioPlayerActiveStateChangedListener);
        }
    }

    public IntArray getSortedAudioPlaybackClientUids() {
        IntArray intArray = new IntArray();
        synchronized (this.mLock) {
            intArray.addAll(this.mSortedAudioPlaybackClientUids);
        }
        return intArray;
    }

    public boolean isPlaybackActive(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = this.mActiveAudioUids.contains(Integer.valueOf(i));
        }
        return zContains;
    }

    public void cleanUpAudioPlaybackUids(int i) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserId(i);
            for (int size = this.mSortedAudioPlaybackClientUids.size() - 1; size >= 0 && this.mSortedAudioPlaybackClientUids.get(size) != i; size--) {
                int i2 = this.mSortedAudioPlaybackClientUids.get(size);
                if (userId == UserHandle.getUserId(i2) && !isPlaybackActive(i2)) {
                    this.mSortedAudioPlaybackClientUids.remove(size);
                }
            }
        }
    }

    public void dump(Context context, PrintWriter printWriter, String str) {
        synchronized (this.mLock) {
            printWriter.println(str + "Audio playback (lastly played comes first)");
            String str2 = str + "  ";
            for (int i = 0; i < this.mSortedAudioPlaybackClientUids.size(); i++) {
                int i2 = this.mSortedAudioPlaybackClientUids.get(i);
                printWriter.print(str2 + "uid=" + i2 + " packages=");
                String[] packagesForUid = context.getPackageManager().getPackagesForUid(i2);
                if (packagesForUid != null && packagesForUid.length > 0) {
                    for (String str3 : packagesForUid) {
                        printWriter.print(str3 + " ");
                    }
                }
                printWriter.println();
            }
        }
    }

    public void registerSelfIntoAudioServiceIfNeeded(IAudioService iAudioService) {
        synchronized (this.mLock) {
            try {
            } catch (RemoteException e) {
                Log.wtf(TAG, "Failed to register playback callback", e);
                this.mRegisteredToAudioService = false;
            }
            if (!this.mRegisteredToAudioService) {
                iAudioService.registerPlaybackCallback(this);
                this.mRegisteredToAudioService = true;
            }
        }
    }

    @GuardedBy("mLock")
    private void sendAudioPlayerActiveStateChangedMessageLocked(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
        Iterator<MessageHandler> it = this.mListenerMap.values().iterator();
        while (it.hasNext()) {
            it.next().sendAudioPlayerActiveStateChangedMessage(audioPlaybackConfiguration, z);
        }
    }
}
