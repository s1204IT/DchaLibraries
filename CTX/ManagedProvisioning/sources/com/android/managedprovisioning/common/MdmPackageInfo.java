package com.android.managedprovisioning.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.Preconditions;

@Immutable
public final class MdmPackageInfo {
    public final String appLabel;
    public final Drawable packageIcon;

    public MdmPackageInfo(Drawable drawable, String str) {
        this.packageIcon = (Drawable) Preconditions.checkNotNull(drawable, "package icon must not be null");
        this.appLabel = (String) Preconditions.checkNotNull(str, "app label must not be null");
    }

    public static MdmPackageInfo createFromPackageName(Context context, String str) {
        if (str != null) {
            PackageManager packageManager = context.getPackageManager();
            try {
                return new MdmPackageInfo(packageManager.getApplicationIcon(str), packageManager.getApplicationLabel(packageManager.getApplicationInfo(str, 0)).toString());
            } catch (PackageManager.NameNotFoundException e) {
                ProvisionLogger.logw("Package not currently installed: " + str);
                return null;
            }
        }
        return null;
    }
}
