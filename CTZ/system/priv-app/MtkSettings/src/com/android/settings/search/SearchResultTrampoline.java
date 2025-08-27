package com.android.settings.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.settings.SubSettings;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class SearchResultTrampoline extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) throws SecurityException, IllegalArgumentException {
        super.onCreate(bundle);
        FeatureFactory.getFactory(this).getSearchFeatureProvider().verifyLaunchSearchResultPageCaller(this, getCallingActivity());
        Intent intent = getIntent();
        String stringExtra = intent.getStringExtra(":settings:fragment_args_key");
        Bundle bundle2 = new Bundle();
        bundle2.putString(":settings:fragment_args_key", stringExtra);
        intent.putExtra(":settings:show_fragment_args", bundle2);
        intent.setClass(this, SubSettings.class).addFlags(33554432);
        startActivity(intent);
        finish();
    }
}
