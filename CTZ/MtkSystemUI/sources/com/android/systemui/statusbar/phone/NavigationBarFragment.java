package com.android.systemui.statusbar.phone;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Fragment;
import android.app.IActivityManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.IRotationWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.LatencyTracker;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.policy.RotationLockController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class NavigationBarFragment extends Fragment implements CommandQueue.Callbacks {
    private boolean mAccessibilityFeedbackEnabled;
    private AccessibilityManager mAccessibilityManager;
    protected AssistManager mAssistManager;
    private CommandQueue mCommandQueue;
    private ContentResolver mContentResolver;
    private int mDisabledFlags1;
    private int mDisabledFlags2;
    private Divider mDivider;
    public boolean mHomeBlockedThisTouch;
    private boolean mHoveringRotationSuggestion;
    private long mLastLockToAppLongPress;
    private int mLastRotationSuggestion;
    private int mLayoutDirection;
    private LightBarController mLightBarController;
    private Locale mLocale;
    private MagnificationContentObserver mMagnificationObserver;
    private int mNavigationBarMode;
    private OverviewProxyService mOverviewProxyService;
    private boolean mPendingRotationSuggestion;
    private Recents mRecents;
    private Animator mRotateHideAnimator;
    private RotationLockController mRotationLockController;
    private StatusBar mStatusBar;
    private int mSystemUiVisibility;
    private TaskStackListenerImpl mTaskStackListener;
    private WindowManager mWindowManager;
    protected NavigationBarView mNavigationBarView = null;
    private int mNavigationBarWindowState = 0;
    private int mNavigationIconHints = 0;
    private final MetricsLogger mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
    private final Runnable mRemoveRotationProposal = new Runnable() {
        @Override
        public final void run() {
            this.f$0.setRotateSuggestionButtonState(false);
        }
    };
    private final Runnable mCancelPendingRotationProposal = new Runnable() {
        @Override
        public final void run() {
            this.f$0.mPendingRotationSuggestion = false;
        }
    };
    private ViewRippler mViewRippler = new ViewRippler();
    private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
        @Override
        public void onConnectionChanged(boolean z) {
            NavigationBarFragment.this.mNavigationBarView.updateStates();
            NavigationBarFragment.this.updateScreenPinningGestures();
        }

        @Override
        public void onQuickStepStarted() {
            NavigationBarFragment.this.setRotateSuggestionButtonState(false);
        }

        @Override
        public void onInteractionFlagsChanged(int i) {
            NavigationBarFragment.this.mNavigationBarView.updateStates();
            NavigationBarFragment.this.updateScreenPinningGestures();
        }

        @Override
        public void onBackButtonAlphaChanged(float f, boolean z) {
            ButtonDispatcher backButton = NavigationBarFragment.this.mNavigationBarView.getBackButton();
            backButton.setVisibility(f > 0.0f ? 0 : 4);
            backButton.setAlpha(f, z);
        }
    };
    private final AccessibilityManager.AccessibilityServicesStateChangeListener mAccessibilityListener = new AccessibilityManager.AccessibilityServicesStateChangeListener() {
        @Override
        public final void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager) {
            this.f$0.updateAccessibilityServicesState(accessibilityManager);
        }
    };
    private final IRotationWatcher.Stub mRotationWatcher = new AnonymousClass3();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action) || "android.intent.action.SCREEN_ON".equals(action)) {
                NavigationBarFragment.this.notifyNavigationBarScreenOn();
            }
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                NavigationBarFragment.this.updateAccessibilityServicesState(NavigationBarFragment.this.mAccessibilityManager);
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class);
        this.mCommandQueue.addCallbacks(this);
        this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(getContext(), StatusBar.class);
        this.mRecents = (Recents) SysUiServiceProvider.getComponent(getContext(), Recents.class);
        this.mDivider = (Divider) SysUiServiceProvider.getComponent(getContext(), Divider.class);
        this.mWindowManager = (WindowManager) getContext().getSystemService(WindowManager.class);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService(AccessibilityManager.class);
        ((AccessibilityManagerWrapper) Dependency.get(AccessibilityManagerWrapper.class)).addCallback(this.mAccessibilityListener);
        this.mContentResolver = getContext().getContentResolver();
        this.mMagnificationObserver = new MagnificationContentObserver(getContext().getMainThreadHandler());
        this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_display_magnification_navbar_enabled"), false, this.mMagnificationObserver, -1);
        if (bundle != null) {
            this.mDisabledFlags1 = bundle.getInt("disabled_state", 0);
            this.mDisabledFlags2 = bundle.getInt("disabled2_state", 0);
        }
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        try {
            WindowManagerGlobal.getWindowManagerService().watchRotation(this.mRotationWatcher, getContext().getDisplay().getDisplayId());
            this.mRotationLockController = (RotationLockController) Dependency.get(RotationLockController.class);
            if (this.mRotationLockController.isRotationLocked()) {
                this.mRotationLockController.setRotationLockedAtAngle(true, this.mWindowManager.getDefaultDisplay().getRotation());
            }
            this.mTaskStackListener = new TaskStackListenerImpl();
            ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mCommandQueue.removeCallbacks(this);
        ((AccessibilityManagerWrapper) Dependency.get(AccessibilityManagerWrapper.class)).removeCallback(this.mAccessibilityListener);
        this.mContentResolver.unregisterContentObserver(this.mMagnificationObserver);
        try {
            WindowManagerGlobal.getWindowManagerService().removeRotationWatcher(this.mRotationWatcher);
            ActivityManagerWrapper.getInstance().unregisterTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.navigation_bar, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mNavigationBarView = (NavigationBarView) view;
        this.mNavigationBarView.setDisabledFlags(this.mDisabledFlags1);
        this.mNavigationBarView.setComponents(this.mRecents, this.mDivider, this.mStatusBar.getPanel());
        this.mNavigationBarView.setOnVerticalChangedListener(new NavigationBarView.OnVerticalChangedListener() {
            @Override
            public final void onVerticalChanged(boolean z) {
                this.f$0.onVerticalChanged(z);
            }
        });
        this.mNavigationBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                return this.f$0.onNavigationTouch(view2, motionEvent);
            }
        });
        if (bundle != null) {
            this.mNavigationBarView.getLightTransitionsController().restoreState(bundle);
        }
        prepareNavigationBarView();
        checkNavBarModes();
        setDisabled2Flags(this.mDisabledFlags2);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        notifyNavigationBarScreenOn();
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mNavigationBarView.getLightTransitionsController().destroy(getContext());
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
        getContext().unregisterReceiver(this.mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("disabled_state", this.mDisabledFlags1);
        bundle.putInt("disabled2_state", this.mDisabledFlags2);
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getLightTransitionsController().saveState(bundle);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        Locale locale = getContext().getResources().getConfiguration().locale;
        int layoutDirectionFromLocale = TextUtils.getLayoutDirectionFromLocale(locale);
        if (!locale.equals(this.mLocale) || layoutDirectionFromLocale != this.mLayoutDirection) {
            this.mLocale = locale;
            this.mLayoutDirection = layoutDirectionFromLocale;
            refreshLayout(layoutDirectionFromLocale);
        }
        repositionNavigationBar();
    }

    @Override
    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mNavigationBarView != null) {
            printWriter.print("  mNavigationBarWindowState=");
            printWriter.println(StatusBarManager.windowStateToString(this.mNavigationBarWindowState));
            printWriter.print("  mNavigationBarMode=");
            printWriter.println(BarTransitions.modeToString(this.mNavigationBarMode));
            StatusBar.dumpBarTransitions(printWriter, "mNavigationBarView", this.mNavigationBarView.getBarTransitions());
        }
        printWriter.print("  mNavigationBarView=");
        if (this.mNavigationBarView == null) {
            printWriter.println("null");
        } else {
            this.mNavigationBarView.dump(fileDescriptor, printWriter, strArr);
        }
    }

    @Override
    public void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) {
        boolean z2;
        int i3;
        if ((i & 2) == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        int i4 = this.mNavigationIconHints;
        switch (i2) {
            case 0:
            case 1:
            case 2:
                if (z2) {
                    i4 |= 1;
                } else {
                    i4 &= -2;
                }
                break;
            case 3:
                i4 &= -2;
                break;
        }
        if (z) {
            i3 = i4 | 2;
        } else {
            i3 = i4 & (-3);
        }
        if (i3 == this.mNavigationIconHints) {
            return;
        }
        this.mNavigationIconHints = i3;
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setNavigationIconHints(i3);
        }
        this.mStatusBar.checkBarModes();
    }

    @Override
    public void topAppWindowChanged(boolean z) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setMenuVisibility(z);
        }
    }

    @Override
    public void setWindowState(int i, int i2) {
        if (this.mNavigationBarView != null && i == 2 && this.mNavigationBarWindowState != i2) {
            this.mNavigationBarWindowState = i2;
            if (i2 == 0 && this.mPendingRotationSuggestion) {
                showAndLogRotationSuggestion();
            }
        }
    }

    @Override
    public void onRotationProposal(int i, boolean z) {
        int i2;
        int rotation = this.mWindowManager.getDefaultDisplay().getRotation();
        boolean zHasDisable2RotateSuggestionFlag = hasDisable2RotateSuggestionFlag(this.mDisabledFlags2);
        StringBuilder sb = new StringBuilder();
        sb.append("onRotationProposal proposedRotation=");
        sb.append(Surface.rotationToString(i));
        sb.append(", winRotation=");
        sb.append(Surface.rotationToString(rotation));
        sb.append(", isValid=");
        sb.append(z);
        sb.append(", mNavBarWindowState=");
        sb.append(StatusBarManager.windowStateToString(this.mNavigationBarWindowState));
        sb.append(", rotateSuggestionsDisabled=");
        sb.append(zHasDisable2RotateSuggestionFlag);
        sb.append(", isRotateButtonVisible=");
        sb.append(this.mNavigationBarView == null ? "null" : Boolean.valueOf(this.mNavigationBarView.isRotateButtonVisible()));
        Log.v("NavigationBar", sb.toString());
        if (zHasDisable2RotateSuggestionFlag) {
            return;
        }
        if (!z) {
            setRotateSuggestionButtonState(false);
            return;
        }
        if (i == rotation) {
            getView().removeCallbacks(this.mRemoveRotationProposal);
            setRotateSuggestionButtonState(false);
            return;
        }
        this.mLastRotationSuggestion = i;
        if (this.mNavigationBarView != null) {
            boolean zIsRotationAnimationCCW = isRotationAnimationCCW(rotation, i);
            if (rotation == 0 || rotation == 2) {
                i2 = zIsRotationAnimationCCW ? R.style.RotateButtonCCWStart90 : R.style.RotateButtonCWStart90;
            } else {
                i2 = zIsRotationAnimationCCW ? R.style.RotateButtonCCWStart0 : R.style.RotateButtonCWStart0;
            }
            this.mNavigationBarView.updateRotateSuggestionButtonStyle(i2, true);
        }
        if (this.mNavigationBarWindowState != 0) {
            this.mPendingRotationSuggestion = true;
            getView().removeCallbacks(this.mCancelPendingRotationProposal);
            getView().postDelayed(this.mCancelPendingRotationProposal, 20000L);
            return;
        }
        showAndLogRotationSuggestion();
    }

    private void onRotationSuggestionsDisabled() {
        setRotateSuggestionButtonState(false, true);
        View view = getView();
        if (view != null) {
            view.removeCallbacks(this.mRemoveRotationProposal);
        }
    }

    private void showAndLogRotationSuggestion() {
        setRotateSuggestionButtonState(true);
        rescheduleRotationTimeout(false);
        this.mMetricsLogger.visible(1288);
    }

    private boolean isRotationAnimationCCW(int i, int i2) {
        if (i == 0 && i2 == 1) {
            return false;
        }
        if (i == 0 && i2 == 2) {
            return true;
        }
        if (i == 0 && i2 == 3) {
            return true;
        }
        if (i == 1 && i2 == 0) {
            return true;
        }
        if (i == 1 && i2 == 2) {
            return false;
        }
        if (i == 1 && i2 == 3) {
            return true;
        }
        if (i == 2 && i2 == 0) {
            return true;
        }
        if (i == 2 && i2 == 1) {
            return true;
        }
        if (i == 2 && i2 == 3) {
            return false;
        }
        if (i == 3 && i2 == 0) {
            return false;
        }
        if (i == 3 && i2 == 1) {
            return true;
        }
        return i == 3 && i2 == 2;
    }

    public void setRotateSuggestionButtonState(boolean z) {
        setRotateSuggestionButtonState(z, false);
    }

    public void setRotateSuggestionButtonState(boolean z, boolean z2) {
        View currentView;
        KeyButtonDrawable imageDrawable;
        AnimatedVectorDrawable animatedVectorDrawable;
        if (this.mNavigationBarView == null) {
            return;
        }
        ButtonDispatcher rotateSuggestionButton = this.mNavigationBarView.getRotateSuggestionButton();
        boolean zIsRotateButtonVisible = this.mNavigationBarView.isRotateButtonVisible();
        if ((!z && !zIsRotateButtonVisible) || (currentView = rotateSuggestionButton.getCurrentView()) == null || (imageDrawable = rotateSuggestionButton.getImageDrawable()) == null) {
            return;
        }
        if (imageDrawable.getDrawable(0) instanceof AnimatedVectorDrawable) {
            animatedVectorDrawable = (AnimatedVectorDrawable) imageDrawable.getDrawable(0);
        } else {
            animatedVectorDrawable = null;
        }
        this.mPendingRotationSuggestion = false;
        if (getView() != null) {
            getView().removeCallbacks(this.mCancelPendingRotationProposal);
        }
        if (z) {
            if (this.mRotateHideAnimator != null && this.mRotateHideAnimator.isRunning()) {
                this.mRotateHideAnimator.cancel();
            }
            this.mRotateHideAnimator = null;
            currentView.setAlpha(1.0f);
            if (animatedVectorDrawable != null) {
                animatedVectorDrawable.reset();
                animatedVectorDrawable.start();
            }
            if (!isRotateSuggestionIntroduced()) {
                this.mViewRippler.start(currentView);
            }
            if (this.mNavigationBarView.setRotateButtonVisibility(true) == 0) {
                this.mStatusBar.touchAutoHide();
                return;
            }
            return;
        }
        this.mViewRippler.stop();
        if (z2) {
            if (this.mRotateHideAnimator != null && this.mRotateHideAnimator.isRunning()) {
                this.mRotateHideAnimator.pause();
            }
            this.mNavigationBarView.setRotateButtonVisibility(false);
            return;
        }
        if (this.mRotateHideAnimator == null || !this.mRotateHideAnimator.isRunning()) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(currentView, "alpha", 0.0f);
            objectAnimatorOfFloat.setDuration(100L);
            objectAnimatorOfFloat.setInterpolator(Interpolators.LINEAR);
            objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    NavigationBarFragment.this.mNavigationBarView.setRotateButtonVisibility(false);
                }
            });
            this.mRotateHideAnimator = objectAnimatorOfFloat;
            objectAnimatorOfFloat.start();
        }
    }

    private void rescheduleRotationTimeout(boolean z) {
        if (!z || ((this.mRotateHideAnimator == null || !this.mRotateHideAnimator.isRunning()) && this.mNavigationBarView.isRotateButtonVisible())) {
            getView().removeCallbacks(this.mRemoveRotationProposal);
            getView().postDelayed(this.mRemoveRotationProposal, computeRotationProposalTimeout());
        }
    }

    private int computeRotationProposalTimeout() {
        if (this.mAccessibilityFeedbackEnabled) {
            return 20000;
        }
        return this.mHoveringRotationSuggestion ? 16000 : 10000;
    }

    private boolean isRotateSuggestionIntroduced() {
        return Settings.Secure.getInt(getContext().getContentResolver(), "num_rotation_suggestions_accepted", 0) >= 3;
    }

    private void incrementNumAcceptedRotationSuggestionsIfNeeded() {
        ContentResolver contentResolver = getContext().getContentResolver();
        int i = Settings.Secure.getInt(contentResolver, "num_rotation_suggestions_accepted", 0);
        if (i < 3) {
            Settings.Secure.putInt(contentResolver, "num_rotation_suggestions_accepted", i + 1);
        }
    }

    public void setCurrentSysuiVisibility(int i) {
        this.mSystemUiVisibility = i;
        this.mNavigationBarMode = this.mStatusBar.computeBarMode(0, this.mSystemUiVisibility, 134217728, Integer.MIN_VALUE, 32768);
        checkNavBarModes();
        this.mStatusBar.touchAutoHide();
        this.mLightBarController.onNavigationVisibilityChanged(this.mSystemUiVisibility, 0, true, this.mNavigationBarMode);
    }

    @Override
    public void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2) {
        int iComputeBarMode;
        int i5 = this.mSystemUiVisibility;
        int i6 = ((~i4) & i5) | (i & i4);
        if ((i6 ^ i5) != 0) {
            this.mSystemUiVisibility = i6;
            if (getView() != null) {
                iComputeBarMode = this.mStatusBar.computeBarMode(i5, i6, 134217728, Integer.MIN_VALUE, 32768);
            } else {
                iComputeBarMode = -1;
            }
            z = iComputeBarMode != -1;
            if (z) {
                if (this.mNavigationBarMode != iComputeBarMode) {
                    this.mNavigationBarMode = iComputeBarMode;
                    checkNavBarModes();
                }
                this.mStatusBar.touchAutoHide();
            }
        }
        this.mLightBarController.onNavigationVisibilityChanged(i, i4, z, this.mNavigationBarMode);
    }

    @Override
    public void disable(int i, int i2, boolean z) {
        int i3 = 56623104 & i;
        if (i3 != this.mDisabledFlags1) {
            this.mDisabledFlags1 = i3;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setDisabledFlags(i);
            }
            updateScreenPinningGestures();
        }
        int i4 = i2 & 16;
        if (i4 != this.mDisabledFlags2) {
            this.mDisabledFlags2 = i4;
            setDisabled2Flags(i4);
        }
    }

    private void setDisabled2Flags(int i) {
        if (hasDisable2RotateSuggestionFlag(i)) {
            onRotationSuggestionsDisabled();
        }
    }

    private boolean hasDisable2RotateSuggestionFlag(int i) {
        return (i & 16) != 0;
    }

    private void refreshLayout(int i) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setLayoutDirection(i);
        }
    }

    private boolean shouldDisableNavbarGestures() {
        return (this.mStatusBar.isDeviceProvisioned() && (this.mDisabledFlags1 & 33554432) == 0) ? false : true;
    }

    private void repositionNavigationBar() {
        if (this.mNavigationBarView == null || !this.mNavigationBarView.isAttachedToWindow()) {
            return;
        }
        prepareNavigationBarView();
        this.mWindowManager.updateViewLayout((View) this.mNavigationBarView.getParent(), ((View) this.mNavigationBarView.getParent()).getLayoutParams());
    }

    private void updateScreenPinningGestures() {
        if (this.mNavigationBarView == null) {
            return;
        }
        boolean zIsRecentsButtonVisible = this.mNavigationBarView.isRecentsButtonVisible();
        ButtonDispatcher backButton = this.mNavigationBarView.getBackButton();
        if (zIsRecentsButtonVisible) {
            backButton.setOnLongClickListener(new $$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8(this));
        } else {
            backButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public final boolean onLongClick(View view) {
                    return this.f$0.onLongPressBackHome(view);
                }
            });
        }
    }

    private void notifyNavigationBarScreenOn() {
        this.mNavigationBarView.updateNavButtonIcons();
    }

    private void prepareNavigationBarView() {
        this.mNavigationBarView.reorient();
        ButtonDispatcher recentsButton = this.mNavigationBarView.getRecentsButton();
        recentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onRecentsClick(view);
            }
        });
        recentsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return this.f$0.onRecentsTouch(view, motionEvent);
            }
        });
        recentsButton.setLongClickable(true);
        recentsButton.setOnLongClickListener(new $$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8(this));
        this.mNavigationBarView.getBackButton().setLongClickable(true);
        ButtonDispatcher homeButton = this.mNavigationBarView.getHomeButton();
        homeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return this.f$0.onHomeTouch(view, motionEvent);
            }
        });
        homeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public final boolean onLongClick(View view) {
                return this.f$0.onHomeLongClick(view);
            }
        });
        ButtonDispatcher accessibilityButton = this.mNavigationBarView.getAccessibilityButton();
        accessibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onAccessibilityClick(view);
            }
        });
        accessibilityButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public final boolean onLongClick(View view) {
                return this.f$0.onAccessibilityLongClick(view);
            }
        });
        updateAccessibilityServicesState(this.mAccessibilityManager);
        ButtonDispatcher rotateSuggestionButton = this.mNavigationBarView.getRotateSuggestionButton();
        rotateSuggestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onRotateSuggestionClick(view);
            }
        });
        rotateSuggestionButton.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public final boolean onHover(View view, MotionEvent motionEvent) {
                return this.f$0.onRotateSuggestionHover(view, motionEvent);
            }
        });
        updateScreenPinningGestures();
    }

    private boolean onHomeTouch(View view, MotionEvent motionEvent) {
        if (this.mHomeBlockedThisTouch && motionEvent.getActionMasked() != 0) {
            return true;
        }
        int action = motionEvent.getAction();
        if (action != 3) {
            switch (action) {
                case 0:
                    this.mHomeBlockedThisTouch = false;
                    TelecomManager telecomManager = (TelecomManager) getContext().getSystemService(TelecomManager.class);
                    if (telecomManager != null && telecomManager.isRinging() && this.mStatusBar.isKeyguardShowing()) {
                        Log.i("NavigationBar", "Ignoring HOME; there's a ringing incoming call. No heads up");
                        this.mHomeBlockedThisTouch = true;
                        return true;
                    }
                    break;
                case 1:
                    this.mStatusBar.awakenDreams();
                    break;
            }
        }
        return false;
    }

    private void onVerticalChanged(boolean z) {
        this.mStatusBar.setQsScrimEnabled(!z);
    }

    private boolean onNavigationTouch(View view, MotionEvent motionEvent) {
        this.mStatusBar.checkUserAutohide(motionEvent);
        return false;
    }

    boolean onHomeLongClick(View view) {
        if (!this.mNavigationBarView.isRecentsButtonVisible() && ActivityManagerWrapper.getInstance().isScreenPinningActive()) {
            return onLongPressBackHome(view);
        }
        if (shouldDisableNavbarGestures()) {
            return false;
        }
        this.mNavigationBarView.onNavigationButtonLongPress(view);
        this.mMetricsLogger.action(239);
        this.mAssistManager.startAssist(new Bundle());
        this.mStatusBar.awakenDreams();
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.abortCurrentGesture();
            return true;
        }
        return true;
    }

    private boolean onRecentsTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        if (action == 0) {
            this.mCommandQueue.preloadRecentApps();
            return false;
        }
        if (action == 3) {
            this.mCommandQueue.cancelPreloadRecentApps();
            return false;
        }
        if (action == 1 && !view.isPressed()) {
            this.mCommandQueue.cancelPreloadRecentApps();
            return false;
        }
        return false;
    }

    private void onRecentsClick(View view) {
        if (LatencyTracker.isEnabled(getContext())) {
            LatencyTracker.getInstance(getContext()).onActionStart(1);
        }
        this.mStatusBar.awakenDreams();
        this.mCommandQueue.toggleRecentApps();
    }

    private boolean onLongPressBackHome(View view) {
        this.mNavigationBarView.onNavigationButtonLongPress(view);
        return onLongPressNavigationButtons(view, R.id.back, R.id.home);
    }

    private boolean onLongPressBackRecents(View view) {
        this.mNavigationBarView.onNavigationButtonLongPress(view);
        return onLongPressNavigationButtons(view, R.id.back, R.id.recent_apps);
    }

    private boolean onLongPressNavigationButtons(View view, int i, int i2) {
        IActivityManager iActivityManager;
        boolean zIsTouchExplorationEnabled;
        boolean zIsInLockTaskMode;
        boolean z;
        ButtonDispatcher homeButton;
        try {
            iActivityManager = ActivityManagerNative.getDefault();
            zIsTouchExplorationEnabled = this.mAccessibilityManager.isTouchExplorationEnabled();
            zIsInLockTaskMode = iActivityManager.isInLockTaskMode();
        } catch (RemoteException e) {
            Log.d("NavigationBar", "Unable to reach activity manager", e);
        }
        if (zIsInLockTaskMode && !zIsTouchExplorationEnabled) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (jCurrentTimeMillis - this.mLastLockToAppLongPress < 200) {
                iActivityManager.stopSystemLockTaskMode();
                this.mNavigationBarView.updateNavButtonIcons();
                return true;
            }
            if (view.getId() != i) {
                z = false;
                this.mLastLockToAppLongPress = jCurrentTimeMillis;
            } else {
                if (i2 == R.id.recent_apps) {
                    homeButton = this.mNavigationBarView.getRecentsButton();
                } else {
                    homeButton = this.mNavigationBarView.getHomeButton();
                }
                if (!homeButton.getCurrentView().isPressed()) {
                    z = true;
                }
                this.mLastLockToAppLongPress = jCurrentTimeMillis;
            }
            return false;
        }
        if (view.getId() != i) {
            if (zIsTouchExplorationEnabled && zIsInLockTaskMode) {
                iActivityManager.stopSystemLockTaskMode();
                this.mNavigationBarView.updateNavButtonIcons();
                return true;
            }
            if (view.getId() == i2) {
                if (i2 == R.id.recent_apps) {
                    return onLongPressRecents();
                }
                return onHomeLongClick(this.mNavigationBarView.getHomeButton().getCurrentView());
            }
            z = false;
        } else {
            z = true;
        }
        if (z) {
            KeyButtonView keyButtonView = (KeyButtonView) view;
            keyButtonView.sendEvent(0, 128);
            keyButtonView.sendAccessibilityEvent(2);
            return true;
        }
        return false;
    }

    private boolean onLongPressRecents() {
        if (this.mRecents == null || !ActivityManager.supportsMultiWindow(getContext()) || !this.mDivider.getView().getSnapAlgorithm().isSplitScreenFeasible() || Recents.getConfiguration().isLowRamDevice || this.mOverviewProxyService.getProxy() != null) {
            return false;
        }
        return this.mStatusBar.toggleSplitScreenMode(271, 286);
    }

    private void onAccessibilityClick(View view) {
        this.mAccessibilityManager.notifyAccessibilityButtonClicked();
    }

    private boolean onAccessibilityLongClick(View view) {
        Intent intent = new Intent("com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON");
        intent.addFlags(268468224);
        view.getContext().startActivityAsUser(intent, UserHandle.CURRENT);
        return true;
    }

    private void updateAccessibilityServicesState(AccessibilityManager accessibilityManager) {
        int i;
        boolean z = false;
        try {
            if (Settings.Secure.getIntForUser(this.mContentResolver, "accessibility_display_magnification_navbar_enabled", -2) != 1) {
                i = 0;
            } else {
                i = 1;
            }
        } catch (Settings.SettingNotFoundException e) {
            i = 0;
        }
        List<AccessibilityServiceInfo> enabledAccessibilityServiceList = accessibilityManager.getEnabledAccessibilityServiceList(-1);
        int i2 = i;
        boolean z2 = false;
        for (int size = enabledAccessibilityServiceList.size() - 1; size >= 0; size--) {
            AccessibilityServiceInfo accessibilityServiceInfo = enabledAccessibilityServiceList.get(size);
            if ((accessibilityServiceInfo.flags & 256) != 0) {
                i2++;
            }
            if (accessibilityServiceInfo.feedbackType != 0 && accessibilityServiceInfo.feedbackType != 16) {
                z2 = true;
            }
        }
        this.mAccessibilityFeedbackEnabled = z2;
        boolean z3 = i2 >= 1;
        if (i2 >= 2) {
            z = true;
        }
        this.mNavigationBarView.setAccessibilityButtonState(z3, z);
    }

    private void onRotateSuggestionClick(View view) {
        this.mMetricsLogger.action(1287);
        incrementNumAcceptedRotationSuggestionsIfNeeded();
        this.mRotationLockController.setRotationLockedAtAngle(true, this.mLastRotationSuggestion);
    }

    private boolean onRotateSuggestionHover(View view, MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        this.mHoveringRotationSuggestion = actionMasked == 9 || actionMasked == 7;
        rescheduleRotationTimeout(true);
        return false;
    }

    public void setLightBarController(LightBarController lightBarController) {
        this.mLightBarController = lightBarController;
        this.mLightBarController.setNavigationBar(this.mNavigationBarView.getLightTransitionsController());
    }

    public boolean isSemiTransparent() {
        return this.mNavigationBarMode == 1;
    }

    public void disableAnimationsDuringHide(long j) {
        this.mNavigationBarView.setLayoutTransitionsEnabled(false);
        this.mNavigationBarView.postDelayed(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mNavigationBarView.setLayoutTransitionsEnabled(true);
            }
        }, j + 448);
    }

    public BarTransitions getBarTransitions() {
        return this.mNavigationBarView.getBarTransitions();
    }

    public void checkNavBarModes() {
        this.mStatusBar.checkBarMode(this.mNavigationBarMode, this.mNavigationBarWindowState, this.mNavigationBarView.getBarTransitions());
    }

    public void finishBarAnimations() {
        this.mNavigationBarView.getBarTransitions().finishAnimations();
    }

    private class MagnificationContentObserver extends ContentObserver {
        public MagnificationContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            NavigationBarFragment.this.updateAccessibilityServicesState(NavigationBarFragment.this.mAccessibilityManager);
        }
    }

    class AnonymousClass3 extends IRotationWatcher.Stub {
        AnonymousClass3() {
        }

        public void onRotationChanged(final int i) throws RemoteException {
            Handler handler = NavigationBarFragment.this.getView().getHandler();
            Message messageObtain = Message.obtain(handler, new Runnable() {
                @Override
                public final void run() {
                    NavigationBarFragment.AnonymousClass3.lambda$onRotationChanged$0(this.f$0, i);
                }
            });
            messageObtain.setAsynchronous(true);
            handler.sendMessageAtFrontOfQueue(messageObtain);
        }

        public static void lambda$onRotationChanged$0(AnonymousClass3 anonymousClass3, int i) {
            if (NavigationBarFragment.this.mRotationLockController.isRotationLocked()) {
                if (anonymousClass3.shouldOverrideUserLockPrefs(i)) {
                    NavigationBarFragment.this.mRotationLockController.setRotationLockedAtAngle(true, i);
                }
                NavigationBarFragment.this.setRotateSuggestionButtonState(false, true);
            }
            if (NavigationBarFragment.this.mNavigationBarView != null && NavigationBarFragment.this.mNavigationBarView.needsReorient(i)) {
                NavigationBarFragment.this.repositionNavigationBar();
            }
        }

        private boolean shouldOverrideUserLockPrefs(int i) {
            return i == 0;
        }
    }

    class TaskStackListenerImpl extends SysUiTaskStackChangeListener {
        TaskStackListenerImpl() {
        }

        @Override
        public void onTaskStackChanged() {
            NavigationBarFragment.this.setRotateSuggestionButtonState(false);
        }

        @Override
        public void onTaskRemoved(int i) {
            NavigationBarFragment.this.setRotateSuggestionButtonState(false);
        }

        @Override
        public void onTaskMovedToFront(int i) {
            NavigationBarFragment.this.setRotateSuggestionButtonState(false);
        }

        @Override
        public void onActivityRequestedOrientationChanged(final int i, int i2) {
            Optional.ofNullable(ActivityManagerWrapper.getInstance()).map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((ActivityManagerWrapper) obj).getRunningTask();
                }
            }).ifPresent(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    NavigationBarFragment.TaskStackListenerImpl.lambda$onActivityRequestedOrientationChanged$0(this.f$0, i, (ActivityManager.RunningTaskInfo) obj);
                }
            });
        }

        public static void lambda$onActivityRequestedOrientationChanged$0(TaskStackListenerImpl taskStackListenerImpl, int i, ActivityManager.RunningTaskInfo runningTaskInfo) {
            if (runningTaskInfo.id == i) {
                NavigationBarFragment.this.setRotateSuggestionButtonState(false);
            }
        }
    }

    private class ViewRippler {
        private final Runnable mRipple;
        private View mRoot;

        private ViewRippler() {
            this.mRipple = new Runnable() {
                @Override
                public void run() {
                    if (ViewRippler.this.mRoot.isAttachedToWindow()) {
                        ViewRippler.this.mRoot.setPressed(true);
                        ViewRippler.this.mRoot.setPressed(false);
                    }
                }
            };
        }

        public void start(View view) {
            stop();
            this.mRoot = view;
            this.mRoot.postOnAnimationDelayed(this.mRipple, 50L);
            this.mRoot.postOnAnimationDelayed(this.mRipple, 2000L);
            this.mRoot.postOnAnimationDelayed(this.mRipple, 4000L);
            this.mRoot.postOnAnimationDelayed(this.mRipple, 6000L);
            this.mRoot.postOnAnimationDelayed(this.mRipple, 8000L);
        }

        public void stop() {
            if (this.mRoot != null) {
                this.mRoot.removeCallbacks(this.mRipple);
            }
        }
    }

    public static View create(Context context, FragmentHostManager.FragmentListener fragmentListener) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2019, 545521768, -3);
        layoutParams.token = new Binder();
        layoutParams.setTitle("NavigationBar");
        layoutParams.accessibilityTitle = context.getString(R.string.nav_bar);
        layoutParams.windowAnimations = 0;
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.navigation_bar_window, (ViewGroup) null);
        if (viewInflate == null) {
            return null;
        }
        ((WindowManager) context.getSystemService(WindowManager.class)).addView(viewInflate, layoutParams);
        FragmentHostManager fragmentHostManager = FragmentHostManager.get(viewInflate);
        fragmentHostManager.getFragmentManager().beginTransaction().replace(R.id.navigation_bar_frame, new NavigationBarFragment(), "NavigationBar").commit();
        fragmentHostManager.addTagListener("NavigationBar", fragmentListener);
        return viewInflate;
    }
}
