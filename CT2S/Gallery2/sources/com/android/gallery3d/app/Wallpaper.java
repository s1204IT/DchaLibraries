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
    protected void onSaveInstanceState(Bundle saveState) {
        saveState.putInt("activity-state", this.mState);
        if (this.mPickedItem != null) {
            saveState.putParcelable("picked-item", this.mPickedItem);
        }
    }

    @TargetApi(13)
    private Point getDefaultDisplaySize(Point size) {
        Display d = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 13) {
            d.getSize(size);
        } else {
            size.set(d.getWidth(), d.getHeight());
        }
        return size;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        switch (this.mState) {
            case 0:
                this.mPickedItem = intent.getData();
                if (this.mPickedItem == null) {
                    Intent request = new Intent("android.intent.action.GET_CONTENT").setClass(this, DialogPicker.class).setType("image/*");
                    startActivityForResult(request, 1);
                    return;
                }
                this.mState = 1;
                break;
            case 1:
                break;
            default:
                return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            WallpaperManager wpm = WallpaperManager.getInstance(getApplicationContext());
            try {
                Intent cropAndSetWallpaperIntent = wpm.getCropAndSetWallpaperIntent(this.mPickedItem);
                startActivity(cropAndSetWallpaperIntent);
                finish();
                return;
            } catch (ActivityNotFoundException e) {
            } catch (IllegalArgumentException e2) {
            }
        }
        int width = getWallpaperDesiredMinimumWidth();
        int height = getWallpaperDesiredMinimumHeight();
        Point size = getDefaultDisplaySize(new Point());
        float spotlightX = size.x / width;
        float spotlightY = size.y / height;
        Intent cropAndSetWallpaperIntent2 = new Intent("com.android.camera.action.CROP").setClass(this, CropActivity.class).setDataAndType(this.mPickedItem, "image/*").addFlags(33554432).putExtra("outputX", width).putExtra("outputY", height).putExtra("aspectX", width).putExtra("aspectY", height).putExtra("spotlightX", spotlightX).putExtra("spotlightY", spotlightY).putExtra("scale", true).putExtra("scaleUpIfNeeded", true).putExtra("set-as-wallpaper", true);
        startActivity(cropAndSetWallpaperIntent2);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != -1) {
            setResult(resultCode);
            finish();
        } else {
            this.mState = requestCode;
            if (this.mState == 1) {
                this.mPickedItem = data.getData();
            }
        }
    }
}
