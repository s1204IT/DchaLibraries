package android.support.v7.view.menu;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.internal.view.SupportMenuItem;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class ActionMenuItem implements SupportMenuItem {
    private static final int CHECKABLE = 1;
    private static final int CHECKED = 2;
    private static final int ENABLED = 16;
    private static final int EXCLUSIVE = 4;
    private static final int HIDDEN = 8;
    private static final int NO_ICON = 0;
    private final int mCategoryOrder;
    private MenuItem.OnMenuItemClickListener mClickListener;
    private CharSequence mContentDescription;
    private Context mContext;
    private final int mGroup;
    private Drawable mIconDrawable;
    private final int mId;
    private Intent mIntent;
    private final int mOrdering;
    private char mShortcutAlphabeticChar;
    private char mShortcutNumericChar;
    private CharSequence mTitle;
    private CharSequence mTitleCondensed;
    private CharSequence mTooltipText;
    private int mShortcutNumericModifiers = 4096;
    private int mShortcutAlphabeticModifiers = 4096;
    private int mIconResId = 0;
    private ColorStateList mIconTintList = null;
    private PorterDuff.Mode mIconTintMode = null;
    private boolean mHasIconTint = false;
    private boolean mHasIconTintMode = false;
    private int mFlags = 16;

    public ActionMenuItem(Context context, int group, int id, int categoryOrder, int ordering, CharSequence title) {
        this.mContext = context;
        this.mId = id;
        this.mGroup = group;
        this.mCategoryOrder = categoryOrder;
        this.mOrdering = ordering;
        this.mTitle = title;
    }

    @Override
    public char getAlphabeticShortcut() {
        return this.mShortcutAlphabeticChar;
    }

    @Override
    public int getAlphabeticModifiers() {
        return this.mShortcutAlphabeticModifiers;
    }

    @Override
    public int getGroupId() {
        return this.mGroup;
    }

    @Override
    public Drawable getIcon() {
        return this.mIconDrawable;
    }

    @Override
    public Intent getIntent() {
        return this.mIntent;
    }

    @Override
    public int getItemId() {
        return this.mId;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return null;
    }

    @Override
    public char getNumericShortcut() {
        return this.mShortcutNumericChar;
    }

    @Override
    public int getNumericModifiers() {
        return this.mShortcutNumericModifiers;
    }

    @Override
    public int getOrder() {
        return this.mOrdering;
    }

    @Override
    public SubMenu getSubMenu() {
        return null;
    }

    @Override
    public CharSequence getTitle() {
        return this.mTitle;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return this.mTitleCondensed != null ? this.mTitleCondensed : this.mTitle;
    }

    @Override
    public boolean hasSubMenu() {
        return false;
    }

    @Override
    public boolean isCheckable() {
        return (this.mFlags & 1) != 0;
    }

    @Override
    public boolean isChecked() {
        return (this.mFlags & 2) != 0;
    }

    @Override
    public boolean isEnabled() {
        return (this.mFlags & 16) != 0;
    }

    @Override
    public boolean isVisible() {
        return (this.mFlags & 8) == 0;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        this.mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        return this;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
        this.mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        this.mShortcutAlphabeticModifiers = KeyEvent.normalizeMetaState(alphaModifiers);
        return this;
    }

    @Override
    public MenuItem setCheckable(boolean z) {
        this.mFlags = (this.mFlags & (-2)) | (z ? 1 : 0);
        return this;
    }

    public ActionMenuItem setExclusiveCheckable(boolean exclusive) {
        this.mFlags = (this.mFlags & (-5)) | (exclusive ? 4 : 0);
        return this;
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        this.mFlags = (this.mFlags & (-3)) | (checked ? 2 : 0);
        return this;
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        this.mFlags = (this.mFlags & (-17)) | (enabled ? 16 : 0);
        return this;
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        this.mIconDrawable = icon;
        this.mIconResId = 0;
        applyIconTint();
        return this;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        this.mIconResId = iconRes;
        this.mIconDrawable = ContextCompat.getDrawable(this.mContext, iconRes);
        applyIconTint();
        return this;
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        this.mIntent = intent;
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        this.mShortcutNumericChar = numericChar;
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
        this.mShortcutNumericChar = numericChar;
        this.mShortcutNumericModifiers = KeyEvent.normalizeMetaState(numericModifiers);
        return this;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener menuItemClickListener) {
        this.mClickListener = menuItemClickListener;
        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        this.mShortcutNumericChar = numericChar;
        this.mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers) {
        this.mShortcutNumericChar = numericChar;
        this.mShortcutNumericModifiers = KeyEvent.normalizeMetaState(numericModifiers);
        this.mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        this.mShortcutAlphabeticModifiers = KeyEvent.normalizeMetaState(alphaModifiers);
        return this;
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        this.mTitle = title;
        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        this.mTitle = this.mContext.getResources().getString(title);
        return this;
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        this.mTitleCondensed = title;
        return this;
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        this.mFlags = (this.mFlags & 8) | (visible ? 0 : 8);
        return this;
    }

    public boolean invoke() {
        if (this.mClickListener != null && this.mClickListener.onMenuItemClick(this)) {
            return true;
        }
        if (this.mIntent != null) {
            this.mContext.startActivity(this.mIntent);
            return true;
        }
        return false;
    }

    @Override
    public void setShowAsAction(int show) {
    }

    @Override
    public SupportMenuItem setActionView(View actionView) {
        throw new UnsupportedOperationException();
    }

    @Override
    public View getActionView() {
        return null;
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActionProvider getActionProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SupportMenuItem setActionView(int resId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public android.support.v4.view.ActionProvider getSupportActionProvider() {
        return null;
    }

    @Override
    public SupportMenuItem setSupportActionProvider(android.support.v4.view.ActionProvider actionProvider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SupportMenuItem setShowAsActionFlags(int actionEnum) {
        setShowAsAction(actionEnum);
        return this;
    }

    @Override
    public boolean expandActionView() {
        return false;
    }

    @Override
    public boolean collapseActionView() {
        return false;
    }

    @Override
    public boolean isActionViewExpanded() {
        return false;
    }

    @Override
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SupportMenuItem setContentDescription(CharSequence contentDescription) {
        this.mContentDescription = contentDescription;
        return this;
    }

    @Override
    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    @Override
    public SupportMenuItem setTooltipText(CharSequence tooltipText) {
        this.mTooltipText = tooltipText;
        return this;
    }

    @Override
    public CharSequence getTooltipText() {
        return this.mTooltipText;
    }

    @Override
    public MenuItem setIconTintList(@Nullable ColorStateList iconTintList) {
        this.mIconTintList = iconTintList;
        this.mHasIconTint = true;
        applyIconTint();
        return this;
    }

    @Override
    public ColorStateList getIconTintList() {
        return this.mIconTintList;
    }

    @Override
    public MenuItem setIconTintMode(PorterDuff.Mode iconTintMode) {
        this.mIconTintMode = iconTintMode;
        this.mHasIconTintMode = true;
        applyIconTint();
        return this;
    }

    @Override
    public PorterDuff.Mode getIconTintMode() {
        return this.mIconTintMode;
    }

    private void applyIconTint() {
        if (this.mIconDrawable != null) {
            if (this.mHasIconTint || this.mHasIconTintMode) {
                this.mIconDrawable = DrawableCompat.wrap(this.mIconDrawable);
                this.mIconDrawable = this.mIconDrawable.mutate();
                if (this.mHasIconTint) {
                    DrawableCompat.setTintList(this.mIconDrawable, this.mIconTintList);
                }
                if (this.mHasIconTintMode) {
                    DrawableCompat.setTintMode(this.mIconDrawable, this.mIconTintMode);
                }
            }
        }
    }
}
