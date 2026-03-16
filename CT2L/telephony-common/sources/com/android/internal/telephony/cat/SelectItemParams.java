package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import java.util.Iterator;

class SelectItemParams extends CommandParams {
    boolean mLoadTitleIcon;
    Menu mMenu;

    SelectItemParams(CommandDetails cmdDet, Menu menu, boolean loadTitleIcon) {
        super(cmdDet);
        this.mMenu = null;
        this.mLoadTitleIcon = false;
        this.mMenu = menu;
        this.mLoadTitleIcon = loadTitleIcon;
    }

    @Override
    boolean setIcon(Bitmap icon) {
        if (icon != null && this.mMenu != null) {
            if (this.mLoadTitleIcon && this.mMenu.titleIcon == null) {
                this.mMenu.titleIcon = icon;
            } else {
                Iterator<Item> it = this.mMenu.items.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Item item = it.next();
                    if (item.icon == null) {
                        item.icon = icon;
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
