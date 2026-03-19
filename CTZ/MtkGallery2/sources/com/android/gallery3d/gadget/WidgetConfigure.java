package com.android.gallery3d.gadget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AlbumPicker;
import com.android.gallery3d.app.DialogPicker;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.omadrm.OmaDrmStore;

public class WidgetConfigure extends Activity {
    private int mAppWidgetId = -1;
    private Uri mPickedItem;
    private static float WIDGET_SCALE_FACTOR = 1.5f;
    private static int MAX_WIDGET_SIDE = 360;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAppWidgetId = getIntent().getIntExtra("appWidgetId", -1);
        if (this.mAppWidgetId == -1) {
            setResult(0);
            finish();
            return;
        }
        Log.d("Gallery2/WidgetConfigure", "<onCreate> widget id=" + this.mAppWidgetId);
        if (bundle == null) {
            if (PermissionHelper.checkAndRequestForWidget(this)) {
                if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
                    startActivityForResult(new Intent(this, (Class<?>) WidgetTypeChooser.class), 1);
                    return;
                } else {
                    setWidgetType(new Intent().putExtra("widget-type", R.id.widget_type_photo));
                    return;
                }
            }
            return;
        }
        this.mPickedItem = (Uri) bundle.getParcelable("picked-item");
    }

    private void updateWidgetAndFinish(WidgetDatabaseHelper.Entry entry) {
        AppWidgetManager.getInstance(this).updateAppWidget(this.mAppWidgetId, PhotoAppWidgetProvider.buildWidget(this, this.mAppWidgetId, entry));
        setResult(-1, new Intent().putExtra("appWidgetId", this.mAppWidgetId));
        startService(new Intent(getApplicationContext(), (Class<?>) WidgetService.class));
        finish();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i2 != -1) {
            setResult(i2, new Intent().putExtra("appWidgetId", this.mAppWidgetId));
            finish();
            return;
        }
        PhotoPlayFacade.registerWidgetMedias(this);
        if (i == 1) {
            setWidgetType(intent);
            return;
        }
        if (i == 2) {
            setChoosenAlbum(intent);
            return;
        }
        if (i == 4) {
            setChoosenPhoto(intent);
        } else {
            if (i == 3) {
                setPhotoWidget(intent);
                return;
            }
            throw new AssertionError("unknown request: " + i);
        }
    }

    private void setPhotoWidget(Intent intent) {
        Bitmap bitmapDecodeByteArray;
        byte[] byteArrayExtra = intent.getByteArrayExtra("data-compress");
        if (byteArrayExtra != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inDither = true;
            options.inPreferQualityOverSpeed = true;
            bitmapDecodeByteArray = BitmapFactory.decodeByteArray(byteArrayExtra, 0, byteArrayExtra.length, options);
        } else {
            bitmapDecodeByteArray = null;
        }
        WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(this);
        try {
            if (this.mPickedItem == null) {
                String absloutePath = getAbsloutePath(intent.getData());
                if (absloutePath != null) {
                    this.mPickedItem = Uri.parse(absloutePath);
                    Log.d("Gallery2/WidgetConfigure", "<setPhotoWidget> use absloute path =" + this.mPickedItem);
                } else {
                    this.mPickedItem = intent.getData();
                }
            }
            if (!widgetDatabaseHelper.setPhoto(this.mAppWidgetId, this.mPickedItem, bitmapDecodeByteArray)) {
                Log.e("Gallery2/WidgetConfigure", "<setPhotoWidget> setPhoto for widget #" + this.mAppWidgetId + " uri[" + this.mPickedItem + "] failed!!");
                Toast.makeText(this, R.string.widget_load_failed, 0).show();
                setResult(0);
                finish();
                return;
            }
            WidgetDatabaseHelper.Entry entry = widgetDatabaseHelper.getEntry(this.mAppWidgetId);
            if (entry != null) {
                updateWidgetAndFinish(entry);
            } else {
                Log.e("Gallery2/WidgetConfigure", "<setPhotoWidget> getEntry(" + this.mAppWidgetId + ") failed!!");
                Toast.makeText(this, R.string.widget_load_failed, 0).show();
                setResult(0);
                finish();
            }
        } finally {
            widgetDatabaseHelper.close();
        }
    }

    private void setChoosenPhoto(Intent intent) {
        Resources resources = getResources();
        float dimension = resources.getDimension(R.dimen.appwidget_width);
        float dimension2 = resources.getDimension(R.dimen.appwidget_height);
        float fMin = Math.min(WIDGET_SCALE_FACTOR, MAX_WIDGET_SIDE / Math.max(dimension, dimension2));
        int iRound = Math.round(dimension * fMin);
        int iRound2 = Math.round(dimension2 * fMin);
        this.mPickedItem = intent.getData();
        startActivityForResult(new Intent("com.android.camera.action.CROP", this.mPickedItem).putExtra("outputX", iRound).putExtra("outputY", iRound2).putExtra("aspectX", iRound).putExtra("aspectY", iRound2).putExtra("scaleUpIfNeeded", true).putExtra("scale", true).putExtra("return-data", true), 3);
    }

    private void setChoosenAlbum(Intent intent) {
        String stringExtra = intent.getStringExtra("album-path");
        WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(this);
        String relativePath = null;
        try {
            ?? r2 = (MediaSet) ((GalleryApp) getApplicationContext()).getDataManager().getMediaObject(Path.fromString(stringExtra));
            if (r2 instanceof LocalAlbum) {
                relativePath = r2.getRelativePath();
                Log.i("Gallery2/WidgetConfigure", "<setChoosenAlbum> Setting widget, album path: " + stringExtra + ", relative path: " + relativePath);
            }
            widgetDatabaseHelper.setWidget(this.mAppWidgetId, 2, stringExtra, relativePath);
            updateWidgetAndFinish(widgetDatabaseHelper.getEntry(this.mAppWidgetId));
        } finally {
            widgetDatabaseHelper.close();
        }
    }

    private void setWidgetType(Intent intent) {
        int intExtra = intent.getIntExtra("widget-type", R.id.widget_type_shuffle);
        if (intExtra == R.id.widget_type_album) {
            Log.d("Gallery2/WidgetConfigure", "<setWidgetType> setWidgetType: type=album");
            Intent intent2 = new Intent(this, (Class<?>) AlbumPicker.class);
            intent2.putExtra(OmaDrmStore.DrmIntentExtra.EXTRA_DRM_LEVEL, 1);
            startActivityForResult(intent2, 2);
            return;
        }
        if (intExtra == R.id.widget_type_shuffle) {
            Log.d("Gallery2/WidgetConfigure", "<setWidgetType> setWidgetType: type=shuffle");
            WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(this);
            try {
                widgetDatabaseHelper.setWidget(this.mAppWidgetId, 1, null, null);
                updateWidgetAndFinish(widgetDatabaseHelper.getEntry(this.mAppWidgetId));
                return;
            } finally {
                widgetDatabaseHelper.close();
            }
        }
        Log.d("Gallery2/WidgetConfigure", "<setWidgetType> type=photo");
        if (startMtkCropFlow()) {
            return;
        }
        Intent type = new Intent(this, (Class<?>) DialogPicker.class).setAction("android.intent.action.GET_CONTENT").setType("image/*");
        type.putExtra(OmaDrmStore.DrmIntentExtra.EXTRA_DRM_LEVEL, 1);
        startActivityForResult(type, 4);
    }

    private boolean startMtkCropFlow() {
        Intent type = new Intent(this, (Class<?>) DialogPicker.class).setAction("android.intent.action.GET_CONTENT").setType("image/*");
        Resources resources = getResources();
        float dimension = resources.getDimension(R.dimen.appwidget_width);
        float dimension2 = resources.getDimension(R.dimen.appwidget_height);
        float fMin = Math.min(WIDGET_SCALE_FACTOR, MAX_WIDGET_SIDE / Math.max(dimension, dimension2));
        int iRound = Math.round(dimension * fMin);
        int iRound2 = Math.round(dimension2 * fMin);
        type.putExtra("outputX", iRound).putExtra("outputY", iRound2).putExtra("aspectX", iRound).putExtra("aspectY", iRound2).putExtra("scaleUpIfNeeded", true).putExtra("scale", true).putExtra("return-data-compress", true);
        type.putExtra("crop", "crop");
        type.putExtra(OmaDrmStore.DrmIntentExtra.EXTRA_DRM_LEVEL, 1);
        startActivityForResult(type, 3);
        return true;
    }

    public String getAbsloutePath(Uri uri) throws Throwable {
        String string;
        SQLiteException e;
        Cursor cursorQuery;
        Log.d("Gallery2/WidgetConfigure", "<getAbsloutePath> Single Photo mode :imageUri=" + ((Object) uri));
        String str = null;
        try {
            if (uri == 0) {
                return null;
            }
            try {
                cursorQuery = getContentResolver().query(uri, new String[]{BookmarkEnhance.COLUMN_DATA}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            string = cursorQuery.getString(0);
                            try {
                                Log.d("Gallery2/WidgetConfigure", "<getAbsloutePath> get absolute path =" + string);
                                str = string;
                            } catch (SQLiteException e2) {
                                e = e2;
                                e.printStackTrace();
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return string;
                            }
                        }
                    } catch (SQLiteException e3) {
                        string = null;
                        e = e3;
                    }
                }
                if (cursorQuery == null) {
                    return str;
                }
                cursorQuery.close();
                return str;
            } catch (SQLiteException e4) {
                string = null;
                e = e4;
                cursorQuery = null;
            } catch (Throwable th) {
                th = th;
                uri = 0;
                if (uri != 0) {
                    uri.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (PermissionHelper.isAllPermissionsGranted(strArr, iArr)) {
            Log.d("Gallery2/WidgetConfigure", "<onRequestPermissionsResult> all permission granted");
            if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
                startActivityForResult(new Intent(this, (Class<?>) WidgetTypeChooser.class), 1);
                return;
            } else {
                setWidgetType(new Intent().putExtra("widget-type", R.id.widget_type_photo));
                return;
            }
        }
        Log.d("Gallery2/WidgetConfigure", "<onRequestPermissionsResult> permission denied, finish");
        PermissionHelper.showDeniedPrompt(this);
        finish();
    }
}
