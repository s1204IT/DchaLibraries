package com.android.documentsui.roots;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.documentsui.DocumentsApplication;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DocumentsApplication.getProvidersCache(context).setBootCompletedResult(goAsync());
    }
}
