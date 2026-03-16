package com.android.providers.contacts;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.VoicemailContract;
import android.util.Log;

public class VoicemailCleanupService extends IntentService {
    public VoicemailCleanupService() {
        super("VoicemailCleanupService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        handleIntentInternal(intent, getContentResolver());
    }

    void handleIntentInternal(Intent intent, ContentResolver contentResolver) {
        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED") && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
            String packageUninstalled = intent.getData().getSchemeSpecificPart();
            Log.d("VoicemailCleanupService", "Cleaning up data for package: " + packageUninstalled);
            contentResolver.delete(VoicemailContract.Voicemails.buildSourceUri(packageUninstalled), null, null);
            contentResolver.delete(VoicemailContract.Status.buildSourceUri(packageUninstalled), null, null);
            return;
        }
        Log.w("VoicemailCleanupService", "Unexpected intent: " + intent);
    }
}
