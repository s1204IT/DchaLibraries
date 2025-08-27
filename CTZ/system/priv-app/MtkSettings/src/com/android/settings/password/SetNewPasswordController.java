package com.android.settings.password;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;

/* loaded from: classes.dex */
final class SetNewPasswordController {
    private final DevicePolicyManager mDevicePolicyManager;
    private final FingerprintManager mFingerprintManager;
    private final PackageManager mPackageManager;
    private final int mTargetUserId;
    private final Ui mUi;

    interface Ui {
        void launchChooseLock(Bundle bundle);
    }

    /* JADX WARN: Removed duplicated region for block: B:7:0x0030  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static SetNewPasswordController create(Context context, Ui ui, Intent intent, IBinder iBinder) {
        int i;
        int currentUser = ActivityManager.getCurrentUser();
        if ("android.app.action.SET_NEW_PASSWORD".equals(intent.getAction())) {
            int identifier = Utils.getSecureTargetUser(iBinder, UserManager.get(context), null, intent.getExtras()).getIdentifier();
            i = new LockPatternUtils(context).isSeparateProfileChallengeAllowed(identifier) ? identifier : currentUser;
        }
        return new SetNewPasswordController(i, context.getPackageManager(), Utils.getFingerprintManagerOrNull(context), (DevicePolicyManager) context.getSystemService("device_policy"), ui);
    }

    @VisibleForTesting
    SetNewPasswordController(int i, PackageManager packageManager, FingerprintManager fingerprintManager, DevicePolicyManager devicePolicyManager, Ui ui) {
        this.mTargetUserId = i;
        this.mPackageManager = (PackageManager) Preconditions.checkNotNull(packageManager);
        this.mFingerprintManager = fingerprintManager;
        this.mDevicePolicyManager = (DevicePolicyManager) Preconditions.checkNotNull(devicePolicyManager);
        this.mUi = (Ui) Preconditions.checkNotNull(ui);
    }

    public void dispatchSetNewPasswordIntent() {
        Bundle bundle;
        if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint") && this.mFingerprintManager != null && this.mFingerprintManager.isHardwareDetected() && !this.mFingerprintManager.hasEnrolledFingerprints(this.mTargetUserId) && !isFingerprintDisabledByAdmin()) {
            bundle = getFingerprintChooseLockExtras();
        } else {
            bundle = new Bundle();
        }
        bundle.putInt("android.intent.extra.USER_ID", this.mTargetUserId);
        this.mUi.launchChooseLock(bundle);
    }

    private Bundle getFingerprintChooseLockExtras() {
        Bundle bundle = new Bundle();
        long jPreEnroll = this.mFingerprintManager.preEnroll();
        bundle.putInt("minimum_quality", 65536);
        bundle.putBoolean("hide_disabled_prefs", true);
        bundle.putBoolean("has_challenge", true);
        bundle.putLong("challenge", jPreEnroll);
        bundle.putBoolean("for_fingerprint", true);
        return bundle;
    }

    private boolean isFingerprintDisabledByAdmin() {
        return (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mTargetUserId) & 32) != 0;
    }
}
