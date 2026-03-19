package com.android.internal.widget;

import android.animation.LayoutTransition;
import android.app.ActionBar;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.CollapsibleActionView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.view.menu.ActionMenuItem;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.view.menu.MenuView;
import com.android.internal.view.menu.SubMenuBuilder;

public class ActionBarView extends AbsActionBarView implements DecorToolbar {
    private static final int DEFAULT_CUSTOM_GRAVITY = 8388627;
    public static final int DISPLAY_DEFAULT = 0;
    private static final int DISPLAY_RELAYOUT_MASK = 63;
    private static final String TAG = "ActionBarView";
    private ActionBarContextView mContextView;
    private View mCustomNavView;
    private int mDefaultUpDescription;
    private int mDisplayOptions;
    View mExpandedActionView;
    private final View.OnClickListener mExpandedActionViewUpListener;
    private HomeView mExpandedHomeLayout;
    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;
    private CharSequence mHomeDescription;
    private int mHomeDescriptionRes;
    private HomeView mHomeLayout;
    private Drawable mIcon;
    private boolean mIncludeTabs;
    private final int mIndeterminateProgressStyle;
    private ProgressBar mIndeterminateProgressView;
    private boolean mIsCollapsible;
    private int mItemPadding;
    private LinearLayout mListNavLayout;
    private Drawable mLogo;
    private ActionMenuItem mLogoNavItem;
    private boolean mMenuPrepared;
    private AdapterView.OnItemSelectedListener mNavItemSelectedListener;
    private int mNavigationMode;
    private MenuBuilder mOptionsMenu;
    private int mProgressBarPadding;
    private final int mProgressStyle;
    private ProgressBar mProgressView;
    private Spinner mSpinner;
    private SpinnerAdapter mSpinnerAdapter;
    private CharSequence mSubtitle;
    private final int mSubtitleStyleRes;
    private TextView mSubtitleView;
    private ScrollingTabContainerView mTabScrollView;
    private Runnable mTabSelector;
    private CharSequence mTitle;
    private LinearLayout mTitleLayout;
    private final int mTitleStyleRes;
    private TextView mTitleView;
    private final View.OnClickListener mUpClickListener;
    private ViewGroup mUpGoerFive;
    private boolean mUserTitle;
    private boolean mWasHomeEnabled;
    Window.Callback mWindowCallback;

