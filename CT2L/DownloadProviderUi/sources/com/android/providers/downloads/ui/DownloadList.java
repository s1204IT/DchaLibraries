package com.android.providers.downloads.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;

public class DownloadList extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = new Intent("android.provider.action.MANAGE_ROOT");
        intent.setData(DocumentsContract.buildRootUri("com.android.providers.downloads.documents", "downloads"));
        startActivity(intent);
        finish();
    }
}
