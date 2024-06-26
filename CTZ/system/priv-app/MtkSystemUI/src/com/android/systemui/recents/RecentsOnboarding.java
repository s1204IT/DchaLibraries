package com.android.systemui.recents;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
@TargetApi(28)
/* loaded from: classes.dex */
public class RecentsOnboarding {
    private final View mArrowView;
    private Set<String> mBlacklistedPackages;
    private final Context mContext;
    private final ImageView mDismissView;
    private boolean mHasDismissedQuickScrubTip;
    private boolean mHasDismissedSwipeUpTip;
    private final View mLayout;
    private boolean mLayoutAttachedToWindow;
    private int mNavBarHeight;
    private int mNumAppsLaunchedSinceSwipeUpTipDismiss;
    private final int mOnboardingToastArrowRadius;
    private final int mOnboardingToastColor;
    private int mOverviewOpenedCountSinceQuickScrubTipDismiss;
    private boolean mOverviewProxyListenerRegistered;
    private final OverviewProxyService mOverviewProxyService;
    private boolean mTaskListenerRegistered;
    private final TextView mTextView;
    private final WindowManager mWindowManager;
    private final SysUiTaskStackChangeListener mTaskListener = new SysUiTaskStackChangeListener() { // from class: com.android.systemui.recents.RecentsOnboarding.1
        private String mLastPackageName;

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskCreated(int i, ComponentName componentName) {
            onAppLaunch();
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskMovedToFront(int i) {
            onAppLaunch();
        }

        private void onAppLaunch() {
            boolean show;
            boolean show2;
            ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask(0);
            if (runningTask != null) {
                if (RecentsOnboarding.this.mBlacklistedPackages.contains(runningTask.baseActivity.getPackageName())) {
                    RecentsOnboarding.this.hide(true);
                } else if (runningTask.baseActivity.getPackageName().equals(this.mLastPackageName)) {
                } else {
                    this.mLastPackageName = runningTask.baseActivity.getPackageName();
                    if (runningTask.configuration.windowConfiguration.getActivityType() == 1) {
                        boolean hasSeenSwipeUpOnboarding = RecentsOnboarding.this.hasSeenSwipeUpOnboarding();
                        boolean hasSeenQuickScrubOnboarding = RecentsOnboarding.this.hasSeenQuickScrubOnboarding();
                        if (hasSeenSwipeUpOnboarding && hasSeenQuickScrubOnboarding) {
                            RecentsOnboarding.this.onDisconnectedFromLauncher();
                            return;
                        }
                        int i = 10;
                        if (!hasSeenSwipeUpOnboarding) {
                            if (RecentsOnboarding.this.getOpenedOverviewFromHomeCount() >= 3) {
                                if (RecentsOnboarding.this.mHasDismissedSwipeUpTip) {
                                    int dismissedSwipeUpOnboardingCount = RecentsOnboarding.this.getDismissedSwipeUpOnboardingCount();
                                    if (dismissedSwipeUpOnboardingCount > 4) {
                                        return;
                                    }
                                    if (dismissedSwipeUpOnboardingCount <= 2) {
                                        i = 5;
                                    }
                                    RecentsOnboarding.access$608(RecentsOnboarding.this);
                                    if (RecentsOnboarding.this.mNumAppsLaunchedSinceSwipeUpTipDismiss >= i) {
                                        RecentsOnboarding.this.mNumAppsLaunchedSinceSwipeUpTipDismiss = 0;
                                        show2 = RecentsOnboarding.this.show(R.string.recents_swipe_up_onboarding);
                                    } else {
                                        show2 = false;
                                    }
                                } else {
                                    show2 = RecentsOnboarding.this.show(R.string.recents_swipe_up_onboarding);
                                }
                                if (show2) {
                                    RecentsOnboarding.this.notifyOnTip(0, 0);
                                    return;
                                }
                                return;
                            }
                            return;
                        } else if (RecentsOnboarding.this.getOpenedOverviewCount() >= 10) {
                            if (RecentsOnboarding.this.mHasDismissedQuickScrubTip) {
                                if (RecentsOnboarding.this.mOverviewOpenedCountSinceQuickScrubTipDismiss >= 10) {
                                    RecentsOnboarding.this.mOverviewOpenedCountSinceQuickScrubTipDismiss = 0;
                                    show = RecentsOnboarding.this.show(R.string.recents_quick_scrub_onboarding);
                                } else {
                                    show = false;
                                }
                            } else {
                                show = RecentsOnboarding.this.show(R.string.recents_quick_scrub_onboarding);
                            }
                            if (show) {
                                RecentsOnboarding.this.notifyOnTip(0, 1);
                                return;
                            }
                            return;
                        } else {
                            return;
                        }
                    }
                    RecentsOnboarding.this.hide(false);
                }
            }
        }
    };
    private OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() { // from class: com.android.systemui.recents.RecentsOnboarding.2
        @Override // com.android.systemui.OverviewProxyService.OverviewProxyListener
        public void onOverviewShown(boolean z) {
            if (!RecentsOnboarding.this.hasSeenSwipeUpOnboarding() && !z) {
                RecentsOnboarding.this.setHasSeenSwipeUpOnboarding(true);
            }
            if (z) {
                RecentsOnboarding.this.incrementOpenedOverviewFromHomeCount();
            }
            RecentsOnboarding.this.incrementOpenedOverviewCount();
            if (RecentsOnboarding.this.getOpenedOverviewCount() >= 10 && RecentsOnboarding.this.mHasDismissedQuickScrubTip) {
                RecentsOnboarding.access$1008(RecentsOnboarding.this);
            }
        }

        @Override // com.android.systemui.OverviewProxyService.OverviewProxyListener
        public void onQuickStepStarted() {
            RecentsOnboarding.this.hide(true);
        }

        @Override // com.android.systemui.OverviewProxyService.OverviewProxyListener
        public void onQuickScrubStarted() {
            if (!RecentsOnboarding.this.hasSeenQuickScrubOnboarding()) {
                RecentsOnboarding.this.setHasSeenQuickScrubOnboarding(true);
            }
        }
    };
    private final View.OnAttachStateChangeListener mOnAttachStateChangeListener = new View.OnAttachStateChangeListener() { // from class: com.android.systemui.recents.RecentsOnboarding.3
        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View view) {
            if (view == RecentsOnboarding.this.mLayout) {
                RecentsOnboarding.this.mContext.registerReceiver(RecentsOnboarding.this.mReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
                RecentsOnboarding.this.mLayoutAttachedToWindow = true;
                if (view.getTag().equals(Integer.valueOf((int) R.string.recents_swipe_up_onboarding))) {
                    RecentsOnboarding.this.mHasDismissedSwipeUpTip = false;
                } else {
                    RecentsOnboarding.this.mHasDismissedQuickScrubTip = false;
                }
            }
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View view) {
            if (view == RecentsOnboarding.this.mLayout) {
                RecentsOnboarding.this.mLayoutAttachedToWindow = false;
                if (view.getTag().equals(Integer.valueOf((int) R.string.recents_quick_scrub_onboarding))) {
                    RecentsOnboarding.this.mHasDismissedQuickScrubTip = true;
                    if (RecentsOnboarding.this.hasDismissedQuickScrubOnboardingOnce()) {
                        RecentsOnboarding.this.setHasSeenQuickScrubOnboarding(true);
                    } else {
                        RecentsOnboarding.this.setHasDismissedQuickScrubOnboardingOnce(true);
                    }
                    RecentsOnboarding.this.mOverviewOpenedCountSinceQuickScrubTipDismiss = 0;
                }
                RecentsOnboarding.this.mContext.unregisterReceiver(RecentsOnboarding.this.mReceiver);
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.systemui.recents.RecentsOnboarding.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                RecentsOnboarding.this.hide(false);
            }
        }
    };

    static /* synthetic */ int access$1008(RecentsOnboarding recentsOnboarding) {
        int i = recentsOnboarding.mOverviewOpenedCountSinceQuickScrubTipDismiss;
        recentsOnboarding.mOverviewOpenedCountSinceQuickScrubTipDismiss = i + 1;
        return i;
    }

    static /* synthetic */ int access$608(RecentsOnboarding recentsOnboarding) {
        int i = recentsOnboarding.mNumAppsLaunchedSinceSwipeUpTipDismiss;
        recentsOnboarding.mNumAppsLaunchedSinceSwipeUpTipDismiss = i + 1;
        return i;
    }

    public RecentsOnboarding(Context context, OverviewProxyService overviewProxyService) {
        this.mContext = context;
        this.mOverviewProxyService = overviewProxyService;
        Resources resources = context.getResources();
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mBlacklistedPackages = new HashSet();
        Collections.addAll(this.mBlacklistedPackages, resources.getStringArray(R.array.recents_onboarding_blacklisted_packages));
        this.mLayout = LayoutInflater.from(this.mContext).inflate(R.layout.recents_onboarding, (ViewGroup) null);
        this.mTextView = (TextView) this.mLayout.findViewById(R.id.onboarding_text);
        this.mDismissView = (ImageView) this.mLayout.findViewById(R.id.dismiss);
        this.mArrowView = this.mLayout.findViewById(R.id.arrow);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(16843829, typedValue, true);
        this.mOnboardingToastColor = resources.getColor(typedValue.resourceId);
        this.mOnboardingToastArrowRadius = resources.getDimensionPixelSize(R.dimen.recents_onboarding_toast_arrow_corner_radius);
        this.mLayout.addOnAttachStateChangeListener(this.mOnAttachStateChangeListener);
        this.mDismissView.setOnClickListener(new View.OnClickListener() { // from class: com.android.systemui.recents.-$$Lambda$RecentsOnboarding$VU_OZtWyvAx7bVWSUdhKQFeocZE
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                RecentsOnboarding.lambda$new$0(RecentsOnboarding.this, view);
            }
        });
        ViewGroup.LayoutParams layoutParams = this.mArrowView.getLayoutParams();
        ShapeDrawable shapeDrawable = new ShapeDrawable(TriangleShape.create(layoutParams.width, layoutParams.height, false));
        Paint paint = shapeDrawable.getPaint();
        paint.setColor(this.mOnboardingToastColor);
        paint.setPathEffect(new CornerPathEffect(this.mOnboardingToastArrowRadius));
        this.mArrowView.setBackground(shapeDrawable);
    }

