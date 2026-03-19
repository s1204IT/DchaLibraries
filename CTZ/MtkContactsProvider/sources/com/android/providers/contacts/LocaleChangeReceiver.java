package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LocaleChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("LocaleChangeReceiver", "[onReceive] intent:" + intent);
        ContentProvider contentProviderCoerceToLocalContentProvider = ContentProvider.coerceToLocalContentProvider(context.getContentResolver().acquireProvider("com.android.contacts"));
        if (contentProviderCoerceToLocalContentProvider instanceof ContactsProvider2) {
            Log.d("LocaleChangeReceiver", "[onReceive] call onLocaleChanged.");
            ((ContactsProvider2) contentProviderCoerceToLocalContentProvider).onLocaleChanged();
        }
    }
}
