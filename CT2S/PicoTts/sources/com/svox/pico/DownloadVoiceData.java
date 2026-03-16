package com.svox.pico;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class DownloadVoiceData extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri marketUri = Uri.parse("market://search?q=pname:com.svox.langpack.installer");
        Intent marketIntent = new Intent("android.intent.action.VIEW", marketUri);
        startActivityForResult(marketIntent, 0);
        finish();
    }
}
