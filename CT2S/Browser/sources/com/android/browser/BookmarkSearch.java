package com.android.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class BookmarkSearch extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if ("android.intent.action.VIEW".equals(action)) {
                intent.setClass(this, BrowserActivity.class);
                startActivity(intent);
            }
        }
        finish();
    }
}
