package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryActivity;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.gallery3d.video.BookmarkEnhance;

public class WidgetClickHandler extends Activity {
    private boolean mLaunchFromEmptyView = false;

    private boolean isValidDataUri(Uri uri) {
        String scheme;
        if (uri == null || (scheme = uri.getScheme()) == null || !"content".equals(scheme)) {
            return false;
        }
        try {
            getContentResolver().openAssetFileDescriptor(uri, "r").close();
            return true;
        } catch (Throwable th) {
            Log.w("Gallery2/WidgetClickHandler", "cannot open uri: " + uri, th);
            return false;
        }
    }

    @Override
    @TargetApi(11)
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mLaunchFromEmptyView = getIntent().getBooleanExtra("on_click_from_empty_view", false) && getIntent().getData() == null;
        Log.d("Gallery2/WidgetClickHandler", "<onCreate> FLAG_FROM_EMPTY_VIEW is " + getIntent().getBooleanExtra("on_click_from_empty_view", false) + ", data = " + getIntent().getData());
        if (PermissionHelper.checkAndRequestForWidget(this)) {
            if (this.mLaunchFromEmptyView) {
                permissionGrantedWhenLaunchFromEmpty();
            }
            startToViewImage();
        }
    }

    public static Uri getContentUri(Uri uri, Context context) throws Throwable {
        Cursor cursorQuery;
        Uri uriWithAppendedId;
        Cursor cursor = null;
        if (uri == null) {
            return null;
        }
        String string = uri.toString();
        Log.d("Gallery2/WidgetClickHandler", "<getContentUri> Single Photo mode absolutePath = " + string);
        try {
            try {
                cursorQuery = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{BookmarkEnhance.COLUMN_ID}, "_data = ?", new String[]{string}, null);
                if (cursorQuery != null) {
                    try {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                int i = cursorQuery.getInt(0);
                                Log.d("Gallery2/WidgetClickHandler", " <getContentUri> " + MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, (long) i);
                                try {
                                    Log.d("Gallery2/WidgetClickHandler", "<getContentUri> Single Photo mode : The URI base of absolte path = " + uriWithAppendedId);
                                } catch (SQLiteException e) {
                                    cursor = cursorQuery;
                                    e = e;
                                    uri = uriWithAppendedId;
                                    e.printStackTrace();
                                    if (cursor == null) {
                                        return uri;
                                    }
                                    cursor.close();
                                    return uri;
                                }
                            } else {
                                uriWithAppendedId = null;
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    } catch (SQLiteException e2) {
                        e = e2;
                        cursor = cursorQuery;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return uriWithAppendedId;
            } catch (SQLiteException e3) {
                e = e3;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursor;
        }
    }

    private void startToViewImage() {
        boolean z;
        Intent intent;
        if (Build.VERSION.SDK_INT < 16) {
            z = false;
        } else {
            z = true;
        }
        Uri data = getIntent().getData();
        if (isValidDataUri(data) || (data = getContentUri(data, getBaseContext())) != null) {
            intent = new Intent("android.intent.action.VIEW", data);
        } else {
            Toast.makeText(this, R.string.no_such_item, 1).show();
            intent = new Intent(this, (Class<?>) GalleryActivity.class);
        }
        if (z) {
            intent.setFlags(268484608);
        }
        intent.putExtra("fromWidget", true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isFinishing()) {
            setVisible(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (PermissionHelper.isAllPermissionsGranted(strArr, iArr)) {
            Log.d("Gallery2/WidgetClickHandler", "<onRequestPermissionsResult> all permission granted");
            if (this.mLaunchFromEmptyView) {
                permissionGrantedWhenLaunchFromEmpty();
                return;
            } else {
                startToViewImage();
                return;
            }
        }
        Log.d("Gallery2/WidgetClickHandler", "<onRequestPermissionsResult> permission denied, finish");
        PermissionHelper.showDeniedPrompt(this);
        finish();
    }

    private void permissionGrantedWhenLaunchFromEmpty() {
        Log.d("Gallery2/WidgetClickHandler", "<permissionGrantedWhenLaunchFromEmpty>");
        WidgetUtils.notifyAllWidgetViewChanged();
        finish();
    }
}
