package com.android.quicksearchbox;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

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
        Intent intent = createVoiceSearchIntent();
        ResolveInfo ri = this.mContext.getPackageManager().resolveActivity(intent, 65536);
        return ri;
    }

    public boolean isVoiceSearchAvailable() {
        return getResolveInfo() != null;
    }

    public Intent createVoiceWebSearchIntent(Bundle appData) {
        if (!isVoiceSearchAvailable()) {
            return null;
        }
        Intent intent = createVoiceSearchIntent();
        intent.addFlags(268435456);
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "web_search");
        if (appData != null) {
            intent.putExtra("app_data", appData);
            return intent;
        }
        return intent;
    }
}