    public static /* synthetic */ void lambda$new$0(RecentsOnboarding recentsOnboarding, View view) {
        recentsOnboarding.hide(true);
        if (view.getTag().equals(Integer.valueOf((int) R.string.recents_swipe_up_onboarding))) {
            recentsOnboarding.mHasDismissedSwipeUpTip = true;
            recentsOnboarding.mNumAppsLaunchedSinceSwipeUpTipDismiss = 0;
            recentsOnboarding.setDismissedSwipeUpOnboardingCount(recentsOnboarding.getDismissedSwipeUpOnboardingCount() + 1);
            if (recentsOnboarding.getDismissedSwipeUpOnboardingCount() > 4) {
                recentsOnboarding.setHasSeenSwipeUpOnboarding(true);
            }
            recentsOnboarding.notifyOnTip(1, 0);
            return;
        }
        recentsOnboarding.notifyOnTip(1, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyOnTip(int i, int i2) {
        try {
            IOverviewProxy proxy = this.mOverviewProxyService.getProxy();
            if (proxy != null) {
                proxy.onTip(i, i2);
            }
        } catch (RemoteException e) {
        }
    }

    public void onConnectedToLauncher() {
        if (hasSeenSwipeUpOnboarding() && hasSeenQuickScrubOnboarding()) {
            return;
        }
        if (!this.mOverviewProxyListenerRegistered) {
            this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
            this.mOverviewProxyListenerRegistered = true;
        }
        if (!this.mTaskListenerRegistered) {
            ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskListener);
            this.mTaskListenerRegistered = true;
        }
    }

    public void onDisconnectedFromLauncher() {
        if (this.mOverviewProxyListenerRegistered) {
            this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
            this.mOverviewProxyListenerRegistered = false;
        }
        if (this.mTaskListenerRegistered) {
            ActivityManagerWrapper.getInstance().unregisterTaskStackListener(this.mTaskListener);
            this.mTaskListenerRegistered = false;
        }
        this.mHasDismissedSwipeUpTip = false;
        this.mHasDismissedQuickScrubTip = false;
        this.mNumAppsLaunchedSinceSwipeUpTipDismiss = 0;
        this.mOverviewOpenedCountSinceQuickScrubTipDismiss = 0;
        hide(true);
    }

    public void onConfigurationChanged(Configuration configuration) {
        if (configuration.orientation != 1) {
            hide(false);
        }
    }

    public boolean show(int i) {
        int i2;
        int i3 = 0;
        if (shouldShow()) {
            this.mDismissView.setTag(Integer.valueOf(i));
            this.mLayout.setTag(Integer.valueOf(i));
            this.mTextView.setText(i);
            int i4 = this.mContext.getResources().getConfiguration().orientation;
            if (this.mLayoutAttachedToWindow || i4 != 1) {
                return false;
            }
            this.mLayout.setSystemUiVisibility(256);
            if (i == R.string.recents_swipe_up_onboarding) {
                i2 = 81;
            } else {
                i2 = (this.mContext.getResources().getConfiguration().getLayoutDirection() == 0 ? 3 : 5) | 80;
                i3 = this.mContext.getResources().getDimensionPixelSize(R.dimen.recents_quick_scrub_onboarding_margin_start);
            }
            this.mWindowManager.addView(this.mLayout, getWindowLayoutParams(i2, i3));
            this.mLayout.setAlpha(0.0f);
            this.mLayout.animate().alpha(1.0f).withLayer().setStartDelay(500L).setDuration(300L).setInterpolator(new DecelerateInterpolator()).start();
            return true;
        }
        return false;
    }

    private boolean shouldShow() {
        return SystemProperties.getBoolean("persist.quickstep.onboarding.enabled", (((UserManager) this.mContext.getSystemService(UserManager.class)).isDemoUser() || ActivityManager.isRunningInTestHarness()) ? false : true);
    }

    public void hide(boolean z) {
        if (this.mLayoutAttachedToWindow) {
            if (z) {
                this.mLayout.animate().alpha(0.0f).withLayer().setStartDelay(0L).setDuration(100L).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() { // from class: com.android.systemui.recents.-$$Lambda$RecentsOnboarding$qki5o8zqrWEPaWaslagffDePdhg
                    @Override // java.lang.Runnable
                    public final void run() {
                        r0.mWindowManager.removeViewImmediate(RecentsOnboarding.this.mLayout);
                    }
                }).start();
                return;
            }
            this.mLayout.animate().cancel();
            this.mWindowManager.removeViewImmediate(this.mLayout);
        }
    }

