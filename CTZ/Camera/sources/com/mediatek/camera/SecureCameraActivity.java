package com.mediatek.camera;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

public class SecureCameraActivity extends CameraActivity {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SecureCameraActivity.class.getSimpleName());
    private static int sSecureAlbumId;
    private boolean mSecureCamera;
    private ArrayList<String> mSecureArray = new ArrayList<>();
    private String mPath = null;
    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SecureCameraActivity.this.finish();
            LogHelper.d(SecureCameraActivity.TAG, "mScreenOffReceiver receive");
        }
    };

    @Override
    protected void onCreateTasks(Bundle bundle) {
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.media.action.STILL_IMAGE_CAMERA_SECURE".equals(action)) {
            this.mSecureCamera = true;
            sSecureAlbumId++;
        } else if ("android.media.action.IMAGE_CAPTURE_SECURE".equals(action)) {
            this.mSecureCamera = true;
        } else {
            this.mSecureCamera = intent.getBooleanExtra("secure_camera", false);
        }
        if (this.mSecureCamera) {
            setScreenFlags();
            this.mPath = "/secure/all/" + sSecureAlbumId;
            registerReceiver(this.mScreenOffReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
        }
        super.onCreateTasks(bundle);
        if (this.mSecureCamera) {
            getAppUi().updateThumbnail(null);
        }
    }

    @Override
    protected void onResumeTasks() {
        if (this.mSecureCamera) {
            if (this.mSecureArray.isEmpty()) {
                getAppUi().updateThumbnail(null);
            } else if (!checkSecureAlbumLive()) {
                getAppUi().updateThumbnail(null);
                this.mSecureArray.clear();
            }
        }
        super.onResumeTasks();
    }

    @Override
    protected void onPauseTasks() {
        super.onPauseTasks();
    }

    @Override
    protected void onDestroyTasks() {
        if (this.mSecureCamera) {
            unregisterReceiver(this.mScreenOffReceiver);
        }
        this.mSecureArray.clear();
        super.onDestroyTasks();
    }

    @Override
    public void notifyNewMedia(Uri uri, boolean z) {
        super.notifyNewMedia(uri, z);
        if (z) {
            addSecureAlbumItem(uri);
        }
    }

    @Override
    protected void goToGallery(Uri uri) {
        if (uri == null) {
            LogHelper.d(TAG, "uri is null, can not go to gallery");
            return;
        }
        String type = getContentResolver().getType(uri);
        LogHelper.d(TAG, "[goToGallery] uri: " + uri + ", mimeType = " + type);
        Intent intent = new Intent("com.android.camera.action.REVIEW");
        intent.setDataAndType(uri, type);
        intent.putExtra("isSecureCamera", true);
        intent.putExtra("secureAlbum", this.mSecureArray);
        intent.putExtra("securePath", this.mPath);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogHelper.e(TAG, "[startGalleryActivity] Couldn't view ", e);
        }
    }

    private void setScreenFlags() {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.flags |= 524288;
        window.setAttributes(attributes);
    }

    private void addSecureAlbumItem(Uri uri) {
        LogHelper.d(TAG, "addSecureAlbumItem uri = " + uri + ", mSecureCamera = " + this.mSecureCamera);
        if (uri != null && this.mSecureCamera) {
            int i = Integer.parseInt(uri.getLastPathSegment());
            String type = getContentResolver().getType(uri);
            if (type == null) {
                LogHelper.e(TAG, "addSecureAlbumItem uri = " + uri + ", mSecureCamera = " + this.mSecureCamera);
                return;
            }
            this.mSecureArray.add(String.valueOf(i) + (type.startsWith("video/") ? "+true" : "+false"));
        }
    }

    private boolean isSecureUriLive(int i) throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQuery = MediaStore.Images.Media.query(getContentResolver(), MediaStore.Files.getContentUri("external"), null, "_id=(" + i + ")", null, null);
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return true;
            }
            try {
                LogHelper.w(TAG, "<isSecureUriLive> cursor " + cursorQuery.getCount());
                boolean z = cursorQuery.getCount() > 0;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return z;
            } catch (Throwable th) {
                cursor = cursorQuery;
                th = th;
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private boolean checkSecureAlbumLive() {
        int size = this.mSecureArray.size();
        LogHelper.d(TAG, "<checkSecureAlbumLive> albumCount " + size);
        for (int i = 0; i < size; i++) {
            try {
                String[] strArrSplit = this.mSecureArray.get(i).split("\\+");
                int length = strArrSplit.length;
                LogHelper.d(TAG, "<checkSecureAlbumLive> albumItemSize " + length);
                if (length == 2) {
                    int i2 = Integer.parseInt(strArrSplit[0].trim());
                    boolean z = Boolean.parseBoolean(strArrSplit[1].trim());
                    LogHelper.d(TAG, "<checkSecureAlbumLive> secure item : id " + i2 + ", isVideo " + z);
                    if (isSecureUriLive(i2)) {
                        return true;
                    }
                } else {
                    continue;
                }
            } catch (NullPointerException e) {
                LogHelper.e(TAG, "<checkSecureAlbumLive> NullPointerException " + e);
            } catch (NumberFormatException e2) {
                LogHelper.e(TAG, "<checkSecureAlbumLive> NumberFormatException " + e2);
            } catch (PatternSyntaxException e3) {
                LogHelper.e(TAG, "<checkSecureAlbumLive> PatternSyntaxException " + e3);
            }
        }
        return false;
    }
}
