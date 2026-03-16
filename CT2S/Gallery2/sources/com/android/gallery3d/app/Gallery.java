package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.util.IntentHelper;

public class Gallery extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = IntentHelper.getGalleryIntent(this);
        intent.setFlags(2097152);
        intent.setFlags(268435456);
        startActivity(intent);
        finish();
    }
}
