package com.android.music;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import com.mediatek.omadrm.OmaDrmUtils;

public class AudioPreviewStarter extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private static boolean isMusicPermissionGrant = true;
    private DrmManagerClient mDrmClient = null;
    private Intent mIntent;
    Bundle mSavedInstanceState;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSavedInstanceState = bundle;
        MusicLogUtils.d("AudioPreStarter", ">> onCreate of AudioPreviewStarter");
        if (getApplicationContext().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            MusicLogUtils.d("AudioPreStarter", "onCreate Permissions not granted");
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);
        } else {
            onCreateContinue(this.mSavedInstanceState);
        }
    }

    public void onCreateContinue(Bundle bundle) {
        MusicLogUtils.d("AudioPreStarter", ">> onCreateontinue of AudioPreviewStarter");
        isMusicPermissionGrant = true;
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null) {
            finish();
            return;
        }
        MusicLogUtils.d("AudioPreStarter", "uri=" + data);
        this.mIntent = new Intent(getIntent());
        this.mIntent.setClass(this, AudioPreview.class);
        if (!OmaDrmUtils.isOmaDrmEnabled()) {
            MusicLogUtils.d("AudioPreStarter", "DRM is off");
            startActivity(this.mIntent);
            finish();
            return;
        }
        this.mDrmClient = new DrmManagerClient(this);
        String action = intent.getAction();
        if ("com.mediatek.dataprotection.ACTION_VIEW_LOCKED_FILE".equals(action)) {
            String stringExtra = intent.getStringExtra("TOKEN");
            String stringExtra2 = intent.getStringExtra("TOKEN_KEY");
            MusicLogUtils.d("AudioPreStarter", "onCreate: action = " + action + ", tokenKey = " + stringExtra2 + ", token = " + stringExtra);
            if (stringExtra == null || !OmaDrmUtils.isTokenValid(this.mDrmClient, stringExtra2, stringExtra)) {
                finish();
            }
        }
        processForDrm(data);
        MusicLogUtils.d("AudioPreStarter", "<< onCreate");
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1) {
            if (iArr.length > 0 && iArr[0] == 0) {
                isMusicPermissionGrant = true;
                onCreateContinue(this.mSavedInstanceState);
            } else {
                isMusicPermissionGrant = false;
                finish();
                Toast.makeText(this, R.string.music_storage_permission_deny, 0).show();
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            MusicLogUtils.d("AudioPreStarter", "onClick: BUTTON_POSITIVE");
            startActivity(this.mIntent);
            finish();
        } else if (i == -2) {
            finish();
            MusicLogUtils.d("AudioPreStarter", "onClick: BUTTON_NEGATIVE");
        } else {
            MusicLogUtils.d("AudioPreStarter", "undefined button on DRM consume dialog!");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        MusicLogUtils.d("AudioPreStarter", "onDismiss");
        finish();
    }

    private void processForDrm(Uri uri) {
        Cursor cursorQuery;
        String scheme = uri.getScheme();
        String host = uri.getHost();
        MusicLogUtils.d("AudioPreStarter", "scheme=" + scheme + ", host=" + host);
        ContentResolver contentResolver = getContentResolver();
        if ("content".equals(scheme) && "media".equals(host)) {
            cursorQuery = contentResolver.query(uri, new String[]{"_id", "is_drm"}, null, null, null);
        } else if ("file".equals(scheme)) {
            String strReplaceAll = uri.getPath().replaceAll("'", "''");
            MusicLogUtils.d("AudioPreStarter", "file path=" + strReplaceAll);
            if (strReplaceAll == null) {
                finish();
                return;
            }
            Uri uri2 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            StringBuilder sb = new StringBuilder();
            sb.append("_data='" + strReplaceAll + "'");
            cursorQuery = contentResolver.query(uri2, new String[]{"_id", "is_drm"}, sb.toString(), null, null);
        } else {
            cursorQuery = null;
        }
        boolean z = false;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("is_drm"));
                    MusicLogUtils.d("AudioPreStarter", "isDrm=" + i);
                    if (i == 1) {
                        OmaDrmUtils.showConsumerDialog(this, this.mDrmClient, uri, this);
                        return;
                    }
                    z = true;
                }
            } finally {
                cursorQuery.close();
            }
        }
        if (!z) {
            String strReplaceAll2 = uri.getPath().replaceAll("'", "''");
            if (OmaDrmUtils.isDrm(this.mDrmClient, uri)) {
                OmaDrmUtils.showConsumerDialog(this, this.mDrmClient, uri, this);
                return;
            }
            MusicLogUtils.d("AudioPreStarter", "drm file is not in db, isDrm returns false=" + strReplaceAll2);
        }
        startActivity(this.mIntent);
        finish();
    }

    @Override
    protected void onResume() {
        if (!isMusicPermissionGrant) {
            finish();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (isMusicPermissionGrant && this.mDrmClient != null) {
            this.mDrmClient.release();
            this.mDrmClient = null;
        }
        super.onDestroy();
    }
}
