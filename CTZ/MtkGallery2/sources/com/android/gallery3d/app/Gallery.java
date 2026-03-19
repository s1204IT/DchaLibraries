package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.gallery3d.util.IntentHelper;

public class Gallery extends Activity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent galleryIntent = IntentHelper.getGalleryIntent(this);
        galleryIntent.setFlags(2097152);
        galleryIntent.setFlags(268435456);
        startActivity(galleryIntent);
        finish();
    }
}
