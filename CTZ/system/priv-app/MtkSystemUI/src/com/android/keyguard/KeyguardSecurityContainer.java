package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.StatsLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.Telephony.KeyguardSimPinPukMeView;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManagerProxy;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public class KeyguardSecurityContainer extends FrameLayout implements KeyguardSecurityView {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private AlertDialog mAlertDialog;
    private KeyguardSecurityCallback mCallback;
    private KeyguardSecurityModel.SecurityMode mCurrentSecuritySelection;
    private LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityCallback mNullCallback;
    private SecurityCallback mSecurityCallback;
    private KeyguardSecurityModel mSecurityModel;
    private KeyguardSecurityViewFlipper mSecurityViewFlipper;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public interface SecurityCallback {
        boolean dismiss(boolean z, int i);

        void finish(boolean z, int i);

        void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean z);

        void reset();

        void updateNavbarStatus();

        void userActivity();
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardSecurityContainer(Context context) {
        this(context, null, 0);
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mCurrentSecuritySelection = KeyguardSecurityModel.SecurityMode.Invalid;
        this.mCallback = new KeyguardSecurityCallback() { // from class: com.android.keyguard.KeyguardSecurityContainer.1
            AnonymousClass1() {
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void userActivity() {
                if (KeyguardSecurityContainer.this.mSecurityCallback != null) {
                    KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
                }
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void dismiss(boolean z, int i2) {
                KeyguardSecurityContainer.this.mSecurityCallback.dismiss(z, i2);
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void reportUnlockAttempt(int i2, boolean z, int i3) {
                KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(KeyguardSecurityContainer.this.mContext);
                if (z) {
                    StatsLog.write(64, 2);
                    keyguardUpdateMonitor.clearFailedUnlockAttempts();
                    KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt(i2);
                } else {
                    StatsLog.write(64, 1);
                    KeyguardSecurityContainer.this.reportFailedUnlockAttempt(i2, i3);
                }
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void reset() {
                KeyguardSecurityContainer.this.mSecurityCallback.reset();
            }
        };
        this.mNullCallback = new KeyguardSecurityCallback() { // from class: com.android.keyguard.KeyguardSecurityContainer.2
            AnonymousClass2() {
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void userActivity() {
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void reportUnlockAttempt(int i2, boolean z, int i3) {
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void dismiss(boolean z, int i2) {
            }

            @Override // com.android.keyguard.KeyguardSecurityCallback
            public void reset() {
            }
        };
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    public void setSecurityCallback(SecurityCallback securityCallback) {
        this.mSecurityCallback = securityCallback;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onResume(int i) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onResume(i);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onPause() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
        }
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onPause();
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).startAppearAnimation();
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean startDisappearAnimation(Runnable runnable) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            return getSecurityView(this.mCurrentSecuritySelection).startDisappearAnimation(runnable);
        }
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public CharSequence getTitle() {
        return this.mSecurityViewFlipper.getTitle();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v5, resolved type: android.view.View */
    /* JADX WARN: Multi-variable type inference failed */
    private KeyguardSecurityView getSecurityView(KeyguardSecurityModel.SecurityMode securityMode) {
        KeyguardSecurityView keyguardSecurityView;
        int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
        int childCount = this.mSecurityViewFlipper.getChildCount();
        int i = 0;
        while (true) {
            if (i < childCount) {
                if (this.mSecurityViewFlipper.getChildAt(i).getId() != securityViewIdForMode) {
                    i++;
                } else {
                    keyguardSecurityView = (KeyguardSecurityView) this.mSecurityViewFlipper.getChildAt(i);
                    break;
                }
            } else {
                keyguardSecurityView = null;
                break;
            }
        }
        int layoutIdFor = getLayoutIdFor(securityMode);
        if (keyguardSecurityView == null && layoutIdFor != 0) {
            LayoutInflater layoutInflaterFrom = LayoutInflater.from(this.mContext);
            if (DEBUG) {
                Log.v("KeyguardSecurityView", "inflating id = " + layoutIdFor);
            }
            View viewInflate = layoutInflaterFrom.inflate(layoutIdFor, (ViewGroup) this.mSecurityViewFlipper, false);
            KeyguardSecurityView keyguardSecurityView2 = (KeyguardSecurityView) viewInflate;
            if (keyguardSecurityView2 instanceof KeyguardSimPinPukMeView) {
                ((KeyguardSimPinPukMeView) keyguardSecurityView2).setPhoneId(this.mSecurityModel.getPhoneIdUsingSecurityMode(securityMode));
            }
            this.mSecurityViewFlipper.addView(viewInflate);
            updateSecurityView(viewInflate);
            return keyguardSecurityView2;
        }
        if (keyguardSecurityView != null && (keyguardSecurityView instanceof KeyguardSimPinPukMeView) && securityMode != this.mCurrentSecuritySelection) {
            Log.i("KeyguardSecurityView", "getSecurityView, here, we will refresh the layout");
            ((KeyguardSimPinPukMeView) keyguardSecurityView).setPhoneId(this.mSecurityModel.getPhoneIdUsingSecurityMode(securityMode));
            return keyguardSecurityView;
        }
        return keyguardSecurityView;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: android.view.View */
    /* JADX WARN: Multi-variable type inference failed */
    private void updateSecurityView(View view) {
        if (view instanceof KeyguardSecurityView) {
            KeyguardSecurityView keyguardSecurityView = (KeyguardSecurityView) view;
            keyguardSecurityView.setKeyguardCallback(this.mCallback);
            keyguardSecurityView.setLockPatternUtils(this.mLockPatternUtils);
        } else {
            Log.w("KeyguardSecurityView", "View " + view + " is not a KeyguardSecurityView");
        }
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        this.mSecurityViewFlipper = (KeyguardSecurityViewFlipper) findViewById(com.android.systemui.R.id.view_flipper);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityModel.setLockPatternUtils(lockPatternUtils);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    private void showDialog(String str, String str2) {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
        this.mAlertDialog = new AlertDialog.Builder(this.mContext).setTitle(str).setMessage(str2).setCancelable(false).setNeutralButton(com.android.systemui.R.string.ok, (DialogInterface.OnClickListener) null).create();
        if (!(this.mContext instanceof Activity)) {
            this.mAlertDialog.getWindow().setType(2009);
        }
        this.mAlertDialog.show();
    }

    private void showAlmostAtWipeDialog(int i, int i2, int i3) {
        String string;
        switch (i3) {
            case 1:
                string = this.mContext.getString(com.android.systemui.R.string.kg_failed_attempts_almost_at_wipe, Integer.valueOf(i), Integer.valueOf(i2));
                break;
            case 2:
                string = this.mContext.getString(com.android.systemui.R.string.kg_failed_attempts_almost_at_erase_profile, Integer.valueOf(i), Integer.valueOf(i2));
                break;
            case 3:
                string = this.mContext.getString(com.android.systemui.R.string.kg_failed_attempts_almost_at_erase_user, Integer.valueOf(i), Integer.valueOf(i2));
                break;
            default:
                string = null;
                break;
        }
        showDialog(null, string);
    }

    private void showWipeDialog(int i, int i2) {
        String string;
        switch (i2) {
            case 1:
                string = this.mContext.getString(com.android.systemui.R.string.kg_failed_attempts_now_wiping, Integer.valueOf(i));
                break;
            case 2:
                string = this.mContext.getString(com.android.systemui.R.string.kg_failed_attempts_now_erasing_profile, Integer.valueOf(i));
                break;
            case 3:
                string = this.mContext.getString(com.android.systemui.R.string.kg_failed_attempts_now_erasing_user, Integer.valueOf(i));
                break;
            default:
                string = null;
                break;
        }
        showDialog(null, string);
    }

    private void reportFailedUnlockAttempt(int i, int i2) {
        int i3;
        KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int i4 = 1;
        int failedUnlockAttempts = keyguardUpdateMonitor.getFailedUnlockAttempts(i) + 1;
        if (DEBUG) {
            Log.d("KeyguardSecurityView", "reportFailedPatternAttempt: #" + failedUnlockAttempts);
        }
        DevicePolicyManager devicePolicyManager = this.mLockPatternUtils.getDevicePolicyManager();
        int maximumFailedPasswordsForWipe = devicePolicyManager.getMaximumFailedPasswordsForWipe(null, i);
        if (maximumFailedPasswordsForWipe > 0) {
            i3 = maximumFailedPasswordsForWipe - failedUnlockAttempts;
        } else {
            i3 = Integer.MAX_VALUE;
        }
        if (i3 < 5) {
            int profileWithMinimumFailedPasswordsForWipe = devicePolicyManager.getProfileWithMinimumFailedPasswordsForWipe(i);
            if (profileWithMinimumFailedPasswordsForWipe == i) {
                if (profileWithMinimumFailedPasswordsForWipe != 0) {
                    i4 = 3;
                }
            } else if (profileWithMinimumFailedPasswordsForWipe != -10000) {
                i4 = 2;
            }
            if (i3 > 0) {
                showAlmostAtWipeDialog(failedUnlockAttempts, i3, i4);
            } else {
                Slog.i("KeyguardSecurityView", "Too many unlock attempts; user " + profileWithMinimumFailedPasswordsForWipe + " will be wiped!");
                showWipeDialog(failedUnlockAttempts, i4);
            }
        }
        keyguardUpdateMonitor.reportFailedStrongAuthUnlockAttempt(i);
        this.mLockPatternUtils.reportFailedPasswordAttempt(i);
        if (i2 > 0) {
            this.mLockPatternUtils.reportPasswordLockout(i2, i);
        }
    }

    void showPrimarySecurityScreen(boolean z) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
        if (DEBUG) {
            Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(turningOff=" + z + ")");
        }
        Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(securityMode=" + securityMode + ")");
        if (this.mSecurityModel.isSimPinPukSecurityMode(this.mCurrentSecuritySelection)) {
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - current is " + this.mCurrentSecuritySelection);
            int phoneIdUsingSecurityMode = this.mSecurityModel.getPhoneIdUsingSecurityMode(this.mCurrentSecuritySelection);
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - phoneId of currentView is " + phoneIdUsingSecurityMode);
            boolean zIsSimPinSecure = this.mUpdateMonitor.isSimPinSecure(phoneIdUsingSecurityMode);
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - isCurrentModeSimPinSecure = " + zIsSimPinSecure);
            if (zIsSimPinSecure) {
                Log.d("KeyguardSecurityView", "Skip show security because it already shows SimPinPukMeView");
                return;
            }
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - since current simpinview not secured, we should call showSecurityScreen() to set correct PhoneId for next view.");
        }
        showSecurityScreen(securityMode);
    }

    boolean showNextSecurityScreenOrFinish(boolean z, int i) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (DEBUG) {
            Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish(" + z + ")");
        }
        Log.d("KeyguardSecurityView", "showNext.. mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
        boolean z2 = false;
        boolean z3 = true;
        if (!this.mUpdateMonitor.getUserCanSkipBouncer(i)) {
            if (KeyguardSecurityModel.SecurityMode.None != this.mCurrentSecuritySelection) {
                if (z) {
                    if (DEBUG) {
                        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - authenticated is True, and mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
                    }
                    KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
                    if (DEBUG) {
                        Log.v("KeyguardSecurityView", "securityMode = " + securityMode);
                    }
                    Log.d("KeyguardSecurityView", "mCurrentSecuritySelection: " + this.mCurrentSecuritySelection);
                    switch (this.mCurrentSecuritySelection) {
                        case Pattern:
                        case PIN:
                        case Password:
                            z2 = true;
                            break;
                        case Invalid:
                        case None:
                        default:
                            Log.v("KeyguardSecurityView", "Bad security screen " + this.mCurrentSecuritySelection + ", fail safe");
                            showPrimarySecurityScreen(false);
                            z3 = false;
                            break;
                        case SimPinPukMe1:
                        case SimPinPukMe2:
                        case SimPinPukMe3:
                        case SimPinPukMe4:
                            if (securityMode != KeyguardSecurityModel.SecurityMode.None) {
                                showSecurityScreen(securityMode);
                                z3 = false;
                                break;
                            }
                            break;
                        case AntiTheft:
                            KeyguardSecurityModel.SecurityMode securityMode2 = this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
                            if (DEBUG) {
                                Log.v("KeyguardSecurityView", "now is Antitheft, next securityMode = " + securityMode2);
                            }
                            if (securityMode2 != KeyguardSecurityModel.SecurityMode.None) {
                                showSecurityScreen(securityMode2);
                                z3 = false;
                                break;
                            }
                            break;
                    }
                } else {
                    z3 = false;
                }
            } else {
                KeyguardSecurityModel.SecurityMode securityMode3 = this.mSecurityModel.getSecurityMode(i);
                if (KeyguardSecurityModel.SecurityMode.None == securityMode3) {
                    if (DEBUG) {
                        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - securityMode is None, just finish.");
                    }
                } else {
                    if (DEBUG) {
                        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish()- switch to the alternate security view for None mode.");
                    }
                    showSecurityScreen(securityMode3);
                    z3 = false;
                }
            }
        }
        this.mSecurityCallback.updateNavbarStatus();
        if (z3) {
            this.mSecurityCallback.finish(z2, i);
            Log.d("KeyguardSecurityView", "finish ");
        }
        if (DEBUG) {
            Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - return finish = " + z3);
        }
        return z3;
    }

    private void showSecurityScreen(KeyguardSecurityModel.SecurityMode securityMode) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (DEBUG) {
            Log.d("KeyguardSecurityView", "showSecurityScreen(" + securityMode + ")");
        }
        if (securityMode == this.mCurrentSecuritySelection && securityMode != KeyguardSecurityModel.SecurityMode.AntiTheft) {
            return;
        }
        VoiceWakeupManagerProxy.getInstance().notifySecurityModeChange(this.mCurrentSecuritySelection, securityMode);
        Log.d("KeyguardSecurityView", "showSecurityScreen() - get oldview for" + this.mCurrentSecuritySelection + ", get newview for" + securityMode);
        KeyguardSecurityView securityView = getSecurityView(this.mCurrentSecuritySelection);
        KeyguardSecurityView securityView2 = getSecurityView(securityMode);
        if (securityView != null) {
            securityView.onPause();
            securityView.setKeyguardCallback(this.mNullCallback);
        }
        if (securityMode != KeyguardSecurityModel.SecurityMode.None) {
            securityView2.setKeyguardCallback(this.mCallback);
            Log.d("KeyguardSecurityView", "showSecurityScreen() - newview.setKeyguardCallback(mCallback)");
            securityView2.onResume(2);
        }
        int childCount = this.mSecurityViewFlipper.getChildCount();
        int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
        boolean z = false;
        int i = 0;
        while (true) {
            if (i >= childCount) {
                break;
            }
            if (this.mSecurityViewFlipper.getChildAt(i).getId() != securityViewIdForMode) {
                i++;
            } else {
                this.mSecurityViewFlipper.setDisplayedChild(i);
                break;
            }
        }
        this.mCurrentSecuritySelection = securityMode;
        Log.d("KeyguardSecurityView", "Before update, mCurrentSecuritySelection = " + securityMode + "After update, mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
        SecurityCallback securityCallback = this.mSecurityCallback;
        if (securityMode != KeyguardSecurityModel.SecurityMode.None && securityView2.needsInput()) {
            z = true;
        }
        securityCallback.onSecurityModeChanged(securityMode, z);
    }

    /* renamed from: com.android.keyguard.KeyguardSecurityContainer$1 */
    class AnonymousClass1 implements KeyguardSecurityCallback {
        AnonymousClass1() {
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void userActivity() {
            if (KeyguardSecurityContainer.this.mSecurityCallback != null) {
                KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
            }
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void dismiss(boolean z, int i2) {
            KeyguardSecurityContainer.this.mSecurityCallback.dismiss(z, i2);
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void reportUnlockAttempt(int i2, boolean z, int i3) {
            KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(KeyguardSecurityContainer.this.mContext);
            if (z) {
                StatsLog.write(64, 2);
                keyguardUpdateMonitor.clearFailedUnlockAttempts();
                KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt(i2);
            } else {
                StatsLog.write(64, 1);
                KeyguardSecurityContainer.this.reportFailedUnlockAttempt(i2, i3);
            }
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void reset() {
            KeyguardSecurityContainer.this.mSecurityCallback.reset();
        }
    }

    /* renamed from: com.android.keyguard.KeyguardSecurityContainer$2 */
    class AnonymousClass2 implements KeyguardSecurityCallback {
        AnonymousClass2() {
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void userActivity() {
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void reportUnlockAttempt(int i2, boolean z, int i3) {
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void dismiss(boolean z, int i2) {
        }

        @Override // com.android.keyguard.KeyguardSecurityCallback
        public void reset() {
        }
    }

    private int getSecurityViewIdForMode(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (securityMode) {
            case Pattern:
                return com.android.systemui.R.id.keyguard_pattern_view;
            case PIN:
                return com.android.systemui.R.id.keyguard_pin_view;
            case Password:
                return com.android.systemui.R.id.keyguard_password_view;
            case Invalid:
            case None:
            default:
                return 0;
            case SimPinPukMe1:
            case SimPinPukMe2:
            case SimPinPukMe3:
            case SimPinPukMe4:
                return com.android.systemui.R.id.keyguard_sim_pin_puk_me_view;
            case AntiTheft:
                return AntiTheftManager.getAntiTheftViewId();
            case AlarmBoot:
                return com.android.systemui.R.id.power_off_alarm_view;
        }
    }

    public int getLayoutIdFor(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (securityMode) {
            case Pattern:
                return com.android.systemui.R.layout.keyguard_pattern_view;
            case PIN:
                return com.android.systemui.R.layout.keyguard_pin_view;
            case Password:
                return com.android.systemui.R.layout.keyguard_password_view;
            case Invalid:
            case None:
            default:
                return 0;
            case SimPinPukMe1:
            case SimPinPukMe2:
            case SimPinPukMe3:
            case SimPinPukMe4:
                return com.android.systemui.R.layout.mtk_keyguard_sim_pin_puk_me_view;
            case AntiTheft:
                return AntiTheftManager.getAntiTheftLayoutId();
            case AlarmBoot:
                return com.android.systemui.R.layout.mtk_power_off_alarm_view;
        }
    }

    public KeyguardSecurityModel.SecurityMode getSecurityMode() {
        return this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
    }

    public KeyguardSecurityModel.SecurityMode getCurrentSecurityMode() {
        return this.mCurrentSecuritySelection;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean needsInput() {
        return this.mSecurityViewFlipper.needsInput();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        this.mSecurityViewFlipper.setKeyguardCallback(keyguardSecurityCallback);
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void reset() {
        this.mSecurityViewFlipper.reset();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showPromptReason(int i) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            if (i != 0) {
                Log.i("KeyguardSecurityView", "Strong auth required, reason: " + i);
            }
            getSecurityView(this.mCurrentSecuritySelection).showPromptReason(i);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showMessage(CharSequence charSequence, int i) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).showMessage(charSequence, i);
        }
    }
}
