package com.android.internal.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.SpinnerAdapter;
import android.widget.Toolbar;
import com.android.internal.R;
import com.android.internal.view.ActionBarPolicy;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPopupHelper;
import com.android.internal.view.menu.SubMenuBuilder;
import com.android.internal.widget.ActionBarContainer;
import com.android.internal.widget.ActionBarContextView;
import com.android.internal.widget.ActionBarOverlayLayout;
import com.android.internal.widget.DecorToolbar;
import com.android.internal.widget.ScrollingTabContainerView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class WindowDecorActionBar extends ActionBar implements ActionBarOverlayLayout.ActionBarVisibilityCallback {
    static final boolean $assertionsDisabled = false;
    private static final int CONTEXT_DISPLAY_NORMAL = 0;
    private static final int CONTEXT_DISPLAY_SPLIT = 1;
    private static final long FADE_IN_DURATION_MS = 200;
    private static final long FADE_OUT_DURATION_MS = 100;
    private static final int INVALID_POSITION = -1;
    private static final String TAG = "WindowDecorActionBar";
    ActionMode mActionMode;
    private Activity mActivity;
    private ActionBarContainer mContainerView;
    private View mContentView;
    private Context mContext;
    private int mContextDisplayMode;
    private ActionBarContextView mContextView;
    private Animator mCurrentShowAnim;
    private DecorToolbar mDecorToolbar;
    ActionMode mDeferredDestroyActionMode;
    ActionMode.Callback mDeferredModeDestroyCallback;
    private Dialog mDialog;
    private boolean mDisplayHomeAsUpSet;
    private boolean mHasEmbeddedTabs;
    private boolean mHiddenByApp;
    private boolean mHiddenBySystem;
    boolean mHideOnContentScroll;
    private boolean mLastMenuVisibility;
    private ActionBarOverlayLayout mOverlayLayout;
    private TabImpl mSelectedTab;
    private boolean mShowHideAnimationEnabled;
    private boolean mShowingForMode;
    private ActionBarContainer mSplitView;
    private ScrollingTabContainerView mTabScrollView;
    private Context mThemedContext;
    private ArrayList<TabImpl> mTabs = new ArrayList<>();
    private int mSavedTabPosition = -1;
    private ArrayList<ActionBar.OnMenuVisibilityListener> mMenuVisibilityListeners = new ArrayList<>();
    private int mCurWindowVisibility = 0;
    private boolean mContentAnimations = true;
    private boolean mNowShowing = true;
    final Animator.AnimatorListener mHideListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            if (WindowDecorActionBar.this.mContentAnimations && WindowDecorActionBar.this.mContentView != null) {
                WindowDecorActionBar.this.mContentView.setTranslationY(0.0f);
                WindowDecorActionBar.this.mContainerView.setTranslationY(0.0f);
            }
            if (WindowDecorActionBar.this.mSplitView != null && WindowDecorActionBar.this.mContextDisplayMode == 1) {
                WindowDecorActionBar.this.mSplitView.setVisibility(8);
            }
            WindowDecorActionBar.this.mContainerView.setVisibility(8);
            WindowDecorActionBar.this.mContainerView.setTransitioning(false);
            WindowDecorActionBar.this.mCurrentShowAnim = null;
            WindowDecorActionBar.this.completeDeferredDestroyActionMode();
            if (WindowDecorActionBar.this.mOverlayLayout != null) {
                WindowDecorActionBar.this.mOverlayLayout.requestApplyInsets();
            }
        }
    };
    final Animator.AnimatorListener mShowListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            WindowDecorActionBar.this.mCurrentShowAnim = null;
            WindowDecorActionBar.this.mContainerView.requestLayout();
        }
    };
    final ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            ((View) WindowDecorActionBar.this.mContainerView.getParent()).invalidate();
        }
    };

    public WindowDecorActionBar(Activity activity) {
        this.mActivity = activity;
        View decorView = activity.getWindow().getDecorView();
        boolean zHasFeature = this.mActivity.getWindow().hasFeature(9);
        init(decorView);
        if (!zHasFeature) {
            this.mContentView = decorView.findViewById(16908290);
        }
    }

    public WindowDecorActionBar(Dialog dialog) {
        this.mDialog = dialog;
        init(dialog.getWindow().getDecorView());
    }

    public WindowDecorActionBar(View view) {
        init(view);
    }

    private void init(View view) {
        this.mOverlayLayout = (ActionBarOverlayLayout) view.findViewById(R.id.decor_content_parent);
        if (this.mOverlayLayout != null) {
            this.mOverlayLayout.setActionBarVisibilityCallback(this);
        }
        this.mDecorToolbar = getDecorToolbar(view.findViewById(R.id.action_bar));
        this.mContextView = (ActionBarContextView) view.findViewById(R.id.action_context_bar);
        this.mContainerView = (ActionBarContainer) view.findViewById(R.id.action_bar_container);
        this.mSplitView = (ActionBarContainer) view.findViewById(R.id.split_action_bar);
        if (this.mDecorToolbar == null || this.mContextView == null || this.mContainerView == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with a compatible window decor layout");
        }
        this.mContext = this.mDecorToolbar.getContext();
        this.mContextDisplayMode = this.mDecorToolbar.isSplit() ? 1 : 0;
        boolean z = (this.mDecorToolbar.getDisplayOptions() & 4) != 0;
        if (z) {
            this.mDisplayHomeAsUpSet = true;
        }
        ActionBarPolicy actionBarPolicy = ActionBarPolicy.get(this.mContext);
        setHomeButtonEnabled(actionBarPolicy.enableHomeButtonByDefault() || z);
        setHasEmbeddedTabs(actionBarPolicy.hasEmbeddedTabs());
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.ActionBar, 16843470, 0);
        if (typedArrayObtainStyledAttributes.getBoolean(21, false)) {
            setHideOnContentScrollEnabled(true);
        }
        int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(20, 0);
        if (dimensionPixelSize != 0) {
            setElevation(dimensionPixelSize);
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    private DecorToolbar getDecorToolbar(View view) {
        if (view instanceof DecorToolbar) {
            return (DecorToolbar) view;
        }
        if (view instanceof Toolbar) {
            return ((Toolbar) view).getWrapper();
        }
        throw new IllegalStateException("Can't make a decor toolbar out of " + view.getClass().getSimpleName());
    }

    @Override
    public void setElevation(float f) {
        this.mContainerView.setElevation(f);
        if (this.mSplitView != null) {
            this.mSplitView.setElevation(f);
        }
    }

    @Override
    public float getElevation() {
        return this.mContainerView.getElevation();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        setHasEmbeddedTabs(ActionBarPolicy.get(this.mContext).hasEmbeddedTabs());
    }

    private void setHasEmbeddedTabs(boolean z) {
        this.mHasEmbeddedTabs = z;
        if (!this.mHasEmbeddedTabs) {
            this.mDecorToolbar.setEmbeddedTabView(null);
            this.mContainerView.setTabContainer(this.mTabScrollView);
        } else {
            this.mContainerView.setTabContainer(null);
            this.mDecorToolbar.setEmbeddedTabView(this.mTabScrollView);
        }
        boolean z2 = getNavigationMode() == 2;
        if (this.mTabScrollView != null) {
            if (z2) {
                this.mTabScrollView.setVisibility(0);
                if (this.mOverlayLayout != null) {
                    this.mOverlayLayout.requestApplyInsets();
                }
            } else {
                this.mTabScrollView.setVisibility(8);
            }
        }
        this.mDecorToolbar.setCollapsible(!this.mHasEmbeddedTabs && z2);
        this.mOverlayLayout.setHasNonEmbeddedTabs(!this.mHasEmbeddedTabs && z2);
    }

    private void ensureTabsExist() {
        if (this.mTabScrollView != null) {
            return;
        }
        ScrollingTabContainerView scrollingTabContainerView = new ScrollingTabContainerView(this.mContext);
        if (this.mHasEmbeddedTabs) {
            scrollingTabContainerView.setVisibility(0);
            this.mDecorToolbar.setEmbeddedTabView(scrollingTabContainerView);
        } else {
            if (getNavigationMode() == 2) {
                scrollingTabContainerView.setVisibility(0);
                if (this.mOverlayLayout != null) {
                    this.mOverlayLayout.requestApplyInsets();
                }
            } else {
                scrollingTabContainerView.setVisibility(8);
            }
            this.mContainerView.setTabContainer(scrollingTabContainerView);
        }
        this.mTabScrollView = scrollingTabContainerView;
    }

    void completeDeferredDestroyActionMode() {
        if (this.mDeferredModeDestroyCallback != null) {
            this.mDeferredModeDestroyCallback.onDestroyActionMode(this.mDeferredDestroyActionMode);
            this.mDeferredDestroyActionMode = null;
            this.mDeferredModeDestroyCallback = null;
        }
    }

    @Override
    public void onWindowVisibilityChanged(int i) {
        this.mCurWindowVisibility = i;
    }

    @Override
    public void setShowHideAnimationEnabled(boolean z) {
        this.mShowHideAnimationEnabled = z;
        if (!z && this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.end();
        }
    }

    @Override
    public void addOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener onMenuVisibilityListener) {
        this.mMenuVisibilityListeners.add(onMenuVisibilityListener);
    }

    @Override
    public void removeOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener onMenuVisibilityListener) {
        this.mMenuVisibilityListeners.remove(onMenuVisibilityListener);
    }

    @Override
    public void dispatchMenuVisibilityChanged(boolean z) {
        if (z == this.mLastMenuVisibility) {
            return;
        }
        this.mLastMenuVisibility = z;
        int size = this.mMenuVisibilityListeners.size();
        for (int i = 0; i < size; i++) {
            this.mMenuVisibilityListeners.get(i).onMenuVisibilityChanged(z);
        }
    }

    @Override
    public void setCustomView(int i) {
        setCustomView(LayoutInflater.from(getThemedContext()).inflate(i, this.mDecorToolbar.getViewGroup(), false));
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean z) {
        setDisplayOptions(z ? 1 : 0, 1);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean z) {
        setDisplayOptions(z ? 2 : 0, 2);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean z) {
        setDisplayOptions(z ? 4 : 0, 4);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean z) {
        setDisplayOptions(z ? 8 : 0, 8);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean z) {
        setDisplayOptions(z ? 16 : 0, 16);
    }

    @Override
    public void setHomeButtonEnabled(boolean z) {
        this.mDecorToolbar.setHomeButtonEnabled(z);
    }

    @Override
    public void setTitle(int i) {
        setTitle(this.mContext.getString(i));
    }

    @Override
    public void setSubtitle(int i) {
        setSubtitle(this.mContext.getString(i));
    }

    @Override
    public void setSelectedNavigationItem(int i) {
        switch (this.mDecorToolbar.getNavigationMode()) {
            case 1:
                this.mDecorToolbar.setDropdownSelectedPosition(i);
                return;
            case 2:
                selectTab(this.mTabs.get(i));
                return;
            default:
                throw new IllegalStateException("setSelectedNavigationIndex not valid for current navigation mode");
        }
    }

    @Override
    public void removeAllTabs() {
        cleanupTabs();
    }

    private void cleanupTabs() {
        if (this.mSelectedTab != null) {
            selectTab(null);
        }
        this.mTabs.clear();
        if (this.mTabScrollView != null) {
            this.mTabScrollView.removeAllTabs();
        }
        this.mSavedTabPosition = -1;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mDecorToolbar.setTitle(charSequence);
    }

    @Override
    public void setWindowTitle(CharSequence charSequence) {
        this.mDecorToolbar.setWindowTitle(charSequence);
    }

    @Override
    public void setSubtitle(CharSequence charSequence) {
        this.mDecorToolbar.setSubtitle(charSequence);
    }

    @Override
    public void setDisplayOptions(int i) {
        if ((i & 4) != 0) {
            this.mDisplayHomeAsUpSet = true;
        }
        this.mDecorToolbar.setDisplayOptions(i);
    }

    @Override
    public void setDisplayOptions(int i, int i2) {
        int displayOptions = this.mDecorToolbar.getDisplayOptions();
        if ((i2 & 4) != 0) {
            this.mDisplayHomeAsUpSet = true;
        }
        this.mDecorToolbar.setDisplayOptions((i & i2) | ((~i2) & displayOptions));
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        this.mContainerView.setPrimaryBackground(drawable);
    }

    @Override
    public void setStackedBackgroundDrawable(Drawable drawable) {
        this.mContainerView.setStackedBackground(drawable);
    }

    @Override
    public void setSplitBackgroundDrawable(Drawable drawable) {
        if (this.mSplitView != null) {
            this.mSplitView.setSplitBackground(drawable);
        }
    }

    @Override
    public View getCustomView() {
        return this.mDecorToolbar.getCustomView();
    }

    @Override
    public CharSequence getTitle() {
        return this.mDecorToolbar.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return this.mDecorToolbar.getSubtitle();
    }

    @Override
    public int getNavigationMode() {
        return this.mDecorToolbar.getNavigationMode();
    }

    @Override
    public int getDisplayOptions() {
        return this.mDecorToolbar.getDisplayOptions();
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
        this.mOverlayLayout.setHideOnContentScrollEnabled(false);
        this.mContextView.killMode();
        ActionModeImpl actionModeImpl = new ActionModeImpl(this.mContextView.getContext(), callback);
        if (actionModeImpl.dispatchOnCreate()) {
            this.mActionMode = actionModeImpl;
            actionModeImpl.invalidate();
            this.mContextView.initForMode(actionModeImpl);
            animateToMode(true);
            if (this.mSplitView != null && this.mContextDisplayMode == 1 && this.mSplitView.getVisibility() != 0) {
                this.mSplitView.setVisibility(0);
                if (this.mOverlayLayout != null) {
                    this.mOverlayLayout.requestApplyInsets();
                }
            }
            this.mContextView.sendAccessibilityEvent(32);
            return actionModeImpl;
        }
        return null;
    }

    private void configureTab(ActionBar.Tab tab, int i) {
        TabImpl tabImpl = (TabImpl) tab;
        if (tabImpl.getCallback() == null) {
            throw new IllegalStateException("Action Bar Tab must have a Callback");
        }
        tabImpl.setPosition(i);
        this.mTabs.add(i, tabImpl);
        int size = this.mTabs.size();
        while (true) {
            i++;
            if (i < size) {
                this.mTabs.get(i).setPosition(i);
            } else {
                return;
            }
        }
    }

    @Override
    public void addTab(ActionBar.Tab tab) {
        addTab(tab, this.mTabs.isEmpty());
    }

    @Override
    public void addTab(ActionBar.Tab tab, int i) {
        addTab(tab, i, this.mTabs.isEmpty());
    }

    @Override
    public void addTab(ActionBar.Tab tab, boolean z) {
        ensureTabsExist();
        this.mTabScrollView.addTab(tab, z);
        configureTab(tab, this.mTabs.size());
        if (z) {
            selectTab(tab);
        }
    }

    @Override
    public void addTab(ActionBar.Tab tab, int i, boolean z) {
        ensureTabsExist();
        this.mTabScrollView.addTab(tab, i, z);
        configureTab(tab, i);
        if (z) {
            selectTab(tab);
        }
    }

    @Override
    public ActionBar.Tab newTab() {
        return new TabImpl();
    }

    @Override
    public void removeTab(ActionBar.Tab tab) {
        removeTabAt(tab.getPosition());
    }

    @Override
    public void removeTabAt(int i) {
        if (this.mTabScrollView == null) {
            return;
        }
        int position = this.mSelectedTab != null ? this.mSelectedTab.getPosition() : this.mSavedTabPosition;
        this.mTabScrollView.removeTabAt(i);
        TabImpl tabImplRemove = this.mTabs.remove(i);
        if (tabImplRemove != null) {
            tabImplRemove.setPosition(-1);
        }
        int size = this.mTabs.size();
        for (int i2 = i; i2 < size; i2++) {
            this.mTabs.get(i2).setPosition(i2);
        }
        if (position == i) {
            selectTab(this.mTabs.isEmpty() ? null : this.mTabs.get(Math.max(0, i - 1)));
        }
    }

    @Override
    public void selectTab(ActionBar.Tab tab) {
        if (getNavigationMode() != 2) {
            this.mSavedTabPosition = tab != null ? tab.getPosition() : -1;
            return;
        }
        FragmentTransaction fragmentTransactionDisallowAddToBackStack = this.mDecorToolbar.getViewGroup().isInEditMode() ? null : this.mActivity.getFragmentManager().beginTransaction().disallowAddToBackStack();
        if (this.mSelectedTab == tab) {
            if (this.mSelectedTab != null) {
                this.mSelectedTab.getCallback().onTabReselected(this.mSelectedTab, fragmentTransactionDisallowAddToBackStack);
                this.mTabScrollView.animateToTab(tab.getPosition());
            }
        } else {
            this.mTabScrollView.setTabSelected(tab != null ? tab.getPosition() : -1);
            if (this.mSelectedTab != null) {
                this.mSelectedTab.getCallback().onTabUnselected(this.mSelectedTab, fragmentTransactionDisallowAddToBackStack);
            }
            this.mSelectedTab = (TabImpl) tab;
            if (this.mSelectedTab != null) {
                this.mSelectedTab.getCallback().onTabSelected(this.mSelectedTab, fragmentTransactionDisallowAddToBackStack);
            }
        }
        if (fragmentTransactionDisallowAddToBackStack != null && !fragmentTransactionDisallowAddToBackStack.isEmpty()) {
            fragmentTransactionDisallowAddToBackStack.commit();
        }
    }

    @Override
    public ActionBar.Tab getSelectedTab() {
        return this.mSelectedTab;
    }

    @Override
    public int getHeight() {
        return this.mContainerView.getHeight();
    }

    @Override
    public void enableContentAnimations(boolean z) {
        this.mContentAnimations = z;
    }

    @Override
    public void show() {
        if (this.mHiddenByApp) {
            this.mHiddenByApp = false;
            updateVisibility(false);
        }
    }

    private void showForActionMode() {
        if (!this.mShowingForMode) {
            this.mShowingForMode = true;
            if (this.mOverlayLayout != null) {
                this.mOverlayLayout.setShowingForActionMode(true);
            }
            updateVisibility(false);
        }
    }

    @Override
    public void showForSystem() {
        if (this.mHiddenBySystem) {
            this.mHiddenBySystem = false;
            updateVisibility(true);
        }
    }

    @Override
    public void hide() {
        if (!this.mHiddenByApp) {
            this.mHiddenByApp = true;
            updateVisibility(false);
        }
    }

    private void hideForActionMode() {
        if (this.mShowingForMode) {
            this.mShowingForMode = false;
            if (this.mOverlayLayout != null) {
                this.mOverlayLayout.setShowingForActionMode(false);
            }
            updateVisibility(false);
        }
    }

    @Override
    public void hideForSystem() {
        if (!this.mHiddenBySystem) {
            this.mHiddenBySystem = true;
            updateVisibility(true);
        }
    }

    @Override
    public void setHideOnContentScrollEnabled(boolean z) {
        if (z && !this.mOverlayLayout.isInOverlayMode()) {
            throw new IllegalStateException("Action bar must be in overlay mode (Window.FEATURE_OVERLAY_ACTION_BAR) to enable hide on content scroll");
        }
        this.mHideOnContentScroll = z;
        this.mOverlayLayout.setHideOnContentScrollEnabled(z);
    }

    @Override
    public boolean isHideOnContentScrollEnabled() {
        return this.mOverlayLayout.isHideOnContentScrollEnabled();
    }

    @Override
    public int getHideOffset() {
        return this.mOverlayLayout.getActionBarHideOffset();
    }

    @Override
    public void setHideOffset(int i) {
        if (i != 0 && !this.mOverlayLayout.isInOverlayMode()) {
            throw new IllegalStateException("Action bar must be in overlay mode (Window.FEATURE_OVERLAY_ACTION_BAR) to set a non-zero hide offset");
        }
        this.mOverlayLayout.setActionBarHideOffset(i);
    }

    private static boolean checkShowingFlags(boolean z, boolean z2, boolean z3) {
        if (z3) {
            return true;
        }
        if (!z && !z2) {
            return true;
        }
        return false;
    }

    private void updateVisibility(boolean z) {
        if (checkShowingFlags(this.mHiddenByApp, this.mHiddenBySystem, this.mShowingForMode)) {
            if (!this.mNowShowing) {
                this.mNowShowing = true;
                doShow(z);
                return;
            }
            return;
        }
        if (this.mNowShowing) {
            this.mNowShowing = false;
            doHide(z);
        }
    }

    public void doShow(boolean z) {
        if (this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.end();
        }
        this.mContainerView.setVisibility(0);
        if (this.mCurWindowVisibility == 0 && (this.mShowHideAnimationEnabled || z)) {
            this.mContainerView.setTranslationY(0.0f);
            float f = -this.mContainerView.getHeight();
            if (z) {
                this.mContainerView.getLocationInWindow(new int[]{0, 0});
                f -= r9[1];
            }
            this.mContainerView.setTranslationY(f);
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mContainerView, (Property<ActionBarContainer, Float>) View.TRANSLATION_Y, 0.0f);
            objectAnimatorOfFloat.addUpdateListener(this.mUpdateListener);
            AnimatorSet.Builder builderPlay = animatorSet.play(objectAnimatorOfFloat);
            if (this.mContentAnimations && this.mContentView != null) {
                builderPlay.with(ObjectAnimator.ofFloat(this.mContentView, View.TRANSLATION_Y, f, 0.0f));
            }
            if (this.mSplitView != null && this.mContextDisplayMode == 1) {
                this.mSplitView.setTranslationY(this.mSplitView.getHeight());
                this.mSplitView.setVisibility(0);
                builderPlay.with(ObjectAnimator.ofFloat(this.mSplitView, (Property<ActionBarContainer, Float>) View.TRANSLATION_Y, 0.0f));
            }
            animatorSet.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 17563651));
            animatorSet.setDuration(250L);
            animatorSet.addListener(this.mShowListener);
            this.mCurrentShowAnim = animatorSet;
            animatorSet.start();
        } else {
            this.mContainerView.setAlpha(1.0f);
            this.mContainerView.setTranslationY(0.0f);
            if (this.mContentAnimations && this.mContentView != null) {
                this.mContentView.setTranslationY(0.0f);
            }
            if (this.mSplitView != null && this.mContextDisplayMode == 1) {
                this.mSplitView.setAlpha(1.0f);
                this.mSplitView.setTranslationY(0.0f);
                this.mSplitView.setVisibility(0);
            }
            this.mShowListener.onAnimationEnd(null);
        }
        if (this.mOverlayLayout != null) {
            this.mOverlayLayout.requestApplyInsets();
        }
    }

    public void doHide(boolean z) {
        if (this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.end();
        }
        if (this.mCurWindowVisibility == 0 && (this.mShowHideAnimationEnabled || z)) {
            this.mContainerView.setAlpha(1.0f);
            this.mContainerView.setTransitioning(true);
            AnimatorSet animatorSet = new AnimatorSet();
            float f = -this.mContainerView.getHeight();
            if (z) {
                this.mContainerView.getLocationInWindow(new int[]{0, 0});
                f -= r10[1];
            }
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mContainerView, (Property<ActionBarContainer, Float>) View.TRANSLATION_Y, f);
            objectAnimatorOfFloat.addUpdateListener(this.mUpdateListener);
            AnimatorSet.Builder builderPlay = animatorSet.play(objectAnimatorOfFloat);
            if (this.mContentAnimations && this.mContentView != null) {
                builderPlay.with(ObjectAnimator.ofFloat(this.mContentView, View.TRANSLATION_Y, 0.0f, f));
            }
            if (this.mSplitView != null && this.mSplitView.getVisibility() == 0) {
                this.mSplitView.setAlpha(1.0f);
                builderPlay.with(ObjectAnimator.ofFloat(this.mSplitView, (Property<ActionBarContainer, Float>) View.TRANSLATION_Y, this.mSplitView.getHeight()));
            }
            animatorSet.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 17563650));
            animatorSet.setDuration(250L);
            animatorSet.addListener(this.mHideListener);
            this.mCurrentShowAnim = animatorSet;
            animatorSet.start();
            return;
        }
        this.mHideListener.onAnimationEnd(null);
    }

    @Override
    public boolean isShowing() {
        int height = getHeight();
        return this.mNowShowing && (height == 0 || getHideOffset() < height);
    }

    void animateToMode(boolean z) {
        Animator animator;
        Animator animator2;
        if (z) {
            showForActionMode();
        } else {
            hideForActionMode();
        }
        if (shouldAnimateContextView()) {
            if (!z) {
                animator = this.mDecorToolbar.setupAnimatorToVisibility(0, FADE_IN_DURATION_MS);
                animator2 = this.mContextView.setupAnimatorToVisibility(8, FADE_OUT_DURATION_MS);
            } else {
                animator2 = this.mDecorToolbar.setupAnimatorToVisibility(8, FADE_OUT_DURATION_MS);
                animator = this.mContextView.setupAnimatorToVisibility(0, FADE_IN_DURATION_MS);
            }
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(animator2, animator);
            animatorSet.start();
            return;
        }
        if (z) {
            this.mDecorToolbar.setVisibility(8);
            this.mContextView.setVisibility(0);
        } else {
            this.mDecorToolbar.setVisibility(0);
            this.mContextView.setVisibility(8);
        }
    }

    private boolean shouldAnimateContextView() {
        return this.mContainerView.isLaidOut();
    }

    @Override
    public Context getThemedContext() {
        if (this.mThemedContext == null) {
            TypedValue typedValue = new TypedValue();
            this.mContext.getTheme().resolveAttribute(16843671, typedValue, true);
            int i = typedValue.resourceId;
            if (i != 0 && this.mContext.getThemeResId() != i) {
                this.mThemedContext = new ContextThemeWrapper(this.mContext, i);
            } else {
                this.mThemedContext = this.mContext;
            }
        }
        return this.mThemedContext;
    }

    @Override
    public boolean isTitleTruncated() {
        return this.mDecorToolbar != null && this.mDecorToolbar.isTitleTruncated();
    }

    @Override
    public void setHomeAsUpIndicator(Drawable drawable) {
        this.mDecorToolbar.setNavigationIcon(drawable);
    }

    @Override
    public void setHomeAsUpIndicator(int i) {
        this.mDecorToolbar.setNavigationIcon(i);
    }

    @Override
    public void setHomeActionContentDescription(CharSequence charSequence) {
        this.mDecorToolbar.setNavigationContentDescription(charSequence);
    }

    @Override
    public void setHomeActionContentDescription(int i) {
        this.mDecorToolbar.setNavigationContentDescription(i);
    }

    @Override
    public void onContentScrollStarted() {
        if (this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.cancel();
            this.mCurrentShowAnim = null;
        }
    }

    @Override
    public void onContentScrollStopped() {
    }

    @Override
    public boolean collapseActionView() {
        if (this.mDecorToolbar != null && this.mDecorToolbar.hasExpandedActionView()) {
            this.mDecorToolbar.collapseActionView();
            return true;
        }
        return false;
    }

    public class ActionModeImpl extends ActionMode implements MenuBuilder.Callback {
        private final Context mActionModeContext;
        private ActionMode.Callback mCallback;
        private WeakReference<View> mCustomView;
        private final MenuBuilder mMenu;

        public ActionModeImpl(Context context, ActionMode.Callback callback) {
            this.mActionModeContext = context;
            this.mCallback = callback;
            this.mMenu = new MenuBuilder(context).setDefaultShowAsAction(1);
            this.mMenu.setCallback(this);
        }

        @Override
        public MenuInflater getMenuInflater() {
            return new MenuInflater(this.mActionModeContext);
        }

        @Override
        public Menu getMenu() {
            return this.mMenu;
        }

        @Override
        public void finish() {
            if (WindowDecorActionBar.this.mActionMode == this) {
                if (!WindowDecorActionBar.checkShowingFlags(WindowDecorActionBar.this.mHiddenByApp, WindowDecorActionBar.this.mHiddenBySystem, false)) {
                    WindowDecorActionBar.this.mDeferredDestroyActionMode = this;
                    WindowDecorActionBar.this.mDeferredModeDestroyCallback = this.mCallback;
                } else {
                    this.mCallback.onDestroyActionMode(this);
                }
                this.mCallback = null;
                WindowDecorActionBar.this.animateToMode(false);
                WindowDecorActionBar.this.mContextView.closeMode();
                WindowDecorActionBar.this.mDecorToolbar.getViewGroup().sendAccessibilityEvent(32);
                WindowDecorActionBar.this.mOverlayLayout.setHideOnContentScrollEnabled(WindowDecorActionBar.this.mHideOnContentScroll);
                WindowDecorActionBar.this.mActionMode = null;
            }
        }

        @Override
        public void invalidate() {
            if (WindowDecorActionBar.this.mActionMode != this) {
                return;
            }
            this.mMenu.stopDispatchingItemsChanged();
            try {
                this.mCallback.onPrepareActionMode(this, this.mMenu);
            } finally {
                this.mMenu.startDispatchingItemsChanged();
            }
        }

        public boolean dispatchOnCreate() {
            this.mMenu.stopDispatchingItemsChanged();
            try {
                return this.mCallback.onCreateActionMode(this, this.mMenu);
            } finally {
                this.mMenu.startDispatchingItemsChanged();
            }
        }

        @Override
        public void setCustomView(View view) {
            WindowDecorActionBar.this.mContextView.setCustomView(view);
            this.mCustomView = new WeakReference<>(view);
        }

        @Override
        public void setSubtitle(CharSequence charSequence) {
            WindowDecorActionBar.this.mContextView.setSubtitle(charSequence);
        }

        @Override
        public void setTitle(CharSequence charSequence) {
            WindowDecorActionBar.this.mContextView.setTitle(charSequence);
        }

        @Override
        public void setTitle(int i) {
            setTitle(WindowDecorActionBar.this.mContext.getResources().getString(i));
        }

        @Override
        public void setSubtitle(int i) {
            setSubtitle(WindowDecorActionBar.this.mContext.getResources().getString(i));
        }

        @Override
        public CharSequence getTitle() {
            return WindowDecorActionBar.this.mContextView.getTitle();
        }

        @Override
        public CharSequence getSubtitle() {
            return WindowDecorActionBar.this.mContextView.getSubtitle();
        }

        @Override
        public void setTitleOptionalHint(boolean z) {
            super.setTitleOptionalHint(z);
            WindowDecorActionBar.this.mContextView.setTitleOptional(z);
        }

        @Override
        public boolean isTitleOptional() {
            return WindowDecorActionBar.this.mContextView.isTitleOptional();
        }

        @Override
        public View getCustomView() {
            if (this.mCustomView != null) {
                return this.mCustomView.get();
            }
            return null;
        }

        @Override
        public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {
            if (this.mCallback != null) {
                return this.mCallback.onActionItemClicked(this, menuItem);
            }
            return false;
        }

        public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
            if (this.mCallback == null) {
                return false;
            }
            if (!subMenuBuilder.hasVisibleItems()) {
                return true;
            }
            new MenuPopupHelper(WindowDecorActionBar.this.getThemedContext(), subMenuBuilder).show();
            return true;
        }

        public void onCloseSubMenu(SubMenuBuilder subMenuBuilder) {
        }

        @Override
        public void onMenuModeChange(MenuBuilder menuBuilder) {
            if (this.mCallback == null) {
                return;
            }
            invalidate();
            WindowDecorActionBar.this.mContextView.showOverflowMenu();
        }
    }

    public class TabImpl extends ActionBar.Tab {
        private ActionBar.TabListener mCallback;
        private CharSequence mContentDesc;
        private View mCustomView;
        private Drawable mIcon;
        private int mPosition = -1;
        private Object mTag;
        private CharSequence mText;

        public TabImpl() {
        }

        @Override
        public Object getTag() {
            return this.mTag;
        }

        @Override
        public ActionBar.Tab setTag(Object obj) {
            this.mTag = obj;
            return this;
        }

        public ActionBar.TabListener getCallback() {
            return this.mCallback;
        }

        @Override
        public ActionBar.Tab setTabListener(ActionBar.TabListener tabListener) {
            this.mCallback = tabListener;
            return this;
        }

        @Override
        public View getCustomView() {
            return this.mCustomView;
        }

        @Override
        public ActionBar.Tab setCustomView(View view) {
            this.mCustomView = view;
            if (this.mPosition >= 0) {
                WindowDecorActionBar.this.mTabScrollView.updateTab(this.mPosition);
            }
            return this;
        }

        @Override
        public ActionBar.Tab setCustomView(int i) {
            return setCustomView(LayoutInflater.from(WindowDecorActionBar.this.getThemedContext()).inflate(i, (ViewGroup) null));
        }

        @Override
        public Drawable getIcon() {
            return this.mIcon;
        }

        @Override
        public int getPosition() {
            return this.mPosition;
        }

        public void setPosition(int i) {
            this.mPosition = i;
        }

        @Override
        public CharSequence getText() {
            return this.mText;
        }

        @Override
        public ActionBar.Tab setIcon(Drawable drawable) {
            this.mIcon = drawable;
            if (this.mPosition >= 0) {
                WindowDecorActionBar.this.mTabScrollView.updateTab(this.mPosition);
            }
            return this;
        }

        @Override
        public ActionBar.Tab setIcon(int i) {
            return setIcon(WindowDecorActionBar.this.mContext.getDrawable(i));
        }

        @Override
        public ActionBar.Tab setText(CharSequence charSequence) {
            this.mText = charSequence;
            if (this.mPosition >= 0) {
                WindowDecorActionBar.this.mTabScrollView.updateTab(this.mPosition);
            }
            return this;
        }

        @Override
        public ActionBar.Tab setText(int i) {
            return setText(WindowDecorActionBar.this.mContext.getResources().getText(i));
        }

        @Override
        public void select() {
            WindowDecorActionBar.this.selectTab(this);
        }

        @Override
        public ActionBar.Tab setContentDescription(int i) {
            return setContentDescription(WindowDecorActionBar.this.mContext.getResources().getText(i));
        }

        @Override
        public ActionBar.Tab setContentDescription(CharSequence charSequence) {
            this.mContentDesc = charSequence;
            if (this.mPosition >= 0) {
                WindowDecorActionBar.this.mTabScrollView.updateTab(this.mPosition);
            }
            return this;
        }

        @Override
        public CharSequence getContentDescription() {
            return this.mContentDesc;
        }
    }

    @Override
    public void setCustomView(View view) {
        this.mDecorToolbar.setCustomView(view);
    }

    @Override
    public void setCustomView(View view, ActionBar.LayoutParams layoutParams) {
        view.setLayoutParams(layoutParams);
        this.mDecorToolbar.setCustomView(view);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter spinnerAdapter, ActionBar.OnNavigationListener onNavigationListener) {
        this.mDecorToolbar.setDropdownParams(spinnerAdapter, new NavItemSelectedListener(onNavigationListener));
    }

    @Override
    public int getSelectedNavigationIndex() {
        switch (this.mDecorToolbar.getNavigationMode()) {
            case 1:
                return this.mDecorToolbar.getDropdownSelectedPosition();
            case 2:
                if (this.mSelectedTab != null) {
                    return this.mSelectedTab.getPosition();
                }
                return -1;
            default:
                return -1;
        }
    }

    @Override
    public int getNavigationItemCount() {
        switch (this.mDecorToolbar.getNavigationMode()) {
            case 1:
                return this.mDecorToolbar.getDropdownItemCount();
            case 2:
                return this.mTabs.size();
            default:
                return 0;
        }
    }

    @Override
    public int getTabCount() {
        return this.mTabs.size();
    }

    @Override
    public void setNavigationMode(int i) {
        int navigationMode = this.mDecorToolbar.getNavigationMode();
        if (navigationMode == 2) {
            this.mSavedTabPosition = getSelectedNavigationIndex();
            selectTab(null);
            this.mTabScrollView.setVisibility(8);
        }
        if (navigationMode != i && !this.mHasEmbeddedTabs && this.mOverlayLayout != null) {
            this.mOverlayLayout.requestFitSystemWindows();
        }
        this.mDecorToolbar.setNavigationMode(i);
        boolean z = false;
        if (i == 2) {
            ensureTabsExist();
            this.mTabScrollView.setVisibility(0);
            if (this.mSavedTabPosition != -1) {
                setSelectedNavigationItem(this.mSavedTabPosition);
                this.mSavedTabPosition = -1;
            }
        }
        this.mDecorToolbar.setCollapsible(i == 2 && !this.mHasEmbeddedTabs);
        ActionBarOverlayLayout actionBarOverlayLayout = this.mOverlayLayout;
        if (i == 2 && !this.mHasEmbeddedTabs) {
            z = true;
        }
        actionBarOverlayLayout.setHasNonEmbeddedTabs(z);
    }

    @Override
    public ActionBar.Tab getTabAt(int i) {
        return this.mTabs.get(i);
    }

    @Override
    public void setIcon(int i) {
        this.mDecorToolbar.setIcon(i);
    }

    @Override
    public void setIcon(Drawable drawable) {
        this.mDecorToolbar.setIcon(drawable);
    }

    public boolean hasIcon() {
        return this.mDecorToolbar.hasIcon();
    }

    @Override
    public void setLogo(int i) {
        this.mDecorToolbar.setLogo(i);
    }

    @Override
    public void setLogo(Drawable drawable) {
        this.mDecorToolbar.setLogo(drawable);
    }

    public boolean hasLogo() {
        return this.mDecorToolbar.hasLogo();
    }

    @Override
    public void setDefaultDisplayHomeAsUpEnabled(boolean z) {
        if (!this.mDisplayHomeAsUpSet) {
            setDisplayHomeAsUpEnabled(z);
        }
    }
}
