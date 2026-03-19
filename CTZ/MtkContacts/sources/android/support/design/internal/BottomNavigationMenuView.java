package android.support.design.internal;

import android.content.res.ColorStateList;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v4.util.Pools;
import android.support.v4.view.ViewCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class BottomNavigationMenuView extends ViewGroup implements MenuView {
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private static final int[] DISABLED_STATE_SET = {-16842910};
    private final int activeItemMaxWidth;
    private final int activeItemMinWidth;
    private BottomNavigationItemView[] buttons;
    private final int inactiveItemMaxWidth;
    private final int inactiveItemMinWidth;
    private int itemBackgroundRes;
    private final int itemHeight;
    private boolean itemHorizontalTranslation;
    private int itemIconSize;
    private ColorStateList itemIconTint;
    private final Pools.Pool<BottomNavigationItemView> itemPool;
    private int itemTextAppearanceActive;
    private int itemTextAppearanceInactive;
    private final ColorStateList itemTextColorDefault;
    private ColorStateList itemTextColorFromUser;
    private int labelVisibilityMode;
    private MenuBuilder menu;
    private final View.OnClickListener onClickListener;
    private BottomNavigationPresenter presenter;
    private int selectedItemId;
    private int selectedItemPosition;
    private final TransitionSet set;
    private int[] tempChildWidths;

    @Override
    public void initialize(MenuBuilder menu) {
        this.menu = menu;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int visibleCount = this.menu.getVisibleItems().size();
        int totalCount = getChildCount();
        int heightSpec = View.MeasureSpec.makeMeasureSpec(this.itemHeight, 1073741824);
        int i = 8;
        if (isShifting(this.labelVisibilityMode, visibleCount) && this.itemHorizontalTranslation) {
            View activeChild = getChildAt(this.selectedItemPosition);
            int activeItemWidth = this.activeItemMinWidth;
            if (activeChild.getVisibility() != 8) {
                activeChild.measure(View.MeasureSpec.makeMeasureSpec(this.activeItemMaxWidth, Integer.MIN_VALUE), heightSpec);
                activeItemWidth = Math.max(activeItemWidth, activeChild.getMeasuredWidth());
            }
            int inactiveCount = visibleCount - (activeChild.getVisibility() != 8 ? 1 : 0);
            int activeMaxAvailable = width - (this.inactiveItemMinWidth * inactiveCount);
            int activeWidth = Math.min(activeMaxAvailable, Math.min(activeItemWidth, this.activeItemMaxWidth));
            int inactiveMaxAvailable = (width - activeWidth) / (inactiveCount == 0 ? 1 : inactiveCount);
            int inactiveWidth = Math.min(inactiveMaxAvailable, this.inactiveItemMaxWidth);
            int extra = (width - activeWidth) - (inactiveWidth * inactiveCount);
            int extra2 = extra;
            int extra3 = 0;
            while (true) {
                int i2 = extra3;
                if (i2 >= totalCount) {
                    break;
                }
                if (getChildAt(i2).getVisibility() != i) {
                    this.tempChildWidths[i2] = i2 == this.selectedItemPosition ? activeWidth : inactiveWidth;
                    if (extra2 > 0) {
                        int[] iArr = this.tempChildWidths;
                        iArr[i2] = iArr[i2] + 1;
                        extra2--;
                    }
                } else {
                    this.tempChildWidths[i2] = 0;
                }
                extra3 = i2 + 1;
                i = 8;
            }
        } else {
            int maxAvailable = width / (visibleCount == 0 ? 1 : visibleCount);
            int childWidth = Math.min(maxAvailable, this.activeItemMaxWidth);
            int extra4 = width - (childWidth * visibleCount);
            int extra5 = extra4;
            for (int extra6 = 0; extra6 < totalCount; extra6++) {
                if (getChildAt(extra6).getVisibility() != 8) {
                    this.tempChildWidths[extra6] = childWidth;
                    if (extra5 > 0) {
                        int[] iArr2 = this.tempChildWidths;
                        iArr2[extra6] = iArr2[extra6] + 1;
                        extra5--;
                    }
                } else {
                    this.tempChildWidths[extra6] = 0;
                }
            }
        }
        int totalWidth = 0;
        for (int totalWidth2 = 0; totalWidth2 < totalCount; totalWidth2++) {
            View child = getChildAt(totalWidth2);
            if (child.getVisibility() != 8) {
                child.measure(View.MeasureSpec.makeMeasureSpec(this.tempChildWidths[totalWidth2], 1073741824), heightSpec);
                ViewGroup.LayoutParams params = child.getLayoutParams();
                params.width = child.getMeasuredWidth();
                totalWidth += child.getMeasuredWidth();
            }
        }
        setMeasuredDimension(View.resolveSizeAndState(totalWidth, View.MeasureSpec.makeMeasureSpec(totalWidth, 1073741824), 0), View.resolveSizeAndState(this.itemHeight, heightSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();
        int width = right - left;
        int height = bottom - top;
        int used = 0;
        for (int used2 = 0; used2 < count; used2++) {
            View child = getChildAt(used2);
            if (child.getVisibility() != 8) {
                if (ViewCompat.getLayoutDirection(this) == 1) {
                    child.layout((width - used) - child.getMeasuredWidth(), 0, width - used, height);
                } else {
                    child.layout(used, 0, child.getMeasuredWidth() + used, height);
                }
                used += child.getMeasuredWidth();
            }
        }
    }

    public void buildMenuView() {
        removeAllViews();
        if (this.buttons != null) {
            for (BottomNavigationItemView item : this.buttons) {
                if (item != null) {
                    this.itemPool.release(item);
                }
            }
        }
        if (this.menu.size() == 0) {
            this.selectedItemId = 0;
            this.selectedItemPosition = 0;
            this.buttons = null;
            return;
        }
        this.buttons = new BottomNavigationItemView[this.menu.size()];
        boolean shifting = isShifting(this.labelVisibilityMode, this.menu.getVisibleItems().size());
        for (int i = 0; i < this.menu.size(); i++) {
            this.presenter.setUpdateSuspended(true);
            this.menu.getItem(i).setCheckable(true);
            this.presenter.setUpdateSuspended(false);
            BottomNavigationItemView child = getNewItem();
            this.buttons[i] = child;
            child.setIconTintList(this.itemIconTint);
            child.setIconSize(this.itemIconSize);
            child.setTextColor(this.itemTextColorDefault);
            child.setTextAppearanceInactive(this.itemTextAppearanceInactive);
            child.setTextAppearanceActive(this.itemTextAppearanceActive);
            child.setTextColor(this.itemTextColorFromUser);
            child.setItemBackground(this.itemBackgroundRes);
            child.setShifting(shifting);
            child.setLabelVisibilityMode(this.labelVisibilityMode);
            child.initialize((MenuItemImpl) this.menu.getItem(i), 0);
            child.setItemPosition(i);
            child.setOnClickListener(this.onClickListener);
            addView(child);
        }
        this.selectedItemPosition = Math.min(this.menu.size() - 1, this.selectedItemPosition);
        this.menu.getItem(this.selectedItemPosition).setChecked(true);
    }

    public void updateMenuView() {
        if (this.menu == null || this.buttons == null) {
            return;
        }
        int menuSize = this.menu.size();
        if (menuSize != this.buttons.length) {
            buildMenuView();
            return;
        }
        int previousSelectedId = this.selectedItemId;
        for (int i = 0; i < menuSize; i++) {
            MenuItem item = this.menu.getItem(i);
            if (item.isChecked()) {
                this.selectedItemId = item.getItemId();
                this.selectedItemPosition = i;
            }
        }
        int i2 = this.selectedItemId;
        if (previousSelectedId != i2) {
            TransitionManager.beginDelayedTransition(this, this.set);
        }
        boolean shifting = isShifting(this.labelVisibilityMode, this.menu.getVisibleItems().size());
        for (int i3 = 0; i3 < menuSize; i3++) {
            this.presenter.setUpdateSuspended(true);
            this.buttons[i3].setLabelVisibilityMode(this.labelVisibilityMode);
            this.buttons[i3].setShifting(shifting);
            this.buttons[i3].initialize((MenuItemImpl) this.menu.getItem(i3), 0);
            this.presenter.setUpdateSuspended(false);
        }
    }

    private BottomNavigationItemView getNewItem() {
        BottomNavigationItemView item = this.itemPool.acquire();
        if (item == null) {
            return new BottomNavigationItemView(getContext());
        }
        return item;
    }

    public int getSelectedItemId() {
        return this.selectedItemId;
    }

    private boolean isShifting(int labelVisibilityMode, int childCount) {
        if (labelVisibilityMode == -1) {
            if (childCount <= 3) {
                return false;
            }
        } else if (labelVisibilityMode != 0) {
            return false;
        }
        return true;
    }

    void tryRestoreSelectedItemId(int itemId) {
        int size = this.menu.size();
        for (int i = 0; i < size; i++) {
            MenuItem item = this.menu.getItem(i);
            if (itemId == item.getItemId()) {
                this.selectedItemId = itemId;
                this.selectedItemPosition = i;
                item.setChecked(true);
                return;
            }
        }
    }
}
