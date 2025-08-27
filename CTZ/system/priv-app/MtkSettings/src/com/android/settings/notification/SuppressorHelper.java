package com.android.settings.notification;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.util.Log;

/* loaded from: classes.dex */
public class SuppressorHelper {
    public static String getSuppressionText(Context context, ComponentName componentName) {
        if (componentName != null) {
            return context.getString(R.string.face_authenticated_confirmation_required, getSuppressorCaption(context, componentName));
        }
        return null;
    }

    static String getSuppressorCaption(Context context, ComponentName componentName) {
        CharSequence charSequenceLoadLabel;
        PackageManager packageManager = context.getPackageManager();
        try {
            ServiceInfo serviceInfo = packageManager.getServiceInfo(componentName, 0);
            if (serviceInfo != null && (charSequenceLoadLabel = serviceInfo.loadLabel(packageManager)) != null) {
                String strTrim = charSequenceLoadLabel.toString().trim();
                if (strTrim.length() > 0) {
                    return strTrim;
                }
            }
        } catch (Throwable th) {
            Log.w("SuppressorHelper", "Error loading suppressor caption", th);
        }
        return componentName.getPackageName();
    }
}
