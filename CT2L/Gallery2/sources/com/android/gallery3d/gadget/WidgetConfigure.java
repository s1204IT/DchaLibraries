package com.android.gallery3d.gadget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AlbumPicker;
import com.android.gallery3d.app.DialogPicker;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;

public class WidgetConfigure extends Activity {
    private int mAppWidgetId = -1;
    private Uri mPickedItem;
    private static float WIDGET_SCALE_FACTOR = 1.5f;
    private static int MAX_WIDGET_SIDE = 360;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        this.mAppWidgetId = getIntent().getIntExtra("appWidgetId", -1);
        if (this.mAppWidgetId == -1) {
            setResult(0);
            finish();
        } else {
            if (savedState == null) {
                if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
                    Intent intent = new Intent(this, (Class<?>) WidgetTypeChooser.class);
                    startActivityForResult(intent, 1);
                    return;
                } else {
                    setWidgetType(new Intent().putExtra("widget-type", R.id.widget_type_photo));
                    return;
                }
            }
            this.mPickedItem = (Uri) savedState.getParcelable("picked-item");
        }
    }

    private void updateWidgetAndFinish(WidgetDatabaseHelper.Entry entry) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews views = PhotoAppWidgetProvider.buildWidget(this, this.mAppWidgetId, entry);
        manager.updateAppWidget(this.mAppWidgetId, views);
        setResult(-1, new Intent().putExtra("appWidgetId", this.mAppWidgetId));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != -1) {
            setResult(resultCode, new Intent().putExtra("appWidgetId", this.mAppWidgetId));
            finish();
            return;
        }
        if (requestCode == 1) {
            setWidgetType(data);
            return;
        }
        if (requestCode == 2) {
            setChoosenAlbum(data);
        } else if (requestCode == 4) {
            setChoosenPhoto(data);
        } else {
            if (requestCode == 3) {
                setPhotoWidget(data);
                return;
            }
            throw new AssertionError("unknown request: " + requestCode);
        }
    }

    private void setPhotoWidget(Intent data) {
        Bitmap bitmap = (Bitmap) data.getParcelableExtra("data");
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
        try {
            helper.setPhoto(this.mAppWidgetId, this.mPickedItem, bitmap);
            updateWidgetAndFinish(helper.getEntry(this.mAppWidgetId));
        } finally {
            helper.close();
        }
    }

    private void setChoosenPhoto(Intent data) {
        Resources res = getResources();
        float width = res.getDimension(R.dimen.appwidget_width);
        float height = res.getDimension(R.dimen.appwidget_height);
        float scale = Math.min(WIDGET_SCALE_FACTOR, MAX_WIDGET_SIDE / Math.max(width, height));
        int widgetWidth = Math.round(width * scale);
        int widgetHeight = Math.round(height * scale);
        this.mPickedItem = data.getData();
        Intent request = new Intent("com.android.camera.action.CROP", this.mPickedItem).putExtra("outputX", widgetWidth).putExtra("outputY", widgetHeight).putExtra("aspectX", widgetWidth).putExtra("aspectY", widgetHeight).putExtra("scaleUpIfNeeded", true).putExtra("scale", true).putExtra("return-data", true);
        startActivityForResult(request, 3);
    }

    private void setChoosenAlbum(Intent data) {
        String albumPath = data.getStringExtra("album-path");
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
        String relativePath = null;
        try {
            GalleryApp galleryApp = (GalleryApp) getApplicationContext();
            DataManager manager = galleryApp.getDataManager();
            Path path = Path.fromString(albumPath);
            MediaSet mediaSet = (MediaSet) manager.getMediaObject(path);
            if (mediaSet instanceof LocalAlbum) {
                int bucketId = Integer.parseInt(path.getSuffix());
                relativePath = LocalAlbum.getRelativePath(bucketId);
                Log.i("WidgetConfigure", "Setting widget, album path: " + albumPath + ", relative path: " + relativePath);
            }
            helper.setWidget(this.mAppWidgetId, 2, albumPath, relativePath);
            updateWidgetAndFinish(helper.getEntry(this.mAppWidgetId));
        } finally {
            helper.close();
        }
    }

    private void setWidgetType(Intent data) {
        int widgetType = data.getIntExtra("widget-type", R.id.widget_type_shuffle);
        if (widgetType == R.id.widget_type_album) {
            Intent intent = new Intent(this, (Class<?>) AlbumPicker.class);
            startActivityForResult(intent, 2);
        } else {
            if (widgetType == R.id.widget_type_shuffle) {
                WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
                try {
                    helper.setWidget(this.mAppWidgetId, 1, null, null);
                    updateWidgetAndFinish(helper.getEntry(this.mAppWidgetId));
                    return;
                } finally {
                    helper.close();
                }
            }
            Intent request = new Intent(this, (Class<?>) DialogPicker.class).setAction("android.intent.action.GET_CONTENT").setType("image/*");
            startActivityForResult(request, 4);
        }
    }
}
