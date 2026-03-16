package android.media;

import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

class AudioPortEventHandler {
    private static final int AUDIOPORT_EVENT_NEW_LISTENER = 4;
    private static final int AUDIOPORT_EVENT_PATCH_LIST_UPDATED = 2;
    private static final int AUDIOPORT_EVENT_PORT_LIST_UPDATED = 1;
    private static final int AUDIOPORT_EVENT_SERVICE_DIED = 3;
    private static final String TAG = "AudioPortEventHandler";
    private Handler mHandler;
    private final ArrayList<AudioManager.OnAudioPortUpdateListener> mListeners = new ArrayList<>();

    private native void native_finalize();

    private native void native_setup(Object obj);

    AudioPortEventHandler() {
    }

    void init() {
        synchronized (this) {
            if (this.mHandler == null) {
                Looper looper = Looper.getMainLooper();
                if (looper != null) {
                    this.mHandler = new Handler(looper) {
                        @Override
                        public void handleMessage(Message msg) {
                            ArrayList<AudioManager.OnAudioPortUpdateListener> listeners;
                            synchronized (this) {
                                if (msg.what != 4) {
                                    listeners = AudioPortEventHandler.this.mListeners;
                                } else {
                                    listeners = new ArrayList<>();
                                    if (AudioPortEventHandler.this.mListeners.contains(msg.obj)) {
                                        listeners.add((AudioManager.OnAudioPortUpdateListener) msg.obj);
                                    }
                                }
                            }
                            if (!listeners.isEmpty()) {
                                if (msg.what == 1 || msg.what == 2 || msg.what == 3) {
                                    AudioManager.resetAudioPortGeneration();
                                }
                                ArrayList<AudioPort> ports = new ArrayList<>();
                                ArrayList<AudioPatch> patches = new ArrayList<>();
                                if (msg.what != 3) {
                                    int status = AudioManager.updateAudioPortCache(ports, patches);
                                    if (status != 0) {
                                        return;
                                    }
                                }
                                switch (msg.what) {
                                    case 1:
                                    case 4:
                                        AudioPort[] portList = (AudioPort[]) ports.toArray(new AudioPort[0]);
                                        for (int i = 0; i < listeners.size(); i++) {
                                            listeners.get(i).onAudioPortListUpdate(portList);
                                        }
                                        if (msg.what == 1) {
                                            return;
                                        }
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        for (int i2 = 0; i2 < listeners.size(); i2++) {
                                            listeners.get(i2).onServiceDied();
                                        }
                                        return;
                                    default:
                                        return;
                                }
                                AudioPatch[] patchList = (AudioPatch[]) patches.toArray(new AudioPatch[0]);
                                for (int i3 = 0; i3 < listeners.size(); i3++) {
                                    listeners.get(i3).onAudioPatchListUpdate(patchList);
                                }
                            }
                        }
                    };
                    native_setup(new WeakReference(this));
                } else {
                    this.mHandler = null;
                }
            }
        }
    }

    protected void finalize() {
        native_finalize();
    }

    void registerListener(AudioManager.OnAudioPortUpdateListener l) {
        synchronized (this) {
            this.mListeners.add(l);
        }
        if (this.mHandler != null) {
            Message m = this.mHandler.obtainMessage(4, 0, 0, l);
            this.mHandler.sendMessage(m);
        }
    }

    void unregisterListener(AudioManager.OnAudioPortUpdateListener l) {
        synchronized (this) {
            this.mListeners.remove(l);
        }
    }

    Handler handler() {
        return this.mHandler;
    }

    private static void postEventFromNative(Object module_ref, int what, int arg1, int arg2, Object obj) {
        Handler handler;
        AudioPortEventHandler eventHandler = (AudioPortEventHandler) ((WeakReference) module_ref).get();
        if (eventHandler != null && eventHandler != null && (handler = eventHandler.handler()) != null) {
            Message m = handler.obtainMessage(what, arg1, arg2, obj);
            handler.sendMessage(m);
        }
    }
}
