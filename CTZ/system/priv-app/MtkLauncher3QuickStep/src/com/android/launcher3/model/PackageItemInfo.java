package com.android.launcher3.model;

import com.android.launcher3.ItemInfoWithIcon;

/* loaded from: classes.dex */
public class PackageItemInfo extends ItemInfoWithIcon {
    public String packageName;

    public PackageItemInfo(String str) {
        this.packageName = str;
    }

    @Override // com.android.launcher3.ItemInfo
    protected String dumpProperties() {
        return super.dumpProperties() + " packageName=" + this.packageName;
    }
}
