package com.android.managedprovisioning;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class HomeReceiverActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent indirectIntent = new Intent("com.android.managedprovisioning.home_indirect");
        LocalBroadcastManager.getInstance(this).sendBroadcast(indirectIntent);
        finish();
    }
}
