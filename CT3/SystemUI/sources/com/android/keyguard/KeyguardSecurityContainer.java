package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.Telephony.KeyguardSimPinPukMeView;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;

public class KeyguardSecurityContainer extends FrameLayout implements KeyguardSecurityView {

    private static final int[] f3x1cbe7e58 = null;
    private KeyguardSecurityCallback mCallback;
    private KeyguardSecurityModel.SecurityMode mCurrentSecuritySelection;
    private LockPatternUtils mLockPatternUtils;
    private ViewGroup mNotificatonPanelView;
    private KeyguardSecurityCallback mNullCallback;
    private SecurityCallback mSecurityCallback;
    private KeyguardSecurityModel mSecurityModel;
    private KeyguardSecurityViewFlipper mSecurityViewFlipper;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public interface SecurityCallback {
        boolean dismiss(boolean z);

        void finish(boolean z);

        void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean z);

        void reset();

        void updateNavbarStatus();

        void userActivity();
    }

    private static int[] m462xec5d63fc() {
        if (f3x1cbe7e58 != null) {
            return f3x1cbe7e58;
        }
        int[] iArr = new int[KeyguardSecurityModel.SecurityMode.valuesCustom().length];
        try {
            iArr[KeyguardSecurityModel.SecurityMode.AlarmBoot.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.AntiTheft.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Biometric.ordinal()] = 13;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Invalid.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.None.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.PIN.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Password.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Pattern.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe1.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe2.ordinal()] = 9;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe3.ordinal()] = 10;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe4.ordinal()] = 11;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Voice.ordinal()] = 12;
        } catch (NoSuchFieldError e13) {
        }
        f3x1cbe7e58 = iArr;
        return iArr;
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardSecurityContainer(Context context) {
        this(context, null, 0);
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCurrentSecuritySelection = KeyguardSecurityModel.SecurityMode.Invalid;
        this.mCallback = new KeyguardSecurityCallback() {
            @Override
            public void userActivity() {
                if (KeyguardSecurityContainer.this.mSecurityCallback == null) {
                    return;
                }
                KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
            }

            @Override
            public void dismiss(boolean authenticated) {
                KeyguardSecurityContainer.this.mSecurityCallback.dismiss(authenticated);
            }

            @Override
            public void reportUnlockAttempt(int userId, boolean success, int timeoutMs) {
                KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(KeyguardSecurityContainer.this.mContext);
                if (success) {
                    monitor.clearFailedUnlockAttempts();
                    KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt(userId);
                } else if (KeyguardSecurityContainer.this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.Biometric || KeyguardSecurityContainer.this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.Voice) {
                    monitor.reportFailedBiometricUnlockAttempt();
                } else {
                    KeyguardSecurityContainer.this.reportFailedUnlockAttempt(userId, timeoutMs);
                }
            }

            @Override
            public void reset() {
                KeyguardSecurityContainer.this.mSecurityCallback.reset();
            }
        };
        this.mNullCallback = new KeyguardSecurityCallback() {
            @Override
            public void userActivity() {
            }

            @Override
            public void reportUnlockAttempt(int userId, boolean success, int timeoutMs) {
            }

            @Override
            public void dismiss(boolean securityVerified) {
            }

            @Override
            public void reset() {
            }
        };
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    public void setSecurityCallback(SecurityCallback callback) {
        this.mSecurityCallback = callback;
    }

    @Override
    public void onResume(int reason) {
        Log.d("KeyguardSecurityView", "onResume(reason = " + reason + ")");
        Log.d("KeyguardSecurityView", "onResume(mCurrentSecuritySelection = " + this.mCurrentSecuritySelection + ")");
        if (this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.None) {
            return;
        }
        getSecurityView(this.mCurrentSecuritySelection).onResume(reason);
    }

    @Override
    public void onPause() {
        Log.d("KeyguardSecurityView", "onPause()");
        if (this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.None) {
            return;
        }
        getSecurityView(this.mCurrentSecuritySelection).onPause();
    }

    @Override
    public void startAppearAnimation() {
        if (this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.None) {
            return;
        }
        getSecurityView(this.mCurrentSecuritySelection).startAppearAnimation();
    }

    @Override
    public boolean startDisappearAnimation(Runnable onFinishRunnable) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            return getSecurityView(this.mCurrentSecuritySelection).startDisappearAnimation(onFinishRunnable);
        }
        return false;
    }

    public CharSequence getCurrentSecurityModeContentDescription() {
        View v = (View) getSecurityView(this.mCurrentSecuritySelection);
        if (v != null) {
            return v.getContentDescription();
        }
        return "";
    }

    private KeyguardSecurityView getSecurityView(KeyguardSecurityModel.SecurityMode securityMode) {
        int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
        KeyguardSecurityView view = null;
        int children = this.mSecurityViewFlipper.getChildCount();
        int child = 0;
        while (true) {
            if (child >= children) {
                break;
            }
            if (this.mSecurityViewFlipper.getChildAt(child).getId() != securityViewIdForMode) {
                child++;
            } else {
                view = (KeyguardSecurityView) this.mSecurityViewFlipper.getChildAt(child);
                break;
            }
        }
        int layoutId = getLayoutIdFor(securityMode);
        if (view == null && layoutId != 0) {
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            Log.v("KeyguardSecurityView", "inflating id = " + layoutId);
            View viewInflate = inflater.inflate(layoutId, (ViewGroup) this.mSecurityViewFlipper, false);
            view = (KeyguardSecurityView) viewInflate;
            if (view instanceof KeyguardSimPinPukMeView) {
                KeyguardSimPinPukMeView pinPukView = (KeyguardSimPinPukMeView) view;
                int phoneId = this.mSecurityModel.getPhoneIdUsingSecurityMode(securityMode);
                pinPukView.setPhoneId(phoneId);
            }
            this.mSecurityViewFlipper.addView(viewInflate);
            updateSecurityView(viewInflate);
        } else if (view != null && (view instanceof KeyguardSimPinPukMeView) && securityMode != this.mCurrentSecuritySelection) {
            Log.i("KeyguardSecurityView", "getSecurityView, here, we will refresh the layout");
            KeyguardSimPinPukMeView pinPukView2 = (KeyguardSimPinPukMeView) view;
            int phoneId2 = this.mSecurityModel.getPhoneIdUsingSecurityMode(securityMode);
            pinPukView2.setPhoneId(phoneId2);
        }
        return view;
    }

    private void updateSecurityView(View view) {
        if (view instanceof KeyguardSecurityView) {
            KeyguardSecurityView ksv = (KeyguardSecurityView) view;
            ksv.setKeyguardCallback(this.mCallback);
            ksv.setLockPatternUtils(this.mLockPatternUtils);
            return;
        }
        Log.w("KeyguardSecurityView", "View " + view + " is not a KeyguardSecurityView");
    }

    @Override
    protected void onFinishInflate() {
        this.mSecurityViewFlipper = (KeyguardSecurityViewFlipper) findViewById(R$id.view_flipper);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mSecurityModel.setLockPatternUtils(utils);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    private void showDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(title).setMessage(message).setNeutralButton(R$string.ok, (DialogInterface.OnClickListener) null).create();
        if (!(this.mContext instanceof Activity)) {
            dialog.getWindow().setType(2009);
        }
        dialog.show();
    }

    private void showTimeoutDialog(int timeoutMs) {
        int timeoutInSeconds = timeoutMs / 1000;
        int messageId = 0;
        switch (m462xec5d63fc()[this.mSecurityModel.getSecurityMode().ordinal()]) {
            case 5:
                messageId = R$string.kg_too_many_failed_pin_attempts_dialog_message;
                break;
            case 6:
                messageId = R$string.kg_too_many_failed_password_attempts_dialog_message;
                break;
            case 7:
                messageId = R$string.kg_too_many_failed_pattern_attempts_dialog_message;
                break;
        }
        if (messageId == 0) {
            return;
        }
        String message = this.mContext.getString(messageId, Integer.valueOf(KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts(KeyguardUpdateMonitor.getCurrentUser())), Integer.valueOf(timeoutInSeconds));
        showDialog(null, message);
    }

    private void showAlmostAtWipeDialog(int attempts, int remaining, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R$string.kg_failed_attempts_almost_at_wipe, Integer.valueOf(attempts), Integer.valueOf(remaining));
                break;
            case 2:
                message = this.mContext.getString(R$string.kg_failed_attempts_almost_at_erase_profile, Integer.valueOf(attempts), Integer.valueOf(remaining));
                break;
            case 3:
                message = this.mContext.getString(R$string.kg_failed_attempts_almost_at_erase_user, Integer.valueOf(attempts), Integer.valueOf(remaining));
                break;
        }
        showDialog(null, message);
    }

    private void showWipeDialog(int attempts, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R$string.kg_failed_attempts_now_wiping, Integer.valueOf(attempts));
                break;
            case 2:
                message = this.mContext.getString(R$string.kg_failed_attempts_now_erasing_profile, Integer.valueOf(attempts));
                break;
            case 3:
                message = this.mContext.getString(R$string.kg_failed_attempts_now_erasing_user, Integer.valueOf(attempts));
                break;
        }
        showDialog(null, message);
    }

    public void reportFailedUnlockAttempt(int userId, int timeoutMs) {
        int remainingBeforeWipe;
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int failedAttempts = monitor.getFailedUnlockAttempts(userId) + 1;
        Log.d("KeyguardSecurityView", "reportFailedPatternAttempt: #" + failedAttempts);
        DevicePolicyManager dpm = this.mLockPatternUtils.getDevicePolicyManager();
        int failedAttemptsBeforeWipe = dpm.getMaximumFailedPasswordsForWipe(null, userId);
        if (failedAttemptsBeforeWipe > 0) {
            remainingBeforeWipe = failedAttemptsBeforeWipe - failedAttempts;
        } else {
            remainingBeforeWipe = Integer.MAX_VALUE;
        }
        if (remainingBeforeWipe < 5) {
            int expiringUser = dpm.getProfileWithMinimumFailedPasswordsForWipe(userId);
            int userType = 1;
            if (expiringUser == userId) {
                if (expiringUser != 0) {
                    userType = 3;
                }
            } else if (expiringUser != -10000) {
                userType = 2;
            }
            if (remainingBeforeWipe > 0) {
                showAlmostAtWipeDialog(failedAttempts, remainingBeforeWipe, userType);
            } else {
                Slog.i("KeyguardSecurityView", "Too many unlock attempts; user " + expiringUser + " will be wiped!");
                showWipeDialog(failedAttempts, userType);
            }
        }
        monitor.reportFailedStrongAuthUnlockAttempt(userId);
        this.mLockPatternUtils.reportFailedPasswordAttempt(userId);
        if (timeoutMs <= 0) {
            return;
        }
        Log.d("KeyguardSecurityView", "timeoutMs " + timeoutMs);
        showTimeoutDialog(timeoutMs);
    }

    void showPrimarySecurityScreen(boolean turningOff) {
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode();
        Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(turningOff=" + turningOff + ")");
        Log.v("KeyguardSecurityView", "showPrimarySecurityScreen(securityMode=" + securityMode + ")");
        if (this.mSecurityModel.isSimPinPukSecurityMode(this.mCurrentSecuritySelection)) {
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - current is " + this.mCurrentSecuritySelection);
            int phoneId = this.mSecurityModel.getPhoneIdUsingSecurityMode(this.mCurrentSecuritySelection);
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - phoneId of currentView is " + phoneId);
            boolean isCurrentModeSimPinSecure = this.mUpdateMonitor.isSimPinSecure(phoneId);
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - isCurrentModeSimPinSecure = " + isCurrentModeSimPinSecure);
            if (isCurrentModeSimPinSecure) {
                Log.d("KeyguardSecurityView", "Skip show security because it already shows SimPinPukMeView");
                return;
            }
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - since current simpinview not secured, we should call showSecurityScreen() to set correct PhoneId for next view.");
        }
        if (!turningOff && KeyguardUpdateMonitor.getInstance(this.mContext).isAlternateUnlockEnabled()) {
            Log.d("KeyguardSecurityView", "showPrimarySecurityScreen() - will be call getAlternateFor");
            securityMode = this.mSecurityModel.getAlternateFor(securityMode);
        }
        showSecurityScreen(securityMode);
    }

    boolean showNextSecurityScreenOrFinish(boolean authenticated) {
        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish(" + authenticated + ")");
        Log.d("KeyguardSecurityView", "showNext.. mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
        boolean finish = false;
        boolean strongAuth = false;
        if (this.mUpdateMonitor.getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser())) {
            finish = true;
        } else if (KeyguardSecurityModel.SecurityMode.None == this.mCurrentSecuritySelection) {
            KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode();
            if (KeyguardSecurityModel.SecurityMode.None == securityMode) {
                Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - securityMode is None, just finish.");
                finish = true;
            } else {
                Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish()- switch to the alternate security view for None mode.");
                showSecurityScreen(securityMode);
            }
        } else if (authenticated) {
            Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - authenticated is True, and mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
            KeyguardSecurityModel.SecurityMode securityMode2 = this.mSecurityModel.getSecurityMode();
            Log.v("KeyguardSecurityView", "securityMode = " + securityMode2);
            Log.d("KeyguardSecurityView", "mCurrentSecuritySelection: " + this.mCurrentSecuritySelection);
            switch (m462xec5d63fc()[this.mCurrentSecuritySelection.ordinal()]) {
                case 2:
                    KeyguardSecurityModel.SecurityMode nextMode = this.mSecurityModel.getSecurityMode();
                    Log.v("KeyguardSecurityView", "now is Antitheft, next securityMode = " + nextMode);
                    if (nextMode != KeyguardSecurityModel.SecurityMode.None) {
                        showSecurityScreen(nextMode);
                    } else {
                        finish = true;
                    }
                    break;
                case 3:
                case 4:
                default:
                    Log.v("KeyguardSecurityView", "Bad security screen " + this.mCurrentSecuritySelection + ", fail safe");
                    showPrimarySecurityScreen(false);
                    break;
                case 5:
                case 6:
                case 7:
                    strongAuth = true;
                    finish = true;
                    break;
                case 8:
                case 9:
                case 10:
                case 11:
                    if (securityMode2 != KeyguardSecurityModel.SecurityMode.None) {
                        showSecurityScreen(securityMode2);
                    } else {
                        finish = true;
                    }
                    break;
                case 12:
                    if (this.mSecurityModel.isSimPinPukSecurityMode(securityMode2)) {
                        showSecurityScreen(securityMode2);
                    } else {
                        finish = true;
                    }
                    break;
            }
        }
        this.mSecurityCallback.updateNavbarStatus();
        if (finish) {
            this.mSecurityCallback.finish(strongAuth);
            Log.d("KeyguardSecurityView", "finish ");
        }
        Log.d("KeyguardSecurityView", "showNextSecurityScreenOrFinish() - return finish = " + finish);
        return finish;
    }

    private void showSecurityScreen(KeyguardSecurityModel.SecurityMode securityMode) {
        Log.d("KeyguardSecurityView", "showSecurityScreen(" + securityMode + ")");
        if (securityMode == this.mCurrentSecuritySelection && securityMode != KeyguardSecurityModel.SecurityMode.AntiTheft) {
            return;
        }
        VoiceWakeupManager.getInstance().notifySecurityModeChange(this.mCurrentSecuritySelection, securityMode);
        Log.d("KeyguardSecurityView", "showSecurityScreen() - get oldview for" + this.mCurrentSecuritySelection);
        KeyguardSecurityView oldView = getSecurityView(this.mCurrentSecuritySelection);
        Log.d("KeyguardSecurityView", "showSecurityScreen() - get newview for" + securityMode);
        KeyguardSecurityView newView = getSecurityView(securityMode);
        if (oldView != null) {
            oldView.onPause();
            Log.d("KeyguardSecurityView", "showSecurityScreen() - oldview.setKeyguardCallback(mNullCallback)");
            oldView.setKeyguardCallback(this.mNullCallback);
        }
        if (securityMode != KeyguardSecurityModel.SecurityMode.None) {
            newView.setKeyguardCallback(this.mCallback);
            Log.d("KeyguardSecurityView", "showSecurityScreen() - newview.setKeyguardCallback(mCallback)");
            newView.onResume(2);
        }
        int childCount = this.mSecurityViewFlipper.getChildCount();
        int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
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
        Log.d("KeyguardSecurityView", "Before update, mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
        this.mCurrentSecuritySelection = securityMode;
        Log.d("KeyguardSecurityView", "After update, mCurrentSecuritySelection = " + this.mCurrentSecuritySelection);
        this.mSecurityCallback.onSecurityModeChanged(securityMode, securityMode != KeyguardSecurityModel.SecurityMode.None ? newView.needsInput() : false);
    }

    private int getSecurityViewIdForMode(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (m462xec5d63fc()[securityMode.ordinal()]) {
            case 1:
                return R$id.power_off_alarm_view;
            case 2:
                return AntiTheftManager.getAntiTheftViewId();
            case 3:
            case 4:
            default:
                return 0;
            case 5:
                return R$id.keyguard_pin_view;
            case 6:
                return R$id.keyguard_password_view;
            case 7:
                return R$id.keyguard_pattern_view;
            case 8:
            case 9:
            case 10:
            case 11:
                return R$id.keyguard_sim_pin_puk_me_view;
        }
    }

    protected int getLayoutIdFor(KeyguardSecurityModel.SecurityMode securityMode) {
        Log.d("KeyguardSecurityView", "getLayoutIdFor, SecurityMode-->" + securityMode);
        switch (m462xec5d63fc()[securityMode.ordinal()]) {
            case 1:
                return R$layout.mtk_power_off_alarm_view;
            case 2:
                return AntiTheftManager.getAntiTheftLayoutId();
            case 3:
            case 4:
            default:
                return 0;
            case 5:
                return R$layout.keyguard_pin_view;
            case 6:
                return R$layout.keyguard_password_view;
            case 7:
                return R$layout.keyguard_pattern_view;
            case 8:
            case 9:
            case 10:
            case 11:
                return R$layout.mtk_keyguard_sim_pin_puk_me_view;
        }
    }

    public KeyguardSecurityModel.SecurityMode getSecurityMode() {
        return this.mSecurityModel.getSecurityMode();
    }

    public KeyguardSecurityModel.SecurityMode getCurrentSecurityMode() {
        return this.mCurrentSecuritySelection;
    }

    @Override
    public boolean needsInput() {
        return this.mSecurityViewFlipper.needsInput();
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mSecurityViewFlipper.setKeyguardCallback(callback);
    }

    @Override
    public void showPromptReason(int reason) {
        if (this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.None) {
            return;
        }
        if (reason != 0) {
            Log.i("KeyguardSecurityView", "Strong auth required, reason: " + reason);
        }
        getSecurityView(this.mCurrentSecuritySelection).showPromptReason(reason);
    }

    @Override
    public void showMessage(String message, int color) {
        if (this.mCurrentSecuritySelection == KeyguardSecurityModel.SecurityMode.None) {
            return;
        }
        getSecurityView(this.mCurrentSecuritySelection).showMessage(message, color);
    }

    public void setNotificationPanelView(ViewGroup notificationPanelView) {
        this.mNotificatonPanelView = notificationPanelView;
    }

    public void onScreenTurnedOff() {
        Log.d("KeyguardSecurityView", "onScreenTurnedOff");
    }

    void setSecurityMode(KeyguardSecurityModel securityMode) {
        this.mSecurityModel = securityMode;
    }

    void setCurrentSecurityMode(KeyguardSecurityModel.SecurityMode currentSecuritySelection) {
        this.mCurrentSecuritySelection = currentSecuritySelection;
    }
}