    public void setNavBarHeight(int i) {
        this.mNavBarHeight = i;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("RecentsOnboarding {");
        printWriter.println("      mTaskListenerRegistered: " + this.mTaskListenerRegistered);
        printWriter.println("      mOverviewProxyListenerRegistered: " + this.mOverviewProxyListenerRegistered);
        printWriter.println("      mLayoutAttachedToWindow: " + this.mLayoutAttachedToWindow);
        printWriter.println("      mHasDismissedSwipeUpTip: " + this.mHasDismissedSwipeUpTip);
        printWriter.println("      mHasDismissedQuickScrubTip: " + this.mHasDismissedQuickScrubTip);
        printWriter.println("      mNumAppsLaunchedSinceSwipeUpTipDismiss: " + this.mNumAppsLaunchedSinceSwipeUpTipDismiss);
        printWriter.println("      hasSeenSwipeUpOnboarding: " + hasSeenSwipeUpOnboarding());
        printWriter.println("      hasSeenQuickScrubOnboarding: " + hasSeenQuickScrubOnboarding());
        printWriter.println("      getDismissedSwipeUpOnboardingCount: " + getDismissedSwipeUpOnboardingCount());
        printWriter.println("      hasDismissedQuickScrubOnboardingOnce: " + hasDismissedQuickScrubOnboardingOnce());
        printWriter.println("      getOpenedOverviewCount: " + getOpenedOverviewCount());
        printWriter.println("      getOpenedOverviewFromHomeCount: " + getOpenedOverviewFromHomeCount());
        printWriter.println("    }");
    }

