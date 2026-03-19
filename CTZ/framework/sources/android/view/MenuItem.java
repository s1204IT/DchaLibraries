package android.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;

public interface MenuItem {
    public static final int SHOW_AS_ACTION_ALWAYS = 2;
    public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;
    public static final int SHOW_AS_ACTION_IF_ROOM = 1;
    public static final int SHOW_AS_ACTION_NEVER = 0;
    public static final int SHOW_AS_ACTION_WITH_TEXT = 4;

    public interface OnActionExpandListener {
        boolean onMenuItemActionCollapse(MenuItem menuItem);

        boolean onMenuItemActionExpand(MenuItem menuItem);
    }

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem menuItem);
    }

    boolean collapseActionView();

    boolean expandActionView();

    ActionProvider getActionProvider();

    View getActionView();

    char getAlphabeticShortcut();

    int getGroupId();

    Drawable getIcon();

    Intent getIntent();

    int getItemId();

    ContextMenu.ContextMenuInfo getMenuInfo();

    char getNumericShortcut();

    int getOrder();

    SubMenu getSubMenu();

    CharSequence getTitle();

    CharSequence getTitleCondensed();

    boolean hasSubMenu();

    boolean isActionViewExpanded();

    boolean isCheckable();

    boolean isChecked();

    boolean isEnabled();

    boolean isVisible();

    MenuItem setActionProvider(ActionProvider actionProvider);

    MenuItem setActionView(int i);

    MenuItem setActionView(View view);

    MenuItem setAlphabeticShortcut(char c);

    MenuItem setCheckable(boolean z);

    MenuItem setChecked(boolean z);

    MenuItem setEnabled(boolean z);

    MenuItem setIcon(int i);

    MenuItem setIcon(Drawable drawable);

    MenuItem setIntent(Intent intent);

    MenuItem setNumericShortcut(char c);

    MenuItem setOnActionExpandListener(OnActionExpandListener onActionExpandListener);

    MenuItem setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener);

    MenuItem setShortcut(char c, char c2);

    void setShowAsAction(int i);

    MenuItem setShowAsActionFlags(int i);

    MenuItem setTitle(int i);

    MenuItem setTitle(CharSequence charSequence);

    MenuItem setTitleCondensed(CharSequence charSequence);

    MenuItem setVisible(boolean z);

    default MenuItem setIconTintList(ColorStateList colorStateList) {
        return this;
    }

    default ColorStateList getIconTintList() {
        return null;
    }

    default MenuItem setIconTintMode(PorterDuff.Mode mode) {
        return this;
    }

    default PorterDuff.Mode getIconTintMode() {
        return null;
    }

    default MenuItem setShortcut(char c, char c2, int i, int i2) {
        if ((i2 & Menu.SUPPORTED_MODIFIERS_MASK) == 4096 && (i & Menu.SUPPORTED_MODIFIERS_MASK) == 4096) {
            return setShortcut(c, c2);
        }
        return this;
    }

    default MenuItem setNumericShortcut(char c, int i) {
        if ((i & Menu.SUPPORTED_MODIFIERS_MASK) == 4096) {
            return setNumericShortcut(c);
        }
        return this;
    }

    default int getNumericModifiers() {
        return 4096;
    }

    default MenuItem setAlphabeticShortcut(char c, int i) {
        if ((i & Menu.SUPPORTED_MODIFIERS_MASK) == 4096) {
            return setAlphabeticShortcut(c);
        }
        return this;
    }

    default int getAlphabeticModifiers() {
        return 4096;
    }

    default MenuItem setContentDescription(CharSequence charSequence) {
        return this;
    }

    default CharSequence getContentDescription() {
        return null;
    }

    default MenuItem setTooltipText(CharSequence charSequence) {
        return this;
    }

    default CharSequence getTooltipText() {
        return null;
    }

    default boolean requiresActionButton() {
        return false;
    }

    default boolean requiresOverflow() {
        return true;
    }
}
