package android.support.v7.view.menu;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.v4.internal.view.SupportMenuItem;
import android.util.Log;
import android.view.ActionProvider;
import android.view.CollapsibleActionView;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.reflect.Method;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class MenuItemWrapperICS extends BaseMenuWrapper<SupportMenuItem> implements MenuItem {
    static final String LOG_TAG = "MenuItemWrapper";
    private Method mSetExclusiveCheckableMethod;

    MenuItemWrapperICS(Context context, SupportMenuItem object) {
        super(context, object);
    }

    @Override
    public int getItemId() {
        return ((SupportMenuItem) this.mWrappedObject).getItemId();
    }

    @Override
    public int getGroupId() {
        return ((SupportMenuItem) this.mWrappedObject).getGroupId();
    }

    @Override
    public int getOrder() {
        return ((SupportMenuItem) this.mWrappedObject).getOrder();
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        ((SupportMenuItem) this.mWrappedObject).setTitle(title);
        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        ((SupportMenuItem) this.mWrappedObject).setTitle(title);
        return this;
    }

    @Override
    public CharSequence getTitle() {
        return ((SupportMenuItem) this.mWrappedObject).getTitle();
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        ((SupportMenuItem) this.mWrappedObject).setTitleCondensed(title);
        return this;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return ((SupportMenuItem) this.mWrappedObject).getTitleCondensed();
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        ((SupportMenuItem) this.mWrappedObject).setIcon(icon);
        return this;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        ((SupportMenuItem) this.mWrappedObject).setIcon(iconRes);
        return this;
    }

    @Override
    public Drawable getIcon() {
        return ((SupportMenuItem) this.mWrappedObject).getIcon();
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        ((SupportMenuItem) this.mWrappedObject).setIntent(intent);
        return this;
    }

    @Override
    public Intent getIntent() {
        return ((SupportMenuItem) this.mWrappedObject).getIntent();
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        ((SupportMenuItem) this.mWrappedObject).setShortcut(numericChar, alphaChar);
        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers) {
        ((SupportMenuItem) this.mWrappedObject).setShortcut(numericChar, alphaChar, numericModifiers, alphaModifiers);
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        ((SupportMenuItem) this.mWrappedObject).setNumericShortcut(numericChar);
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
        ((SupportMenuItem) this.mWrappedObject).setNumericShortcut(numericChar, numericModifiers);
        return this;
    }

    @Override
    public char getNumericShortcut() {
        return ((SupportMenuItem) this.mWrappedObject).getNumericShortcut();
    }

    @Override
    public int getNumericModifiers() {
        return ((SupportMenuItem) this.mWrappedObject).getNumericModifiers();
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        ((SupportMenuItem) this.mWrappedObject).setAlphabeticShortcut(alphaChar);
        return this;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
        ((SupportMenuItem) this.mWrappedObject).setAlphabeticShortcut(alphaChar, alphaModifiers);
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return ((SupportMenuItem) this.mWrappedObject).getAlphabeticShortcut();
    }

    @Override
    public int getAlphabeticModifiers() {
        return ((SupportMenuItem) this.mWrappedObject).getAlphabeticModifiers();
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        ((SupportMenuItem) this.mWrappedObject).setCheckable(checkable);
        return this;
    }

    @Override
    public boolean isCheckable() {
        return ((SupportMenuItem) this.mWrappedObject).isCheckable();
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        ((SupportMenuItem) this.mWrappedObject).setChecked(checked);
        return this;
    }

    @Override
    public boolean isChecked() {
        return ((SupportMenuItem) this.mWrappedObject).isChecked();
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        return ((SupportMenuItem) this.mWrappedObject).setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return ((SupportMenuItem) this.mWrappedObject).isVisible();
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        ((SupportMenuItem) this.mWrappedObject).setEnabled(enabled);
        return this;
    }

    @Override
    public boolean isEnabled() {
        return ((SupportMenuItem) this.mWrappedObject).isEnabled();
    }

    @Override
    public boolean hasSubMenu() {
        return ((SupportMenuItem) this.mWrappedObject).hasSubMenu();
    }

    @Override
    public SubMenu getSubMenu() {
        return getSubMenuWrapper(((SupportMenuItem) this.mWrappedObject).getSubMenu());
    }

    @Override
    public MenuItem setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener menuItemClickListener) {
        ((SupportMenuItem) this.mWrappedObject).setOnMenuItemClickListener(menuItemClickListener != null ? new OnMenuItemClickListenerWrapper(menuItemClickListener) : null);
        return this;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return ((SupportMenuItem) this.mWrappedObject).getMenuInfo();
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        ((SupportMenuItem) this.mWrappedObject).setShowAsAction(actionEnum);
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        ((SupportMenuItem) this.mWrappedObject).setShowAsActionFlags(actionEnum);
        return this;
    }

    @Override
    public MenuItem setActionView(View view) {
        if (view instanceof CollapsibleActionView) {
            view = new CollapsibleActionViewWrapper(view);
        }
        ((SupportMenuItem) this.mWrappedObject).setActionView(view);
        return this;
    }

    @Override
    public MenuItem setActionView(int resId) {
        ((SupportMenuItem) this.mWrappedObject).setActionView(resId);
        View actionView = ((SupportMenuItem) this.mWrappedObject).getActionView();
        if (actionView instanceof CollapsibleActionView) {
            ((SupportMenuItem) this.mWrappedObject).setActionView(new CollapsibleActionViewWrapper(actionView));
        }
        return this;
    }

    @Override
    public View getActionView() {
        View actionView = ((SupportMenuItem) this.mWrappedObject).getActionView();
        if (actionView instanceof CollapsibleActionViewWrapper) {
            return ((CollapsibleActionViewWrapper) actionView).getWrappedView();
        }
        return actionView;
    }

    @Override
    public MenuItem setActionProvider(ActionProvider provider) {
        ((SupportMenuItem) this.mWrappedObject).setSupportActionProvider(provider != null ? createActionProviderWrapper(provider) : null);
        return this;
    }

    @Override
    public ActionProvider getActionProvider() {
        android.support.v4.view.ActionProvider provider = ((SupportMenuItem) this.mWrappedObject).getSupportActionProvider();
        if (provider instanceof ActionProviderWrapper) {
            return ((ActionProviderWrapper) provider).mInner;
        }
        return null;
    }

    @Override
    public boolean expandActionView() {
        return ((SupportMenuItem) this.mWrappedObject).expandActionView();
    }

    @Override
    public boolean collapseActionView() {
        return ((SupportMenuItem) this.mWrappedObject).collapseActionView();
    }

    @Override
    public boolean isActionViewExpanded() {
        return ((SupportMenuItem) this.mWrappedObject).isActionViewExpanded();
    }

    @Override
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        ((SupportMenuItem) this.mWrappedObject).setOnActionExpandListener(listener != null ? new OnActionExpandListenerWrapper(listener) : null);
        return this;
    }

    @Override
    public MenuItem setContentDescription(CharSequence contentDescription) {
        ((SupportMenuItem) this.mWrappedObject).setContentDescription(contentDescription);
        return this;
    }

    @Override
    public CharSequence getContentDescription() {
        return ((SupportMenuItem) this.mWrappedObject).getContentDescription();
    }

    @Override
    public MenuItem setTooltipText(CharSequence tooltipText) {
        ((SupportMenuItem) this.mWrappedObject).setTooltipText(tooltipText);
        return this;
    }

    @Override
    public CharSequence getTooltipText() {
        return ((SupportMenuItem) this.mWrappedObject).getTooltipText();
    }

    @Override
    public MenuItem setIconTintList(ColorStateList tint) {
        ((SupportMenuItem) this.mWrappedObject).setIconTintList(tint);
        return this;
    }

    @Override
    public ColorStateList getIconTintList() {
        return ((SupportMenuItem) this.mWrappedObject).getIconTintList();
    }

    @Override
    public MenuItem setIconTintMode(PorterDuff.Mode tintMode) {
        ((SupportMenuItem) this.mWrappedObject).setIconTintMode(tintMode);
        return this;
    }

    @Override
    public PorterDuff.Mode getIconTintMode() {
        return ((SupportMenuItem) this.mWrappedObject).getIconTintMode();
    }

    public void setExclusiveCheckable(boolean checkable) {
        try {
            if (this.mSetExclusiveCheckableMethod == null) {
                this.mSetExclusiveCheckableMethod = ((SupportMenuItem) this.mWrappedObject).getClass().getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE);
            }
            this.mSetExclusiveCheckableMethod.invoke(this.mWrappedObject, Boolean.valueOf(checkable));
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error while calling setExclusiveCheckable", e);
        }
    }

    ActionProviderWrapper createActionProviderWrapper(ActionProvider provider) {
        return new ActionProviderWrapper(this.mContext, provider);
    }

    private class OnMenuItemClickListenerWrapper extends BaseWrapper<MenuItem.OnMenuItemClickListener> implements MenuItem.OnMenuItemClickListener {
        OnMenuItemClickListenerWrapper(MenuItem.OnMenuItemClickListener object) {
            super(object);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return ((MenuItem.OnMenuItemClickListener) this.mWrappedObject).onMenuItemClick(MenuItemWrapperICS.this.getMenuItemWrapper(item));
        }
    }

    private class OnActionExpandListenerWrapper extends BaseWrapper<MenuItem.OnActionExpandListener> implements MenuItem.OnActionExpandListener {
        OnActionExpandListenerWrapper(MenuItem.OnActionExpandListener object) {
            super(object);
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            return ((MenuItem.OnActionExpandListener) this.mWrappedObject).onMenuItemActionExpand(MenuItemWrapperICS.this.getMenuItemWrapper(item));
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            return ((MenuItem.OnActionExpandListener) this.mWrappedObject).onMenuItemActionCollapse(MenuItemWrapperICS.this.getMenuItemWrapper(item));
        }
    }

    class ActionProviderWrapper extends android.support.v4.view.ActionProvider {
        final ActionProvider mInner;

        public ActionProviderWrapper(Context context, ActionProvider inner) {
            super(context);
            this.mInner = inner;
        }

        @Override
        public View onCreateActionView() {
            return this.mInner.onCreateActionView();
        }

        @Override
        public boolean onPerformDefaultAction() {
            return this.mInner.onPerformDefaultAction();
        }

        @Override
        public boolean hasSubMenu() {
            return this.mInner.hasSubMenu();
        }

        @Override
        public void onPrepareSubMenu(SubMenu subMenu) {
            this.mInner.onPrepareSubMenu(MenuItemWrapperICS.this.getSubMenuWrapper(subMenu));
        }
    }

    static class CollapsibleActionViewWrapper extends FrameLayout implements android.support.v7.view.CollapsibleActionView {
        final CollapsibleActionView mWrappedView;

        CollapsibleActionViewWrapper(View view) {
            super(view.getContext());
            this.mWrappedView = (CollapsibleActionView) view;
            addView(view);
        }

        @Override
        public void onActionViewExpanded() {
            this.mWrappedView.onActionViewExpanded();
        }

        @Override
        public void onActionViewCollapsed() {
            this.mWrappedView.onActionViewCollapsed();
        }

        View getWrappedView() {
            return (View) this.mWrappedView;
        }
    }
}
