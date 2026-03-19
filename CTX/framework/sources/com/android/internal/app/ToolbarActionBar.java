package com.android.internal.app;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.ActionMode;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowCallbackWrapper;
import android.widget.SpinnerAdapter;
import android.widget.Toolbar;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.widget.DecorToolbar;
import com.android.internal.widget.ToolbarWidgetWrapper;
import java.util.ArrayList;

public class ToolbarActionBar extends ActionBar {
    private DecorToolbar mDecorToolbar;
    private boolean mLastMenuVisibility;
    private boolean mMenuCallbackSet;
    private boolean mToolbarMenuPrepared;
    private Window.Callback mWindowCallback;
    private ArrayList<ActionBar.OnMenuVisibilityListener> mMenuVisibilityListeners = new ArrayList<>();
    private final Runnable mMenuInvalidator = new Runnable() {
        @Override
        public void run() {
            ToolbarActionBar.this.populateOptionsMenu();
        }
    };
    private final Toolbar.OnMenuItemClickListener mMenuClicker = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return ToolbarActionBar.this.mWindowCallback.onMenuItemSelected(0, menuItem);
        }
    };

    public ToolbarActionBar(Toolbar toolbar, CharSequence charSequence, Window.Callback callback) {
        this.mDecorToolbar = new ToolbarWidgetWrapper(toolbar, false);
        this.mWindowCallback = new ToolbarCallbackWrapper(callback);
        this.mDecorToolbar.setWindowCallback(this.mWindowCallback);
        toolbar.setOnMenuItemClickListener(this.mMenuClicker);
        this.mDecorToolbar.setWindowTitle(charSequence);
    }

    public Window.Callback getWrappedWindowCallback() {
        return this.mWindowCallback;
    }

    @Override
    public void setCustomView(View view) {
        setCustomView(view, new ActionBar.LayoutParams(-2, -2));
    }

    @Override
    public void setCustomView(View view, ActionBar.LayoutParams layoutParams) {
        if (view != null) {
            view.setLayoutParams(layoutParams);
        }
        this.mDecorToolbar.setCustomView(view);
    }

    @Override
    public void setCustomView(int i) {
        setCustomView(LayoutInflater.from(this.mDecorToolbar.getContext()).inflate(i, this.mDecorToolbar.getViewGroup(), false));
    }

    @Override
    public void setIcon(int i) {
        this.mDecorToolbar.setIcon(i);
    }

    @Override
    public void setIcon(Drawable drawable) {
        this.mDecorToolbar.setIcon(drawable);
    }

    @Override
    public void setLogo(int i) {
        this.mDecorToolbar.setLogo(i);
    }

    @Override
    public void setLogo(Drawable drawable) {
        this.mDecorToolbar.setLogo(drawable);
    }

    @Override
    public void setStackedBackgroundDrawable(Drawable drawable) {
    }

    @Override
    public void setSplitBackgroundDrawable(Drawable drawable) {
    }

    @Override
    public void setHomeButtonEnabled(boolean z) {
    }

    @Override
    public void setElevation(float f) {
        this.mDecorToolbar.getViewGroup().setElevation(f);
    }

    @Override
    public float getElevation() {
        return this.mDecorToolbar.getViewGroup().getElevation();
    }

    @Override
    public Context getThemedContext() {
        return this.mDecorToolbar.getContext();
    }

    @Override
    public boolean isTitleTruncated() {
        return super.isTitleTruncated();
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
    public void setDefaultDisplayHomeAsUpEnabled(boolean z) {
    }

    @Override
    public void setHomeActionContentDescription(int i) {
        this.mDecorToolbar.setNavigationContentDescription(i);
    }

    @Override
    public void setShowHideAnimationEnabled(boolean z) {
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return null;
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter spinnerAdapter, ActionBar.OnNavigationListener onNavigationListener) {
        this.mDecorToolbar.setDropdownParams(spinnerAdapter, new NavItemSelectedListener(onNavigationListener));
    }

    @Override
    public void setSelectedNavigationItem(int i) {
        if (this.mDecorToolbar.getNavigationMode() == 1) {
            this.mDecorToolbar.setDropdownSelectedPosition(i);
            return;
        }
        throw new IllegalStateException("setSelectedNavigationIndex not valid for current navigation mode");
    }

    @Override
    public int getSelectedNavigationIndex() {
        return -1;
    }

    @Override
    public int getNavigationItemCount() {
        return 0;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mDecorToolbar.setTitle(charSequence);
    }

    @Override
    public void setTitle(int i) {
        this.mDecorToolbar.setTitle(i != 0 ? this.mDecorToolbar.getContext().getText(i) : null);
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
    public void setSubtitle(int i) {
        this.mDecorToolbar.setSubtitle(i != 0 ? this.mDecorToolbar.getContext().getText(i) : null);
    }

    @Override
    public void setDisplayOptions(int i) {
        setDisplayOptions(i, -1);
    }

    @Override
    public void setDisplayOptions(int i, int i2) {
        this.mDecorToolbar.setDisplayOptions((i & i2) | ((~i2) & this.mDecorToolbar.getDisplayOptions()));
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
    public void setBackgroundDrawable(Drawable drawable) {
        this.mDecorToolbar.setBackgroundDrawable(drawable);
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
        return 0;
    }

    @Override
    public void setNavigationMode(int i) {
        if (i == 2) {
            throw new IllegalArgumentException("Tabs not supported in this configuration");
        }
        this.mDecorToolbar.setNavigationMode(i);
    }

    @Override
    public int getDisplayOptions() {
        return this.mDecorToolbar.getDisplayOptions();
    }

    @Override
    public ActionBar.Tab newTab() {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab, boolean z) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab, int i) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab, int i, boolean z) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void removeTab(ActionBar.Tab tab) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void removeTabAt(int i) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void removeAllTabs() {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public void selectTab(ActionBar.Tab tab) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public ActionBar.Tab getSelectedTab() {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public ActionBar.Tab getTabAt(int i) {
        throw new UnsupportedOperationException("Tabs are not supported in toolbar action bars");
    }

    @Override
    public int getTabCount() {
        return 0;
    }

    @Override
    public int getHeight() {
        return this.mDecorToolbar.getHeight();
    }

    @Override
    public void show() {
        this.mDecorToolbar.setVisibility(0);
    }

    @Override
    public void hide() {
        this.mDecorToolbar.setVisibility(8);
    }

    @Override
    public boolean isShowing() {
        return this.mDecorToolbar.getVisibility() == 0;
    }

    @Override
    public boolean openOptionsMenu() {
        return this.mDecorToolbar.showOverflowMenu();
    }

    @Override
    public boolean closeOptionsMenu() {
        return this.mDecorToolbar.hideOverflowMenu();
    }

    @Override
    public boolean invalidateOptionsMenu() {
        this.mDecorToolbar.getViewGroup().removeCallbacks(this.mMenuInvalidator);
        this.mDecorToolbar.getViewGroup().postOnAnimation(this.mMenuInvalidator);
        return true;
    }

    @Override
    public boolean collapseActionView() {
        if (this.mDecorToolbar.hasExpandedActionView()) {
            this.mDecorToolbar.collapseActionView();
            return true;
        }
        return false;
    }

    void populateOptionsMenu() {
        if (!this.mMenuCallbackSet) {
            this.mDecorToolbar.setMenuCallbacks(new ActionMenuPresenterCallback(), new MenuBuilderCallback());
            this.mMenuCallbackSet = true;
        }
        Menu menu = this.mDecorToolbar.getMenu();
        MenuBuilder menuBuilder = menu instanceof MenuBuilder ? (MenuBuilder) menu : null;
        if (menuBuilder != null) {
            menuBuilder.stopDispatchingItemsChanged();
        }
        try {
            menu.clear();
            if (!this.mWindowCallback.onCreatePanelMenu(0, menu) || !this.mWindowCallback.onPreparePanel(0, null, menu)) {
                menu.clear();
            }
        } finally {
            if (menuBuilder != null) {
                menuBuilder.startDispatchingItemsChanged();
            }
        }
    }

    @Override
    public boolean onMenuKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 1) {
            openOptionsMenu();
        }
        return true;
    }

    @Override
    public boolean onKeyShortcut(int i, KeyEvent keyEvent) {
        Menu menu = this.mDecorToolbar.getMenu();
        if (menu == null) {
            return false;
        }
        menu.setQwertyMode(KeyCharacterMap.load(keyEvent != null ? keyEvent.getDeviceId() : -1).getKeyboardType() != 1);
        return menu.performShortcut(i, keyEvent, 0);
    }

    @Override
    public void onDestroy() {
        this.mDecorToolbar.getViewGroup().removeCallbacks(this.mMenuInvalidator);
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

    private class ToolbarCallbackWrapper extends WindowCallbackWrapper {
        public ToolbarCallbackWrapper(Window.Callback callback) {
            super(callback);
        }

        @Override
        public boolean onPreparePanel(int i, View view, Menu menu) {
            boolean zOnPreparePanel = super.onPreparePanel(i, view, menu);
            if (zOnPreparePanel && !ToolbarActionBar.this.mToolbarMenuPrepared) {
                ToolbarActionBar.this.mDecorToolbar.setMenuPrepared();
                ToolbarActionBar.this.mToolbarMenuPrepared = true;
            }
            return zOnPreparePanel;
        }

        @Override
        public View onCreatePanelView(int i) {
            if (i == 0) {
                return new View(ToolbarActionBar.this.mDecorToolbar.getContext());
            }
            return super.onCreatePanelView(i);
        }
    }

    private final class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        private boolean mClosingActionMenu;

        private ActionMenuPresenterCallback() {
        }

        @Override
        public boolean onOpenSubMenu(MenuBuilder menuBuilder) {
            if (ToolbarActionBar.this.mWindowCallback != null) {
                ToolbarActionBar.this.mWindowCallback.onMenuOpened(8, menuBuilder);
                return true;
            }
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
            if (this.mClosingActionMenu) {
                return;
            }
            this.mClosingActionMenu = true;
            ToolbarActionBar.this.mDecorToolbar.dismissPopupMenus();
            if (ToolbarActionBar.this.mWindowCallback != null) {
                ToolbarActionBar.this.mWindowCallback.onPanelClosed(8, menuBuilder);
            }
            this.mClosingActionMenu = false;
        }
    }

    private final class MenuBuilderCallback implements MenuBuilder.Callback {
        private MenuBuilderCallback() {
        }

        @Override
        public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {
            return false;
        }

        @Override
        public void onMenuModeChange(MenuBuilder menuBuilder) {
            if (ToolbarActionBar.this.mWindowCallback != null) {
                if (ToolbarActionBar.this.mDecorToolbar.isOverflowMenuShowing()) {
                    ToolbarActionBar.this.mWindowCallback.onPanelClosed(8, menuBuilder);
                } else if (ToolbarActionBar.this.mWindowCallback.onPreparePanel(0, null, menuBuilder)) {
                    ToolbarActionBar.this.mWindowCallback.onMenuOpened(8, menuBuilder);
                }
            }
        }
    }
}
