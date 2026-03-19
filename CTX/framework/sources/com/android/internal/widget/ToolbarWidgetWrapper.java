package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Property;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ActionMenuPresenter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toolbar;
import com.android.internal.R;
import com.android.internal.view.menu.ActionMenuItem;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPresenter;

public class ToolbarWidgetWrapper implements DecorToolbar {
    private static final int AFFECTS_LOGO_MASK = 3;
    private static final long DEFAULT_FADE_DURATION_MS = 200;
    private static final String TAG = "ToolbarWidgetWrapper";
    private ActionMenuPresenter mActionMenuPresenter;
    private View mCustomView;
    private int mDefaultNavigationContentDescription;
    private Drawable mDefaultNavigationIcon;
    private int mDisplayOpts;
    private CharSequence mHomeDescription;
    private Drawable mIcon;
    private Drawable mLogo;
    private boolean mMenuPrepared;
    private Drawable mNavIcon;
    private int mNavigationMode;
    private Spinner mSpinner;
    private CharSequence mSubtitle;
    private View mTabView;
    private CharSequence mTitle;
    private boolean mTitleSet;
    private Toolbar mToolbar;
    private Window.Callback mWindowCallback;

    public ToolbarWidgetWrapper(Toolbar toolbar, boolean z) {
        this(toolbar, z, R.string.action_bar_up_description);
    }

