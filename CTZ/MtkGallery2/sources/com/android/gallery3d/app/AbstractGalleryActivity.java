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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.print.PrintHelper;
import android.view.KeyEvent;
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
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallerybasic.base.IActivityCallback;
import com.mediatek.gallerybasic.base.MediaFilter;
import com.mediatek.gallerybasic.base.MediaFilterSetting;
import com.mediatek.galleryportable.SystemPropertyUtils;
import com.mediatek.plugin.preload.SoOperater;
import java.io.FileNotFoundException;

public class AbstractGalleryActivity extends Activity implements GalleryContext {
    private GalleryActionBar mActionBar;
    private BatchService mBatchService;
    private String mDefaultPath;
    private boolean mDisableToggleStatusBar;
    private EjectListener mEjectListener;
    private GLRootView mGLRootView;
    private volatile boolean mHasPausedActivity;
    private IActivityCallback[] mLifeCycleListeners;
    private MediaFilter mMediaFilter;
    private MultiWindowModeListener mMultiWindowModeListener;
    private OrientationManager mOrientationManager;
    private PanoramaViewHelper mPanoramaViewHelper;
    private boolean mStartPrintActivity;
    private StateManager mStateManager;
    private BroadcastReceiver mStorageReceiver;
    private TransitionStore mTransitionStore = new TransitionStore();
    private AlertDialog mAlertDialog = null;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractGalleryActivity.this.onStorageReady();
        }
    };
    private IntentFilter mMountFilter = null;
    private boolean mBatchServiceIsBound = false;
    private ServiceConnection mBatchServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AbstractGalleryActivity.this.mBatchService = ((BatchService.LocalBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            AbstractGalleryActivity.this.mBatchService = null;
        }
    };
    public boolean mShouldCheckStorageState = true;
    private boolean mDebugRenderLock = false;

    public interface EjectListener {
        void onEjectSdcard();
    }

    public interface MultiWindowModeListener {
        void onMultiWindowModeChanged(boolean z);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onCreate>");
        PhotoPlayFacade.initialize((GalleryAppImpl) getApplication(), MediaItem.getTargetSize(2), MediaItem.getTargetSize(1), MediaItem.getTargetSize(4));
        super.onCreate(bundle);
        this.mLifeCycleListeners = (IActivityCallback[]) FeatureManager.getInstance().getImplement(IActivityCallback.class, new Object[0]);
        for (IActivityCallback iActivityCallback : this.mLifeCycleListeners) {
            iActivityCallback.onCreate(this);
        }
        this.mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        getWindow().setBackgroundDrawable(null);
        this.mPanoramaViewHelper = new PanoramaViewHelper(this);
        this.mPanoramaViewHelper.onCreate();
        doBindBatchService();
        initializeMediaFilter();
        registerStorageReceiver();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        this.mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(bundle);
            getStateManager().saveState(bundle);
        } finally {
            this.mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mStateManager.onConfigurationChange(configuration);
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
    public void setContentView(int i) {
        super.setContentView(i);
        this.mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
        PhotoPlayFacade.registerMedias(getAndroidContext(), this.mGLRootView.getGLIdleExecuter());
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
        for (IActivityCallback iActivityCallback : this.mLifeCycleListeners) {
            iActivityCallback.onStart(this);
        }
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onStart> mShouldCheckStorageState = " + this.mShouldCheckStorageState);
        if (!this.mShouldCheckStorageState) {
            return;
        }
        if (FeatureHelper.getExternalCacheDir(this) == null && !FeatureHelper.isDefaultStorageMounted(this)) {
            AlertDialog.Builder onCancelListener = new AlertDialog.Builder(this).setTitle(R.string.no_external_storage_title).setMessage(R.string.no_external_storage).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    AbstractGalleryActivity.this.finish();
                }
            });
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(onCancelListener);
            } else {
                onCancelListener.setIcon(android.R.drawable.ic_dialog_alert);
            }
            this.mAlertDialog = onCancelListener.show();
            if (this.mMountFilter == null) {
                this.mMountFilter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
                this.mMountFilter.addDataScheme("file");
            }
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
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onStop>");
        super.onStop();
        for (IActivityCallback iActivityCallback : this.mLifeCycleListeners) {
            iActivityCallback.onStop(this);
        }
        if (this.mAlertDialog != null) {
            unregisterReceiver(this.mMountReceiver);
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
        }
        this.mPanoramaViewHelper.onStop();
    }

    @Override
    protected void onResume() {
        if (SystemPropertyUtils.getInt("gallery.debug.renderlock", 0) == 1) {
            this.mDebugRenderLock = true;
            this.mGLRootView.startDebug();
        }
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onResume>");
        super.onResume();
        for (IActivityCallback iActivityCallback : this.mLifeCycleListeners) {
            iActivityCallback.onResume(this);
        }
        restoreFilter();
        PhotoPlayFacade.registerMedias(getAndroidContext(), this.mGLRootView.getGLIdleExecuter());
        this.mGLRootView.lockRenderThread();
        MediaSetUtils.refreshBucketId();
        try {
            getStateManager().resume();
            getDataManager().resume();
            this.mGLRootView.unlockRenderThread();
            this.mGLRootView.onResume();
            this.mOrientationManager.resume();
            this.mHasPausedActivity = false;
            this.mGLRootView.setVisibility(0);
            if (this.mMultiWindowModeListener != null) {
                this.mMultiWindowModeListener.onMultiWindowModeChanged(isInMultiWindowMode());
            }
            this.mStartPrintActivity = false;
        } catch (Throwable th) {
            this.mGLRootView.unlockRenderThread();
            throw th;
        }
    }

    @Override
    protected void onPause() {
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onPause>");
        super.onPause();
        for (IActivityCallback iActivityCallback : this.mLifeCycleListeners) {
            iActivityCallback.onPause(this);
        }
        this.mOrientationManager.pause();
        this.mGLRootView.onPause();
        this.mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
            this.mGLRootView.unlockRenderThread();
            GalleryBitmapPool.getInstance().clear();
            MediaItem.getBytesBufferPool().clear();
            this.mHasPausedActivity = true;
            if (this.mDebugRenderLock) {
                this.mGLRootView.stopDebug();
                this.mDebugRenderLock = false;
            }
        } catch (Throwable th) {
            this.mGLRootView.unlockRenderThread();
            throw th;
        }
    }

    @Override
    protected void onDestroy() {
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onDestroy>");
        super.onDestroy();
        for (IActivityCallback iActivityCallback : this.mLifeCycleListeners) {
            iActivityCallback.onDestroy(this);
        }
        this.mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
            this.mGLRootView.unlockRenderThread();
            doUnbindBatchService();
            removeFilter();
            unregisterReceiver(this.mStorageReceiver);
        } catch (Throwable th) {
            this.mGLRootView.unlockRenderThread();
            throw th;
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        this.mGLRootView.lockRenderThread();
        try {
            if (getStateManager().getStateCount() == 0) {
                com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onActivityResult> no state, return");
            } else {
                getStateManager().notifyActivityResult(i, i2, intent);
            }
        } finally {
            this.mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        GLRoot gLRoot = getGLRoot();
        gLRoot.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            gLRoot.unlockRenderThread();
        }
    }

    public GalleryActionBar getGalleryActionBar() {
        if (this.mActionBar == null) {
            this.mActionBar = new GalleryActionBar(this);
        }
        return this.mActionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        GLRoot gLRoot = getGLRoot();
        gLRoot.lockRenderThread();
        try {
            return getStateManager().itemSelected(menuItem);
        } finally {
            gLRoot.unlockRenderThread();
        }
    }

    private void toggleStatusBarByOrientation() {
        if (this.mDisableToggleStatusBar) {
            return;
        }
        Window window = getWindow();
        if (isInMultiWindowMode()) {
            window.clearFlags(SoOperater.STEP);
        } else if (getResources().getConfiguration().orientation == 1) {
            window.clearFlags(SoOperater.STEP);
        } else {
            window.addFlags(SoOperater.STEP);
        }
    }

    public TransitionStore getTransitionStore() {
        return this.mTransitionStore;
    }

    public PanoramaViewHelper getPanoramaViewHelper() {
        return this.mPanoramaViewHelper;
    }

    protected boolean isFullscreen() {
        return (getWindow().getAttributes().flags & SoOperater.STEP) != 0;
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
        String lastPathSegment;
        if (uri == null) {
            return;
        }
        String localPathFromUri = ImageLoader.getLocalPathFromUri(this, uri);
        if (localPathFromUri != null) {
            lastPathSegment = Uri.parse(localPathFromUri).getLastPathSegment();
        } else {
            lastPathSegment = uri.getLastPathSegment();
        }
        if (this.mStartPrintActivity) {
            return;
        }
        this.mStartPrintActivity = true;
        try {
            new PrintHelper(this).printBitmap(lastPathSegment, uri);
        } catch (FileNotFoundException e) {
            com.android.gallery3d.ui.Log.e("Gallery2/AbstractGalleryActivity", "Error printing an image", e);
        }
    }

    private void initializeMediaFilter() {
        this.mMediaFilter = new MediaFilter();
        this.mMediaFilter.setFlagFromIntent(getIntent());
        if (!MediaFilterSetting.setCurrentFilter(this, this.mMediaFilter)) {
            com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<initializeMediaFilter> forceRefreshAll~");
            getDataManager().forceRefreshAll();
        }
    }

    private void restoreFilter() {
        boolean zRestoreFilter = MediaFilterSetting.restoreFilter(this);
        boolean z = !isDefaultPathChange();
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<restoreFilter> isFilterSame = " + zRestoreFilter + ", isFilePathSame = " + z);
        if (!zRestoreFilter || !z) {
            com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<restoreFilter> forceRefreshAll");
            getDataManager().forceRefreshAll();
        }
    }

    private void removeFilter() {
        MediaFilterSetting.removeFilter(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        this.mGLRootView.dispatchKeyEventView(keyEvent);
        return super.dispatchKeyEvent(keyEvent);
    }

    public void setEjectListener(EjectListener ejectListener) {
        this.mEjectListener = ejectListener;
    }

    private void registerStorageReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addDataScheme("file");
        this.mStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.MEDIA_EJECT".equals(intent.getAction()) && AbstractGalleryActivity.this.mEjectListener != null) {
                    AbstractGalleryActivity.this.mEjectListener.onEjectSdcard();
                }
            }
        };
        registerReceiver(this.mStorageReceiver, intentFilter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return getStateManager().onPrepareOptionsMenu(menu);
    }

    private boolean isDefaultPathChange() {
        boolean z = false;
        if (!this.mShouldCheckStorageState) {
            return false;
        }
        String defaultPath = FeatureHelper.getDefaultPath();
        if (this.mDefaultPath != null && !this.mDefaultPath.equals(defaultPath)) {
            z = true;
        }
        this.mDefaultPath = defaultPath;
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<isDefaultPathChange> mDefaultPath = " + this.mDefaultPath);
        return z;
    }

    public void setMultiWindowModeListener(MultiWindowModeListener multiWindowModeListener) {
        this.mMultiWindowModeListener = multiWindowModeListener;
    }

    @Override
    public void onMultiWindowModeChanged(boolean z) {
        toggleStatusBarByOrientation();
        com.android.gallery3d.ui.Log.d("Gallery2/AbstractGalleryActivity", "<onMultiWindowModeChanged> isInMultiWindowMode " + z);
        if (this.mMultiWindowModeListener != null) {
            this.mMultiWindowModeListener.onMultiWindowModeChanged(z);
        }
    }

    @Override
    public boolean isInMultiWindowMode() {
        if (Build.VERSION.SDK_INT >= 24) {
            return super.isInMultiWindowMode();
        }
        return false;
    }
}
