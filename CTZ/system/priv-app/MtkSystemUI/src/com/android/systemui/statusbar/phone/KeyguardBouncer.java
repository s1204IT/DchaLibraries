package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import android.os.UserManager;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.StatsLog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public class KeyguardBouncer {
    private int mBouncerPromptReason;
    protected final ViewMediatorCallback mCallback;
    protected final ViewGroup mContainer;
    protected final Context mContext;
    private final DismissCallbackRegistry mDismissCallbackRegistry;
    private final BouncerExpansionCallback mExpansionCallback;
    private final FalsingManager mFalsingManager;
    private final Handler mHandler;
    private boolean mIsAnimatingAway;
    private boolean mIsScrimmed;
    protected KeyguardHostView mKeyguardView;
    protected final LockPatternUtils mLockPatternUtils;
    protected ViewGroup mRoot;
    public KeyguardSecurityModel mSecurityModel;
    private boolean mShowingSoon;
    private int mStatusBarHeight;
    private final boolean DEBUG = true;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() { // from class: com.android.systemui.statusbar.phone.KeyguardBouncer.1
        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStrongAuthStateChanged(int i) {
            KeyguardBouncer.this.mBouncerPromptReason = KeyguardBouncer.this.mCallback.getBouncerPromptReason();
        }
    };
    private final Runnable mRemoveViewRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$iQsniWdIxLGqyYwRi09kQ-Ah02M
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.removeView();
        }
    };
    private final Runnable mResetRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$KeyguardBouncer$Y9Hvfk0n3yPK2FQ39O1Z5j49gj0
        @Override // java.lang.Runnable
        public final void run() {
            KeyguardBouncer.lambda$new$0(this.f$0);
        }
    };
    private float mExpansion = 1.0f;
    private final Runnable mShowRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.KeyguardBouncer.2
        @Override // java.lang.Runnable
        public void run() {
            KeyguardBouncer.this.mRoot.setVisibility(0);
            KeyguardBouncer.this.showPromptReason(KeyguardBouncer.this.mBouncerPromptReason);
            CharSequence charSequenceConsumeCustomMessage = KeyguardBouncer.this.mCallback.consumeCustomMessage();
            if (charSequenceConsumeCustomMessage != null) {
                KeyguardBouncer.this.mKeyguardView.showErrorMessage(charSequenceConsumeCustomMessage);
            }
            if (KeyguardBouncer.this.mKeyguardView.getHeight() != 0 && KeyguardBouncer.this.mKeyguardView.getHeight() != KeyguardBouncer.this.mStatusBarHeight) {
                KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
            } else {
                KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // from class: com.android.systemui.statusbar.phone.KeyguardBouncer.2.1
                    @Override // android.view.ViewTreeObserver.OnPreDrawListener
                    public boolean onPreDraw() {
                        KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().removeOnPreDrawListener(this);
                        KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
                        return true;
                    }
                });
                KeyguardBouncer.this.mKeyguardView.requestLayout();
            }
            KeyguardBouncer.this.mShowingSoon = false;
            if (KeyguardBouncer.this.mExpansion == 0.0f) {
                KeyguardBouncer.this.mKeyguardView.onResume();
            }
            StatsLog.write(63, 2);
        }
    };

    public interface BouncerExpansionCallback {
        void onFullyHidden();

        void onFullyShown();
    }

    public static /* synthetic */ void lambda$new$0(KeyguardBouncer keyguardBouncer) {
        if (keyguardBouncer.mKeyguardView != null) {
            keyguardBouncer.mKeyguardView.resetSecurityContainer();
        }
    }

    public KeyguardBouncer(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils, ViewGroup viewGroup, DismissCallbackRegistry dismissCallbackRegistry, FalsingManager falsingManager, BouncerExpansionCallback bouncerExpansionCallback) {
        this.mContext = context;
        this.mCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mContainer = viewGroup;
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
        this.mFalsingManager = falsingManager;
        this.mDismissCallbackRegistry = dismissCallbackRegistry;
        this.mExpansionCallback = bouncerExpansionCallback;
        this.mHandler = new Handler();
        this.mSecurityModel = new KeyguardSecurityModel(this.mContext);
    }

    public void show(boolean z) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        show(z, false, true);
    }

    public void show(boolean z, boolean z2, boolean z3) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (currentUser == 0 && UserManager.isSplitSystemUser()) {
            return;
        }
        if (PowerOffAlarmManager.isAlarmBoot()) {
            Slog.d("KeyguardBouncer", "show() - this is alarm boot, just re-inflate.");
            if (this.mKeyguardView != null && this.mRoot != null) {
                Slog.d("KeyguardBouncer", "show() - before re-inflate, we should pause current view.");
                this.mKeyguardView.onPause();
            }
            inflateView();
        } else {
            ensureView();
        }
        if (z3) {
            setExpansion(0.0f);
        }
        this.mIsScrimmed = z3;
        if (z) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        if (this.mRoot.getVisibility() == 0 || this.mShowingSoon) {
            return;
        }
        int currentUser2 = KeyguardUpdateMonitor.getCurrentUser();
        boolean z4 = false;
        if (!(UserManager.isSplitSystemUser() && currentUser2 == 0) && currentUser2 == currentUser) {
            z4 = true;
        }
        if (z4 && this.mKeyguardView.dismiss(currentUser2)) {
            return;
        }
        if (!z4) {
            Slog.w("KeyguardBouncer", "User can't dismiss keyguard: " + currentUser2 + " != " + currentUser);
        }
        if (!this.mKeyguardView.dismiss(z2, currentUser2)) {
            Slog.d("KeyguardBouncer", "show() - try to dismiss \"Bouncer\" directly.");
            this.mShowingSoon = true;
            DejankUtils.removeCallbacks(this.mResetRunnable);
            DejankUtils.postAfterTraversal(this.mShowRunnable);
            this.mCallback.onBouncerVisiblityChanged(true);
        }
    }

    public boolean isShowingScrimmed() {
        return isShowing() && this.mIsScrimmed;
    }

    private void onFullyShown() {
        this.mFalsingManager.onBouncerShown();
        if (this.mKeyguardView == null) {
            Log.e("KeyguardBouncer", "onFullyShown when view was null");
        } else {
            this.mKeyguardView.onResume();
        }
    }

    private void onFullyHidden() {
        if (!this.mShowingSoon) {
            cancelShowRunnable();
            if (this.mRoot != null) {
                this.mRoot.setVisibility(4);
            }
            this.mFalsingManager.onBouncerHidden();
            DejankUtils.postAfterTraversal(this.mResetRunnable);
        }
    }

    public void showPromptReason(int i) {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.showPromptReason(i);
        } else {
            Log.w("KeyguardBouncer", "Trying to show prompt reason on empty bouncer");
        }
    }

    public void showMessage(String str, int i) {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.showMessage(str, i);
        } else {
            Log.w("KeyguardBouncer", "Trying to show message on empty bouncer");
        }
    }

    private void cancelShowRunnable() {
        DejankUtils.removeCallbacks(this.mShowRunnable);
        this.mShowingSoon = false;
    }

    public void showWithDismissAction(KeyguardHostView.OnDismissAction onDismissAction, Runnable runnable) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ensureView();
        this.mKeyguardView.setOnDismissAction(onDismissAction, runnable);
        show(false);
    }

    public void hide(boolean z) {
        if (isShowing()) {
            StatsLog.write(63, 1);
            this.mDismissCallbackRegistry.notifyDismissCancelled();
        }
        this.mFalsingManager.onBouncerHidden();
        this.mCallback.onBouncerVisiblityChanged(false);
        cancelShowRunnable();
        if (this.mKeyguardView != null) {
            this.mKeyguardView.cancelDismissAction();
            this.mKeyguardView.cleanUp();
        }
        this.mIsAnimatingAway = false;
        if (this.mRoot != null) {
            this.mRoot.setVisibility(4);
            if (z) {
                this.mHandler.postDelayed(this.mRemoveViewRunnable, 50L);
            }
        }
    }

    public void startPreHideAnimation(Runnable runnable) {
        this.mIsAnimatingAway = true;
        if (this.mKeyguardView != null) {
            this.mKeyguardView.startDisappearAnimation(runnable);
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void onScreenTurnedOff() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (this.mKeyguardView != null && this.mRoot != null && this.mRoot.getVisibility() == 0) {
            this.mKeyguardView.onPause();
        }
    }

    public boolean isShowing() {
        return (this.mShowingSoon || (this.mRoot != null && this.mRoot.getVisibility() == 0)) && this.mExpansion == 0.0f && !isAnimatingAway();
    }

    public boolean isAnimatingAway() {
        return this.mIsAnimatingAway;
    }

    public void prepare() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean z = this.mRoot != null;
        ensureView();
        if (z) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        this.mBouncerPromptReason = this.mCallback.getBouncerPromptReason();
    }

    public void setExpansion(float f) {
        float f2 = this.mExpansion;
        this.mExpansion = f;
        if (this.mKeyguardView != null && !this.mIsAnimatingAway) {
            this.mKeyguardView.setAlpha(MathUtils.constrain(MathUtils.map(0.95f, 1.0f, 1.0f, 0.0f, f), 0.0f, 1.0f));
            this.mKeyguardView.setTranslationY(this.mKeyguardView.getHeight() * f);
        }
        if (f == 0.0f && f2 != 0.0f) {
            onFullyShown();
            this.mExpansionCallback.onFullyShown();
        } else if (f == 1.0f && f2 != 1.0f) {
            onFullyHidden();
            this.mExpansionCallback.onFullyHidden();
        }
    }

    public boolean willDismissWithAction() {
        return this.mKeyguardView != null && this.mKeyguardView.hasDismissActions();
    }

    public int getTop() {
        if (this.mKeyguardView == null) {
            return 0;
        }
        int top = this.mKeyguardView.getTop();
        if (this.mKeyguardView.getCurrentSecurityMode() == KeyguardSecurityModel.SecurityMode.Password) {
            return top + this.mKeyguardView.findViewById(R.id.keyguard_message_area).getTop();
        }
        return top;
    }

    protected void ensureView() {
        boolean zHasCallbacks = this.mHandler.hasCallbacks(this.mRemoveViewRunnable);
        if (this.mRoot == null || zHasCallbacks) {
            inflateView();
        }
    }

    protected void inflateView() {
        removeView();
        this.mHandler.removeCallbacks(this.mRemoveViewRunnable);
        this.mRoot = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_bouncer, (ViewGroup) null);
        this.mKeyguardView = (KeyguardHostView) this.mRoot.findViewById(R.id.keyguard_host_view);
        this.mKeyguardView.setLockPatternUtils(this.mLockPatternUtils);
        this.mKeyguardView.setViewMediatorCallback(this.mCallback);
        this.mContainer.addView(this.mRoot, this.mContainer.getChildCount());
        this.mStatusBarHeight = this.mRoot.getResources().getDimensionPixelOffset(R.dimen.status_bar_height);
        this.mRoot.setVisibility(4);
        this.mRoot.setAccessibilityPaneTitle(this.mKeyguardView.getAccessibilityTitleForCurrentMode());
        WindowInsets rootWindowInsets = this.mRoot.getRootWindowInsets();
        if (rootWindowInsets != null) {
            this.mRoot.dispatchApplyWindowInsets(rootWindowInsets);
        }
    }

    protected void removeView() {
        if (this.mRoot != null && this.mRoot.getParent() == this.mContainer) {
            this.mContainer.removeView(this.mRoot);
            this.mRoot = null;
        }
    }

    public boolean needsFullscreenBouncer() {
        ensureView();
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
        return securityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe1 || securityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe2 || securityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe3 || securityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe4 || securityMode == KeyguardSecurityModel.SecurityMode.AntiTheft || securityMode == KeyguardSecurityModel.SecurityMode.AlarmBoot;
    }

    public boolean isFullscreenBouncer() {
        if (this.mKeyguardView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode currentSecurityMode = this.mKeyguardView.getCurrentSecurityMode();
        return currentSecurityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe1 || currentSecurityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe2 || currentSecurityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe3 || currentSecurityMode == KeyguardSecurityModel.SecurityMode.SimPinPukMe4 || currentSecurityMode == KeyguardSecurityModel.SecurityMode.AntiTheft || currentSecurityMode == KeyguardSecurityModel.SecurityMode.AlarmBoot;
    }

    public boolean isSecure() {
        return this.mKeyguardView == null || this.mKeyguardView.getSecurityMode() != KeyguardSecurityModel.SecurityMode.None;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mKeyguardView.shouldEnableMenuKey();
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        ensureView();
        return this.mKeyguardView.interceptMediaKey(keyEvent);
    }

    public void notifyKeyguardAuthenticated(boolean z) {
        ensureView();
        this.mKeyguardView.finish(z, KeyguardUpdateMonitor.getCurrentUser());
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("KeyguardBouncer");
        printWriter.println("  isShowing(): " + isShowing());
        printWriter.println("  mStatusBarHeight: " + this.mStatusBarHeight);
        printWriter.println("  mExpansion: " + this.mExpansion);
        printWriter.println("  mKeyguardView; " + this.mKeyguardView);
        printWriter.println("  mShowingSoon: " + this.mKeyguardView);
        printWriter.println("  mBouncerPromptReason: " + this.mBouncerPromptReason);
        printWriter.println("  mIsAnimatingAway: " + this.mIsAnimatingAway);
    }
}
