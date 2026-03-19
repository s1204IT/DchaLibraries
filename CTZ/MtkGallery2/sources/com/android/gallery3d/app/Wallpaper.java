package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.mediatek.omadrm.OmaDrmStore;

public class Wallpaper extends Activity {
    private Uri mPickedItem;
    private int mState = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mState = bundle.getInt("activity-state");
            this.mPickedItem = (Uri) bundle.getParcelable("picked-item");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("activity-state", this.mState);
        if (this.mPickedItem != null) {
            bundle.putParcelable("picked-item", this.mPickedItem);
        }
    }

    @TargetApi(13)
    private Point getDefaultDisplaySize(Point point) {
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 13) {
            defaultDisplay.getSize(point);
        } else {
            point.set(defaultDisplay.getWidth(), defaultDisplay.getHeight());
        }
        return point;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        switch (this.mState) {
            case 0:
                this.mPickedItem = intent.getData();
                if (this.mPickedItem == null) {
                    Intent type = new Intent("android.intent.action.GET_CONTENT").setClass(this, DialogPicker.class).setType("image/*");
                    type.putExtra(OmaDrmStore.DrmIntentExtra.EXTRA_DRM_LEVEL, 1);
                    startActivityForResult(type, 1);
                    return;
                }
                this.mState = 1;
                break;
                break;
            case 1:
                break;
            default:
                return;
        }
        Log.d("Gallery2/Wallpaper", "<onResume> mPickedItem " + this.mPickedItem);
        if (Build.VERSION.SDK_INT >= 19) {
            try {
                Intent cropAndSetWallpaperIntent = WallpaperManager.getInstance(getApplicationContext()).getCropAndSetWallpaperIntent(this.mPickedItem);
                Log.d("Gallery2/Wallpaper", "<onResume> start WallpaperCropActivity, intent " + cropAndSetWallpaperIntent);
                startActivity(cropAndSetWallpaperIntent);
                finish();
                return;
            } catch (ActivityNotFoundException e) {
                Log.e("Gallery2/Wallpaper", "<onResume> ActivityNotFoundException", e);
            } catch (IllegalArgumentException e2) {
                Log.e("Gallery2/Wallpaper", "<onResume> IllegalArgumentException", e2);
            }
        }
        int wallpaperDesiredMinimumWidth = getWallpaperDesiredMinimumWidth();
        int wallpaperDesiredMinimumHeight = getWallpaperDesiredMinimumHeight();
        Point defaultDisplaySize = getDefaultDisplaySize(new Point());
        Intent intentPutExtra = new Intent("com.android.camera.action.CROP").setClass(this, CropActivity.class).setDataAndType(this.mPickedItem, "image/*").addFlags(33554432).putExtra("outputX", wallpaperDesiredMinimumWidth).putExtra("outputY", wallpaperDesiredMinimumHeight).putExtra("aspectX", wallpaperDesiredMinimumWidth).putExtra("aspectY", wallpaperDesiredMinimumHeight).putExtra("spotlightX", defaultDisplaySize.x / wallpaperDesiredMinimumWidth).putExtra("spotlightY", defaultDisplaySize.y / wallpaperDesiredMinimumHeight).putExtra("scale", true).putExtra("scaleUpIfNeeded", true).putExtra("set-as-wallpaper", true);
        Log.d("Gallery2/Wallpaper", "<onResume> start CropActivity, intent " + intentPutExtra);
        startActivity(intentPutExtra);
        finish();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i2 != -1) {
            setResult(i2);
            finish();
        } else {
            this.mState = i;
            if (this.mState == 1) {
                this.mPickedItem = intent.getData();
            }
        }
    }
}
