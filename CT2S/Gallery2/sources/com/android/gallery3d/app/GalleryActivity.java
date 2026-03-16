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
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;

public final class GalleryActivity extends AbstractGalleryActivity implements DialogInterface.OnCancelListener {
    private Dialog mVersionCheckDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(8);
        requestWindowFeature(9);
        if (getIntent().getBooleanExtra("dismiss-keyguard", false)) {
            getWindow().addFlags(4194304);
        }
        setContentView(R.layout.main);
        if (savedInstanceState != null) {
            getStateManager().restoreFromState(savedInstanceState);
        } else {
            initializeByIntent();
        }
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.intent.action.GET_CONTENT".equalsIgnoreCase(action)) {
            startGetContent(intent);
            return;
        }
        if ("android.intent.action.PICK".equalsIgnoreCase(action)) {
            Log.w("GalleryActivity", "action PICK is not supported");
            String type = Utils.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image")) {
                    intent.setType("image/*");
                }
                if (type.endsWith("/video")) {
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
        Bundle data = new Bundle();
        data.putString("media-path", getDataManager().getTopSetPath(3));
        getStateManager().startState(AlbumSetPage.class, data);
        this.mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (this.mVersionCheckDialog != null) {
            this.mVersionCheckDialog.setOnCancelListener(this);
        }
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null ? new Bundle(intent.getExtras()) : new Bundle();
        data.putBoolean("get-content", true);
        int typeBits = GalleryUtils.determineTypeBits(this, intent);
        data.putInt("type-bits", typeBits);
        data.putString("media-path", getDataManager().getTopSetPath(typeBits));
        getStateManager().startState(AlbumSetPage.class, data);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) {
            return "application/vnd.google.panorama360+jpg".equals(type) ? "image/jpeg" : type;
        }
        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            Log.w("GalleryActivity", "get type fail", t);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = Boolean.valueOf(intent.getBooleanExtra("slideshow", false));
        if (slideshow.booleanValue()) {
            getActionBar().hide();
            DataManager manager = getDataManager();
            Path path = manager.findPathByUri(intent.getData(), intent.getType());
            if (path == null || (manager.getMediaObject(path) instanceof MediaItem)) {
                path = Path.fromString(manager.getTopSetPath(1));
            }
            Bundle data = new Bundle();
            data.putString("media-set-path", path.toString());
            data.putBoolean("random-order", true);
            data.putBoolean("repeat", true);
            if (intent.getBooleanExtra("dream", false)) {
                data.putBoolean("dream", true);
            }
            getStateManager().startState(SlideshowPage.class, data);
            return;
        }
        Bundle data2 = new Bundle();
        DataManager dm = getDataManager();
        Uri uri = intent.getData();
        String contentType = getContentType(intent);
        if (contentType == null) {
            Toast.makeText(this, R.string.no_such_item, 1).show();
            finish();
            return;
        }
        if (uri == null) {
            int typeBits = GalleryUtils.determineTypeBits(this, intent);
            data2.putInt("type-bits", typeBits);
            data2.putString("media-path", getDataManager().getTopSetPath(typeBits));
            getStateManager().startState(AlbumSetPage.class, data2);
            return;
        }
        if (contentType.startsWith("vnd.android.cursor.dir")) {
            int mediaType = intent.getIntExtra("mediaTypes", 0);
            if (mediaType != 0) {
                uri = uri.buildUpon().appendQueryParameter("mediaTypes", String.valueOf(mediaType)).build();
            }
            Path setPath = dm.findPathByUri(uri, null);
            MediaSet mediaSet = null;
            if (setPath != null) {
                mediaSet = (MediaSet) dm.getMediaObject(setPath);
            }
            if (mediaSet != null) {
                if (mediaSet.isLeafAlbum()) {
                    data2.putString("media-path", setPath.toString());
                    data2.putString("parent-media-path", dm.getTopSetPath(3));
                    getStateManager().startState(AlbumPage.class, data2);
                    return;
                } else {
                    data2.putString("media-path", setPath.toString());
                    getStateManager().startState(AlbumSetPage.class, data2);
                    return;
                }
            }
            startDefaultPage();
            return;
        }
        Path itemPath = dm.findPathByUri(uri, contentType);
        Path albumPath = dm.getDefaultSetOf(itemPath);
        data2.putString("media-item-path", itemPath.toString());
        data2.putBoolean("read-only", true);
        boolean singleItemOnly = albumPath == null || intent.getBooleanExtra("SingleItemOnly", false);
        if (!singleItemOnly) {
            data2.putString("media-set-path", albumPath.toString());
            if (intent.getBooleanExtra("treat-back-as-up", false) || (intent.getFlags() & 268435456) != 0) {
                data2.putBoolean("treat-back-as-up", true);
            }
        }
        getStateManager().startState(SinglePhotoPage.class, data2);
    }

    @Override
    protected void onResume() {
        Utils.assertTrue(getStateManager().getStateCount() > 0);
        super.onResume();
        if (this.mVersionCheckDialog != null) {
            this.mVersionCheckDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mVersionCheckDialog != null) {
            this.mVersionCheckDialog.dismiss();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == this.mVersionCheckDialog) {
            this.mVersionCheckDialog = null;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean isTouchPad = (event.getSource() & 8) != 0;
        if (isTouchPad) {
            float maxX = event.getDevice().getMotionRange(0).getMax();
            float maxY = event.getDevice().getMotionRange(1).getMax();
            View decor = getWindow().getDecorView();
            float scaleX = decor.getWidth() / maxX;
            float scaleY = decor.getHeight() / maxY;
            float x = event.getX() * scaleX;
            float y = event.getY() * scaleY;
            MotionEvent touchEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), x, y, event.getMetaState());
            return dispatchTouchEvent(touchEvent);
        }
        return super.onGenericMotionEvent(event);
    }
}
