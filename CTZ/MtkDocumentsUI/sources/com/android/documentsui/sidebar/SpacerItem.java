package com.android.documentsui.sidebar;

import android.util.Log;
import android.view.View;
import com.android.documentsui.R;
import com.android.documentsui.base.SharedMinimal;

class SpacerItem extends Item {
    public SpacerItem() {
        super(R.layout.item_root_spacer, "SpacerItem");
    }

    @Override
    void bindView(View view) {
    }

    @Override
    boolean isRoot() {
        return false;
    }

    @Override
    void open() {
        if (SharedMinimal.DEBUG) {
            Log.d("SpacerItem", "Ignoring click/hover on spacer item.");
        }
    }
}
