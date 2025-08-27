package com.android.settings.notification;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/* loaded from: classes.dex */
public class ZenSuggestionActivity extends Activity {
    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startActivity(new Intent("android.settings.ZEN_MODE_SETTINGS"));
        startActivity(new Intent("android.settings.ZEN_MODE_ONBOARDING"));
        finish();
    }
}
