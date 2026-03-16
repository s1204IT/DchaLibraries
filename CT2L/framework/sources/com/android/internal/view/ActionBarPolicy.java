package com.android.internal.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import com.android.internal.R;

public class ActionBarPolicy {
    private Context mContext;

    public static ActionBarPolicy get(Context context) {
        return new ActionBarPolicy(context);
    }

    private ActionBarPolicy(Context context) {
        this.mContext = context;
    }

    public int getMaxActionButtons() {
        return this.mContext.getResources().getInteger(R.integer.max_action_buttons);
    }

    public boolean showsOverflowMenuButton() {
        return true;
    }

    public int getEmbeddedMenuWidthLimit() {
        return this.mContext.getResources().getDisplayMetrics().widthPixels / 2;
    }

    public boolean hasEmbeddedTabs() {
        int targetSdk = this.mContext.getApplicationInfo().targetSdkVersion;
        return targetSdk >= 16 ? this.mContext.getResources().getBoolean(R.bool.action_bar_embed_tabs) : this.mContext.getResources().getBoolean(R.bool.action_bar_embed_tabs_pre_jb);
    }

    public int getTabContainerHeight() {
        TypedArray a = this.mContext.obtainStyledAttributes(null, R.styleable.ActionBar, 16843470, 0);
        int height = a.getLayoutDimension(4, 0);
        Resources r = this.mContext.getResources();
        if (!hasEmbeddedTabs()) {
            height = Math.min(height, r.getDimensionPixelSize(R.dimen.action_bar_stacked_max_height));
        }
        a.recycle();
        return height;
    }

    public boolean enableHomeButtonByDefault() {
        return this.mContext.getApplicationInfo().targetSdkVersion < 14;
    }

    public int getStackedTabMaxWidth() {
        return this.mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_stacked_tab_max_width);
    }
}
