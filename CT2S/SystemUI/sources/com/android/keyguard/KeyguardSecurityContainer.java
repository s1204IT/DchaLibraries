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

public class KeyguardSecurityContainer extends FrameLayout implements KeyguardSecurityView {
    private KeyguardSecurityCallback mCallback;
    private KeyguardSecurityModel.SecurityMode mCurrentSecuritySelection;
    private boolean mEnableFallback;
    private boolean mIsBouncing;
    private LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityCallback mNullCallback;
    private SecurityCallback mSecurityCallback;
    private KeyguardSecurityModel mSecurityModel;
    private KeyguardSecurityViewFlipper mSecurityViewFlipper;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public interface SecurityCallback {
        boolean dismiss(boolean z);

        void finish();

        void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean z);

        void userActivity();
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
                if (KeyguardSecurityContainer.this.mSecurityCallback != null) {
                    KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
                }
            }

            @Override
            public void dismiss(boolean authenticated) {
                KeyguardSecurityContainer.this.mSecurityCallback.dismiss(authenticated);
            }

            @Override
            public void reportUnlockAttempt(boolean success) {
                KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(KeyguardSecurityContainer.this.mContext);
                if (!success) {
                    if (KeyguardSecurityContainer.this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.Biometric) {
                        KeyguardSecurityContainer.this.reportFailedUnlockAttempt();
                        return;
                    } else {
                        monitor.reportFailedBiometricUnlockAttempt();
                        return;
                    }
                }
                monitor.clearFailedUnlockAttempts();
                KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt();
            }

            @Override
            public void showBackupSecurity() {
                KeyguardSecurityContainer.this.showBackupSecurityScreen();
            }
        };
        this.mNullCallback = new KeyguardSecurityCallback() {
            @Override
            public void userActivity() {
            }

            @Override
            public void showBackupSecurity() {
            }

            @Override
            public void reportUnlockAttempt(boolean success) {
            }

            @Override
            public void dismiss(boolean securityVerified) {
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
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onResume(reason);
        }
    }

    @Override
    public void onPause() {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).onPause();
        }
    }

    @Override
    public void startAppearAnimation() {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            getSecurityView(this.mCurrentSecuritySelection).startAppearAnimation();
        }
    }

    @Override
    public boolean startDisappearAnimation(Runnable onFinishRunnable) {
        if (this.mCurrentSecuritySelection != KeyguardSecurityModel.SecurityMode.None) {
            return getSecurityView(this.mCurrentSecuritySelection).startDisappearAnimation(onFinishRunnable);
        }
        return false;
    }

    void updateSecurityViews(boolean isBouncing) {
        int children = this.mSecurityViewFlipper.getChildCount();
        for (int i = 0; i < children; i++) {
            updateSecurityView(this.mSecurityViewFlipper.getChildAt(i), isBouncing);
        }
    }

    public CharSequence getCurrentSecurityModeContentDescription() {
        View v = (View) getSecurityView(this.mCurrentSecuritySelection);
        return v != null ? v.getContentDescription() : "";
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
            View viewInflate = inflater.inflate(layoutId, (ViewGroup) this.mSecurityViewFlipper, false);
            this.mSecurityViewFlipper.addView(viewInflate);
            updateSecurityView(viewInflate, this.mIsBouncing);
            KeyguardSecurityView view2 = (KeyguardSecurityView) viewInflate;
            return view2;
        }
        return view;
    }

    private void updateSecurityView(View view, boolean isBouncing) {
        this.mIsBouncing = isBouncing;
        if (view instanceof KeyguardSecurityView) {
            KeyguardSecurityView ksv = (KeyguardSecurityView) view;
            ksv.setKeyguardCallback(this.mCallback);
            ksv.setLockPatternUtils(this.mLockPatternUtils);
            if (isBouncing) {
                ksv.showBouncer(0);
                return;
            } else {
                ksv.hideBouncer(0);
                return;
            }
        }
        Log.w("KeyguardSecurityView", "View " + view + " is not a KeyguardSecurityView");
    }

    @Override
    protected void onFinishInflate() {
        this.mSecurityViewFlipper = (KeyguardSecurityViewFlipper) findViewById(R.id.view_flipper);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mSecurityModel.setLockPatternUtils(utils);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    private void showDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(title).setMessage(message).setNeutralButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
        if (!(this.mContext instanceof Activity)) {
            dialog.getWindow().setType(2009);
        }
        dialog.show();
    }

    private void showTimeoutDialog() {
        int messageId = 0;
        switch (this.mSecurityModel.getSecurityMode()) {
            case Pattern:
                messageId = R.string.kg_too_many_failed_pattern_attempts_dialog_message;
                break;
            case PIN:
                messageId = R.string.kg_too_many_failed_pin_attempts_dialog_message;
                break;
            case Password:
                messageId = R.string.kg_too_many_failed_password_attempts_dialog_message;
                break;
        }
        if (messageId != 0) {
            String message = this.mContext.getString(messageId, Integer.valueOf(KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts()), 30);
            showDialog(null, message);
        }
    }

    private void showAlmostAtWipeDialog(int attempts, int remaining, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_wipe, Integer.valueOf(attempts), Integer.valueOf(remaining));
                break;
            case 2:
                message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_erase_profile, Integer.valueOf(attempts), Integer.valueOf(remaining));
                break;
            case 3:
                message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_erase_user, Integer.valueOf(attempts), Integer.valueOf(remaining));
                break;
        }
        showDialog(null, message);
    }

    private void showWipeDialog(int attempts, int userType) {
        String message = null;
        switch (userType) {
            case 1:
                message = this.mContext.getString(R.string.kg_failed_attempts_now_wiping, Integer.valueOf(attempts));
                break;
            case 2:
                message = this.mContext.getString(R.string.kg_failed_attempts_now_erasing_profile, Integer.valueOf(attempts));
                break;
            case 3:
                message = this.mContext.getString(R.string.kg_failed_attempts_now_erasing_user, Integer.valueOf(attempts));
                break;
        }
        showDialog(null, message);
    }

    private void showAlmostAtAccountLoginDialog() {
        String message = this.mContext.getString(R.string.kg_failed_attempts_almost_at_login, 15, 5, 30);
        showDialog(null, message);
    }

    private void reportFailedUnlockAttempt() {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int failedAttempts = monitor.getFailedUnlockAttempts() + 1;
        KeyguardSecurityModel.SecurityMode mode = this.mSecurityModel.getSecurityMode();
        boolean usingPattern = mode == KeyguardSecurityModel.SecurityMode.Pattern;
        int currentUser = this.mLockPatternUtils.getCurrentUser();
        DevicePolicyManager dpm = this.mLockPatternUtils.getDevicePolicyManager();
        int failedAttemptsBeforeWipe = dpm.getMaximumFailedPasswordsForWipe(null, currentUser);
        int remainingBeforeWipe = failedAttemptsBeforeWipe > 0 ? failedAttemptsBeforeWipe - failedAttempts : Integer.MAX_VALUE;
        boolean showTimeout = false;
        if (remainingBeforeWipe < 5) {
            int expiringUser = dpm.getProfileWithMinimumFailedPasswordsForWipe(currentUser);
            int userType = 1;
            if (expiringUser == currentUser) {
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
        } else {
            showTimeout = failedAttempts % 5 == 0;
            if (usingPattern && this.mEnableFallback) {
                if (failedAttempts == 15) {
                    showAlmostAtAccountLoginDialog();
                    showTimeout = false;
                } else if (failedAttempts >= 20) {
                    this.mLockPatternUtils.setPermanentlyLocked(true);
                    showSecurityScreen(KeyguardSecurityModel.SecurityMode.Account);
                    showTimeout = false;
                }
            }
        }
        monitor.reportFailedUnlockAttempt();
        this.mLockPatternUtils.reportFailedPasswordAttempt();
        if (showTimeout) {
            showTimeoutDialog();
        }
    }

    void showPrimarySecurityScreen(boolean turningOff) {
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode();
        if (!turningOff && KeyguardUpdateMonitor.getInstance(this.mContext).isAlternateUnlockEnabled()) {
            securityMode = this.mSecurityModel.getAlternateFor(securityMode);
        }
        showSecurityScreen(securityMode);
    }

    private void showBackupSecurityScreen() {
        KeyguardSecurityModel.SecurityMode backup = this.mSecurityModel.getBackupSecurityMode(this.mCurrentSecuritySelection);
        showSecurityScreen(backup);
    }

    boolean showNextSecurityScreenOrFinish(boolean authenticated) {
        boolean finish = false;
        if (this.mUpdateMonitor.getUserHasTrust(this.mLockPatternUtils.getCurrentUser())) {
            finish = true;
        } else if (KeyguardSecurityModel.SecurityMode.None == this.mCurrentSecuritySelection) {
            KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getAlternateFor(this.mSecurityModel.getSecurityMode());
            if (KeyguardSecurityModel.SecurityMode.None == securityMode) {
                finish = true;
            } else {
                showSecurityScreen(securityMode);
            }
        } else if (authenticated) {
            switch (this.mCurrentSecuritySelection) {
                case Pattern:
                case PIN:
                case Password:
                case Account:
                case Biometric:
                    finish = true;
                    break;
                case Invalid:
                case None:
                default:
                    Log.v("KeyguardSecurityView", "Bad security screen " + this.mCurrentSecuritySelection + ", fail safe");
                    showPrimarySecurityScreen(false);
                    break;
                case SimPin:
                case SimPuk:
                    KeyguardSecurityModel.SecurityMode securityMode2 = this.mSecurityModel.getSecurityMode();
                    if (securityMode2 != KeyguardSecurityModel.SecurityMode.None) {
                        showSecurityScreen(securityMode2);
                    } else {
                        finish = true;
                    }
                    break;
            }
        }
        if (finish) {
            this.mSecurityCallback.finish();
        }
        return finish;
    }

    private void showSecurityScreen(KeyguardSecurityModel.SecurityMode securityMode) {
        if (securityMode != this.mCurrentSecuritySelection) {
            KeyguardSecurityView oldView = getSecurityView(this.mCurrentSecuritySelection);
            KeyguardSecurityView newView = getSecurityView(securityMode);
            if (oldView != null) {
                oldView.onPause();
                oldView.setKeyguardCallback(this.mNullCallback);
            }
            if (securityMode != KeyguardSecurityModel.SecurityMode.None) {
                newView.onResume(2);
                newView.setKeyguardCallback(this.mCallback);
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
            this.mCurrentSecuritySelection = securityMode;
            this.mSecurityCallback.onSecurityModeChanged(securityMode, securityMode != KeyguardSecurityModel.SecurityMode.None && newView.needsInput());
        }
    }

    private KeyguardSecurityViewFlipper getFlipper() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof KeyguardSecurityViewFlipper) {
                return (KeyguardSecurityViewFlipper) child;
            }
        }
        return null;
    }

    @Override
    public void showBouncer(int duration) {
        KeyguardSecurityViewFlipper flipper = getFlipper();
        if (flipper != null) {
            flipper.showBouncer(duration);
        }
    }

    @Override
    public void hideBouncer(int duration) {
        KeyguardSecurityViewFlipper flipper = getFlipper();
        if (flipper != null) {
            flipper.hideBouncer(duration);
        }
    }

    private int getSecurityViewIdForMode(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (securityMode) {
            case Pattern:
                return R.id.keyguard_pattern_view;
            case PIN:
                return R.id.keyguard_pin_view;
            case Password:
                return R.id.keyguard_password_view;
            case Account:
                return R.id.keyguard_account_view;
            case Biometric:
                return R.id.keyguard_face_unlock_view;
            case Invalid:
            case None:
            default:
                return 0;
            case SimPin:
                return R.id.keyguard_sim_pin_view;
            case SimPuk:
                return R.id.keyguard_sim_puk_view;
        }
    }

    private int getLayoutIdFor(KeyguardSecurityModel.SecurityMode securityMode) {
        switch (securityMode) {
            case Pattern:
                return R.layout.keyguard_pattern_view;
            case PIN:
                return R.layout.keyguard_pin_view;
            case Password:
                return R.layout.keyguard_password_view;
            case Account:
                return R.layout.keyguard_account_view;
            case Biometric:
                return R.layout.keyguard_face_unlock_view;
            case Invalid:
            case None:
            default:
                return 0;
            case SimPin:
                return R.layout.keyguard_sim_pin_view;
            case SimPuk:
                return R.layout.keyguard_sim_puk_view;
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
    public void showUsabilityHint() {
        this.mSecurityViewFlipper.showUsabilityHint();
    }
}
