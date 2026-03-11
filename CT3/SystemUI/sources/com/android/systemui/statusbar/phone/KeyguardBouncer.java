package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.Context;
import android.os.UserManager;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;

public class KeyguardBouncer {
    private int mBouncerPromptReason;
    private ViewMediatorCallback mCallback;
    private ViewGroup mContainer;
    private Context mContext;
    private FalsingManager mFalsingManager;
    protected KeyguardHostView mKeyguardView;
    private LockPatternUtils mLockPatternUtils;
    private ViewGroup mNotificationPanel;
    protected ViewGroup mRoot;
    private KeyguardSecurityModel mSecurityModel;
    private boolean mShowingSoon;
    private StatusBarWindowManager mWindowManager;
    private final String TAG = "KeyguardBouncer";
    private final boolean DEBUG = true;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onStrongAuthStateChanged(int userId) {
            KeyguardBouncer.this.mBouncerPromptReason = KeyguardBouncer.this.mCallback.getBouncerPromptReason();
        }
    };
    private final Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("KeyguardBouncer", "mShowRunnable.run() is called.");
            KeyguardBouncer.this.mRoot.setVisibility(0);
            KeyguardBouncer.this.mKeyguardView.onResume();
            KeyguardBouncer.this.showPromptReason(KeyguardBouncer.this.mBouncerPromptReason);
            if (KeyguardBouncer.this.mKeyguardView.getHeight() != 0) {
                KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
            } else {
                KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().removeOnPreDrawListener(this);
                        KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
                        return true;
                    }
                });
                KeyguardBouncer.this.mKeyguardView.requestLayout();
            }
            KeyguardBouncer.this.mShowingSoon = false;
            KeyguardBouncer.this.mKeyguardView.sendAccessibilityEvent(32);
        }
    };

    public KeyguardBouncer(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils, StatusBarWindowManager windowManager, ViewGroup container) {
        this.mContext = context;
        this.mCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mContainer = container;
        this.mWindowManager = windowManager;
        this.mNotificationPanel = (ViewGroup) this.mContainer.findViewById(R.id.notification_panel);
        this.mSecurityModel = new KeyguardSecurityModel(this.mContext);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
    }

    public void show(boolean resetSecuritySelection) {
        int keyguardUserId = KeyguardUpdateMonitor.getCurrentUser();
        if (keyguardUserId == 0 && UserManager.isSplitSystemUser()) {
            return;
        }
        this.mFalsingManager.onBouncerShown();
        ensureView();
        show(resetSecuritySelection, false);
    }

    public void show(boolean resetSecuritySelection, boolean authenticated) {
        boolean allowDismissKeyguard = false;
        Log.d("KeyguardBouncer", "show(resetSecuritySelection = " + resetSecuritySelection);
        if (PowerOffAlarmManager.isAlarmBoot()) {
            Log.d("KeyguardBouncer", "show() - this is alarm boot, just re-inflate.");
            if (this.mKeyguardView != null && this.mRoot != null) {
                Log.d("KeyguardBouncer", "show() - before re-inflate, we should pause current view.");
                this.mKeyguardView.onPause();
            }
            inflateView();
        } else {
            ensureView();
        }
        if (resetSecuritySelection) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        if (this.mRoot.getVisibility() == 0 || this.mShowingSoon) {
            return;
        }
        int activeUserId = ActivityManager.getCurrentUser();
        int keyguardUserId = KeyguardUpdateMonitor.getCurrentUser();
        if (!(UserManager.isSplitSystemUser() && activeUserId == 0) && activeUserId == keyguardUserId) {
            allowDismissKeyguard = true;
        }
        if (allowDismissKeyguard && this.mKeyguardView.dismiss()) {
            return;
        }
        if (!allowDismissKeyguard) {
            Slog.w("KeyguardBouncer", "User can't dismiss keyguard: " + activeUserId + " != " + keyguardUserId);
        }
        if (this.mKeyguardView.dismiss(authenticated)) {
            return;
        }
        Log.d("KeyguardBouncer", "show() - try to dismiss \"Bouncer\" directly.");
        this.mShowingSoon = true;
        DejankUtils.postAfterTraversal(this.mShowRunnable);
    }

    public void showPromptReason(int reason) {
        this.mKeyguardView.showPromptReason(reason);
    }

    public void showMessage(String message, int color) {
        this.mKeyguardView.showMessage(message, color);
    }

    private void cancelShowRunnable() {
        DejankUtils.removeCallbacks(this.mShowRunnable);
        this.mShowingSoon = false;
    }

    public void showWithDismissAction(KeyguardHostView.OnDismissAction r) {
        ensureView();
        this.mKeyguardView.setOnDismissAction(r);
        show(false);
    }

    public void hide(boolean destroyView) {
        this.mFalsingManager.onBouncerHidden();
        Log.d("KeyguardBouncer", "hide() is called, destroyView = " + destroyView);
        cancelShowRunnable();
        if (this.mKeyguardView != null) {
            this.mKeyguardView.cancelDismissAction();
            this.mKeyguardView.cleanUp();
        }
        if (destroyView) {
            Log.d("KeyguardBouncer", "call removeView()");
            removeView();
        } else if (this.mRoot != null) {
            Log.d("KeyguardBouncer", "just set keyguard Invisible.");
            this.mRoot.setVisibility(4);
        }
        Log.d("KeyguardBouncer", "hide() - user has left keyguard, setAlternateUnlockEnabled(true)");
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(true);
    }

    public void startPreHideAnimation(Runnable runnable) {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.startDisappearAnimation(runnable);
        } else {
            if (runnable == null) {
                return;
            }
            runnable.run();
        }
    }

    public void onScreenTurnedOff() {
        if (this.mKeyguardView == null || this.mRoot == null || this.mRoot.getVisibility() != 0) {
            return;
        }
        this.mKeyguardView.onScreenTurnedOff();
        this.mKeyguardView.onPause();
    }

    public boolean isShowing() {
        if (this.mShowingSoon) {
            return true;
        }
        return this.mRoot != null && this.mRoot.getVisibility() == 0;
    }

    public void prepare() {
        boolean wasInitialized = this.mRoot != null;
        ensureView();
        if (wasInitialized) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        this.mBouncerPromptReason = this.mCallback.getBouncerPromptReason();
    }

    protected void ensureView() {
        if (this.mRoot != null) {
            return;
        }
        inflateView();
    }

    protected void inflateView() {
        Log.d("KeyguardBouncer", "inflateView() is called, we force to re-inflate the \"Bouncer\" view.");
        removeView();
        this.mRoot = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_bouncer, (ViewGroup) null);
        this.mKeyguardView = (KeyguardHostView) this.mRoot.findViewById(R.id.keyguard_host_view);
        this.mKeyguardView.setLockPatternUtils(this.mLockPatternUtils);
        this.mKeyguardView.setViewMediatorCallback(this.mCallback);
        this.mKeyguardView.setNotificationPanelView(this.mNotificationPanel);
        this.mContainer.addView(this.mRoot, this.mContainer.getChildCount());
        this.mRoot.setVisibility(4);
        this.mRoot.setSystemUiVisibility(2097152);
    }

    protected void removeView() {
        if (this.mRoot == null || this.mRoot.getParent() != this.mContainer) {
            return;
        }
        Log.d("KeyguardBouncer", "removeView() - really remove all views.");
        this.mContainer.removeView(this.mRoot);
        this.mRoot = null;
    }

    public boolean needsFullscreenBouncer() {
        ensureView();
        KeyguardSecurityModel.SecurityMode mode = this.mSecurityModel.getSecurityMode();
        return mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe1 || mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe2 || mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe3 || mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe4 || mode == KeyguardSecurityModel.SecurityMode.AntiTheft || mode == KeyguardSecurityModel.SecurityMode.AlarmBoot;
    }

    public boolean isFullscreenBouncer() {
        if (this.mKeyguardView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode mode = this.mKeyguardView.getCurrentSecurityMode();
        return mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe1 || mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe2 || mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe3 || mode == KeyguardSecurityModel.SecurityMode.SimPinPukMe4 || mode == KeyguardSecurityModel.SecurityMode.AntiTheft || mode == KeyguardSecurityModel.SecurityMode.AlarmBoot;
    }

    public boolean isSecure() {
        return this.mKeyguardView == null || this.mKeyguardView.getSecurityMode() != KeyguardSecurityModel.SecurityMode.None;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mKeyguardView.shouldEnableMenuKey();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        ensureView();
        return this.mKeyguardView.interceptMediaKey(event);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        ensureView();
        this.mKeyguardView.finish(strongAuth);
    }
}
