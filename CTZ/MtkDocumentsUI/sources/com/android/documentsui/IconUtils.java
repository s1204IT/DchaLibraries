package com.android.documentsui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;

public class IconUtils {
    public static Drawable loadPackageIcon(Context context, String str, int i) {
        if (i != 0) {
            if (str != null) {
                PackageManager packageManager = context.getPackageManager();
                ProviderInfo providerInfoResolveContentProvider = packageManager.resolveContentProvider(str, 0);
                if (providerInfoResolveContentProvider != null) {
                    return packageManager.getDrawable(providerInfoResolveContentProvider.packageName, i, providerInfoResolveContentProvider.applicationInfo);
                }
                return null;
            }
            return context.getDrawable(i);
        }
        return null;
    }

    public static Drawable loadMimeIcon(Context context, String str, String str2, String str3, int i) {
        if ("vnd.android.document/directory".equals(str)) {
            return context.getDrawable(R.drawable.ic_doc_folder);
        }
        return loadMimeIcon(context, str);
    }

    public static Drawable loadMimeIcon(Context context, String str) {
        return context.getContentResolver().getTypeDrawable(str);
    }

    public static Drawable applyTintColor(Context context, int i, int i2) {
        Drawable drawable = context.getDrawable(i);
        drawable.mutate();
        drawable.setTintList(context.getColorStateList(i2));
        return drawable;
    }
}
