package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ViewGroup;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.IllegalFormatConversionException;

public class KeyguardIndicationController {
    private final IBatteryStats mBatteryInfo;
    private int mBatteryLevel;
    private int mChargingSpeed;
    private int mChargingWattage;
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private KeyguardIndicationTextView mDisclosure;
    private boolean mDozing;
    private final int mFastThreshold;
    private final Handler mHandler;
    private ViewGroup mIndicationArea;
    private int mInitialTextColor;
    private LockIcon mLockIcon;
    private String mMessageToShowOnScreenOn;
    private boolean mPowerCharged;
    private boolean mPowerPluggedIn;
    private boolean mPowerPluggedInWired;
    private String mRestingIndication;
    private final int mSlowThreshold;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private KeyguardIndicationTextView mTextView;
    private final BroadcastReceiver mTickReceiver;
    private CharSequence mTransientIndication;
    private int mTransientTextColor;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private final UserManager mUserManager;
    private boolean mVisible;
    private final SettableWakeLock mWakeLock;

    public KeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon) {
        this(context, viewGroup, lockIcon, WakeLock.createPartial(context, "Doze:KeyguardIndication"));
        registerCallbacks(KeyguardUpdateMonitor.getInstance(context));
    }

    @VisibleForTesting
    KeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon, WakeLock wakeLock) {
        this.mTickReceiver = new AnonymousClass2();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    KeyguardIndicationController.this.hideTransientIndication();
                } else if (message.what == 2) {
                    KeyguardIndicationController.this.mLockIcon.setTransientFpError(false);
                }
            }
        };
        this.mContext = context;
        this.mIndicationArea = viewGroup;
        this.mTextView = (KeyguardIndicationTextView) viewGroup.findViewById(R.id.keyguard_indication_text);
        this.mInitialTextColor = this.mTextView != null ? this.mTextView.getCurrentTextColor() : -1;
        this.mDisclosure = (KeyguardIndicationTextView) viewGroup.findViewById(R.id.keyguard_indication_enterprise_disclosure);
        this.mLockIcon = lockIcon;
        this.mWakeLock = new SettableWakeLock(wakeLock);
        Resources resources = context.getResources();
        this.mSlowThreshold = resources.getInteger(R.integer.config_chargingSlowlyThreshold);
        this.mFastThreshold = resources.getInteger(R.integer.config_chargingFastThreshold);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        updateDisclosure();
    }

    private void registerCallbacks(KeyguardUpdateMonitor keyguardUpdateMonitor) {
        keyguardUpdateMonitor.registerCallback(getKeyguardCallback());
        this.mContext.registerReceiverAsUser(this.mTickReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.TIME_TICK"), null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
    }

    protected KeyguardUpdateMonitorCallback getKeyguardCallback() {
        if (this.mUpdateMonitorCallback == null) {
            this.mUpdateMonitorCallback = new BaseKeyguardCallback();
        }
        return this.mUpdateMonitorCallback;
    }

    private void updateDisclosure() {
        if (this.mDevicePolicyManager == null) {
            return;
        }
        if (!this.mDozing && this.mDevicePolicyManager.isDeviceManaged()) {
            CharSequence deviceOwnerOrganizationName = this.mDevicePolicyManager.getDeviceOwnerOrganizationName();
            if (deviceOwnerOrganizationName != null) {
                this.mDisclosure.switchIndication(this.mContext.getResources().getString(R.string.do_disclosure_with_name, deviceOwnerOrganizationName));
            } else {
                this.mDisclosure.switchIndication(R.string.do_disclosure_generic);
            }
            this.mDisclosure.setVisibility(0);
            return;
        }
        this.mDisclosure.setVisibility(8);
    }

    public void setVisible(boolean z) {
        this.mVisible = z;
        this.mIndicationArea.setVisibility(z ? 0 : 8);
        if (z) {
            if (!this.mHandler.hasMessages(1)) {
                hideTransientIndication();
            }
            updateIndication(false);
        } else if (!z) {
            hideTransientIndication();
        }
    }

    protected String getTrustGrantedIndication() {
        return null;
    }

    protected String getTrustManagedIndication() {
        return null;
    }

    public void hideTransientIndicationDelayed(long j) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), j);
    }

    public void showTransientIndication(int i) {
        showTransientIndication(this.mContext.getResources().getString(i));
    }

    public void showTransientIndication(CharSequence charSequence) {
        showTransientIndication(charSequence, this.mInitialTextColor);
    }

    public void showTransientIndication(CharSequence charSequence, int i) {
        this.mTransientIndication = charSequence;
        this.mTransientTextColor = i;
        this.mHandler.removeMessages(1);
        if (this.mDozing && !TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(true);
            hideTransientIndicationDelayed(5000L);
        }
        updateIndication(false);
    }

    public void hideTransientIndication() {
        if (this.mTransientIndication != null) {
            this.mTransientIndication = null;
            this.mHandler.removeMessages(1);
            updateIndication(false);
        }
    }

    protected final void updateIndication(boolean z) {
        if (TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(false);
        }
        if (this.mVisible) {
            if (this.mDozing) {
                this.mTextView.setTextColor(-1);
                if (!TextUtils.isEmpty(this.mTransientIndication)) {
                    this.mTextView.switchIndication(this.mTransientIndication);
                    return;
                }
                if (this.mPowerPluggedIn) {
                    String strComputePowerIndication = computePowerIndication();
                    if (z) {
                        animateText(this.mTextView, strComputePowerIndication);
                        return;
                    } else {
                        this.mTextView.switchIndication(strComputePowerIndication);
                        return;
                    }
                }
                this.mTextView.switchIndication(NumberFormat.getPercentInstance().format(this.mBatteryLevel / 100.0f));
                return;
            }
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            String trustGrantedIndication = getTrustGrantedIndication();
            String trustManagedIndication = getTrustManagedIndication();
            if (!this.mUserManager.isUserUnlocked(currentUser)) {
                this.mTextView.switchIndication(android.R.string.date_picker_decrement_year_button);
                this.mTextView.setTextColor(this.mInitialTextColor);
                return;
            }
            if (!TextUtils.isEmpty(this.mTransientIndication)) {
                this.mTextView.switchIndication(this.mTransientIndication);
                this.mTextView.setTextColor(this.mTransientTextColor);
                return;
            }
            if (!TextUtils.isEmpty(trustGrantedIndication) && keyguardUpdateMonitor.getUserHasTrust(currentUser)) {
                this.mTextView.switchIndication(trustGrantedIndication);
                this.mTextView.setTextColor(this.mInitialTextColor);
                return;
            }
            if (this.mPowerPluggedIn) {
                String strComputePowerIndication2 = computePowerIndication();
                this.mTextView.setTextColor(this.mInitialTextColor);
                if (z) {
                    animateText(this.mTextView, strComputePowerIndication2);
                    return;
                } else {
                    this.mTextView.switchIndication(strComputePowerIndication2);
                    return;
                }
            }
            if (!TextUtils.isEmpty(trustManagedIndication) && keyguardUpdateMonitor.getUserTrustIsManaged(currentUser) && !keyguardUpdateMonitor.getUserHasTrust(currentUser)) {
                this.mTextView.switchIndication(trustManagedIndication);
                this.mTextView.setTextColor(this.mInitialTextColor);
            } else {
                this.mTextView.switchIndication(this.mRestingIndication);
                this.mTextView.setTextColor(this.mInitialTextColor);
            }
        }
    }

    private void animateText(final KeyguardIndicationTextView keyguardIndicationTextView, final String str) {
        final int integer = this.mContext.getResources().getInteger(R.integer.wired_charging_keyguard_text_animation_distance);
        int integer2 = this.mContext.getResources().getInteger(R.integer.wired_charging_keyguard_text_animation_duration_up);
        final int integer3 = this.mContext.getResources().getInteger(R.integer.wired_charging_keyguard_text_animation_duration_down);
        keyguardIndicationTextView.animate().translationYBy(integer).setInterpolator(Interpolators.LINEAR).setDuration(integer2).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                keyguardIndicationTextView.switchIndication(str);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                keyguardIndicationTextView.animate().setDuration(integer3).setInterpolator(Interpolators.BOUNCE).translationYBy((-1) * integer).setListener(null);
            }
        });
    }

    private String computePowerIndication() {
        long jComputeChargeTimeRemaining;
        int i;
        if (this.mPowerCharged) {
            return this.mContext.getResources().getString(R.string.keyguard_charged);
        }
        try {
            jComputeChargeTimeRemaining = this.mBatteryInfo.computeChargeTimeRemaining();
        } catch (RemoteException e) {
            Log.e("KeyguardIndication", "Error calling IBatteryStats: ", e);
            jComputeChargeTimeRemaining = 0;
        }
        boolean z = jComputeChargeTimeRemaining > 0;
        int i2 = this.mChargingSpeed;
        if (i2 != 0) {
            if (i2 == 2) {
                if (z) {
                    i = R.string.keyguard_indication_charging_time_fast;
                } else {
                    i = R.string.keyguard_plugged_in_charging_fast;
                }
            } else if (z) {
                i = R.string.keyguard_indication_charging_time;
            } else {
                i = R.string.keyguard_plugged_in;
            }
        } else if (z) {
            i = R.string.keyguard_indication_charging_time_slowly;
        } else {
            i = R.string.keyguard_plugged_in_charging_slowly;
        }
        String str = NumberFormat.getPercentInstance().format(this.mBatteryLevel / 100.0f);
        if (z) {
            String shortElapsedTimeRoundingUpToMinutes = Formatter.formatShortElapsedTimeRoundingUpToMinutes(this.mContext, jComputeChargeTimeRemaining);
            try {
                return this.mContext.getResources().getString(i, shortElapsedTimeRoundingUpToMinutes, str);
            } catch (IllegalFormatConversionException e2) {
                return this.mContext.getResources().getString(i, shortElapsedTimeRoundingUpToMinutes);
            }
        }
        try {
            return this.mContext.getResources().getString(i, str);
        } catch (IllegalFormatConversionException e3) {
            return this.mContext.getResources().getString(i);
        }
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            KeyguardIndicationController.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    KeyguardIndicationController.AnonymousClass2.lambda$onReceive$0(this.f$0);
                }
            });
        }

        public static void lambda$onReceive$0(AnonymousClass2 anonymousClass2) {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication(false);
            }
        }
    }

    public void setDozing(boolean z) {
        if (this.mDozing == z) {
            return;
        }
        this.mDozing = z;
        updateIndication(false);
        updateDisclosure();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("KeyguardIndicationController:");
        printWriter.println("  mTransientTextColor: " + Integer.toHexString(this.mTransientTextColor));
        printWriter.println("  mInitialTextColor: " + Integer.toHexString(this.mInitialTextColor));
        printWriter.println("  mPowerPluggedInWired: " + this.mPowerPluggedInWired);
        printWriter.println("  mPowerPluggedIn: " + this.mPowerPluggedIn);
        printWriter.println("  mPowerCharged: " + this.mPowerCharged);
        printWriter.println("  mChargingSpeed: " + this.mChargingSpeed);
        printWriter.println("  mChargingWattage: " + this.mChargingWattage);
        printWriter.println("  mMessageToShowOnScreenOn: " + this.mMessageToShowOnScreenOn);
        printWriter.println("  mDozing: " + this.mDozing);
        printWriter.println("  mBatteryLevel: " + this.mBatteryLevel);
        StringBuilder sb = new StringBuilder();
        sb.append("  mTextView.getText(): ");
        sb.append((Object) (this.mTextView == null ? null : this.mTextView.getText()));
        printWriter.println(sb.toString());
        printWriter.println("  computePowerIndication(): " + computePowerIndication());
    }

    protected class BaseKeyguardCallback extends KeyguardUpdateMonitorCallback {
        private int mLastSuccessiveErrorMessage = -1;

        protected BaseKeyguardCallback() {
        }

        @Override
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus batteryStatus) {
            boolean z = batteryStatus.status == 2 || batteryStatus.status == 5;
            boolean z2 = KeyguardIndicationController.this.mPowerPluggedIn;
            KeyguardIndicationController.this.mPowerPluggedInWired = batteryStatus.isPluggedInWired() && z;
            KeyguardIndicationController.this.mPowerPluggedIn = batteryStatus.isPluggedIn() && z;
            KeyguardIndicationController.this.mPowerCharged = batteryStatus.isCharged();
            KeyguardIndicationController.this.mChargingWattage = batteryStatus.maxChargingWattage;
            KeyguardIndicationController.this.mChargingSpeed = batteryStatus.getChargingSpeed(KeyguardIndicationController.this.mSlowThreshold, KeyguardIndicationController.this.mFastThreshold);
            KeyguardIndicationController.this.mBatteryLevel = batteryStatus.level;
            KeyguardIndicationController.this.updateIndication(!z2 && KeyguardIndicationController.this.mPowerPluggedInWired);
            if (KeyguardIndicationController.this.mDozing) {
                if (!z2 && KeyguardIndicationController.this.mPowerPluggedIn) {
                    KeyguardIndicationController.this.showTransientIndication(KeyguardIndicationController.this.computePowerIndication());
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000L);
                } else if (z2 && !KeyguardIndicationController.this.mPowerPluggedIn) {
                    KeyguardIndicationController.this.hideTransientIndication();
                }
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean z) {
            if (z) {
                KeyguardIndicationController.this.updateDisclosure();
            }
        }

        @Override
        public void onFingerprintHelp(int i, String str) {
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (keyguardUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                int colorError = Utils.getColorError(KeyguardIndicationController.this.mContext);
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(str, colorError);
                } else if (keyguardUpdateMonitor.isScreenOn()) {
                    KeyguardIndicationController.this.mLockIcon.setTransientFpError(true);
                    KeyguardIndicationController.this.showTransientIndication(str, colorError);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(1300L);
                    KeyguardIndicationController.this.mHandler.removeMessages(2);
                    KeyguardIndicationController.this.mHandler.sendMessageDelayed(KeyguardIndicationController.this.mHandler.obtainMessage(2), 1300L);
                }
                this.mLastSuccessiveErrorMessage = -1;
            }
        }

        @Override
        public void onFingerprintError(int i, String str) {
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if ((keyguardUpdateMonitor.isUnlockingWithFingerprintAllowed() || i == 9) && i != 5) {
                int colorError = Utils.getColorError(KeyguardIndicationController.this.mContext);
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    if (this.mLastSuccessiveErrorMessage != i) {
                        KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(str, colorError);
                    }
                } else if (!keyguardUpdateMonitor.isScreenOn()) {
                    KeyguardIndicationController.this.mMessageToShowOnScreenOn = str;
                } else {
                    KeyguardIndicationController.this.showTransientIndication(str, colorError);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000L);
                }
                this.mLastSuccessiveErrorMessage = i;
            }
        }

        @Override
        public void onTrustAgentErrorMessage(CharSequence charSequence) {
            KeyguardIndicationController.this.showTransientIndication(charSequence, Utils.getColorError(KeyguardIndicationController.this.mContext));
        }

        @Override
        public void onScreenTurnedOn() {
            if (KeyguardIndicationController.this.mMessageToShowOnScreenOn != null) {
                KeyguardIndicationController.this.showTransientIndication(KeyguardIndicationController.this.mMessageToShowOnScreenOn, Utils.getColorError(KeyguardIndicationController.this.mContext));
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000L);
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        @Override
        public void onFingerprintRunningStateChanged(boolean z) {
            if (z) {
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        @Override
        public void onFingerprintAuthenticated(int i) {
            super.onFingerprintAuthenticated(i);
            this.mLastSuccessiveErrorMessage = -1;
        }

        @Override
        public void onFingerprintAuthFailed() {
            super.onFingerprintAuthFailed();
            this.mLastSuccessiveErrorMessage = -1;
        }

        @Override
        public void onUserUnlocked() {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication(false);
            }
        }
    }
}
