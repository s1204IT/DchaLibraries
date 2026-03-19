package com.android.managedprovisioning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class TrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = (Intent) getIntent().getParcelableExtra("intent");
        if (intent != null) {
            startActivityForResult(intent, 0);
        }
        finish();
    }

    public static void startActivity(Context context, Intent intent) {
        context.startActivity(createIntent(context, intent));
    }

    public static Intent createIntent(Context context, Intent intent) {
        Intent intent2 = new Intent(context, (Class<?>) TrampolineActivity.class);
        intent2.putExtra("intent", intent);
        intent2.addFlags(268435456);
        return intent2;
    }
}
