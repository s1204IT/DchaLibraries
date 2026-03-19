package android.hardware.radio;

import android.hardware.radio.RadioManager;
import android.hardware.radio.RadioTuner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.ref.WeakReference;

public class RadioModule extends RadioTuner {
    static final int EVENT_AF_SWITCH = 6;
    static final int EVENT_ANTENNA = 2;
    static final int EVENT_CONFIG = 1;
    static final int EVENT_CONTROL = 100;
    static final int EVENT_EA = 7;
    static final int EVENT_HW_FAILURE = 0;
    static final int EVENT_METADATA = 4;
    static final int EVENT_SERVER_DIED = 101;
    static final int EVENT_TA = 5;
    static final int EVENT_TUNED = 3;
    private NativeEventHandlerDelegate mEventHandlerDelegate;
    private int mId;
    private long mNativeContext = 0;

    private native void native_finalize();

    private native void native_setup(Object obj, RadioManager.BandConfig bandConfig, boolean z);

    @Override
    public native int cancel();

    @Override
    public native void close();

    @Override
    public native int getConfiguration(RadioManager.BandConfig[] bandConfigArr);

    @Override
    public native boolean getMute();

    @Override
    public native int getProgramInformation(RadioManager.ProgramInfo[] programInfoArr);

    @Override
    public native boolean hasControl();

    @Override
    public native boolean isAntennaConnected();

    @Override
    public native int scan(int i, boolean z);

    @Override
    public native int setConfiguration(RadioManager.BandConfig bandConfig);

    @Override
    public native int setMute(boolean z);

    @Override
    public native int step(int i, boolean z);

    @Override
    public native int tune(int i, int i2);

    RadioModule(int moduleId, RadioManager.BandConfig config, boolean withAudio, RadioTuner.Callback callback, Handler handler) {
        this.mId = moduleId;
        this.mEventHandlerDelegate = new NativeEventHandlerDelegate(callback, handler);
        native_setup(new WeakReference(this), config, withAudio);
    }

    protected void finalize() {
        native_finalize();
    }

    boolean initCheck() {
        return this.mNativeContext != 0;
    }

    private class NativeEventHandlerDelegate {
        private final Handler mHandler;

        NativeEventHandlerDelegate(final RadioTuner.Callback callback, Handler handler) {
            Looper looper;
            if (handler != null) {
                looper = handler.getLooper();
            } else {
                looper = Looper.getMainLooper();
            }
            if (looper != null) {
                this.mHandler = new Handler(looper) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 0:
                                if (callback == null) {
                                    return;
                                }
                                callback.onError(0);
                                return;
                            case 1:
                                RadioManager.BandConfig config = (RadioManager.BandConfig) msg.obj;
                                switch (msg.arg1) {
                                    case 0:
                                        if (callback != null) {
                                            callback.onConfigurationChanged(config);
                                        }
                                        break;
                                    default:
                                        if (callback != null) {
                                            callback.onError(4);
                                        }
                                        break;
                                }
                                return;
                            case 2:
                                if (callback == null) {
                                    return;
                                }
                                callback.onAntennaState(msg.arg2 == 1);
                                return;
                            case 3:
                            case 6:
                                RadioManager.ProgramInfo info = (RadioManager.ProgramInfo) msg.obj;
                                switch (msg.arg1) {
                                    case -110:
                                        if (callback != null) {
                                            callback.onError(3);
                                        }
                                        break;
                                    case 0:
                                        if (callback != null) {
                                            callback.onProgramInfoChanged(info);
                                        }
                                        break;
                                    default:
                                        if (callback != null) {
                                            callback.onError(2);
                                        }
                                        break;
                                }
                                return;
                            case 4:
                                RadioMetadata metadata = (RadioMetadata) msg.obj;
                                if (callback == null) {
                                    return;
                                }
                                callback.onMetadataChanged(metadata);
                                return;
                            case 5:
                                if (callback == null) {
                                    return;
                                }
                                callback.onTrafficAnnouncement(msg.arg2 == 1);
                                return;
                            case 7:
                                if (callback != null) {
                                    callback.onEmergencyAnnouncement(msg.arg2 == 1);
                                }
                                break;
                            case 100:
                                break;
                            case 101:
                                if (callback == null) {
                                    return;
                                }
                                callback.onError(1);
                                return;
                            default:
                                return;
                        }
                        if (callback == null) {
                            return;
                        }
                        callback.onControlChanged(msg.arg2 == 1);
                    }
                };
            } else {
                this.mHandler = null;
            }
        }

        Handler handler() {
            return this.mHandler;
        }
    }

    private static void postEventFromNative(Object module_ref, int what, int arg1, int arg2, Object obj) {
        NativeEventHandlerDelegate delegate;
        Handler handler;
        RadioModule module = (RadioModule) ((WeakReference) module_ref).get();
        if (module == null || (delegate = module.mEventHandlerDelegate) == null || (handler = delegate.handler()) == null) {
            return;
        }
        Message m = handler.obtainMessage(what, arg1, arg2, obj);
        handler.sendMessage(m);
    }
}
