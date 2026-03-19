package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class SelectItemParams extends CommandParams {
    boolean mLoadTitleIcon;
    Menu mMenu;

    public SelectItemParams(CommandDetails commandDetails, Menu menu, boolean z) {
        super(commandDetails);
        this.mMenu = null;
        this.mLoadTitleIcon = false;
        this.mMenu = menu;
        this.mLoadTitleIcon = z;
    }

    @Override
    boolean setIcon(Bitmap bitmap) {
        if (bitmap != null && this.mMenu != null) {
            if (this.mLoadTitleIcon && this.mMenu.titleIcon == null) {
                this.mMenu.titleIcon = bitmap;
                return true;
            }
            for (Item item : this.mMenu.items) {
                if (item.icon == null) {
                    item.icon = bitmap;
                    return true;
                }
            }
            return true;
        }
        return false;
    }
}
