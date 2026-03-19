package com.android.systemui.settings;

import android.R;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Log;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.display.BrightnessUtils;
import com.android.systemui.Dependency;
import com.android.systemui.settings.ToggleSlider;
import java.util.ArrayList;
import java.util.Iterator;

public class BrightnessController implements ToggleSlider.Listener {
    private volatile boolean mAutomatic;
    private final boolean mAutomaticAvailable;
    private final Handler mBackgroundHandler;
    private final BrightnessObserver mBrightnessObserver;
    private final Context mContext;
    private final ToggleSlider mControl;
    private boolean mControlValueInitialized;
    private final int mDefaultBacklight;
    private final int mDefaultBacklightForVr;
    private final DisplayManager mDisplayManager;
    private boolean mExternalChange;
    private final ImageView mIcon;
    private volatile boolean mIsVrModeEnabled;
    private boolean mListening;
    private final int mMaximumBacklight;
    private final int mMaximumBacklightForVr;
    private final int mMinimumBacklight;
    private final int mMinimumBacklightForVr;
    private ValueAnimator mSliderAnimator;
    private final CurrentUserTracker mUserTracker;
    private final IVrManager mVrManager;
    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final Runnable mStartListeningRunnable = new Runnable() {
        @Override
        public void run() {
            BrightnessController.this.mBrightnessObserver.startObserving();
            BrightnessController.this.mUserTracker.startTracking();
            BrightnessController.this.mUpdateModeRunnable.run();
            BrightnessController.this.mUpdateSliderRunnable.run();
            BrightnessController.this.mHandler.sendEmptyMessage(3);
        }
    };
    private final Runnable mStopListeningRunnable = new Runnable() {
        @Override
        public void run() {
            BrightnessController.this.mBrightnessObserver.stopObserving();
            BrightnessController.this.mUserTracker.stopTracking();
            BrightnessController.this.mHandler.sendEmptyMessage(4);
        }
    };
    private final Runnable mUpdateModeRunnable = new Runnable() {
        @Override
        public void run() {
            if (BrightnessController.this.mAutomaticAvailable) {
                int intForUser = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2);
                BrightnessController.this.mAutomatic = intForUser != 0;
                BrightnessController.this.mHandler.obtainMessage(0, Integer.valueOf(BrightnessController.this.mAutomatic ? 1 : 0)).sendToTarget();
                return;
            }
            BrightnessController.this.mHandler.obtainMessage(2, 0).sendToTarget();
            BrightnessController.this.mHandler.obtainMessage(0, 0).sendToTarget();
        }
    };
    private final Runnable mUpdateSliderRunnable = new Runnable() {
        @Override
        public void run() {
            int intForUser;
            boolean z = BrightnessController.this.mIsVrModeEnabled;
            if (z) {
                intForUser = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_for_vr", BrightnessController.this.mDefaultBacklightForVr, -2);
            } else {
                intForUser = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", BrightnessController.this.mDefaultBacklight, -2);
            }
            BrightnessController.this.mHandler.obtainMessage(1, intForUser, z ? 1 : 0).sendToTarget();
        }
    };
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() {
        public void onVrStateChanged(boolean z) {
            BrightnessController.this.mHandler.obtainMessage(5, z ? 1 : 0, 0).sendToTarget();
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            boolean z = true;
            BrightnessController.this.mExternalChange = true;
            try {
                switch (message.what) {
                    case 0:
                        BrightnessController brightnessController = BrightnessController.this;
                        if (message.arg1 == 0) {
                            z = false;
                        }
                        brightnessController.updateIcon(z);
                        break;
                    case 1:
                        BrightnessController brightnessController2 = BrightnessController.this;
                        int i = message.arg1;
                        if (message.arg2 == 0) {
                            z = false;
                        }
                        brightnessController2.updateSlider(i, z);
                        break;
                    case 2:
                        ToggleSlider toggleSlider = BrightnessController.this.mControl;
                        if (message.arg1 == 0) {
                            z = false;
                        }
                        toggleSlider.setChecked(z);
                        break;
                    case 3:
                        BrightnessController.this.mControl.setOnChangedListener(BrightnessController.this);
                        break;
                    case 4:
                        BrightnessController.this.mControl.setOnChangedListener(null);
                        break;
                    case 5:
                        BrightnessController brightnessController3 = BrightnessController.this;
                        if (message.arg1 == 0) {
                            z = false;
                        }
                        brightnessController3.updateVrMode(z);
                        break;
                    default:
                        super.handleMessage(message);
                        break;
                }
            } finally {
                BrightnessController.this.mExternalChange = false;
            }
        }
    };

    public interface BrightnessStateChangeCallback {
        void onBrightnessLevelChanged();
    }

    private class BrightnessObserver extends ContentObserver {
        private final Uri BRIGHTNESS_FOR_VR_URI;
        private final Uri BRIGHTNESS_MODE_URI;
        private final Uri BRIGHTNESS_URI;

        public BrightnessObserver(Handler handler) {
            super(handler);
            this.BRIGHTNESS_MODE_URI = Settings.System.getUriFor("screen_brightness_mode");
            this.BRIGHTNESS_URI = Settings.System.getUriFor("screen_brightness");
            this.BRIGHTNESS_FOR_VR_URI = Settings.System.getUriFor("screen_brightness_for_vr");
        }

        @Override
        public void onChange(boolean z) {
            onChange(z, null);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (z) {
                return;
            }
            if (this.BRIGHTNESS_MODE_URI.equals(uri)) {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            } else if (!this.BRIGHTNESS_URI.equals(uri) && !this.BRIGHTNESS_FOR_VR_URI.equals(uri)) {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            } else {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            }
            Iterator it = BrightnessController.this.mChangeCallbacks.iterator();
            while (it.hasNext()) {
                ((BrightnessStateChangeCallback) it.next()).onBrightnessLevelChanged();
            }
        }

        public void startObserving() {
            ContentResolver contentResolver = BrightnessController.this.mContext.getContentResolver();
            contentResolver.unregisterContentObserver(this);
            contentResolver.registerContentObserver(this.BRIGHTNESS_MODE_URI, false, this, -1);
            contentResolver.registerContentObserver(this.BRIGHTNESS_URI, false, this, -1);
            contentResolver.registerContentObserver(this.BRIGHTNESS_FOR_VR_URI, false, this, -1);
        }

        public void stopObserving() {
            BrightnessController.this.mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public BrightnessController(Context context, ImageView imageView, ToggleSlider toggleSlider) {
        this.mContext = context;
        this.mIcon = imageView;
        this.mControl = toggleSlider;
        this.mControl.setMax(1023);
        this.mBackgroundHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            @Override
            public void onUserSwitched(int i) {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            }
        };
        this.mBrightnessObserver = new BrightnessObserver(this.mHandler);
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMinimumBacklight = powerManager.getMinimumScreenBrightnessSetting();
        this.mMaximumBacklight = powerManager.getMaximumScreenBrightnessSetting();
        this.mDefaultBacklight = powerManager.getDefaultScreenBrightnessSetting();
        this.mMinimumBacklightForVr = powerManager.getMinimumScreenBrightnessForVrSetting();
        this.mMaximumBacklightForVr = powerManager.getMaximumScreenBrightnessForVrSetting();
        this.mDefaultBacklightForVr = powerManager.getDefaultScreenBrightnessForVrSetting();
        this.mAutomaticAvailable = context.getResources().getBoolean(R.^attr-private.borderTop);
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        this.mVrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
    }

    @Override
    public void onInit(ToggleSlider toggleSlider) {
    }

    public void registerCallbacks() {
        if (this.mListening) {
            return;
        }
        if (this.mVrManager != null) {
            try {
                this.mVrManager.registerListener(this.mVrStateCallbacks);
                this.mIsVrModeEnabled = this.mVrManager.getVrModeState();
            } catch (RemoteException e) {
                Log.e("StatusBar.BrightnessController", "Failed to register VR mode state listener: ", e);
            }
        }
        this.mBackgroundHandler.post(this.mStartListeningRunnable);
        this.mListening = true;
    }

    public void unregisterCallbacks() {
        if (!this.mListening) {
            return;
        }
        if (this.mVrManager != null) {
            try {
                this.mVrManager.unregisterListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                Log.e("StatusBar.BrightnessController", "Failed to unregister VR mode state listener: ", e);
            }
        }
        this.mBackgroundHandler.post(this.mStopListeningRunnable);
        this.mListening = false;
        this.mControlValueInitialized = false;
    }

    @Override
    public void onChanged(ToggleSlider toggleSlider, boolean z, boolean z2, int i, boolean z3) {
        int i2;
        int i3;
        int i4;
        final String str;
        updateIcon(this.mAutomatic);
        if (this.mExternalChange) {
            return;
        }
        if (this.mSliderAnimator != null) {
            this.mSliderAnimator.cancel();
        }
        if (this.mIsVrModeEnabled) {
            i2 = 498;
            i3 = this.mMinimumBacklightForVr;
            i4 = this.mMaximumBacklightForVr;
            str = "screen_brightness_for_vr";
        } else {
            if (this.mAutomatic) {
                i2 = 219;
            } else {
                i2 = 218;
            }
            i3 = this.mMinimumBacklight;
            i4 = this.mMaximumBacklight;
            str = "screen_brightness";
        }
        final int iConvertGammaToLinear = BrightnessUtils.convertGammaToLinear(i, i3, i4);
        if (z3) {
            MetricsLogger.action(this.mContext, i2, iConvertGammaToLinear);
        }
        setBrightness(iConvertGammaToLinear);
        if (!z) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Settings.System.putIntForUser(BrightnessController.this.mContext.getContentResolver(), str, iConvertGammaToLinear, -2);
                }
            });
        }
        Iterator<BrightnessStateChangeCallback> it = this.mChangeCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onBrightnessLevelChanged();
        }
    }

    public void checkRestrictionAndSetEnabled() {
        this.mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                ((ToggleSliderView) BrightnessController.this.mControl).setEnforcedAdmin(RestrictedLockUtils.checkIfRestrictionEnforced(BrightnessController.this.mContext, "no_config_brightness", BrightnessController.this.mUserTracker.getCurrentUserId()));
            }
        });
    }

    private void setBrightness(int i) {
        this.mDisplayManager.setTemporaryBrightness(i);
    }

    private void updateIcon(boolean z) {
        if (this.mIcon != null) {
            this.mIcon.setImageResource(com.android.systemui.R.drawable.ic_qs_brightness_auto_off);
        }
    }

    private void updateVrMode(boolean z) {
        if (this.mIsVrModeEnabled != z) {
            this.mIsVrModeEnabled = z;
            this.mBackgroundHandler.post(this.mUpdateSliderRunnable);
        }
    }

    private void updateSlider(int i, boolean z) {
        int i2;
        int i3;
        if (z) {
            i2 = this.mMinimumBacklightForVr;
            i3 = this.mMaximumBacklightForVr;
        } else {
            i2 = this.mMinimumBacklight;
            i3 = this.mMaximumBacklight;
        }
        if (i == BrightnessUtils.convertGammaToLinear(this.mControl.getValue(), i2, i3)) {
            return;
        }
        animateSliderTo(BrightnessUtils.convertLinearToGamma(i, i2, i3));
    }

    private void animateSliderTo(int i) {
        if (!this.mControlValueInitialized) {
            this.mControl.setValue(i);
            this.mControlValueInitialized = true;
        }
        if (this.mSliderAnimator != null && this.mSliderAnimator.isStarted()) {
            this.mSliderAnimator.cancel();
        }
        this.mSliderAnimator = ValueAnimator.ofInt(this.mControl.getValue(), i);
        this.mSliderAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                BrightnessController.lambda$animateSliderTo$0(this.f$0, valueAnimator);
            }
        });
        this.mSliderAnimator.setDuration(3000L);
        this.mSliderAnimator.start();
    }

    public static void lambda$animateSliderTo$0(BrightnessController brightnessController, ValueAnimator valueAnimator) {
        brightnessController.mExternalChange = true;
        brightnessController.mControl.setValue(((Integer) valueAnimator.getAnimatedValue()).intValue());
        brightnessController.mExternalChange = false;
    }
}