    public ToolbarWidgetWrapper(Toolbar toolbar, boolean z, int i) {
        this.mNavigationMode = 0;
        this.mDefaultNavigationContentDescription = 0;
        this.mToolbar = toolbar;
        this.mTitle = toolbar.getTitle();
        this.mSubtitle = toolbar.getSubtitle();
        this.mTitleSet = this.mTitle != null;
        this.mNavIcon = this.mToolbar.getNavigationIcon();
        TypedArray typedArrayObtainStyledAttributes = toolbar.getContext().obtainStyledAttributes(null, R.styleable.ActionBar, 16843470, 0);
        this.mDefaultNavigationIcon = typedArrayObtainStyledAttributes.getDrawable(13);
        if (z) {
            CharSequence text = typedArrayObtainStyledAttributes.getText(5);
            if (!TextUtils.isEmpty(text)) {
                setTitle(text);
            }
            CharSequence text2 = typedArrayObtainStyledAttributes.getText(9);
            if (!TextUtils.isEmpty(text2)) {
                setSubtitle(text2);
            }
            Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(6);
            if (drawable != null) {
                setLogo(drawable);
            }
            Drawable drawable2 = typedArrayObtainStyledAttributes.getDrawable(0);
            if (drawable2 != null) {
                setIcon(drawable2);
            }
            if (this.mNavIcon == null && this.mDefaultNavigationIcon != null) {
                setNavigationIcon(this.mDefaultNavigationIcon);
            }
            setDisplayOptions(typedArrayObtainStyledAttributes.getInt(8, 0));
            int resourceId = typedArrayObtainStyledAttributes.getResourceId(10, 0);
            if (resourceId != 0) {
                setCustomView(LayoutInflater.from(this.mToolbar.getContext()).inflate(resourceId, (ViewGroup) this.mToolbar, false));
                setDisplayOptions(this.mDisplayOpts | 16);
            }
            int layoutDimension = typedArrayObtainStyledAttributes.getLayoutDimension(4, 0);
            if (layoutDimension > 0) {
                ViewGroup.LayoutParams layoutParams = this.mToolbar.getLayoutParams();
                layoutParams.height = layoutDimension;
                this.mToolbar.setLayoutParams(layoutParams);
            }
            int dimensionPixelOffset = typedArrayObtainStyledAttributes.getDimensionPixelOffset(22, -1);
            int dimensionPixelOffset2 = typedArrayObtainStyledAttributes.getDimensionPixelOffset(23, -1);
            if (dimensionPixelOffset >= 0 || dimensionPixelOffset2 >= 0) {
                this.mToolbar.setContentInsetsRelative(Math.max(dimensionPixelOffset, 0), Math.max(dimensionPixelOffset2, 0));
            }
            int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(11, 0);
            if (resourceId2 != 0) {
                this.mToolbar.setTitleTextAppearance(this.mToolbar.getContext(), resourceId2);
            }
            int resourceId3 = typedArrayObtainStyledAttributes.getResourceId(12, 0);
            if (resourceId3 != 0) {
                this.mToolbar.setSubtitleTextAppearance(this.mToolbar.getContext(), resourceId3);
            }
            int resourceId4 = typedArrayObtainStyledAttributes.getResourceId(26, 0);
            if (resourceId4 != 0) {
                this.mToolbar.setPopupTheme(resourceId4);
            }
        } else {
            this.mDisplayOpts = detectDisplayOptions();
        }
        typedArrayObtainStyledAttributes.recycle();
        setDefaultNavigationContentDescription(i);
        this.mHomeDescription = this.mToolbar.getNavigationContentDescription();
        this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            final ActionMenuItem mNavItem;

            {
                this.mNavItem = new ActionMenuItem(ToolbarWidgetWrapper.this.mToolbar.getContext(), 0, 16908332, 0, 0, ToolbarWidgetWrapper.this.mTitle);
            }

            @Override
            public void onClick(View view) {
                if (ToolbarWidgetWrapper.this.mWindowCallback != null && ToolbarWidgetWrapper.this.mMenuPrepared) {
                    ToolbarWidgetWrapper.this.mWindowCallback.onMenuItemSelected(0, this.mNavItem);
                }
            }
        });
    }

    @Override
    public void setDefaultNavigationContentDescription(int i) {
        if (i == this.mDefaultNavigationContentDescription) {
            return;
        }
        this.mDefaultNavigationContentDescription = i;
        if (TextUtils.isEmpty(this.mToolbar.getNavigationContentDescription())) {
            setNavigationContentDescription(this.mDefaultNavigationContentDescription);
        }
    }

    private int detectDisplayOptions() {
        if (this.mToolbar.getNavigationIcon() != null) {
            this.mDefaultNavigationIcon = this.mToolbar.getNavigationIcon();
            return 15;
        }
        return 11;
    }

    @Override
    public ViewGroup getViewGroup() {
        return this.mToolbar;
    }

    @Override
    public Context getContext() {
        return this.mToolbar.getContext();
    }

    @Override
    public boolean isSplit() {
        return false;
    }

    @Override
    public boolean hasExpandedActionView() {
        return this.mToolbar.hasExpandedActionView();
    }

    @Override
    public void collapseActionView() {
        this.mToolbar.collapseActionView();
    }

    @Override
    public void setWindowCallback(Window.Callback callback) {
        this.mWindowCallback = callback;
    }

    @Override
    public void setWindowTitle(CharSequence charSequence) {
        if (!this.mTitleSet) {
            setTitleInt(charSequence);
        }
    }

    @Override
    public CharSequence getTitle() {
        return this.mToolbar.getTitle();
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mTitleSet = true;
        setTitleInt(charSequence);
    }

    private void setTitleInt(CharSequence charSequence) {
        this.mTitle = charSequence;
        if ((this.mDisplayOpts & 8) != 0) {
            this.mToolbar.setTitle(charSequence);
        }
    }

    @Override
    public CharSequence getSubtitle() {
        return this.mToolbar.getSubtitle();
    }

    @Override
    public void setSubtitle(CharSequence charSequence) {
        this.mSubtitle = charSequence;
        if ((this.mDisplayOpts & 8) != 0) {
            this.mToolbar.setSubtitle(charSequence);
        }
    }

    @Override
    public void initProgress() {
        Log.i(TAG, "Progress display unsupported");
    }

    @Override
    public void initIndeterminateProgress() {
        Log.i(TAG, "Progress display unsupported");
    }

    @Override
    public boolean canSplit() {
        return false;
    }

    @Override
    public void setSplitView(ViewGroup viewGroup) {
    }

    @Override
    public void setSplitToolbar(boolean z) {
        if (z) {
            throw new UnsupportedOperationException("Cannot split an android.widget.Toolbar");
        }
    }

    @Override
    public void setSplitWhenNarrow(boolean z) {
    }

    @Override
    public boolean hasIcon() {
        return this.mIcon != null;
    }

    @Override
    public boolean hasLogo() {
        return this.mLogo != null;
    }

    @Override
    public void setIcon(int i) {
        setIcon(i != 0 ? getContext().getDrawable(i) : null);
    }

    @Override
    public void setIcon(Drawable drawable) {
        this.mIcon = drawable;
        updateToolbarLogo();
    }

    @Override
    public void setLogo(int i) {
        setLogo(i != 0 ? getContext().getDrawable(i) : null);
    }

    @Override
    public void setLogo(Drawable drawable) {
        this.mLogo = drawable;
        updateToolbarLogo();
    }

    private void updateToolbarLogo() {
        Drawable drawable;
        if ((this.mDisplayOpts & 2) != 0) {
            drawable = ((this.mDisplayOpts & 1) == 0 || this.mLogo == null) ? this.mIcon : this.mLogo;
        } else {
            drawable = null;
        }
        this.mToolbar.setLogo(drawable);
    }

    @Override
    public boolean canShowOverflowMenu() {
        return this.mToolbar.canShowOverflowMenu();
    }

    @Override
    public boolean isOverflowMenuShowing() {
        return this.mToolbar.isOverflowMenuShowing();
    }

    @Override
    public boolean isOverflowMenuShowPending() {
        return this.mToolbar.isOverflowMenuShowPending();
    }

    @Override
    public boolean showOverflowMenu() {
        return this.mToolbar.showOverflowMenu();
    }

    @Override
    public boolean hideOverflowMenu() {
        return this.mToolbar.hideOverflowMenu();
    }

    @Override
    public void setMenuPrepared() {
        this.mMenuPrepared = true;
    }

    @Override
    public void setMenu(Menu menu, MenuPresenter.Callback callback) {
        if (this.mActionMenuPresenter == null) {
            this.mActionMenuPresenter = new ActionMenuPresenter(this.mToolbar.getContext());
            this.mActionMenuPresenter.setId(R.id.action_menu_presenter);
        }
        this.mActionMenuPresenter.setCallback(callback);
        this.mToolbar.setMenu((MenuBuilder) menu, this.mActionMenuPresenter);
    }

    @Override
    public void dismissPopupMenus() {
        this.mToolbar.dismissPopupMenus();
    }

    @Override
    public int getDisplayOptions() {
        return this.mDisplayOpts;
    }

    @Override
    public void setDisplayOptions(int i) {
        int i2 = this.mDisplayOpts ^ i;
        this.mDisplayOpts = i;
        if (i2 != 0) {
            if ((i2 & 4) != 0) {
                if ((i & 4) != 0) {
                    updateHomeAccessibility();
                }
                updateNavigationIcon();
            }
            if ((i2 & 3) != 0) {
                updateToolbarLogo();
            }
            if ((i2 & 8) != 0) {
                if ((i & 8) != 0) {
                    this.mToolbar.setTitle(this.mTitle);
                    this.mToolbar.setSubtitle(this.mSubtitle);
                } else {
                    this.mToolbar.setTitle((CharSequence) null);
                    this.mToolbar.setSubtitle((CharSequence) null);
                }
            }
            if ((i2 & 16) != 0 && this.mCustomView != null) {
                if ((i & 16) != 0) {
                    this.mToolbar.addView(this.mCustomView);
                } else {
                    this.mToolbar.removeView(this.mCustomView);
                }
            }
        }
    }

    @Override
    public void setEmbeddedTabView(ScrollingTabContainerView scrollingTabContainerView) {
        if (this.mTabView != null && this.mTabView.getParent() == this.mToolbar) {
            this.mToolbar.removeView(this.mTabView);
        }
        this.mTabView = scrollingTabContainerView;
        if (scrollingTabContainerView != null && this.mNavigationMode == 2) {
            this.mToolbar.addView(this.mTabView, 0);
            Toolbar.LayoutParams layoutParams = (Toolbar.LayoutParams) this.mTabView.getLayoutParams();
            layoutParams.width = -2;
            layoutParams.height = -2;
            layoutParams.gravity = 8388691;
            scrollingTabContainerView.setAllowCollapse(true);
        }
    }

    @Override
    public boolean hasEmbeddedTabs() {
        return this.mTabView != null;
    }

    @Override
    public boolean isTitleTruncated() {
        return this.mToolbar.isTitleTruncated();
    }

    @Override
    public void setCollapsible(boolean z) {
        this.mToolbar.setCollapsible(z);
    }

    @Override
    public void setHomeButtonEnabled(boolean z) {
    }

    @Override
    public int getNavigationMode() {
        return this.mNavigationMode;
    }

    @Override
    public void setNavigationMode(int i) {
        int i2 = this.mNavigationMode;
        if (i != i2) {
            switch (i2) {
                case 1:
                    if (this.mSpinner != null && this.mSpinner.getParent() == this.mToolbar) {
                        this.mToolbar.removeView(this.mSpinner);
                    }
                    break;
                case 2:
                    if (this.mTabView != null && this.mTabView.getParent() == this.mToolbar) {
                        this.mToolbar.removeView(this.mTabView);
                    }
                    break;
            }
            this.mNavigationMode = i;
            switch (i) {
                case 0:
                    return;
                case 1:
                    ensureSpinner();
                    this.mToolbar.addView(this.mSpinner, 0);
                    return;
                case 2:
                    if (this.mTabView != null) {
                        this.mToolbar.addView(this.mTabView, 0);
                        Toolbar.LayoutParams layoutParams = (Toolbar.LayoutParams) this.mTabView.getLayoutParams();
                        layoutParams.width = -2;
                        layoutParams.height = -2;
                        layoutParams.gravity = 8388691;
                        return;
                    }
                    return;
                default:
                    throw new IllegalArgumentException("Invalid navigation mode " + i);
            }
        }
    }

    private void ensureSpinner() {
        if (this.mSpinner == null) {
            this.mSpinner = new Spinner(getContext(), null, 16843479);
            this.mSpinner.setLayoutParams(new Toolbar.LayoutParams(-2, -2, 8388627));
        }
    }

    @Override
    public void setDropdownParams(SpinnerAdapter spinnerAdapter, AdapterView.OnItemSelectedListener onItemSelectedListener) {
        ensureSpinner();
        this.mSpinner.setAdapter(spinnerAdapter);
        this.mSpinner.setOnItemSelectedListener(onItemSelectedListener);
    }

    @Override
    public void setDropdownSelectedPosition(int i) {
        if (this.mSpinner == null) {
            throw new IllegalStateException("Can't set dropdown selected position without an adapter");
        }
        this.mSpinner.setSelection(i);
    }

    @Override
    public int getDropdownSelectedPosition() {
        if (this.mSpinner != null) {
            return this.mSpinner.getSelectedItemPosition();
        }
        return 0;
    }

    @Override
    public int getDropdownItemCount() {
        if (this.mSpinner != null) {
            return this.mSpinner.getCount();
        }
        return 0;
    }

    @Override
    public void setCustomView(View view) {
        if (this.mCustomView != null && (this.mDisplayOpts & 16) != 0) {
            this.mToolbar.removeView(this.mCustomView);
        }
        this.mCustomView = view;
        if (view != null && (this.mDisplayOpts & 16) != 0) {
            this.mToolbar.addView(this.mCustomView);
        }
    }

    @Override
    public View getCustomView() {
        return this.mCustomView;
    }

    @Override
    public void animateToVisibility(int i) {
        Animator animator = setupAnimatorToVisibility(i, DEFAULT_FADE_DURATION_MS);
        if (animator != null) {
            animator.start();
        }
    }

    @Override
    public Animator setupAnimatorToVisibility(int i, long j) {
        if (i == 8) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mToolbar, (Property<Toolbar, Float>) View.ALPHA, 1.0f, 0.0f);
            objectAnimatorOfFloat.setDuration(j);
            objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                private boolean mCanceled = false;

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (!this.mCanceled) {
                        ToolbarWidgetWrapper.this.mToolbar.setVisibility(8);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    this.mCanceled = true;
                }
            });
            return objectAnimatorOfFloat;
        }
        if (i == 0) {
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mToolbar, (Property<Toolbar, Float>) View.ALPHA, 0.0f, 1.0f);
            objectAnimatorOfFloat2.setDuration(j);
            objectAnimatorOfFloat2.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    ToolbarWidgetWrapper.this.mToolbar.setVisibility(0);
                }
            });
            return objectAnimatorOfFloat2;
        }
        return null;
    }

    @Override
    public void setNavigationIcon(Drawable drawable) {
        this.mNavIcon = drawable;
        updateNavigationIcon();
    }

    @Override
    public void setNavigationIcon(int i) {
        setNavigationIcon(i != 0 ? this.mToolbar.getContext().getDrawable(i) : null);
    }

    @Override
    public void setDefaultNavigationIcon(Drawable drawable) {
        if (this.mDefaultNavigationIcon != drawable) {
            this.mDefaultNavigationIcon = drawable;
            updateNavigationIcon();
        }
    }

    private void updateNavigationIcon() {
        if ((this.mDisplayOpts & 4) != 0) {
            this.mToolbar.setNavigationIcon(this.mNavIcon != null ? this.mNavIcon : this.mDefaultNavigationIcon);
        } else {
            this.mToolbar.setNavigationIcon((Drawable) null);
        }
    }

    @Override
    public void setNavigationContentDescription(CharSequence charSequence) {
        this.mHomeDescription = charSequence;
        updateHomeAccessibility();
    }

    @Override
    public void setNavigationContentDescription(int i) {
        setNavigationContentDescription(i == 0 ? null : getContext().getString(i));
    }

    private void updateHomeAccessibility() {
        if ((this.mDisplayOpts & 4) != 0) {
            if (TextUtils.isEmpty(this.mHomeDescription)) {
                this.mToolbar.setNavigationContentDescription(this.mDefaultNavigationContentDescription);
            } else {
                this.mToolbar.setNavigationContentDescription(this.mHomeDescription);
            }
        }
    }

    @Override
    public void saveHierarchyState(SparseArray<Parcelable> sparseArray) {
        this.mToolbar.saveHierarchyState(sparseArray);
    }

    @Override
    public void restoreHierarchyState(SparseArray<Parcelable> sparseArray) {
        this.mToolbar.restoreHierarchyState(sparseArray);
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        this.mToolbar.setBackgroundDrawable(drawable);
    }

    @Override
    public int getHeight() {
        return this.mToolbar.getHeight();
    }

    @Override
    public void setVisibility(int i) {
        this.mToolbar.setVisibility(i);
    }

    @Override
    public int getVisibility() {
        return this.mToolbar.getVisibility();
    }

    @Override
    public void setMenuCallbacks(MenuPresenter.Callback callback, MenuBuilder.Callback callback2) {
        this.mToolbar.setMenuCallbacks(callback, callback2);
    }

    @Override
    public Menu getMenu() {
        return this.mToolbar.getMenu();
    }
}
