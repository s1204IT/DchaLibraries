package com.android.calendar;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PermissionDeniedActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        Log.d("Calendar", "Permission denied dialog ");
        super.onCreate(bundle);
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }
}
