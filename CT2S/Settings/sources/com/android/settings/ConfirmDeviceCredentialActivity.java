package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConfirmDeviceCredentialActivity extends Activity {
    public static final String TAG = ConfirmDeviceCredentialActivity.class.getSimpleName();

    public static Intent createIntent(CharSequence title, CharSequence details) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", ConfirmDeviceCredentialActivity.class.getName());
        intent.putExtra("android.app.extra.TITLE", title);
        intent.putExtra("android.app.extra.DESCRIPTION", details);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String title = intent.getStringExtra("android.app.extra.TITLE");
        String details = intent.getStringExtra("android.app.extra.DESCRIPTION");
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        if (!helper.launchConfirmationActivity(0, title, details)) {
            Log.d(TAG, "No pattern, password or PIN set.");
            setResult(-1);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean credentialsConfirmed = resultCode == -1;
        Log.d(TAG, "Device credentials confirmed: " + credentialsConfirmed);
        setResult(credentialsConfirmed ? -1 : 0);
        finish();
    }
}
