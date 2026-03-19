package com.android.internal.view;

import android.content.Context;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPopupHelper;
import com.android.internal.view.menu.SubMenuBuilder;
import com.android.internal.widget.ActionBarContextView;
import java.lang.ref.WeakReference;

public class StandaloneActionMode extends ActionMode implements MenuBuilder.Callback {
    private ActionMode.Callback mCallback;
    private Context mContext;
    private ActionBarContextView mContextView;
    private WeakReference<View> mCustomView;
    private boolean mFinished;
    private boolean mFocusable;
    private MenuBuilder mMenu;

    public StandaloneActionMode(Context context, ActionBarContextView actionBarContextView, ActionMode.Callback callback, boolean z) {
        this.mContext = context;
        this.mContextView = actionBarContextView;
        this.mCallback = callback;
        this.mMenu = new MenuBuilder(actionBarContextView.getContext()).setDefaultShowAsAction(1);
        this.mMenu.setCallback(this);
        this.mFocusable = z;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mContextView.setTitle(charSequence);
    }

    @Override
    public void setSubtitle(CharSequence charSequence) {
        this.mContextView.setSubtitle(charSequence);
    }

    @Override
    public void setTitle(int i) {
        setTitle(i != 0 ? this.mContext.getString(i) : null);
    }

    @Override
    public void setSubtitle(int i) {
        setSubtitle(i != 0 ? this.mContext.getString(i) : null);
    }

    @Override
    public void setTitleOptionalHint(boolean z) {
        super.setTitleOptionalHint(z);
        this.mContextView.setTitleOptional(z);
    }

    @Override
    public boolean isTitleOptional() {
        return this.mContextView.isTitleOptional();
    }

    @Override
    public void setCustomView(View view) {
        this.mContextView.setCustomView(view);
        this.mCustomView = view != null ? new WeakReference<>(view) : null;
    }

    @Override
    public void invalidate() {
        this.mCallback.onPrepareActionMode(this, this.mMenu);
    }

    @Override
    public void finish() {
        if (this.mFinished) {
            return;
        }
        this.mFinished = true;
        this.mContextView.sendAccessibilityEvent(32);
        this.mCallback.onDestroyActionMode(this);
    }

    @Override
    public Menu getMenu() {
        return this.mMenu;
    }

    @Override
    public CharSequence getTitle() {
        return this.mContextView.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return this.mContextView.getSubtitle();
    }

    @Override
    public View getCustomView() {
        if (this.mCustomView != null) {
            return this.mCustomView.get();
        }
        return null;
    }

    @Override
    public MenuInflater getMenuInflater() {
        return new MenuInflater(this.mContextView.getContext());
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {
        return this.mCallback.onActionItemClicked(this, menuItem);
    }

    public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
    }

    public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
        if (!subMenuBuilder.hasVisibleItems()) {
            return true;
        }
        new MenuPopupHelper(this.mContextView.getContext(), subMenuBuilder).show();
        return true;
    }

    public void onCloseSubMenu(SubMenuBuilder subMenuBuilder) {
    }

    @Override
    public void onMenuModeChange(MenuBuilder menuBuilder) {
        invalidate();
        this.mContextView.showOverflowMenu();
    }

    @Override
    public boolean isUiFocusable() {
        return this.mFocusable;
    }
}
