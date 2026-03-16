package com.android.contacts.activities;

import android.app.Activity;
import android.os.Bundle;

public class CancelActivity extends Activity {
    private final String LOG_TAG = "CancelActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mode = getIntent().getIntExtra("android.contacts.extra.MODE", -1);
        if (mode != -1) {
            CancelDialog.show(getFragmentManager(), mode);
        }
    }
}
