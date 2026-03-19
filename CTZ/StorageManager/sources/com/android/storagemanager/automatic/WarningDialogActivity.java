package com.android.storagemanager.automatic;

import android.app.Activity;
import android.os.Bundle;

public class WarningDialogActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getFragmentManager().findFragmentByTag("WarningDialogFragment") != null) {
            return;
        }
        WarningDialogFragment.newInstance().show(getFragmentManager(), "WarningDialogFragment");
    }
}
