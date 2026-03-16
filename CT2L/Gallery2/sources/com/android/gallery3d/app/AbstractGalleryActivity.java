package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import com.android.gallery3d.R;
import com.android.gallery3d.app.BatchService;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import java.io.FileNotFoundException;

public class AbstractGalleryActivity extends Activity implements GalleryContext {
    private GalleryActionBar mActionBar;
    private BatchService mBatchService;
    private boolean mDisableToggleStatusBar;
    private GLRootView mGLRootView;
    private OrientationManager mOrientationManager;
    private PanoramaViewHelper mPanoramaViewHelper;
    private StateManager mStateManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private AlertDialog mAlertDialog = null;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AbstractGalleryActivity.this.getExternalCacheDir() != null) {
                AbstractGalleryActivity.this.onStorageReady();
            }
        }
    };
    private IntentFilter mMountFilter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
    private boolean mBatchServiceIsBound = false;
    private ServiceConnection mBatchServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            AbstractGalleryActivity.this.mBatchService = ((BatchService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            AbstractGalleryActivity.this.mBatchService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        getWindow().setBackgroundDrawable(null);
        this.mPanoramaViewHelper = new PanoramaViewHelper(this);
        this.mPanoramaViewHelper.onCreate();
        doBindBatchService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        this.mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getStateManager().saveState(outState);
        } finally {
            this.mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        this.mStateManager.onConfigurationChange(config);
        getGalleryActionBar().onConfigurationChanged();
        invalidateOptionsMenu();
        toggleStatusBarByOrientation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    public DataManager getDataManager() {
        return ((GalleryApp) getApplication()).getDataManager();
    }

    @Override
    public ThreadPool getThreadPool() {
        return ((GalleryApp) getApplication()).getThreadPool();
    }

    public synchronized StateManager getStateManager() {
        if (this.mStateManager == null) {
            this.mStateManager = new StateManager(this);
        }
        return this.mStateManager;
    }

    public GLRoot getGLRoot() {
        return this.mGLRootView;
    }

    public OrientationManager getOrientationManager() {
        return this.mOrientationManager;
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        this.mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    protected void onStorageReady() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
            unregisterReceiver(this.mMountReceiver);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getExternalCacheDir() == null) {
            DialogInterface.OnCancelListener onCancel = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    AbstractGalleryActivity.this.finish();
                }
            };
            DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.no_external_storage_title).setMessage(R.string.no_external_storage).setNegativeButton(android.R.string.cancel, onClick).setOnCancelListener(onCancel);
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(builder);
            } else {
                builder.setIcon(android.R.drawable.ic_dialog_alert);
            }
            this.mAlertDialog = builder.show();
            registerReceiver(this.mMountReceiver, this.mMountFilter);
        }
        this.mPanoramaViewHelper.onStart();
    }

    @TargetApi(11)
    private static void setAlertDialogIconAttribute(AlertDialog.Builder builder) {
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mAlertDialog != null) {
            unregisterReceiver(this.mMountReceiver);
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
        }
        this.mPanoramaViewHelper.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mGLRootView.lockRenderThread();
        try {
            getStateManager().resume();
            getDataManager().resume();
            this.mGLRootView.unlockRenderThread();
            this.mGLRootView.onResume();
            this.mOrientationManager.resume();
        } catch (Throwable th) {
            this.mGLRootView.unlockRenderThread();
            throw th;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mOrientationManager.pause();
        this.mGLRootView.onPause();
        this.mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
            this.mGLRootView.unlockRenderThread();
            GalleryBitmapPool.getInstance().clear();
            MediaItem.getBytesBufferPool().clear();
        } catch (Throwable th) {
            this.mGLRootView.unlockRenderThread();
            throw th;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
            this.mGLRootView.unlockRenderThread();
            doUnbindBatchService();
        } catch (Throwable th) {
            this.mGLRootView.unlockRenderThread();
            throw th;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.mGLRootView.lockRenderThread();
        try {
            getStateManager().notifyActivityResult(requestCode, resultCode, data);
        } finally {
            this.mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    public GalleryActionBar getGalleryActionBar() {
        if (this.mActionBar == null) {
            this.mActionBar = new GalleryActionBar(this);
        }
        return this.mActionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    private void toggleStatusBarByOrientation() {
        if (!this.mDisableToggleStatusBar) {
            Window win = getWindow();
            if (getResources().getConfiguration().orientation == 1) {
                win.clearFlags(1024);
            } else {
                win.addFlags(1024);
            }
        }
    }

    public TransitionStore getTransitionStore() {
        return this.mTransitionStore;
    }

    public PanoramaViewHelper getPanoramaViewHelper() {
        return this.mPanoramaViewHelper;
    }

    protected boolean isFullscreen() {
        return (getWindow().getAttributes().flags & 1024) != 0;
    }

    private void doBindBatchService() {
        bindService(new Intent(this, (Class<?>) BatchService.class), this.mBatchServiceConnection, 1);
        this.mBatchServiceIsBound = true;
    }

    private void doUnbindBatchService() {
        if (this.mBatchServiceIsBound) {
            unbindService(this.mBatchServiceConnection);
            this.mBatchServiceIsBound = false;
        }
    }

    public ThreadPool getBatchServiceThreadPoolIfAvailable() {
        if (this.mBatchServiceIsBound && this.mBatchService != null) {
            return this.mBatchService.getThreadPool();
        }
        throw new RuntimeException("Batch service unavailable");
    }

    public void printSelectedImage(Uri uri) {
        String path;
        if (uri != null) {
            String path2 = ImageLoader.getLocalPathFromUri(this, uri);
            if (path2 != null) {
                Uri localUri = Uri.parse(path2);
                path = localUri.getLastPathSegment();
            } else {
                path = uri.getLastPathSegment();
            }
            PrintHelper printer = new PrintHelper(this);
            try {
                printer.printBitmap(path, uri);
            } catch (FileNotFoundException fnfe) {
                Log.e("AbstractGalleryActivity", "Error printing an image", fnfe);
            }
        }
    }
}
