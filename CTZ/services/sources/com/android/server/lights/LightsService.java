package com.android.server.lights;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.SystemService;

public class LightsService extends SystemService {
    static final boolean DEBUG = false;
    static final String TAG = "LightsService";
    private Handler mH;
    final LightImpl[] mLights;
    private final LightsManager mService;

    static native void setLight_native(int i, int i2, int i3, int i4, int i5, int i6);

    private final class LightImpl extends Light {
        private int mBrightnessMode;
        private int mColor;
        private boolean mFlashing;
        private int mId;
        private boolean mInitialized;
        private int mLastBrightnessMode;
        private int mLastColor;
        private int mMode;
        private int mOffMS;
        private int mOnMS;
        private boolean mUseLowPersistenceForVR;
        private boolean mVrModeEnabled;

        private LightImpl(int i) {
            this.mId = i;
        }

        @Override
        public void setBrightness(int i) {
            setBrightness(i, 0);
        }

        @Override
        public void setBrightness(int i, int i2) {
            synchronized (this) {
                try {
                    if (i2 == 2) {
                        Slog.w(LightsService.TAG, "setBrightness with LOW_PERSISTENCE unexpected #" + this.mId + ": brightness=0x" + Integer.toHexString(i));
                        return;
                    }
                    int i3 = i & 255;
                    setLightLocked((-16777216) | (i3 << 16) | (i3 << 8) | i3, 0, 0, 0, i2);
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        @Override
        public void setColor(int i) {
            synchronized (this) {
                setLightLocked(i, 0, 0, 0, 0);
            }
        }

        @Override
        public void setFlashing(int i, int i2, int i3, int i4) {
            synchronized (this) {
                setLightLocked(i, i2, i3, i4, 0);
            }
        }

        @Override
        public void pulse() {
            pulse(16777215, 7);
        }

        @Override
        public void pulse(int i, int i2) {
            synchronized (this) {
                if (this.mColor == 0 && !this.mFlashing) {
                    setLightLocked(i, 2, i2, 1000, 0);
                    this.mColor = 0;
                    LightsService.this.mH.sendMessageDelayed(Message.obtain(LightsService.this.mH, 1, this), i2);
                }
            }
        }

        @Override
        public void turnOff() {
            synchronized (this) {
                setLightLocked(0, 0, 0, 0, 0);
            }
        }

        @Override
        public void setVrMode(boolean z) {
            synchronized (this) {
                if (this.mVrModeEnabled != z) {
                    this.mVrModeEnabled = z;
                    this.mUseLowPersistenceForVR = LightsService.this.getVrDisplayMode() == 0;
                    if (shouldBeInLowPersistenceMode()) {
                        this.mLastBrightnessMode = this.mBrightnessMode;
                    }
                }
            }
        }

        private void stopFlashing() {
            synchronized (this) {
                setLightLocked(this.mColor, 0, 0, 0, 0);
            }
        }

        private void setLightLocked(int i, int i2, int i3, int i4, int i5) {
            int i6;
            if (!shouldBeInLowPersistenceMode()) {
                if (i5 == 2) {
                    i5 = this.mLastBrightnessMode;
                }
                i6 = i5;
            } else {
                i6 = 2;
            }
            if (!this.mInitialized || i != this.mColor || i2 != this.mMode || i3 != this.mOnMS || i4 != this.mOffMS || this.mBrightnessMode != i6) {
                this.mInitialized = true;
                this.mLastColor = this.mColor;
                this.mColor = i;
                this.mMode = i2;
                this.mOnMS = i3;
                this.mOffMS = i4;
                this.mBrightnessMode = i6;
                Trace.traceBegin(131072L, "setLight(" + this.mId + ", 0x" + Integer.toHexString(i) + ")");
                try {
                    LightsService.setLight_native(this.mId, i, i2, i3, i4, i6);
                } finally {
                    Trace.traceEnd(131072L);
                }
            }
        }

        private boolean shouldBeInLowPersistenceMode() {
            return this.mVrModeEnabled && this.mUseLowPersistenceForVR;
        }
    }

    public LightsService(Context context) {
        super(context);
        this.mLights = new LightImpl[8];
        this.mService = new LightsManager() {
            @Override
            public Light getLight(int i) {
                if (i >= 0 && i < 8) {
                    return LightsService.this.mLights[i];
                }
                return null;
            }
        };
        this.mH = new Handler() {
            @Override
            public void handleMessage(Message message) {
                ((LightImpl) message.obj).stopFlashing();
            }
        };
        for (int i = 0; i < 8; i++) {
            this.mLights[i] = new LightImpl(i);
        }
    }

    @Override
    public void onStart() {
        publishLocalService(LightsManager.class, this.mService);
    }

    @Override
    public void onBootPhase(int i) {
    }

    private int getVrDisplayMode() {
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "vr_display_mode", 0, ActivityManager.getCurrentUser());
    }
}
