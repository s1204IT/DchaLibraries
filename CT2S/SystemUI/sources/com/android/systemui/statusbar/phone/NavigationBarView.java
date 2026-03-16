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
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DelegateViewHelper;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NavigationBarView extends LinearLayout {
    private Drawable mBackAltIcon;
    private Drawable mBackAltLandIcon;
    private Drawable mBackIcon;
    private Drawable mBackLandIcon;
    int mBarSize;
    private final NavigationBarTransitions mBarTransitions;
    View mCurrentView;
    private DeadZone mDeadZone;
    private DelegateViewHelper mDelegateHelper;
    private boolean mDelegateIntercepted;
    int mDisabledFlags;
    final Display mDisplay;
    private H mHandler;
    private final View.OnClickListener mImeSwitcherClickListener;
    private boolean mIsLayoutRtl;
    int mNavigationIconHints;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;
    View[] mRotatedViews;
    boolean mScreenOn;
    boolean mShowMenu;
    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;
    private final NavTransitionListener mTransitionListener;
    boolean mVertical;

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    private class NavTransitionListener implements LayoutTransition.TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        private NavTransitionListener() {
        }

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = true;
                return;
            }
            if (view.getId() == R.id.home && transitionType == 2) {
                this.mHomeAppearing = true;
                this.mStartDelay = transition.getStartDelay(transitionType);
                this.mDuration = transition.getDuration(transitionType);
                this.mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == 2) {
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            if (!this.mBackTransitioning && NavigationBarView.this.getBackButton().getVisibility() == 0 && this.mHomeAppearing && NavigationBarView.this.getHomeButton().getAlpha() == 0.0f) {
                NavigationBarView.this.getBackButton().setAlpha(0.0f);
                ValueAnimator a = ObjectAnimator.ofFloat(NavigationBarView.this.getBackButton(), "alpha", 0.0f, 1.0f);
                a.setStartDelay(this.mStartDelay);
                a.setDuration(this.mDuration);
                a.setInterpolator(this.mInterpolator);
                a.start();
            }
        }
    }

    private class H extends Handler {
        private H() {
        }

        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case 8686:
                    String how = "" + m.obj;
                    int w = NavigationBarView.this.getWidth();
                    int h = NavigationBarView.this.getHeight();
                    int vw = NavigationBarView.this.mCurrentView.getWidth();
                    int vh = NavigationBarView.this.mCurrentView.getHeight();
                    if (h != vh || w != vw) {
                        Log.w("PhoneStatusBar/NavigationBarView", String.format("*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)", how, Integer.valueOf(w), Integer.valueOf(h), Integer.valueOf(vw), Integer.valueOf(vh)));
                        NavigationBarView.this.requestLayout();
                    }
                    break;
            }
        }
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentView = null;
        this.mRotatedViews = new View[4];
        this.mDisabledFlags = 0;
        this.mNavigationIconHints = 0;
        this.mTransitionListener = new NavTransitionListener();
        this.mImeSwitcherClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BenesseExtension.getDchaState() == 0) {
                    ((InputMethodManager) NavigationBarView.this.mContext.getSystemService("input_method")).showInputMethodPicker();
                }
            }
        };
        this.mHandler = new H();
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Resources res = getContext().getResources();
        this.mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        this.mVertical = false;
        this.mShowMenu = false;
        this.mDelegateHelper = new DelegateViewHelper(this);
        this.mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);
        getIcons(res);
        this.mBarTransitions = new NavigationBarTransitions(this);
        ContentObserver obs = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                NavigationBarView.this.setDisabledFlags(NavigationBarView.this.mDisabledFlags, true);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, obs, -1);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setDelegateView(View view) {
        this.mDelegateHelper.setDelegateView(view);
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        this.mTaskSwitchHelper.setBar(phoneStatusBar);
        this.mDelegateHelper.setBar(phoneStatusBar);
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        this.mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(this.mVertical);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initDownStates(event);
        if (!this.mDelegateIntercepted && this.mTaskSwitchHelper.onTouchEvent(event)) {
            return true;
        }
        if (this.mDeadZone != null && event.getAction() == 4) {
            this.mDeadZone.poke(event);
        }
        if (this.mDelegateHelper != null && this.mDelegateIntercepted) {
            boolean ret = this.mDelegateHelper.onInterceptTouchEvent(event);
            if (ret) {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mDelegateIntercepted = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        initDownStates(event);
        boolean intercept = this.mTaskSwitchHelper.onInterceptTouchEvent(event);
        if (!intercept) {
            this.mDelegateIntercepted = this.mDelegateHelper.onInterceptTouchEvent(event);
            return this.mDelegateIntercepted;
        }
        MotionEvent cancelEvent = MotionEvent.obtain(event);
        cancelEvent.setAction(3);
        this.mDelegateHelper.onInterceptTouchEvent(cancelEvent);
        cancelEvent.recycle();
        return intercept;
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public View getRecentsButton() {
        return this.mCurrentView.findViewById(R.id.recent_apps);
    }

    public View getMenuButton() {
        return this.mCurrentView.findViewById(R.id.menu);
    }

    public View getBackButton() {
        return this.mCurrentView.findViewById(R.id.back);
    }

    public View getHomeButton() {
        return this.mCurrentView.findViewById(R.id.home);
    }

    public View getImeSwitchButton() {
        return this.mCurrentView.findViewById(R.id.ime_switcher);
    }

    private void getIcons(Resources res) {
        this.mBackIcon = res.getDrawable(R.drawable.ic_sysbar_back);
        this.mBackLandIcon = res.getDrawable(R.drawable.ic_sysbar_back_land);
        this.mBackAltIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
        this.mBackAltLandIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
        this.mRecentIcon = res.getDrawable(R.drawable.ic_sysbar_recent);
        this.mRecentLandIcon = res.getDrawable(R.drawable.ic_sysbar_recent_land);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        getIcons(getContext().getResources());
        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        this.mScreenOn = screenOn;
        setDisabledFlags(this.mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        Drawable drawable;
        if (force || hints != this.mNavigationIconHints) {
            boolean backAlt = (hints & 1) != 0;
            if ((this.mNavigationIconHints & 1) != 0 && !backAlt) {
                this.mTransitionListener.onBackAltCleared();
            }
            this.mNavigationIconHints = hints;
            ImageView imageView = (ImageView) getBackButton();
            if (backAlt) {
                drawable = this.mVertical ? this.mBackAltLandIcon : this.mBackAltIcon;
            } else {
                drawable = this.mVertical ? this.mBackLandIcon : this.mBackIcon;
            }
            imageView.setImageDrawable(drawable);
            ((ImageView) getRecentsButton()).setImageDrawable(this.mVertical ? this.mRecentLandIcon : this.mRecentIcon);
            boolean showImeButton = (hints & 2) != 0;
            getImeSwitchButton().setVisibility(showImeButton ? 0 : 4);
            setMenuVisibility(this.mShowMenu, true);
            setDisabledFlags(this.mDisabledFlags, true);
        }
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        LayoutTransition lt;
        int i = 4;
        if (force || this.mDisabledFlags != disabledFlags) {
            this.mDisabledFlags = disabledFlags;
            boolean disableHome = (2097152 & disabledFlags) != 0;
            boolean disableRecent = (16777216 & disabledFlags) != 0;
            boolean disableBack = (4194304 & disabledFlags) != 0 && (this.mNavigationIconHints & 1) == 0;
            boolean disableSearch = (33554432 & disabledFlags) != 0;
            setSlippery(disableHome && disableRecent && disableBack && disableSearch);
            ViewGroup navButtons = (ViewGroup) this.mCurrentView.findViewById(R.id.nav_buttons);
            if (navButtons != null && (lt = navButtons.getLayoutTransition()) != null) {
                if (!lt.getTransitionListeners().contains(this.mTransitionListener)) {
                    lt.addTransitionListener(this.mTransitionListener);
                }
                if (!this.mScreenOn && this.mCurrentView != null) {
                    lt.disableTransitionType(3);
                }
            }
            if (inLockTask() && disableRecent && !disableHome) {
                disableRecent = false;
            }
            getBackButton().setVisibility(disableBack ? 4 : 0);
            getHomeButton().setVisibility(disableHome ? 4 : 0);
            View recentsButton = getRecentsButton();
            if (BenesseExtension.getDchaState() == 0 && !disableRecent) {
                i = 0;
            }
            recentsButton.setVisibility(i);
            this.mBarTransitions.applyBackButtonQuiescentAlpha(this.mBarTransitions.getMode(), true);
        }
    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
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
        View[] viewArr = this.mRotatedViews;
        View[] viewArr2 = this.mRotatedViews;
        View viewFindViewById = findViewById(R.id.rot0);
        viewArr2[2] = viewFindViewById;
        viewArr[0] = viewFindViewById;
        this.mRotatedViews[1] = findViewById(R.id.rot90);
        this.mRotatedViews[3] = this.mRotatedViews[1];
        this.mCurrentView = this.mRotatedViews[0];
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        updateRTLOrder();
    }

    public boolean isVertical() {
        return this.mVertical;
    }

    public void reorient() {
        int rot = this.mDisplay.getRotation();
        for (int i = 0; i < 4; i++) {
            this.mRotatedViews[i].setVisibility(8);
        }
        this.mCurrentView = this.mRotatedViews[rot];
        this.mCurrentView.setVisibility(0);
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        this.mDeadZone = (DeadZone) this.mCurrentView.findViewById(R.id.deadzone);
        this.mBarTransitions.init(this.mVertical);
        setDisabledFlags(this.mDisabledFlags, true);
        setMenuVisibility(this.mShowMenu, true);
        if (this.mDelegateHelper != null) {
            this.mDelegateHelper.setSwapXY(this.mVertical);
        }
        updateTaskSwitchHelper();
        setNavigationIconHints(this.mNavigationIconHints, true);
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = getLayoutDirection() == 1;
        this.mTaskSwitchHelper.setBarState(this.mVertical, isRtl);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mDelegateHelper.setInitialTouchRegion(getHomeButton(), getBackButton(), getRecentsButton());
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
        if (this.mOnVerticalChangedListener != null) {
            this.mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();
    }

    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration().getLayoutDirection() == 1;
        if (this.mIsLayoutRtl != isLayoutRtl) {
            View rotation90 = this.mRotatedViews[1];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));
            adjustExtraKeyGravity(rotation90, isLayoutRtl);
            View rotation270 = this.mRotatedViews[3];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
                adjustExtraKeyGravity(rotation270, isLayoutRtl);
            }
            this.mIsLayoutRtl = isLayoutRtl;
        }
    }

    private void adjustExtraKeyGravity(View navBar, boolean isLayoutRtl) {
        View menu = navBar.findViewById(R.id.menu);
        View imeSwitcher = navBar.findViewById(R.id.ime_switcher);
        if (menu != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menu.getLayoutParams();
            lp.gravity = isLayoutRtl ? 80 : 48;
            menu.setLayoutParams(lp);
        }
        if (imeSwitcher != null) {
            FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) imeSwitcher.getLayoutParams();
            lp2.gravity = isLayoutRtl ? 80 : 48;
            imeSwitcher.setLayoutParams(lp2);
        }
    }

    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) group;
            if (linearLayout.getOrientation() == 1) {
                int childCount = linearLayout.getChildCount();
                ArrayList<View> childList = new ArrayList<>(childCount);
                for (int i = 0; i < childCount; i++) {
                    childList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i2 = childCount - 1; i2 >= 0; i2--) {
                    linearLayout.addView(childList.get(i2));
                }
            }
        }
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
        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s", getResourceName(this.mCurrentView.getId()), Integer.valueOf(this.mCurrentView.getWidth()), Integer.valueOf(this.mCurrentView.getHeight()), visibilityToString(this.mCurrentView.getVisibility())));
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

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(PhoneStatusBar.viewInfo(button) + " " + visibilityToString(button.getVisibility()) + " alpha=" + button.getAlpha());
            if (button instanceof KeyButtonView) {
                pw.print(" drawingAlpha=" + ((KeyButtonView) button).getDrawingAlpha());
                pw.print(" quiescentAlpha=" + ((KeyButtonView) button).getQuiescentAlpha());
            }
        }
        pw.println();
    }
}
