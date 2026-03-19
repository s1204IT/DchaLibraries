package android.support.v7.view.menu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.v7.view.menu.MenuBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class SubMenuBuilder extends MenuBuilder implements SubMenu {
    private MenuItemImpl mItem;
    private MenuBuilder mParentMenu;

    public SubMenuBuilder(Context context, MenuBuilder parentMenu, MenuItemImpl item) {
        super(context);
        this.mParentMenu = parentMenu;
        this.mItem = item;
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        this.mParentMenu.setQwertyMode(isQwerty);
    }

    @Override
    public boolean isQwertyMode() {
        return this.mParentMenu.isQwertyMode();
    }

    @Override
    public void setShortcutsVisible(boolean shortcutsVisible) {
        this.mParentMenu.setShortcutsVisible(shortcutsVisible);
    }

    @Override
    public boolean isShortcutsVisible() {
        return this.mParentMenu.isShortcutsVisible();
    }

    public Menu getParentMenu() {
        return this.mParentMenu;
    }

    @Override
    public MenuItem getItem() {
        return this.mItem;
    }

    @Override
    public void setCallback(MenuBuilder.Callback callback) {
        this.mParentMenu.setCallback(callback);
    }

    @Override
    public MenuBuilder getRootMenu() {
        return this.mParentMenu.getRootMenu();
    }

    @Override
    boolean dispatchMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return super.dispatchMenuItemSelected(menu, item) || this.mParentMenu.dispatchMenuItemSelected(menu, item);
    }

    @Override
    public SubMenu setIcon(Drawable icon) {
        this.mItem.setIcon(icon);
        return this;
    }

    @Override
    public SubMenu setIcon(int iconRes) {
        this.mItem.setIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        return (SubMenu) super.setHeaderIconInt(icon);
    }

    @Override
    public SubMenu setHeaderIcon(int iconRes) {
        return (SubMenu) super.setHeaderIconInt(iconRes);
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        return (SubMenu) super.setHeaderTitleInt(title);
    }

    @Override
    public SubMenu setHeaderTitle(int titleRes) {
        return (SubMenu) super.setHeaderTitleInt(titleRes);
    }

    @Override
    public SubMenu setHeaderView(View view) {
        return (SubMenu) super.setHeaderViewInt(view);
    }

    @Override
    public boolean expandItemActionView(MenuItemImpl item) {
        return this.mParentMenu.expandItemActionView(item);
    }

    @Override
    public boolean collapseItemActionView(MenuItemImpl item) {
        return this.mParentMenu.collapseItemActionView(item);
    }

    @Override
    public String getActionViewStatesKey() {
        int itemId = this.mItem != null ? this.mItem.getItemId() : 0;
        if (itemId == 0) {
            return null;
        }
        return super.getActionViewStatesKey() + ":" + itemId;
    }

    @Override
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        this.mParentMenu.setGroupDividerEnabled(groupDividerEnabled);
    }

    @Override
    public boolean isGroupDividerEnabled() {
        return this.mParentMenu.isGroupDividerEnabled();
    }
}
