package com.android.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import com.android.settingslib.HelpUtils;

public class HelpTrampoline extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            String name = getIntent().getStringExtra("android.intent.extra.TEXT");
            int id = getResources().getIdentifier(name, "string", getPackageName());
            String value = getResources().getString(id);
            Intent intent = HelpUtils.getHelpIntent(this, value, null);
            if (intent != null) {
                startActivity(intent);
            }
        } catch (ActivityNotFoundException | Resources.NotFoundException e) {
            Log.w("HelpTrampoline", "Failed to resolve help", e);
        }
        finish();
    }
}
