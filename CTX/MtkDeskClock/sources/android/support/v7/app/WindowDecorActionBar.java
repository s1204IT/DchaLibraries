package android.support.v7.app;

import android.R;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.ViewPropertyAnimatorUpdateListener;
import android.support.v7.app.ActionBar;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.view.ActionBarPolicy;
import android.support.v7.view.ActionMode;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.ViewPropertyAnimatorCompatSet;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.view.menu.SubMenuBuilder;
import android.support.v7.widget.ActionBarContainer;
import android.support.v7.widget.ActionBarContextView;
import android.support.v7.widget.ActionBarOverlayLayout;
import android.support.v7.widget.DecorToolbar;
import android.support.v7.widget.ScrollingTabContainerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.SpinnerAdapter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class WindowDecorActionBar extends ActionBar implements ActionBarOverlayLayout.ActionBarVisibilityCallback {
    static final boolean $assertionsDisabled = false;
    private static final long FADE_IN_DURATION_MS = 200;
    private static final long FADE_OUT_DURATION_MS = 100;
    private static final int INVALID_POSITION = -1;
    private static final String TAG = "WindowDecorActionBar";
    private static final Interpolator sHideInterpolator = new AccelerateInterpolator();
    private static final Interpolator sShowInterpolator = new DecelerateInterpolator();
    ActionModeImpl mActionMode;
    private Activity mActivity;
    ActionBarContainer mContainerView;
    View mContentView;
    Context mContext;
    ActionBarContextView mContextView;
    ViewPropertyAnimatorCompatSet mCurrentShowAnim;
    DecorToolbar mDecorToolbar;
    ActionMode mDeferredDestroyActionMode;
    ActionMode.Callback mDeferredModeDestroyCallback;
    private Dialog mDialog;
    private boolean mDisplayHomeAsUpSet;
    private boolean mHasEmbeddedTabs;
    boolean mHiddenByApp;
    boolean mHiddenBySystem;
    boolean mHideOnContentScroll;
    private boolean mLastMenuVisibility;
    ActionBarOverlayLayout mOverlayLayout;
    private TabImpl mSelectedTab;
    private boolean mShowHideAnimationEnabled;
    private boolean mShowingForMode;
    ScrollingTabContainerView mTabScrollView;
    private Context mThemedContext;
    private ArrayList<TabImpl> mTabs = new ArrayList<>();
    private int mSavedTabPosition = -1;
    private ArrayList<ActionBar.OnMenuVisibilityListener> mMenuVisibilityListeners = new ArrayList<>();
    private int mCurWindowVisibility = 0;
    boolean mContentAnimations = true;
    private boolean mNowShowing = true;
    final ViewPropertyAnimatorListener mHideListener = new ViewPropertyAnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(View view) {
            if (WindowDecorActionBar.this.mContentAnimations && WindowDecorActionBar.this.mContentView != null) {
                WindowDecorActionBar.this.mContentView.setTranslationY(0.0f);
                WindowDecorActionBar.this.mContainerView.setTranslationY(0.0f);
            }
            WindowDecorActionBar.this.mContainerView.setVisibility(8);
            WindowDecorActionBar.this.mContainerView.setTransitioning(false);
            WindowDecorActionBar.this.mCurrentShowAnim = null;
            WindowDecorActionBar.this.completeDeferredDestroyActionMode();
            if (WindowDecorActionBar.this.mOverlayLayout != null) {
                ViewCompat.requestApplyInsets(WindowDecorActionBar.this.mOverlayLayout);
            }
        }
    };
    final ViewPropertyAnimatorListener mShowListener = new ViewPropertyAnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(View view) {
            WindowDecorActionBar.this.mCurrentShowAnim = null;
            WindowDecorActionBar.this.mContainerView.requestLayout();
        }
    };
    final ViewPropertyAnimatorUpdateListener mUpdateListener = new ViewPropertyAnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(View view) {
            ((View) WindowDecorActionBar.this.mContainerView.getParent()).invalidate();
        }
    };

    public WindowDecorActionBar(Activity activity, boolean overlayMode) {
        this.mActivity = activity;
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        init(decor);
        if (!overlayMode) {
            this.mContentView = decor.findViewById(R.id.content);
        }
    }

    public WindowDecorActionBar(Dialog dialog) {
        this.mDialog = dialog;
        init(dialog.getWindow().getDecorView());
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public WindowDecorActionBar(View layout) {
        init(layout);
    }

    private void init(View decor) {
        this.mOverlayLayout = (ActionBarOverlayLayout) decor.findViewById(android.support.v7.appcompat.R.id.decor_content_parent);
        if (this.mOverlayLayout != null) {
            this.mOverlayLayout.setActionBarVisibilityCallback(this);
        }
        this.mDecorToolbar = getDecorToolbar(decor.findViewById(android.support.v7.appcompat.R.id.action_bar));
        this.mContextView = (ActionBarContextView) decor.findViewById(android.support.v7.appcompat.R.id.action_context_bar);
        this.mContainerView = (ActionBarContainer) decor.findViewById(android.support.v7.appcompat.R.id.action_bar_container);
        if (this.mDecorToolbar == null || this.mContextView == null || this.mContainerView == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with a compatible window decor layout");
        }
        this.mContext = this.mDecorToolbar.getContext();
        int current = this.mDecorToolbar.getDisplayOptions();
        boolean homeAsUp = (current & 4) != 0;
        if (homeAsUp) {
            this.mDisplayHomeAsUpSet = true;
        }
        ActionBarPolicy abp = ActionBarPolicy.get(this.mContext);
        setHomeButtonEnabled(abp.enableHomeButtonByDefault() || homeAsUp);
        setHasEmbeddedTabs(abp.hasEmbeddedTabs());
        TypedArray a = this.mContext.obtainStyledAttributes(null, android.support.v7.appcompat.R.styleable.ActionBar, android.support.v7.appcompat.R.attr.actionBarStyle, 0);
        if (a.getBoolean(android.support.v7.appcompat.R.styleable.ActionBar_hideOnContentScroll, false)) {
            setHideOnContentScrollEnabled(true);
        }
        int elevation = a.getDimensionPixelSize(android.support.v7.appcompat.R.styleable.ActionBar_elevation, 0);
        if (elevation != 0) {
            setElevation(elevation);
        }
        a.recycle();
    }

    private DecorToolbar getDecorToolbar(View view) {
        if (view instanceof DecorToolbar) {
            return (DecorToolbar) view;
        }
        if (view instanceof Toolbar) {
            return ((Toolbar) view).getWrapper();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Can't make a decor toolbar out of ");
        sb.append(view != 0 ? view.getClass().getSimpleName() : "null");
        throw new IllegalStateException(sb.toString());
    }

    @Override
    public void setElevation(float elevation) {
        ViewCompat.setElevation(this.mContainerView, elevation);
    }

    @Override
    public float getElevation() {
        return ViewCompat.getElevation(this.mContainerView);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setHasEmbeddedTabs(ActionBarPolicy.get(this.mContext).hasEmbeddedTabs());
    }

    private void setHasEmbeddedTabs(boolean hasEmbeddedTabs) {
        this.mHasEmbeddedTabs = hasEmbeddedTabs;
        if (!this.mHasEmbeddedTabs) {
            this.mDecorToolbar.setEmbeddedTabView(null);
            this.mContainerView.setTabContainer(this.mTabScrollView);
        } else {
            this.mContainerView.setTabContainer(null);
            this.mDecorToolbar.setEmbeddedTabView(this.mTabScrollView);
        }
        boolean isInTabMode = getNavigationMode() == 2;
        if (this.mTabScrollView != null) {
            if (isInTabMode) {
                this.mTabScrollView.setVisibility(0);
                if (this.mOverlayLayout != null) {
                    ViewCompat.requestApplyInsets(this.mOverlayLayout);
                }
            } else {
                this.mTabScrollView.setVisibility(8);
            }
        }
        this.mDecorToolbar.setCollapsible(!this.mHasEmbeddedTabs && isInTabMode);
        this.mOverlayLayout.setHasNonEmbeddedTabs(!this.mHasEmbeddedTabs && isInTabMode);
    }

    private void ensureTabsExist() {
        if (this.mTabScrollView != null) {
            return;
        }
        ScrollingTabContainerView tabScroller = new ScrollingTabContainerView(this.mContext);
        if (this.mHasEmbeddedTabs) {
            tabScroller.setVisibility(0);
            this.mDecorToolbar.setEmbeddedTabView(tabScroller);
        } else {
            if (getNavigationMode() == 2) {
                tabScroller.setVisibility(0);
                if (this.mOverlayLayout != null) {
                    ViewCompat.requestApplyInsets(this.mOverlayLayout);
                }
            } else {
                tabScroller.setVisibility(8);
            }
            this.mContainerView.setTabContainer(tabScroller);
        }
        this.mTabScrollView = tabScroller;
    }

    void completeDeferredDestroyActionMode() {
        if (this.mDeferredModeDestroyCallback != null) {
            this.mDeferredModeDestroyCallback.onDestroyActionMode(this.mDeferredDestroyActionMode);
            this.mDeferredDestroyActionMode = null;
            this.mDeferredModeDestroyCallback = null;
        }
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        this.mCurWindowVisibility = visibility;
    }

    @Override
    public void setShowHideAnimationEnabled(boolean enabled) {
        this.mShowHideAnimationEnabled = enabled;
        if (!enabled && this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.cancel();
        }
    }

    @Override
    public void addOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener listener) {
        this.mMenuVisibilityListeners.add(listener);
    }

    @Override
    public void removeOnMenuVisibilityListener(ActionBar.OnMenuVisibilityListener listener) {
        this.mMenuVisibilityListeners.remove(listener);
    }

    @Override
    public void dispatchMenuVisibilityChanged(boolean isVisible) {
        if (isVisible == this.mLastMenuVisibility) {
            return;
        }
        this.mLastMenuVisibility = isVisible;
        int count = this.mMenuVisibilityListeners.size();
        for (int i = 0; i < count; i++) {
            this.mMenuVisibilityListeners.get(i).onMenuVisibilityChanged(isVisible);
        }
    }

    @Override
    public void setCustomView(int resId) {
        setCustomView(LayoutInflater.from(getThemedContext()).inflate(resId, this.mDecorToolbar.getViewGroup(), false));
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean z) {
        setDisplayOptions(z ? 1 : 0, 1);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome) {
        setDisplayOptions(showHome ? 2 : 0, 2);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        setDisplayOptions(showHomeAsUp ? 4 : 0, 4);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle) {
        setDisplayOptions(showTitle ? 8 : 0, 8);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom) {
        setDisplayOptions(showCustom ? 16 : 0, 16);
    }

    @Override
    public void setHomeButtonEnabled(boolean enable) {
        this.mDecorToolbar.setHomeButtonEnabled(enable);
    }

    @Override
    public void setTitle(int resId) {
        setTitle(this.mContext.getString(resId));
    }

    @Override
    public void setSubtitle(int resId) {
        setSubtitle(this.mContext.getString(resId));
    }

    @Override
    public void setSelectedNavigationItem(int position) {
        switch (this.mDecorToolbar.getNavigationMode()) {
            case 1:
                this.mDecorToolbar.setDropdownSelectedPosition(position);
                return;
            case 2:
                selectTab(this.mTabs.get(position));
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
    public void setTitle(CharSequence title) {
        this.mDecorToolbar.setTitle(title);
    }

    @Override
    public void setWindowTitle(CharSequence title) {
        this.mDecorToolbar.setWindowTitle(title);
    }

    @Override
    public boolean requestFocus() {
        ViewGroup viewGroup = this.mDecorToolbar.getViewGroup();
        if (viewGroup != null && !viewGroup.hasFocus()) {
            viewGroup.requestFocus();
            return true;
        }
        return false;
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        this.mDecorToolbar.setSubtitle(subtitle);
    }

    @Override
    public void setDisplayOptions(int options) {
        if ((options & 4) != 0) {
            this.mDisplayHomeAsUpSet = true;
        }
        this.mDecorToolbar.setDisplayOptions(options);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        int current = this.mDecorToolbar.getDisplayOptions();
        if ((mask & 4) != 0) {
            this.mDisplayHomeAsUpSet = true;
        }
        this.mDecorToolbar.setDisplayOptions((options & mask) | ((~mask) & current));
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        this.mContainerView.setPrimaryBackground(d);
    }

    @Override
    public void setStackedBackgroundDrawable(Drawable d) {
        this.mContainerView.setStackedBackground(d);
    }

    @Override
    public void setSplitBackgroundDrawable(Drawable d) {
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
        ActionModeImpl mode = new ActionModeImpl(this.mContextView.getContext(), callback);
        if (mode.dispatchOnCreate()) {
            this.mActionMode = mode;
            mode.invalidate();
            this.mContextView.initForMode(mode);
            animateToMode(true);
            this.mContextView.sendAccessibilityEvent(32);
            return mode;
        }
        return null;
    }

    private void configureTab(ActionBar.Tab tab, int position) {
        TabImpl tabi = (TabImpl) tab;
        ActionBar.TabListener callback = tabi.getCallback();
        if (callback == null) {
            throw new IllegalStateException("Action Bar Tab must have a Callback");
        }
        tabi.setPosition(position);
        this.mTabs.add(position, tabi);
        int count = this.mTabs.size();
        for (int i = position + 1; i < count; i++) {
            this.mTabs.get(i).setPosition(i);
        }
    }

    @Override
    public void addTab(ActionBar.Tab tab) {
        addTab(tab, this.mTabs.isEmpty());
    }

    @Override
    public void addTab(ActionBar.Tab tab, int position) {
        addTab(tab, position, this.mTabs.isEmpty());
    }

    @Override
    public void addTab(ActionBar.Tab tab, boolean setSelected) {
        ensureTabsExist();
        this.mTabScrollView.addTab(tab, setSelected);
        configureTab(tab, this.mTabs.size());
        if (setSelected) {
            selectTab(tab);
        }
    }

    @Override
    public void addTab(ActionBar.Tab tab, int position, boolean setSelected) {
        ensureTabsExist();
        this.mTabScrollView.addTab(tab, position, setSelected);
        configureTab(tab, position);
        if (setSelected) {
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
    public void removeTabAt(int position) {
        if (this.mTabScrollView == null) {
            return;
        }
        int selectedTabPosition = this.mSelectedTab != null ? this.mSelectedTab.getPosition() : this.mSavedTabPosition;
        this.mTabScrollView.removeTabAt(position);
        TabImpl removedTab = this.mTabs.remove(position);
        if (removedTab != null) {
            removedTab.setPosition(-1);
        }
        int newTabCount = this.mTabs.size();
        for (int i = position; i < newTabCount; i++) {
            this.mTabs.get(i).setPosition(i);
        }
        if (selectedTabPosition == position) {
            selectTab(this.mTabs.isEmpty() ? null : this.mTabs.get(Math.max(0, position - 1)));
        }
    }

    @Override
    public void selectTab(ActionBar.Tab tab) {
        FragmentTransaction trans;
        if (getNavigationMode() != 2) {
            this.mSavedTabPosition = tab != null ? tab.getPosition() : -1;
            return;
        }
        if ((this.mActivity instanceof FragmentActivity) && !this.mDecorToolbar.getViewGroup().isInEditMode()) {
            trans = ((FragmentActivity) this.mActivity).getSupportFragmentManager().beginTransaction().disallowAddToBackStack();
        } else {
            trans = null;
        }
        if (this.mSelectedTab == tab) {
            if (this.mSelectedTab != null) {
                this.mSelectedTab.getCallback().onTabReselected(this.mSelectedTab, trans);
                this.mTabScrollView.animateToTab(tab.getPosition());
            }
        } else {
            this.mTabScrollView.setTabSelected(tab != null ? tab.getPosition() : -1);
            if (this.mSelectedTab != null) {
                this.mSelectedTab.getCallback().onTabUnselected(this.mSelectedTab, trans);
            }
            this.mSelectedTab = (TabImpl) tab;
            if (this.mSelectedTab != null) {
                this.mSelectedTab.getCallback().onTabSelected(this.mSelectedTab, trans);
            }
        }
        if (trans != null && !trans.isEmpty()) {
            trans.commit();
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
    public void enableContentAnimations(boolean enabled) {
        this.mContentAnimations = enabled;
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
    public void setHideOnContentScrollEnabled(boolean hideOnContentScroll) {
        if (hideOnContentScroll && !this.mOverlayLayout.isInOverlayMode()) {
            throw new IllegalStateException("Action bar must be in overlay mode (Window.FEATURE_OVERLAY_ACTION_BAR) to enable hide on content scroll");
        }
        this.mHideOnContentScroll = hideOnContentScroll;
        this.mOverlayLayout.setHideOnContentScrollEnabled(hideOnContentScroll);
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
    public void setHideOffset(int offset) {
        if (offset != 0 && !this.mOverlayLayout.isInOverlayMode()) {
            throw new IllegalStateException("Action bar must be in overlay mode (Window.FEATURE_OVERLAY_ACTION_BAR) to set a non-zero hide offset");
        }
        this.mOverlayLayout.setActionBarHideOffset(offset);
    }

    static boolean checkShowingFlags(boolean hiddenByApp, boolean hiddenBySystem, boolean showingForMode) {
        if (showingForMode) {
            return true;
        }
        if (!hiddenByApp && !hiddenBySystem) {
            return true;
        }
        return false;
    }

    private void updateVisibility(boolean fromSystem) {
        boolean shown = checkShowingFlags(this.mHiddenByApp, this.mHiddenBySystem, this.mShowingForMode);
        if (shown) {
            if (!this.mNowShowing) {
                this.mNowShowing = true;
                doShow(fromSystem);
                return;
            }
            return;
        }
        if (this.mNowShowing) {
            this.mNowShowing = false;
            doHide(fromSystem);
        }
    }

    public void doShow(boolean fromSystem) {
        if (this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.cancel();
        }
        this.mContainerView.setVisibility(0);
        if (this.mCurWindowVisibility == 0 && (this.mShowHideAnimationEnabled || fromSystem)) {
            this.mContainerView.setTranslationY(0.0f);
            float startingY = -this.mContainerView.getHeight();
            if (fromSystem) {
                int[] topLeft = {0, 0};
                this.mContainerView.getLocationInWindow(topLeft);
                startingY -= topLeft[1];
            }
            this.mContainerView.setTranslationY(startingY);
            ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
            ViewPropertyAnimatorCompat a = ViewCompat.animate(this.mContainerView).translationY(0.0f);
            a.setUpdateListener(this.mUpdateListener);
            anim.play(a);
            if (this.mContentAnimations && this.mContentView != null) {
                this.mContentView.setTranslationY(startingY);
                anim.play(ViewCompat.animate(this.mContentView).translationY(0.0f));
            }
            anim.setInterpolator(sShowInterpolator);
            anim.setDuration(250L);
            anim.setListener(this.mShowListener);
            this.mCurrentShowAnim = anim;
            anim.start();
        } else {
            this.mContainerView.setAlpha(1.0f);
            this.mContainerView.setTranslationY(0.0f);
            if (this.mContentAnimations && this.mContentView != null) {
                this.mContentView.setTranslationY(0.0f);
            }
            this.mShowListener.onAnimationEnd(null);
        }
        if (this.mOverlayLayout != null) {
            ViewCompat.requestApplyInsets(this.mOverlayLayout);
        }
    }

    public void doHide(boolean fromSystem) {
        if (this.mCurrentShowAnim != null) {
            this.mCurrentShowAnim.cancel();
        }
        if (this.mCurWindowVisibility == 0 && (this.mShowHideAnimationEnabled || fromSystem)) {
            this.mContainerView.setAlpha(1.0f);
            this.mContainerView.setTransitioning(true);
            ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
            float endingY = -this.mContainerView.getHeight();
            if (fromSystem) {
                int[] topLeft = {0, 0};
                this.mContainerView.getLocationInWindow(topLeft);
                endingY -= topLeft[1];
            }
            ViewPropertyAnimatorCompat a = ViewCompat.animate(this.mContainerView).translationY(endingY);
            a.setUpdateListener(this.mUpdateListener);
            anim.play(a);
            if (this.mContentAnimations && this.mContentView != null) {
                anim.play(ViewCompat.animate(this.mContentView).translationY(endingY));
            }
            anim.setInterpolator(sHideInterpolator);
            anim.setDuration(250L);
            anim.setListener(this.mHideListener);
            this.mCurrentShowAnim = anim;
            anim.start();
            return;
        }
        this.mHideListener.onAnimationEnd(null);
    }

    @Override
    public boolean isShowing() {
        int height = getHeight();
        return this.mNowShowing && (height == 0 || getHideOffset() < height);
    }

    public void animateToMode(boolean toActionMode) {
        ViewPropertyAnimatorCompat fadeIn;
        ViewPropertyAnimatorCompat fadeIn2;
        if (toActionMode) {
            showForActionMode();
        } else {
            hideForActionMode();
        }
        if (shouldAnimateContextView()) {
            if (!toActionMode) {
                ViewPropertyAnimatorCompat fadeIn3 = this.mDecorToolbar.setupAnimatorToVisibility(0, FADE_IN_DURATION_MS);
                fadeIn = fadeIn3;
                fadeIn2 = this.mContextView.setupAnimatorToVisibility(8, FADE_OUT_DURATION_MS);
            } else {
                fadeIn2 = this.mDecorToolbar.setupAnimatorToVisibility(4, FADE_OUT_DURATION_MS);
                fadeIn = this.mContextView.setupAnimatorToVisibility(0, FADE_IN_DURATION_MS);
            }
            ViewPropertyAnimatorCompatSet set = new ViewPropertyAnimatorCompatSet();
            set.playSequentially(fadeIn2, fadeIn);
            set.start();
            return;
        }
        if (toActionMode) {
            this.mDecorToolbar.setVisibility(4);
            this.mContextView.setVisibility(0);
        } else {
            this.mDecorToolbar.setVisibility(0);
            this.mContextView.setVisibility(8);
        }
    }

    private boolean shouldAnimateContextView() {
        return ViewCompat.isLaidOut(this.mContainerView);
    }

    @Override
    public Context getThemedContext() {
        if (this.mThemedContext == null) {
            TypedValue outValue = new TypedValue();
            Resources.Theme currentTheme = this.mContext.getTheme();
            currentTheme.resolveAttribute(android.support.v7.appcompat.R.attr.actionBarWidgetTheme, outValue, true);
            int targetThemeRes = outValue.resourceId;
            if (targetThemeRes != 0) {
                this.mThemedContext = new ContextThemeWrapper(this.mContext, targetThemeRes);
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
    public void setHomeAsUpIndicator(Drawable indicator) {
        this.mDecorToolbar.setNavigationIcon(indicator);
    }

    @Override
    public void setHomeAsUpIndicator(int resId) {
        this.mDecorToolbar.setNavigationIcon(resId);
    }

    @Override
    public void setHomeActionContentDescription(CharSequence description) {
        this.mDecorToolbar.setNavigationContentDescription(description);
    }

    @Override
    public void setHomeActionContentDescription(int resId) {
        this.mDecorToolbar.setNavigationContentDescription(resId);
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

    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
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
            return new SupportMenuInflater(this.mActionModeContext);
        }

        @Override
        public Menu getMenu() {
            return this.mMenu;
        }

        @Override
        public void finish() {
            if (WindowDecorActionBar.this.mActionMode != this) {
                return;
            }
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
        public void setSubtitle(CharSequence subtitle) {
            WindowDecorActionBar.this.mContextView.setSubtitle(subtitle);
        }

        @Override
        public void setTitle(CharSequence title) {
            WindowDecorActionBar.this.mContextView.setTitle(title);
        }

        @Override
        public void setTitle(int resId) {
            setTitle(WindowDecorActionBar.this.mContext.getResources().getString(resId));
        }

        @Override
        public void setSubtitle(int resId) {
            setSubtitle(WindowDecorActionBar.this.mContext.getResources().getString(resId));
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
        public void setTitleOptionalHint(boolean titleOptional) {
            super.setTitleOptionalHint(titleOptional);
            WindowDecorActionBar.this.mContextView.setTitleOptional(titleOptional);
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
        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            if (this.mCallback != null) {
                return this.mCallback.onActionItemClicked(this, item);
            }
            return false;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            if (this.mCallback == null) {
                return false;
            }
            if (!subMenu.hasVisibleItems()) {
                return true;
            }
            new MenuPopupHelper(WindowDecorActionBar.this.getThemedContext(), subMenu).show();
            return true;
        }

        public void onCloseSubMenu(SubMenuBuilder menu) {
        }

        @Override
        public void onMenuModeChange(MenuBuilder menu) {
            if (this.mCallback == null) {
                return;
            }
            invalidate();
            WindowDecorActionBar.this.mContextView.showOverflowMenu();
        }
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
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
        public ActionBar.Tab setTag(Object tag) {
            this.mTag = tag;
            return this;
        }

        public ActionBar.TabListener getCallback() {
            return this.mCallback;
        }

        @Override
        public ActionBar.Tab setTabListener(ActionBar.TabListener callback) {
            this.mCallback = callback;
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
        public ActionBar.Tab setCustomView(int layoutResId) {
            return setCustomView(LayoutInflater.from(WindowDecorActionBar.this.getThemedContext()).inflate(layoutResId, (ViewGroup) null));
        }

        @Override
        public Drawable getIcon() {
            return this.mIcon;
        }

        @Override
        public int getPosition() {
            return this.mPosition;
        }

        public void setPosition(int position) {
            this.mPosition = position;
        }

        @Override
        public CharSequence getText() {
            return this.mText;
        }

        @Override
        public ActionBar.Tab setIcon(Drawable icon) {
            this.mIcon = icon;
            if (this.mPosition >= 0) {
                WindowDecorActionBar.this.mTabScrollView.updateTab(this.mPosition);
            }
            return this;
        }

        @Override
        public ActionBar.Tab setIcon(int resId) {
            return setIcon(AppCompatResources.getDrawable(WindowDecorActionBar.this.mContext, resId));
        }

        @Override
        public ActionBar.Tab setText(CharSequence text) {
            this.mText = text;
            if (this.mPosition >= 0) {
                WindowDecorActionBar.this.mTabScrollView.updateTab(this.mPosition);
            }
            return this;
        }

        @Override
        public ActionBar.Tab setText(int resId) {
            return setText(WindowDecorActionBar.this.mContext.getResources().getText(resId));
        }

        @Override
        public void select() {
            WindowDecorActionBar.this.selectTab(this);
        }

        @Override
        public ActionBar.Tab setContentDescription(int resId) {
            return setContentDescription(WindowDecorActionBar.this.mContext.getResources().getText(resId));
        }

        @Override
        public ActionBar.Tab setContentDescription(CharSequence contentDesc) {
            this.mContentDesc = contentDesc;
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
    public void setListNavigationCallbacks(SpinnerAdapter adapter, ActionBar.OnNavigationListener callback) {
        this.mDecorToolbar.setDropdownParams(adapter, new NavItemSelectedListener(callback));
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
    public void setNavigationMode(int mode) {
        int oldMode = this.mDecorToolbar.getNavigationMode();
        if (oldMode == 2) {
            this.mSavedTabPosition = getSelectedNavigationIndex();
            selectTab(null);
            this.mTabScrollView.setVisibility(8);
        }
        if (oldMode != mode && !this.mHasEmbeddedTabs && this.mOverlayLayout != null) {
            ViewCompat.requestApplyInsets(this.mOverlayLayout);
        }
        this.mDecorToolbar.setNavigationMode(mode);
        boolean z = false;
        if (mode == 2) {
            ensureTabsExist();
            this.mTabScrollView.setVisibility(0);
            if (this.mSavedTabPosition != -1) {
                setSelectedNavigationItem(this.mSavedTabPosition);
                this.mSavedTabPosition = -1;
            }
        }
        this.mDecorToolbar.setCollapsible(mode == 2 && !this.mHasEmbeddedTabs);
        ActionBarOverlayLayout actionBarOverlayLayout = this.mOverlayLayout;
        if (mode == 2 && !this.mHasEmbeddedTabs) {
            z = true;
        }
        actionBarOverlayLayout.setHasNonEmbeddedTabs(z);
    }

    @Override
    public ActionBar.Tab getTabAt(int index) {
        return this.mTabs.get(index);
    }

    @Override
    public void setIcon(int resId) {
        this.mDecorToolbar.setIcon(resId);
    }

    @Override
    public void setIcon(Drawable icon) {
        this.mDecorToolbar.setIcon(icon);
    }

    public boolean hasIcon() {
        return this.mDecorToolbar.hasIcon();
    }

    @Override
    public void setLogo(int resId) {
        this.mDecorToolbar.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        this.mDecorToolbar.setLogo(logo);
    }

    public boolean hasLogo() {
        return this.mDecorToolbar.hasLogo();
    }

    @Override
    public void setDefaultDisplayHomeAsUpEnabled(boolean enable) {
        if (!this.mDisplayHomeAsUpSet) {
            setDisplayHomeAsUpEnabled(enable);
        }
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        Menu menu;
        if (this.mActionMode == null || (menu = this.mActionMode.getMenu()) == null) {
            return false;
        }
        KeyCharacterMap kmap = KeyCharacterMap.load(event != null ? event.getDeviceId() : -1);
        menu.setQwertyMode(kmap.getKeyboardType() != 1);
        return menu.performShortcut(keyCode, event, 0);
    }
}
