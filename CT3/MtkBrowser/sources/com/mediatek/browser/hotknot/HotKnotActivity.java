package com.mediatek.browser.hotknot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class HotKnotActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("browser/HotKnotActivity", "HotKnotActivity onCreate.");
        Intent intent = getIntent();
        if (!intent.getAction().equals("com.mediatek.hotknot.action.MESSAGE_DISCOVERED")) {
            Log.w("browser/HotKnotActivity", "Invalid intent:" + intent);
            finish();
            return;
        }
        String mimeType = intent.getType();
        if (mimeType == null || !"com.mediatek.browser.hotknot/com.mediatek.browser.hotknot.MIME_TYPE".equalsIgnoreCase(mimeType)) {
            StringBuilder sbAppend = new StringBuilder().append("Invalid mimeType:");
            if (mimeType == null) {
                mimeType = "null";
            }
            Log.w("browser/HotKnotActivity", sbAppend.append(mimeType).toString());
            finish();
            return;
        }
        byte[] data = intent.getByteArrayExtra("com.mediatek.hotknot.extra.DATA");
        if (data == null || data.length == 0) {
            Log.w("browser/HotKnotActivity", "Invalid url:" + (data == null ? "null" : ""));
            finish();
            return;
        }
        String url = new String(data);
        Uri uri = Uri.parse(url);
        Intent intent2 = new Intent("android.intent.action.VIEW", uri);
        intent2.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
        intent2.putExtra("HotKnot_Intent", true);
        startActivity(intent2);
        finish();
    }
}
