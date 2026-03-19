package com.android.documentsui;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.android.documentsui.picker.LastAccessedProvider;
import com.android.documentsui.prefs.ScopedAccessLocalPreferences;

public class PackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String schemeSpecificPart;
        ContentResolver contentResolver = context.getContentResolver();
        String action = intent.getAction();
        Uri data = intent.getData();
        if (data != null) {
            schemeSpecificPart = data.getSchemeSpecificPart();
        } else {
            schemeSpecificPart = null;
        }
        if ("android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
            contentResolver.call(LastAccessedProvider.buildLastAccessed(schemeSpecificPart), "purge", (String) null, (Bundle) null);
            if (schemeSpecificPart != null) {
                ScopedAccessLocalPreferences.clearPackagePreferences(context, schemeSpecificPart);
                return;
            }
            return;
        }
        if ("android.intent.action.PACKAGE_DATA_CLEARED".equals(action) && schemeSpecificPart != null) {
            contentResolver.call(LastAccessedProvider.buildLastAccessed(schemeSpecificPart), "purgePackage", schemeSpecificPart, (Bundle) null);
            ScopedAccessLocalPreferences.clearPackagePreferences(context, schemeSpecificPart);
        }
    }
}
