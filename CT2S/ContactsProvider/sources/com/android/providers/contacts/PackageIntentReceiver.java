package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.net.Uri;

public class PackageIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri packageUri = intent.getData();
        String packageName = packageUri.getSchemeSpecificPart();
        IContentProvider iprovider = context.getContentResolver().acquireProvider("com.android.contacts");
        ContentProvider provider = ContentProvider.coerceToLocalContentProvider(iprovider);
        if (provider instanceof ContactsProvider2) {
            ((ContactsProvider2) provider).onPackageChanged(packageName);
        }
        handlePackageChangedForVoicemail(context, intent);
    }

    private void handlePackageChangedForVoicemail(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED") && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
            Intent intentToForward = new Intent(context, (Class<?>) VoicemailCleanupService.class);
            intentToForward.setData(intent.getData());
            intentToForward.setAction(intent.getAction());
            intentToForward.putExtras(intent.getExtras());
            context.startService(intentToForward);
        }
    }
}
