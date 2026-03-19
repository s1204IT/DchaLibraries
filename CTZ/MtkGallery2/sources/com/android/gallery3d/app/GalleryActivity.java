package com.android.gallery3d.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetUtils;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.galleryportable.TraceHelper;

public final class GalleryActivity extends AbstractGalleryActivity implements DialogInterface.OnCancelListener {
    private Bundle mSaveInstanceState;
    public long mStopTime = 0;
    private Dialog mVersionCheckDialog;

    @Override
    protected void onCreate(Bundle bundle) {
        View viewFindViewById;
        TraceHelper.beginSection(">>>>Gallery-onCreate");
        super.onCreate(bundle);
        requestWindowFeature(8);
        requestWindowFeature(9);
        if (getIntent().getBooleanExtra("dismiss-keyguard", false)) {
            getWindow().addFlags(4194304);
        }
        setContentView(R.layout.main);
        Intent intent = getIntent();
        if (intent != null && ((intent.getBooleanExtra("fromWidget", false) || (intent.getAction() != null && intent.getAction().equals("android.intent.action.MAIN"))) && (viewFindViewById = findViewById(R.id.gl_root_cover)) != null)) {
            viewFindViewById.setVisibility(0);
            Log.d("Gallery2/GalleryActivity", "<onCreate> from widget or launcher, set gl_root_cover VISIBLE");
        }
        if (PermissionHelper.checkAndRequestForGallery(this)) {
            if (bundle != null) {
                getStateManager().restoreFromState(bundle);
            } else {
                initializeByIntent();
            }
        } else {
            this.mSaveInstanceState = bundle;
        }
        FeatureHelper.modifyBoostPolicy(this);
        TraceHelper.endSection();
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.intent.action.GET_CONTENT".equalsIgnoreCase(action)) {
            startGetContent(intent);
            return;
        }
        if ("android.intent.action.PICK".equalsIgnoreCase(action)) {
            Log.w("Gallery2/GalleryActivity", "action PICK is not supported");
            String strEnsureNotNull = Utils.ensureNotNull(intent.getType());
            if (strEnsureNotNull.startsWith("vnd.android.cursor.dir/")) {
                if (strEnsureNotNull.endsWith("/image")) {
                    intent.setType("image/*");
                }
                if (strEnsureNotNull.endsWith("/video")) {
                    intent.setType("video/*");
                }
            }
            startGetContent(intent);
            return;
        }
        if ("android.intent.action.VIEW".equalsIgnoreCase(action) || "com.android.camera.action.REVIEW".equalsIgnoreCase(action)) {
            startViewAction(intent);
        } else {
            startDefaultPage();
        }
    }

    public void startDefaultPage() {
        PicasaSource.showSignInReminder(this);
        Bundle bundle = new Bundle();
        bundle.putString("media-path", getDataManager().getTopSetPath(3));
        getStateManager().startState(AlbumSetPage.class, bundle);
        this.mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (this.mVersionCheckDialog != null) {
            this.mVersionCheckDialog.setOnCancelListener(this);
        }
    }

    private void startGetContent(Intent intent) {
        Bundle bundle;
        if (intent.getExtras() != null) {
            bundle = new Bundle(intent.getExtras());
        } else {
            bundle = new Bundle();
        }
        bundle.putBoolean("get-content", true);
        int iDetermineTypeBits = GalleryUtils.determineTypeBits(this, intent);
        bundle.putInt("type-bits", iDetermineTypeBits);
        bundle.putString("media-path", getDataManager().getTopSetPath(iDetermineTypeBits));
        getStateManager().startState(AlbumSetPage.class, bundle);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) {
            if (!"application/vnd.google.panorama360+jpg".equals(type)) {
                return type;
            }
            return "image/jpeg";
        }
        try {
            return getContentResolver().getType(intent.getData());
        } catch (Throwable th) {
            Log.w("Gallery2/GalleryActivity", "get type fail", th);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        if (Boolean.valueOf(intent.getBooleanExtra("slideshow", false)).booleanValue()) {
            getActionBar().hide();
            DataManager dataManager = getDataManager();
            Path pathFindPathByUri = dataManager.findPathByUri(intent.getData(), intent.getType());
            if (pathFindPathByUri == null || (dataManager.getMediaObject(pathFindPathByUri) instanceof MediaItem)) {
                pathFindPathByUri = Path.fromString(dataManager.getTopSetPath(1));
            }
            Bundle bundle = new Bundle();
            bundle.putString("media-set-path", pathFindPathByUri.toString());
            bundle.putBoolean("random-order", true);
            bundle.putBoolean("repeat", true);
            if (intent.getBooleanExtra("dream", false)) {
                bundle.putBoolean("dream", true);
            }
            getStateManager().startState(SlideshowPage.class, bundle);
            return;
        }
        Bundle bundle2 = new Bundle();
        DataManager dataManager2 = getDataManager();
        Uri data = intent.getData();
        String contentType = getContentType(intent);
        if (contentType == null) {
            Toast.makeText(this, R.string.no_such_item, 1).show();
            finish();
            return;
        }
        if (data == null) {
            int iDetermineTypeBits = GalleryUtils.determineTypeBits(this, intent);
            bundle2.putInt("type-bits", iDetermineTypeBits);
            bundle2.putString("media-path", getDataManager().getTopSetPath(iDetermineTypeBits));
            getStateManager().startState(AlbumSetPage.class, bundle2);
            return;
        }
        if (contentType.startsWith("vnd.android.cursor.dir")) {
            int intExtra = intent.getIntExtra("mediaTypes", 0);
            if (intExtra != 0) {
                data = data.buildUpon().appendQueryParameter("mediaTypes", String.valueOf(intExtra)).build();
            }
            MediaSet mediaSet = null;
            Path pathFindPathByUri2 = dataManager2.findPathByUri(data, null);
            if (pathFindPathByUri2 != null) {
                mediaSet = (MediaSet) dataManager2.getMediaObject(pathFindPathByUri2);
            }
            if (mediaSet != null) {
                if (mediaSet.isLeafAlbum()) {
                    bundle2.putString("media-path", pathFindPathByUri2.toString());
                    bundle2.putString("parent-media-path", dataManager2.getTopSetPath(3));
                    getStateManager().startState(AlbumPage.class, bundle2);
                    return;
                } else {
                    bundle2.putString("media-path", pathFindPathByUri2.toString());
                    getStateManager().startState(AlbumSetPage.class, bundle2);
                    return;
                }
            }
            startDefaultPage();
            return;
        }
        Uri uriTryContentMediaUri = FeatureHelper.tryContentMediaUri(this, data);
        Log.d("Gallery2/GalleryActivity", "<startViewAction> uri:" + uriTryContentMediaUri);
        if (!FeatureHelper.isLocalUri(uriTryContentMediaUri)) {
            Log.d("Gallery2/GalleryActivity", "<startViewAction>: uri=" + uriTryContentMediaUri + ", not local!!");
            this.mShouldCheckStorageState = false;
        }
        Path pathFindPathByUri3 = dataManager2.findPathByUri(uriTryContentMediaUri, contentType);
        if (pathFindPathByUri3 == null) {
            Toast.makeText(this, R.string.no_such_item, 1).show();
            finish();
            return;
        }
        pathFindPathByUri3.clearObject();
        Path defaultSetOf = dataManager2.getDefaultSetOf(pathFindPathByUri3);
        bundle2.putString("media-item-path", pathFindPathByUri3.toString());
        bundle2.putBoolean("read-only", true);
        if (!(defaultSetOf == null || intent.getBooleanExtra("SingleItemOnly", false))) {
            bundle2.putString("media-set-path", defaultSetOf.toString());
            if (intent.getBooleanExtra("treat-back-as-up", false) || (intent.getFlags() & 268435456) != 0) {
                bundle2.putBoolean("treat-back-as-up", true);
            }
        }
        FeatureHelper.setExtBundle(this, intent, bundle2, pathFindPathByUri3);
        getStateManager().startState(SinglePhotoPage.class, bundle2);
    }

    @Override
    protected void onResume() {
        TraceHelper.beginSection(">>>>Gallery-onResume");
        super.onResume();
        if (this.mVersionCheckDialog != null) {
            this.mVersionCheckDialog.show();
        }
        TraceHelper.endSection();
        this.mStopTime = 0L;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mVersionCheckDialog != null) {
            this.mVersionCheckDialog.dismiss();
        }
        this.mStopTime = System.currentTimeMillis();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (dialogInterface == this.mVersionCheckDialog) {
            this.mVersionCheckDialog = null;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if ((motionEvent.getSource() & 8) != 0) {
            float max = motionEvent.getDevice().getMotionRange(0).getMax();
            float max2 = motionEvent.getDevice().getMotionRange(1).getMax();
            View decorView = getWindow().getDecorView();
            return dispatchTouchEvent(MotionEvent.obtain(motionEvent.getDownTime(), motionEvent.getEventTime(), motionEvent.getAction(), motionEvent.getX() * (decorView.getWidth() / max), motionEvent.getY() * (decorView.getHeight() / max2), motionEvent.getMetaState()));
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    @Override
    protected void onStart() {
        TraceHelper.beginSection(">>>>Gallery-onStart");
        super.onStart();
        TraceHelper.endSection();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        WidgetUtils.notifyAllWidgetViewChanged();
        if (getStateManager().getStateCount() != 0) {
            Log.d("Gallery2/GalleryActivity", "<onRequestPermissionsResult> dispatch to ActivityState");
            getStateManager().getTopState().onRequestPermissionsResult(i, strArr, iArr);
            return;
        }
        if (PermissionHelper.isAllPermissionsGranted(strArr, iArr)) {
            Log.d("Gallery2/GalleryActivity", "<onRequestPermissionsResult> all permission granted");
            if (this.mSaveInstanceState != null) {
                getStateManager().restoreFromState(this.mSaveInstanceState);
                return;
            } else {
                initializeByIntent();
                return;
            }
        }
        int i2 = 0;
        while (true) {
            if (i2 < strArr.length) {
                if ("android.permission.READ_EXTERNAL_STORAGE".equals(strArr[i2]) && iArr[i2] == -1) {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                } else if (!"android.permission.WRITE_EXTERNAL_STORAGE".equals(strArr[i2]) || iArr[i2] != -1) {
                    i2++;
                } else {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                }
            } else {
                break;
            }
        }
        Log.d("Gallery2/GalleryActivity", "<onRequestPermissionsResult> permission denied, finish");
        finish();
    }
}
