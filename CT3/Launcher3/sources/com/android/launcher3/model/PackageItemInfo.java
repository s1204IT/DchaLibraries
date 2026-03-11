package com.android.launcher3.model;

import android.graphics.Bitmap;
import com.android.launcher3.ItemInfo;
import java.util.Arrays;

public class PackageItemInfo extends ItemInfo {
    int flags = 0;
    public Bitmap iconBitmap;
    public String packageName;
    public String titleSectionName;
    public boolean usingLowResIcon;

    PackageItemInfo(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "PackageItemInfo(title=" + this.title + " id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + ")";
    }
}
