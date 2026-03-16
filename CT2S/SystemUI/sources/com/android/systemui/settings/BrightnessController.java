package com.android.systemui.settings;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.widget.ImageView;
import com.android.systemui.settings.ToggleSlider;
import java.util.ArrayList;

public class BrightnessController implements ToggleSlider.Listener {
    private boolean mAutomatic;
    private final boolean mAutomaticAvailable;
    private final Context mContext;
    private final ToggleSlider mControl;
    private boolean mExternalChange;
    private final ImageView mIcon;
    private boolean mListening;
    private final int mMaximumBacklight;
    private final int mMinimumBacklight;
    private final IPowerManager mPower;
    private final CurrentUserTracker mUserTracker;
    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private final BrightnessObserver mBrightnessObserver = new BrightnessObserver(this.mHandler);

    public interface BrightnessStateChangeCallback {
        void onBrightnessLevelChanged();
    }

    private class BrightnessObserver extends ContentObserver {
        private final Uri BRIGHTNESS_ADJ_URI;
        private final Uri BRIGHTNESS_MODE_URI;
        private final Uri BRIGHTNESS_URI;

        public BrightnessObserver(Handler handler) {
            super(handler);
            this.BRIGHTNESS_MODE_URI = Settings.System.getUriFor("screen_brightness_mode");
            this.BRIGHTNESS_URI = Settings.System.getUriFor("screen_brightness");
            this.BRIGHTNESS_ADJ_URI = Settings.System.getUriFor("screen_auto_brightness_adj");
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!selfChange) {
                try {
                    BrightnessController.this.mExternalChange = true;
                    if (this.BRIGHTNESS_MODE_URI.equals(uri)) {
                        BrightnessController.this.updateMode();
                        BrightnessController.this.updateSlider();
                    } else if (this.BRIGHTNESS_URI.equals(uri) && !BrightnessController.this.mAutomatic) {
                        BrightnessController.this.updateSlider();
                    } else if (!this.BRIGHTNESS_ADJ_URI.equals(uri) || !BrightnessController.this.mAutomatic) {
                        BrightnessController.this.updateMode();
                        BrightnessController.this.updateSlider();
                    } else {
                        BrightnessController.this.updateSlider();
                    }
                    for (BrightnessStateChangeCallback cb : BrightnessController.this.mChangeCallbacks) {
                        cb.onBrightnessLevelChanged();
                    }
                } finally {
                    BrightnessController.this.mExternalChange = false;
                }
            }
        }

        public void startObserving() {
            ContentResolver cr = BrightnessController.this.mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(this.BRIGHTNESS_MODE_URI, false, this, -1);
            cr.registerContentObserver(this.BRIGHTNESS_URI, false, this, -1);
            cr.registerContentObserver(this.BRIGHTNESS_ADJ_URI, false, this, -1);
        }

        public void stopObserving() {
            ContentResolver cr = BrightnessController.this.mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }
    }

    public BrightnessController(Context context, ImageView icon, ToggleSlider control) {
        this.mContext = context;
        this.mIcon = icon;
        this.mControl = control;
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                BrightnessController.this.updateMode();
                BrightnessController.this.updateSlider();
            }
        };
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        this.mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();
        this.mAutomaticAvailable = context.getResources().getBoolean(R.^attr-private.borderRight);
        this.mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
    }

    @Override
    public void onInit(ToggleSlider control) {
    }

    public void registerCallbacks() {
        if (!this.mListening) {
            this.mBrightnessObserver.startObserving();
            this.mUserTracker.startTracking();
            updateMode();
            updateSlider();
            this.mControl.setOnChangedListener(this);
            this.mListening = true;
        }
    }

    public void unregisterCallbacks() {
        if (this.mListening) {
            this.mBrightnessObserver.stopObserving();
            this.mUserTracker.stopTracking();
            this.mControl.setOnChangedListener(null);
            this.mListening = false;
        }
    }

    @Override
    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value) {
        updateIcon(this.mAutomatic);
        if (!this.mExternalChange) {
            if (!this.mAutomatic) {
                final int val = value + this.mMinimumBacklight;
                setBrightness(val);
                if (!tracking) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            Settings.System.putIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", val, -2);
                        }
                    });
                }
            } else {
                final float adj = (value / 50.0f) - 1.0f;
                setBrightnessAdj(adj);
                if (!tracking) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            Settings.System.putFloatForUser(BrightnessController.this.mContext.getContentResolver(), "screen_auto_brightness_adj", adj, -2);
                        }
                    });
                }
            }
            for (BrightnessStateChangeCallback cb : this.mChangeCallbacks) {
                cb.onBrightnessLevelChanged();
            }
        }
    }

    private void setBrightness(int brightness) {
        try {
            this.mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException e) {
        }
    }

    private void setBrightnessAdj(float adj) {
        try {
            this.mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj);
        } catch (RemoteException e) {
        }
    }

    private void updateIcon(boolean automatic) {
        if (this.mIcon != null) {
            ImageView imageView = this.mIcon;
            if (automatic) {
            }
            imageView.setImageResource(com.android.systemui.R.drawable.ic_qs_brightness_auto_off);
        }
    }

    private void updateMode() {
        if (this.mAutomaticAvailable) {
            int automatic = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2);
            this.mAutomatic = automatic != 0;
            updateIcon(this.mAutomatic);
        } else {
            this.mControl.setChecked(false);
            updateIcon(false);
        }
    }

    private void updateSlider() {
        if (this.mAutomatic) {
            float value = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", 0.0f, -2);
            this.mControl.setMax(100);
            this.mControl.setValue((int) (((1.0f + value) * 100.0f) / 2.0f));
        } else {
            int value2 = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mMaximumBacklight, -2);
            this.mControl.setMax(this.mMaximumBacklight - this.mMinimumBacklight);
            this.mControl.setValue(value2 - this.mMinimumBacklight);
        }
    }
}
