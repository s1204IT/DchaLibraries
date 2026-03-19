package com.android.server.lights;

import android.app.ActivityManager;
import android.content.Context;
import android.net.dhcp.DhcpPacket;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Trace;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.usb.UsbAudioDevice;
import com.android.server.vr.VrManagerService;

public class LightsService extends SystemService {
    static final boolean DEBUG = false;
    static final String TAG = "LightsService";
    private Handler mH;
    final LightImpl[] mLights;
    private long mNativePointer;
    private final LightsManager mService;
    private boolean mVrModeEnabled;
    private final IVrStateCallbacks mVrStateCallbacks;

    private static native void finalize_native(long j);

    private static native long init_native();

    static native void setLight_native(long j, int i, int i2, int i3, int i4, int i5, int i6);

    private final class LightImpl extends Light {
        private int mBrightnessMode;
        private int mColor;
        private boolean mFlashing;
        private int mId;
        private int mLastBrightnessMode;
        private int mLastColor;
        private boolean mLocked;
        private int mMode;
        private int mOffMS;
        private int mOnMS;

        LightImpl(LightsService this$0, int id, LightImpl lightImpl) {
            this(id);
        }

        private LightImpl(int id) {
            this.mId = id;
        }

        @Override
        public void setBrightness(int brightness) {
            setBrightness(brightness, 0);
        }

        @Override
        public void setBrightness(int brightness, int brightnessMode) {
            synchronized (this) {
                int color = brightness & DhcpPacket.MAX_OPTION_LEN;
                setLightLocked(color | (color << 16) | UsbAudioDevice.kAudioDeviceMetaMask | (color << 8), 0, 0, 0, brightnessMode);
            }
        }

        @Override
        public void setColor(int color) {
            synchronized (this) {
                setLightLocked(color, 0, 0, 0, 0);
            }
        }

        @Override
        public void setFlashing(int color, int mode, int onMS, int offMS) {
            synchronized (this) {
                setLightLocked(color, mode, onMS, offMS, 0);
            }
        }

        @Override
        public void pulse() {
            pulse(UsbAudioDevice.kAudioDeviceClassMask, 7);
        }

        @Override
        public void pulse(int color, int onMS) {
            synchronized (this) {
                if (this.mBrightnessMode == 2) {
                    return;
                }
                if (this.mColor == 0 && !this.mFlashing) {
                    setLightLocked(color, 2, onMS, 1000, 0);
                    this.mColor = 0;
                    LightsService.this.mH.sendMessageDelayed(Message.obtain(LightsService.this.mH, 1, this), onMS);
                }
            }
        }

        @Override
        public void turnOff() {
            synchronized (this) {
                setLightLocked(0, 0, 0, 0, 0);
            }
        }

        void enableLowPersistence() {
            synchronized (this) {
                setLightLocked(0, 0, 0, 0, 2);
                this.mLocked = true;
            }
        }

        void disableLowPersistence() {
            synchronized (this) {
                this.mLocked = false;
                setLightLocked(this.mLastColor, 0, 0, 0, this.mLastBrightnessMode);
            }
        }

        private void stopFlashing() {
            synchronized (this) {
                setLightLocked(this.mColor, 0, 0, 0, 0);
            }
        }

        private void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (this.mLocked) {
                return;
            }
            if (color == this.mColor && mode == this.mMode && onMS == this.mOnMS && offMS == this.mOffMS && this.mBrightnessMode == brightnessMode) {
                return;
            }
            this.mLastColor = this.mColor;
            this.mColor = color;
            this.mMode = mode;
            this.mOnMS = onMS;
            this.mOffMS = offMS;
            this.mLastBrightnessMode = this.mBrightnessMode;
            this.mBrightnessMode = brightnessMode;
            Trace.traceBegin(524288L, "setLight(" + this.mId + ", 0x" + Integer.toHexString(color) + ")");
            try {
                LightsService.setLight_native(LightsService.this.mNativePointer, this.mId, color, mode, onMS, offMS, brightnessMode);
            } finally {
                Trace.traceEnd(524288L);
            }
        }
    }

    public LightsService(Context context) {
        super(context);
        this.mLights = new LightImpl[8];
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() {
            public void onVrStateChanged(boolean enabled) throws RemoteException {
                LightImpl l = LightsService.this.mLights[0];
                int vrDisplayMode = LightsService.this.getVrDisplayMode();
                if (enabled && vrDisplayMode == 0) {
                    if (LightsService.this.mVrModeEnabled) {
                        return;
                    }
                    l.enableLowPersistence();
                    LightsService.this.mVrModeEnabled = true;
                    return;
                }
                if (!LightsService.this.mVrModeEnabled) {
                    return;
                }
                l.disableLowPersistence();
                LightsService.this.mVrModeEnabled = false;
            }
        };
        this.mService = new LightsManager() {
            @Override
            public Light getLight(int id) {
                if (id < 8) {
                    return LightsService.this.mLights[id];
                }
                return null;
            }
        };
        this.mH = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                LightImpl light = (LightImpl) msg.obj;
                light.stopFlashing();
            }
        };
        this.mNativePointer = init_native();
        for (int i = 0; i < 8; i++) {
            this.mLights[i] = new LightImpl(this, i, null);
        }
    }

    @Override
    public void onStart() {
        publishLocalService(LightsManager.class, this.mService);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        IVrManager vrManager = getBinderService(VrManagerService.VR_MANAGER_BINDER_SERVICE);
        try {
            vrManager.registerListener(this.mVrStateCallbacks);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register VR mode state listener: " + e);
        }
    }

    private int getVrDisplayMode() {
        int currentUser = ActivityManager.getCurrentUser();
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "vr_display_mode", 0, currentUser);
    }

    protected void finalize() throws Throwable {
        finalize_native(this.mNativePointer);
        super.finalize();
    }
}
