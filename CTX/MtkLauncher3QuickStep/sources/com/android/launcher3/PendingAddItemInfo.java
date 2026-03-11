package com.android.launcher3;

import android.content.ComponentName;

public class PendingAddItemInfo extends ItemInfo {
    public ComponentName componentName;

    @Override
    protected String dumpProperties() {
        return super.dumpProperties() + " componentName=" + this.componentName;
    }
}
