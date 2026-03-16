package com.android.contacts.quickcontact;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class QuickContactBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri dataUri = intent.getData();
        Intent newIntent = new Intent("android.provider.action.QUICK_CONTACT");
        newIntent.setSourceBounds(intent.getSourceBounds());
        newIntent.addFlags(268468224);
        newIntent.setData(dataUri);
        context.startActivity(newIntent);
    }
}
