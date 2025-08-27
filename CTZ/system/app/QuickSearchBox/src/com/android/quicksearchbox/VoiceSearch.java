package com.android.quicksearchbox;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

/* loaded from: classes.dex */
public class VoiceSearch {
    private final Context mContext;

    public VoiceSearch(Context context) {
        this.mContext = context;
    }

    public boolean shouldShowVoiceSearch() {
        return isVoiceSearchAvailable();
    }

    protected Intent createVoiceSearchIntent() {
        return new Intent("android.speech.action.WEB_SEARCH");
    }

    private ResolveInfo getResolveInfo() {
        return this.mContext.getPackageManager().resolveActivity(createVoiceSearchIntent(), 65536);
    }

    public boolean isVoiceSearchAvailable() {
        return getResolveInfo() != null;
    }

    public Intent createVoiceWebSearchIntent(Bundle bundle) {
        if (!isVoiceSearchAvailable()) {
            return null;
        }
        Intent intentCreateVoiceSearchIntent = createVoiceSearchIntent();
        intentCreateVoiceSearchIntent.addFlags(268435456);
        intentCreateVoiceSearchIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "web_search");
        if (bundle != null) {
            intentCreateVoiceSearchIntent.putExtra("app_data", bundle);
        }
        return intentCreateVoiceSearchIntent;
    }
}
