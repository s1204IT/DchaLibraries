package android.support.v4.internal.view;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.support.v4.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;

public interface SupportMenuItem extends MenuItem {
    @Override
    boolean collapseActionView();

    @Override
    boolean expandActionView();

    @Override
    View getActionView();

    @Override
    int getAlphabeticModifiers();

    @Override
    CharSequence getContentDescription();

    @Override
    ColorStateList getIconTintList();

    @Override
    PorterDuff.Mode getIconTintMode();

    @Override
    int getNumericModifiers();

    ActionProvider getSupportActionProvider();

    @Override
    CharSequence getTooltipText();

    @Override
    boolean isActionViewExpanded();

    @Override
    MenuItem setActionView(int i);

    @Override
    MenuItem setActionView(View view);

    @Override
    MenuItem setAlphabeticShortcut(char c, int i);

    @Override
    SupportMenuItem setContentDescription(CharSequence charSequence);

    @Override
    MenuItem setIconTintList(ColorStateList colorStateList);

    @Override
    MenuItem setIconTintMode(PorterDuff.Mode mode);

    @Override
    MenuItem setNumericShortcut(char c, int i);

    @Override
    MenuItem setShortcut(char c, char c2, int i, int i2);

    @Override
    void setShowAsAction(int i);

    @Override
    MenuItem setShowAsActionFlags(int i);

    SupportMenuItem setSupportActionProvider(ActionProvider actionProvider);

    @Override
    SupportMenuItem setTooltipText(CharSequence charSequence);
}
