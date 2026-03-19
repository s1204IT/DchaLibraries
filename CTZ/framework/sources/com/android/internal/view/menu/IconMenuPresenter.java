package com.android.internal.view.menu;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.view.menu.MenuView;
import java.util.ArrayList;

public class IconMenuPresenter extends BaseMenuPresenter {
    private static final String OPEN_SUBMENU_KEY = "android:menu:icon:submenu";
    private static final String VIEWS_TAG = "android:menu:icon";
    private int mMaxItems;
    private IconMenuItemView mMoreView;
    MenuDialogHelper mOpenSubMenu;
    int mOpenSubMenuId;
    SubMenuPresenterCallback mSubMenuPresenterCallback;

    public IconMenuPresenter(Context context) {
        super(new ContextThemeWrapper(context, R.style.Theme_IconMenu), R.layout.icon_menu_layout, R.layout.icon_menu_item_layout);
        this.mMaxItems = -1;
        this.mSubMenuPresenterCallback = new SubMenuPresenterCallback();
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menuBuilder) {
        super.initForMenu(context, menuBuilder);
        this.mMaxItems = -1;
    }

    @Override
    public void bindItemView(MenuItemImpl menuItemImpl, MenuView.ItemView itemView) {
        IconMenuItemView iconMenuItemView = (IconMenuItemView) itemView;
        iconMenuItemView.setItemData(menuItemImpl);
        iconMenuItemView.initialize(menuItemImpl.getTitleForItemView(iconMenuItemView), menuItemImpl.getIcon());
        iconMenuItemView.setVisibility(menuItemImpl.isVisible() ? 0 : 8);
        iconMenuItemView.setEnabled(iconMenuItemView.isEnabled());
        iconMenuItemView.setLayoutParams(iconMenuItemView.getTextAppropriateLayoutParams());
    }

    @Override
    public boolean shouldIncludeItem(int i, MenuItemImpl menuItemImpl) {
        return ((this.mMenu.getNonActionItems().size() == this.mMaxItems && i < this.mMaxItems) || i < this.mMaxItems - 1) && !menuItemImpl.isActionButton();
    }

    @Override
    protected void addItemView(View view, int i) {
        IconMenuItemView iconMenuItemView = (IconMenuItemView) view;
        IconMenuView iconMenuView = (IconMenuView) this.mMenuView;
        iconMenuItemView.setIconMenuView(iconMenuView);
        iconMenuItemView.setItemInvoker(iconMenuView);
        iconMenuItemView.setBackgroundDrawable(iconMenuView.getItemBackgroundDrawable());
        super.addItemView(view, i);
    }

    @Override
    public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
        if (!subMenuBuilder.hasVisibleItems()) {
            return false;
        }
        MenuDialogHelper menuDialogHelper = new MenuDialogHelper(subMenuBuilder);
        menuDialogHelper.setPresenterCallback(this.mSubMenuPresenterCallback);
        menuDialogHelper.show(null);
        this.mOpenSubMenu = menuDialogHelper;
        this.mOpenSubMenuId = subMenuBuilder.getItem().getItemId();
        super.onSubMenuSelected(subMenuBuilder);
        return true;
    }

    @Override
    public void updateMenuView(boolean z) {
        boolean z2;
        IconMenuView iconMenuView = (IconMenuView) this.mMenuView;
        if (this.mMaxItems < 0) {
            this.mMaxItems = iconMenuView.getMaxItems();
        }
        ArrayList<MenuItemImpl> nonActionItems = this.mMenu.getNonActionItems();
        if (nonActionItems.size() <= this.mMaxItems) {
            z2 = false;
        } else {
            z2 = true;
        }
        super.updateMenuView(z);
        if (z2 && (this.mMoreView == null || this.mMoreView.getParent() != iconMenuView)) {
            if (this.mMoreView == null) {
                this.mMoreView = iconMenuView.createMoreItemView();
                this.mMoreView.setBackgroundDrawable(iconMenuView.getItemBackgroundDrawable());
            }
            iconMenuView.addView(this.mMoreView);
        } else if (!z2 && this.mMoreView != null) {
            iconMenuView.removeView(this.mMoreView);
        }
        iconMenuView.setNumActualItemsShown(z2 ? this.mMaxItems - 1 : nonActionItems.size());
    }

    @Override
    protected boolean filterLeftoverView(ViewGroup viewGroup, int i) {
        if (viewGroup.getChildAt(i) != this.mMoreView) {
            return super.filterLeftoverView(viewGroup, i);
        }
        return false;
    }

    public int getNumActualItemsShown() {
        return ((IconMenuView) this.mMenuView).getNumActualItemsShown();
    }

    public void saveHierarchyState(Bundle bundle) {
        SparseArray<Parcelable> sparseArray = new SparseArray<>();
        if (this.mMenuView != null) {
            ((View) this.mMenuView).saveHierarchyState(sparseArray);
        }
        bundle.putSparseParcelableArray(VIEWS_TAG, sparseArray);
    }

    public void restoreHierarchyState(Bundle bundle) {
        MenuItem menuItemFindItem;
        SparseArray<Parcelable> sparseParcelableArray = bundle.getSparseParcelableArray(VIEWS_TAG);
        if (sparseParcelableArray != null) {
            ((View) this.mMenuView).restoreHierarchyState(sparseParcelableArray);
        }
        int i = bundle.getInt(OPEN_SUBMENU_KEY, 0);
        if (i > 0 && this.mMenu != null && (menuItemFindItem = this.mMenu.findItem(i)) != null) {
            onSubMenuSelected((SubMenuBuilder) menuItemFindItem.getSubMenu());
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (this.mMenuView == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        saveHierarchyState(bundle);
        if (this.mOpenSubMenuId > 0) {
            bundle.putInt(OPEN_SUBMENU_KEY, this.mOpenSubMenuId);
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        restoreHierarchyState((Bundle) parcelable);
    }

    class SubMenuPresenterCallback implements MenuPresenter.Callback {
        SubMenuPresenterCallback() {
        }

        @Override
        public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
            IconMenuPresenter.this.mOpenSubMenuId = 0;
            if (IconMenuPresenter.this.mOpenSubMenu != null) {
                IconMenuPresenter.this.mOpenSubMenu.dismiss();
                IconMenuPresenter.this.mOpenSubMenu = null;
            }
        }

        @Override
        public boolean onOpenSubMenu(MenuBuilder menuBuilder) {
            if (menuBuilder != null) {
                IconMenuPresenter.this.mOpenSubMenuId = ((SubMenuBuilder) menuBuilder).getItem().getItemId();
                return false;
            }
            return false;
        }
    }
}
