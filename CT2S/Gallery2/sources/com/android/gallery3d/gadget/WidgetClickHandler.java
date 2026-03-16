package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryActivity;

public class WidgetClickHandler extends Activity {
    private boolean isValidDataUri(Uri dataUri) {
        if (dataUri == null) {
            return false;
        }
        try {
            AssetFileDescriptor f = getContentResolver().openAssetFileDescriptor(dataUri, "r");
            f.close();
            return true;
        } catch (Throwable e) {
            Log.w("PhotoAppWidgetClickHandler", "cannot open uri: " + dataUri, e);
            return false;
        }
    }

    @Override
    @TargetApi(11)
    protected void onCreate(Bundle savedState) {
        Intent intent;
        super.onCreate(savedState);
        boolean tediousBack = Build.VERSION.SDK_INT >= 16;
        Uri uri = getIntent().getData();
        if (isValidDataUri(uri)) {
            intent = new Intent("android.intent.action.VIEW", uri);
            if (tediousBack) {
                intent.putExtra("treat-back-as-up", true);
            }
        } else {
            Toast.makeText(this, R.string.no_such_item, 1).show();
            intent = new Intent(this, (Class<?>) GalleryActivity.class);
        }
        if (tediousBack) {
            intent.setFlags(268484608);
        }
        startActivity(intent);
        finish();
    }
}
