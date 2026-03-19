package com.android.internal.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Property;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.DisplayListCanvas;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowCallbacks;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import com.android.internal.R;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.view.RootViewSurfaceTaker;
import com.android.internal.view.StandaloneActionMode;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuHelper;
import com.android.internal.widget.ActionBarContextView;
import com.android.internal.widget.BackgroundFallback;
import com.android.internal.widget.DecorCaptionView;
import com.android.internal.widget.FloatingToolbar;
import com.mediatek.view.ViewDebugManager;
import java.util.List;

public class DecorView extends FrameLayout implements RootViewSurfaceTaker, WindowCallbacks {
    private static final boolean DEBUG_MEASURE = false;
    private static final int DECOR_SHADOW_FOCUSED_HEIGHT_IN_DIP = 20;
    private static final int DECOR_SHADOW_UNFOCUSED_HEIGHT_IN_DIP = 5;
    private static final boolean SWEEP_OPEN_MENU = false;
    private static final String TAG = "DecorView";
    private boolean mAllowUpdateElevation;
    private boolean mApplyFloatingHorizontalInsets;
    private boolean mApplyFloatingVerticalInsets;
    private float mAvailableWidth;
    private BackdropFrameRenderer mBackdropFrameRenderer;
    private final BackgroundFallback mBackgroundFallback;
    private final Rect mBackgroundPadding;
    private final int mBarEnterExitDuration;
    private Drawable mCaptionBackgroundDrawable;
    private boolean mChanging;
    ViewGroup mContentRoot;
    DecorCaptionView mDecorCaptionView;
    int mDefaultOpacity;
    private int mDownY;
    private final Rect mDrawingBounds;
    private boolean mElevationAdjustedForStack;
    private ObjectAnimator mFadeAnim;
    private final int mFeatureId;
    private ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private final Rect mFloatingInsets;
    private FloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
    final boolean mForceWindowDrawsStatusBarBackground;
    private final Rect mFrameOffsets;
    private final Rect mFramePadding;
    private boolean mHasCaption;
    private final Interpolator mHideInterpolator;
    private final Paint mHorizontalResizeShadowPaint;
    private boolean mIsInPictureInPictureMode;
    private Drawable.Callback mLastBackgroundDrawableCb;
    private int mLastBottomInset;
    private boolean mLastHasBottomStableInset;
    private boolean mLastHasLeftStableInset;
    private boolean mLastHasRightStableInset;
    private boolean mLastHasTopStableInset;
    private int mLastLeftInset;
    private ViewOutlineProvider mLastOutlineProvider;
    private int mLastRightInset;
    private boolean mLastShouldAlwaysConsumeNavBar;
    private int mLastTopInset;
    private int mLastWindowFlags;
    String mLogTag;
    private Drawable mMenuBackground;
    private final ColorViewState mNavigationColorViewState;
    private Rect mOutsets;
    ActionMode mPrimaryActionMode;
    private PopupWindow mPrimaryActionModePopup;
    private ActionBarContextView mPrimaryActionModeView;
    private int mResizeMode;
    private final int mResizeShadowSize;
    private Drawable mResizingBackgroundDrawable;
    private int mRootScrollY;
    private final int mSemiTransparentStatusBarColor;
    private final Interpolator mShowInterpolator;
    private Runnable mShowPrimaryActionModePopup;
    private final ColorViewState mStatusColorViewState;
    private View mStatusGuard;
    private Rect mTempRect;
    private Drawable mUserCaptionBackgroundDrawable;
    private final Paint mVerticalResizeShadowPaint;
    private boolean mWatchingForMenu;
    private PhoneWindow mWindow;
    private boolean mWindowResizeCallbacksAdded;
    public static final ColorViewAttributes STATUS_BAR_COLOR_VIEW_ATTRIBUTES = new ColorViewAttributes(4, 67108864, 48, 3, 5, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME, 16908335, 1024);
    public static final ColorViewAttributes NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES = new ColorViewAttributes(2, 134217728, 80, 5, 3, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME, 16908336, 0);
    private static final ViewOutlineProvider PIP_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
            outline.setAlpha(1.0f);
        }
    };

    DecorView(Context context, int i, PhoneWindow phoneWindow, WindowManager.LayoutParams layoutParams) {
        super(context);
        boolean z = false;
        this.mAllowUpdateElevation = false;
        this.mElevationAdjustedForStack = false;
        this.mDefaultOpacity = -1;
        this.mDrawingBounds = new Rect();
        this.mBackgroundPadding = new Rect();
        this.mFramePadding = new Rect();
        this.mFrameOffsets = new Rect();
        this.mHasCaption = false;
        this.mStatusColorViewState = new ColorViewState(STATUS_BAR_COLOR_VIEW_ATTRIBUTES);
        this.mNavigationColorViewState = new ColorViewState(NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES);
        this.mBackgroundFallback = new BackgroundFallback();
        this.mLastTopInset = 0;
        this.mLastBottomInset = 0;
        this.mLastRightInset = 0;
        this.mLastLeftInset = 0;
        this.mLastHasTopStableInset = false;
        this.mLastHasBottomStableInset = false;
        this.mLastHasRightStableInset = false;
        this.mLastHasLeftStableInset = false;
        this.mLastWindowFlags = 0;
        this.mLastShouldAlwaysConsumeNavBar = false;
        this.mRootScrollY = 0;
        this.mOutsets = new Rect();
        this.mWindowResizeCallbacksAdded = false;
        this.mLastBackgroundDrawableCb = null;
        this.mBackdropFrameRenderer = null;
        this.mLogTag = TAG;
        this.mFloatingInsets = new Rect();
        this.mApplyFloatingVerticalInsets = false;
        this.mApplyFloatingHorizontalInsets = false;
        this.mResizeMode = -1;
        this.mVerticalResizeShadowPaint = new Paint();
        this.mHorizontalResizeShadowPaint = new Paint();
        this.mFeatureId = i;
        this.mShowInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mHideInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.mBarEnterExitDuration = context.getResources().getInteger(R.integer.dock_enter_exit_duration);
        if (context.getResources().getBoolean(R.bool.config_forceWindowDrawsStatusBarBackground) && context.getApplicationInfo().targetSdkVersion >= 24) {
            z = true;
        }
        this.mForceWindowDrawsStatusBarBackground = z;
        this.mSemiTransparentStatusBarColor = context.getResources().getColor(R.color.system_bar_background_semi_transparent, null);
        updateAvailableWidth();
        setWindow(phoneWindow);
        updateLogTag(layoutParams);
        this.mResizeShadowSize = context.getResources().getDimensionPixelSize(R.dimen.resize_shadow_size);
        initResizingPaints();
    }

    void setBackgroundFallback(int i) {
        this.mBackgroundFallback.setDrawable(i != 0 ? getContext().getDrawable(i) : null);
        setWillNotDraw(getBackground() == null && !this.mBackgroundFallback.hasFallback());
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        return gatherTransparentRegion(this.mStatusColorViewState, region) || gatherTransparentRegion(this.mNavigationColorViewState, region) || super.gatherTransparentRegion(region);
    }

    boolean gatherTransparentRegion(ColorViewState colorViewState, Region region) {
        if (colorViewState.view != null && colorViewState.visible && isResizing()) {
            return colorViewState.view.gatherTransparentRegion(region);
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mBackgroundFallback.draw(this, this.mContentRoot, canvas, this.mWindow.mContentParent, this.mStatusColorViewState.view, this.mNavigationColorViewState.view);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        boolean z = keyEvent.getAction() == 0;
        if (z && keyEvent.getRepeatCount() == 0) {
            if (this.mWindow.mPanelChordingKey > 0 && this.mWindow.mPanelChordingKey != keyCode && dispatchKeyShortcutEvent(keyEvent)) {
                return true;
            }
            if (this.mWindow.mPreparedPanel != null && this.mWindow.mPreparedPanel.isOpen && this.mWindow.performPanelShortcut(this.mWindow.mPreparedPanel, keyCode, keyEvent, 0)) {
                return true;
            }
        }
        if (!this.mWindow.isDestroyed()) {
            Window.Callback callback = this.mWindow.getCallback();
            if ((callback == null || this.mFeatureId >= 0) ? super.dispatchKeyEvent(keyEvent) : callback.dispatchKeyEvent(keyEvent)) {
                return true;
            }
        }
        return z ? this.mWindow.onKeyDown(this.mFeatureId, keyEvent.getKeyCode(), keyEvent) : this.mWindow.onKeyUp(this.mFeatureId, keyEvent.getKeyCode(), keyEvent);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        if (this.mWindow.mPreparedPanel != null && this.mWindow.performPanelShortcut(this.mWindow.mPreparedPanel, keyEvent.getKeyCode(), keyEvent, 1)) {
            if (this.mWindow.mPreparedPanel != null) {
                this.mWindow.mPreparedPanel.isHandled = true;
            }
            return true;
        }
        Window.Callback callback = this.mWindow.getCallback();
        if ((callback == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchKeyShortcutEvent(keyEvent) : callback.dispatchKeyShortcutEvent(keyEvent)) {
            return true;
        }
        PhoneWindow.PanelFeatureState panelState = this.mWindow.getPanelState(0, false);
        if (panelState != null && this.mWindow.mPreparedPanel == null) {
            this.mWindow.preparePanel(panelState, keyEvent);
            boolean zPerformPanelShortcut = this.mWindow.performPanelShortcut(panelState, keyEvent.getKeyCode(), keyEvent, 1);
            panelState.isPrepared = false;
            if (zPerformPanelShortcut) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        Window.Callback callback = this.mWindow.getCallback();
        return (callback == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchTouchEvent(motionEvent) : callback.dispatchTouchEvent(motionEvent);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        Window.Callback callback = this.mWindow.getCallback();
        return (callback == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchTrackballEvent(motionEvent) : callback.dispatchTrackballEvent(motionEvent);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        Window.Callback callback = this.mWindow.getCallback();
        return (callback == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchGenericMotionEvent(motionEvent) : callback.dispatchGenericMotionEvent(motionEvent);
    }

    public boolean superDispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 4) {
            int action = keyEvent.getAction();
            if (this.mPrimaryActionMode != null) {
                if (action == 1) {
                    this.mPrimaryActionMode.finish();
                }
                return true;
            }
        }
        if (super.dispatchKeyEvent(keyEvent)) {
            return true;
        }
        return getViewRootImpl() != null && getViewRootImpl().dispatchUnhandledKeyEvent(keyEvent);
    }

    public boolean superDispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return super.dispatchKeyShortcutEvent(keyEvent);
    }

    public boolean superDispatchTouchEvent(MotionEvent motionEvent) {
        return super.dispatchTouchEvent(motionEvent);
    }

    public boolean superDispatchTrackballEvent(MotionEvent motionEvent) {
        return super.dispatchTrackballEvent(motionEvent);
    }

    public boolean superDispatchGenericMotionEvent(MotionEvent motionEvent) {
        return super.dispatchGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return onInterceptTouchEvent(motionEvent);
    }

    private boolean isOutOfInnerBounds(int i, int i2) {
        return i < 0 || i2 < 0 || i > getWidth() || i2 > getHeight();
    }

    private boolean isOutOfBounds(int i, int i2) {
        return i < -5 || i2 < -5 || i > getWidth() + 5 || i2 > getHeight() + 5;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (this.mHasCaption && isShowingCaption() && action == 0 && isOutOfInnerBounds((int) motionEvent.getX(), (int) motionEvent.getY())) {
            return true;
        }
        if (this.mFeatureId >= 0 && action == 0 && isOutOfBounds((int) motionEvent.getX(), (int) motionEvent.getY())) {
            this.mWindow.closePanel(this.mFeatureId);
            return true;
        }
        return false;
    }

    @Override
    public void sendAccessibilityEvent(int i) {
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            return;
        }
        if ((this.mFeatureId == 0 || this.mFeatureId == 6 || this.mFeatureId == 2 || this.mFeatureId == 5) && getChildCount() == 1) {
            getChildAt(0).sendAccessibilityEvent(i);
        } else {
            super.sendAccessibilityEvent(i);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        Window.Callback callback = this.mWindow.getCallback();
        if (callback != null && !this.mWindow.isDestroyed() && callback.dispatchPopulateAccessibilityEvent(accessibilityEvent)) {
            return true;
        }
        return super.dispatchPopulateAccessibilityEventInternal(accessibilityEvent);
    }

    @Override
    protected boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        if (frame) {
            Rect rect = this.mDrawingBounds;
            getDrawingRect(rect);
            Drawable foreground = getForeground();
            if (foreground != null) {
                Rect rect2 = this.mFrameOffsets;
                rect.left += rect2.left;
                rect.top += rect2.top;
                rect.right -= rect2.right;
                rect.bottom -= rect2.bottom;
                foreground.setBounds(rect);
                Rect rect3 = this.mFramePadding;
                rect.left += rect3.left - rect2.left;
                rect.top += rect3.top - rect2.top;
                rect.right -= rect3.right - rect2.right;
                rect.bottom -= rect3.bottom - rect2.bottom;
            }
            Drawable background = getBackground();
            if (background != null) {
                background.setBounds(rect);
            }
        }
        return frame;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int iMakeMeasureSpec;
        boolean z;
        int mode;
        int mode2;
        int fraction;
        int fraction2;
        int fraction3;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        boolean z2 = true;
        boolean z3 = getResources().getConfiguration().orientation == 1;
        int mode3 = View.MeasureSpec.getMode(i);
        int mode4 = View.MeasureSpec.getMode(i2);
        this.mApplyFloatingHorizontalInsets = false;
        if (mode3 == Integer.MIN_VALUE) {
            TypedValue typedValue = z3 ? this.mWindow.mFixedWidthMinor : this.mWindow.mFixedWidthMajor;
            if (typedValue != null && typedValue.type != 0) {
                if (typedValue.type != 5) {
                    if (typedValue.type == 6) {
                        fraction3 = (int) typedValue.getFraction(displayMetrics.widthPixels, displayMetrics.widthPixels);
                    } else {
                        fraction3 = 0;
                    }
                } else {
                    fraction3 = (int) typedValue.getDimension(displayMetrics);
                }
                int size = View.MeasureSpec.getSize(i);
                if (fraction3 > 0) {
                    iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.min(fraction3, size), 1073741824);
                    z = true;
                } else {
                    i = View.MeasureSpec.makeMeasureSpec((size - this.mFloatingInsets.left) - this.mFloatingInsets.right, Integer.MIN_VALUE);
                    this.mApplyFloatingHorizontalInsets = true;
                    iMakeMeasureSpec = i;
                    z = false;
                }
            }
        } else {
            iMakeMeasureSpec = i;
            z = false;
        }
        this.mApplyFloatingVerticalInsets = false;
        if (mode4 == Integer.MIN_VALUE) {
            TypedValue typedValue2 = z3 ? this.mWindow.mFixedHeightMajor : this.mWindow.mFixedHeightMinor;
            if (typedValue2 != null && typedValue2.type != 0) {
                if (typedValue2.type != 5) {
                    if (typedValue2.type == 6) {
                        fraction2 = (int) typedValue2.getFraction(displayMetrics.heightPixels, displayMetrics.heightPixels);
                    } else {
                        fraction2 = 0;
                    }
                } else {
                    fraction2 = (int) typedValue2.getDimension(displayMetrics);
                }
                int size2 = View.MeasureSpec.getSize(i2);
                if (fraction2 > 0) {
                    i2 = View.MeasureSpec.makeMeasureSpec(Math.min(fraction2, size2), 1073741824);
                } else if ((this.mWindow.getAttributes().flags & 256) == 0) {
                    i2 = View.MeasureSpec.makeMeasureSpec((size2 - this.mFloatingInsets.top) - this.mFloatingInsets.bottom, Integer.MIN_VALUE);
                    this.mApplyFloatingVerticalInsets = true;
                }
            }
        }
        getOutsets(this.mOutsets);
        if ((this.mOutsets.top > 0 || this.mOutsets.bottom > 0) && (mode = View.MeasureSpec.getMode(i2)) != 0) {
            i2 = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i2) + this.mOutsets.top + this.mOutsets.bottom, mode);
        }
        if ((this.mOutsets.left > 0 || this.mOutsets.right > 0) && (mode2 = View.MeasureSpec.getMode(iMakeMeasureSpec)) != 0) {
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(iMakeMeasureSpec) + this.mOutsets.left + this.mOutsets.right, mode2);
        }
        super.onMeasure(iMakeMeasureSpec, i2);
        int measuredWidth = getMeasuredWidth();
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824);
        if (!z && mode3 == Integer.MIN_VALUE) {
            TypedValue typedValue3 = z3 ? this.mWindow.mMinWidthMinor : this.mWindow.mMinWidthMajor;
            if (typedValue3.type != 0) {
                if (typedValue3.type != 5) {
                    if (typedValue3.type == 6) {
                        fraction = (int) typedValue3.getFraction(this.mAvailableWidth, this.mAvailableWidth);
                    } else {
                        fraction = 0;
                    }
                } else {
                    fraction = (int) typedValue3.getDimension(displayMetrics);
                }
                if (measuredWidth < fraction) {
                    iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(fraction, 1073741824);
                }
            }
        } else {
            z2 = false;
        }
        if (z2) {
            super.onMeasure(iMakeMeasureSpec2, i2);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        getOutsets(this.mOutsets);
        if (this.mOutsets.left > 0) {
            offsetLeftAndRight(-this.mOutsets.left);
        }
        if (this.mOutsets.top > 0) {
            offsetTopAndBottom(-this.mOutsets.top);
        }
        if (this.mApplyFloatingVerticalInsets) {
            offsetTopAndBottom(this.mFloatingInsets.top);
        }
        if (this.mApplyFloatingHorizontalInsets) {
            offsetLeftAndRight(this.mFloatingInsets.left);
        }
        updateElevation();
        this.mAllowUpdateElevation = true;
        if (z && this.mResizeMode == 1) {
            getViewRootImpl().requestInvalidateRootRenderNode();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mMenuBackground != null) {
            this.mMenuBackground.draw(canvas);
        }
    }

    @Override
    public boolean showContextMenuForChild(View view) {
        return showContextMenuForChildInternal(view, Float.NaN, Float.NaN);
    }

    @Override
    public boolean showContextMenuForChild(View view, float f, float f2) {
        return showContextMenuForChildInternal(view, f, f2);
    }

    private boolean showContextMenuForChildInternal(View view, float f, float f2) {
        MenuHelper menuHelperShowDialog;
        if (this.mWindow.mContextMenuHelper != null) {
            this.mWindow.mContextMenuHelper.dismiss();
            this.mWindow.mContextMenuHelper = null;
        }
        PhoneWindow.PhoneWindowMenuCallback phoneWindowMenuCallback = this.mWindow.mContextMenuCallback;
        if (this.mWindow.mContextMenu == null) {
            this.mWindow.mContextMenu = new ContextMenuBuilder(getContext());
            this.mWindow.mContextMenu.setCallback(phoneWindowMenuCallback);
        } else {
            this.mWindow.mContextMenu.clearAll();
        }
        boolean z = (Float.isNaN(f) || Float.isNaN(f2)) ? false : true;
        if (z) {
            menuHelperShowDialog = this.mWindow.mContextMenu.showPopup(getContext(), view, f, f2);
        } else {
            menuHelperShowDialog = this.mWindow.mContextMenu.showDialog(view, view.getWindowToken());
        }
        if (menuHelperShowDialog != null) {
            phoneWindowMenuCallback.setShowDialogForSubmenu(!z);
            menuHelperShowDialog.setPresenterCallback(phoneWindowMenuCallback);
        }
        this.mWindow.mContextMenuHelper = menuHelperShowDialog;
        return menuHelperShowDialog != null;
    }

    @Override
    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback) {
        return startActionModeForChild(view, callback, 0);
    }

    @Override
    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback, int i) {
        return startActionMode(view, callback, i);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return startActionMode(callback, 0);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int i) {
        return startActionMode(this, callback, i);
    }

    private ActionMode startActionMode(View view, ActionMode.Callback callback, int i) {
        ActionMode actionModeOnWindowStartingActionMode;
        ActionMode actionModeCreateActionMode;
        ActionModeCallback2Wrapper actionModeCallback2Wrapper = new ActionModeCallback2Wrapper(callback);
        if (this.mWindow.getCallback() != null && !this.mWindow.isDestroyed()) {
            try {
                actionModeOnWindowStartingActionMode = this.mWindow.getCallback().onWindowStartingActionMode(actionModeCallback2Wrapper, i);
            } catch (AbstractMethodError e) {
                if (i == 0) {
                    try {
                        actionModeOnWindowStartingActionMode = this.mWindow.getCallback().onWindowStartingActionMode(actionModeCallback2Wrapper);
                    } catch (AbstractMethodError e2) {
                        actionModeOnWindowStartingActionMode = null;
                    }
                }
            }
        } else {
            actionModeOnWindowStartingActionMode = null;
        }
        if (actionModeOnWindowStartingActionMode != null) {
            if (actionModeOnWindowStartingActionMode.getType() == 0) {
                cleanupPrimaryActionMode();
                this.mPrimaryActionMode = actionModeOnWindowStartingActionMode;
            } else if (actionModeOnWindowStartingActionMode.getType() == 1) {
                if (this.mFloatingActionMode != null) {
                    this.mFloatingActionMode.finish();
                }
                this.mFloatingActionMode = actionModeOnWindowStartingActionMode;
            }
            actionModeCreateActionMode = actionModeOnWindowStartingActionMode;
        } else {
            actionModeCreateActionMode = createActionMode(i, actionModeCallback2Wrapper, view);
            if (actionModeCreateActionMode != null && actionModeCallback2Wrapper.onCreateActionMode(actionModeCreateActionMode, actionModeCreateActionMode.getMenu())) {
                setHandledActionMode(actionModeCreateActionMode);
            } else {
                actionModeCreateActionMode = null;
            }
        }
        if (actionModeCreateActionMode != null && this.mWindow.getCallback() != null && !this.mWindow.isDestroyed()) {
            try {
                this.mWindow.getCallback().onActionModeStarted(actionModeCreateActionMode);
            } catch (AbstractMethodError e3) {
            }
        }
        return actionModeCreateActionMode;
    }

    private void cleanupPrimaryActionMode() {
        if (this.mPrimaryActionMode != null) {
            this.mPrimaryActionMode.finish();
            this.mPrimaryActionMode = null;
        }
        if (this.mPrimaryActionModeView != null) {
            this.mPrimaryActionModeView.killMode();
        }
    }

    private void cleanupFloatingActionModeViews() {
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        if (this.mFloatingActionModeOriginatingView != null) {
            if (this.mFloatingToolbarPreDrawListener != null) {
                this.mFloatingActionModeOriginatingView.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
                this.mFloatingToolbarPreDrawListener = null;
            }
            this.mFloatingActionModeOriginatingView = null;
        }
    }

    void startChanging() {
        this.mChanging = true;
    }

    void finishChanging() {
        this.mChanging = false;
        drawableChanged();
    }

    public void setWindowBackground(Drawable drawable) {
        if (getBackground() != drawable) {
            setBackgroundDrawable(drawable);
            boolean z = true;
            if (drawable != null) {
                if (!this.mWindow.isTranslucent() && !this.mWindow.isShowingWallpaper()) {
                    z = false;
                }
                this.mResizingBackgroundDrawable = enforceNonTranslucentBackground(drawable, z);
            } else {
                Context context = getContext();
                int i = this.mWindow.mBackgroundFallbackResource;
                if (!this.mWindow.isTranslucent() && !this.mWindow.isShowingWallpaper()) {
                    z = false;
                }
                this.mResizingBackgroundDrawable = getResizingBackgroundDrawable(context, 0, i, z);
            }
            if (this.mResizingBackgroundDrawable != null) {
                this.mResizingBackgroundDrawable.getPadding(this.mBackgroundPadding);
            } else {
                this.mBackgroundPadding.setEmpty();
            }
            drawableChanged();
        }
    }

    public void setWindowFrame(Drawable drawable) {
        if (getForeground() != drawable) {
            setForeground(drawable);
            if (drawable != null) {
                drawable.getPadding(this.mFramePadding);
            } else {
                this.mFramePadding.setEmpty();
            }
            drawableChanged();
        }
    }

    @Override
    public void onWindowSystemUiVisibilityChanged(int i) {
        updateColorViews(null, true);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        WindowManager.LayoutParams attributes = this.mWindow.getAttributes();
        this.mFloatingInsets.setEmpty();
        if ((attributes.flags & 256) == 0) {
            if (attributes.height == -2) {
                this.mFloatingInsets.top = windowInsets.getSystemWindowInsetTop();
                this.mFloatingInsets.bottom = windowInsets.getSystemWindowInsetBottom();
                windowInsets = windowInsets.inset(0, windowInsets.getSystemWindowInsetTop(), 0, windowInsets.getSystemWindowInsetBottom());
            }
            if (this.mWindow.getAttributes().width == -2) {
                this.mFloatingInsets.left = windowInsets.getSystemWindowInsetTop();
                this.mFloatingInsets.right = windowInsets.getSystemWindowInsetBottom();
                windowInsets = windowInsets.inset(windowInsets.getSystemWindowInsetLeft(), 0, windowInsets.getSystemWindowInsetRight(), 0);
            }
        }
        this.mFrameOffsets.set(windowInsets.getSystemWindowInsets());
        WindowInsets windowInsetsUpdateStatusGuard = updateStatusGuard(updateColorViews(windowInsets, true));
        if (getForeground() != null) {
            drawableChanged();
        }
        return windowInsetsUpdateStatusGuard;
    }

    @Override
    public boolean isTransitionGroup() {
        return false;
    }

    public static int getColorViewTopInset(int i, int i2) {
        return Math.min(i, i2);
    }

    public static int getColorViewBottomInset(int i, int i2) {
        return Math.min(i, i2);
    }

    public static int getColorViewRightInset(int i, int i2) {
        return Math.min(i, i2);
    }

    public static int getColorViewLeftInset(int i, int i2) {
        return Math.min(i, i2);
    }

    public static boolean isNavBarToRightEdge(int i, int i2) {
        return i == 0 && i2 > 0;
    }

    public static boolean isNavBarToLeftEdge(int i, int i2) {
        return i == 0 && i2 > 0;
    }

    public static int getNavBarSize(int i, int i2, int i3) {
        return isNavBarToRightEdge(i, i2) ? i2 : isNavBarToLeftEdge(i, i3) ? i3 : i;
    }

    public static void getNavigationBarRect(int i, int i2, Rect rect, Rect rect2, Rect rect3) {
        int colorViewBottomInset = getColorViewBottomInset(rect.bottom, rect2.bottom);
        int colorViewLeftInset = getColorViewLeftInset(rect.left, rect2.left);
        int colorViewLeftInset2 = getColorViewLeftInset(rect.right, rect2.right);
        int navBarSize = getNavBarSize(colorViewBottomInset, colorViewLeftInset2, colorViewLeftInset);
        if (isNavBarToRightEdge(colorViewBottomInset, colorViewLeftInset2)) {
            rect3.set(i - navBarSize, 0, i, i2);
        } else if (isNavBarToLeftEdge(colorViewBottomInset, colorViewLeftInset)) {
            rect3.set(0, 0, navBarSize, i2);
        } else {
            rect3.set(0, i2 - navBarSize, i, i2);
        }
    }

    WindowInsets updateColorViews(WindowInsets windowInsets, boolean z) {
        int i;
        int i2;
        WindowInsets windowInsetsInset = windowInsets;
        WindowManager.LayoutParams attributes = this.mWindow.getAttributes();
        int windowSystemUiVisibility = attributes.systemUiVisibility | getWindowSystemUiVisibility();
        boolean z2 = this.mWindow.getAttributes().type == 2011;
        if (!this.mWindow.mIsFloating || z2) {
            boolean z3 = (!isLaidOut()) | (((this.mLastWindowFlags ^ attributes.flags) & Integer.MIN_VALUE) != 0);
            this.mLastWindowFlags = attributes.flags;
            if (windowInsetsInset != null) {
                this.mLastTopInset = getColorViewTopInset(windowInsets.getStableInsetTop(), windowInsets.getSystemWindowInsetTop());
                this.mLastBottomInset = getColorViewBottomInset(windowInsets.getStableInsetBottom(), windowInsets.getSystemWindowInsetBottom());
                this.mLastRightInset = getColorViewRightInset(windowInsets.getStableInsetRight(), windowInsets.getSystemWindowInsetRight());
                this.mLastLeftInset = getColorViewRightInset(windowInsets.getStableInsetLeft(), windowInsets.getSystemWindowInsetLeft());
                boolean z4 = windowInsets.getStableInsetTop() != 0;
                boolean z5 = z3 | (z4 != this.mLastHasTopStableInset);
                this.mLastHasTopStableInset = z4;
                boolean z6 = windowInsets.getStableInsetBottom() != 0;
                boolean z7 = z5 | (z6 != this.mLastHasBottomStableInset);
                this.mLastHasBottomStableInset = z6;
                boolean z8 = windowInsets.getStableInsetRight() != 0;
                boolean z9 = z7 | (z8 != this.mLastHasRightStableInset);
                this.mLastHasRightStableInset = z8;
                boolean z10 = windowInsets.getStableInsetLeft() != 0;
                z3 = z9 | (z10 != this.mLastHasLeftStableInset);
                this.mLastHasLeftStableInset = z10;
                this.mLastShouldAlwaysConsumeNavBar = windowInsets.shouldAlwaysConsumeNavBar();
            }
            boolean z11 = z3;
            boolean zIsNavBarToRightEdge = isNavBarToRightEdge(this.mLastBottomInset, this.mLastRightInset);
            boolean zIsNavBarToLeftEdge = isNavBarToLeftEdge(this.mLastBottomInset, this.mLastLeftInset);
            updateColorViewInt(this.mNavigationColorViewState, windowSystemUiVisibility, this.mWindow.mNavigationBarColor, this.mWindow.mNavigationBarDividerColor, getNavBarSize(this.mLastBottomInset, this.mLastRightInset, this.mLastLeftInset), zIsNavBarToRightEdge || zIsNavBarToLeftEdge, zIsNavBarToLeftEdge, 0, z && !z11, false);
            boolean z12 = zIsNavBarToRightEdge && this.mNavigationColorViewState.present;
            boolean z13 = zIsNavBarToLeftEdge && this.mNavigationColorViewState.present;
            if (z12) {
                i2 = this.mLastRightInset;
            } else if (z13) {
                i2 = this.mLastLeftInset;
            } else {
                i = 0;
                updateColorViewInt(this.mStatusColorViewState, windowSystemUiVisibility, calculateStatusBarColor(), 0, this.mLastTopInset, false, z13, i, (z || z11) ? false : true, this.mForceWindowDrawsStatusBarBackground);
            }
            i = i2;
            if (z) {
                updateColorViewInt(this.mStatusColorViewState, windowSystemUiVisibility, calculateStatusBarColor(), 0, this.mLastTopInset, false, z13, i, (z || z11) ? false : true, this.mForceWindowDrawsStatusBarBackground);
            }
        }
        boolean z14 = ((attributes.flags & Integer.MIN_VALUE) != 0 && (windowSystemUiVisibility & 512) == 0 && (windowSystemUiVisibility & 2) == 0) || this.mLastShouldAlwaysConsumeNavBar;
        int i3 = (windowSystemUiVisibility & 1024) == 0 && (windowSystemUiVisibility & Integer.MIN_VALUE) == 0 && (attributes.flags & 256) == 0 && (attributes.flags & 65536) == 0 && this.mForceWindowDrawsStatusBarBackground && this.mLastTopInset != 0 ? this.mLastTopInset : 0;
        int i4 = z14 ? this.mLastRightInset : 0;
        int i5 = z14 ? this.mLastBottomInset : 0;
        int i6 = z14 ? this.mLastLeftInset : 0;
        if (this.mContentRoot != null && (this.mContentRoot.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mContentRoot.getLayoutParams();
            if (marginLayoutParams.topMargin != i3 || marginLayoutParams.rightMargin != i4 || marginLayoutParams.bottomMargin != i5 || marginLayoutParams.leftMargin != i6) {
                marginLayoutParams.topMargin = i3;
                marginLayoutParams.rightMargin = i4;
                marginLayoutParams.bottomMargin = i5;
                marginLayoutParams.leftMargin = i6;
                this.mContentRoot.setLayoutParams(marginLayoutParams);
                if (windowInsetsInset == null) {
                    requestApplyInsets();
                }
            }
            if (windowInsetsInset != null) {
                windowInsetsInset = windowInsetsInset.inset(i6, i3, i4, i5);
            }
        }
        if (windowInsetsInset != null) {
            return windowInsetsInset.consumeStableInsets();
        }
        return windowInsetsInset;
    }

    private int calculateStatusBarColor() {
        return calculateStatusBarColor(this.mWindow.getAttributes().flags, this.mSemiTransparentStatusBarColor, this.mWindow.mStatusBarColor);
    }

    public static int calculateStatusBarColor(int i, int i2, int i3) {
        if ((67108864 & i) != 0) {
            return i2;
        }
        if ((i & Integer.MIN_VALUE) != 0) {
            return i3;
        }
        return -16777216;
    }

    private int getCurrentColor(ColorViewState colorViewState) {
        if (colorViewState.visible) {
            return colorViewState.color;
        }
        return 0;
    }

    private void updateColorViewInt(final ColorViewState colorViewState, int i, int i2, int i3, int i4, boolean z, boolean z2, int i5, boolean z3, boolean z4) {
        int i6;
        boolean z5;
        int i7 = i5;
        colorViewState.present = colorViewState.attributes.isPresent(i, this.mWindow.getAttributes().flags, z4);
        boolean zIsVisible = colorViewState.attributes.isVisible(colorViewState.present, i2, this.mWindow.getAttributes().flags, z4);
        boolean z6 = zIsVisible && !isResizing() && i4 > 0;
        View view = colorViewState.view;
        int i8 = z ? -1 : i4;
        int i9 = z ? i4 : -1;
        if (z) {
            i6 = z2 ? colorViewState.attributes.seascapeGravity : colorViewState.attributes.horizontalGravity;
        } else {
            i6 = colorViewState.attributes.verticalGravity;
        }
        if (view == null) {
            if (!z6) {
                z5 = false;
            } else {
                view = new View(this.mContext);
                colorViewState.view = view;
                setColor(view, i2, i3, z, z2);
                view.setTransitionName(colorViewState.attributes.transitionName);
                view.setId(colorViewState.attributes.id);
                view.setVisibility(4);
                colorViewState.targetVisibility = 0;
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(i9, i8, i6);
                if (z2) {
                    layoutParams.leftMargin = i7;
                } else {
                    layoutParams.rightMargin = i7;
                }
                addView(view, layoutParams);
                updateColorViewTranslations();
                z5 = true;
            }
        } else {
            int i10 = z6 ? 0 : 4;
            z5 = colorViewState.targetVisibility != i10;
            colorViewState.targetVisibility = i10;
            FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) view.getLayoutParams();
            int i11 = z2 ? 0 : i7;
            if (!z2) {
                i7 = 0;
            }
            if (layoutParams2.height != i8 || layoutParams2.width != i9 || layoutParams2.gravity != i6 || layoutParams2.rightMargin != i11 || layoutParams2.leftMargin != i7) {
                layoutParams2.height = i8;
                layoutParams2.width = i9;
                layoutParams2.gravity = i6;
                layoutParams2.rightMargin = i11;
                layoutParams2.leftMargin = i7;
                view.setLayoutParams(layoutParams2);
            }
            if (z6) {
                setColor(view, i2, i3, z, z2);
            }
        }
        if (z5) {
            view.animate().cancel();
            if (!z3 || isResizing()) {
                view.setAlpha(1.0f);
                view.setVisibility(z6 ? 0 : 4);
            } else if (z6) {
                if (view.getVisibility() != 0) {
                    view.setVisibility(0);
                    view.setAlpha(0.0f);
                }
                view.animate().alpha(1.0f).setInterpolator(this.mShowInterpolator).setDuration(this.mBarEnterExitDuration);
            } else {
                view.animate().alpha(0.0f).setInterpolator(this.mHideInterpolator).setDuration(this.mBarEnterExitDuration).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        colorViewState.view.setAlpha(1.0f);
                        colorViewState.view.setVisibility(4);
                    }
                });
            }
        }
        colorViewState.visible = zIsVisible;
        colorViewState.color = i2;
    }

    private static void setColor(View view, int i, int i2, boolean z, boolean z2) {
        if (i2 != 0) {
            Pair pair = (Pair) view.getTag();
            if (pair == null || ((Boolean) pair.first).booleanValue() != z || ((Boolean) pair.second).booleanValue() != z2) {
                int iRound = Math.round(TypedValue.applyDimension(1, 1.0f, view.getContext().getResources().getDisplayMetrics()));
                view.setBackground(new LayerDrawable(new Drawable[]{new ColorDrawable(i2), new InsetDrawable((Drawable) new ColorDrawable(i), (!z || z2) ? 0 : iRound, !z ? iRound : 0, (z && z2) ? iRound : 0, 0)}));
                view.setTag(new Pair(Boolean.valueOf(z), Boolean.valueOf(z2)));
                return;
            } else {
                LayerDrawable layerDrawable = (LayerDrawable) view.getBackground();
                ((ColorDrawable) ((InsetDrawable) layerDrawable.getDrawable(1)).getDrawable()).setColor(i);
                ((ColorDrawable) layerDrawable.getDrawable(0)).setColor(i2);
                return;
            }
        }
        view.setTag(null);
        view.setBackgroundColor(i);
    }

    private void updateColorViewTranslations() {
        int i = this.mRootScrollY;
        if (this.mStatusColorViewState.view != null) {
            this.mStatusColorViewState.view.setTranslationY(i > 0 ? i : 0.0f);
        }
        if (this.mNavigationColorViewState.view != null) {
            this.mNavigationColorViewState.view.setTranslationY(i < 0 ? i : 0.0f);
        }
    }

    private WindowInsets updateStatusGuard(WindowInsets windowInsets) {
        boolean z;
        boolean z2;
        if (this.mPrimaryActionModeView != null && (this.mPrimaryActionModeView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mPrimaryActionModeView.getLayoutParams();
            if (this.mPrimaryActionModeView.isShown()) {
                if (this.mTempRect == null) {
                    this.mTempRect = new Rect();
                }
                Rect rect = this.mTempRect;
                this.mWindow.mContentParent.computeSystemWindowInsets(windowInsets, rect);
                if (marginLayoutParams.topMargin != (rect.top == 0 ? windowInsets.getSystemWindowInsetTop() : 0)) {
                    marginLayoutParams.topMargin = windowInsets.getSystemWindowInsetTop();
                    if (this.mStatusGuard == null) {
                        this.mStatusGuard = new View(this.mContext);
                        this.mStatusGuard.setBackgroundColor(this.mContext.getColor(R.color.decor_view_status_guard));
                        addView(this.mStatusGuard, indexOfChild(this.mStatusColorViewState.view), new FrameLayout.LayoutParams(-1, marginLayoutParams.topMargin, 8388659));
                    } else {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mStatusGuard.getLayoutParams();
                        if (layoutParams.height != marginLayoutParams.topMargin) {
                            layoutParams.height = marginLayoutParams.topMargin;
                            this.mStatusGuard.setLayoutParams(layoutParams);
                        }
                    }
                    z2 = true;
                } else {
                    z2 = false;
                }
                z = this.mStatusGuard != null;
                if (((this.mWindow.getLocalFeaturesPrivate() & 1024) == 0) && z) {
                    windowInsets = windowInsets.inset(0, windowInsets.getSystemWindowInsetTop(), 0, 0);
                }
            } else if (marginLayoutParams.topMargin != 0) {
                marginLayoutParams.topMargin = 0;
                z = false;
                z2 = true;
            } else {
                z2 = false;
                z = false;
            }
            if (z2) {
                this.mPrimaryActionModeView.setLayoutParams(marginLayoutParams);
            }
        } else {
            z = false;
        }
        if (this.mStatusGuard != null) {
            this.mStatusGuard.setVisibility(z ? 0 : 8);
        }
        return windowInsets;
    }

    public void updatePictureInPictureOutlineProvider(boolean z) {
        if (this.mIsInPictureInPictureMode == z) {
            return;
        }
        if (z) {
            Window.WindowControllerCallback windowControllerCallback = this.mWindow.getWindowControllerCallback();
            if (windowControllerCallback != null && windowControllerCallback.isTaskRoot()) {
                super.setOutlineProvider(PIP_OUTLINE_PROVIDER);
            }
        } else if (getOutlineProvider() != this.mLastOutlineProvider) {
            setOutlineProvider(this.mLastOutlineProvider);
        }
        this.mIsInPictureInPictureMode = z;
    }

    @Override
    public void setOutlineProvider(ViewOutlineProvider viewOutlineProvider) {
        super.setOutlineProvider(viewOutlineProvider);
        this.mLastOutlineProvider = viewOutlineProvider;
    }

    private void drawableChanged() {
        if (this.mChanging) {
            return;
        }
        setPadding(this.mFramePadding.left + this.mBackgroundPadding.left, this.mFramePadding.top + this.mBackgroundPadding.top, this.mFramePadding.right + this.mBackgroundPadding.right, this.mFramePadding.bottom + this.mBackgroundPadding.bottom);
        requestLayout();
        invalidate();
        int opacity = -3;
        if (!getResources().getConfiguration().windowConfiguration.hasWindowShadow()) {
            Drawable background = getBackground();
            Drawable foreground = getForeground();
            if (background != null) {
                if (foreground == null) {
                    opacity = background.getOpacity();
                } else if (this.mFramePadding.left <= 0 && this.mFramePadding.top <= 0 && this.mFramePadding.right <= 0 && this.mFramePadding.bottom <= 0) {
                    opacity = foreground.getOpacity();
                    int opacity2 = background.getOpacity();
                    if (opacity != -1 && opacity2 != -1) {
                        if (opacity != 0) {
                            if (opacity2 != 0) {
                                opacity2 = Drawable.resolveOpacity(opacity, opacity2);
                                opacity = opacity2;
                            }
                        } else {
                            opacity = opacity2;
                        }
                    } else {
                        opacity = -1;
                    }
                }
            } else {
                opacity = -1;
            }
        }
        this.mDefaultOpacity = opacity;
        if (this.mFeatureId < 0) {
            this.mWindow.setDefaultWindowFormat(opacity);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (this.mWindow.hasFeature(0) && !z && this.mWindow.mPanelChordingKey != 0) {
            this.mWindow.closePanel(0);
        }
        Window.Callback callback = this.mWindow.getCallback();
        if (callback != null && !this.mWindow.isDestroyed() && this.mFeatureId < 0) {
            callback.onWindowFocusChanged(z);
        }
        if (this.mPrimaryActionMode != null) {
            this.mPrimaryActionMode.onWindowFocusChanged(z);
        }
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.onWindowFocusChanged(z);
        }
        updateElevation();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window.Callback callback = this.mWindow.getCallback();
        if (callback != null && !this.mWindow.isDestroyed() && this.mFeatureId < 0) {
            callback.onAttachedToWindow();
        }
        if (this.mFeatureId == -1) {
            this.mWindow.openPanelsAfterRestore();
        }
        if (!this.mWindowResizeCallbacksAdded) {
            getViewRootImpl().addWindowCallbacks(this);
            this.mWindowResizeCallbacksAdded = true;
        } else if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.onConfigurationChange();
        }
        this.mWindow.onViewRootImplSet(getViewRootImpl());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Window.Callback callback = this.mWindow.getCallback();
        if (callback != null && this.mFeatureId < 0) {
            callback.onDetachedFromWindow();
        }
        if (this.mWindow.mDecorContentParent != null) {
            this.mWindow.mDecorContentParent.dismissPopups();
        }
        if (this.mPrimaryActionModePopup != null) {
            removeCallbacks(this.mShowPrimaryActionModePopup);
            if (this.mPrimaryActionModePopup.isShowing()) {
                this.mPrimaryActionModePopup.dismiss();
            }
            this.mPrimaryActionModePopup = null;
        }
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        PhoneWindow.PanelFeatureState panelState = this.mWindow.getPanelState(0, false);
        if (panelState != null && panelState.menu != null && this.mFeatureId < 0) {
            panelState.menu.close();
        }
        releaseThreadedRenderer();
        if (this.mWindowResizeCallbacksAdded) {
            getViewRootImpl().removeWindowCallbacks(this);
            this.mWindowResizeCallbacksAdded = false;
        }
    }

    @Override
    public void onCloseSystemDialogs(String str) {
        if (this.mFeatureId >= 0) {
            this.mWindow.closeAllPanels();
        }
    }

    @Override
    public SurfaceHolder.Callback2 willYouTakeTheSurface() {
        if (this.mFeatureId < 0) {
            return this.mWindow.mTakeSurfaceCallback;
        }
        return null;
    }

    @Override
    public InputQueue.Callback willYouTakeTheInputQueue() {
        if (this.mFeatureId < 0) {
            return this.mWindow.mTakeInputQueueCallback;
        }
        return null;
    }

    @Override
    public void setSurfaceType(int i) {
        this.mWindow.setType(i);
    }

    @Override
    public void setSurfaceFormat(int i) {
        this.mWindow.setFormat(i);
    }

    @Override
    public void setSurfaceKeepScreenOn(boolean z) {
        if (z) {
            this.mWindow.addFlags(128);
        } else {
            this.mWindow.clearFlags(128);
        }
    }

    @Override
    public void onRootViewScrollYChanged(int i) {
        this.mRootScrollY = i;
        updateColorViewTranslations();
    }

    @Override
    public void setVisibility(int i) {
        super.setVisibility(i);
        if (ViewDebugManager.DEBUG_USER) {
            Log.v("PhoneWindow", "DecorView setVisiblity: visibility = " + i + ", Parent = " + getParent() + ", this = " + this);
        }
    }

    private ActionMode createActionMode(int i, ActionMode.Callback2 callback2, View view) {
        if (i != 1) {
            return createStandaloneActionMode(callback2);
        }
        return createFloatingActionMode(view, callback2);
    }

    private void setHandledActionMode(ActionMode actionMode) {
        if (actionMode.getType() == 0) {
            setHandledPrimaryActionMode(actionMode);
        } else if (actionMode.getType() == 1) {
            setHandledFloatingActionMode(actionMode);
        }
    }

    private ActionMode createStandaloneActionMode(ActionMode.Callback callback) {
        Context contextThemeWrapper;
        endOnGoingFadeAnimation();
        cleanupPrimaryActionMode();
        if (this.mPrimaryActionModeView == null || !this.mPrimaryActionModeView.isAttachedToWindow()) {
            if (this.mWindow.isFloating()) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = this.mContext.getTheme();
                theme.resolveAttribute(16843825, typedValue, true);
                if (typedValue.resourceId != 0) {
                    Resources.Theme themeNewTheme = this.mContext.getResources().newTheme();
                    themeNewTheme.setTo(theme);
                    themeNewTheme.applyStyle(typedValue.resourceId, true);
                    contextThemeWrapper = new ContextThemeWrapper(this.mContext, 0);
                    contextThemeWrapper.getTheme().setTo(themeNewTheme);
                } else {
                    contextThemeWrapper = this.mContext;
                }
                this.mPrimaryActionModeView = new ActionBarContextView(contextThemeWrapper);
                this.mPrimaryActionModePopup = new PopupWindow(contextThemeWrapper, (AttributeSet) null, R.attr.actionModePopupWindowStyle);
                this.mPrimaryActionModePopup.setWindowLayoutType(2);
                this.mPrimaryActionModePopup.setContentView(this.mPrimaryActionModeView);
                this.mPrimaryActionModePopup.setWidth(-1);
                contextThemeWrapper.getTheme().resolveAttribute(16843499, typedValue, true);
                this.mPrimaryActionModeView.setContentHeight(TypedValue.complexToDimensionPixelSize(typedValue.data, contextThemeWrapper.getResources().getDisplayMetrics()));
                this.mPrimaryActionModePopup.setHeight(-2);
                this.mShowPrimaryActionModePopup = new Runnable() {
                    @Override
                    public void run() {
                        DecorView.this.mPrimaryActionModePopup.showAtLocation(DecorView.this.mPrimaryActionModeView.getApplicationWindowToken(), 55, 0, 0);
                        DecorView.this.endOnGoingFadeAnimation();
                        if (!DecorView.this.shouldAnimatePrimaryActionModeView()) {
                            DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                            DecorView.this.mPrimaryActionModeView.setVisibility(0);
                        } else {
                            DecorView.this.mFadeAnim = ObjectAnimator.ofFloat(DecorView.this.mPrimaryActionModeView, (Property<ActionBarContextView, Float>) View.ALPHA, 0.0f, 1.0f);
                            DecorView.this.mFadeAnim.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animator) {
                                    DecorView.this.mPrimaryActionModeView.setVisibility(0);
                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                                    DecorView.this.mFadeAnim = null;
                                }
                            });
                            DecorView.this.mFadeAnim.start();
                        }
                    }
                };
            } else {
                ViewStub viewStub = (ViewStub) findViewById(R.id.action_mode_bar_stub);
                if (viewStub != null) {
                    this.mPrimaryActionModeView = (ActionBarContextView) viewStub.inflate();
                    this.mPrimaryActionModePopup = null;
                }
            }
        }
        if (this.mPrimaryActionModeView == null) {
            return null;
        }
        this.mPrimaryActionModeView.killMode();
        return new StandaloneActionMode(this.mPrimaryActionModeView.getContext(), this.mPrimaryActionModeView, callback, this.mPrimaryActionModePopup == null);
    }

    private void endOnGoingFadeAnimation() {
        if (this.mFadeAnim != null) {
            this.mFadeAnim.end();
        }
    }

    private void setHandledPrimaryActionMode(ActionMode actionMode) {
        endOnGoingFadeAnimation();
        this.mPrimaryActionMode = actionMode;
        this.mPrimaryActionMode.invalidate();
        this.mPrimaryActionModeView.initForMode(this.mPrimaryActionMode);
        if (this.mPrimaryActionModePopup != null) {
            post(this.mShowPrimaryActionModePopup);
        } else if (shouldAnimatePrimaryActionModeView()) {
            this.mFadeAnim = ObjectAnimator.ofFloat(this.mPrimaryActionModeView, (Property<ActionBarContextView, Float>) View.ALPHA, 0.0f, 1.0f);
            this.mFadeAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    DecorView.this.mPrimaryActionModeView.setVisibility(0);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                    DecorView.this.mFadeAnim = null;
                }
            });
            this.mFadeAnim.start();
        } else {
            this.mPrimaryActionModeView.setAlpha(1.0f);
            this.mPrimaryActionModeView.setVisibility(0);
        }
        this.mPrimaryActionModeView.sendAccessibilityEvent(32);
    }

    boolean shouldAnimatePrimaryActionModeView() {
        return isLaidOut();
    }

    private ActionMode createFloatingActionMode(View view, ActionMode.Callback2 callback2) {
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        this.mFloatingToolbar = new FloatingToolbar(this.mWindow);
        final FloatingActionMode floatingActionMode = new FloatingActionMode(this.mContext, callback2, view, this.mFloatingToolbar);
        this.mFloatingActionModeOriginatingView = view;
        this.mFloatingToolbarPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                floatingActionMode.updateViewLocationInWindow();
                return true;
            }
        };
        return floatingActionMode;
    }

    private void setHandledFloatingActionMode(ActionMode actionMode) {
        this.mFloatingActionMode = actionMode;
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    void enableCaption(boolean z) {
        if (this.mHasCaption != z) {
            this.mHasCaption = z;
            if (getForeground() != null) {
                drawableChanged();
            }
        }
    }

    void setWindow(PhoneWindow phoneWindow) {
        this.mWindow = phoneWindow;
        Context context = getContext();
        if (context instanceof DecorContext) {
            ((DecorContext) context).setPhoneWindow(this.mWindow);
        }
    }

    @Override
    public Resources getResources() {
        return getContext().getResources();
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        boolean zHasWindowDecorCaption = configuration.windowConfiguration.hasWindowDecorCaption();
        if (this.mDecorCaptionView == null && zHasWindowDecorCaption) {
            this.mDecorCaptionView = createDecorCaptionView(this.mWindow.getLayoutInflater());
            if (this.mDecorCaptionView != null) {
                if (this.mDecorCaptionView.getParent() == null) {
                    addView(this.mDecorCaptionView, 0, new ViewGroup.LayoutParams(-1, -1));
                }
                removeView(this.mContentRoot);
                this.mDecorCaptionView.addView(this.mContentRoot, new ViewGroup.MarginLayoutParams(-1, -1));
            }
        } else if (this.mDecorCaptionView != null) {
            this.mDecorCaptionView.onConfigurationChanged(zHasWindowDecorCaption);
            enableCaption(zHasWindowDecorCaption);
        }
        updateAvailableWidth();
        initializeElevation();
    }

    void onResourcesLoaded(LayoutInflater layoutInflater, int i) {
        if (this.mBackdropFrameRenderer != null) {
            loadBackgroundDrawablesIfNeeded();
            this.mBackdropFrameRenderer.onResourcesLoaded(this, this.mResizingBackgroundDrawable, this.mCaptionBackgroundDrawable, this.mUserCaptionBackgroundDrawable, getCurrentColor(this.mStatusColorViewState), getCurrentColor(this.mNavigationColorViewState));
        }
        this.mDecorCaptionView = createDecorCaptionView(layoutInflater);
        View viewInflate = layoutInflater.inflate(i, (ViewGroup) null);
        if (this.mDecorCaptionView != null) {
            if (this.mDecorCaptionView.getParent() == null) {
                addView(this.mDecorCaptionView, new ViewGroup.LayoutParams(-1, -1));
            }
            this.mDecorCaptionView.addView(viewInflate, new ViewGroup.MarginLayoutParams(-1, -1));
        } else {
            addView(viewInflate, 0, new ViewGroup.LayoutParams(-1, -1));
        }
        this.mContentRoot = (ViewGroup) viewInflate;
        initializeElevation();
    }

    private void loadBackgroundDrawablesIfNeeded() {
        if (this.mResizingBackgroundDrawable == null) {
            this.mResizingBackgroundDrawable = getResizingBackgroundDrawable(getContext(), this.mWindow.mBackgroundResource, this.mWindow.mBackgroundFallbackResource, this.mWindow.isTranslucent() || this.mWindow.isShowingWallpaper());
            if (this.mResizingBackgroundDrawable == null) {
                Log.w(this.mLogTag, "Failed to find background drawable for PhoneWindow=" + this.mWindow);
            }
        }
        if (this.mCaptionBackgroundDrawable == null) {
            this.mCaptionBackgroundDrawable = getContext().getDrawable(R.drawable.decor_caption_title_focused);
        }
        if (this.mResizingBackgroundDrawable != null) {
            this.mLastBackgroundDrawableCb = this.mResizingBackgroundDrawable.getCallback();
            this.mResizingBackgroundDrawable.setCallback(null);
        }
    }

    private DecorCaptionView createDecorCaptionView(LayoutInflater layoutInflater) {
        DecorCaptionView decorCaptionViewInflateDecorCaptionView = null;
        DecorCaptionView decorCaptionView = null;
        for (int childCount = getChildCount() - 1; childCount >= 0 && decorCaptionView == null; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt instanceof DecorCaptionView) {
                removeViewAt(childCount);
                decorCaptionView = (DecorCaptionView) childAt;
            }
        }
        WindowManager.LayoutParams attributes = this.mWindow.getAttributes();
        boolean z = attributes.type == 1 || attributes.type == 2 || attributes.type == 4;
        WindowConfiguration windowConfiguration = getResources().getConfiguration().windowConfiguration;
        if (!this.mWindow.isFloating() && z && windowConfiguration.hasWindowDecorCaption()) {
            decorCaptionViewInflateDecorCaptionView = decorCaptionView == null ? inflateDecorCaptionView(layoutInflater) : decorCaptionView;
            decorCaptionViewInflateDecorCaptionView.setPhoneWindow(this.mWindow, true);
        }
        enableCaption(decorCaptionViewInflateDecorCaptionView != null);
        return decorCaptionViewInflateDecorCaptionView;
    }

    private DecorCaptionView inflateDecorCaptionView(LayoutInflater layoutInflater) {
        Context context = getContext();
        DecorCaptionView decorCaptionView = (DecorCaptionView) LayoutInflater.from(context).inflate(R.layout.decor_caption, (ViewGroup) null);
        setDecorCaptionShade(context, decorCaptionView);
        return decorCaptionView;
    }

    private void setDecorCaptionShade(Context context, DecorCaptionView decorCaptionView) {
        switch (this.mWindow.getDecorCaptionShade()) {
            case 1:
                setLightDecorCaptionShade(decorCaptionView);
                break;
            case 2:
                setDarkDecorCaptionShade(decorCaptionView);
                break;
            default:
                context.getTheme().resolveAttribute(16843827, new TypedValue(), true);
                if (Color.luminance(r0.data) < 0.5d) {
                    setLightDecorCaptionShade(decorCaptionView);
                } else {
                    setDarkDecorCaptionShade(decorCaptionView);
                }
                break;
        }
    }

    void updateDecorCaptionShade() {
        if (this.mDecorCaptionView != null) {
            setDecorCaptionShade(getContext(), this.mDecorCaptionView);
        }
    }

    private void setLightDecorCaptionShade(DecorCaptionView decorCaptionView) {
        decorCaptionView.findViewById(R.id.maximize_window).setBackgroundResource(R.drawable.decor_maximize_button_light);
        decorCaptionView.findViewById(R.id.close_window).setBackgroundResource(R.drawable.decor_close_button_light);
    }

    private void setDarkDecorCaptionShade(DecorCaptionView decorCaptionView) {
        decorCaptionView.findViewById(R.id.maximize_window).setBackgroundResource(R.drawable.decor_maximize_button_dark);
        decorCaptionView.findViewById(R.id.close_window).setBackgroundResource(R.drawable.decor_close_button_dark);
    }

    public static Drawable getResizingBackgroundDrawable(Context context, int i, int i2, boolean z) {
        Drawable drawable;
        Drawable drawable2;
        if (i != 0 && (drawable2 = context.getDrawable(i)) != null) {
            return enforceNonTranslucentBackground(drawable2, z);
        }
        if (i2 != 0 && (drawable = context.getDrawable(i2)) != null) {
            return enforceNonTranslucentBackground(drawable, z);
        }
        return new ColorDrawable(-16777216);
    }

    private static Drawable enforceNonTranslucentBackground(Drawable drawable, boolean z) {
        if (!z && (drawable instanceof ColorDrawable)) {
            ColorDrawable colorDrawable = (ColorDrawable) drawable;
            int color = colorDrawable.getColor();
            if (Color.alpha(color) != 255) {
                ColorDrawable colorDrawable2 = (ColorDrawable) colorDrawable.getConstantState().newDrawable().mutate();
                colorDrawable2.setColor(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
                return colorDrawable2;
            }
        }
        return drawable;
    }

    void clearContentView() {
        if (this.mDecorCaptionView != null) {
            this.mDecorCaptionView.removeContentView();
            return;
        }
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt != this.mStatusColorViewState.view && childAt != this.mNavigationColorViewState.view && childAt != this.mStatusGuard) {
                removeViewAt(childCount);
            }
        }
    }

    @Override
    public void onWindowSizeIsChanging(Rect rect, boolean z, Rect rect2, Rect rect3) {
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.setTargetRect(rect, z, rect2, rect3);
        }
    }

    @Override
    public void onWindowDragResizeStart(Rect rect, boolean z, Rect rect2, Rect rect3, int i) {
        if (this.mWindow.isDestroyed()) {
            releaseThreadedRenderer();
            return;
        }
        if (this.mBackdropFrameRenderer != null) {
            return;
        }
        ThreadedRenderer threadedRenderer = getThreadedRenderer();
        if (threadedRenderer != null) {
            loadBackgroundDrawablesIfNeeded();
            this.mBackdropFrameRenderer = new BackdropFrameRenderer(this, threadedRenderer, rect, this.mResizingBackgroundDrawable, this.mCaptionBackgroundDrawable, this.mUserCaptionBackgroundDrawable, getCurrentColor(this.mStatusColorViewState), getCurrentColor(this.mNavigationColorViewState), z, rect2, rect3, i);
            updateElevation();
            updateColorViews(null, false);
        }
        this.mResizeMode = i;
        getViewRootImpl().requestInvalidateRootRenderNode();
    }

    @Override
    public void onWindowDragResizeEnd() {
        releaseThreadedRenderer();
        updateColorViews(null, false);
        this.mResizeMode = -1;
        getViewRootImpl().requestInvalidateRootRenderNode();
    }

    @Override
    public boolean onContentDrawn(int i, int i2, int i3, int i4) {
        if (this.mBackdropFrameRenderer == null) {
            return false;
        }
        return this.mBackdropFrameRenderer.onContentDrawn(i, i2, i3, i4);
    }

    @Override
    public void onRequestDraw(boolean z) {
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.onRequestDraw(z);
        } else if (z && isAttachedToWindow()) {
            getViewRootImpl().reportDrawFinish();
        }
    }

    @Override
    public void onPostDraw(DisplayListCanvas displayListCanvas) {
        drawResizingShadowIfNeeded(displayListCanvas);
    }

    private void initResizingPaints() {
        int color = this.mContext.getResources().getColor(R.color.resize_shadow_start_color, null);
        int color2 = this.mContext.getResources().getColor(R.color.resize_shadow_end_color, null);
        int i = (color + color2) / 2;
        this.mHorizontalResizeShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, this.mResizeShadowSize, new int[]{color, i, color2}, new float[]{0.0f, 0.3f, 1.0f}, Shader.TileMode.CLAMP));
        this.mVerticalResizeShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, this.mResizeShadowSize, 0.0f, new int[]{color, i, color2}, new float[]{0.0f, 0.3f, 1.0f}, Shader.TileMode.CLAMP));
    }

    private void drawResizingShadowIfNeeded(DisplayListCanvas displayListCanvas) {
        if (this.mResizeMode != 1 || this.mWindow.mIsFloating || this.mWindow.isTranslucent() || this.mWindow.isShowingWallpaper()) {
            return;
        }
        displayListCanvas.save();
        displayListCanvas.translate(0.0f, getHeight() - this.mFrameOffsets.bottom);
        displayListCanvas.drawRect(0.0f, 0.0f, getWidth(), this.mResizeShadowSize, this.mHorizontalResizeShadowPaint);
        displayListCanvas.restore();
        displayListCanvas.save();
        displayListCanvas.translate(getWidth() - this.mFrameOffsets.right, 0.0f);
        displayListCanvas.drawRect(0.0f, 0.0f, this.mResizeShadowSize, getHeight(), this.mVerticalResizeShadowPaint);
        displayListCanvas.restore();
    }

    private void releaseThreadedRenderer() {
        if (this.mResizingBackgroundDrawable != null && this.mLastBackgroundDrawableCb != null) {
            this.mResizingBackgroundDrawable.setCallback(this.mLastBackgroundDrawableCb);
            this.mLastBackgroundDrawableCb = null;
        }
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.releaseRenderer();
            this.mBackdropFrameRenderer = null;
            updateElevation();
        }
    }

    private boolean isResizing() {
        return this.mBackdropFrameRenderer != null;
    }

    private void initializeElevation() {
        this.mAllowUpdateElevation = false;
        updateElevation();
    }

    private void updateElevation() {
        float fDipToPx;
        boolean z = this.mElevationAdjustedForStack;
        int windowingMode = getResources().getConfiguration().windowConfiguration.getWindowingMode();
        if (windowingMode == 5 && !isResizing()) {
            float f = hasWindowFocus() ? 20.0f : 5.0f;
            if (!this.mAllowUpdateElevation) {
                f = 20.0f;
            }
            fDipToPx = dipToPx(f);
            this.mElevationAdjustedForStack = true;
        } else if (windowingMode == 2) {
            fDipToPx = dipToPx(5.0f);
            this.mElevationAdjustedForStack = true;
        } else {
            this.mElevationAdjustedForStack = false;
            fDipToPx = 0.0f;
        }
        if ((z || this.mElevationAdjustedForStack) && getElevation() != fDipToPx) {
            this.mWindow.setElevation(fDipToPx);
        }
    }

    boolean isShowingCaption() {
        return this.mDecorCaptionView != null && this.mDecorCaptionView.isCaptionShowing();
    }

    int getCaptionHeight() {
        if (isShowingCaption()) {
            return this.mDecorCaptionView.getCaptionHeight();
        }
        return 0;
    }

    private float dipToPx(float f) {
        return TypedValue.applyDimension(1, f, getResources().getDisplayMetrics());
    }

    void setUserCaptionBackgroundDrawable(Drawable drawable) {
        this.mUserCaptionBackgroundDrawable = drawable;
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.setUserCaptionBackgroundDrawable(drawable);
        }
    }

    private static String getTitleSuffix(WindowManager.LayoutParams layoutParams) {
        if (layoutParams == null) {
            return "";
        }
        String[] strArrSplit = layoutParams.getTitle().toString().split("\\.");
        if (strArrSplit.length > 0) {
            return strArrSplit[strArrSplit.length - 1];
        }
        return "";
    }

    void updateLogTag(WindowManager.LayoutParams layoutParams) {
        this.mLogTag = "DecorView[" + getTitleSuffix(layoutParams) + "]";
    }

    private void updateAvailableWidth() {
        this.mAvailableWidth = TypedValue.applyDimension(1, r0.getConfiguration().screenWidthDp, getResources().getDisplayMetrics());
    }

    @Override
    public void requestKeyboardShortcuts(List<KeyboardShortcutGroup> list, int i) {
        PhoneWindow.PanelFeatureState panelState = this.mWindow.getPanelState(0, false);
        MenuBuilder menuBuilder = panelState != null ? panelState.menu : null;
        if (!this.mWindow.isDestroyed() && this.mWindow.getCallback() != null) {
            this.mWindow.getCallback().onProvideKeyboardShortcuts(list, menuBuilder, i);
        }
    }

    @Override
    public void dispatchPointerCaptureChanged(boolean z) {
        super.dispatchPointerCaptureChanged(z);
        if (!this.mWindow.isDestroyed() && this.mWindow.getCallback() != null) {
            this.mWindow.getCallback().onPointerCaptureChanged(z);
        }
    }

    @Override
    public int getAccessibilityViewId() {
        return 2147483646;
    }

    @Override
    public String toString() {
        if (this.mWindow == null) {
            return "DecorView@" + Integer.toHexString(hashCode()) + "[null]";
        }
        return "DecorView@" + Integer.toHexString(hashCode()) + "[" + getTitleSuffix(this.mWindow.getAttributes()) + "]";
    }

    private static class ColorViewState {
        final ColorViewAttributes attributes;
        int color;
        boolean visible;
        View view = null;
        int targetVisibility = 4;
        boolean present = false;

        ColorViewState(ColorViewAttributes colorViewAttributes) {
            this.attributes = colorViewAttributes;
        }
    }

    public static class ColorViewAttributes {
        final int hideWindowFlag;
        final int horizontalGravity;
        final int id;
        final int seascapeGravity;
        final int systemUiHideFlag;
        final String transitionName;
        final int translucentFlag;
        final int verticalGravity;

        private ColorViewAttributes(int i, int i2, int i3, int i4, int i5, String str, int i6, int i7) {
            this.id = i6;
            this.systemUiHideFlag = i;
            this.translucentFlag = i2;
            this.verticalGravity = i3;
            this.horizontalGravity = i4;
            this.seascapeGravity = i5;
            this.transitionName = str;
            this.hideWindowFlag = i7;
        }

        public boolean isPresent(int i, int i2, boolean z) {
            return (i & this.systemUiHideFlag) == 0 && (this.hideWindowFlag & i2) == 0 && ((Integer.MIN_VALUE & i2) != 0 || z);
        }

        public boolean isVisible(boolean z, int i, int i2, boolean z2) {
            return z && ((-16777216) & i) != 0 && ((this.translucentFlag & i2) == 0 || z2);
        }

        public boolean isVisible(int i, int i2, int i3, boolean z) {
            return isVisible(isPresent(i, i3, z), i2, i3, z);
        }
    }

    private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback callback) {
            this.mWrapped = callback;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            return this.mWrapped.onCreateActionMode(actionMode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            DecorView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(actionMode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return this.mWrapped.onActionItemClicked(actionMode, menuItem);
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            boolean z;
            this.mWrapped.onDestroyActionMode(actionMode);
            if (DecorView.this.mContext.getApplicationInfo().targetSdkVersion >= 23) {
                z = actionMode == DecorView.this.mPrimaryActionMode;
                z = actionMode == DecorView.this.mFloatingActionMode;
                if (!z && actionMode.getType() == 0) {
                    Log.e(DecorView.this.mLogTag, "Destroying unexpected ActionMode instance of TYPE_PRIMARY; " + actionMode + " was not the current primary action mode! Expected " + DecorView.this.mPrimaryActionMode);
                }
                if (!z && actionMode.getType() == 1) {
                    Log.e(DecorView.this.mLogTag, "Destroying unexpected ActionMode instance of TYPE_FLOATING; " + actionMode + " was not the current floating action mode! Expected " + DecorView.this.mFloatingActionMode);
                }
            } else {
                z = actionMode.getType() == 0;
                if (actionMode.getType() == 1) {
                    z = true;
                }
            }
            if (z) {
                if (DecorView.this.mPrimaryActionModePopup != null) {
                    DecorView.this.removeCallbacks(DecorView.this.mShowPrimaryActionModePopup);
                }
                if (DecorView.this.mPrimaryActionModeView != null) {
                    DecorView.this.endOnGoingFadeAnimation();
                    final ActionBarContextView actionBarContextView = DecorView.this.mPrimaryActionModeView;
                    DecorView.this.mFadeAnim = ObjectAnimator.ofFloat(DecorView.this.mPrimaryActionModeView, (Property<ActionBarContextView, Float>) View.ALPHA, 1.0f, 0.0f);
                    DecorView.this.mFadeAnim.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (actionBarContextView == DecorView.this.mPrimaryActionModeView) {
                                actionBarContextView.setVisibility(8);
                                if (DecorView.this.mPrimaryActionModePopup != null) {
                                    DecorView.this.mPrimaryActionModePopup.dismiss();
                                }
                                actionBarContextView.killMode();
                                DecorView.this.mFadeAnim = null;
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });
                    DecorView.this.mFadeAnim.start();
                }
                DecorView.this.mPrimaryActionMode = null;
            } else if (z) {
                DecorView.this.cleanupFloatingActionModeViews();
                DecorView.this.mFloatingActionMode = null;
            }
            if (DecorView.this.mWindow.getCallback() != null && !DecorView.this.mWindow.isDestroyed()) {
                try {
                    DecorView.this.mWindow.getCallback().onActionModeFinished(actionMode);
                } catch (AbstractMethodError e) {
                }
            }
            DecorView.this.requestFitSystemWindows();
        }

        @Override
        public void onGetContentRect(ActionMode actionMode, View view, Rect rect) {
            if (this.mWrapped instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) this.mWrapped).onGetContentRect(actionMode, view, rect);
            } else {
                super.onGetContentRect(actionMode, view, rect);
            }
        }
    }
}