    private WindowManager.LayoutParams getWindowLayoutParams(int i, int i2) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, i2, (-this.mNavBarHeight) / 2, 2024, 520, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("RecentsOnboarding");
        layoutParams.gravity = i;
        return layoutParams;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasSeenSwipeUpOnboarding() {
        return Prefs.getBoolean(this.mContext, "HasSeenRecentsSwipeUpOnboarding", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHasSeenSwipeUpOnboarding(boolean z) {
        Prefs.putBoolean(this.mContext, "HasSeenRecentsSwipeUpOnboarding", z);
        if (z && hasSeenQuickScrubOnboarding()) {
            onDisconnectedFromLauncher();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasSeenQuickScrubOnboarding() {
        return Prefs.getBoolean(this.mContext, "HasSeenRecentsQuickScrubOnboarding", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHasSeenQuickScrubOnboarding(boolean z) {
        Prefs.putBoolean(this.mContext, "HasSeenRecentsQuickScrubOnboarding", z);
        if (z && hasSeenSwipeUpOnboarding()) {
            onDisconnectedFromLauncher();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getDismissedSwipeUpOnboardingCount() {
        return Prefs.getInt(this.mContext, "DismissedRecentsSwipeUpOnboardingCount", 0);
    }

    private void setDismissedSwipeUpOnboardingCount(int i) {
        Prefs.putInt(this.mContext, "DismissedRecentsSwipeUpOnboardingCount", i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasDismissedQuickScrubOnboardingOnce() {
        return Prefs.getBoolean(this.mContext, "HasDismissedRecentsQuickScrubOnboardingOnce", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHasDismissedQuickScrubOnboardingOnce(boolean z) {
        Prefs.putBoolean(this.mContext, "HasDismissedRecentsQuickScrubOnboardingOnce", z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getOpenedOverviewFromHomeCount() {
        return Prefs.getInt(this.mContext, "OverviewOpenedFromHomeCount", 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void incrementOpenedOverviewFromHomeCount() {
        int openedOverviewFromHomeCount = getOpenedOverviewFromHomeCount();
        if (openedOverviewFromHomeCount >= 3) {
            return;
        }
        setOpenedOverviewFromHomeCount(openedOverviewFromHomeCount + 1);
    }

    private void setOpenedOverviewFromHomeCount(int i) {
        Prefs.putInt(this.mContext, "OverviewOpenedFromHomeCount", i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getOpenedOverviewCount() {
        return Prefs.getInt(this.mContext, "OverviewOpenedCount", 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void incrementOpenedOverviewCount() {
        int openedOverviewCount = getOpenedOverviewCount();
        if (openedOverviewCount >= 10) {
            return;
        }
        setOpenedOverviewCount(openedOverviewCount + 1);
    }

    private void setOpenedOverviewCount(int i) {
        Prefs.putInt(this.mContext, "OverviewOpenedCount", i);
    }
}
