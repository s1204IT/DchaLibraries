package com.android.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/* loaded from: classes.dex */
public class BookmarkSearch extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent != null && "android.intent.action.VIEW".equals(intent.getAction())) {
            intent.setClass(this, BrowserActivity.class);
            startActivity(intent);
        }
        finish();
    }
}