    public ActionBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDisplayOptions = -1;
        this.mDefaultUpDescription = R.string.action_bar_up_description;
        this.mExpandedActionViewUpListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MenuItemImpl menuItemImpl = ActionBarView.this.mExpandedMenuPresenter.mCurrentExpandedItem;
                if (menuItemImpl != null) {
                    menuItemImpl.collapseActionView();
                }
            }
        };
        this.mUpClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActionBarView.this.mMenuPrepared) {
                    ActionBarView.this.mWindowCallback.onMenuItemSelected(0, ActionBarView.this.mLogoNavItem);
                }
            }
        };
        setBackgroundResource(0);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ActionBar, 16843470, 0);
        this.mNavigationMode = typedArrayObtainStyledAttributes.getInt(7, 0);
        this.mTitle = typedArrayObtainStyledAttributes.getText(5);
        this.mSubtitle = typedArrayObtainStyledAttributes.getText(9);
        this.mLogo = typedArrayObtainStyledAttributes.getDrawable(6);
        this.mIcon = typedArrayObtainStyledAttributes.getDrawable(0);
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(context);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(16, R.layout.action_bar_home);
        this.mUpGoerFive = (ViewGroup) layoutInflaterFrom.inflate(R.layout.action_bar_up_container, (ViewGroup) this, false);
        this.mHomeLayout = (HomeView) layoutInflaterFrom.inflate(resourceId, this.mUpGoerFive, false);
        this.mExpandedHomeLayout = (HomeView) layoutInflaterFrom.inflate(resourceId, this.mUpGoerFive, false);
        this.mExpandedHomeLayout.setShowUp(true);
        this.mExpandedHomeLayout.setOnClickListener(this.mExpandedActionViewUpListener);
        this.mExpandedHomeLayout.setContentDescription(getResources().getText(this.mDefaultUpDescription));
        Drawable background = this.mUpGoerFive.getBackground();
        if (background != null) {
            this.mExpandedHomeLayout.setBackground(background.getConstantState().newDrawable());
        }
        this.mExpandedHomeLayout.setEnabled(true);
        this.mExpandedHomeLayout.setFocusable(true);
        this.mTitleStyleRes = typedArrayObtainStyledAttributes.getResourceId(11, 0);
        this.mSubtitleStyleRes = typedArrayObtainStyledAttributes.getResourceId(12, 0);
        this.mProgressStyle = typedArrayObtainStyledAttributes.getResourceId(1, 0);
        this.mIndeterminateProgressStyle = typedArrayObtainStyledAttributes.getResourceId(14, 0);
        this.mProgressBarPadding = typedArrayObtainStyledAttributes.getDimensionPixelOffset(15, 0);
        this.mItemPadding = typedArrayObtainStyledAttributes.getDimensionPixelOffset(17, 0);
        setDisplayOptions(typedArrayObtainStyledAttributes.getInt(8, 0));
        int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(10, 0);
        if (resourceId2 != 0) {
            this.mCustomNavView = layoutInflaterFrom.inflate(resourceId2, (ViewGroup) this, false);
            this.mNavigationMode = 0;
            setDisplayOptions(this.mDisplayOptions | 16);
        }
        this.mContentHeight = typedArrayObtainStyledAttributes.getLayoutDimension(4, 0);
        typedArrayObtainStyledAttributes.recycle();
        this.mLogoNavItem = new ActionMenuItem(context, 0, 16908332, 0, 0, this.mTitle);
        this.mUpGoerFive.setOnClickListener(this.mUpClickListener);
        this.mUpGoerFive.setClickable(true);
        this.mUpGoerFive.setFocusable(true);
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mTitleView = null;
        this.mSubtitleView = null;
        if (this.mTitleLayout != null && this.mTitleLayout.getParent() == this.mUpGoerFive) {
            this.mUpGoerFive.removeView(this.mTitleLayout);
        }
        this.mTitleLayout = null;
        if ((this.mDisplayOptions & 8) != 0) {
            initTitle();
        }
        if (this.mHomeDescriptionRes != 0) {
            setNavigationContentDescription(this.mHomeDescriptionRes);
        }
        if (this.mTabScrollView != null && this.mIncludeTabs) {
            ViewGroup.LayoutParams layoutParams = this.mTabScrollView.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.width = -2;
                layoutParams.height = -1;
            }
            this.mTabScrollView.setAllowCollapse(true);
        }
    }

    @Override
    public void setWindowCallback(Window.Callback callback) {
        this.mWindowCallback = callback;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mTabSelector);
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.hideOverflowMenu();
            this.mActionMenuPresenter.hideSubMenus();
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void initProgress() {
        this.mProgressView = new ProgressBar(this.mContext, null, 0, this.mProgressStyle);
        this.mProgressView.setId(R.id.progress_horizontal);
        this.mProgressView.setMax(10000);
        this.mProgressView.setVisibility(8);
        addView(this.mProgressView);
    }

    @Override
    public void initIndeterminateProgress() {
        this.mIndeterminateProgressView = new ProgressBar(this.mContext, null, 0, this.mIndeterminateProgressStyle);
        this.mIndeterminateProgressView.setId(R.id.progress_circular);
        this.mIndeterminateProgressView.setVisibility(8);
        addView(this.mIndeterminateProgressView);
    }

    @Override
    public void setSplitToolbar(boolean z) {
        if (this.mSplitActionBar != z) {
            if (this.mMenuView != null) {
                ViewGroup viewGroup = (ViewGroup) this.mMenuView.getParent();
                if (viewGroup != null) {
                    viewGroup.removeView(this.mMenuView);
                }
                if (z) {
                    if (this.mSplitView != null) {
                        this.mSplitView.addView(this.mMenuView);
                    }
                    this.mMenuView.getLayoutParams().width = -1;
                } else {
                    addView(this.mMenuView);
                    this.mMenuView.getLayoutParams().width = -2;
                }
                this.mMenuView.requestLayout();
            }
            if (this.mSplitView != null) {
                this.mSplitView.setVisibility(z ? 0 : 8);
            }
            if (this.mActionMenuPresenter != null) {
                if (!z) {
                    this.mActionMenuPresenter.setExpandedActionViewsExclusive(getResources().getBoolean(R.bool.action_bar_expanded_action_views_exclusive));
                } else {
                    this.mActionMenuPresenter.setExpandedActionViewsExclusive(false);
                    this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
                    this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
                }
            }
            super.setSplitToolbar(z);
        }
    }

    @Override
    public boolean isSplit() {
        return this.mSplitActionBar;
    }

    @Override
    public boolean canSplit() {
        return true;
    }

    @Override
    public boolean hasEmbeddedTabs() {
        return this.mIncludeTabs;
    }

    @Override
    public void setEmbeddedTabView(ScrollingTabContainerView scrollingTabContainerView) {
        if (this.mTabScrollView != null) {
            removeView(this.mTabScrollView);
        }
        this.mTabScrollView = scrollingTabContainerView;
        this.mIncludeTabs = scrollingTabContainerView != null;
        if (this.mIncludeTabs && this.mNavigationMode == 2) {
            addView(this.mTabScrollView);
            ViewGroup.LayoutParams layoutParams = this.mTabScrollView.getLayoutParams();
            layoutParams.width = -2;
            layoutParams.height = -1;
            scrollingTabContainerView.setAllowCollapse(true);
        }
    }

    @Override
    public void setMenuPrepared() {
        this.mMenuPrepared = true;
    }

    @Override
    public void setMenu(Menu menu, MenuPresenter.Callback callback) {
        ActionMenuView actionMenuView;
        ViewGroup viewGroup;
        if (menu == this.mOptionsMenu) {
            return;
        }
        if (this.mOptionsMenu != null) {
            this.mOptionsMenu.removeMenuPresenter(this.mActionMenuPresenter);
            this.mOptionsMenu.removeMenuPresenter(this.mExpandedMenuPresenter);
        }
        MenuBuilder menuBuilder = (MenuBuilder) menu;
        this.mOptionsMenu = menuBuilder;
        if (this.mMenuView != null && (viewGroup = (ViewGroup) this.mMenuView.getParent()) != null) {
            viewGroup.removeView(this.mMenuView);
        }
        if (this.mActionMenuPresenter == null) {
            this.mActionMenuPresenter = new ActionMenuPresenter(this.mContext);
            this.mActionMenuPresenter.setCallback(callback);
            this.mActionMenuPresenter.setId(R.id.action_menu_presenter);
            this.mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
        }
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -1);
        if (!this.mSplitActionBar) {
            this.mActionMenuPresenter.setExpandedActionViewsExclusive(getResources().getBoolean(R.bool.action_bar_expanded_action_views_exclusive));
            configPresenters(menuBuilder);
            actionMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
            ViewGroup viewGroup2 = (ViewGroup) actionMenuView.getParent();
            if (viewGroup2 != null && viewGroup2 != this) {
                viewGroup2.removeView(actionMenuView);
            }
            addView(actionMenuView, layoutParams);
        } else {
            this.mActionMenuPresenter.setExpandedActionViewsExclusive(false);
            this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
            this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
            layoutParams.width = -1;
            layoutParams.height = -2;
            configPresenters(menuBuilder);
            actionMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
            if (this.mSplitView != null) {
                ViewGroup viewGroup3 = (ViewGroup) actionMenuView.getParent();
                if (viewGroup3 != null && viewGroup3 != this.mSplitView) {
                    viewGroup3.removeView(actionMenuView);
                }
                actionMenuView.setVisibility(getAnimatedVisibility());
                this.mSplitView.addView(actionMenuView, layoutParams);
            } else {
                actionMenuView.setLayoutParams(layoutParams);
            }
        }
        this.mMenuView = actionMenuView;
    }

    private void configPresenters(MenuBuilder menuBuilder) {
        if (menuBuilder != null) {
            menuBuilder.addMenuPresenter(this.mActionMenuPresenter, this.mPopupContext);
            menuBuilder.addMenuPresenter(this.mExpandedMenuPresenter, this.mPopupContext);
        } else {
            this.mActionMenuPresenter.initForMenu(this.mPopupContext, null);
            this.mExpandedMenuPresenter.initForMenu(this.mPopupContext, null);
            this.mActionMenuPresenter.updateMenuView(true);
            this.mExpandedMenuPresenter.updateMenuView(true);
        }
    }

    @Override
    public boolean hasExpandedActionView() {
        return (this.mExpandedMenuPresenter == null || this.mExpandedMenuPresenter.mCurrentExpandedItem == null) ? false : true;
    }

    @Override
    public void collapseActionView() {
        MenuItemImpl menuItemImpl = this.mExpandedMenuPresenter == null ? null : this.mExpandedMenuPresenter.mCurrentExpandedItem;
        if (menuItemImpl != null) {
            menuItemImpl.collapseActionView();
        }
    }

    @Override
    public void setCustomView(View view) {
        boolean z = (this.mDisplayOptions & 16) != 0;
        if (this.mCustomNavView != null && z) {
            removeView(this.mCustomNavView);
        }
        this.mCustomNavView = view;
        if (this.mCustomNavView != null && z) {
            addView(this.mCustomNavView);
        }
    }

    @Override
    public CharSequence getTitle() {
        return this.mTitle;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mUserTitle = true;
        setTitleImpl(charSequence);
    }

    @Override
    public void setWindowTitle(CharSequence charSequence) {
        if (!this.mUserTitle) {
            setTitleImpl(charSequence);
        }
    }

    private void setTitleImpl(CharSequence charSequence) {
        this.mTitle = charSequence;
        if (this.mTitleView != null) {
            this.mTitleView.setText(charSequence);
            this.mTitleLayout.setVisibility(this.mExpandedActionView == null && (this.mDisplayOptions & 8) != 0 && (!TextUtils.isEmpty(this.mTitle) || !TextUtils.isEmpty(this.mSubtitle)) ? 0 : 8);
        }
        if (this.mLogoNavItem != null) {
            this.mLogoNavItem.setTitle(charSequence);
        }
        updateHomeAccessibility(this.mUpGoerFive.isEnabled());
    }

    @Override
    public CharSequence getSubtitle() {
        return this.mSubtitle;
    }

    @Override
    public void setSubtitle(CharSequence charSequence) {
        this.mSubtitle = charSequence;
        if (this.mSubtitleView != null) {
            this.mSubtitleView.setText(charSequence);
            this.mSubtitleView.setVisibility(charSequence != null ? 0 : 8);
            this.mTitleLayout.setVisibility(this.mExpandedActionView == null && (this.mDisplayOptions & 8) != 0 && (!TextUtils.isEmpty(this.mTitle) || !TextUtils.isEmpty(this.mSubtitle)) ? 0 : 8);
        }
        updateHomeAccessibility(this.mUpGoerFive.isEnabled());
    }

    @Override
    public void setHomeButtonEnabled(boolean z) {
        setHomeButtonEnabled(z, true);
    }

    private void setHomeButtonEnabled(boolean z, boolean z2) {
        if (z2) {
            this.mWasHomeEnabled = z;
        }
        if (this.mExpandedActionView != null) {
            return;
        }
        this.mUpGoerFive.setEnabled(z);
        this.mUpGoerFive.setFocusable(z);
        updateHomeAccessibility(z);
    }

    private void updateHomeAccessibility(boolean z) {
        if (!z) {
            this.mUpGoerFive.setContentDescription(null);
            this.mUpGoerFive.setImportantForAccessibility(2);
        } else {
            this.mUpGoerFive.setImportantForAccessibility(0);
            this.mUpGoerFive.setContentDescription(buildHomeContentDescription());
        }
    }

    private CharSequence buildHomeContentDescription() {
        CharSequence text;
        if (this.mHomeDescription != null) {
            text = this.mHomeDescription;
        } else if ((this.mDisplayOptions & 4) != 0) {
            text = this.mContext.getResources().getText(this.mDefaultUpDescription);
        } else {
            text = this.mContext.getResources().getText(R.string.action_bar_home_description);
        }
        CharSequence title = getTitle();
        CharSequence subtitle = getSubtitle();
        if (TextUtils.isEmpty(title)) {
            return text;
        }
        return !TextUtils.isEmpty(subtitle) ? getResources().getString(R.string.action_bar_home_subtitle_description_format, title, subtitle, text) : getResources().getString(R.string.action_bar_home_description_format, title, text);
    }

    @Override
    public void setDisplayOptions(int i) {
        int i2 = this.mDisplayOptions != -1 ? i ^ this.mDisplayOptions : -1;
        this.mDisplayOptions = i;
        if ((i2 & 63) != 0) {
            if ((i2 & 4) != 0) {
                boolean z = (i & 4) != 0;
                this.mHomeLayout.setShowUp(z);
                if (z) {
                    setHomeButtonEnabled(true);
                }
            }
            if ((i2 & 1) != 0) {
                this.mHomeLayout.setIcon(this.mLogo != null && (i & 1) != 0 ? this.mLogo : this.mIcon);
            }
            if ((i2 & 8) != 0) {
                if ((i & 8) != 0) {
                    initTitle();
                } else {
                    this.mUpGoerFive.removeView(this.mTitleLayout);
                }
            }
            boolean z2 = (i & 2) != 0;
            boolean z3 = !z2 && ((this.mDisplayOptions & 4) != 0);
            this.mHomeLayout.setShowIcon(z2);
            this.mHomeLayout.setVisibility(((z2 || z3) && this.mExpandedActionView == null) ? 0 : 8);
            if ((i2 & 16) != 0 && this.mCustomNavView != null) {
                if ((i & 16) != 0) {
                    addView(this.mCustomNavView);
                } else {
                    removeView(this.mCustomNavView);
                }
            }
            if (this.mTitleLayout != null && (i2 & 32) != 0) {
                if ((i & 32) != 0) {
                    this.mTitleView.setSingleLine(false);
                    this.mTitleView.setMaxLines(2);
                } else {
                    this.mTitleView.setMaxLines(1);
                    this.mTitleView.setSingleLine(true);
                }
            }
            requestLayout();
        } else {
            invalidate();
        }
        updateHomeAccessibility(this.mUpGoerFive.isEnabled());
    }

    @Override
    public void setIcon(Drawable drawable) {
        this.mIcon = drawable;
        if (drawable != null && ((this.mDisplayOptions & 1) == 0 || this.mLogo == null)) {
            this.mHomeLayout.setIcon(drawable);
        }
        if (this.mExpandedActionView != null) {
            this.mExpandedHomeLayout.setIcon(this.mIcon.getConstantState().newDrawable(getResources()));
        }
    }

    @Override
    public void setIcon(int i) {
        setIcon(i != 0 ? this.mContext.getDrawable(i) : null);
    }

    @Override
    public boolean hasIcon() {
        return this.mIcon != null;
    }

    @Override
    public void setLogo(Drawable drawable) {
        this.mLogo = drawable;
        if (drawable != null && (this.mDisplayOptions & 1) != 0) {
            this.mHomeLayout.setIcon(drawable);
        }
    }

    @Override
    public void setLogo(int i) {
        setLogo(i != 0 ? this.mContext.getDrawable(i) : null);
    }

    @Override
    public boolean hasLogo() {
        return this.mLogo != null;
    }

    @Override
    public void setNavigationMode(int i) {
        int i2 = this.mNavigationMode;
        if (i != i2) {
            switch (i2) {
                case 1:
                    if (this.mListNavLayout != null) {
                        removeView(this.mListNavLayout);
                    }
                    break;
                case 2:
                    if (this.mTabScrollView != null && this.mIncludeTabs) {
                        removeView(this.mTabScrollView);
                    }
                    break;
            }
            switch (i) {
                case 1:
                    if (this.mSpinner == null) {
                        this.mSpinner = new Spinner(this.mContext, null, 16843479);
                        this.mSpinner.setId(R.id.action_bar_spinner);
                        this.mListNavLayout = new LinearLayout(this.mContext, null, 16843508);
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -1);
                        layoutParams.gravity = 17;
                        this.mListNavLayout.addView(this.mSpinner, layoutParams);
                    }
                    if (this.mSpinner.getAdapter() != this.mSpinnerAdapter) {
                        this.mSpinner.setAdapter(this.mSpinnerAdapter);
                    }
                    this.mSpinner.setOnItemSelectedListener(this.mNavItemSelectedListener);
                    addView(this.mListNavLayout);
                    break;
                case 2:
                    if (this.mTabScrollView != null && this.mIncludeTabs) {
                        addView(this.mTabScrollView);
                    }
                    break;
            }
            this.mNavigationMode = i;
            requestLayout();
        }
    }

    @Override
    public void setDropdownParams(SpinnerAdapter spinnerAdapter, AdapterView.OnItemSelectedListener onItemSelectedListener) {
        this.mSpinnerAdapter = spinnerAdapter;
        this.mNavItemSelectedListener = onItemSelectedListener;
        if (this.mSpinner != null) {
            this.mSpinner.setAdapter(spinnerAdapter);
            this.mSpinner.setOnItemSelectedListener(onItemSelectedListener);
        }
    }

    @Override
    public int getDropdownItemCount() {
        if (this.mSpinnerAdapter != null) {
            return this.mSpinnerAdapter.getCount();
        }
        return 0;
    }

    @Override
    public void setDropdownSelectedPosition(int i) {
        this.mSpinner.setSelection(i);
    }

    @Override
    public int getDropdownSelectedPosition() {
        return this.mSpinner.getSelectedItemPosition();
    }

    @Override
    public View getCustomView() {
        return this.mCustomNavView;
    }

    @Override
    public int getNavigationMode() {
        return this.mNavigationMode;
    }

    @Override
    public int getDisplayOptions() {
        return this.mDisplayOptions;
    }

    @Override
    public ViewGroup getViewGroup() {
        return this;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ActionBar.LayoutParams(DEFAULT_CUSTOM_GRAVITY);
    }

    @Override
    protected void onFinishInflate() {
        ViewParent parent;
        super.onFinishInflate();
        this.mUpGoerFive.addView(this.mHomeLayout, 0);
        addView(this.mUpGoerFive);
        if (this.mCustomNavView != null && (this.mDisplayOptions & 16) != 0 && (parent = this.mCustomNavView.getParent()) != this) {
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(this.mCustomNavView);
            }
            addView(this.mCustomNavView);
        }
    }

    private void initTitle() {
        if (this.mTitleLayout == null) {
            this.mTitleLayout = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.action_bar_title_item, (ViewGroup) this, false);
            this.mTitleView = (TextView) this.mTitleLayout.findViewById(R.id.action_bar_title);
            this.mSubtitleView = (TextView) this.mTitleLayout.findViewById(R.id.action_bar_subtitle);
            if (this.mTitleStyleRes != 0) {
                this.mTitleView.setTextAppearance(this.mTitleStyleRes);
            }
            if (this.mTitle != null) {
                this.mTitleView.setText(this.mTitle);
            }
            if (this.mSubtitleStyleRes != 0) {
                this.mSubtitleView.setTextAppearance(this.mSubtitleStyleRes);
            }
            if (this.mSubtitle != null) {
                this.mSubtitleView.setText(this.mSubtitle);
                this.mSubtitleView.setVisibility(0);
            }
        }
        this.mUpGoerFive.addView(this.mTitleLayout);
        if (this.mExpandedActionView != null || (TextUtils.isEmpty(this.mTitle) && TextUtils.isEmpty(this.mSubtitle))) {
            this.mTitleLayout.setVisibility(8);
        } else {
            this.mTitleLayout.setVisibility(0);
        }
    }

    public void setContextView(ActionBarContextView actionBarContextView) {
        this.mContextView = actionBarContextView;
    }

    @Override
    public void setCollapsible(boolean z) {
        this.mIsCollapsible = z;
    }

    @Override
    public boolean isTitleTruncated() {
        Layout layout;
        if (this.mTitleView == null || (layout = this.mTitleView.getLayout()) == null) {
            return false;
        }
        int lineCount = layout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            if (layout.getEllipsisCount(i) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int iMax;
        int measuredWidth;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int childCount = getChildCount();
        if (this.mIsCollapsible) {
            int i8 = 0;
            for (int i9 = 0; i9 < childCount; i9++) {
                View childAt = getChildAt(i9);
                if (childAt.getVisibility() != 8 && ((childAt != this.mMenuView || this.mMenuView.getChildCount() != 0) && childAt != this.mUpGoerFive)) {
                    i8++;
                }
            }
            int childCount2 = this.mUpGoerFive.getChildCount();
            int i10 = i8;
            for (int i11 = 0; i11 < childCount2; i11++) {
                if (this.mUpGoerFive.getChildAt(i11).getVisibility() != 8) {
                    i10++;
                }
            }
            if (i10 == 0) {
                setMeasuredDimension(0, 0);
                return;
            }
        }
        if (View.MeasureSpec.getMode(i) != 1073741824) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with android:layout_width=\"match_parent\" (or fill_parent)");
        }
        if (View.MeasureSpec.getMode(i2) != Integer.MIN_VALUE) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with android:layout_height=\"wrap_content\"");
        }
        int size = View.MeasureSpec.getSize(i);
        int size2 = this.mContentHeight >= 0 ? this.mContentHeight : View.MeasureSpec.getSize(i2);
        int paddingTop = getPaddingTop() + getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int iMin = size2 - paddingTop;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(iMin, Integer.MIN_VALUE);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(iMin, 1073741824);
        int measuredWidth2 = (size - paddingLeft) - paddingRight;
        int iMax2 = measuredWidth2 / 2;
        boolean z = (this.mTitleLayout == null || this.mTitleLayout.getVisibility() == 8 || (this.mDisplayOptions & 8) == 0) ? false : true;
        HomeView homeView = this.mExpandedActionView != null ? this.mExpandedHomeLayout : this.mHomeLayout;
        ViewGroup.LayoutParams layoutParams = homeView.getLayoutParams();
        homeView.measure(layoutParams.width < 0 ? View.MeasureSpec.makeMeasureSpec(measuredWidth2, Integer.MIN_VALUE) : View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824), iMakeMeasureSpec2);
        if ((homeView.getVisibility() == 8 || homeView.getParent() != this.mUpGoerFive) && !z) {
            iMax = iMax2;
            measuredWidth = 0;
        } else {
            measuredWidth = homeView.getMeasuredWidth();
            int startOffset = homeView.getStartOffset() + measuredWidth;
            measuredWidth2 = Math.max(0, measuredWidth2 - startOffset);
            iMax = Math.max(0, measuredWidth2 - startOffset);
        }
        if (this.mMenuView != null && this.mMenuView.getParent() == this) {
            measuredWidth2 = measureChildView(this.mMenuView, measuredWidth2, iMakeMeasureSpec2, 0);
            iMax2 = Math.max(0, iMax2 - this.mMenuView.getMeasuredWidth());
        }
        if (this.mIndeterminateProgressView != null && this.mIndeterminateProgressView.getVisibility() != 8) {
            measuredWidth2 = measureChildView(this.mIndeterminateProgressView, measuredWidth2, iMakeMeasureSpec, 0);
            iMax2 = Math.max(0, iMax2 - this.mIndeterminateProgressView.getMeasuredWidth());
        }
        if (this.mExpandedActionView == null) {
            switch (this.mNavigationMode) {
                case 1:
                    if (this.mListNavLayout != null) {
                        int i12 = z ? this.mItemPadding * 2 : this.mItemPadding;
                        int iMax3 = Math.max(0, measuredWidth2 - i12);
                        int iMax4 = Math.max(0, iMax - i12);
                        this.mListNavLayout.measure(View.MeasureSpec.makeMeasureSpec(iMax3, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(iMin, 1073741824));
                        int measuredWidth3 = this.mListNavLayout.getMeasuredWidth();
                        measuredWidth2 = Math.max(0, iMax3 - measuredWidth3);
                        iMax = Math.max(0, iMax4 - measuredWidth3);
                    }
                    break;
                case 2:
                    if (this.mTabScrollView != null) {
                        int i13 = z ? this.mItemPadding * 2 : this.mItemPadding;
                        int iMax5 = Math.max(0, measuredWidth2 - i13);
                        int iMax6 = Math.max(0, iMax - i13);
                        this.mTabScrollView.measure(View.MeasureSpec.makeMeasureSpec(iMax5, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(iMin, 1073741824));
                        int measuredWidth4 = this.mTabScrollView.getMeasuredWidth();
                        measuredWidth2 = Math.max(0, iMax5 - measuredWidth4);
                        iMax = Math.max(0, iMax6 - measuredWidth4);
                    }
                    break;
            }
        }
        View view = this.mExpandedActionView != null ? this.mExpandedActionView : ((this.mDisplayOptions & 16) == 0 || this.mCustomNavView == null) ? null : this.mCustomNavView;
        if (view != null) {
            ViewGroup.LayoutParams layoutParamsGenerateLayoutParams = generateLayoutParams(view.getLayoutParams());
            ActionBar.LayoutParams layoutParams2 = layoutParamsGenerateLayoutParams instanceof ActionBar.LayoutParams ? (ActionBar.LayoutParams) layoutParamsGenerateLayoutParams : null;
            if (layoutParams2 != null) {
                i7 = layoutParams2.leftMargin + layoutParams2.rightMargin;
                i6 = layoutParams2.bottomMargin + layoutParams2.topMargin;
            } else {
                i6 = 0;
                i7 = 0;
            }
            i4 = size2;
            int i14 = (this.mContentHeight > 0 && layoutParamsGenerateLayoutParams.height != -2) ? 1073741824 : Integer.MIN_VALUE;
            if (layoutParamsGenerateLayoutParams.height >= 0) {
                iMin = Math.min(layoutParamsGenerateLayoutParams.height, iMin);
            }
            int iMax7 = Math.max(0, iMin - i6);
            int i15 = layoutParamsGenerateLayoutParams.width != -2 ? 1073741824 : Integer.MIN_VALUE;
            i3 = size;
            int iMax8 = Math.max(0, (layoutParamsGenerateLayoutParams.width >= 0 ? Math.min(layoutParamsGenerateLayoutParams.width, measuredWidth2) : measuredWidth2) - i7);
            if (((layoutParams2 != null ? layoutParams2.gravity : DEFAULT_CUSTOM_GRAVITY) & 7) == 1 && layoutParamsGenerateLayoutParams.width == -1) {
                iMax8 = Math.min(iMax, iMax2) * 2;
            }
            view.measure(View.MeasureSpec.makeMeasureSpec(iMax8, i15), View.MeasureSpec.makeMeasureSpec(iMax7, i14));
            measuredWidth2 -= i7 + view.getMeasuredWidth();
        } else {
            i3 = size;
            i4 = size2;
        }
        measureChildView(this.mUpGoerFive, measuredWidth2 + measuredWidth, View.MeasureSpec.makeMeasureSpec(this.mContentHeight, 1073741824), 0);
        if (this.mTitleLayout != null) {
            Math.max(0, iMax - this.mTitleLayout.getMeasuredWidth());
        }
        if (this.mContentHeight <= 0) {
            int i16 = 0;
            for (int i17 = 0; i17 < childCount; i17++) {
                int measuredHeight = getChildAt(i17).getMeasuredHeight() + paddingTop;
                if (measuredHeight > i16) {
                    i16 = measuredHeight;
                }
            }
            i5 = i3;
            setMeasuredDimension(i5, i16);
        } else {
            i5 = i3;
            setMeasuredDimension(i5, i4);
        }
        if (this.mContextView != null) {
            this.mContextView.setContentHeight(getMeasuredHeight());
        }
        if (this.mProgressView == null || this.mProgressView.getVisibility() == 8) {
            return;
        }
        this.mProgressView.measure(View.MeasureSpec.makeMeasureSpec(i5 - (this.mProgressBarPadding * 2), 1073741824), View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), Integer.MIN_VALUE));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        View view;
        int i6;
        int i7;
        int i8;
        int i9;
        int paddingBottom;
        int upWidth;
        int paddingTop = ((i4 - i2) - getPaddingTop()) - getPaddingBottom();
        if (paddingTop <= 0) {
            return;
        }
        boolean zIsLayoutRtl = isLayoutRtl();
        int i10 = zIsLayoutRtl ? 1 : -1;
        int paddingLeft = zIsLayoutRtl ? getPaddingLeft() : (i3 - i) - getPaddingRight();
        int paddingRight = zIsLayoutRtl ? (i3 - i) - getPaddingRight() : getPaddingLeft();
        int paddingTop2 = getPaddingTop();
        HomeView homeView = this.mExpandedActionView != null ? this.mExpandedHomeLayout : this.mHomeLayout;
        boolean z2 = (this.mTitleLayout == null || this.mTitleLayout.getVisibility() == 8 || (this.mDisplayOptions & 8) == 0) ? false : true;
        if (homeView.getParent() == this.mUpGoerFive) {
            if (homeView.getVisibility() != 8) {
                upWidth = homeView.getStartOffset();
            } else {
                if (z2) {
                    upWidth = homeView.getUpWidth();
                }
                i5 = 0;
            }
            i5 = upWidth;
        } else {
            i5 = 0;
        }
        int next = next(paddingRight + positionChild(this.mUpGoerFive, next(paddingRight, i5, zIsLayoutRtl), paddingTop2, paddingTop, zIsLayoutRtl), i5, zIsLayoutRtl);
        if (this.mExpandedActionView == null) {
            switch (this.mNavigationMode) {
                case 1:
                    if (this.mListNavLayout != null) {
                        if (z2) {
                            next = next(next, this.mItemPadding, zIsLayoutRtl);
                        }
                        int i11 = next;
                        next = next(i11 + positionChild(this.mListNavLayout, i11, paddingTop2, paddingTop, zIsLayoutRtl), this.mItemPadding, zIsLayoutRtl);
                    }
                    break;
                case 2:
                    if (this.mTabScrollView != null) {
                        if (z2) {
                            next = next(next, this.mItemPadding, zIsLayoutRtl);
                        }
                        int i12 = next;
                        next = next(i12 + positionChild(this.mTabScrollView, i12, paddingTop2, paddingTop, zIsLayoutRtl), this.mItemPadding, zIsLayoutRtl);
                    }
                    break;
            }
        }
        int next2 = next;
        if (this.mMenuView != null && this.mMenuView.getParent() == this) {
            positionChild(this.mMenuView, paddingLeft, paddingTop2, paddingTop, !zIsLayoutRtl);
            paddingLeft += this.mMenuView.getMeasuredWidth() * i10;
        }
        if (this.mIndeterminateProgressView != null && this.mIndeterminateProgressView.getVisibility() != 8) {
            positionChild(this.mIndeterminateProgressView, paddingLeft, paddingTop2, paddingTop, !zIsLayoutRtl);
            paddingLeft += this.mIndeterminateProgressView.getMeasuredWidth() * i10;
        }
        if (this.mExpandedActionView != null) {
            view = this.mExpandedActionView;
        } else if ((this.mDisplayOptions & 16) != 0 && this.mCustomNavView != null) {
            view = this.mCustomNavView;
        } else {
            view = null;
        }
        if (view != null) {
            int layoutDirection = getLayoutDirection();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            ActionBar.LayoutParams layoutParams2 = layoutParams instanceof ActionBar.LayoutParams ? (ActionBar.LayoutParams) layoutParams : null;
            int i13 = layoutParams2 != null ? layoutParams2.gravity : DEFAULT_CUSTOM_GRAVITY;
            int measuredWidth = view.getMeasuredWidth();
            if (layoutParams2 != null) {
                next2 = next(next2, layoutParams2.getMarginStart(), zIsLayoutRtl);
                paddingLeft += i10 * layoutParams2.getMarginEnd();
                i7 = layoutParams2.topMargin;
                i6 = layoutParams2.bottomMargin;
            } else {
                i6 = 0;
                i7 = 0;
            }
            int i14 = 8388615 & i13;
            if (i14 == 1) {
                int i15 = ((this.mRight - this.mLeft) - measuredWidth) / 2;
                if (zIsLayoutRtl) {
                    if (i15 + measuredWidth <= next2) {
                        if (i15 < paddingLeft) {
                            i14 = 3;
                        }
                    } else {
                        i14 = 5;
                    }
                } else {
                    int i16 = i15 + measuredWidth;
                    if (i15 >= next2) {
                        if (i16 > paddingLeft) {
                            i14 = 5;
                        }
                    } else {
                        i14 = 3;
                    }
                }
            } else if (i13 == 0) {
                i14 = Gravity.START;
            }
            int absoluteGravity = Gravity.getAbsoluteGravity(i14, layoutDirection);
            if (absoluteGravity != 1) {
                if (absoluteGravity != 3) {
                    if (absoluteGravity == 5) {
                        if (zIsLayoutRtl) {
                            i8 = next2 - measuredWidth;
                        } else {
                            paddingLeft -= measuredWidth;
                        }
                    } else {
                        paddingLeft = 0;
                    }
                } else if (!zIsLayoutRtl) {
                    paddingLeft = next2;
                }
                i9 = i13 & 112;
                if (i13 == 0) {
                    i9 = 16;
                }
                if (i9 != 16) {
                    paddingBottom = ((((this.mBottom - this.mTop) - getPaddingBottom()) - getPaddingTop()) - view.getMeasuredHeight()) / 2;
                } else if (i9 == 48) {
                    paddingBottom = i7 + getPaddingTop();
                } else if (i9 == 80) {
                    paddingBottom = ((getHeight() - getPaddingBottom()) - view.getMeasuredHeight()) - i6;
                } else {
                    paddingBottom = 0;
                }
                int measuredWidth2 = view.getMeasuredWidth();
                view.layout(paddingLeft, paddingBottom, paddingLeft + measuredWidth2, view.getMeasuredHeight() + paddingBottom);
                next(next2, measuredWidth2, zIsLayoutRtl);
            } else {
                i8 = ((this.mRight - this.mLeft) - measuredWidth) / 2;
            }
            paddingLeft = i8;
            i9 = i13 & 112;
            if (i13 == 0) {
            }
            if (i9 != 16) {
            }
            int measuredWidth22 = view.getMeasuredWidth();
            view.layout(paddingLeft, paddingBottom, paddingLeft + measuredWidth22, view.getMeasuredHeight() + paddingBottom);
            next(next2, measuredWidth22, zIsLayoutRtl);
        }
        if (this.mProgressView != null) {
            this.mProgressView.bringToFront();
            int measuredHeight = this.mProgressView.getMeasuredHeight() / 2;
            this.mProgressView.layout(this.mProgressBarPadding, -measuredHeight, this.mProgressBarPadding + this.mProgressView.getMeasuredWidth(), measuredHeight);
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new ActionBar.LayoutParams(getContext(), attributeSet);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (layoutParams == null) {
            return generateDefaultLayoutParams();
        }
        return layoutParams;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        if (this.mExpandedMenuPresenter != null && this.mExpandedMenuPresenter.mCurrentExpandedItem != null) {
            savedState.expandedMenuItemId = this.mExpandedMenuPresenter.mCurrentExpandedItem.getItemId();
        }
        savedState.isOverflowOpen = isOverflowMenuShowing();
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        MenuItem menuItemFindItem;
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.expandedMenuItemId != 0 && this.mExpandedMenuPresenter != null && this.mOptionsMenu != null && (menuItemFindItem = this.mOptionsMenu.findItem(savedState.expandedMenuItemId)) != null) {
            menuItemFindItem.expandActionView();
        }
        if (savedState.isOverflowOpen) {
            postShowOverflowMenu();
        }
    }

    @Override
    public void setNavigationIcon(Drawable drawable) {
        this.mHomeLayout.setUpIndicator(drawable);
    }

    @Override
    public void setDefaultNavigationIcon(Drawable drawable) {
        this.mHomeLayout.setDefaultUpIndicator(drawable);
    }

    @Override
    public void setNavigationIcon(int i) {
        this.mHomeLayout.setUpIndicator(i);
    }

    @Override
    public void setNavigationContentDescription(CharSequence charSequence) {
        this.mHomeDescription = charSequence;
        updateHomeAccessibility(this.mUpGoerFive.isEnabled());
    }

    @Override
    public void setNavigationContentDescription(int i) {
        this.mHomeDescriptionRes = i;
        this.mHomeDescription = i != 0 ? getResources().getText(i) : null;
        updateHomeAccessibility(this.mUpGoerFive.isEnabled());
    }

    @Override
    public void setDefaultNavigationContentDescription(int i) {
        if (this.mDefaultUpDescription == i) {
            return;
        }
        this.mDefaultUpDescription = i;
        updateHomeAccessibility(this.mUpGoerFive.isEnabled());
    }

    @Override
    public void setMenuCallbacks(MenuPresenter.Callback callback, MenuBuilder.Callback callback2) {
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.setCallback(callback);
        }
        if (this.mOptionsMenu != null) {
            this.mOptionsMenu.setCallback(callback2);
        }
    }

    @Override
    public Menu getMenu() {
        return this.mOptionsMenu;
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int expandedMenuItemId;
        boolean isOverflowOpen;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.expandedMenuItemId = parcel.readInt();
            this.isOverflowOpen = parcel.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.expandedMenuItemId);
            parcel.writeInt(this.isOverflowOpen ? 1 : 0);
        }
    }

    private static class HomeView extends FrameLayout {
        private static final long DEFAULT_TRANSITION_DURATION = 150;
        private Drawable mDefaultUpIndicator;
        private ImageView mIconView;
        private int mStartOffset;
        private Drawable mUpIndicator;
        private int mUpIndicatorRes;
        private ImageView mUpView;
        private int mUpWidth;

        public HomeView(Context context) {
            this(context, null);
        }

        public HomeView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            LayoutTransition layoutTransition = getLayoutTransition();
            if (layoutTransition != null) {
                layoutTransition.setDuration(DEFAULT_TRANSITION_DURATION);
            }
        }

        public void setShowUp(boolean z) {
            this.mUpView.setVisibility(z ? 0 : 8);
        }

        public void setShowIcon(boolean z) {
            this.mIconView.setVisibility(z ? 0 : 8);
        }

        public void setIcon(Drawable drawable) {
            this.mIconView.setImageDrawable(drawable);
        }

        public void setUpIndicator(Drawable drawable) {
            this.mUpIndicator = drawable;
            this.mUpIndicatorRes = 0;
            updateUpIndicator();
        }

        public void setDefaultUpIndicator(Drawable drawable) {
            this.mDefaultUpIndicator = drawable;
            updateUpIndicator();
        }

        public void setUpIndicator(int i) {
            this.mUpIndicatorRes = i;
            this.mUpIndicator = null;
            updateUpIndicator();
        }

        private void updateUpIndicator() {
            if (this.mUpIndicator != null) {
                this.mUpView.setImageDrawable(this.mUpIndicator);
            } else if (this.mUpIndicatorRes != 0) {
                this.mUpView.setImageDrawable(getContext().getDrawable(this.mUpIndicatorRes));
            } else {
                this.mUpView.setImageDrawable(this.mDefaultUpIndicator);
            }
        }

        @Override
        protected void onConfigurationChanged(Configuration configuration) {
            super.onConfigurationChanged(configuration);
            if (this.mUpIndicatorRes != 0) {
                updateUpIndicator();
            }
        }

        @Override
        public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
            onPopulateAccessibilityEvent(accessibilityEvent);
            return true;
        }

        @Override
        public void onPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
            super.onPopulateAccessibilityEventInternal(accessibilityEvent);
            CharSequence contentDescription = getContentDescription();
            if (!TextUtils.isEmpty(contentDescription)) {
                accessibilityEvent.getText().add(contentDescription);
            }
        }

        @Override
        public boolean dispatchHoverEvent(MotionEvent motionEvent) {
            return onHoverEvent(motionEvent);
        }

        @Override
        protected void onFinishInflate() {
            this.mUpView = (ImageView) findViewById(R.id.up);
            this.mIconView = (ImageView) findViewById(16908332);
            this.mDefaultUpIndicator = this.mUpView.getDrawable();
        }

        public int getStartOffset() {
            if (this.mUpView.getVisibility() == 8) {
                return this.mStartOffset;
            }
            return 0;
        }

        public int getUpWidth() {
            return this.mUpWidth;
        }

        @Override
        protected void onMeasure(int i, int i2) {
            measureChildWithMargins(this.mUpView, i, 0, i2, 0);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mUpView.getLayoutParams();
            int i3 = layoutParams.leftMargin + layoutParams.rightMargin;
            this.mUpWidth = this.mUpView.getMeasuredWidth();
            this.mStartOffset = this.mUpWidth + i3;
            int iMin = this.mUpView.getVisibility() == 8 ? 0 : this.mStartOffset;
            int measuredHeight = layoutParams.bottomMargin + layoutParams.topMargin + this.mUpView.getMeasuredHeight();
            if (this.mIconView.getVisibility() != 8) {
                measureChildWithMargins(this.mIconView, i, iMin, i2, 0);
                FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.mIconView.getLayoutParams();
                iMin += layoutParams2.leftMargin + this.mIconView.getMeasuredWidth() + layoutParams2.rightMargin;
                measuredHeight = Math.max(measuredHeight, layoutParams2.topMargin + this.mIconView.getMeasuredHeight() + layoutParams2.bottomMargin);
            } else if (i3 < 0) {
                iMin -= i3;
            }
            int mode = View.MeasureSpec.getMode(i);
            int mode2 = View.MeasureSpec.getMode(i2);
            int size = View.MeasureSpec.getSize(i);
            int size2 = View.MeasureSpec.getSize(i2);
            if (mode != Integer.MIN_VALUE) {
                if (mode == 1073741824) {
                    iMin = size;
                }
            } else {
                iMin = Math.min(iMin, size);
            }
            if (mode2 != Integer.MIN_VALUE) {
                if (mode2 == 1073741824) {
                    measuredHeight = size2;
                }
            } else {
                measuredHeight = Math.min(measuredHeight, size2);
            }
            setMeasuredDimension(iMin, measuredHeight);
        }

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            int i5;
            int i6;
            int i7;
            int i8 = (i4 - i2) / 2;
            boolean zIsLayoutRtl = isLayoutRtl();
            int width = getWidth();
            int i9 = 0;
            if (this.mUpView.getVisibility() != 8) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mUpView.getLayoutParams();
                int measuredHeight = this.mUpView.getMeasuredHeight();
                int measuredWidth = this.mUpView.getMeasuredWidth();
                i5 = layoutParams.rightMargin + layoutParams.leftMargin + measuredWidth;
                int i10 = i8 - (measuredHeight / 2);
                int i11 = measuredHeight + i10;
                if (zIsLayoutRtl) {
                    i9 = width - measuredWidth;
                    i3 -= i5;
                    measuredWidth = width;
                } else {
                    i += i5;
                }
                this.mUpView.layout(i9, i10, measuredWidth, i11);
            } else {
                i5 = 0;
            }
            FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.mIconView.getLayoutParams();
            int measuredHeight2 = this.mIconView.getMeasuredHeight();
            int measuredWidth2 = this.mIconView.getMeasuredWidth();
            int i12 = (i3 - i) / 2;
            int iMax = Math.max(layoutParams2.topMargin, i8 - (measuredHeight2 / 2));
            int i13 = measuredHeight2 + iMax;
            int iMax2 = Math.max(layoutParams2.getMarginStart(), i12 - (measuredWidth2 / 2));
            if (zIsLayoutRtl) {
                i7 = (width - i5) - iMax2;
                i6 = i7 - measuredWidth2;
            } else {
                i6 = i5 + iMax2;
                i7 = i6 + measuredWidth2;
            }
            this.mIconView.layout(i6, iMax, i7, i13);
        }
    }

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {
        MenuItemImpl mCurrentExpandedItem;
        MenuBuilder mMenu;

        private ExpandedActionViewMenuPresenter() {
        }

        @Override
        public void initForMenu(Context context, MenuBuilder menuBuilder) {
            if (this.mMenu != null && this.mCurrentExpandedItem != null) {
                this.mMenu.collapseItemActionView(this.mCurrentExpandedItem);
            }
            this.mMenu = menuBuilder;
        }

        @Override
        public MenuView getMenuView(ViewGroup viewGroup) {
            return null;
        }

        @Override
        public void updateMenuView(boolean z) {
            if (this.mCurrentExpandedItem != null) {
                boolean z2 = false;
                if (this.mMenu != null) {
                    int size = this.mMenu.size();
                    int i = 0;
                    while (true) {
                        if (i >= size) {
                            break;
                        }
                        if (this.mMenu.getItem(i) != this.mCurrentExpandedItem) {
                            i++;
                        } else {
                            z2 = true;
                            break;
                        }
                    }
                }
                if (!z2) {
                    collapseItemActionView(this.mMenu, this.mCurrentExpandedItem);
                }
            }
        }

        @Override
        public void setCallback(MenuPresenter.Callback callback) {
        }

        @Override
        public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
        }

        @Override
        public boolean flagActionItems() {
            return false;
        }

        @Override
        public boolean expandItemActionView(MenuBuilder menuBuilder, MenuItemImpl menuItemImpl) {
            ActionBarView.this.mExpandedActionView = menuItemImpl.getActionView();
            ActionBarView.this.mExpandedHomeLayout.setIcon(ActionBarView.this.mIcon.getConstantState().newDrawable(ActionBarView.this.getResources()));
            this.mCurrentExpandedItem = menuItemImpl;
            if (ActionBarView.this.mExpandedActionView.getParent() != ActionBarView.this) {
                ActionBarView.this.addView(ActionBarView.this.mExpandedActionView);
            }
            if (ActionBarView.this.mExpandedHomeLayout.getParent() != ActionBarView.this.mUpGoerFive) {
                ActionBarView.this.mUpGoerFive.addView(ActionBarView.this.mExpandedHomeLayout);
            }
            ActionBarView.this.mHomeLayout.setVisibility(8);
            if (ActionBarView.this.mTitleLayout != null) {
                ActionBarView.this.mTitleLayout.setVisibility(8);
            }
            if (ActionBarView.this.mTabScrollView != null) {
                ActionBarView.this.mTabScrollView.setVisibility(8);
            }
            if (ActionBarView.this.mSpinner != null) {
                ActionBarView.this.mSpinner.setVisibility(8);
            }
            if (ActionBarView.this.mCustomNavView != null) {
                ActionBarView.this.mCustomNavView.setVisibility(8);
            }
            ActionBarView.this.setHomeButtonEnabled(false, false);
            ActionBarView.this.requestLayout();
            menuItemImpl.setActionViewExpanded(true);
            if (ActionBarView.this.mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) ActionBarView.this.mExpandedActionView).onActionViewExpanded();
            }
            return true;
        }

        @Override
        public boolean collapseItemActionView(MenuBuilder menuBuilder, MenuItemImpl menuItemImpl) {
            if (ActionBarView.this.mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) ActionBarView.this.mExpandedActionView).onActionViewCollapsed();
            }
            ActionBarView.this.removeView(ActionBarView.this.mExpandedActionView);
            ActionBarView.this.mUpGoerFive.removeView(ActionBarView.this.mExpandedHomeLayout);
            ActionBarView.this.mExpandedActionView = null;
            if ((ActionBarView.this.mDisplayOptions & 2) != 0) {
                ActionBarView.this.mHomeLayout.setVisibility(0);
            }
            if ((ActionBarView.this.mDisplayOptions & 8) != 0) {
                if (ActionBarView.this.mTitleLayout == null) {
                    ActionBarView.this.initTitle();
                } else {
                    ActionBarView.this.mTitleLayout.setVisibility(0);
                }
            }
            if (ActionBarView.this.mTabScrollView != null) {
                ActionBarView.this.mTabScrollView.setVisibility(0);
            }
            if (ActionBarView.this.mSpinner != null) {
                ActionBarView.this.mSpinner.setVisibility(0);
            }
            if (ActionBarView.this.mCustomNavView != null) {
                ActionBarView.this.mCustomNavView.setVisibility(0);
            }
            ActionBarView.this.mExpandedHomeLayout.setIcon(null);
            this.mCurrentExpandedItem = null;
            ActionBarView.this.setHomeButtonEnabled(ActionBarView.this.mWasHomeEnabled);
            ActionBarView.this.requestLayout();
            menuItemImpl.setActionViewExpanded(false);
            return true;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            return null;
        }

        @Override
        public void onRestoreInstanceState(Parcelable parcelable) {
        }
    }
}
