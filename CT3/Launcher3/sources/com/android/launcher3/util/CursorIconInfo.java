package com.android.launcher3.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.PackageInstallerCompat;

public class CursorIconInfo {
    public final int iconIndex;
    public final int iconPackageIndex;
    public final int iconResourceIndex;
    public final int iconTypeIndex;

    public CursorIconInfo(Cursor c) {
        this.iconTypeIndex = c.getColumnIndexOrThrow("iconType");
        this.iconIndex = c.getColumnIndexOrThrow("icon");
        this.iconPackageIndex = c.getColumnIndexOrThrow("iconPackage");
        this.iconResourceIndex = c.getColumnIndexOrThrow("iconResource");
    }

    public Bitmap loadIcon(Cursor c, ShortcutInfo info, Context context) {
        Bitmap icon = null;
        int iconType = c.getInt(this.iconTypeIndex);
        switch (iconType) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                String packageName = c.getString(this.iconPackageIndex);
                String resourceName = c.getString(this.iconResourceIndex);
                if (!TextUtils.isEmpty(packageName) || !TextUtils.isEmpty(resourceName)) {
                    info.iconResource = new Intent.ShortcutIconResource();
                    info.iconResource.packageName = packageName;
                    info.iconResource.resourceName = resourceName;
                    icon = Utilities.createIconBitmap(packageName, resourceName, context);
                }
                if (icon == null) {
                    Bitmap icon2 = Utilities.createIconBitmap(c, this.iconIndex, context);
                    return icon2;
                }
                return icon;
            case PackageInstallerCompat.STATUS_INSTALLING:
                Bitmap icon3 = Utilities.createIconBitmap(c, this.iconIndex, context);
                info.customIcon = icon3 != null;
                return icon3;
            default:
                return null;
        }
    }
}
