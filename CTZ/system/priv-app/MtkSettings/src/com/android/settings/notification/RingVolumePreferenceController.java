package com.android.settings.notification;

import android.app.NotificationManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.Objects;

/* loaded from: classes.dex */
public class RingVolumePreferenceController extends VolumeSeekBarPreferenceController {
    private static final String KEY_RING_VOLUME = "ring_volume";
    private static final String TAG = "RingVolumeController";
    private final H mHandler;
    private int mMuteIcon;
    private final RingReceiver mReceiver;
    private int mRingerMode;
    private ComponentName mSuppressor;
    private Vibrator mVibrator;

    public RingVolumePreferenceController(Context context) {
        this(context, KEY_RING_VOLUME);
    }

    public RingVolumePreferenceController(Context context, String str) {
        super(context, str);
        this.mRingerMode = -1;
        this.mReceiver = new RingReceiver();
        this.mHandler = new H();
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (this.mVibrator != null && !this.mVibrator.hasVibrator()) {
            this.mVibrator = null;
        }
        updateRingerMode();
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        super.onResume();
        this.mReceiver.register(true);
        updateEffectsSuppressor();
        updatePreferenceIcon();
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        super.onPause();
        this.mReceiver.register(false);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_RING_VOLUME;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return (!Utils.isVoiceCapable(this.mContext) || this.mHelper.isSingleVolume()) ? 2 : 0;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_RING_VOLUME);
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getAudioStream() {
        return 2;
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getMuteIcon() {
        return this.mMuteIcon;
    }

    private void updateRingerMode() {
        int ringerModeInternal = this.mHelper.getRingerModeInternal();
        if (this.mRingerMode == ringerModeInternal) {
            return;
        }
        this.mRingerMode = ringerModeInternal;
        updatePreferenceIcon();
    }

    private void updateEffectsSuppressor() {
        ComponentName effectsSuppressor = NotificationManager.from(this.mContext).getEffectsSuppressor();
        if (Objects.equals(effectsSuppressor, this.mSuppressor)) {
            return;
        }
        this.mSuppressor = effectsSuppressor;
        if (this.mPreference != null) {
            this.mPreference.setSuppressionText(SuppressorHelper.getSuppressionText(this.mContext, effectsSuppressor));
        }
        updatePreferenceIcon();
    }

    private void updatePreferenceIcon() {
        if (this.mPreference != null) {
            if (this.mRingerMode == 1) {
                this.mMuteIcon = R.drawable.ic_volume_ringer_vibrate;
                this.mPreference.showIcon(R.drawable.ic_volume_ringer_vibrate);
            } else if (this.mRingerMode == 0) {
                this.mMuteIcon = R.drawable.ic_notifications_off_24dp;
                this.mPreference.showIcon(R.drawable.ic_notifications_off_24dp);
            } else {
                this.mPreference.showIcon(R.drawable.ic_notifications);
            }
        }
    }

    private final class H extends Handler {
        private H() {
            super(Looper.getMainLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    RingVolumePreferenceController.this.updateEffectsSuppressor();
                    break;
                case 2:
                    RingVolumePreferenceController.this.updateRingerMode();
                    break;
            }
        }
    }

    private class RingReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        private RingReceiver() {
        }

        public void register(boolean z) {
            if (this.mRegistered == z) {
                return;
            }
            if (!z) {
                RingVolumePreferenceController.this.mContext.unregisterReceiver(this);
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                intentFilter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
                RingVolumePreferenceController.this.mContext.registerReceiver(this, intentFilter);
            }
            this.mRegistered = z;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(action)) {
                RingVolumePreferenceController.this.mHandler.sendEmptyMessage(1);
            } else if ("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(action)) {
                RingVolumePreferenceController.this.mHandler.sendEmptyMessage(2);
            }
        }
    }
}
