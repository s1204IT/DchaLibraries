package com.android.documentsui;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class PackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data;
        ContentResolver resolver = context.getContentResolver();
        String action = intent.getAction();
        if ("android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
            resolver.call(RecentsProvider.buildRecent(), "purge", (String) null, (Bundle) null);
        } else if ("android.intent.action.PACKAGE_DATA_CLEARED".equals(action) && (data = intent.getData()) != null) {
            String packageName = data.getSchemeSpecificPart();
            resolver.call(RecentsProvider.buildRecent(), "purgePackage", packageName, (Bundle) null);
        }
    }
}
