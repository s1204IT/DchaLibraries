package com.android.internal.policy.impl;

import android.R;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.IRotationWatcher;
import android.view.IWindowManager;
import android.view.InputEvent;
import android.view.InputQueue;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.view.RootViewSurfaceTaker;
import com.android.internal.view.StandaloneActionMode;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.internal.view.menu.IconMenuPresenter;
import com.android.internal.view.menu.ListMenuPresenter;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuDialogHelper;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.view.menu.MenuView;
import com.android.internal.widget.ActionBarContextView;
import com.android.internal.widget.BackgroundFallback;
import com.android.internal.widget.DecorContentParent;
import com.android.internal.widget.SwipeDismissLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PhoneWindow extends Window implements MenuBuilder.Callback {
    private static final String ACTION_BAR_TAG = "android:ActionBar";
    private static final int CUSTOM_TITLE_COMPATIBLE_FEATURES = 13505;
    private static final int DEFAULT_BACKGROUND_FADE_DURATION_MS = 300;
    static final int FLAG_RESOURCE_SET_ICON = 1;
    static final int FLAG_RESOURCE_SET_ICON_FALLBACK = 4;
    static final int FLAG_RESOURCE_SET_LOGO = 2;
    private static final String FOCUSED_ID_TAG = "android:focusedViewId";
    private static final String PANELS_TAG = "android:Panels";
    private static final boolean SWEEP_OPEN_MENU = false;
    private static final String TAG = "PhoneWindow";
    private static final String VIEWS_TAG = "android:views";
    private ActionMenuPresenterCallback mActionMenuPresenterCallback;
    private Boolean mAllowEnterTransitionOverlap;
    private Boolean mAllowReturnTransitionOverlap;
    private boolean mAlwaysReadCloseOnTouchAttr;
    private AudioManager mAudioManager;
    private Drawable mBackgroundDrawable;
    private long mBackgroundFadeDurationMillis;
    private int mBackgroundFallbackResource;
    private int mBackgroundResource;
    private ProgressBar mCircularProgressBar;
    private boolean mClipToOutline;
    private boolean mClosingActionMenu;
    private ViewGroup mContentParent;
    private ViewGroup mContentRoot;
    private Scene mContentScene;
    private ContextMenuBuilder mContextMenu;
    final DialogMenuCallback mContextMenuCallback;
    private MenuDialogHelper mContextMenuHelper;
    private DecorView mDecor;
    private DecorContentParent mDecorContentParent;
    private DrawableFeatureState[] mDrawables;
    private float mElevation;
    private Transition mEnterTransition;
    private Transition mExitTransition;
    TypedValue mFixedHeightMajor;
    TypedValue mFixedHeightMinor;
    TypedValue mFixedWidthMajor;
    TypedValue mFixedWidthMinor;
    private boolean mForcedNavigationBarColor;
    private boolean mForcedStatusBarColor;
    private int mFrameResource;
    private ProgressBar mHorizontalProgressBar;
    int mIconRes;
    private int mInvalidatePanelMenuFeatures;
    private boolean mInvalidatePanelMenuPosted;
    private final Runnable mInvalidatePanelMenuRunnable;
    private boolean mIsFloating;
    private KeyguardManager mKeyguardManager;
    private LayoutInflater mLayoutInflater;
    private ImageView mLeftIconView;
    int mLogoRes;
    private MediaController mMediaController;
    final TypedValue mMinWidthMajor;
    final TypedValue mMinWidthMinor;
    private int mNavigationBarColor;
    TypedValue mOutsetBottom;
    private int mPanelChordingKey;
    private PanelMenuPresenterCallback mPanelMenuPresenterCallback;
    private PanelFeatureState[] mPanels;
    private PanelFeatureState mPreparedPanel;
    private Transition mReenterTransition;
    int mResourcesSetFlags;
    private Transition mReturnTransition;
    private ImageView mRightIconView;
    private Transition mSharedElementEnterTransition;
    private Transition mSharedElementExitTransition;
    private Transition mSharedElementReenterTransition;
    private Transition mSharedElementReturnTransition;
    private Boolean mSharedElementsUseOverlay;
    private int mStatusBarColor;
    InputQueue.Callback mTakeInputQueueCallback;
    SurfaceHolder.Callback2 mTakeSurfaceCallback;
    private Rect mTempRect;
    private int mTextColor;
    private CharSequence mTitle;
    private int mTitleColor;
    private TextView mTitleView;
    private TransitionManager mTransitionManager;
    private int mUiOptions;
    private int mVolumeControlStreamType;
    private static final Transition USE_DEFAULT_TRANSITION = new TransitionSet();
    static final RotationWatcher sRotationWatcher = new RotationWatcher();

    static class WindowManagerHolder {
        static final IWindowManager sWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        WindowManagerHolder() {
        }
    }

    public PhoneWindow(Context context) {
        super(context);
        this.mContextMenuCallback = new DialogMenuCallback(6);
        this.mMinWidthMajor = new TypedValue();
        this.mMinWidthMinor = new TypedValue();
        this.mBackgroundResource = 0;
        this.mBackgroundFallbackResource = 0;
        this.mFrameResource = 0;
        this.mTextColor = 0;
        this.mStatusBarColor = 0;
        this.mNavigationBarColor = 0;
        this.mForcedStatusBarColor = SWEEP_OPEN_MENU;
        this.mForcedNavigationBarColor = SWEEP_OPEN_MENU;
        this.mTitle = null;
        this.mTitleColor = 0;
        this.mAlwaysReadCloseOnTouchAttr = SWEEP_OPEN_MENU;
        this.mVolumeControlStreamType = Integer.MIN_VALUE;
        this.mUiOptions = 0;
        this.mInvalidatePanelMenuRunnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 13; i++) {
                    if ((PhoneWindow.this.mInvalidatePanelMenuFeatures & (1 << i)) != 0) {
                        PhoneWindow.this.doInvalidatePanelMenu(i);
                    }
                }
                PhoneWindow.this.mInvalidatePanelMenuPosted = PhoneWindow.SWEEP_OPEN_MENU;
                PhoneWindow.this.mInvalidatePanelMenuFeatures = 0;
            }
        };
        this.mEnterTransition = null;
        this.mReturnTransition = USE_DEFAULT_TRANSITION;
        this.mExitTransition = null;
        this.mReenterTransition = USE_DEFAULT_TRANSITION;
        this.mSharedElementEnterTransition = null;
        this.mSharedElementReturnTransition = USE_DEFAULT_TRANSITION;
        this.mSharedElementExitTransition = null;
        this.mSharedElementReenterTransition = USE_DEFAULT_TRANSITION;
        this.mBackgroundFadeDurationMillis = -1L;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public final void setContainer(Window container) {
        super.setContainer(container);
    }

    @Override
    public boolean requestFeature(int featureId) {
        if (this.mContentParent != null) {
            throw new AndroidRuntimeException("requestFeature() must be called before adding content");
        }
        int features = getFeatures();
        int newFeatures = features | (1 << featureId);
        if ((newFeatures & 128) != 0 && (newFeatures & (-13506)) != 0) {
            throw new AndroidRuntimeException("You cannot combine custom titles with other title features");
        }
        if ((features & 2) != 0 && featureId == 8) {
            return SWEEP_OPEN_MENU;
        }
        if ((features & 256) != 0 && featureId == 1) {
            removeFeature(8);
        }
        if ((features & 256) != 0 && featureId == 11) {
            throw new AndroidRuntimeException("You cannot combine swipe dismissal and the action bar.");
        }
        if ((features & 2048) != 0 && featureId == 8) {
            throw new AndroidRuntimeException("You cannot combine swipe dismissal and the action bar.");
        }
        if (featureId == 5 && getContext().getPackageManager().hasSystemFeature("android.hardware.type.watch")) {
            throw new AndroidRuntimeException("You cannot use indeterminate progress on a watch.");
        }
        return super.requestFeature(featureId);
    }

    @Override
    public void setUiOptions(int uiOptions) {
        this.mUiOptions = uiOptions;
    }

    @Override
    public void setUiOptions(int uiOptions, int mask) {
        this.mUiOptions = (this.mUiOptions & (mask ^ (-1))) | (uiOptions & mask);
    }

    @Override
    public TransitionManager getTransitionManager() {
        return this.mTransitionManager;
    }

    @Override
    public void setTransitionManager(TransitionManager tm) {
        this.mTransitionManager = tm;
    }

    @Override
    public Scene getContentScene() {
        return this.mContentScene;
    }

    @Override
    public void setContentView(int layoutResID) {
        if (this.mContentParent == null) {
            installDecor();
        } else if (!hasFeature(12)) {
            this.mContentParent.removeAllViews();
        }
        if (hasFeature(12)) {
            Scene newScene = Scene.getSceneForLayout(this.mContentParent, layoutResID, getContext());
            transitionTo(newScene);
        } else {
            this.mLayoutInflater.inflate(layoutResID, this.mContentParent);
        }
        Window.Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }

    @Override
    public void setContentView(View view) {
        setContentView(view, new ViewGroup.LayoutParams(-1, -1));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (this.mContentParent == null) {
            installDecor();
        } else if (!hasFeature(12)) {
            this.mContentParent.removeAllViews();
        }
        if (hasFeature(12)) {
            view.setLayoutParams(params);
            Scene newScene = new Scene(this.mContentParent, view);
            transitionTo(newScene);
        } else {
            this.mContentParent.addView(view, params);
        }
        Window.Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if (this.mContentParent == null) {
            installDecor();
        }
        if (hasFeature(12)) {
            Log.v(TAG, "addContentView does not support content transitions");
        }
        this.mContentParent.addView(view, params);
        Window.Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }

    private void transitionTo(Scene scene) {
        if (this.mContentScene == null) {
            scene.enter();
        } else {
            this.mTransitionManager.transitionTo(scene);
        }
        this.mContentScene = scene;
    }

    @Override
    public View getCurrentFocus() {
        if (this.mDecor != null) {
            return this.mDecor.findFocus();
        }
        return null;
    }

    @Override
    public void takeSurface(SurfaceHolder.Callback2 callback) {
        this.mTakeSurfaceCallback = callback;
    }

    @Override
    public void takeInputQueue(InputQueue.Callback callback) {
        this.mTakeInputQueueCallback = callback;
    }

    @Override
    public boolean isFloating() {
        return this.mIsFloating;
    }

    @Override
    public LayoutInflater getLayoutInflater() {
        return this.mLayoutInflater;
    }

    @Override
    public void setTitle(CharSequence title) {
        if (this.mTitleView != null) {
            this.mTitleView.setText(title);
        } else if (this.mDecorContentParent != null) {
            this.mDecorContentParent.setWindowTitle(title);
        }
        this.mTitle = title;
    }

    @Override
    @Deprecated
    public void setTitleColor(int textColor) {
        if (this.mTitleView != null) {
            this.mTitleView.setTextColor(textColor);
        }
        this.mTitleColor = textColor;
    }

    public final boolean preparePanel(PanelFeatureState st, KeyEvent event) {
        if (isDestroyed()) {
            return SWEEP_OPEN_MENU;
        }
        if (st.isPrepared) {
            return true;
        }
        if (this.mPreparedPanel != null && this.mPreparedPanel != st) {
            closePanel(this.mPreparedPanel, SWEEP_OPEN_MENU);
        }
        Window.Callback cb = getCallback();
        if (cb != null) {
            st.createdPanelView = cb.onCreatePanelView(st.featureId);
        }
        boolean isActionBarMenu = st.featureId == 0 || st.featureId == 8;
        if (isActionBarMenu && this.mDecorContentParent != null) {
            this.mDecorContentParent.setMenuPrepared();
        }
        if (st.createdPanelView == null) {
            if (st.menu == null || st.refreshMenuContent) {
                if (st.menu == null && (!initializePanelMenu(st) || st.menu == null)) {
                    return SWEEP_OPEN_MENU;
                }
                if (isActionBarMenu && this.mDecorContentParent != null) {
                    if (this.mActionMenuPresenterCallback == null) {
                        this.mActionMenuPresenterCallback = new ActionMenuPresenterCallback();
                    }
                    this.mDecorContentParent.setMenu(st.menu, this.mActionMenuPresenterCallback);
                }
                st.menu.stopDispatchingItemsChanged();
                if (cb == null || !cb.onCreatePanelMenu(st.featureId, st.menu)) {
                    st.setMenu(null);
                    if (!isActionBarMenu || this.mDecorContentParent == null) {
                        return SWEEP_OPEN_MENU;
                    }
                    this.mDecorContentParent.setMenu((Menu) null, this.mActionMenuPresenterCallback);
                    return SWEEP_OPEN_MENU;
                }
                st.refreshMenuContent = SWEEP_OPEN_MENU;
            }
            st.menu.stopDispatchingItemsChanged();
            if (st.frozenActionViewState != null) {
                st.menu.restoreActionViewStates(st.frozenActionViewState);
                st.frozenActionViewState = null;
            }
            if (!cb.onPreparePanel(st.featureId, st.createdPanelView, st.menu)) {
                if (isActionBarMenu && this.mDecorContentParent != null) {
                    this.mDecorContentParent.setMenu((Menu) null, this.mActionMenuPresenterCallback);
                }
                st.menu.startDispatchingItemsChanged();
                return SWEEP_OPEN_MENU;
            }
            KeyCharacterMap kmap = KeyCharacterMap.load(event != null ? event.getDeviceId() : -1);
            st.qwertyMode = kmap.getKeyboardType() != 1;
            st.menu.setQwertyMode(st.qwertyMode);
            st.menu.startDispatchingItemsChanged();
        }
        st.isPrepared = true;
        st.isHandled = SWEEP_OPEN_MENU;
        this.mPreparedPanel = st;
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        PanelFeatureState st;
        if (this.mDecorContentParent == null && (st = getPanelState(0, SWEEP_OPEN_MENU)) != null && st.menu != null) {
            if (st.isOpen) {
                Bundle state = new Bundle();
                if (st.iconMenuPresenter != null) {
                    st.iconMenuPresenter.saveHierarchyState(state);
                }
                if (st.listMenuPresenter != null) {
                    st.listMenuPresenter.saveHierarchyState(state);
                }
                clearMenuViews(st);
                reopenMenu(SWEEP_OPEN_MENU);
                if (st.iconMenuPresenter != null) {
                    st.iconMenuPresenter.restoreHierarchyState(state);
                }
                if (st.listMenuPresenter != null) {
                    st.listMenuPresenter.restoreHierarchyState(state);
                    return;
                }
                return;
            }
            clearMenuViews(st);
        }
    }

    private static void clearMenuViews(PanelFeatureState st) {
        st.createdPanelView = null;
        st.refreshDecorView = true;
        st.clearMenuPresenters();
    }

    @Override
    public final void openPanel(int featureId, KeyEvent event) {
        if (featureId == 0 && this.mDecorContentParent != null && this.mDecorContentParent.canShowOverflowMenu() && !ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
            this.mDecorContentParent.showOverflowMenu();
        } else {
            openPanel(getPanelState(featureId, true), event);
        }
    }

    private void openPanel(PanelFeatureState st, KeyEvent event) {
        int backgroundResId;
        ViewGroup.LayoutParams lp;
        if (!st.isOpen && !isDestroyed()) {
            if (st.featureId == 0) {
                Context context = getContext();
                Configuration config = context.getResources().getConfiguration();
                boolean isXLarge = (config.screenLayout & 15) == FLAG_RESOURCE_SET_ICON_FALLBACK ? true : SWEEP_OPEN_MENU;
                boolean isHoneycombApp = context.getApplicationInfo().targetSdkVersion >= 11 ? true : SWEEP_OPEN_MENU;
                if (isXLarge && isHoneycombApp) {
                    return;
                }
            }
            Window.Callback cb = getCallback();
            if (cb != null && !cb.onMenuOpened(st.featureId, st.menu)) {
                closePanel(st, true);
                return;
            }
            WindowManager wm = getWindowManager();
            if (wm != null && preparePanel(st, event)) {
                int width = -2;
                if (st.decorView == null || st.refreshDecorView) {
                    if (st.decorView == null) {
                        if (!initializePanelDecor(st) || st.decorView == null) {
                            return;
                        }
                    } else if (st.refreshDecorView && st.decorView.getChildCount() > 0) {
                        st.decorView.removeAllViews();
                    }
                    if (initializePanelContent(st) && st.hasPanelItems()) {
                        ViewGroup.LayoutParams lp2 = st.shownPanelView.getLayoutParams();
                        if (lp2 == null) {
                            lp2 = new ViewGroup.LayoutParams(-2, -2);
                        }
                        if (lp2.width == -1) {
                            backgroundResId = st.fullBackground;
                            width = -1;
                        } else {
                            backgroundResId = st.background;
                        }
                        st.decorView.setWindowBackground(getContext().getDrawable(backgroundResId));
                        ViewParent shownPanelParent = st.shownPanelView.getParent();
                        if (shownPanelParent != null && (shownPanelParent instanceof ViewGroup)) {
                            ((ViewGroup) shownPanelParent).removeView(st.shownPanelView);
                        }
                        st.decorView.addView(st.shownPanelView, lp2);
                        if (!st.shownPanelView.hasFocus()) {
                            st.shownPanelView.requestFocus();
                        }
                    } else {
                        return;
                    }
                } else if (!st.isInListMode()) {
                    width = -1;
                } else if (st.createdPanelView != null && (lp = st.createdPanelView.getLayoutParams()) != null && lp.width == -1) {
                    width = -1;
                }
                st.isHandled = SWEEP_OPEN_MENU;
                WindowManager.LayoutParams lp3 = new WindowManager.LayoutParams(width, -2, st.x, st.y, 1003, 8519680, st.decorView.mDefaultOpacity);
                if (st.isCompact) {
                    lp3.gravity = getOptionsPanelGravity();
                    sRotationWatcher.addWindow(this);
                } else {
                    lp3.gravity = st.gravity;
                }
                lp3.windowAnimations = st.windowAnimations;
                wm.addView(st.decorView, lp3);
                st.isOpen = true;
            }
        }
    }

    @Override
    public final void closePanel(int featureId) {
        if (featureId == 0 && this.mDecorContentParent != null && this.mDecorContentParent.canShowOverflowMenu() && !ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
            this.mDecorContentParent.hideOverflowMenu();
        } else if (featureId == 6) {
            closeContextMenu();
        } else {
            closePanel(getPanelState(featureId, true), true);
        }
    }

    public final void closePanel(PanelFeatureState st, boolean doCallback) {
        if (doCallback && st.featureId == 0 && this.mDecorContentParent != null && this.mDecorContentParent.isOverflowMenuShowing()) {
            checkCloseActionMenu(st.menu);
            return;
        }
        ViewManager wm = getWindowManager();
        if (wm != null && st.isOpen) {
            if (st.decorView != null) {
                wm.removeView(st.decorView);
                if (st.isCompact) {
                    sRotationWatcher.removeWindow(this);
                }
            }
            if (doCallback) {
                callOnPanelClosed(st.featureId, st, null);
            }
        }
        st.isPrepared = SWEEP_OPEN_MENU;
        st.isHandled = SWEEP_OPEN_MENU;
        st.isOpen = SWEEP_OPEN_MENU;
        st.shownPanelView = null;
        if (st.isInExpandedMode) {
            st.refreshDecorView = true;
            st.isInExpandedMode = SWEEP_OPEN_MENU;
        }
        if (this.mPreparedPanel == st) {
            this.mPreparedPanel = null;
            this.mPanelChordingKey = 0;
        }
    }

    void checkCloseActionMenu(Menu menu) {
        if (!this.mClosingActionMenu) {
            this.mClosingActionMenu = true;
            this.mDecorContentParent.dismissPopups();
            Window.Callback cb = getCallback();
            if (cb != null && !isDestroyed()) {
                cb.onPanelClosed(8, menu);
            }
            this.mClosingActionMenu = SWEEP_OPEN_MENU;
        }
    }

    @Override
    public final void togglePanel(int featureId, KeyEvent event) {
        PanelFeatureState st = getPanelState(featureId, true);
        if (st.isOpen) {
            closePanel(st, true);
        } else {
            openPanel(st, event);
        }
    }

    @Override
    public void invalidatePanelMenu(int featureId) {
        this.mInvalidatePanelMenuFeatures |= 1 << featureId;
        if (!this.mInvalidatePanelMenuPosted && this.mDecor != null) {
            this.mDecor.postOnAnimation(this.mInvalidatePanelMenuRunnable);
            this.mInvalidatePanelMenuPosted = true;
        }
    }

    void doPendingInvalidatePanelMenu() {
        if (this.mInvalidatePanelMenuPosted) {
            this.mDecor.removeCallbacks(this.mInvalidatePanelMenuRunnable);
            this.mInvalidatePanelMenuRunnable.run();
        }
    }

    void doInvalidatePanelMenu(int featureId) {
        PanelFeatureState st;
        PanelFeatureState st2 = getPanelState(featureId, SWEEP_OPEN_MENU);
        if (st2 != null) {
            if (st2.menu != null) {
                Bundle savedActionViewStates = new Bundle();
                st2.menu.saveActionViewStates(savedActionViewStates);
                if (savedActionViewStates.size() > 0) {
                    st2.frozenActionViewState = savedActionViewStates;
                }
                st2.menu.stopDispatchingItemsChanged();
                st2.menu.clear();
            }
            st2.refreshMenuContent = true;
            st2.refreshDecorView = true;
            if ((featureId == 8 || featureId == 0) && this.mDecorContentParent != null && (st = getPanelState(0, SWEEP_OPEN_MENU)) != null) {
                st.isPrepared = SWEEP_OPEN_MENU;
                preparePanel(st, null);
            }
        }
    }

    public final boolean onKeyDownPanel(int featureId, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() != 0) {
            return SWEEP_OPEN_MENU;
        }
        this.mPanelChordingKey = keyCode;
        PanelFeatureState st = getPanelState(featureId, SWEEP_OPEN_MENU);
        return (st == null || st.isOpen) ? SWEEP_OPEN_MENU : preparePanel(st, event);
    }

    public final void onKeyUpPanel(int featureId, KeyEvent event) {
        if (this.mPanelChordingKey != 0) {
            this.mPanelChordingKey = 0;
            PanelFeatureState st = getPanelState(featureId, SWEEP_OPEN_MENU);
            if (event.isCanceled()) {
                return;
            }
            if ((this.mDecor == null || this.mDecor.mActionMode == null) && st != null) {
                boolean playSoundEffect = SWEEP_OPEN_MENU;
                if (featureId == 0 && this.mDecorContentParent != null && this.mDecorContentParent.canShowOverflowMenu() && !ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
                    if (!this.mDecorContentParent.isOverflowMenuShowing()) {
                        if (!isDestroyed() && preparePanel(st, event)) {
                            playSoundEffect = this.mDecorContentParent.showOverflowMenu();
                        }
                    } else {
                        playSoundEffect = this.mDecorContentParent.hideOverflowMenu();
                    }
                } else if (st.isOpen || st.isHandled) {
                    playSoundEffect = st.isOpen;
                    closePanel(st, true);
                } else if (st.isPrepared) {
                    boolean show = true;
                    if (st.refreshMenuContent) {
                        st.isPrepared = SWEEP_OPEN_MENU;
                        show = preparePanel(st, event);
                    }
                    if (show) {
                        EventLog.writeEvent(50001, 0);
                        openPanel(st, event);
                        playSoundEffect = true;
                    }
                }
                if (playSoundEffect) {
                    AudioManager audioManager = (AudioManager) getContext().getSystemService("audio");
                    if (audioManager != null) {
                        audioManager.playSoundEffect(0);
                    } else {
                        Log.w(TAG, "Couldn't get audio manager");
                    }
                }
            }
        }
    }

    @Override
    public final void closeAllPanels() {
        ViewManager wm = getWindowManager();
        if (wm != null) {
            PanelFeatureState[] panels = this.mPanels;
            int N = panels != null ? panels.length : 0;
            for (int i = 0; i < N; i++) {
                PanelFeatureState panel = panels[i];
                if (panel != null) {
                    closePanel(panel, true);
                }
            }
            closeContextMenu();
        }
    }

    private synchronized void closeContextMenu() {
        if (this.mContextMenu != null) {
            this.mContextMenu.close();
            dismissContextMenu();
        }
    }

    private synchronized void dismissContextMenu() {
        this.mContextMenu = null;
        if (this.mContextMenuHelper != null) {
            this.mContextMenuHelper.dismiss();
            this.mContextMenuHelper = null;
        }
    }

    @Override
    public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
        return performPanelShortcut(getPanelState(featureId, SWEEP_OPEN_MENU), keyCode, event, flags);
    }

    private boolean performPanelShortcut(PanelFeatureState st, int keyCode, KeyEvent event, int flags) {
        if (event.isSystem() || st == null) {
            return SWEEP_OPEN_MENU;
        }
        boolean handled = SWEEP_OPEN_MENU;
        if ((st.isPrepared || preparePanel(st, event)) && st.menu != null) {
            handled = st.menu.performShortcut(keyCode, event, flags);
        }
        if (handled) {
            st.isHandled = true;
            if ((flags & 1) == 0 && this.mDecorContentParent == null) {
                closePanel(st, true);
                return handled;
            }
            return handled;
        }
        return handled;
    }

    @Override
    public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
        boolean zPerformIdentifierAction = SWEEP_OPEN_MENU;
        PanelFeatureState st = getPanelState(featureId, true);
        if (preparePanel(st, new KeyEvent(0, 82)) && st.menu != null) {
            zPerformIdentifierAction = st.menu.performIdentifierAction(id, flags);
            if (this.mDecorContentParent == null) {
                closePanel(st, true);
            }
        }
        return zPerformIdentifierAction;
    }

    public PanelFeatureState findMenuPanel(Menu menu) {
        PanelFeatureState[] panels = this.mPanels;
        int N = panels != null ? panels.length : 0;
        for (int i = 0; i < N; i++) {
            PanelFeatureState panel = panels[i];
            if (panel != null && panel.menu == menu) {
                return panel;
            }
        }
        return null;
    }

    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        PanelFeatureState panel;
        Window.Callback cb = getCallback();
        return (cb == null || isDestroyed() || (panel = findMenuPanel(menu.getRootMenu())) == null) ? SWEEP_OPEN_MENU : cb.onMenuItemSelected(panel.featureId, item);
    }

    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(true);
    }

    private void reopenMenu(boolean toggleMenuMode) {
        if (this.mDecorContentParent != null && this.mDecorContentParent.canShowOverflowMenu() && (!ViewConfiguration.get(getContext()).hasPermanentMenuKey() || this.mDecorContentParent.isOverflowMenuShowPending())) {
            Window.Callback cb = getCallback();
            if (!this.mDecorContentParent.isOverflowMenuShowing() || !toggleMenuMode) {
                if (cb != null && !isDestroyed()) {
                    if (this.mInvalidatePanelMenuPosted && (this.mInvalidatePanelMenuFeatures & 1) != 0) {
                        this.mDecor.removeCallbacks(this.mInvalidatePanelMenuRunnable);
                        this.mInvalidatePanelMenuRunnable.run();
                    }
                    PanelFeatureState st = getPanelState(0, SWEEP_OPEN_MENU);
                    if (st != null && st.menu != null && !st.refreshMenuContent && cb.onPreparePanel(0, st.createdPanelView, st.menu)) {
                        cb.onMenuOpened(8, st.menu);
                        this.mDecorContentParent.showOverflowMenu();
                        return;
                    }
                    return;
                }
                return;
            }
            this.mDecorContentParent.hideOverflowMenu();
            PanelFeatureState st2 = getPanelState(0, SWEEP_OPEN_MENU);
            if (st2 != null && cb != null && !isDestroyed()) {
                cb.onPanelClosed(8, st2.menu);
                return;
            }
            return;
        }
        PanelFeatureState st3 = getPanelState(0, SWEEP_OPEN_MENU);
        if (st3 != null) {
            boolean newExpandedMode = toggleMenuMode ? !st3.isInExpandedMode : st3.isInExpandedMode;
            st3.refreshDecorView = true;
            closePanel(st3, SWEEP_OPEN_MENU);
            st3.isInExpandedMode = newExpandedMode;
            openPanel(st3, (KeyEvent) null);
        }
    }

    protected boolean initializePanelMenu(PanelFeatureState st) {
        Context context = getContext();
        if ((st.featureId == 0 || st.featureId == 8) && this.mDecorContentParent != null) {
            TypedValue outValue = new TypedValue();
            Resources.Theme baseTheme = context.getTheme();
            baseTheme.resolveAttribute(R.attr.actionBarTheme, outValue, true);
            Resources.Theme widgetTheme = null;
            if (outValue.resourceId != 0) {
                widgetTheme = context.getResources().newTheme();
                widgetTheme.setTo(baseTheme);
                widgetTheme.applyStyle(outValue.resourceId, true);
                widgetTheme.resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
            } else {
                baseTheme.resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
            }
            if (outValue.resourceId != 0) {
                if (widgetTheme == null) {
                    widgetTheme = context.getResources().newTheme();
                    widgetTheme.setTo(baseTheme);
                }
                widgetTheme.applyStyle(outValue.resourceId, true);
            }
            if (widgetTheme != null) {
                Context context2 = new ContextThemeWrapper(context, 0);
                context2.getTheme().setTo(widgetTheme);
                context = context2;
            }
        }
        MenuBuilder menu = new MenuBuilder(context);
        menu.setCallback(this);
        st.setMenu(menu);
        return true;
    }

    protected boolean initializePanelDecor(PanelFeatureState st) {
        st.decorView = new DecorView(getContext(), st.featureId);
        st.gravity = 81;
        st.setStyle(getContext());
        TypedArray a = getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Window, 0, st.listPresenterTheme);
        float elevation = a.getDimension(38, 0.0f);
        if (elevation != 0.0f) {
            st.decorView.setElevation(elevation);
        }
        a.recycle();
        return true;
    }

    private int getOptionsPanelGravity() {
        try {
            return WindowManagerHolder.sWindowManager.getPreferredOptionsPanelGravity();
        } catch (RemoteException ex) {
            Log.e(TAG, "Couldn't getOptionsPanelGravity; using default", ex);
            return 81;
        }
    }

    void onOptionsPanelRotationChanged() {
        PanelFeatureState st = getPanelState(0, SWEEP_OPEN_MENU);
        if (st != null) {
            WindowManager.LayoutParams lp = st.decorView != null ? (WindowManager.LayoutParams) st.decorView.getLayoutParams() : null;
            if (lp != null) {
                lp.gravity = getOptionsPanelGravity();
                ViewManager wm = getWindowManager();
                if (wm != null) {
                    wm.updateViewLayout(st.decorView, lp);
                }
            }
        }
    }

    protected boolean initializePanelContent(PanelFeatureState st) {
        if (st.createdPanelView != null) {
            st.shownPanelView = st.createdPanelView;
            return true;
        }
        if (st.menu == null) {
            return SWEEP_OPEN_MENU;
        }
        if (this.mPanelMenuPresenterCallback == null) {
            this.mPanelMenuPresenterCallback = new PanelMenuPresenterCallback();
        }
        MenuView menuView = st.isInListMode() ? st.getListMenuView(getContext(), this.mPanelMenuPresenterCallback) : st.getIconMenuView(getContext(), this.mPanelMenuPresenterCallback);
        st.shownPanelView = (View) menuView;
        if (st.shownPanelView == null) {
            return SWEEP_OPEN_MENU;
        }
        int defaultAnimations = menuView.getWindowAnimations();
        if (defaultAnimations != 0) {
            st.windowAnimations = defaultAnimations;
        }
        return true;
    }

    @Override
    public boolean performContextMenuIdentifierAction(int id, int flags) {
        return this.mContextMenu != null ? this.mContextMenu.performIdentifierAction(id, flags) : SWEEP_OPEN_MENU;
    }

    @Override
    public final void setElevation(float elevation) {
        this.mElevation = elevation;
        if (this.mDecor != null) {
            this.mDecor.setElevation(elevation);
        }
        dispatchWindowAttributesChanged(getAttributes());
    }

    @Override
    public final void setClipToOutline(boolean clipToOutline) {
        this.mClipToOutline = clipToOutline;
        if (this.mDecor != null) {
            this.mDecor.setClipToOutline(clipToOutline);
        }
    }

    @Override
    public final void setBackgroundDrawable(Drawable drawable) {
        if (drawable != this.mBackgroundDrawable || this.mBackgroundResource != 0) {
            this.mBackgroundResource = 0;
            this.mBackgroundDrawable = drawable;
            if (this.mDecor != null) {
                this.mDecor.setWindowBackground(drawable);
            }
            if (this.mBackgroundFallbackResource != 0) {
                this.mDecor.setBackgroundFallback(drawable == null ? this.mBackgroundFallbackResource : 0);
            }
        }
    }

    @Override
    public final void setFeatureDrawableResource(int featureId, int resId) {
        if (resId != 0) {
            DrawableFeatureState st = getDrawableState(featureId, true);
            if (st.resid != resId) {
                st.resid = resId;
                st.uri = null;
                st.local = getContext().getDrawable(resId);
                updateDrawable(featureId, st, SWEEP_OPEN_MENU);
                return;
            }
            return;
        }
        setFeatureDrawable(featureId, null);
    }

    @Override
    public final void setFeatureDrawableUri(int featureId, Uri uri) {
        if (uri != null) {
            DrawableFeatureState st = getDrawableState(featureId, true);
            if (st.uri == null || !st.uri.equals(uri)) {
                st.resid = 0;
                st.uri = uri;
                st.local = loadImageURI(uri);
                updateDrawable(featureId, st, SWEEP_OPEN_MENU);
                return;
            }
            return;
        }
        setFeatureDrawable(featureId, null);
    }

    @Override
    public final void setFeatureDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        st.resid = 0;
        st.uri = null;
        if (st.local != drawable) {
            st.local = drawable;
            updateDrawable(featureId, st, SWEEP_OPEN_MENU);
        }
    }

    @Override
    public void setFeatureDrawableAlpha(int featureId, int alpha) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        if (st.alpha != alpha) {
            st.alpha = alpha;
            updateDrawable(featureId, st, SWEEP_OPEN_MENU);
        }
    }

    protected final void setFeatureDefaultDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        if (st.def != drawable) {
            st.def = drawable;
            updateDrawable(featureId, st, SWEEP_OPEN_MENU);
        }
    }

    @Override
    public final void setFeatureInt(int featureId, int value) {
        updateInt(featureId, value, SWEEP_OPEN_MENU);
    }

    protected final void updateDrawable(int featureId, boolean fromActive) {
        DrawableFeatureState st = getDrawableState(featureId, SWEEP_OPEN_MENU);
        if (st != null) {
            updateDrawable(featureId, st, fromActive);
        }
    }

    protected void onDrawableChanged(int featureId, Drawable drawable, int alpha) {
        ImageView view;
        if (featureId == 3) {
            view = getLeftIconView();
        } else if (featureId == FLAG_RESOURCE_SET_ICON_FALLBACK) {
            view = getRightIconView();
        } else {
            return;
        }
        if (drawable != null) {
            drawable.setAlpha(alpha);
            view.setImageDrawable(drawable);
            view.setVisibility(0);
            return;
        }
        view.setVisibility(8);
    }

    protected void onIntChanged(int featureId, int value) {
        FrameLayout titleContainer;
        if (featureId == 2 || featureId == 5) {
            updateProgressBars(value);
        } else if (featureId == 7 && (titleContainer = (FrameLayout) findViewById(R.id.shareText)) != null) {
            this.mLayoutInflater.inflate(value, titleContainer);
        }
    }

    private void updateProgressBars(int value) {
        ProgressBar circularProgressBar = getCircularProgressBar(true);
        ProgressBar horizontalProgressBar = getHorizontalProgressBar(true);
        int features = getLocalFeatures();
        if (value == -1) {
            if ((features & FLAG_RESOURCE_SET_ICON_FALLBACK) != 0) {
                if (horizontalProgressBar != null) {
                    int level = horizontalProgressBar.getProgress();
                    int visibility = (horizontalProgressBar.isIndeterminate() || level < 10000) ? 0 : FLAG_RESOURCE_SET_ICON_FALLBACK;
                    horizontalProgressBar.setVisibility(visibility);
                } else {
                    Log.e(TAG, "Horizontal progress bar not located in current window decor");
                }
            }
            if ((features & 32) != 0) {
                if (circularProgressBar != null) {
                    circularProgressBar.setVisibility(0);
                    return;
                } else {
                    Log.e(TAG, "Circular progress bar not located in current window decor");
                    return;
                }
            }
            return;
        }
        if (value == -2) {
            if ((features & FLAG_RESOURCE_SET_ICON_FALLBACK) != 0) {
                if (horizontalProgressBar != null) {
                    horizontalProgressBar.setVisibility(8);
                } else {
                    Log.e(TAG, "Horizontal progress bar not located in current window decor");
                }
            }
            if ((features & 32) != 0) {
                if (circularProgressBar != null) {
                    circularProgressBar.setVisibility(8);
                    return;
                } else {
                    Log.e(TAG, "Circular progress bar not located in current window decor");
                    return;
                }
            }
            return;
        }
        if (value == -3) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setIndeterminate(true);
                return;
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
                return;
            }
        }
        if (value == -4) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setIndeterminate(SWEEP_OPEN_MENU);
                return;
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
                return;
            }
        }
        if (value >= 0 && value <= 10000) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setProgress(value + 0);
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
            }
            if (value < 10000) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
                return;
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
                return;
            }
        }
        if (20000 <= value && value <= 30000) {
            if (horizontalProgressBar != null) {
                horizontalProgressBar.setSecondaryProgress(value - 20000);
            } else {
                Log.e(TAG, "Horizontal progress bar not located in current window decor");
            }
            showProgressBars(horizontalProgressBar, circularProgressBar);
        }
    }

    private void showProgressBars(ProgressBar horizontalProgressBar, ProgressBar spinnyProgressBar) {
        int features = getLocalFeatures();
        if ((features & 32) != 0 && spinnyProgressBar != null && spinnyProgressBar.getVisibility() == FLAG_RESOURCE_SET_ICON_FALLBACK) {
            spinnyProgressBar.setVisibility(0);
        }
        if ((features & FLAG_RESOURCE_SET_ICON_FALLBACK) != 0 && horizontalProgressBar != null && horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(0);
        }
    }

    private void hideProgressBars(ProgressBar horizontalProgressBar, ProgressBar spinnyProgressBar) {
        int features = getLocalFeatures();
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        anim.setDuration(1000L);
        if ((features & 32) != 0 && spinnyProgressBar != null && spinnyProgressBar.getVisibility() == 0) {
            spinnyProgressBar.startAnimation(anim);
            spinnyProgressBar.setVisibility(FLAG_RESOURCE_SET_ICON_FALLBACK);
        }
        if ((features & FLAG_RESOURCE_SET_ICON_FALLBACK) != 0 && horizontalProgressBar != null && horizontalProgressBar.getVisibility() == 0) {
            horizontalProgressBar.startAnimation(anim);
            horizontalProgressBar.setVisibility(FLAG_RESOURCE_SET_ICON_FALLBACK);
        }
    }

    @Override
    public void setIcon(int resId) {
        this.mIconRes = resId;
        this.mResourcesSetFlags |= 1;
        this.mResourcesSetFlags &= -5;
        if (this.mDecorContentParent != null) {
            this.mDecorContentParent.setIcon(resId);
        }
    }

    public void setDefaultIcon(int resId) {
        if ((this.mResourcesSetFlags & 1) == 0) {
            this.mIconRes = resId;
            if (this.mDecorContentParent != null) {
                if (!this.mDecorContentParent.hasIcon() || (this.mResourcesSetFlags & FLAG_RESOURCE_SET_ICON_FALLBACK) != 0) {
                    if (resId != 0) {
                        this.mDecorContentParent.setIcon(resId);
                        this.mResourcesSetFlags &= -5;
                    } else {
                        this.mDecorContentParent.setIcon(getContext().getPackageManager().getDefaultActivityIcon());
                        this.mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON_FALLBACK;
                    }
                }
            }
        }
    }

    @Override
    public void setLogo(int resId) {
        this.mLogoRes = resId;
        this.mResourcesSetFlags |= 2;
        if (this.mDecorContentParent != null) {
            this.mDecorContentParent.setLogo(resId);
        }
    }

    public void setDefaultLogo(int resId) {
        if ((this.mResourcesSetFlags & 2) == 0) {
            this.mLogoRes = resId;
            if (this.mDecorContentParent != null && !this.mDecorContentParent.hasLogo()) {
                this.mDecorContentParent.setLogo(resId);
            }
        }
    }

    @Override
    public void setLocalFocus(boolean hasFocus, boolean inTouchMode) {
        getViewRootImpl().windowFocusChanged(hasFocus, inTouchMode);
    }

    @Override
    public void injectInputEvent(InputEvent event) {
        getViewRootImpl().dispatchInputEvent(event);
    }

    private ViewRootImpl getViewRootImpl() {
        ViewRootImpl viewRootImpl;
        if (this.mDecor != null && (viewRootImpl = this.mDecor.getViewRootImpl()) != null) {
            return viewRootImpl;
        }
        throw new IllegalStateException("view not added");
    }

    @Override
    public void takeKeyEvents(boolean get) {
        this.mDecor.setFocusable(get);
    }

    @Override
    public boolean superDispatchKeyEvent(KeyEvent event) {
        return this.mDecor.superDispatchKeyEvent(event);
    }

    @Override
    public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
        return this.mDecor.superDispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean superDispatchTouchEvent(MotionEvent event) {
        return this.mDecor.superDispatchTouchEvent(event);
    }

    @Override
    public boolean superDispatchTrackballEvent(MotionEvent event) {
        return this.mDecor.superDispatchTrackballEvent(event);
    }

    @Override
    public boolean superDispatchGenericMotionEvent(MotionEvent event) {
        return this.mDecor.superDispatchGenericMotionEvent(event);
    }

    protected boolean onKeyDown(int featureId, int keyCode, KeyEvent event) {
        KeyEvent.DispatcherState dispatcher = this.mDecor != null ? this.mDecor.getKeyDispatcherState() : null;
        switch (keyCode) {
            case FLAG_RESOURCE_SET_ICON_FALLBACK:
                if (event.getRepeatCount() > 0 || featureId < 0) {
                    return SWEEP_OPEN_MENU;
                }
                if (dispatcher == null) {
                    return true;
                }
                dispatcher.startTracking(event, this);
                return true;
            case 24:
            case 25:
                int direction = keyCode == 24 ? 1 : -1;
                if (this.mMediaController != null) {
                    this.mMediaController.adjustVolume(direction, 1);
                    return true;
                }
                MediaSessionLegacyHelper.getHelper(getContext()).sendAdjustVolumeBy(this.mVolumeControlStreamType, direction, 17);
                return true;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
                if (this.mMediaController == null || !this.mMediaController.dispatchMediaButtonEvent(event)) {
                    return SWEEP_OPEN_MENU;
                }
                return true;
            case 82:
                if (featureId < 0) {
                    featureId = 0;
                }
                onKeyDownPanel(featureId, event);
                return true;
            case 164:
                getAudioManager().handleKeyDown(event, this.mVolumeControlStreamType);
                return true;
            default:
                return SWEEP_OPEN_MENU;
        }
    }

    private KeyguardManager getKeyguardManager() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) getContext().getSystemService("keyguard");
        }
        return this.mKeyguardManager;
    }

    AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
        }
        return this.mAudioManager;
    }

    protected boolean onKeyUp(int featureId, int keyCode, KeyEvent event) {
        PanelFeatureState st;
        KeyEvent.DispatcherState dispatcher = this.mDecor != null ? this.mDecor.getKeyDispatcherState() : null;
        if (dispatcher != null) {
            dispatcher.handleUpEvent(event);
        }
        switch (keyCode) {
            case FLAG_RESOURCE_SET_ICON_FALLBACK:
                if (featureId < 0 || !event.isTracking() || event.isCanceled()) {
                    return SWEEP_OPEN_MENU;
                }
                if (featureId == 0 && (st = getPanelState(featureId, SWEEP_OPEN_MENU)) != null && st.isInExpandedMode) {
                    reopenMenu(true);
                    return true;
                }
                closePanel(featureId);
                return true;
            case 24:
            case 25:
                if (this.mMediaController != null) {
                    this.mMediaController.adjustVolume(0, 20);
                } else {
                    MediaSessionLegacyHelper.getHelper(getContext()).sendAdjustVolumeBy(this.mVolumeControlStreamType, 0, 20);
                }
                return true;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
                if (this.mMediaController == null || !this.mMediaController.dispatchMediaButtonEvent(event)) {
                    return SWEEP_OPEN_MENU;
                }
                return true;
            case 82:
                if (featureId < 0) {
                    featureId = 0;
                }
                onKeyUpPanel(featureId, event);
                return true;
            case 84:
                if (getKeyguardManager().inKeyguardRestrictedInputMode()) {
                    return SWEEP_OPEN_MENU;
                }
                if (event.isTracking() && !event.isCanceled()) {
                    launchDefaultSearch();
                }
                return true;
            case 164:
                getAudioManager().handleKeyUp(event, this.mVolumeControlStreamType);
                return true;
            default:
                return SWEEP_OPEN_MENU;
        }
    }

    @Override
    protected void onActive() {
    }

    @Override
    public final View getDecorView() {
        if (this.mDecor == null) {
            installDecor();
        }
        return this.mDecor;
    }

    @Override
    public final View peekDecorView() {
        return this.mDecor;
    }

    @Override
    public Bundle saveHierarchyState() {
        Bundle outState = new Bundle();
        if (this.mContentParent != null) {
            SparseArray<Parcelable> states = new SparseArray<>();
            this.mContentParent.saveHierarchyState(states);
            outState.putSparseParcelableArray(VIEWS_TAG, states);
            View focusedView = this.mContentParent.findFocus();
            if (focusedView != null && focusedView.getId() != -1) {
                outState.putInt(FOCUSED_ID_TAG, focusedView.getId());
            }
            SparseArray<Parcelable> panelStates = new SparseArray<>();
            savePanelState(panelStates);
            if (panelStates.size() > 0) {
                outState.putSparseParcelableArray(PANELS_TAG, panelStates);
            }
            if (this.mDecorContentParent != null) {
                SparseArray<Parcelable> actionBarStates = new SparseArray<>();
                this.mDecorContentParent.saveToolbarHierarchyState(actionBarStates);
                outState.putSparseParcelableArray(ACTION_BAR_TAG, actionBarStates);
            }
        }
        return outState;
    }

    @Override
    public void restoreHierarchyState(Bundle savedInstanceState) {
        if (this.mContentParent != null) {
            SparseArray<Parcelable> savedStates = savedInstanceState.getSparseParcelableArray(VIEWS_TAG);
            if (savedStates != null) {
                this.mContentParent.restoreHierarchyState(savedStates);
            }
            int focusedViewId = savedInstanceState.getInt(FOCUSED_ID_TAG, -1);
            if (focusedViewId != -1) {
                View needsFocus = this.mContentParent.findViewById(focusedViewId);
                if (needsFocus != null) {
                    needsFocus.requestFocus();
                } else {
                    Log.w(TAG, "Previously focused view reported id " + focusedViewId + " during save, but can't be found during restore.");
                }
            }
            SparseArray<Parcelable> panelStates = savedInstanceState.getSparseParcelableArray(PANELS_TAG);
            if (panelStates != null) {
                restorePanelState(panelStates);
            }
            if (this.mDecorContentParent != null) {
                SparseArray<Parcelable> actionBarStates = savedInstanceState.getSparseParcelableArray(ACTION_BAR_TAG);
                if (actionBarStates != null) {
                    doPendingInvalidatePanelMenu();
                    this.mDecorContentParent.restoreToolbarHierarchyState(actionBarStates);
                } else {
                    Log.w(TAG, "Missing saved instance states for action bar views! State will not be restored.");
                }
            }
        }
    }

    private void savePanelState(SparseArray<Parcelable> icicles) {
        PanelFeatureState[] panels = this.mPanels;
        if (panels != null) {
            for (int curFeatureId = panels.length - 1; curFeatureId >= 0; curFeatureId--) {
                if (panels[curFeatureId] != null) {
                    icicles.put(curFeatureId, panels[curFeatureId].onSaveInstanceState());
                }
            }
        }
    }

    private void restorePanelState(SparseArray<Parcelable> icicles) {
        for (int i = icicles.size() - 1; i >= 0; i--) {
            int curFeatureId = icicles.keyAt(i);
            PanelFeatureState st = getPanelState(curFeatureId, SWEEP_OPEN_MENU);
            if (st != null) {
                st.onRestoreInstanceState(icicles.get(curFeatureId));
                invalidatePanelMenu(curFeatureId);
            }
        }
    }

    private void openPanelsAfterRestore() {
        PanelFeatureState[] panels = this.mPanels;
        if (panels != null) {
            for (int i = panels.length - 1; i >= 0; i--) {
                PanelFeatureState st = panels[i];
                if (st != null) {
                    st.applyFrozenState();
                    if (!st.isOpen && st.wasLastOpen) {
                        st.isInExpandedMode = st.wasLastExpanded;
                        openPanel(st, (KeyEvent) null);
                    }
                }
            }
        }
    }

    private class PanelMenuPresenterCallback implements MenuPresenter.Callback {
        private PanelMenuPresenterCallback() {
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            MenuBuilder rootMenu = menu.getRootMenu();
            boolean isSubMenu = rootMenu != menu ? true : PhoneWindow.SWEEP_OPEN_MENU;
            PhoneWindow phoneWindow = PhoneWindow.this;
            if (isSubMenu) {
                menu = rootMenu;
            }
            PanelFeatureState panel = phoneWindow.findMenuPanel(menu);
            if (panel != null) {
                if (isSubMenu) {
                    PhoneWindow.this.callOnPanelClosed(panel.featureId, panel, rootMenu);
                    PhoneWindow.this.closePanel(panel, true);
                } else {
                    PhoneWindow.this.closePanel(panel, allMenusAreClosing);
                }
            }
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            Window.Callback cb;
            if (subMenu == null && PhoneWindow.this.hasFeature(8) && (cb = PhoneWindow.this.getCallback()) != null && !PhoneWindow.this.isDestroyed()) {
                cb.onMenuOpened(8, subMenu);
                return true;
            }
            return true;
        }
    }

    private final class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        private ActionMenuPresenterCallback() {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            Window.Callback cb = PhoneWindow.this.getCallback();
            if (cb == null) {
                return PhoneWindow.SWEEP_OPEN_MENU;
            }
            cb.onMenuOpened(8, subMenu);
            return true;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            PhoneWindow.this.checkCloseActionMenu(menu);
        }
    }

    private final class DecorView extends FrameLayout implements RootViewSurfaceTaker {
        private ActionMode mActionMode;
        private PopupWindow mActionModePopup;
        private ActionBarContextView mActionModeView;
        private final BackgroundFallback mBackgroundFallback;
        private final Rect mBackgroundPadding;
        private final int mBarEnterExitDuration;
        private boolean mChanging;
        int mDefaultOpacity;
        private int mDownY;
        private final Rect mDrawingBounds;
        private final int mFeatureId;
        private final Rect mFrameOffsets;
        private final Rect mFramePadding;
        private final Interpolator mHideInterpolator;
        private int mLastBottomInset;
        private boolean mLastHasBottomStableInset;
        private boolean mLastHasTopStableInset;
        private int mLastRightInset;
        private int mLastTopInset;
        private int mLastWindowFlags;
        private Drawable mMenuBackground;
        private final ColorViewState mNavigationColorViewState;
        private View mNavigationGuard;
        private int mRootScrollY;
        private Runnable mShowActionModePopup;
        private final Interpolator mShowInterpolator;
        private final ColorViewState mStatusColorViewState;
        private View mStatusGuard;
        private boolean mWatchingForMenu;

        public DecorView(Context context, int featureId) {
            super(context);
            this.mDefaultOpacity = -1;
            this.mDrawingBounds = new Rect();
            this.mBackgroundPadding = new Rect();
            this.mFramePadding = new Rect();
            this.mFrameOffsets = new Rect();
            this.mStatusColorViewState = new ColorViewState(PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK, 67108864, 48, "android:status:background", R.id.statusBarBackground, 1024);
            this.mNavigationColorViewState = new ColorViewState(2, 134217728, 80, "android:navigation:background", R.id.navigationBarBackground, 0);
            this.mBackgroundFallback = new BackgroundFallback();
            this.mLastTopInset = 0;
            this.mLastBottomInset = 0;
            this.mLastRightInset = 0;
            this.mLastHasTopStableInset = PhoneWindow.SWEEP_OPEN_MENU;
            this.mLastHasBottomStableInset = PhoneWindow.SWEEP_OPEN_MENU;
            this.mLastWindowFlags = 0;
            this.mRootScrollY = 0;
            this.mFeatureId = featureId;
            this.mShowInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
            this.mHideInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_linear_in);
            this.mBarEnterExitDuration = context.getResources().getInteger(R.integer.config_extraFreeKbytesAbsolute);
        }

        public void setBackgroundFallback(int resId) {
            this.mBackgroundFallback.setDrawable(resId != 0 ? getContext().getDrawable(resId) : null);
            setWillNotDraw((getBackground() != null || this.mBackgroundFallback.hasFallback()) ? PhoneWindow.SWEEP_OPEN_MENU : true);
        }

        @Override
        public void onDraw(Canvas c) {
            super.onDraw(c);
            this.mBackgroundFallback.draw(PhoneWindow.this.mContentRoot, c, PhoneWindow.this.mContentParent);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keyCode = event.getKeyCode();
            int action = event.getAction();
            boolean isDown = action == 0;
            if (isDown && event.getRepeatCount() == 0) {
                if (PhoneWindow.this.mPanelChordingKey > 0 && PhoneWindow.this.mPanelChordingKey != keyCode) {
                    boolean handled = dispatchKeyShortcutEvent(event);
                    if (handled) {
                        return true;
                    }
                }
                if (PhoneWindow.this.mPreparedPanel != null && PhoneWindow.this.mPreparedPanel.isOpen && PhoneWindow.this.performPanelShortcut(PhoneWindow.this.mPreparedPanel, keyCode, event, 0)) {
                    return true;
                }
            }
            if (!PhoneWindow.this.isDestroyed()) {
                Window.Callback cb = PhoneWindow.this.getCallback();
                boolean handled2 = (cb == null || this.mFeatureId >= 0) ? super.dispatchKeyEvent(event) : cb.dispatchKeyEvent(event);
                if (handled2) {
                    return true;
                }
            }
            return isDown ? PhoneWindow.this.onKeyDown(this.mFeatureId, event.getKeyCode(), event) : PhoneWindow.this.onKeyUp(this.mFeatureId, event.getKeyCode(), event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent ev) {
            if (PhoneWindow.this.mPreparedPanel != null) {
                boolean handled = PhoneWindow.this.performPanelShortcut(PhoneWindow.this.mPreparedPanel, ev.getKeyCode(), ev, 1);
                if (handled) {
                    if (PhoneWindow.this.mPreparedPanel == null) {
                        return true;
                    }
                    PhoneWindow.this.mPreparedPanel.isHandled = true;
                    return true;
                }
            }
            Window.Callback cb = PhoneWindow.this.getCallback();
            boolean handled2 = (cb == null || PhoneWindow.this.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchKeyShortcutEvent(ev) : cb.dispatchKeyShortcutEvent(ev);
            if (handled2) {
                return true;
            }
            PanelFeatureState st = PhoneWindow.this.getPanelState(0, PhoneWindow.SWEEP_OPEN_MENU);
            if (st != null && PhoneWindow.this.mPreparedPanel == null) {
                PhoneWindow.this.preparePanel(st, ev);
                boolean handled3 = PhoneWindow.this.performPanelShortcut(st, ev.getKeyCode(), ev, 1);
                st.isPrepared = PhoneWindow.SWEEP_OPEN_MENU;
                if (handled3) {
                    return true;
                }
            }
            return PhoneWindow.SWEEP_OPEN_MENU;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            Window.Callback cb = PhoneWindow.this.getCallback();
            return (cb == null || PhoneWindow.this.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchTouchEvent(ev) : cb.dispatchTouchEvent(ev);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent ev) {
            Window.Callback cb = PhoneWindow.this.getCallback();
            return (cb == null || PhoneWindow.this.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchTrackballEvent(ev) : cb.dispatchTrackballEvent(ev);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent ev) {
            Window.Callback cb = PhoneWindow.this.getCallback();
            return (cb == null || PhoneWindow.this.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchGenericMotionEvent(ev) : cb.dispatchGenericMotionEvent(ev);
        }

        public boolean superDispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK) {
                int action = event.getAction();
                if (this.mActionMode != null) {
                    if (action != 1) {
                        return true;
                    }
                    this.mActionMode.finish();
                    return true;
                }
            }
            return super.dispatchKeyEvent(event);
        }

        public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
            return super.dispatchKeyShortcutEvent(event);
        }

        public boolean superDispatchTouchEvent(MotionEvent event) {
            return super.dispatchTouchEvent(event);
        }

        public boolean superDispatchTrackballEvent(MotionEvent event) {
            return super.dispatchTrackballEvent(event);
        }

        public boolean superDispatchGenericMotionEvent(MotionEvent event) {
            return super.dispatchGenericMotionEvent(event);
        }

        @Override
        public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
            if (PhoneWindow.this.mOutsetBottom == null) {
                return super.dispatchApplyWindowInsets(insets);
            }
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            int bottom = (int) PhoneWindow.this.mOutsetBottom.getDimension(metrics);
            WindowInsets newInsets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), bottom);
            return super.dispatchApplyWindowInsets(newInsets);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return onInterceptTouchEvent(event);
        }

        private boolean isOutOfBounds(int x, int y) {
            if (x < -5 || y < -5 || x > getWidth() + 5 || y > getHeight() + 5) {
                return true;
            }
            return PhoneWindow.SWEEP_OPEN_MENU;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            int action = event.getAction();
            if (this.mFeatureId >= 0 && action == 0) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (isOutOfBounds(x, y)) {
                    PhoneWindow.this.closePanel(this.mFeatureId);
                    return true;
                }
            }
            return PhoneWindow.SWEEP_OPEN_MENU;
        }

        @Override
        public void sendAccessibilityEvent(int eventType) {
            if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                if ((this.mFeatureId == 0 || this.mFeatureId == 6 || this.mFeatureId == 2 || this.mFeatureId == 5) && getChildCount() == 1) {
                    getChildAt(0).sendAccessibilityEvent(eventType);
                } else {
                    super.sendAccessibilityEvent(eventType);
                }
            }
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            Window.Callback cb = PhoneWindow.this.getCallback();
            if (cb == null || PhoneWindow.this.isDestroyed() || !cb.dispatchPopulateAccessibilityEvent(event)) {
                return super.dispatchPopulateAccessibilityEvent(event);
            }
            return true;
        }

        protected boolean setFrame(int l, int t, int r, int b) {
            boolean changed = super.setFrame(l, t, r, b);
            if (changed) {
                Rect drawingBounds = this.mDrawingBounds;
                getDrawingRect(drawingBounds);
                Drawable fg = getForeground();
                if (fg != null) {
                    Rect frameOffsets = this.mFrameOffsets;
                    drawingBounds.left += frameOffsets.left;
                    drawingBounds.top += frameOffsets.top;
                    drawingBounds.right -= frameOffsets.right;
                    drawingBounds.bottom -= frameOffsets.bottom;
                    fg.setBounds(drawingBounds);
                    Rect framePadding = this.mFramePadding;
                    drawingBounds.left += framePadding.left - frameOffsets.left;
                    drawingBounds.top += framePadding.top - frameOffsets.top;
                    drawingBounds.right -= framePadding.right - frameOffsets.right;
                    drawingBounds.bottom -= framePadding.bottom - frameOffsets.bottom;
                }
                Drawable bg = getBackground();
                if (bg != null) {
                    bg.setBounds(drawingBounds);
                }
            }
            return changed;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int min;
            int mode;
            int h;
            int w;
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            boolean isPortrait = metrics.widthPixels < metrics.heightPixels ? true : PhoneWindow.SWEEP_OPEN_MENU;
            int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
            boolean fixedWidth = PhoneWindow.SWEEP_OPEN_MENU;
            if (widthMode == Integer.MIN_VALUE) {
                TypedValue tvw = isPortrait ? PhoneWindow.this.mFixedWidthMinor : PhoneWindow.this.mFixedWidthMajor;
                if (tvw != null && tvw.type != 0) {
                    if (tvw.type == 5) {
                        w = (int) tvw.getDimension(metrics);
                    } else if (tvw.type == 6) {
                        w = (int) tvw.getFraction(metrics.widthPixels, metrics.widthPixels);
                    } else {
                        w = 0;
                    }
                    if (w > 0) {
                        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
                        widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.min(w, widthSize), 1073741824);
                        fixedWidth = true;
                    }
                }
            }
            if (heightMode == Integer.MIN_VALUE) {
                TypedValue tvh = isPortrait ? PhoneWindow.this.mFixedHeightMajor : PhoneWindow.this.mFixedHeightMinor;
                if (tvh != null && tvh.type != 0) {
                    if (tvh.type == 5) {
                        h = (int) tvh.getDimension(metrics);
                    } else if (tvh.type == 6) {
                        h = (int) tvh.getFraction(metrics.heightPixels, metrics.heightPixels);
                    } else {
                        h = 0;
                    }
                    if (h > 0) {
                        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
                        heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.min(h, heightSize), 1073741824);
                    }
                }
            }
            if (PhoneWindow.this.mOutsetBottom != null && (mode = View.MeasureSpec.getMode(heightMeasureSpec)) != 0) {
                int outset = (int) PhoneWindow.this.mOutsetBottom.getDimension(metrics);
                int height = View.MeasureSpec.getSize(heightMeasureSpec);
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height + outset, mode);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = getMeasuredWidth();
            boolean measure = PhoneWindow.SWEEP_OPEN_MENU;
            int widthMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(width, 1073741824);
            if (!fixedWidth && widthMode == Integer.MIN_VALUE) {
                TypedValue tv = isPortrait ? PhoneWindow.this.mMinWidthMinor : PhoneWindow.this.mMinWidthMajor;
                if (tv.type != 0) {
                    if (tv.type == 5) {
                        min = (int) tv.getDimension(metrics);
                    } else if (tv.type == 6) {
                        min = (int) tv.getFraction(metrics.widthPixels, metrics.widthPixels);
                    } else {
                        min = 0;
                    }
                    if (width < min) {
                        widthMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(min, 1073741824);
                        measure = true;
                    }
                }
            }
            if (measure) {
                super.onMeasure(widthMeasureSpec2, heightMeasureSpec);
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
        public boolean showContextMenuForChild(View originalView) {
            if (PhoneWindow.this.mContextMenu != null) {
                PhoneWindow.this.mContextMenu.clearAll();
            } else {
                PhoneWindow.this.mContextMenu = new ContextMenuBuilder(getContext());
                PhoneWindow.this.mContextMenu.setCallback(PhoneWindow.this.mContextMenuCallback);
            }
            MenuDialogHelper helper = PhoneWindow.this.mContextMenu.show(originalView, originalView.getWindowToken());
            if (helper == null) {
                if (PhoneWindow.this.mContextMenuHelper != null) {
                    PhoneWindow.this.mContextMenuHelper.dismiss();
                }
            } else {
                helper.setPresenterCallback(PhoneWindow.this.mContextMenuCallback);
            }
            PhoneWindow.this.mContextMenuHelper = helper;
            if (helper != null) {
                return true;
            }
            return PhoneWindow.SWEEP_OPEN_MENU;
        }

        @Override
        public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
            return startActionMode(callback);
        }

        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {
            Context actionBarContext;
            if (this.mActionMode != null) {
                this.mActionMode.finish();
            }
            ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);
            ActionMode mode = null;
            if (PhoneWindow.this.getCallback() != null && !PhoneWindow.this.isDestroyed()) {
                try {
                    mode = PhoneWindow.this.getCallback().onWindowStartingActionMode(wrappedCallback);
                } catch (AbstractMethodError e) {
                }
            }
            if (mode != null) {
                this.mActionMode = mode;
            } else {
                if (this.mActionModeView == null) {
                    if (PhoneWindow.this.isFloating()) {
                        TypedValue outValue = new TypedValue();
                        Resources.Theme baseTheme = this.mContext.getTheme();
                        baseTheme.resolveAttribute(R.attr.actionBarTheme, outValue, true);
                        if (outValue.resourceId != 0) {
                            Resources.Theme actionBarTheme = this.mContext.getResources().newTheme();
                            actionBarTheme.setTo(baseTheme);
                            actionBarTheme.applyStyle(outValue.resourceId, true);
                            actionBarContext = new ContextThemeWrapper(this.mContext, 0);
                            actionBarContext.getTheme().setTo(actionBarTheme);
                        } else {
                            actionBarContext = this.mContext;
                        }
                        this.mActionModeView = new ActionBarContextView(actionBarContext);
                        this.mActionModePopup = new PopupWindow(actionBarContext, (AttributeSet) null, R.^attr-private.panelMenuListTheme);
                        this.mActionModePopup.setWindowLayoutType(2);
                        this.mActionModePopup.setContentView(this.mActionModeView);
                        this.mActionModePopup.setWidth(-1);
                        actionBarContext.getTheme().resolveAttribute(R.attr.actionBarSize, outValue, true);
                        int height = TypedValue.complexToDimensionPixelSize(outValue.data, actionBarContext.getResources().getDisplayMetrics());
                        this.mActionModeView.setContentHeight(height);
                        this.mActionModePopup.setHeight(-2);
                        this.mShowActionModePopup = new Runnable() {
                            @Override
                            public void run() {
                                DecorView.this.mActionModePopup.showAtLocation(DecorView.this.mActionModeView.getApplicationWindowToken(), 55, 0, 0);
                            }
                        };
                    } else {
                        ViewStub stub = (ViewStub) findViewById(R.id.inbox_text0);
                        if (stub != null) {
                            this.mActionModeView = stub.inflate();
                        }
                    }
                }
                if (this.mActionModeView != null) {
                    this.mActionModeView.killMode();
                    StandaloneActionMode standaloneActionMode = new StandaloneActionMode(this.mActionModeView.getContext(), this.mActionModeView, wrappedCallback, this.mActionModePopup == null);
                    if (callback.onCreateActionMode(standaloneActionMode, standaloneActionMode.getMenu())) {
                        standaloneActionMode.invalidate();
                        this.mActionModeView.initForMode(standaloneActionMode);
                        this.mActionModeView.setVisibility(0);
                        this.mActionMode = standaloneActionMode;
                        if (this.mActionModePopup != null) {
                            post(this.mShowActionModePopup);
                        }
                        this.mActionModeView.sendAccessibilityEvent(32);
                    } else {
                        this.mActionMode = null;
                    }
                }
            }
            if (this.mActionMode != null && PhoneWindow.this.getCallback() != null && !PhoneWindow.this.isDestroyed()) {
                try {
                    PhoneWindow.this.getCallback().onActionModeStarted(this.mActionMode);
                } catch (AbstractMethodError e2) {
                }
            }
            return this.mActionMode;
        }

        public void startChanging() {
            this.mChanging = true;
        }

        public void finishChanging() {
            this.mChanging = PhoneWindow.SWEEP_OPEN_MENU;
            drawableChanged();
        }

        public void setWindowBackground(Drawable drawable) {
            if (getBackground() != drawable) {
                setBackgroundDrawable(drawable);
                if (drawable != null) {
                    drawable.getPadding(this.mBackgroundPadding);
                } else {
                    this.mBackgroundPadding.setEmpty();
                }
                drawableChanged();
            }
        }

        @Override
        public void setBackgroundDrawable(Drawable d) {
            super.setBackgroundDrawable(d);
            if (getWindowToken() != null) {
                updateWindowResizeState();
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
        public void onWindowSystemUiVisibilityChanged(int visible) {
            updateColorViews(null, true);
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            this.mFrameOffsets.set(insets.getSystemWindowInsets());
            WindowInsets insets2 = updateStatusGuard(updateColorViews(insets, true));
            updateNavigationGuard(insets2);
            if (getForeground() != null) {
                drawableChanged();
            }
            return insets2;
        }

        @Override
        public boolean isTransitionGroup() {
            return PhoneWindow.SWEEP_OPEN_MENU;
        }

        private WindowInsets updateColorViews(WindowInsets insets, boolean animate) {
            WindowManager.LayoutParams attrs = PhoneWindow.this.getAttributes();
            int sysUiVisibility = attrs.systemUiVisibility | getWindowSystemUiVisibility();
            if (!PhoneWindow.this.mIsFloating && ActivityManager.isHighEndGfx()) {
                boolean disallowAnimate = (!isLaidOut() ? true : PhoneWindow.SWEEP_OPEN_MENU) | (((this.mLastWindowFlags ^ attrs.flags) & Integer.MIN_VALUE) != 0 ? true : PhoneWindow.SWEEP_OPEN_MENU);
                this.mLastWindowFlags = attrs.flags;
                if (insets != null) {
                    this.mLastTopInset = Math.min(insets.getStableInsetTop(), insets.getSystemWindowInsetTop());
                    this.mLastBottomInset = Math.min(insets.getStableInsetBottom(), insets.getSystemWindowInsetBottom());
                    this.mLastRightInset = Math.min(insets.getStableInsetRight(), insets.getSystemWindowInsetRight());
                    boolean hasTopStableInset = insets.getStableInsetTop() != 0 ? true : PhoneWindow.SWEEP_OPEN_MENU;
                    boolean disallowAnimate2 = disallowAnimate | (hasTopStableInset != this.mLastHasTopStableInset ? true : PhoneWindow.SWEEP_OPEN_MENU);
                    this.mLastHasTopStableInset = hasTopStableInset;
                    boolean hasBottomStableInset = insets.getStableInsetBottom() != 0 ? true : PhoneWindow.SWEEP_OPEN_MENU;
                    disallowAnimate = disallowAnimate2 | (hasBottomStableInset != this.mLastHasBottomStableInset ? true : PhoneWindow.SWEEP_OPEN_MENU);
                    this.mLastHasBottomStableInset = hasBottomStableInset;
                }
                updateColorViewInt(this.mStatusColorViewState, sysUiVisibility, PhoneWindow.this.mStatusBarColor, this.mLastTopInset, (!animate || disallowAnimate) ? PhoneWindow.SWEEP_OPEN_MENU : true);
                updateColorViewInt(this.mNavigationColorViewState, sysUiVisibility, PhoneWindow.this.mNavigationBarColor, this.mLastBottomInset, (!animate || disallowAnimate) ? PhoneWindow.SWEEP_OPEN_MENU : true);
            }
            boolean consumingNavBar = ((attrs.flags & Integer.MIN_VALUE) != 0 && (sysUiVisibility & 512) == 0 && (sysUiVisibility & 2) == 0) ? true : PhoneWindow.SWEEP_OPEN_MENU;
            int consumedRight = consumingNavBar ? this.mLastRightInset : 0;
            int consumedBottom = consumingNavBar ? this.mLastBottomInset : 0;
            if (PhoneWindow.this.mContentRoot != null && (PhoneWindow.this.mContentRoot.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) PhoneWindow.this.mContentRoot.getLayoutParams();
                if (lp.rightMargin != consumedRight || lp.bottomMargin != consumedBottom) {
                    lp.rightMargin = consumedRight;
                    lp.bottomMargin = consumedBottom;
                    PhoneWindow.this.mContentRoot.setLayoutParams(lp);
                    if (insets == null) {
                        requestApplyInsets();
                    }
                }
                if (insets != null) {
                    insets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight() - consumedRight, insets.getSystemWindowInsetBottom() - consumedBottom);
                }
            }
            if (insets != null) {
                return insets.consumeStableInsets();
            }
            return insets;
        }

        private void updateColorViewInt(final ColorViewState state, int sysUiVis, int color, int height, boolean animate) {
            boolean show = (height <= 0 || (state.systemUiHideFlag & sysUiVis) != 0 || (PhoneWindow.this.getAttributes().flags & state.hideWindowFlag) != 0 || (PhoneWindow.this.getAttributes().flags & state.translucentFlag) != 0 || ((-16777216) & color) == 0 || (PhoneWindow.this.getAttributes().flags & Integer.MIN_VALUE) == 0) ? PhoneWindow.SWEEP_OPEN_MENU : true;
            boolean visibilityChanged = PhoneWindow.SWEEP_OPEN_MENU;
            View view = state.view;
            if (view == null) {
                if (show) {
                    view = new View(this.mContext);
                    state.view = view;
                    view.setBackgroundColor(color);
                    view.setTransitionName(state.transitionName);
                    view.setId(state.id);
                    visibilityChanged = true;
                    view.setVisibility(PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK);
                    state.targetVisibility = 0;
                    addView(view, new FrameLayout.LayoutParams(-1, height, 8388611 | state.verticalGravity));
                    updateColorViewTranslations();
                }
            } else {
                int vis = show ? 0 : PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK;
                visibilityChanged = state.targetVisibility != vis ? true : PhoneWindow.SWEEP_OPEN_MENU;
                state.targetVisibility = vis;
                if (show) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                    if (lp.height != height) {
                        lp.height = height;
                        view.setLayoutParams(lp);
                    }
                    view.setBackgroundColor(color);
                }
            }
            if (visibilityChanged) {
                view.animate().cancel();
                if (animate) {
                    if (show) {
                        if (view.getVisibility() != 0) {
                            view.setVisibility(0);
                            view.setAlpha(0.0f);
                        }
                        view.animate().alpha(1.0f).setInterpolator(this.mShowInterpolator).setDuration(this.mBarEnterExitDuration);
                        return;
                    }
                    view.animate().alpha(0.0f).setInterpolator(this.mHideInterpolator).setDuration(this.mBarEnterExitDuration).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            state.view.setAlpha(1.0f);
                            state.view.setVisibility(PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK);
                        }
                    });
                    return;
                }
                view.setAlpha(1.0f);
                view.setVisibility(show ? 0 : PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK);
            }
        }

        private void updateColorViewTranslations() {
            int rootScrollY = this.mRootScrollY;
            if (this.mStatusColorViewState.view != null) {
                this.mStatusColorViewState.view.setTranslationY(rootScrollY > 0 ? rootScrollY : 0.0f);
            }
            if (this.mNavigationColorViewState.view != null) {
                this.mNavigationColorViewState.view.setTranslationY(rootScrollY < 0 ? rootScrollY : 0.0f);
            }
        }

        private WindowInsets updateStatusGuard(WindowInsets insets) {
            boolean showStatusGuard = PhoneWindow.SWEEP_OPEN_MENU;
            if (this.mActionModeView != null && (this.mActionModeView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) this.mActionModeView.getLayoutParams();
                boolean mlpChanged = PhoneWindow.SWEEP_OPEN_MENU;
                if (this.mActionModeView.isShown()) {
                    if (PhoneWindow.this.mTempRect == null) {
                        PhoneWindow.this.mTempRect = new Rect();
                    }
                    Rect rect = PhoneWindow.this.mTempRect;
                    PhoneWindow.this.mContentParent.computeSystemWindowInsets(insets, rect);
                    int newMargin = rect.top == 0 ? insets.getSystemWindowInsetTop() : 0;
                    if (mlp.topMargin != newMargin) {
                        mlpChanged = true;
                        mlp.topMargin = insets.getSystemWindowInsetTop();
                        if (this.mStatusGuard == null) {
                            this.mStatusGuard = new View(this.mContext);
                            this.mStatusGuard.setBackgroundColor(this.mContext.getResources().getColor(R.color.system_neutral2_600));
                            addView(this.mStatusGuard, indexOfChild(this.mStatusColorViewState.view), new FrameLayout.LayoutParams(-1, mlp.topMargin, 8388659));
                        } else {
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mStatusGuard.getLayoutParams();
                            if (lp.height != mlp.topMargin) {
                                lp.height = mlp.topMargin;
                                this.mStatusGuard.setLayoutParams(lp);
                            }
                        }
                    }
                    showStatusGuard = this.mStatusGuard != null ? true : PhoneWindow.SWEEP_OPEN_MENU;
                    boolean nonOverlay = (PhoneWindow.this.getLocalFeatures() & 1024) == 0 ? true : PhoneWindow.SWEEP_OPEN_MENU;
                    insets = insets.consumeSystemWindowInsets(PhoneWindow.SWEEP_OPEN_MENU, (nonOverlay && showStatusGuard) ? true : PhoneWindow.SWEEP_OPEN_MENU, PhoneWindow.SWEEP_OPEN_MENU, PhoneWindow.SWEEP_OPEN_MENU);
                } else if (mlp.topMargin != 0) {
                    mlpChanged = true;
                    mlp.topMargin = 0;
                }
                if (mlpChanged) {
                    this.mActionModeView.setLayoutParams(mlp);
                }
            }
            if (this.mStatusGuard != null) {
                this.mStatusGuard.setVisibility(showStatusGuard ? 0 : 8);
            }
            return insets;
        }

        private void updateNavigationGuard(WindowInsets insets) {
            if (PhoneWindow.this.getAttributes().type == 2011) {
                if (PhoneWindow.this.mContentParent != null && (PhoneWindow.this.mContentParent.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) PhoneWindow.this.mContentParent.getLayoutParams();
                    mlp.bottomMargin = insets.getSystemWindowInsetBottom();
                    PhoneWindow.this.mContentParent.setLayoutParams(mlp);
                }
                if (this.mNavigationGuard == null) {
                    this.mNavigationGuard = new View(this.mContext);
                    this.mNavigationGuard.setBackgroundColor(this.mContext.getResources().getColor(R.color.system_neutral2_600));
                    addView(this.mNavigationGuard, indexOfChild(this.mNavigationColorViewState.view), new FrameLayout.LayoutParams(-1, insets.getSystemWindowInsetBottom(), 8388691));
                } else {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mNavigationGuard.getLayoutParams();
                    lp.height = insets.getSystemWindowInsetBottom();
                    this.mNavigationGuard.setLayoutParams(lp);
                }
            }
        }

        private void drawableChanged() {
            if (!this.mChanging) {
                setPadding(this.mFramePadding.left + this.mBackgroundPadding.left, this.mFramePadding.top + this.mBackgroundPadding.top, this.mFramePadding.right + this.mBackgroundPadding.right, this.mFramePadding.bottom + this.mBackgroundPadding.bottom);
                requestLayout();
                invalidate();
                int opacity = -1;
                Drawable bg = getBackground();
                Drawable fg = getForeground();
                if (bg != null) {
                    if (fg == null) {
                        opacity = bg.getOpacity();
                    } else if (this.mFramePadding.left <= 0 && this.mFramePadding.top <= 0 && this.mFramePadding.right <= 0 && this.mFramePadding.bottom <= 0) {
                        int fop = fg.getOpacity();
                        int bop = bg.getOpacity();
                        if (fop == -1 || bop == -1) {
                            opacity = -1;
                        } else if (fop == 0) {
                            opacity = bop;
                        } else if (bop == 0) {
                            opacity = fop;
                        } else {
                            opacity = Drawable.resolveOpacity(fop, bop);
                        }
                    } else {
                        opacity = -3;
                    }
                }
                this.mDefaultOpacity = opacity;
                if (this.mFeatureId < 0) {
                    PhoneWindow.this.setDefaultWindowFormat(opacity);
                }
            }
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            if (PhoneWindow.this.hasFeature(0) && !hasWindowFocus && PhoneWindow.this.mPanelChordingKey != 0) {
                PhoneWindow.this.closePanel(0);
            }
            Window.Callback cb = PhoneWindow.this.getCallback();
            if (cb != null && !PhoneWindow.this.isDestroyed() && this.mFeatureId < 0) {
                cb.onWindowFocusChanged(hasWindowFocus);
            }
        }

        void updateWindowResizeState() {
            Drawable bg = getBackground();
            hackTurnOffWindowResizeAnim((bg == null || bg.getOpacity() != -1) ? true : PhoneWindow.SWEEP_OPEN_MENU);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            updateWindowResizeState();
            Window.Callback cb = PhoneWindow.this.getCallback();
            if (cb != null && !PhoneWindow.this.isDestroyed() && this.mFeatureId < 0) {
                cb.onAttachedToWindow();
            }
            if (this.mFeatureId == -1) {
                PhoneWindow.this.openPanelsAfterRestore();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            Window.Callback cb = PhoneWindow.this.getCallback();
            if (cb != null && this.mFeatureId < 0) {
                cb.onDetachedFromWindow();
            }
            if (PhoneWindow.this.mDecorContentParent != null) {
                PhoneWindow.this.mDecorContentParent.dismissPopups();
            }
            if (this.mActionModePopup != null) {
                removeCallbacks(this.mShowActionModePopup);
                if (this.mActionModePopup.isShowing()) {
                    this.mActionModePopup.dismiss();
                }
                this.mActionModePopup = null;
            }
            PanelFeatureState st = PhoneWindow.this.getPanelState(0, PhoneWindow.SWEEP_OPEN_MENU);
            if (st != null && st.menu != null && this.mFeatureId < 0) {
                st.menu.close();
            }
        }

        public void onCloseSystemDialogs(String reason) {
            if (this.mFeatureId >= 0) {
                PhoneWindow.this.closeAllPanels();
            }
        }

        public SurfaceHolder.Callback2 willYouTakeTheSurface() {
            if (this.mFeatureId < 0) {
                return PhoneWindow.this.mTakeSurfaceCallback;
            }
            return null;
        }

        public InputQueue.Callback willYouTakeTheInputQueue() {
            if (this.mFeatureId < 0) {
                return PhoneWindow.this.mTakeInputQueueCallback;
            }
            return null;
        }

        public void setSurfaceType(int type) {
            PhoneWindow.this.setType(type);
        }

        public void setSurfaceFormat(int format) {
            PhoneWindow.this.setFormat(format);
        }

        public void setSurfaceKeepScreenOn(boolean keepOn) {
            if (!keepOn) {
                PhoneWindow.this.clearFlags(128);
            } else {
                PhoneWindow.this.addFlags(128);
            }
        }

        public void onRootViewScrollYChanged(int rootScrollY) {
            this.mRootScrollY = rootScrollY;
            updateColorViewTranslations();
        }

        private class ActionModeCallbackWrapper implements ActionMode.Callback {
            private ActionMode.Callback mWrapped;

            public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
                this.mWrapped = wrapped;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return this.mWrapped.onCreateActionMode(mode, menu);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                DecorView.this.requestFitSystemWindows();
                return this.mWrapped.onPrepareActionMode(mode, menu);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return this.mWrapped.onActionItemClicked(mode, item);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                this.mWrapped.onDestroyActionMode(mode);
                if (DecorView.this.mActionModePopup != null) {
                    DecorView.this.removeCallbacks(DecorView.this.mShowActionModePopup);
                    DecorView.this.mActionModePopup.dismiss();
                } else if (DecorView.this.mActionModeView != null) {
                    DecorView.this.mActionModeView.setVisibility(8);
                }
                if (DecorView.this.mActionModeView != null) {
                    DecorView.this.mActionModeView.removeAllViews();
                }
                if (PhoneWindow.this.getCallback() != null && !PhoneWindow.this.isDestroyed()) {
                    try {
                        PhoneWindow.this.getCallback().onActionModeFinished(DecorView.this.mActionMode);
                    } catch (AbstractMethodError e) {
                    }
                }
                DecorView.this.mActionMode = null;
                DecorView.this.requestFitSystemWindows();
            }
        }
    }

    protected DecorView generateDecor() {
        return new DecorView(getContext(), -1);
    }

    protected void setFeatureFromAttrs(int featureId, TypedArray attrs, int drawableAttr, int alphaAttr) {
        int alpha;
        Drawable d = attrs.getDrawable(drawableAttr);
        if (d != null) {
            requestFeature(featureId);
            setFeatureDefaultDrawable(featureId, d);
        }
        if ((getFeatures() & (1 << featureId)) != 0 && (alpha = attrs.getInt(alphaAttr, -1)) >= 0) {
            setFeatureDrawableAlpha(featureId, alpha);
        }
    }

    protected ViewGroup generateLayout(DecorView decor) {
        int layoutResource;
        Drawable background;
        Drawable frame;
        ProgressBar progress;
        TypedArray a = getWindowStyle();
        this.mIsFloating = a.getBoolean(FLAG_RESOURCE_SET_ICON_FALLBACK, SWEEP_OPEN_MENU);
        int flagsToUpdate = 65792 & (getForcedWindowFlags() ^ (-1));
        if (this.mIsFloating) {
            setLayout(-2, -2);
            setFlags(0, flagsToUpdate);
        } else {
            setFlags(65792, flagsToUpdate);
        }
        if (a.getBoolean(3, SWEEP_OPEN_MENU)) {
            requestFeature(1);
        } else if (a.getBoolean(15, SWEEP_OPEN_MENU)) {
            requestFeature(8);
        }
        if (a.getBoolean(17, SWEEP_OPEN_MENU)) {
            requestFeature(9);
        }
        if (a.getBoolean(16, SWEEP_OPEN_MENU)) {
            requestFeature(10);
        }
        if (a.getBoolean(25, SWEEP_OPEN_MENU)) {
            requestFeature(11);
        }
        if (a.getBoolean(9, SWEEP_OPEN_MENU)) {
            setFlags(1024, (getForcedWindowFlags() ^ (-1)) & 1024);
        }
        if (a.getBoolean(23, SWEEP_OPEN_MENU)) {
            setFlags(67108864, 67108864 & (getForcedWindowFlags() ^ (-1)));
        }
        if (a.getBoolean(24, SWEEP_OPEN_MENU)) {
            setFlags(134217728, 134217728 & (getForcedWindowFlags() ^ (-1)));
        }
        if (a.getBoolean(22, SWEEP_OPEN_MENU)) {
            setFlags(33554432, 33554432 & (getForcedWindowFlags() ^ (-1)));
        }
        if (a.getBoolean(14, SWEEP_OPEN_MENU)) {
            setFlags(1048576, 1048576 & (getForcedWindowFlags() ^ (-1)));
        }
        if (a.getBoolean(18, getContext().getApplicationInfo().targetSdkVersion >= 11 ? true : SWEEP_OPEN_MENU)) {
            setFlags(8388608, 8388608 & (getForcedWindowFlags() ^ (-1)));
        }
        a.getValue(19, this.mMinWidthMajor);
        a.getValue(20, this.mMinWidthMinor);
        if (a.hasValue(48)) {
            if (this.mFixedWidthMajor == null) {
                this.mFixedWidthMajor = new TypedValue();
            }
            a.getValue(48, this.mFixedWidthMajor);
        }
        if (a.hasValue(50)) {
            if (this.mFixedWidthMinor == null) {
                this.mFixedWidthMinor = new TypedValue();
            }
            a.getValue(50, this.mFixedWidthMinor);
        }
        if (a.hasValue(51)) {
            if (this.mFixedHeightMajor == null) {
                this.mFixedHeightMajor = new TypedValue();
            }
            a.getValue(51, this.mFixedHeightMajor);
        }
        if (a.hasValue(49)) {
            if (this.mFixedHeightMinor == null) {
                this.mFixedHeightMinor = new TypedValue();
            }
            a.getValue(49, this.mFixedHeightMinor);
        }
        if (a.getBoolean(26, SWEEP_OPEN_MENU)) {
            requestFeature(12);
        }
        if (a.getBoolean(45, SWEEP_OPEN_MENU)) {
            requestFeature(13);
        }
        WindowManager windowService = (WindowManager) getContext().getSystemService("window");
        if (windowService != null) {
            Display display = windowService.getDefaultDisplay();
            boolean shouldUseBottomOutset = (display.getDisplayId() == 0 || (getForcedWindowFlags() & 1024) != 0) ? true : SWEEP_OPEN_MENU;
            if (shouldUseBottomOutset && a.hasValue(52)) {
                if (this.mOutsetBottom == null) {
                    this.mOutsetBottom = new TypedValue();
                }
                a.getValue(52, this.mOutsetBottom);
            }
        }
        Context context = getContext();
        int targetSdk = context.getApplicationInfo().targetSdkVersion;
        boolean targetPreHoneycomb = targetSdk < 11 ? true : SWEEP_OPEN_MENU;
        boolean targetPreIcs = targetSdk < 14 ? true : SWEEP_OPEN_MENU;
        boolean targetPreL = targetSdk < 21 ? true : SWEEP_OPEN_MENU;
        boolean targetHcNeedsOptions = context.getResources().getBoolean(R.^attr-private.actionModeRedoDrawable);
        boolean noActionBar = (!hasFeature(8) || hasFeature(1)) ? true : SWEEP_OPEN_MENU;
        if (targetPreHoneycomb || (targetPreIcs && targetHcNeedsOptions && noActionBar)) {
            setNeedsMenuKey(1);
        } else {
            setNeedsMenuKey(2);
        }
        if (!this.mIsFloating && ActivityManager.isHighEndGfx() && !targetPreL && a.getBoolean(34, SWEEP_OPEN_MENU)) {
            setFlags(Integer.MIN_VALUE, Integer.MIN_VALUE & (getForcedWindowFlags() ^ (-1)));
        }
        if (!this.mForcedStatusBarColor) {
            this.mStatusBarColor = a.getColor(35, -16777216);
        }
        if (!this.mForcedNavigationBarColor) {
            this.mNavigationBarColor = a.getColor(36, -16777216);
        }
        if ((this.mAlwaysReadCloseOnTouchAttr || getContext().getApplicationInfo().targetSdkVersion >= 11) && a.getBoolean(21, SWEEP_OPEN_MENU)) {
            setCloseOnTouchOutsideIfNotSet(true);
        }
        WindowManager.LayoutParams params = getAttributes();
        if (!hasSoftInputMode()) {
            params.softInputMode = a.getInt(13, params.softInputMode);
        }
        if (a.getBoolean(11, this.mIsFloating)) {
            if ((getForcedWindowFlags() & 2) == 0) {
                params.flags |= 2;
            }
            if (!haveDimAmount()) {
                params.dimAmount = a.getFloat(0, 0.5f);
            }
        }
        if (params.windowAnimations == 0) {
            params.windowAnimations = a.getResourceId(8, 0);
        }
        if (getContainer() == null) {
            if (this.mBackgroundDrawable == null) {
                if (this.mBackgroundResource == 0) {
                    this.mBackgroundResource = a.getResourceId(1, 0);
                }
                if (this.mFrameResource == 0) {
                    this.mFrameResource = a.getResourceId(2, 0);
                }
                this.mBackgroundFallbackResource = a.getResourceId(46, 0);
            }
            this.mElevation = a.getDimension(38, 0.0f);
            this.mClipToOutline = a.getBoolean(39, SWEEP_OPEN_MENU);
            this.mTextColor = a.getColor(7, 0);
        }
        int features = getLocalFeatures();
        if ((features & 2048) != 0) {
            layoutResource = R.layout.notification_2025_expand_button;
        } else if ((features & 24) != 0) {
            if (this.mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(R.^attr-private.preferenceHeaderPanelStyle, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.notification_2025_reply_history_container;
            }
            removeFeature(8);
        } else if ((features & 36) != 0 && (features & 256) == 0) {
            layoutResource = R.layout.notification_2025_conversation_face_pile_layout;
        } else if ((features & 128) != 0) {
            if (this.mIsFloating) {
                TypedValue res2 = new TypedValue();
                getContext().getTheme().resolveAttribute(R.^attr-private.preferenceListStyle, res2, true);
                layoutResource = res2.resourceId;
            } else {
                layoutResource = R.layout.notification_2025_action_list;
            }
            removeFeature(8);
        } else if ((features & 2) == 0) {
            if (this.mIsFloating) {
                TypedValue res3 = new TypedValue();
                getContext().getTheme().resolveAttribute(R.^attr-private.preferenceFragmentListStyle, res3, true);
                layoutResource = res3.resourceId;
            } else if ((features & 256) != 0) {
                layoutResource = a.getResourceId(47, R.layout.miniresolver);
            } else {
                layoutResource = R.layout.notification_2025_messaging_group;
            }
        } else if ((features & 1024) != 0) {
            layoutResource = R.layout.notification_2025_conversation_icon_container;
        } else {
            layoutResource = R.layout.notification_2025_conversation_header;
        }
        this.mDecor.startChanging();
        View in = this.mLayoutInflater.inflate(layoutResource, (ViewGroup) null);
        decor.addView(in, new ViewGroup.LayoutParams(-1, -1));
        this.mContentRoot = (ViewGroup) in;
        ViewGroup contentParent = (ViewGroup) findViewById(R.id.content);
        if (contentParent == null) {
            throw new RuntimeException("Window couldn't find content container view");
        }
        if ((features & 32) != 0 && (progress = getCircularProgressBar(SWEEP_OPEN_MENU)) != null) {
            progress.setIndeterminate(true);
        }
        if ((features & 2048) != 0) {
            registerSwipeCallbacks();
        }
        if (getContainer() == null) {
            if (this.mBackgroundResource != 0) {
                background = getContext().getDrawable(this.mBackgroundResource);
            } else {
                background = this.mBackgroundDrawable;
            }
            this.mDecor.setWindowBackground(background);
            if (this.mFrameResource != 0) {
                frame = getContext().getDrawable(this.mFrameResource);
            } else {
                frame = null;
            }
            this.mDecor.setWindowFrame(frame);
            this.mDecor.setElevation(this.mElevation);
            this.mDecor.setClipToOutline(this.mClipToOutline);
            if (this.mTitle != null) {
                setTitle(this.mTitle);
            }
            if (this.mTitleColor == 0) {
                this.mTitleColor = this.mTextColor;
            }
            setTitleColor(this.mTitleColor);
        }
        this.mDecor.finishChanging();
        return contentParent;
    }

    public void alwaysReadCloseOnTouchAttr() {
        this.mAlwaysReadCloseOnTouchAttr = true;
    }

    private void installDecor() {
        if (this.mDecor == null) {
            this.mDecor = generateDecor();
            this.mDecor.setDescendantFocusability(262144);
            this.mDecor.setIsRootNamespace(true);
            if (!this.mInvalidatePanelMenuPosted && this.mInvalidatePanelMenuFeatures != 0) {
                this.mDecor.postOnAnimation(this.mInvalidatePanelMenuRunnable);
            }
        }
        if (this.mContentParent == null) {
            this.mContentParent = generateLayout(this.mDecor);
            this.mDecor.makeOptionalFitsSystemWindows();
            DecorContentParent decorContentParent = this.mDecor.findViewById(R.id.inbox_text5);
            if (decorContentParent != null) {
                this.mDecorContentParent = decorContentParent;
                this.mDecorContentParent.setWindowCallback(getCallback());
                if (this.mDecorContentParent.getTitle() == null) {
                    this.mDecorContentParent.setWindowTitle(this.mTitle);
                }
                int localFeatures = getLocalFeatures();
                for (int i = 0; i < 13; i++) {
                    if (((1 << i) & localFeatures) != 0) {
                        this.mDecorContentParent.initFeature(i);
                    }
                }
                this.mDecorContentParent.setUiOptions(this.mUiOptions);
                if ((this.mResourcesSetFlags & 1) != 0 || (this.mIconRes != 0 && !this.mDecorContentParent.hasIcon())) {
                    this.mDecorContentParent.setIcon(this.mIconRes);
                } else if ((this.mResourcesSetFlags & 1) == 0 && this.mIconRes == 0 && !this.mDecorContentParent.hasIcon()) {
                    this.mDecorContentParent.setIcon(getContext().getPackageManager().getDefaultActivityIcon());
                    this.mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON_FALLBACK;
                }
                if ((this.mResourcesSetFlags & 2) != 0 || (this.mLogoRes != 0 && !this.mDecorContentParent.hasLogo())) {
                    this.mDecorContentParent.setLogo(this.mLogoRes);
                }
                PanelFeatureState st = getPanelState(0, SWEEP_OPEN_MENU);
                if (!isDestroyed() && (st == null || st.menu == null)) {
                    invalidatePanelMenu(8);
                }
            } else {
                this.mTitleView = (TextView) findViewById(R.id.title);
                if (this.mTitleView != null) {
                    this.mTitleView.setLayoutDirection(this.mDecor.getLayoutDirection());
                    if ((getLocalFeatures() & 2) != 0) {
                        View titleContainer = findViewById(R.id.shareText);
                        if (titleContainer != null) {
                            titleContainer.setVisibility(8);
                        } else {
                            this.mTitleView.setVisibility(8);
                        }
                        if (this.mContentParent instanceof FrameLayout) {
                            ((FrameLayout) this.mContentParent).setForeground(null);
                        }
                    } else {
                        this.mTitleView.setText(this.mTitle);
                    }
                }
            }
            if (this.mDecor.getBackground() == null && this.mBackgroundFallbackResource != 0) {
                this.mDecor.setBackgroundFallback(this.mBackgroundFallbackResource);
            }
            if (hasFeature(13)) {
                if (this.mTransitionManager == null) {
                    int transitionRes = getWindowStyle().getResourceId(27, 0);
                    if (transitionRes != 0) {
                        TransitionInflater inflater = TransitionInflater.from(getContext());
                        this.mTransitionManager = inflater.inflateTransitionManager(transitionRes, this.mContentParent);
                    } else {
                        this.mTransitionManager = new TransitionManager();
                    }
                }
                this.mEnterTransition = getTransition(this.mEnterTransition, null, 28);
                this.mReturnTransition = getTransition(this.mReturnTransition, USE_DEFAULT_TRANSITION, 40);
                this.mExitTransition = getTransition(this.mExitTransition, null, 29);
                this.mReenterTransition = getTransition(this.mReenterTransition, USE_DEFAULT_TRANSITION, 41);
                this.mSharedElementEnterTransition = getTransition(this.mSharedElementEnterTransition, null, 30);
                this.mSharedElementReturnTransition = getTransition(this.mSharedElementReturnTransition, USE_DEFAULT_TRANSITION, 42);
                this.mSharedElementExitTransition = getTransition(this.mSharedElementExitTransition, null, 31);
                this.mSharedElementReenterTransition = getTransition(this.mSharedElementReenterTransition, USE_DEFAULT_TRANSITION, 43);
                if (this.mAllowEnterTransitionOverlap == null) {
                    this.mAllowEnterTransitionOverlap = Boolean.valueOf(getWindowStyle().getBoolean(33, true));
                }
                if (this.mAllowReturnTransitionOverlap == null) {
                    this.mAllowReturnTransitionOverlap = Boolean.valueOf(getWindowStyle().getBoolean(32, true));
                }
                if (this.mBackgroundFadeDurationMillis < 0) {
                    this.mBackgroundFadeDurationMillis = getWindowStyle().getInteger(37, DEFAULT_BACKGROUND_FADE_DURATION_MS);
                }
                if (this.mSharedElementsUseOverlay == null) {
                    this.mSharedElementsUseOverlay = Boolean.valueOf(getWindowStyle().getBoolean(44, true));
                }
            }
        }
    }

    private Transition getTransition(Transition currentValue, Transition defaultValue, int id) {
        if (currentValue == defaultValue) {
            int transitionId = getWindowStyle().getResourceId(id, -1);
            Transition transition = defaultValue;
            if (transitionId != -1 && transitionId != 17760256) {
                TransitionInflater inflater = TransitionInflater.from(getContext());
                transition = inflater.inflateTransition(transitionId);
                if ((transition instanceof TransitionSet) && ((TransitionSet) transition).getTransitionCount() == 0) {
                    transition = null;
                }
            }
            return transition;
        }
        return currentValue;
    }

    private Drawable loadImageURI(Uri uri) {
        try {
            return Drawable.createFromStream(getContext().getContentResolver().openInputStream(uri), null);
        } catch (Exception e) {
            Log.w(TAG, "Unable to open content: " + uri);
            return null;
        }
    }

    private DrawableFeatureState getDrawableState(int featureId, boolean required) {
        if ((getFeatures() & (1 << featureId)) == 0) {
            if (!required) {
                return null;
            }
            throw new RuntimeException("The feature has not been requested");
        }
        DrawableFeatureState[] ar = this.mDrawables;
        if (ar == null || ar.length <= featureId) {
            DrawableFeatureState[] nar = new DrawableFeatureState[featureId + 1];
            if (ar != null) {
                System.arraycopy(ar, 0, nar, 0, ar.length);
            }
            ar = nar;
            this.mDrawables = nar;
        }
        DrawableFeatureState st = ar[featureId];
        if (st == null) {
            DrawableFeatureState st2 = new DrawableFeatureState(featureId);
            ar[featureId] = st2;
            return st2;
        }
        return st;
    }

    private PanelFeatureState getPanelState(int featureId, boolean required) {
        return getPanelState(featureId, required, null);
    }

    private PanelFeatureState getPanelState(int featureId, boolean required, PanelFeatureState convertPanelState) {
        if ((getFeatures() & (1 << featureId)) == 0) {
            if (!required) {
                return null;
            }
            throw new RuntimeException("The feature has not been requested");
        }
        PanelFeatureState[] ar = this.mPanels;
        if (ar == null || ar.length <= featureId) {
            PanelFeatureState[] nar = new PanelFeatureState[featureId + 1];
            if (ar != null) {
                System.arraycopy(ar, 0, nar, 0, ar.length);
            }
            ar = nar;
            this.mPanels = nar;
        }
        PanelFeatureState st = ar[featureId];
        if (st == null) {
            PanelFeatureState st2 = convertPanelState != null ? convertPanelState : new PanelFeatureState(featureId);
            ar[featureId] = st2;
            return st2;
        }
        return st;
    }

    @Override
    public final void setChildDrawable(int featureId, Drawable drawable) {
        DrawableFeatureState st = getDrawableState(featureId, true);
        st.child = drawable;
        updateDrawable(featureId, st, SWEEP_OPEN_MENU);
    }

    @Override
    public final void setChildInt(int featureId, int value) {
        updateInt(featureId, value, SWEEP_OPEN_MENU);
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        PanelFeatureState st = getPanelState(0, SWEEP_OPEN_MENU);
        if (st == null || st.menu == null || !st.menu.isShortcutKey(keyCode, event)) {
            return SWEEP_OPEN_MENU;
        }
        return true;
    }

    private void updateDrawable(int featureId, DrawableFeatureState st, boolean fromResume) {
        if (this.mContentParent != null) {
            int featureMask = 1 << featureId;
            if ((getFeatures() & featureMask) != 0 || fromResume) {
                Drawable drawable = null;
                if (st != null) {
                    drawable = st.child;
                    if (drawable == null) {
                        drawable = st.local;
                    }
                    if (drawable == null) {
                        drawable = st.def;
                    }
                }
                if ((getLocalFeatures() & featureMask) == 0) {
                    if (getContainer() != null) {
                        if (isActive() || fromResume) {
                            getContainer().setChildDrawable(featureId, drawable);
                            return;
                        }
                        return;
                    }
                    return;
                }
                if (st != null) {
                    if (st.cur != drawable || st.curAlpha != st.alpha) {
                        st.cur = drawable;
                        st.curAlpha = st.alpha;
                        onDrawableChanged(featureId, drawable, st.alpha);
                    }
                }
            }
        }
    }

    private void updateInt(int featureId, int value, boolean fromResume) {
        if (this.mContentParent != null) {
            int featureMask = 1 << featureId;
            if ((getFeatures() & featureMask) != 0 || fromResume) {
                if ((getLocalFeatures() & featureMask) == 0) {
                    if (getContainer() != null) {
                        getContainer().setChildInt(featureId, value);
                        return;
                    }
                    return;
                }
                onIntChanged(featureId, value);
            }
        }
    }

    private ImageView getLeftIconView() {
        if (this.mLeftIconView != null) {
            return this.mLeftIconView;
        }
        if (this.mContentParent == null) {
            installDecor();
        }
        ImageView imageView = (ImageView) findViewById(R.id.undo);
        this.mLeftIconView = imageView;
        return imageView;
    }

    protected void dispatchWindowAttributesChanged(WindowManager.LayoutParams attrs) {
        super.dispatchWindowAttributesChanged(attrs);
        if (this.mDecor != null) {
            this.mDecor.updateColorViews(null, true);
        }
    }

    private ProgressBar getCircularProgressBar(boolean shouldInstallDecor) {
        if (this.mCircularProgressBar != null) {
            return this.mCircularProgressBar;
        }
        if (this.mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        this.mCircularProgressBar = (ProgressBar) findViewById(R.id.inbox_text3);
        if (this.mCircularProgressBar != null) {
            this.mCircularProgressBar.setVisibility(FLAG_RESOURCE_SET_ICON_FALLBACK);
        }
        return this.mCircularProgressBar;
    }

    private ProgressBar getHorizontalProgressBar(boolean shouldInstallDecor) {
        if (this.mHorizontalProgressBar != null) {
            return this.mHorizontalProgressBar;
        }
        if (this.mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        this.mHorizontalProgressBar = (ProgressBar) findViewById(R.id.inbox_text4);
        if (this.mHorizontalProgressBar != null) {
            this.mHorizontalProgressBar.setVisibility(FLAG_RESOURCE_SET_ICON_FALLBACK);
        }
        return this.mHorizontalProgressBar;
    }

    private ImageView getRightIconView() {
        if (this.mRightIconView != null) {
            return this.mRightIconView;
        }
        if (this.mContentParent == null) {
            installDecor();
        }
        ImageView imageView = (ImageView) findViewById(R.id.replaceText);
        this.mRightIconView = imageView;
        return imageView;
    }

    private void registerSwipeCallbacks() {
        SwipeDismissLayout swipeDismiss = findViewById(R.id.content);
        swipeDismiss.setOnDismissedListener(new SwipeDismissLayout.OnDismissedListener() {
            public void onDismissed(SwipeDismissLayout layout) {
                PhoneWindow.this.dispatchOnWindowDismissed();
            }
        });
        swipeDismiss.setOnSwipeProgressChangedListener(new SwipeDismissLayout.OnSwipeProgressChangedListener() {
            private static final float ALPHA_DECREASE = 0.5f;
            private boolean mIsTranslucent = PhoneWindow.SWEEP_OPEN_MENU;

            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
                int flags;
                WindowManager.LayoutParams newParams = PhoneWindow.this.getAttributes();
                newParams.x = (int) translate;
                newParams.alpha = 1.0f - (ALPHA_DECREASE * progress);
                PhoneWindow.this.setAttributes(newParams);
                if (newParams.x == 0) {
                    flags = 1024;
                } else {
                    flags = 512;
                }
                PhoneWindow.this.setFlags(flags, 1536);
            }

            public void onSwipeCancelled(SwipeDismissLayout layout) {
                WindowManager.LayoutParams newParams = PhoneWindow.this.getAttributes();
                newParams.x = 0;
                newParams.alpha = 1.0f;
                PhoneWindow.this.setAttributes(newParams);
                PhoneWindow.this.setFlags(1024, 1536);
            }
        });
    }

    private void callOnPanelClosed(int featureId, PanelFeatureState panel, Menu menu) {
        Window.Callback cb = getCallback();
        if (cb != null) {
            if (menu == null) {
                if (panel == null && featureId >= 0 && featureId < this.mPanels.length) {
                    panel = this.mPanels[featureId];
                }
                if (panel != null) {
                    menu = panel.menu;
                }
            }
            if ((panel == null || panel.isOpen) && !isDestroyed()) {
                cb.onPanelClosed(featureId, menu);
            }
        }
    }

    private boolean launchDefaultSearch() {
        boolean result;
        Window.Callback cb = getCallback();
        if (cb == null || isDestroyed()) {
            result = SWEEP_OPEN_MENU;
        } else {
            sendCloseSystemWindows("search");
            result = cb.onSearchRequested();
        }
        if (!result && (getContext().getResources().getConfiguration().uiMode & 15) == FLAG_RESOURCE_SET_ICON_FALLBACK) {
            boolean result2 = ((SearchManager) getContext().getSystemService("search")).launchAssistAction(0, null, UserHandle.myUserId());
            return result2;
        }
        return result;
    }

    @Override
    public void setVolumeControlStream(int streamType) {
        this.mVolumeControlStreamType = streamType;
    }

    @Override
    public int getVolumeControlStream() {
        return this.mVolumeControlStreamType;
    }

    @Override
    public void setMediaController(MediaController controller) {
        this.mMediaController = controller;
    }

    @Override
    public MediaController getMediaController() {
        return this.mMediaController;
    }

    private boolean isTranslucent() {
        TypedArray a = getWindowStyle();
        return a.getBoolean(a.getResourceId(5, 0), SWEEP_OPEN_MENU);
    }

    @Override
    public void setEnterTransition(Transition enterTransition) {
        this.mEnterTransition = enterTransition;
    }

    @Override
    public void setReturnTransition(Transition transition) {
        this.mReturnTransition = transition;
    }

    @Override
    public void setExitTransition(Transition exitTransition) {
        this.mExitTransition = exitTransition;
    }

    @Override
    public void setReenterTransition(Transition transition) {
        this.mReenterTransition = transition;
    }

    @Override
    public void setSharedElementEnterTransition(Transition sharedElementEnterTransition) {
        this.mSharedElementEnterTransition = sharedElementEnterTransition;
    }

    @Override
    public void setSharedElementReturnTransition(Transition transition) {
        this.mSharedElementReturnTransition = transition;
    }

    @Override
    public void setSharedElementExitTransition(Transition sharedElementExitTransition) {
        this.mSharedElementExitTransition = sharedElementExitTransition;
    }

    @Override
    public void setSharedElementReenterTransition(Transition transition) {
        this.mSharedElementReenterTransition = transition;
    }

    @Override
    public Transition getEnterTransition() {
        return this.mEnterTransition;
    }

    @Override
    public Transition getReturnTransition() {
        return this.mReturnTransition == USE_DEFAULT_TRANSITION ? getEnterTransition() : this.mReturnTransition;
    }

    @Override
    public Transition getExitTransition() {
        return this.mExitTransition;
    }

    @Override
    public Transition getReenterTransition() {
        return this.mReenterTransition == USE_DEFAULT_TRANSITION ? getExitTransition() : this.mReenterTransition;
    }

    @Override
    public Transition getSharedElementEnterTransition() {
        return this.mSharedElementEnterTransition;
    }

    @Override
    public Transition getSharedElementReturnTransition() {
        return this.mSharedElementReturnTransition == USE_DEFAULT_TRANSITION ? getSharedElementEnterTransition() : this.mSharedElementReturnTransition;
    }

    @Override
    public Transition getSharedElementExitTransition() {
        return this.mSharedElementExitTransition;
    }

    @Override
    public Transition getSharedElementReenterTransition() {
        return this.mSharedElementReenterTransition == USE_DEFAULT_TRANSITION ? getSharedElementExitTransition() : this.mSharedElementReenterTransition;
    }

    @Override
    public void setAllowEnterTransitionOverlap(boolean allow) {
        this.mAllowEnterTransitionOverlap = Boolean.valueOf(allow);
    }

    @Override
    public boolean getAllowEnterTransitionOverlap() {
        if (this.mAllowEnterTransitionOverlap == null) {
            return true;
        }
        return this.mAllowEnterTransitionOverlap.booleanValue();
    }

    @Override
    public void setAllowReturnTransitionOverlap(boolean allowExitTransitionOverlap) {
        this.mAllowReturnTransitionOverlap = Boolean.valueOf(allowExitTransitionOverlap);
    }

    @Override
    public boolean getAllowReturnTransitionOverlap() {
        if (this.mAllowReturnTransitionOverlap == null) {
            return true;
        }
        return this.mAllowReturnTransitionOverlap.booleanValue();
    }

    @Override
    public long getTransitionBackgroundFadeDuration() {
        if (this.mBackgroundFadeDurationMillis < 0) {
            return 300L;
        }
        return this.mBackgroundFadeDurationMillis;
    }

    @Override
    public void setTransitionBackgroundFadeDuration(long fadeDurationMillis) {
        if (fadeDurationMillis < 0) {
            throw new IllegalArgumentException("negative durations are not allowed");
        }
        this.mBackgroundFadeDurationMillis = fadeDurationMillis;
    }

    @Override
    public void setSharedElementsUseOverlay(boolean sharedElementsUseOverlay) {
        this.mSharedElementsUseOverlay = Boolean.valueOf(sharedElementsUseOverlay);
    }

    @Override
    public boolean getSharedElementsUseOverlay() {
        if (this.mSharedElementsUseOverlay == null) {
            return true;
        }
        return this.mSharedElementsUseOverlay.booleanValue();
    }

    private static final class DrawableFeatureState {
        Drawable child;
        Drawable cur;
        Drawable def;
        final int featureId;
        Drawable local;
        int resid;
        Uri uri;
        int alpha = 255;
        int curAlpha = 255;

        DrawableFeatureState(int _featureId) {
            this.featureId = _featureId;
        }
    }

    private static final class PanelFeatureState {
        int background;
        View createdPanelView;
        DecorView decorView;
        int featureId;
        Bundle frozenActionViewState;
        Bundle frozenMenuState;
        int fullBackground;
        int gravity;
        IconMenuPresenter iconMenuPresenter;
        boolean isCompact;
        boolean isHandled;
        boolean isInExpandedMode;
        boolean isOpen;
        boolean isPrepared;
        ListMenuPresenter listMenuPresenter;
        int listPresenterTheme;
        MenuBuilder menu;
        public boolean qwertyMode;
        boolean refreshDecorView = PhoneWindow.SWEEP_OPEN_MENU;
        boolean refreshMenuContent;
        View shownPanelView;
        boolean wasLastExpanded;
        boolean wasLastOpen;
        int windowAnimations;
        int x;
        int y;

        PanelFeatureState(int featureId) {
            this.featureId = featureId;
        }

        public boolean isInListMode() {
            if (this.isInExpandedMode || this.isCompact) {
                return true;
            }
            return PhoneWindow.SWEEP_OPEN_MENU;
        }

        public boolean hasPanelItems() {
            if (this.shownPanelView == null) {
                return PhoneWindow.SWEEP_OPEN_MENU;
            }
            if (this.createdPanelView != null) {
                return true;
            }
            if (this.isCompact || this.isInExpandedMode) {
                return this.listMenuPresenter.getAdapter().getCount() > 0;
            }
            if (((ViewGroup) this.shownPanelView).getChildCount() <= 0) {
                return PhoneWindow.SWEEP_OPEN_MENU;
            }
            return true;
        }

        public void clearMenuPresenters() {
            if (this.menu != null) {
                this.menu.removeMenuPresenter(this.iconMenuPresenter);
                this.menu.removeMenuPresenter(this.listMenuPresenter);
            }
            this.iconMenuPresenter = null;
            this.listMenuPresenter = null;
        }

        void setStyle(Context context) {
            TypedArray a = context.obtainStyledAttributes(com.android.internal.R.styleable.Theme);
            this.background = a.getResourceId(46, 0);
            this.fullBackground = a.getResourceId(47, 0);
            this.windowAnimations = a.getResourceId(93, 0);
            this.isCompact = a.getBoolean(280, PhoneWindow.SWEEP_OPEN_MENU);
            this.listPresenterTheme = a.getResourceId(282, R.style.Widget.DeviceDefault.Light.AbsListView);
            a.recycle();
        }

        void setMenu(MenuBuilder menu) {
            if (menu != this.menu) {
                if (this.menu != null) {
                    this.menu.removeMenuPresenter(this.iconMenuPresenter);
                    this.menu.removeMenuPresenter(this.listMenuPresenter);
                }
                this.menu = menu;
                if (menu != null) {
                    if (this.iconMenuPresenter != null) {
                        menu.addMenuPresenter(this.iconMenuPresenter);
                    }
                    if (this.listMenuPresenter != null) {
                        menu.addMenuPresenter(this.listMenuPresenter);
                    }
                }
            }
        }

        MenuView getListMenuView(Context context, MenuPresenter.Callback cb) {
            if (this.menu == null) {
                return null;
            }
            if (!this.isCompact) {
                getIconMenuView(context, cb);
            }
            if (this.listMenuPresenter == null) {
                this.listMenuPresenter = new ListMenuPresenter(R.layout.chooser_row, this.listPresenterTheme);
                this.listMenuPresenter.setCallback(cb);
                this.listMenuPresenter.setId(R.id.icon_frame);
                this.menu.addMenuPresenter(this.listMenuPresenter);
            }
            if (this.iconMenuPresenter != null) {
                this.listMenuPresenter.setItemIndexOffset(this.iconMenuPresenter.getNumActualItemsShown());
            }
            return this.listMenuPresenter.getMenuView(this.decorView);
        }

        MenuView getIconMenuView(Context context, MenuPresenter.Callback cb) {
            if (this.menu == null) {
                return null;
            }
            if (this.iconMenuPresenter == null) {
                this.iconMenuPresenter = new IconMenuPresenter(context);
                this.iconMenuPresenter.setCallback(cb);
                this.iconMenuPresenter.setId(R.id.accessibilityActionSetProgress);
                this.menu.addMenuPresenter(this.iconMenuPresenter);
            }
            return this.iconMenuPresenter.getMenuView(this.decorView);
        }

        Parcelable onSaveInstanceState() {
            SavedState savedState = new SavedState();
            savedState.featureId = this.featureId;
            savedState.isOpen = this.isOpen;
            savedState.isInExpandedMode = this.isInExpandedMode;
            if (this.menu != null) {
                savedState.menuState = new Bundle();
                this.menu.savePresenterStates(savedState.menuState);
            }
            return savedState;
        }

        void onRestoreInstanceState(Parcelable state) {
            SavedState savedState = (SavedState) state;
            this.featureId = savedState.featureId;
            this.wasLastOpen = savedState.isOpen;
            this.wasLastExpanded = savedState.isInExpandedMode;
            this.frozenMenuState = savedState.menuState;
            this.createdPanelView = null;
            this.shownPanelView = null;
            this.decorView = null;
        }

        void applyFrozenState() {
            if (this.menu != null && this.frozenMenuState != null) {
                this.menu.restorePresenterStates(this.frozenMenuState);
                this.frozenMenuState = null;
            }
        }

        private static class SavedState implements Parcelable {
            public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
                @Override
                public SavedState createFromParcel(Parcel in) {
                    return SavedState.readFromParcel(in);
                }

                @Override
                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
            int featureId;
            boolean isInExpandedMode;
            boolean isOpen;
            Bundle menuState;

            private SavedState() {
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(this.featureId);
                dest.writeInt(this.isOpen ? 1 : 0);
                dest.writeInt(this.isInExpandedMode ? 1 : 0);
                if (this.isOpen) {
                    dest.writeBundle(this.menuState);
                }
            }

            private static SavedState readFromParcel(Parcel source) {
                SavedState savedState = new SavedState();
                savedState.featureId = source.readInt();
                savedState.isOpen = source.readInt() == 1;
                savedState.isInExpandedMode = source.readInt() == 1;
                if (savedState.isOpen) {
                    savedState.menuState = source.readBundle();
                }
                return savedState;
            }
        }
    }

    static class RotationWatcher extends IRotationWatcher.Stub {
        private Handler mHandler;
        private boolean mIsWatching;
        private final Runnable mRotationChanged = new Runnable() {
            @Override
            public void run() {
                RotationWatcher.this.dispatchRotationChanged();
            }
        };
        private final ArrayList<WeakReference<PhoneWindow>> mWindows = new ArrayList<>();

        RotationWatcher() {
        }

        public void onRotationChanged(int rotation) throws RemoteException {
            this.mHandler.post(this.mRotationChanged);
        }

        public void addWindow(PhoneWindow phoneWindow) {
            synchronized (this.mWindows) {
                if (!this.mIsWatching) {
                    try {
                        WindowManagerHolder.sWindowManager.watchRotation(this);
                        this.mHandler = new Handler();
                        this.mIsWatching = true;
                    } catch (RemoteException ex) {
                        Log.e(PhoneWindow.TAG, "Couldn't start watching for device rotation", ex);
                    }
                    this.mWindows.add(new WeakReference<>(phoneWindow));
                } else {
                    this.mWindows.add(new WeakReference<>(phoneWindow));
                }
            }
        }

        public void removeWindow(PhoneWindow phoneWindow) {
            synchronized (this.mWindows) {
                int i = 0;
                while (i < this.mWindows.size()) {
                    WeakReference<PhoneWindow> ref = this.mWindows.get(i);
                    PhoneWindow win = ref.get();
                    if (win == null || win == phoneWindow) {
                        this.mWindows.remove(i);
                    } else {
                        i++;
                    }
                }
            }
        }

        void dispatchRotationChanged() {
            synchronized (this.mWindows) {
                int i = 0;
                while (i < this.mWindows.size()) {
                    WeakReference<PhoneWindow> ref = this.mWindows.get(i);
                    PhoneWindow win = ref.get();
                    if (win != null) {
                        win.onOptionsPanelRotationChanged();
                        i++;
                    } else {
                        this.mWindows.remove(i);
                    }
                }
            }
        }
    }

    private final class DialogMenuCallback implements MenuBuilder.Callback, MenuPresenter.Callback {
        private int mFeatureId;
        private MenuDialogHelper mSubMenuHelper;

        public DialogMenuCallback(int featureId) {
            this.mFeatureId = featureId;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (menu.getRootMenu() != menu) {
                onCloseSubMenu(menu);
            }
            if (allMenusAreClosing) {
                Window.Callback callback = PhoneWindow.this.getCallback();
                if (callback != null && !PhoneWindow.this.isDestroyed()) {
                    callback.onPanelClosed(this.mFeatureId, menu);
                }
                if (menu == PhoneWindow.this.mContextMenu) {
                    PhoneWindow.this.dismissContextMenu();
                }
                if (this.mSubMenuHelper != null) {
                    this.mSubMenuHelper.dismiss();
                    this.mSubMenuHelper = null;
                }
            }
        }

        public void onCloseSubMenu(MenuBuilder menu) {
            Window.Callback callback = PhoneWindow.this.getCallback();
            if (callback != null && !PhoneWindow.this.isDestroyed()) {
                callback.onPanelClosed(this.mFeatureId, menu.getRootMenu());
            }
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            Window.Callback callback = PhoneWindow.this.getCallback();
            if (callback == null || PhoneWindow.this.isDestroyed() || !callback.onMenuItemSelected(this.mFeatureId, item)) {
                return PhoneWindow.SWEEP_OPEN_MENU;
            }
            return true;
        }

        public void onMenuModeChange(MenuBuilder menu) {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            if (subMenu == null) {
                return PhoneWindow.SWEEP_OPEN_MENU;
            }
            subMenu.setCallback(this);
            this.mSubMenuHelper = new MenuDialogHelper(subMenu);
            this.mSubMenuHelper.show((IBinder) null);
            return true;
        }
    }

    private static class ColorViewState {
        final int hideWindowFlag;
        final int id;
        final int systemUiHideFlag;
        final String transitionName;
        final int translucentFlag;
        final int verticalGravity;
        View view = null;
        int targetVisibility = PhoneWindow.FLAG_RESOURCE_SET_ICON_FALLBACK;

        ColorViewState(int systemUiHideFlag, int translucentFlag, int verticalGravity, String transitionName, int id, int hideWindowFlag) {
            this.id = id;
            this.systemUiHideFlag = systemUiHideFlag;
            this.translucentFlag = translucentFlag;
            this.verticalGravity = verticalGravity;
            this.transitionName = transitionName;
            this.hideWindowFlag = hideWindowFlag;
        }
    }

    void sendCloseSystemWindows() {
        PhoneWindowManager.sendCloseSystemWindows(getContext(), null);
    }

    void sendCloseSystemWindows(String reason) {
        PhoneWindowManager.sendCloseSystemWindows(getContext(), reason);
    }

    @Override
    public int getStatusBarColor() {
        return this.mStatusBarColor;
    }

    @Override
    public void setStatusBarColor(int color) {
        this.mStatusBarColor = color;
        this.mForcedStatusBarColor = true;
        if (this.mDecor != null) {
            this.mDecor.updateColorViews(null, SWEEP_OPEN_MENU);
        }
    }

    @Override
    public int getNavigationBarColor() {
        return this.mNavigationBarColor;
    }

    @Override
    public void setNavigationBarColor(int color) {
        this.mNavigationBarColor = color;
        this.mForcedNavigationBarColor = true;
        if (this.mDecor != null) {
            this.mDecor.updateColorViews(null, SWEEP_OPEN_MENU);
        }
    }
}
