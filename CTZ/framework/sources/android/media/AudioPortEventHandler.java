package android.media;

import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

class AudioPortEventHandler {
    private static final int AUDIOPORT_EVENT_NEW_LISTENER = 4;
    private static final int AUDIOPORT_EVENT_PATCH_LIST_UPDATED = 2;
    private static final int AUDIOPORT_EVENT_PORT_LIST_UPDATED = 1;
    private static final int AUDIOPORT_EVENT_SERVICE_DIED = 3;
    private static final boolean DEBUG = !"user".equals(Build.TYPE);
    private static final long RESCHEDULE_MESSAGE_DELAY_MS = 100;
    private static final String TAG = "AudioPortEventHandler";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private long mJniCallback;
    private final ArrayList<AudioManager.OnAudioPortUpdateListener> mListeners = new ArrayList<>();

    private native void native_finalize();

    private native void native_setup(Object obj);

    AudioPortEventHandler() {
    }

    void init() {
        synchronized (this) {
            if (this.mHandler != null) {
                return;
            }
            this.mHandlerThread = new HandlerThread(TAG);
            this.mHandlerThread.start();
            if (this.mHandlerThread.getLooper() != null) {
                this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message message) {
                        ArrayList arrayList;
                        synchronized (this) {
                            if (message.what != 4) {
                                arrayList = AudioPortEventHandler.this.mListeners;
                            } else {
                                arrayList = new ArrayList();
                                if (AudioPortEventHandler.this.mListeners.contains(message.obj)) {
                                    arrayList.add((AudioManager.OnAudioPortUpdateListener) message.obj);
                                }
                            }
                        }
                        if (message.what == 1 || message.what == 2 || message.what == 3) {
                            AudioManager.resetAudioPortGeneration();
                        }
                        if (arrayList.isEmpty()) {
                            return;
                        }
                        ArrayList arrayList2 = new ArrayList();
                        ArrayList arrayList3 = new ArrayList();
                        if (message.what != 3 && AudioManager.updateAudioPortCache(arrayList2, arrayList3, null) != 0) {
                            sendMessageDelayed(obtainMessage(message.what, message.obj), AudioPortEventHandler.RESCHEDULE_MESSAGE_DELAY_MS);
                            return;
                        }
                        int i = 0;
                        switch (message.what) {
                            case 1:
                            case 4:
                                AudioPort[] audioPortArr = (AudioPort[]) arrayList2.toArray(new AudioPort[0]);
                                for (int i2 = 0; i2 < arrayList.size(); i2++) {
                                    ((AudioManager.OnAudioPortUpdateListener) arrayList.get(i2)).onAudioPortListUpdate(audioPortArr);
                                }
                                if (message.what == 1) {
                                    if (AudioPortEventHandler.DEBUG) {
                                        Log.d(AudioPortEventHandler.TAG, "AUDIOPORT_EVENT_PORT_LIST_UPDATED");
                                        return;
                                    }
                                    return;
                                } else {
                                    if (AudioPortEventHandler.DEBUG) {
                                        Log.d(AudioPortEventHandler.TAG, "AUDIOPORT_EVENT_NEW_LISTENER");
                                    }
                                    AudioPatch[] audioPatchArr = (AudioPatch[]) arrayList3.toArray(new AudioPatch[0]);
                                    while (i < arrayList.size()) {
                                        ((AudioManager.OnAudioPortUpdateListener) arrayList.get(i)).onAudioPatchListUpdate(audioPatchArr);
                                        i++;
                                    }
                                    return;
                                }
                            case 2:
                                AudioPatch[] audioPatchArr2 = (AudioPatch[]) arrayList3.toArray(new AudioPatch[0]);
                                while (i < arrayList.size()) {
                                }
                                return;
                            case 3:
                                while (i < arrayList.size()) {
                                    ((AudioManager.OnAudioPortUpdateListener) arrayList.get(i)).onServiceDied();
                                    i++;
                                }
                                if (AudioPortEventHandler.DEBUG) {
                                    Log.d(AudioPortEventHandler.TAG, "AUDIOPORT_EVENT_SERVICE_DIED");
                                    return;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                };
                native_setup(new WeakReference(this));
            } else {
                this.mHandler = null;
            }
        }
    }

    protected void finalize() {
        native_finalize();
        if (this.mHandlerThread.isAlive()) {
            this.mHandlerThread.quit();
        }
    }

    void registerListener(AudioManager.OnAudioPortUpdateListener onAudioPortUpdateListener) {
        synchronized (this) {
            this.mListeners.add(onAudioPortUpdateListener);
        }
        if (this.mHandler != null) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(4, 0, 0, onAudioPortUpdateListener));
        }
    }

    void unregisterListener(AudioManager.OnAudioPortUpdateListener onAudioPortUpdateListener) {
        synchronized (this) {
            this.mListeners.remove(onAudioPortUpdateListener);
        }
    }

    Handler handler() {
        return this.mHandler;
    }

    private static void postEventFromNative(Object obj, int i, int i2, int i3, Object obj2) {
        Handler handler;
        AudioPortEventHandler audioPortEventHandler = (AudioPortEventHandler) ((WeakReference) obj).get();
        if (audioPortEventHandler != null && audioPortEventHandler != null && (handler = audioPortEventHandler.handler()) != null) {
            Message messageObtainMessage = handler.obtainMessage(i, i2, i3, obj2);
            if (i != 4) {
                handler.removeMessages(i);
            }
            handler.sendMessage(messageObtainMessage);
        }
    }
}
