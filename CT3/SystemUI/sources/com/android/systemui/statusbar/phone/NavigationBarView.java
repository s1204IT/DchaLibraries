package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.IDockedStackListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.policy.DeadZone;
import com.mediatek.common.MPlugin;
import com.mediatek.multiwindow.IFreeformStackListener;
import com.mediatek.multiwindow.MultiWindowManager;
import com.mediatek.systemui.ext.DefaultNavigationBarPlugin;
import com.mediatek.systemui.ext.INavigationBarPlugin;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class NavigationBarView extends LinearLayout {
    private Drawable mBackAltCarModeIcon;
    private Drawable mBackAltIcon;
    private Drawable mBackAltLandCarModeIcon;
    private Drawable mBackAltLandIcon;
    private Drawable mBackCarModeIcon;
    private Drawable mBackIcon;
    private Drawable mBackLandCarModeIcon;
    private Drawable mBackLandIcon;
    private final NavigationBarTransitions mBarTransitions;
    private final SparseArray<ButtonDispatcher> mButtonDisatchers;
    private boolean mCarMode;
    private Configuration mConfiguration;
    View mCurrentView;
    private DeadZone mDeadZone;
    int mDisabledFlags;
    final Display mDisplay;
    private Drawable mDockedIcon;
    private boolean mDockedStackExists;
    private NavigationBarGestureHelper mGestureHelper;
    private H mHandler;
    private Drawable mHomeCarModeIcon;
    private Drawable mHomeDefaultIcon;
    private Drawable mImeIcon;
    private final View.OnClickListener mImeSwitcherClickListener;
    private KeyguardViewMediator mKeyguardViewMediator;
    private boolean mLayoutTransitionsEnabled;
    private Drawable mMenuIcon;
    private INavigationBarPlugin mNavBarPlugin;
    int mNavigationIconHints;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private Drawable mRecentIcon;
    private boolean mResizeMode;
    private Drawable mRestoreIcon;
    private boolean mRestoreShow;
    View[] mRotatedViews;
    boolean mScreenOn;
    boolean mShowMenu;
    private final NavTransitionListener mTransitionListener;
    boolean mVertical;
    private boolean mWakeAndUnlocking;

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    private class NavTransitionListener implements LayoutTransition.TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        NavTransitionListener(NavigationBarView this$0, NavTransitionListener navTransitionListener) {
            this();
        }

        private NavTransitionListener() {
        }

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = true;
                return;
            }
            if (view.getId() != R.id.home || transitionType != 2) {
                return;
            }
            this.mHomeAppearing = true;
            this.mStartDelay = transition.getStartDelay(transitionType);
            this.mDuration = transition.getDuration(transitionType);
            this.mInterpolator = transition.getInterpolator(transitionType);
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = false;
            } else {
                if (view.getId() != R.id.home || transitionType != 2) {
                    return;
                }
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            ButtonDispatcher backButton = NavigationBarView.this.getBackButton();
            if (this.mBackTransitioning || backButton.getVisibility() != 0 || !this.mHomeAppearing || NavigationBarView.this.getHomeButton().getAlpha() != 0.0f) {
                return;
            }
            NavigationBarView.this.getBackButton().setAlpha(0);
            ValueAnimator a = ObjectAnimator.ofFloat(backButton, "alpha", 0.0f, 1.0f);
            a.setStartDelay(this.mStartDelay);
            a.setDuration(this.mDuration);
            a.setInterpolator(this.mInterpolator);
            a.start();
        }
    }

    private class H extends Handler {
        H(NavigationBarView this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case 8686:
                    String how = "" + m.obj;
                    int w = NavigationBarView.this.getWidth();
                    int h = NavigationBarView.this.getHeight();
                    int vw = NavigationBarView.this.getCurrentView().getWidth();
                    int vh = NavigationBarView.this.getCurrentView().getHeight();
                    if (h != vh || w != vw) {
                        Log.w("PhoneStatusBar/NavigationBarView", String.format("*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)", how, Integer.valueOf(w), Integer.valueOf(h), Integer.valueOf(vw), Integer.valueOf(vh)));
                        NavigationBarView.this.requestLayout();
                    }
                    break;
            }
        }
    }

    public NavigationBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentView = null;
        this.mRotatedViews = new View[4];
        this.mDisabledFlags = 0;
        this.mNavigationIconHints = 0;
        this.mTransitionListener = new NavTransitionListener(this, null);
        this.mLayoutTransitionsEnabled = true;
        this.mCarMode = false;
        this.mButtonDisatchers = new SparseArray<>();
        this.mImeSwitcherClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((InputMethodManager) NavigationBarView.this.mContext.getSystemService(InputMethodManager.class)).showInputMethodPicker(true);
            }
        };
        this.mHandler = new H(this, 0 == true ? 1 : 0);
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        this.mVertical = false;
        this.mShowMenu = false;
        this.mGestureHelper = new NavigationBarGestureHelper(context);
        this.mConfiguration = new Configuration();
        this.mConfiguration.updateFrom(context.getResources().getConfiguration());
        updateIcons(context, Configuration.EMPTY, this.mConfiguration);
        try {
            this.mNavBarPlugin = (INavigationBarPlugin) MPlugin.createInstance(INavigationBarPlugin.class.getName(), context);
        } catch (Exception e) {
            Log.e("PhoneStatusBar/NavigationBarView", "Catch INavigationBarPlugin exception: ", e);
        }
        if (this.mNavBarPlugin == null) {
            Log.d("PhoneStatusBar/NavigationBarView", "DefaultNavigationBarPlugin");
            this.mNavBarPlugin = new DefaultNavigationBarPlugin(context);
        }
        this.mBarTransitions = new NavigationBarTransitions(this);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                NavigationBarView.this.setDisabledFlags(NavigationBarView.this.mDisabledFlags, true);
            }
        }, -1);
        this.mButtonDisatchers.put(R.id.back, new ButtonDispatcher(R.id.back));
        this.mButtonDisatchers.put(R.id.home, new ButtonDispatcher(R.id.home));
        this.mButtonDisatchers.put(R.id.recent_apps, new ButtonDispatcher(R.id.recent_apps));
        if (MultiWindowManager.isSupported()) {
            this.mButtonDisatchers.put(R.id.restore, new ButtonDispatcher(R.id.restore));
            this.mKeyguardViewMediator = (KeyguardViewMediator) ((SystemUIApplication) context).getComponent(KeyguardViewMediator.class);
        }
        this.mButtonDisatchers.put(R.id.menu, new ButtonDispatcher(R.id.menu));
        this.mButtonDisatchers.put(R.id.ime_switcher, new ButtonDispatcher(R.id.ime_switcher));
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setComponents(RecentsComponent recentsComponent, Divider divider) {
        this.mGestureHelper.setComponents(recentsComponent, divider, this);
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        this.mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(this.mVertical);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mGestureHelper.onTouchEvent(event)) {
            return true;
        }
        if (this.mDeadZone != null && event.getAction() == 4) {
            this.mDeadZone.poke(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mGestureHelper.onInterceptTouchEvent(event);
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public ButtonDispatcher getRecentsButton() {
        return this.mButtonDisatchers.get(R.id.recent_apps);
    }

    public ButtonDispatcher getMenuButton() {
        return this.mButtonDisatchers.get(R.id.menu);
    }

    public ButtonDispatcher getBackButton() {
        return this.mButtonDisatchers.get(R.id.back);
    }

    public ButtonDispatcher getHomeButton() {
        return this.mButtonDisatchers.get(R.id.home);
    }

    public ButtonDispatcher getImeSwitchButton() {
        return this.mButtonDisatchers.get(R.id.ime_switcher);
    }

    public ButtonDispatcher getRestoreButton() {
        return this.mButtonDisatchers.get(R.id.restore);
    }

    private void updateCarModeIcons(Context ctx) {
        this.mBackCarModeIcon = ctx.getDrawable(R.drawable.ic_sysbar_back_carmode);
        this.mBackLandCarModeIcon = this.mBackCarModeIcon;
        this.mBackAltCarModeIcon = ctx.getDrawable(R.drawable.ic_sysbar_back_ime_carmode);
        this.mBackAltLandCarModeIcon = this.mBackAltCarModeIcon;
        this.mHomeCarModeIcon = ctx.getDrawable(R.drawable.ic_sysbar_home_carmode);
    }

    private void updateIcons(Context ctx, Configuration oldConfig, Configuration newConfig) {
        if (oldConfig.orientation != newConfig.orientation || oldConfig.densityDpi != newConfig.densityDpi) {
            this.mDockedIcon = ctx.getDrawable(R.drawable.ic_sysbar_docked);
        }
        if (oldConfig.densityDpi == newConfig.densityDpi) {
            return;
        }
        this.mBackIcon = ctx.getDrawable(R.drawable.ic_sysbar_back);
        this.mBackLandIcon = this.mBackIcon;
        this.mBackAltIcon = ctx.getDrawable(R.drawable.ic_sysbar_back_ime);
        this.mBackAltLandIcon = this.mBackAltIcon;
        this.mHomeDefaultIcon = ctx.getDrawable(R.drawable.ic_sysbar_home);
        this.mRecentIcon = ctx.getDrawable(R.drawable.ic_sysbar_recent);
        this.mMenuIcon = ctx.getDrawable(R.drawable.ic_sysbar_menu);
        this.mImeIcon = ctx.getDrawable(R.drawable.ic_ime_switcher_default);
        if (MultiWindowManager.isSupported()) {
            this.mRestoreIcon = ctx.getDrawable(R.drawable.ic_sysbar_restore);
        }
        updateCarModeIcons(ctx);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        updateIcons(getContext(), Configuration.EMPTY, this.mConfiguration);
        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        this.mScreenOn = screenOn;
        setDisabledFlags(this.mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    private Drawable getBackIconWithAlt(boolean carMode, boolean landscape) {
        return landscape ? carMode ? this.mBackAltLandCarModeIcon : this.mBackAltLandIcon : carMode ? this.mBackAltCarModeIcon : this.mBackAltIcon;
    }

    private Drawable getBackIcon(boolean carMode, boolean landscape) {
        return landscape ? carMode ? this.mBackLandCarModeIcon : this.mBackLandIcon : carMode ? this.mBackCarModeIcon : this.mBackIcon;
    }

    public void setNavigationIconHints(int hints, boolean force) {
        Drawable backIcon;
        if (force || hints != this.mNavigationIconHints) {
            boolean backAlt = (hints & 1) != 0;
            if ((this.mNavigationIconHints & 1) != 0 && !backAlt) {
                this.mTransitionListener.onBackAltCleared();
            }
            this.mNavigationIconHints = hints;
            if (backAlt) {
                backIcon = getBackIconWithAlt(this.mCarMode, this.mVertical);
            } else {
                backIcon = getBackIcon(this.mCarMode, this.mVertical);
            }
            getBackButton().setImageDrawable(this.mNavBarPlugin.getBackImage(backIcon));
            updateRecentsIcon();
            if (MultiWindowManager.isSupported()) {
                updateRestoreIcon();
            }
            if (this.mCarMode) {
                getHomeButton().setImageDrawable(this.mHomeCarModeIcon);
            } else {
                getHomeButton().setImageDrawable(this.mNavBarPlugin.getHomeImage(this.mHomeDefaultIcon));
            }
            boolean showImeButton = (hints & 2) != 0;
            getImeSwitchButton().setVisibility(showImeButton ? 0 : 4);
            getImeSwitchButton().setImageDrawable(this.mImeIcon);
            setMenuVisibility(this.mShowMenu, true);
            getMenuButton().setImageDrawable(this.mMenuIcon);
            setDisabledFlags(this.mDisabledFlags, true);
        }
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        LayoutTransition lt;
        int i = 4;
        boolean z = false;
        if (force || this.mDisabledFlags != disabledFlags) {
            this.mDisabledFlags = disabledFlags;
            boolean disableHome = (2097152 & disabledFlags) != 0;
            boolean disableRecent = this.mCarMode || (16777216 & disabledFlags) != 0;
            boolean disableBack = (4194304 & disabledFlags) != 0 && (this.mNavigationIconHints & 1) == 0;
            boolean disableSearch = (33554432 & disabledFlags) != 0;
            if (!disableHome || !disableRecent || !disableBack) {
                disableSearch = false;
            }
            setSlippery(disableSearch);
            ViewGroup navButtons = (ViewGroup) getCurrentView().findViewById(R.id.nav_buttons);
            if (navButtons != null && (lt = navButtons.getLayoutTransition()) != null && !lt.getTransitionListeners().contains(this.mTransitionListener)) {
                lt.addTransitionListener(this.mTransitionListener);
            }
            if (inLockTask() && disableRecent && !disableHome) {
                disableRecent = false;
            }
            getBackButton().setVisibility(disableBack ? 4 : 0);
            getHomeButton().setVisibility(disableHome ? 4 : 0);
            ButtonDispatcher recentsButton = getRecentsButton();
            if (BenesseExtension.getDchaState() == 0 && !disableRecent) {
                i = 0;
            }
            recentsButton.setVisibility(i);
            if (!MultiWindowManager.isSupported() || this.mKeyguardViewMediator == null) {
                return;
            }
            boolean isKeyguardShowing = this.mKeyguardViewMediator.isShowing();
            if (this.mRestoreShow && !isKeyguardShowing) {
                z = true;
            }
            this.mResizeMode = z;
            updateRestoreIcon();
        }
    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setLayoutTransitionsEnabled(boolean enabled) {
        this.mLayoutTransitionsEnabled = enabled;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        this.mWakeAndUnlocking = wakeAndUnlocking;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean z = !this.mWakeAndUnlocking ? this.mLayoutTransitionsEnabled : false;
        ViewGroup navButtons = (ViewGroup) getCurrentView().findViewById(R.id.nav_buttons);
        LayoutTransition lt = navButtons.getLayoutTransition();
        if (lt == null) {
            return;
        }
        if (z) {
            lt.enableTransitionType(2);
            lt.enableTransitionType(3);
            lt.enableTransitionType(0);
            lt.enableTransitionType(1);
            return;
        }
        lt.disableTransitionType(2);
        lt.disableTransitionType(3);
        lt.disableTransitionType(0);
        lt.disableTransitionType(1);
    }

    private void setUseFadingAnimations(boolean useFadingAnimations) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp == null) {
            return;
        }
        boolean old = lp.windowAnimations != 0;
        if (!old && useFadingAnimations) {
            lp.windowAnimations = R.style.Animation_NavigationBarFadeIn;
        } else if (old && !useFadingAnimations) {
            lp.windowAnimations = 0;
        } else {
            return;
        }
        WindowManager wm = (WindowManager) getContext().getSystemService("window");
        wm.updateViewLayout(this, lp);
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp == null) {
            return;
        }
        boolean oldSlippery = (lp.flags & 536870912) != 0;
        if (!oldSlippery && newSlippery) {
            lp.flags |= 536870912;
        } else if (oldSlippery && !newSlippery) {
            lp.flags &= -536870913;
        } else {
            return;
        }
        WindowManager wm = (WindowManager) getContext().getSystemService("window");
        wm.updateViewLayout(this, lp);
    }

    public void setMenuVisibility(boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(boolean show, boolean force) {
        if (force || this.mShowMenu != show) {
            this.mShowMenu = show;
            boolean shouldShow = this.mShowMenu && (this.mNavigationIconHints & 2) == 0;
            getMenuButton().setVisibility(shouldShow ? 0 : 4);
        }
    }

    @Override
    public void onFinishInflate() {
        updateRotatedViews();
        ((NavigationBarInflaterView) findViewById(R.id.navigation_inflater)).setButtonDispatchers(this.mButtonDisatchers);
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new IDockedStackListener.Stub() {
                public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
                }

                public void onDockedStackExistsChanged(final boolean exists) throws RemoteException {
                    NavigationBarView.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NavigationBarView.this.mDockedStackExists = exists;
                            NavigationBarView.this.updateRecentsIcon();
                        }
                    });
                }

                public void onDockedStackMinimizedChanged(boolean minimized, long animDuration) throws RemoteException {
                }

                public void onAdjustedForImeChanged(boolean adjustedForIme, long animDuration) throws RemoteException {
                }

                public void onDockSideChanged(int newDockSide) throws RemoteException {
                }
            });
        } catch (RemoteException e) {
            Log.e("PhoneStatusBar/NavigationBarView", "Failed registering docked stack exists listener", e);
        }
        if (!MultiWindowManager.isSupported()) {
            return;
        }
        try {
            WindowManagerGlobal.getWindowManagerService().registerFreeformStackListener(new IFreeformStackListener.Stub() {
                public void onShowRestoreButtonChanged(final boolean isShown) throws RemoteException {
                    NavigationBarView.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean z = false;
                            boolean isKeyguardShowing = false;
                            if (NavigationBarView.this.mKeyguardViewMediator != null) {
                                isKeyguardShowing = NavigationBarView.this.mKeyguardViewMediator.isShowing();
                            }
                            NavigationBarView.this.mRestoreShow = isShown;
                            NavigationBarView navigationBarView = NavigationBarView.this;
                            if (isShown && !isKeyguardShowing) {
                                z = true;
                            }
                            navigationBarView.mResizeMode = z;
                            NavigationBarView.this.updateRestoreIcon();
                        }
                    });
                }
            });
        } catch (RemoteException e2) {
            Log.e("PhoneStatusBar/NavigationBarView", "Failed registering freeform stack exists listener", e2);
        }
    }

    public void updateRestoreIcon() {
        if (MultiWindowManager.DEBUG) {
            Log.d("PhoneStatusBar/NavigationBarView", "BMW, updateRestoreIcon, mResizeMode = " + this.mResizeMode);
        }
        getRestoreButton().setImageDrawable(this.mRestoreIcon);
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.freeform_window_management")) {
            getRestoreButton().setVisibility(this.mResizeMode ? 0 : 4);
        } else {
            getRestoreButton().setVisibility(4);
        }
    }

    void updateRotatedViews() {
        View[] viewArr = this.mRotatedViews;
        View viewFindViewById = findViewById(R.id.rot0);
        this.mRotatedViews[2] = viewFindViewById;
        viewArr[0] = viewFindViewById;
        View[] viewArr2 = this.mRotatedViews;
        View viewFindViewById2 = findViewById(R.id.rot90);
        this.mRotatedViews[1] = viewFindViewById2;
        viewArr2[3] = viewFindViewById2;
        updateCurrentView();
    }

    private void updateCurrentView() {
        int rot = this.mDisplay.getRotation();
        for (int i = 0; i < 4; i++) {
            this.mRotatedViews[i].setVisibility(8);
        }
        this.mCurrentView = this.mRotatedViews[rot];
        this.mCurrentView.setVisibility(0);
        for (int i2 = 0; i2 < this.mButtonDisatchers.size(); i2++) {
            this.mButtonDisatchers.valueAt(i2).setCurrentView(this.mCurrentView);
        }
        updateLayoutTransitionsEnabled();
    }

    public void updateRecentsIcon() {
        getRecentsButton().setImageDrawable(this.mNavBarPlugin.getRecentImage(this.mDockedStackExists ? this.mDockedIcon : this.mRecentIcon));
    }

    public void reorient() {
        updateCurrentView();
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        this.mDeadZone = (DeadZone) this.mCurrentView.findViewById(R.id.deadzone);
        this.mBarTransitions.init();
        setDisabledFlags(this.mDisabledFlags, true);
        setMenuVisibility(this.mShowMenu, true);
        updateTaskSwitchHelper();
        setNavigationIconHints(this.mNavigationIconHints, true);
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = getLayoutDirection() == 1;
        this.mGestureHelper.setBarState(this.mVertical, isRtl);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        boolean newVertical = w > 0 && h > w;
        if (newVertical != this.mVertical) {
            this.mVertical = newVertical;
            reorient();
            notifyVerticalChangedListener(newVertical);
        }
        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (this.mOnVerticalChangedListener == null) {
            return;
        }
        this.mOnVerticalChangedListener.onVerticalChanged(newVertical);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean uiCarModeChanged = updateCarMode(newConfig);
        updateTaskSwitchHelper();
        updateIcons(getContext(), this.mConfiguration, newConfig);
        updateRecentsIcon();
        if (uiCarModeChanged || this.mConfiguration.densityDpi != newConfig.densityDpi) {
            setNavigationIconHints(this.mNavigationIconHints, true);
        }
        this.mConfiguration.updateFrom(newConfig);
    }

    private boolean updateCarMode(Configuration newConfig) {
        if (newConfig == null) {
            return false;
        }
        int uiMode = newConfig.uiMode & 15;
        if (this.mCarMode && uiMode != 3) {
            this.mCarMode = false;
            return true;
        }
        if (uiMode != 3) {
            return false;
        }
        this.mCarMode = true;
        return true;
    }

    private String getResourceName(int resId) {
        if (resId != 0) {
            Resources res = getContext().getResources();
            try {
                return res.getResourceName(resId);
            } catch (Resources.NotFoundException e) {
                return "(unknown)";
            }
        }
        return "(null)";
    }

    private void postCheckForInvalidLayout(String how) {
        this.mHandler.obtainMessage(8686, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case 4:
                return "INVISIBLE";
            case 8:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        Rect r = new Rect();
        Point size = new Point();
        this.mDisplay.getRealSize(size);
        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this) + " " + visibilityToString(getVisibility()), new Object[0]));
        getWindowVisibleDisplayFrame(r);
        boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: " + r.toShortString() + " " + visibilityToString(getWindowVisibility()) + (offscreen ? " OFFSCREEN!" : ""));
        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s", getResourceName(getCurrentView().getId()), Integer.valueOf(getCurrentView().getWidth()), Integer.valueOf(getCurrentView().getHeight()), visibilityToString(getCurrentView().getVisibility())));
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(this.mDisabledFlags);
        objArr[1] = this.mVertical ? "true" : "false";
        objArr[2] = this.mShowMenu ? "true" : "false";
        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s", objArr));
        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());
        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, ButtonDispatcher button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(visibilityToString(button.getVisibility()) + " alpha=" + button.getAlpha());
        }
        pw.println();
    }
}
