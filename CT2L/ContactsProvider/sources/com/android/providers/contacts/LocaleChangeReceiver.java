package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;

public class LocaleChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        IContentProvider iprovider = context.getContentResolver().acquireProvider("com.android.contacts");
        ContentProvider provider = ContentProvider.coerceToLocalContentProvider(iprovider);
        if (provider instanceof ContactsProvider2) {
            ((ContactsProvider2) provider).onLocaleChanged();
        }
    }
}
