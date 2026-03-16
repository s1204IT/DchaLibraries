package com.android.camera;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.text.TextUtils;
import android.util.CameraPerformanceTracker;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.CameraController;
import com.android.camera.app.CameraProvider;
import com.android.camera.app.CameraServices;
import com.android.camera.app.LocationManager;
import com.android.camera.app.MemoryManager;
import com.android.camera.app.MemoryQuery;
import com.android.camera.app.ModuleManager;
import com.android.camera.app.ModuleManagerImpl;
import com.android.camera.app.MotionManager;
import com.android.camera.app.OrientationManager;
import com.android.camera.app.OrientationManagerImpl;
import com.android.camera.data.CameraDataAdapter;
import com.android.camera.data.FixedLastDataAdapter;
import com.android.camera.data.LocalData;
import com.android.camera.data.LocalDataAdapter;
import com.android.camera.data.LocalDataUtil;
import com.android.camera.data.LocalDataViewType;
import com.android.camera.data.LocalMediaData;
import com.android.camera.data.LocalMediaObserver;
import com.android.camera.data.LocalSessionData;
import com.android.camera.data.MediaDetails;
import com.android.camera.data.MetadataLoader;
import com.android.camera.data.PanoramaMetadataLoader;
import com.android.camera.data.RgbzMetadataLoader;
import com.android.camera.data.SimpleViewData;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.filmstrip.FilmstripController;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.hardware.HardwareSpecImpl;
import com.android.camera.module.ModuleController;
import com.android.camera.module.ModulesInfo;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraManager;
import com.android.camera.session.CaptureSession;
import com.android.camera.session.CaptureSessionManager;
import com.android.camera.settings.AppUpgrader;
import com.android.camera.settings.CameraSettingsActivity;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.tinyplanet.TinyPlanetFragment;
import com.android.camera.ui.AbstractTutorialOverlay;
import com.android.camera.ui.DetailsDialog;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.ui.ModeListView;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.Callback;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GalleryHelper;
import com.android.camera.util.GcamHelper;
import com.android.camera.util.GoogleHelpHelper;
import com.android.camera.util.IntentHelper;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.util.QuickActivity;
import com.android.camera.util.ReleaseHelper;
import com.android.camera.util.UsageStatistics;
import com.android.camera.widget.FilmstripView;
import com.android.camera.widget.Preloader;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraExceptionHandler;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends QuickActivity implements AppController, CameraAgent.CameraOpenCallback, ShareActionProvider.OnShareTargetSelectedListener, OrientationManager.OnOrientationChangeListener {
    public static final String ACTION_IMAGE_CAPTURE_SECURE = "android.media.action.IMAGE_CAPTURE_SECURE";
    public static final String CAMERA_SCOPE_PREFIX = "_preferences_camera_";
    private static final int FILMSTRIP_PRELOAD_AHEAD_ITEMS = 10;
    private static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE = "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    private static final int LIGHTS_OUT_DELAY_MS = 4000;
    private static final int MAX_PEEK_BITMAP_PIXELS = 1600000;
    public static final String MODULE_SCOPE_PREFIX = "_preferences_module_";
    private static final int MSG_CLEAR_SCREEN_ON_FLAG = 2;
    private static final long SCREEN_DELAY_MS = 120000;
    public static final String SECURE_CAMERA_EXTRA = "secure_camera";
    private static final Log.Tag TAG = new Log.Tag("CameraActivity");
    private FrameLayout mAboveFilmstripControlLayout;
    private ActionBar mActionBar;
    private Menu mActionBarMenu;
    private Context mAppContext;
    private boolean mAutoRotateScreen;
    private ButtonManager mButtonManager;
    private CameraAppUI mCameraAppUI;
    private CameraController mCameraController;
    private OneCameraManager mCameraManager;
    private int mCurrentModeIndex;
    private CameraModule mCurrentModule;
    private LocalDataAdapter mDataAdapter;
    private FilmstripController mFilmstripController;
    private boolean mFilmstripVisible;
    private Intent mGalleryIntent;
    private boolean mKeepScreenOn;
    private int mLastLayoutOrientation;
    private int mLastRawOrientation;
    private LocalMediaObserver mLocalImagesObserver;
    private LocalMediaObserver mLocalVideosObserver;
    private LocationManager mLocationManager;
    private Handler mMainHandler;
    private MemoryManager mMemoryManager;
    private ModeListView mModeListView;
    private ModuleManagerImpl mModuleManager;
    private MotionManager mMotionManager;
    private long mOnCreateTime;
    private OrientationManagerImpl mOrientationManager;
    private PhotoSphereHelper.PanoramaViewHelper mPanoramaViewHelper;
    private boolean mPaused;
    private PeekAnimationHandler mPeekAnimationHandler;
    private HandlerThread mPeekAnimationThread;
    private Preloader<Integer, AsyncTask> mPreloader;
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    private boolean mSecureCamera;
    private SettingsManager mSettingsManager;
    private SoundPlayer mSoundPlayer;
    private OnScreenHint mStorageHint;
    private ViewGroup mUndoDeletionBar;
    private boolean mCameraFatalError = false;
    private boolean mResetToPreviewOnResume = true;
    private boolean mModeListVisible = false;
    private boolean mFilmstripCoversPreview = false;
    private final Object mStorageSpaceLock = new Object();
    private long mStorageSpaceBytes = Storage.LOW_STORAGE_THRESHOLD_BYTES;
    private boolean mIsUndoingDeletion = false;
    private boolean mIsActivityRunning = false;
    private final Uri[] mNfcPushUris = new Uri[1];
    private boolean mPendingDeletion = false;
    private final int BASE_SYS_UI_VISIBILITY = 1280;
    private final Runnable mLightsOutRunnable = new Runnable() {
        @Override
        public void run() {
            CameraActivity.this.getWindow().getDecorView().setSystemUiVisibility(1281);
        }
    };
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CameraActivity.this.finish();
        }
    };
    private final CameraAppUI.BottomPanel.Listener mMyFilmstripBottomControlListener = new CameraAppUI.BottomPanel.Listener() {
        @Override
        public void onExternalViewer() {
            if (CameraActivity.this.mPanoramaViewHelper != null) {
                LocalData data = getCurrentLocalData();
                if (data == null) {
                    Log.w(CameraActivity.TAG, "Cannot open null data.");
                    return;
                }
                Uri contentUri = data.getUri();
                if (contentUri == Uri.EMPTY) {
                    Log.w(CameraActivity.TAG, "Cannot open empty URL.");
                    return;
                }
                if (PanoramaMetadataLoader.isPanoramaAndUseViewer(data)) {
                    CameraActivity.this.mPanoramaViewHelper.showPanorama(CameraActivity.this, contentUri);
                    return;
                }
                if (RgbzMetadataLoader.hasRGBZData(data)) {
                    CameraActivity.this.mPanoramaViewHelper.showRgbz(contentUri);
                    if (CameraActivity.this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING)) {
                        CameraActivity.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING, false);
                        CameraActivity.this.mCameraAppUI.clearClingForViewer(2);
                    }
                }
            }
        }

        @Override
        public void onEdit() {
            LocalData data = getCurrentLocalData();
            if (data == null) {
                Log.w(CameraActivity.TAG, "Cannot edit null data.");
                return;
            }
            int currentDataId = getCurrentDataId();
            UsageStatistics.instance().mediaInteraction(CameraActivity.this.fileNameFromDataID(currentDataId), 10000, 10000, CameraActivity.this.fileAgeFromDataID(currentDataId));
            CameraActivity.this.launchEditor(data);
        }

        @Override
        public void onTinyPlanet() {
            LocalData data = getCurrentLocalData();
            if (data == null) {
                Log.w(CameraActivity.TAG, "Cannot edit tiny planet on null data.");
            } else {
                CameraActivity.this.launchTinyPlanetEditor(data);
            }
        }

        @Override
        public void onDelete() {
            int currentDataId = getCurrentDataId();
            UsageStatistics.instance().mediaInteraction(CameraActivity.this.fileNameFromDataID(currentDataId), 10000, 10000, CameraActivity.this.fileAgeFromDataID(currentDataId));
            CameraActivity.this.removeData(currentDataId);
        }

        @Override
        public void onShare() {
            final LocalData data = getCurrentLocalData();
            if (data == null) {
                Log.w(CameraActivity.TAG, "Cannot share null data.");
                return;
            }
            int currentDataId = getCurrentDataId();
            UsageStatistics.instance().mediaInteraction(CameraActivity.this.fileNameFromDataID(currentDataId), 10000, 10000, CameraActivity.this.fileAgeFromDataID(currentDataId));
            if (ReleaseHelper.shouldShowReleaseInfoDialogOnShare(data)) {
                ReleaseHelper.showReleaseInfoDialog(CameraActivity.this, new Callback<Void>() {
                    @Override
                    public void onCallback(Void result) {
                        share(data);
                    }
                });
            } else {
                share(data);
            }
        }

        private void share(LocalData data) {
            Intent shareIntent = getShareIntentByData(data);
            if (shareIntent != null) {
                try {
                    CameraActivity.this.launchActivityByIntent(shareIntent);
                    CameraActivity.this.mCameraAppUI.getFilmstripBottomControls().setShareEnabled(false);
                } catch (ActivityNotFoundException e) {
                }
            }
        }

        private int getCurrentDataId() {
            return CameraActivity.this.mFilmstripController.getCurrentId();
        }

        private LocalData getCurrentLocalData() {
            return CameraActivity.this.mDataAdapter.getLocalData(getCurrentDataId());
        }

        private Intent getShareIntentByData(LocalData data) {
            Uri contentUri = data.getUri();
            String msgShareTo = CameraActivity.this.getResources().getString(R.string.share_to);
            if (PanoramaMetadataLoader.isPanorama360(data) && data.getUri() != Uri.EMPTY) {
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("application/vnd.google.panorama360+jpg");
                intent.putExtra("android.intent.extra.STREAM", contentUri);
                return intent;
            }
            if (!data.isDataActionSupported(8)) {
                return null;
            }
            String mimeType = data.getMimeType();
            Intent intent2 = getShareIntentFromType(mimeType);
            if (intent2 != null) {
                intent2.putExtra("android.intent.extra.STREAM", contentUri);
                intent2.addFlags(1);
            }
            return Intent.createChooser(intent2, msgShareTo);
        }

        private Intent getShareIntentFromType(String mimeType) {
            Intent intent = new Intent("android.intent.action.SEND");
            if (mimeType.startsWith("video/")) {
                intent.setType("video/*");
            } else if (!mimeType.startsWith("image/")) {
                Log.w(CameraActivity.TAG, "unsupported mimeType " + mimeType);
            } else {
                intent.setType("image/*");
            }
            return intent;
        }

        @Override
        public void onProgressErrorClicked() {
            LocalData data = getCurrentLocalData();
            CameraActivity.this.getServices().getCaptureSessionManager().removeErrorMessage(data.getUri());
            CameraActivity.this.updateBottomControlsByData(data);
        }
    };
    private final FilmstripContentPanel.Listener mFilmstripListener = new FilmstripContentPanel.Listener() {
        @Override
        public void onSwipeOut() {
        }

        @Override
        public void onSwipeOutBegin() {
            CameraActivity.this.mActionBar.hide();
            CameraActivity.this.mCameraAppUI.hideBottomControls();
            CameraActivity.this.mFilmstripCoversPreview = false;
            CameraActivity.this.updatePreviewVisibility();
        }

        @Override
        public void onFilmstripHidden() {
            CameraActivity.this.mFilmstripVisible = false;
            UsageStatistics.instance().changeScreen(CameraActivity.this.currentUserInterfaceMode(), 10000);
            CameraActivity.this.setFilmstripUiVisibility(false);
            CameraActivity.this.mFilmstripController.goToFirstItem();
        }

        @Override
        public void onFilmstripShown() {
            CameraActivity.this.mFilmstripVisible = true;
            UsageStatistics.instance().changeScreen(CameraActivity.this.currentUserInterfaceMode(), 10000);
            CameraActivity.this.updateUiByData(CameraActivity.this.mFilmstripController.getCurrentId());
        }

        @Override
        public void onFocusedDataLongPressed(int dataId) {
        }

        @Override
        public void onFocusedDataPromoted(int dataID) {
            UsageStatistics.instance().mediaInteraction(CameraActivity.this.fileNameFromDataID(dataID), 10000, 10000, CameraActivity.this.fileAgeFromDataID(dataID));
            CameraActivity.this.removeData(dataID);
        }

        @Override
        public void onFocusedDataDemoted(int dataID) {
            UsageStatistics.instance().mediaInteraction(CameraActivity.this.fileNameFromDataID(dataID), 10000, 10000, CameraActivity.this.fileAgeFromDataID(dataID));
            CameraActivity.this.removeData(dataID);
        }

        @Override
        public void onEnterFullScreenUiShown(int dataId) {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.setFilmstripUiVisibility(true);
            }
        }

        @Override
        public void onLeaveFullScreenUiShown(int dataId) {
        }

        @Override
        public void onEnterFullScreenUiHidden(int dataId) {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.setFilmstripUiVisibility(false);
            }
        }

        @Override
        public void onLeaveFullScreenUiHidden(int dataId) {
        }

        @Override
        public void onEnterFilmstrip(int dataId) {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.setFilmstripUiVisibility(true);
            }
        }

        @Override
        public void onLeaveFilmstrip(int dataId) {
        }

        @Override
        public void onDataReloaded() {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.updateUiByData(CameraActivity.this.mFilmstripController.getCurrentId());
            }
        }

        @Override
        public void onDataUpdated(int dataId) {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.updateUiByData(CameraActivity.this.mFilmstripController.getCurrentId());
            }
        }

        @Override
        public void onEnterZoomView(int dataID) {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.setFilmstripUiVisibility(false);
            }
        }

        @Override
        public void onZoomAtIndexChanged(int dataId, float zoom) {
            LocalData localData = CameraActivity.this.mDataAdapter.getLocalData(dataId);
            long ageMillis = System.currentTimeMillis() - (localData.getDateModified() * 1000);
            if (!TextUtils.isEmpty(localData.getPath()) && ageMillis <= 0) {
                File localFile = new File(localData.getPath());
                UsageStatistics.instance().mediaView(localFile.getName(), TimeUnit.SECONDS.toMillis(localData.getDateModified()), zoom);
            }
        }

        @Override
        public void onDataFocusChanged(int prevDataId, final int newDataId) {
            if (CameraActivity.this.mFilmstripVisible) {
                CameraActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CameraActivity.this.updateUiByData(newDataId);
                    }
                });
            }
        }

        @Override
        public void onScroll(int firstVisiblePosition, int visibleItemCount, int totalItemCount) {
            CameraActivity.this.mPreloader.onScroll(null, firstVisiblePosition, visibleItemCount, totalItemCount);
        }
    };
    private final LocalDataAdapter.LocalDataListener mLocalDataListener = new LocalDataAdapter.LocalDataListener() {
        @Override
        public void onMetadataUpdated(List<Integer> updatedData) {
            if (!CameraActivity.this.mPaused) {
                int currentDataId = CameraActivity.this.mFilmstripController.getCurrentId();
                for (Integer dataId : updatedData) {
                    if (dataId.intValue() == currentDataId) {
                        CameraActivity.this.updateBottomControlsByData(CameraActivity.this.mDataAdapter.getLocalData(dataId.intValue()));
                        return;
                    }
                }
            }
        }
    };
    private final CaptureSessionManager.SessionListener mSessionListener = new CaptureSessionManager.SessionListener() {
        @Override
        public void onSessionQueued(Uri uri) {
            Log.v(CameraActivity.TAG, "onSessionQueued: " + uri);
            if (Storage.isSessionUri(uri)) {
                LocalSessionData newData = new LocalSessionData(uri);
                CameraActivity.this.mDataAdapter.addData(newData);
            }
        }

        @Override
        public void onSessionDone(Uri sessionUri) {
            Log.v(CameraActivity.TAG, "onSessionDone:" + sessionUri);
            Uri contentUri = Storage.getContentUriForSessionUri(sessionUri);
            if (contentUri == null) {
                CameraActivity.this.mDataAdapter.refresh(sessionUri);
                return;
            }
            LocalData newData = LocalMediaData.PhotoData.fromContentUri(CameraActivity.this.getContentResolver(), contentUri);
            if (newData == null) {
                Log.i(CameraActivity.TAG, "onSessionDone: Could not find LocalData for URI: " + contentUri);
                return;
            }
            int pos = CameraActivity.this.mDataAdapter.findDataByContentUri(sessionUri);
            if (pos == -1) {
                CameraActivity.this.mDataAdapter.addData(newData);
            } else {
                CameraActivity.this.mDataAdapter.updateData(pos, newData);
            }
        }

        @Override
        public void onSessionProgress(Uri uri, int progress) {
            int currentDataId;
            if (progress >= 0 && (currentDataId = CameraActivity.this.mFilmstripController.getCurrentId()) != -1 && uri.equals(CameraActivity.this.mDataAdapter.getLocalData(currentDataId).getUri())) {
                CameraActivity.this.updateSessionProgress(progress);
            }
        }

        @Override
        public void onSessionProgressText(Uri uri, CharSequence message) {
            int currentDataId = CameraActivity.this.mFilmstripController.getCurrentId();
            if (currentDataId != -1 && uri.equals(CameraActivity.this.mDataAdapter.getLocalData(currentDataId).getUri())) {
                CameraActivity.this.updateSessionProgressText(message);
            }
        }

        @Override
        public void onSessionUpdated(Uri uri) {
            Log.v(CameraActivity.TAG, "onSessionUpdated: " + uri);
            CameraActivity.this.mDataAdapter.refresh(uri);
        }

        @Override
        public void onSessionPreviewAvailable(Uri uri) {
            Log.v(CameraActivity.TAG, "onSessionPreviewAvailable: " + uri);
            CameraActivity.this.mDataAdapter.refresh(uri);
            int dataId = CameraActivity.this.mDataAdapter.findDataByContentUri(uri);
            if (dataId != -1) {
                CameraActivity.this.startPeekAnimation(CameraActivity.this.mDataAdapter.getLocalData(dataId), CameraActivity.this.mCurrentModule.getPeekAccessibilityString());
            }
        }

        @Override
        public void onSessionFailed(Uri uri, CharSequence reason) {
            Log.v(CameraActivity.TAG, "onSessionFailed:" + uri);
            int failedDataId = CameraActivity.this.mDataAdapter.findDataByContentUri(uri);
            int currentDataId = CameraActivity.this.mFilmstripController.getCurrentId();
            if (currentDataId == failedDataId) {
                CameraActivity.this.updateSessionProgress(0);
                CameraActivity.this.showProcessError(reason);
            }
            CameraActivity.this.mDataAdapter.refresh(uri);
        }
    };
    private final CameraExceptionHandler.CameraExceptionCallback mCameraExceptionCallback = new CameraExceptionHandler.CameraExceptionCallback() {
        @Override
        public void onCameraError(int errorCode) {
            Log.e(CameraActivity.TAG, "Camera error callback. error=" + errorCode);
        }

        @Override
        public void onCameraException(RuntimeException ex, String commandHistory, int action, int state) {
            Log.e(CameraActivity.TAG, "Camera Exception", ex);
            UsageStatistics.instance().cameraFailure(10000, commandHistory, action, state);
            onFatalError();
        }

        @Override
        public void onDispatchThreadException(RuntimeException ex) {
            Log.e(CameraActivity.TAG, "DispatchThread Exception", ex);
            UsageStatistics.instance().cameraFailure(10000, null, -1, -1);
            onFatalError();
        }

        private void onFatalError() {
            if (!CameraActivity.this.mCameraFatalError) {
                CameraActivity.this.mCameraFatalError = true;
                if (CameraActivity.this.mPaused && !CameraActivity.this.isFinishing()) {
                    Log.e(CameraActivity.TAG, "Fatal error during onPause, call Activity.finish()");
                    CameraActivity.this.finish();
                } else {
                    CameraUtil.showErrorAndFinish(CameraActivity.this, R.string.cannot_connect_camera);
                }
            }
        }
    };

    protected interface OnStorageUpdateDoneListener {
        void onStorageUpdateDone(long j);
    }

    @Override
    public CameraAppUI getCameraAppUI() {
        return this.mCameraAppUI;
    }

    @Override
    public ModuleManager getModuleManager() {
        return this.mModuleManager;
    }

    @Override
    public void onCameraOpened(CameraAgent.CameraProxy camera) {
        Log.v(TAG, "onCameraOpened");
        if (this.mPaused) {
            Log.v(TAG, "received onCameraOpened but activity is paused, closing Camera");
            this.mCameraController.closeCamera(false);
            return;
        }
        if (!this.mSettingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA)) {
            HardwareSpec hardware = new HardwareSpecImpl(getCameraProvider(), camera.getCapabilities());
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA, hardware.isFlashSupported());
        }
        if (!this.mModuleManager.getModuleAgent(this.mCurrentModeIndex).requestAppForCamera()) {
            this.mCameraController.closeCamera(false);
            throw new IllegalStateException("Camera opened but the module shouldn't be requesting");
        }
        if (this.mCurrentModule != null) {
            resetExposureCompensationToDefault(camera);
            this.mCurrentModule.onCameraAvailable(camera);
        } else {
            Log.v(TAG, "mCurrentModule null, not invoking onCameraAvailable");
        }
        Log.v(TAG, "invoking onChangeCamera");
        this.mCameraAppUI.onChangeCamera();
    }

    private void resetExposureCompensationToDefault(CameraAgent.CameraProxy camera) {
        CameraSettings cameraSettings = camera.getSettings();
        cameraSettings.setExposureCompensationIndex(0);
        camera.applySettings(cameraSettings);
    }

    @Override
    public void onCameraDisabled(int cameraId) {
        UsageStatistics.instance().cameraFailure(10000, null, -1, -1);
        Log.w(TAG, "Camera disabled: " + cameraId);
        CameraUtil.showErrorAndFinish(this, R.string.camera_disabled);
    }

    @Override
    public void onDeviceOpenFailure(int cameraId, String info) {
        UsageStatistics.instance().cameraFailure(10000, info, -1, -1);
        Log.w(TAG, "Camera open failure: " + info);
        CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
    }

    @Override
    public void onDeviceOpenedAlready(int cameraId, String info) {
        Log.w(TAG, "Camera open already: " + cameraId + Size.DELIMITER + info);
        CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
    }

    @Override
    public void onReconnectionFailure(CameraAgent mgr, String info) {
        UsageStatistics.instance().cameraFailure(10000, null, -1, -1);
        Log.w(TAG, "Camera reconnection failure:" + info);
        CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
    }

    private static class MainHandler extends Handler {
        final WeakReference<CameraActivity> mActivity;

        public MainHandler(CameraActivity activity, Looper looper) {
            super(looper);
            this.mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraActivity activity = this.mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case 2:
                        if (!activity.mPaused) {
                            activity.getWindow().clearFlags(128);
                        }
                        break;
                }
            }
        }
    }

    private String fileNameFromDataID(int dataID) {
        LocalData localData = this.mDataAdapter.getLocalData(dataID);
        if (localData == null) {
            return "";
        }
        File localFile = new File(localData.getPath());
        return localFile.getName();
    }

    private float fileAgeFromDataID(int dataID) {
        LocalData localData = this.mDataAdapter.getLocalData(dataID);
        if (localData == null) {
            return 0.0f;
        }
        File localFile = new File(localData.getPath());
        return 0.001f * (System.currentTimeMillis() - localFile.lastModified());
    }

    public void gotoGallery() {
        UsageStatistics.instance().changeScreen(10000, 10000);
        this.mFilmstripController.goToNextItem();
    }

    private void setFilmstripUiVisibility(boolean visible) {
        this.mLightsOutRunnable.run();
        this.mCameraAppUI.getFilmstripBottomControls().setVisible(visible);
        if (visible != this.mActionBar.isShowing()) {
            if (visible) {
                this.mActionBar.show();
                this.mCameraAppUI.showBottomControls();
            } else {
                this.mActionBar.hide();
                this.mCameraAppUI.hideBottomControls();
            }
        }
        this.mFilmstripCoversPreview = visible;
        updatePreviewVisibility();
    }

    private void hideSessionProgress() {
        this.mCameraAppUI.getFilmstripBottomControls().hideProgress();
    }

    private void showSessionProgress(CharSequence message) {
        CameraAppUI.BottomPanel controls = this.mCameraAppUI.getFilmstripBottomControls();
        controls.setProgressText(message);
        controls.hideControls();
        controls.hideProgressError();
        controls.showProgress();
    }

    private void showProcessError(CharSequence message) {
        this.mCameraAppUI.getFilmstripBottomControls().showProgressError(message);
    }

    private void updateSessionProgress(int progress) {
        this.mCameraAppUI.getFilmstripBottomControls().setProgress(progress);
    }

    private void updateSessionProgressText(CharSequence message) {
        this.mCameraAppUI.getFilmstripBottomControls().setProgressText(message);
    }

    @TargetApi(16)
    private void setupNfcBeamPush() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this.mAppContext);
        if (adapter != null) {
            if (!ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
                adapter.setNdefPushMessage(null, this, new Activity[0]);
            } else {
                adapter.setBeamPushUris(null, this);
                adapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                    @Override
                    public Uri[] createBeamUris(NfcEvent event) {
                        return CameraActivity.this.mNfcPushUris;
                    }
                }, this);
            }
        }
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        int currentDataId = this.mFilmstripController.getCurrentId();
        if (currentDataId < 0) {
            return false;
        }
        UsageStatistics.instance().mediaInteraction(fileNameFromDataID(currentDataId), 10000, 10000, fileAgeFromDataID(currentDataId));
        return true;
    }

    @Override
    public Context getAndroidContext() {
        return this.mAppContext;
    }

    @Override
    public void launchActivityByIntent(Intent intent) {
        this.mResetToPreviewOnResume = false;
        intent.addFlags(AccessibilityEventCompat.TYPE_GESTURE_DETECTION_END);
        startActivity(intent);
    }

    @Override
    public int getCurrentModuleIndex() {
        return this.mCurrentModeIndex;
    }

    @Override
    public int getCurrentCameraId() {
        return this.mCameraController.getCurrentCameraId();
    }

    @Override
    public String getModuleScope() {
        return MODULE_SCOPE_PREFIX + this.mCurrentModule.getModuleStringIdentifier();
    }

    @Override
    public String getCameraScope() {
        int currentCameraId = getCurrentCameraId();
        if (currentCameraId < 0) {
            Log.w(TAG, "getting camera scope with no open camera, using id: " + currentCameraId);
        }
        return CAMERA_SCOPE_PREFIX + Integer.toString(currentCameraId);
    }

    @Override
    public ModuleController getCurrentModuleController() {
        return this.mCurrentModule;
    }

    @Override
    public int getQuickSwitchToModuleId(int currentModuleIndex) {
        return this.mModuleManager.getQuickSwitchToModuleId(currentModuleIndex, this.mSettingsManager, this.mAppContext);
    }

    @Override
    public SurfaceTexture getPreviewBuffer() {
        return null;
    }

    @Override
    public void onPreviewReadyToStart() {
        this.mCameraAppUI.onPreviewReadyToStart();
    }

    @Override
    public void onPreviewStarted() {
        this.mCameraAppUI.onPreviewStarted();
    }

    @Override
    public void addPreviewAreaSizeChangedListener(PreviewStatusListener.PreviewAreaChangedListener listener) {
        this.mCameraAppUI.addPreviewAreaChangedListener(listener);
    }

    @Override
    public void removePreviewAreaSizeChangedListener(PreviewStatusListener.PreviewAreaChangedListener listener) {
        this.mCameraAppUI.removePreviewAreaChangedListener(listener);
    }

    @Override
    public void setupOneShotPreviewListener() {
        this.mCameraController.setOneShotPreviewCallback(this.mMainHandler, new CameraAgent.CameraPreviewDataCallback() {
            @Override
            public void onPreviewFrame(byte[] data, CameraAgent.CameraProxy camera) {
                CameraActivity.this.mCurrentModule.onPreviewInitialDataReceived();
                CameraActivity.this.mCameraAppUI.onNewPreviewFrame();
            }
        });
    }

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mCameraAppUI.updatePreviewAspectRatio(aspectRatio);
    }

    @Override
    public void updatePreviewTransformFullscreen(Matrix matrix, float aspectRatio) {
        this.mCameraAppUI.updatePreviewTransformFullscreen(matrix, aspectRatio);
    }

    @Override
    public RectF getFullscreenRect() {
        return this.mCameraAppUI.getFullscreenRect();
    }

    @Override
    public void updatePreviewTransform(Matrix matrix) {
        this.mCameraAppUI.updatePreviewTransform(matrix);
    }

    @Override
    public void setPreviewStatusListener(PreviewStatusListener previewStatusListener) {
        this.mCameraAppUI.setPreviewStatusListener(previewStatusListener);
    }

    @Override
    public FrameLayout getModuleLayoutRoot() {
        return this.mCameraAppUI.getModuleRootView();
    }

    @Override
    public void setShutterEventsListener(AppController.ShutterEventsListener listener) {
    }

    @Override
    public void setShutterEnabled(boolean enabled) {
        this.mCameraAppUI.setShutterButtonEnabled(enabled);
    }

    @Override
    public boolean isShutterEnabled() {
        return this.mCameraAppUI.isShutterButtonEnabled();
    }

    @Override
    public void startPreCaptureAnimation(boolean shortFlash) {
        this.mCameraAppUI.startPreCaptureAnimation(shortFlash);
    }

    @Override
    public void startPreCaptureAnimation() {
        this.mCameraAppUI.startPreCaptureAnimation(false);
    }

    @Override
    public void cancelPreCaptureAnimation() {
    }

    @Override
    public void startPostCaptureAnimation() {
    }

    @Override
    public void startPostCaptureAnimation(Bitmap thumbnail) {
    }

    @Override
    public void cancelPostCaptureAnimation() {
    }

    @Override
    public OrientationManager getOrientationManager() {
        return this.mOrientationManager;
    }

    @Override
    public LocationManager getLocationManager() {
        return this.mLocationManager;
    }

    @Override
    public void lockOrientation() {
        if (this.mOrientationManager != null) {
            this.mOrientationManager.lockOrientation();
        }
    }

    @Override
    public void unlockOrientation() {
        if (this.mOrientationManager != null) {
            this.mOrientationManager.unlockOrientation();
        }
    }

    private void startPeekAnimation(LocalData data, final String accessibilityString) {
        if (!this.mFilmstripVisible && this.mPeekAnimationHandler != null) {
            int dataType = data.getLocalDataType();
            if (dataType == 3 || dataType == 5 || dataType == 4) {
                this.mPeekAnimationHandler.startDecodingJob(data, new Callback<Bitmap>() {
                    @Override
                    public void onCallback(Bitmap result) {
                        CameraActivity.this.mCameraAppUI.startPeekAnimation(result, true, accessibilityString);
                    }
                });
            }
        }
    }

    @Override
    public void notifyNewMedia(Uri uri) {
        LocalData newData;
        updateStorageSpaceAndHint(null);
        ContentResolver cr = getContentResolver();
        String mimeType = cr.getType(uri);
        if (LocalDataUtil.isMimeTypeVideo(mimeType)) {
            sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri));
            newData = LocalMediaData.VideoData.fromContentUri(getContentResolver(), uri);
            if (newData == null) {
                Log.e(TAG, "Can't find video data in content resolver:" + uri);
                return;
            }
        } else if (LocalDataUtil.isMimeTypeImage(mimeType)) {
            CameraUtil.broadcastNewPicture(this.mAppContext, uri);
            newData = LocalMediaData.PhotoData.fromContentUri(getContentResolver(), uri);
            if (newData == null) {
                Log.e(TAG, "Can't find photo data in content resolver:" + uri);
                return;
            }
        } else {
            Log.w(TAG, "Unknown new media with MIME type:" + mimeType + ", uri:" + uri);
            return;
        }
        new AsyncTask<LocalData, Void, LocalData>() {
            @Override
            protected LocalData doInBackground(LocalData... params) {
                LocalData data = params[0];
                MetadataLoader.loadMetadata(CameraActivity.this.getAndroidContext(), data);
                return data;
            }

            @Override
            protected void onPostExecute(LocalData data) {
                CameraActivity.this.mDataAdapter.addData(data);
                CameraActivity.this.startPeekAnimation(data, CameraActivity.this.mCurrentModule.getPeekAccessibilityString());
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, newData);
    }

    @Override
    public void enableKeepScreenOn(boolean enabled) {
        if (!this.mPaused) {
            this.mKeepScreenOn = enabled;
            if (this.mKeepScreenOn) {
                this.mMainHandler.removeMessages(2);
                getWindow().addFlags(128);
            } else {
                keepScreenOnForAWhile();
            }
        }
    }

    @Override
    public CameraProvider getCameraProvider() {
        return this.mCameraController;
    }

    @Override
    public OneCameraManager getCameraManager() {
        return this.mCameraManager;
    }

    private void removeData(int dataID) {
        this.mDataAdapter.removeData(dataID);
        if (this.mDataAdapter.getTotalNumber() > 0) {
            showUndoDeletionBar();
            return;
        }
        this.mPendingDeletion = true;
        performDeletion();
        if (this.mFilmstripVisible) {
            this.mCameraAppUI.getFilmstripContentPanel().animateHide();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_details:
                showDetailsDialog(this.mFilmstripController.getCurrentId());
                return true;
            case R.id.action_help_and_feedback:
                this.mResetToPreviewOnResume = false;
                GoogleHelpHelper.launchGoogleHelp(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isCaptureIntent() {
        return "android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction()) || "android.media.action.IMAGE_CAPTURE".equals(getIntent().getAction()) || ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction());
    }

    @Override
    public void onNewIntentTasks(Intent intent) {
        onModeSelected(getModeIndex());
    }

    @Override
    public void onCreateTasks(Bundle state) {
        CameraPerformanceTracker.onEvent(0);
        this.mAppContext = getApplication().getBaseContext();
        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(getAndroidContext()).setResizeService(new FifoPriorityThreadPoolExecutor(2)));
            Glide.get(getAndroidContext()).setMemoryCategory(MemoryCategory.HIGH);
        }
        this.mOnCreateTime = System.currentTimeMillis();
        this.mSoundPlayer = new SoundPlayer(this.mAppContext);
        try {
            this.mCameraManager = OneCameraManager.get(this);
            this.mModuleManager = new ModuleManagerImpl();
            GcamHelper.init(getContentResolver());
            ModulesInfo.setupModules(this.mAppContext, this.mModuleManager);
            this.mSettingsManager = getServices().getSettingsManager();
            AppUpgrader appUpgrader = new AppUpgrader(this);
            appUpgrader.upgrade(this.mSettingsManager);
            Keys.setDefaults(this.mSettingsManager, this.mAppContext);
            getWindow().requestFeature(8);
            setContentView(R.layout.activity_main);
            this.mActionBar = getActionBar();
            if (ApiHelper.isLOrHigher()) {
                this.mActionBar.setBackgroundDrawable(new ColorDrawable(0));
            } else {
                this.mActionBar.setBackgroundDrawable(new ColorDrawable(Integer.MIN_VALUE));
            }
            this.mMainHandler = new MainHandler(this, getMainLooper());
            this.mCameraController = new CameraController(this.mAppContext, this, this.mMainHandler, CameraAgentFactory.getAndroidCameraAgent(this.mAppContext, CameraAgentFactory.CameraApi.API_1), CameraAgentFactory.getAndroidCameraAgent(this.mAppContext, CameraAgentFactory.CameraApi.AUTO));
            this.mCameraController.setCameraExceptionHandler(new CameraExceptionHandler(this.mCameraExceptionCallback, this.mMainHandler));
            this.mModeListView = (ModeListView) findViewById(R.id.mode_list_layout);
            this.mModeListView.init(this.mModuleManager.getSupportedModeIndexList());
            if (ApiHelper.HAS_ROTATION_ANIMATION) {
                setRotationAnimation();
            }
            this.mModeListView.setVisibilityChangedListener(new ModeListView.ModeListVisibilityChangedListener() {
                @Override
                public void onVisibilityChanged(boolean visible) {
                    CameraActivity.this.mModeListVisible = visible;
                    CameraActivity.this.mCameraAppUI.setShutterButtonImportantToA11y(!visible);
                    CameraActivity.this.updatePreviewVisibility();
                }
            });
            Intent intent = getIntent();
            String action = intent.getAction();
            if (INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action) || ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
                this.mSecureCamera = true;
            } else {
                this.mSecureCamera = intent.getBooleanExtra(SECURE_CAMERA_EXTRA, false);
            }
            if (this.mSecureCamera) {
                Window win = getWindow();
                WindowManager.LayoutParams params = win.getAttributes();
                params.flags |= AccessibilityEventCompat.TYPE_GESTURE_DETECTION_END;
                win.setAttributes(params);
                IntentFilter filter_screen_off = new IntentFilter("android.intent.action.SCREEN_OFF");
                registerReceiver(this.mShutdownReceiver, filter_screen_off);
                IntentFilter filter_user_unlock = new IntentFilter("android.intent.action.USER_PRESENT");
                registerReceiver(this.mShutdownReceiver, filter_user_unlock);
            }
            this.mCameraAppUI = new CameraAppUI(this, (MainActivityLayout) findViewById(R.id.activity_root_view), isCaptureIntent());
            this.mCameraAppUI.setFilmstripBottomControlsListener(this.mMyFilmstripBottomControlListener);
            this.mAboveFilmstripControlLayout = (FrameLayout) findViewById(R.id.camera_filmstrip_content_layout);
            getServices().getCaptureSessionManager().addSessionListener(this.mSessionListener);
            this.mFilmstripController = ((FilmstripView) findViewById(R.id.filmstrip_view)).getController();
            this.mFilmstripController.setImageGap(getResources().getDimensionPixelSize(R.dimen.camera_film_strip_gap));
            this.mPanoramaViewHelper = new PhotoSphereHelper.PanoramaViewHelper(this);
            this.mPanoramaViewHelper.onCreate();
            this.mDataAdapter = new CameraDataAdapter(this.mAppContext, R.color.photo_placeholder);
            this.mDataAdapter.setLocalDataListener(this.mLocalDataListener);
            this.mPreloader = new Preloader<>(10, this.mDataAdapter, this.mDataAdapter);
            this.mCameraAppUI.getFilmstripContentPanel().setFilmstripListener(this.mFilmstripListener);
            if (this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING)) {
                this.mCameraAppUI.setupClingForViewer(2);
            }
            this.mLocationManager = new LocationManager(this.mAppContext);
            this.mOrientationManager = new OrientationManagerImpl(this);
            this.mOrientationManager.addOnOrientationChangeListener(this.mMainHandler, this);
            setModuleFromModeIndex(getModeIndex());
            this.mCameraAppUI.prepareModuleUI();
            this.mCurrentModule.init(this, isSecureCamera(), isCaptureIntent());
            if (!this.mSecureCamera) {
                this.mFilmstripController.setDataAdapter(this.mDataAdapter);
                if (!isCaptureIntent()) {
                    this.mDataAdapter.requestLoad(new Callback<Void>() {
                        @Override
                        public void onCallback(Void result) {
                            CameraActivity.this.fillTemporarySessions();
                        }
                    });
                }
            } else {
                ImageView v = (ImageView) getLayoutInflater().inflate(R.layout.secure_album_placeholder, (ViewGroup) null);
                v.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(LocalDataViewType.SECURE_ALBUM_PLACEHOLDER.ordinal()));
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        UsageStatistics.instance().changeScreen(10000, 10000);
                        CameraActivity.this.startGallery();
                        CameraActivity.this.finish();
                    }
                });
                v.setContentDescription(getString(R.string.accessibility_unlock_to_camera));
                this.mDataAdapter = new FixedLastDataAdapter(this.mAppContext, this.mDataAdapter, new SimpleViewData(v, LocalDataViewType.SECURE_ALBUM_PLACEHOLDER, v.getDrawable().getIntrinsicWidth(), v.getDrawable().getIntrinsicHeight(), 0, 0));
                this.mDataAdapter.flush();
                this.mFilmstripController.setDataAdapter(this.mDataAdapter);
            }
            setupNfcBeamPush();
            this.mLocalImagesObserver = new LocalMediaObserver();
            this.mLocalVideosObserver = new LocalMediaObserver();
            getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, this.mLocalImagesObserver);
            getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, this.mLocalVideosObserver);
            this.mMemoryManager = getServices().getMemoryManager();
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    HashMap memoryData = CameraActivity.this.mMemoryManager.queryMemory();
                    UsageStatistics.instance().reportMemoryConsumed(memoryData, MemoryQuery.REPORT_LABEL_LAUNCH);
                }
            });
            this.mMotionManager = getServices().getMotionManager();
        } catch (OneCameraException e) {
            Log.d(TAG, "Creating camera manager failed.", e);
            CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
        }
    }

    public int getModeIndex() {
        int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
        int videoIndex = getResources().getInteger(R.integer.camera_mode_video);
        int gcamIndex = getResources().getInteger(R.integer.camera_mode_gcam);
        if ("android.media.action.VIDEO_CAMERA".equals(getIntent().getAction()) || "android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction())) {
            return videoIndex;
        }
        if ("android.media.action.IMAGE_CAPTURE".equals(getIntent().getAction())) {
            return photoIndex;
        }
        if ("android.media.action.STILL_IMAGE_CAMERA".equals(getIntent().getAction()) || INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(getIntent().getAction()) || ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            int modeIndex = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue();
            if (!this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
                return photoIndex;
            }
            return modeIndex;
        }
        int modeIndex2 = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX).intValue();
        if ((modeIndex2 == gcamIndex && !GcamHelper.hasGcamAsSeparateModule()) || modeIndex2 < 0) {
            return photoIndex;
        }
        return modeIndex2;
    }

    private void updatePreviewVisibility() {
        if (this.mCurrentModule != null) {
            int visibility = getPreviewVisibility();
            this.mCameraAppUI.onPreviewVisiblityChanged(visibility);
            updatePreviewRendering(visibility);
            this.mCurrentModule.onPreviewVisibilityChanged(visibility);
        }
    }

    private void updatePreviewRendering(int visibility) {
        if (visibility == 2) {
            this.mCameraAppUI.pausePreviewRendering();
        } else {
            this.mCameraAppUI.resumePreviewRendering();
        }
    }

    private int getPreviewVisibility() {
        if (this.mFilmstripCoversPreview) {
            return 2;
        }
        if (this.mModeListVisible) {
            return 1;
        }
        return 0;
    }

    private void setRotationAnimation() {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = 1;
        win.setAttributes(winParams);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!isFinishing()) {
            keepScreenOnForAWhile();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);
        if (ev.getActionMasked() == 0 && this.mPendingDeletion && !this.mIsUndoingDeletion) {
            performDeletion();
        }
        return result;
    }

    @Override
    public void onPauseTasks() {
        CameraPerformanceTracker.onEvent(1);
        if (!isCaptureIntent()) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, this.mCurrentModeIndex);
        }
        this.mPaused = true;
        this.mPeekAnimationHandler = null;
        this.mPeekAnimationThread.quitSafely();
        this.mPeekAnimationThread = null;
        performDeletion();
        this.mCurrentModule.pause();
        this.mOrientationManager.pause();
        this.mPanoramaViewHelper.onPause();
        this.mLocalImagesObserver.setForegroundChangeListener(null);
        this.mLocalImagesObserver.setActivityPaused(true);
        this.mLocalVideosObserver.setActivityPaused(true);
        this.mPreloader.cancelAllLoads();
        resetScreenOn();
        this.mMotionManager.stop();
        UsageStatistics.instance().backgrounded();
        if (this.mCameraFatalError && !isFinishing()) {
            Log.v(TAG, "onPause when camera is in fatal state, call Activity.finish()");
            finish();
        } else {
            this.mCameraController.closeCamera(true);
        }
    }

    @Override
    public void onResumeTasks() {
        int source;
        LocalData data;
        CameraPerformanceTracker.onEvent(2);
        Log.v(TAG, "Build info: " + Build.DISPLAY);
        this.mPaused = false;
        updateStorageSpaceAndHint(null);
        this.mLastLayoutOrientation = getResources().getConfiguration().orientation;
        if (Settings.System.getInt(getContentResolver(), "accelerometer_rotation", 0) == 0) {
            setRequestedOrientation(-1);
            this.mAutoRotateScreen = false;
        } else {
            setRequestedOrientation(10);
            this.mAutoRotateScreen = true;
        }
        String action = getIntent().getAction();
        if (action == null) {
            source = 10000;
        } else {
            switch (action) {
                case "android.media.action.IMAGE_CAPTURE":
                    source = 10000;
                    break;
                case "android.media.action.STILL_IMAGE_CAMERA":
                    source = 10000;
                    break;
                case "android.media.action.VIDEO_CAMERA":
                    source = 10000;
                    break;
                case "android.media.action.VIDEO_CAPTURE":
                    source = 10000;
                    break;
                case "android.media.action.STILL_IMAGE_CAMERA_SECURE":
                    source = 10000;
                    break;
                case "android.media.action.IMAGE_CAPTURE_SECURE":
                    source = 10000;
                    break;
                case "android.intent.action.MAIN":
                    source = 10000;
                    break;
                default:
                    source = 10000;
                    break;
            }
        }
        UsageStatistics.instance().foregrounded(source, currentUserInterfaceMode());
        this.mGalleryIntent = IntentHelper.getGalleryIntent(this.mAppContext);
        if (ApiHelper.isLOrHigher()) {
            this.mActionBar.setDisplayShowHomeEnabled(false);
        }
        this.mOrientationManager.resume();
        this.mPeekAnimationThread = new HandlerThread("Peek animation");
        this.mPeekAnimationThread.start();
        this.mPeekAnimationHandler = new PeekAnimationHandler(this.mPeekAnimationThread.getLooper(), this.mMainHandler, this.mAboveFilmstripControlLayout);
        this.mCurrentModule.hardResetSettings(this.mSettingsManager);
        this.mCurrentModule.resume();
        UsageStatistics.instance().changeScreen(currentUserInterfaceMode(), 10000);
        setSwipingEnabled(true);
        if (!this.mResetToPreviewOnResume && (data = this.mDataAdapter.getLocalData(this.mFilmstripController.getCurrentId())) != null) {
            this.mDataAdapter.refresh(data.getUri());
        }
        this.mCameraAppUI.getFilmstripBottomControls().setShareEnabled(true);
        this.mResetToPreviewOnResume = true;
        if ((this.mLocalVideosObserver.isMediaDataChangedDuringPause() || this.mLocalImagesObserver.isMediaDataChangedDuringPause()) && !this.mSecureCamera) {
            if (!this.mFilmstripVisible) {
                this.mDataAdapter.requestLoad(new Callback<Void>() {
                    @Override
                    public void onCallback(Void result) {
                        CameraActivity.this.fillTemporarySessions();
                    }
                });
            } else {
                this.mDataAdapter.requestLoadNewPhotos();
            }
        }
        this.mLocalImagesObserver.setActivityPaused(false);
        this.mLocalVideosObserver.setActivityPaused(false);
        if (!this.mSecureCamera) {
            this.mLocalImagesObserver.setForegroundChangeListener(new LocalMediaObserver.ChangeListener() {
                @Override
                public void onChange() {
                    CameraActivity.this.mDataAdapter.requestLoadNewPhotos();
                }
            });
        }
        keepScreenOnForAWhile();
        findViewById(R.id.activity_root_view);
        this.mLightsOutRunnable.run();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                CameraActivity.this.mMainHandler.removeCallbacks(CameraActivity.this.mLightsOutRunnable);
                CameraActivity.this.mMainHandler.postDelayed(CameraActivity.this.mLightsOutRunnable, 4000L);
            }
        });
        this.mPanoramaViewHelper.onResume();
        ReleaseHelper.showReleaseInfoDialogOnStart(this, this.mSettingsManager);
        syncLocationManagerSetting();
        int previewVisibility = getPreviewVisibility();
        updatePreviewRendering(previewVisibility);
        this.mMotionManager.start();
    }

    private void fillTemporarySessions() {
        if (!this.mSecureCamera) {
            getServices().getCaptureSessionManager().fillTemporarySession(this.mSessionListener);
        }
    }

    @Override
    public void onStartTasks() {
        this.mIsActivityRunning = true;
        this.mPanoramaViewHelper.onStart();
        int modeIndex = getModeIndex();
        if (!isCaptureIntent() && this.mCurrentModeIndex != modeIndex) {
            onModeSelected(modeIndex);
        }
        if (this.mResetToPreviewOnResume) {
            this.mCameraAppUI.resume();
            this.mResetToPreviewOnResume = false;
        }
    }

    @Override
    protected void onStopTasks() {
        this.mIsActivityRunning = false;
        this.mPanoramaViewHelper.onStop();
        this.mLocationManager.disconnect();
    }

    @Override
    public void onDestroyTasks() {
        if (this.mSecureCamera) {
            unregisterReceiver(this.mShutdownReceiver);
        }
        this.mSettingsManager.removeAllListeners();
        this.mCameraController.removeCallbackReceiver();
        this.mCameraController.setCameraExceptionHandler(null);
        getContentResolver().unregisterContentObserver(this.mLocalImagesObserver);
        getContentResolver().unregisterContentObserver(this.mLocalVideosObserver);
        getServices().getCaptureSessionManager().removeSessionListener(this.mSessionListener);
        this.mCameraAppUI.onDestroy();
        this.mModeListView.setVisibilityChangedListener(null);
        this.mCameraController = null;
        this.mSettingsManager = null;
        this.mOrientationManager = null;
        this.mButtonManager = null;
        this.mSoundPlayer.release();
        CameraAgentFactory.recycle(CameraAgentFactory.CameraApi.API_1);
        CameraAgentFactory.recycle(CameraAgentFactory.CameraApi.AUTO);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Log.v(TAG, "onConfigurationChanged");
        if (config.orientation != 0 && this.mLastLayoutOrientation != config.orientation) {
            this.mLastLayoutOrientation = config.orientation;
            this.mCurrentModule.onLayoutOrientationChanged(this.mLastLayoutOrientation == 2);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!this.mFilmstripVisible) {
            if (this.mCurrentModule.onKeyDown(keyCode, event)) {
                return true;
            }
            if ((keyCode == 84 || keyCode == 82) && event.isLongPress()) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!this.mFilmstripVisible) {
            if (this.mCurrentModule.onKeyUp(keyCode, event)) {
                return true;
            }
            if (keyCode == 82 || keyCode == 21) {
                this.mCameraAppUI.openModeList();
                return true;
            }
            if (keyCode == 22) {
                this.mCameraAppUI.showFilmstrip();
                return true;
            }
        } else {
            if (keyCode == 22) {
                this.mFilmstripController.goToNextItem();
                return true;
            }
            if (keyCode == 21) {
                boolean wentToPrevious = this.mFilmstripController.goToPreviousItem();
                if (wentToPrevious) {
                    return true;
                }
                this.mCameraAppUI.hideFilmstrip();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (!this.mCameraAppUI.onBackPressed() && !this.mCurrentModule.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean isAutoRotateScreen() {
        return this.mAutoRotateScreen;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        CharSequence appName;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filmstrip_menu, menu);
        this.mActionBarMenu = menu;
        menu.removeItem(R.id.action_help_and_feedback);
        if (this.mGalleryIntent != null && (appName = IntentHelper.getGalleryAppName(this.mAppContext, this.mGalleryIntent)) != null) {
            MenuItem menuItem = menu.add(appName);
            menuItem.setShowAsAction(2);
            menuItem.setIntent(this.mGalleryIntent);
            Drawable galleryLogo = IntentHelper.getGalleryIcon(this.mAppContext, this.mGalleryIntent);
            if (galleryLogo != null) {
                menuItem.setIcon(galleryLogo);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isSecureCamera() && !ApiHelper.isLOrHigher()) {
            menu.removeItem(R.id.action_help_and_feedback);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    protected long getStorageSpaceBytes() {
        long j;
        synchronized (this.mStorageSpaceLock) {
            j = this.mStorageSpaceBytes;
        }
        return j;
    }

    protected void updateStorageSpaceAndHint(final OnStorageUpdateDoneListener callback) {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... arg) {
                Long lValueOf;
                synchronized (CameraActivity.this.mStorageSpaceLock) {
                    CameraActivity.this.mStorageSpaceBytes = Storage.getAvailableSpace();
                    lValueOf = Long.valueOf(CameraActivity.this.mStorageSpaceBytes);
                }
                return lValueOf;
            }

            @Override
            protected void onPostExecute(Long bytes) {
                CameraActivity.this.updateStorageHint(bytes.longValue());
                if (callback == null || CameraActivity.this.mPaused) {
                    Log.v(CameraActivity.TAG, "ignoring storage callback after activity pause");
                } else {
                    callback.onStorageUpdateDone(bytes.longValue());
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    protected void updateStorageHint(long storageSpace) {
        if (this.mIsActivityRunning) {
            String message = null;
            if (storageSpace == -1) {
                message = getString(R.string.no_storage);
            } else if (storageSpace == -2) {
                message = getString(R.string.preparing_sd);
            } else if (storageSpace == -3) {
                message = getString(R.string.access_sd_fail);
            } else if (storageSpace <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                message = getString(R.string.spaceIsLow_content);
            }
            if (message != null) {
                Log.w(TAG, "Storage warning: " + message);
                if (this.mStorageHint == null) {
                    this.mStorageHint = OnScreenHint.makeText(this, message);
                } else {
                    this.mStorageHint.setText(message);
                }
                this.mStorageHint.show();
                UsageStatistics.instance().storageWarning(storageSpace);
                this.mCameraAppUI.setDisableAllUserInteractions(true);
                return;
            }
            if (this.mStorageHint != null) {
                this.mStorageHint.cancel();
                this.mStorageHint = null;
                this.mCameraAppUI.setDisableAllUserInteractions(false);
            }
        }
    }

    protected void setResultEx(int resultCode) {
        this.mResultCodeForTesting = resultCode;
        setResult(resultCode);
    }

    protected void setResultEx(int resultCode, Intent data) {
        this.mResultCodeForTesting = resultCode;
        this.mResultDataForTesting = data;
        setResult(resultCode, data);
    }

    public int getResultCode() {
        return this.mResultCodeForTesting;
    }

    public Intent getResultData() {
        return this.mResultDataForTesting;
    }

    public boolean isSecureCamera() {
        return this.mSecureCamera;
    }

    @Override
    public boolean isPaused() {
        return this.mPaused;
    }

    @Override
    public int getPreferredChildModeIndex(int modeIndex) {
        if (modeIndex == getResources().getInteger(R.integer.camera_mode_photo)) {
            boolean hdrPlusOn = Keys.isHdrPlusOn(this.mSettingsManager);
            if (hdrPlusOn && GcamHelper.hasGcamAsSeparateModule()) {
                return getResources().getInteger(R.integer.camera_mode_gcam);
            }
            return modeIndex;
        }
        return modeIndex;
    }

    @Override
    public void onModeSelected(int modeIndex) {
        if (this.mCurrentModeIndex != modeIndex) {
            CameraPerformanceTracker.onEvent(3);
            if (modeIndex == getResources().getInteger(R.integer.camera_mode_photo) || modeIndex == getResources().getInteger(R.integer.camera_mode_gcam)) {
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, modeIndex);
            }
            closeModule(this.mCurrentModule);
            int modeIndex2 = getPreferredChildModeIndex(modeIndex);
            setModuleFromModeIndex(modeIndex2);
            this.mCameraAppUI.resetBottomControls(this.mCurrentModule, modeIndex2);
            this.mCameraAppUI.addShutterListener(this.mCurrentModule);
            openModule(this.mCurrentModule);
            this.mCurrentModule.onOrientationChanged(this.mLastRawOrientation);
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, modeIndex2);
        }
    }

    @Override
    public void onSettingsSelected() {
        UsageStatistics.instance().controlUsed(10000);
        Intent intent = new Intent(this, (Class<?>) CameraSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void freezeScreenUntilPreviewReady() {
        this.mCameraAppUI.freezeScreenUntilPreviewReady();
    }

    private void setModuleFromModeIndex(int modeIndex) {
        ModuleManager.ModuleAgent agent = this.mModuleManager.getModuleAgent(modeIndex);
        if (agent != null) {
            if (!agent.requestAppForCamera()) {
                this.mCameraController.closeCamera(true);
            }
            this.mCurrentModeIndex = agent.getModuleId();
            this.mCurrentModule = (CameraModule) agent.createModule(this);
        }
    }

    @Override
    public SettingsManager getSettingsManager() {
        return this.mSettingsManager;
    }

    @Override
    public CameraServices getServices() {
        return (CameraServices) getApplication();
    }

    public List<String> getSupportedModeNames() {
        List<Integer> indices = this.mModuleManager.getSupportedModeIndexList();
        List<String> supported = new ArrayList<>();
        for (Integer modeIndex : indices) {
            String name = CameraUtil.getCameraModeText(modeIndex.intValue(), this.mAppContext);
            if (name != null && !name.equals("")) {
                supported.add(name);
            }
        }
        return supported;
    }

    @Override
    public ButtonManager getButtonManager() {
        if (this.mButtonManager == null) {
            this.mButtonManager = new ButtonManager(this);
        }
        return this.mButtonManager;
    }

    @Override
    public SoundPlayer getSoundPlayer() {
        return this.mSoundPlayer;
    }

    public AlertDialog getFirstTimeLocationAlert() {
        AlertDialog.Builder builder = SettingsUtil.getFirstTimeLocationAlertBuilder(new AlertDialog.Builder(this), new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean locationOn) {
                Keys.setLocation(CameraActivity.this.mSettingsManager, locationOn.booleanValue(), CameraActivity.this.mLocationManager);
            }
        });
        if (builder != null) {
            return builder.create();
        }
        return null;
    }

    public void launchEditor(LocalData data) {
        Intent intent = new Intent("android.intent.action.EDIT").setDataAndType(data.getUri(), data.getMimeType()).setFlags(1);
        try {
            launchActivityByIntent(intent);
        } catch (ActivityNotFoundException e) {
            String msgEditWith = getResources().getString(R.string.edit_with);
            launchActivityByIntent(Intent.createChooser(intent, msgEditWith));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filmstrip_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tiny_planet_editor:
                this.mMyFilmstripBottomControlListener.onTinyPlanet();
                break;
            case R.id.photo_editor:
                this.mMyFilmstripBottomControlListener.onEdit();
                break;
        }
        return true;
    }

    public void launchTinyPlanetEditor(LocalData data) {
        TinyPlanetFragment fragment = new TinyPlanetFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TinyPlanetFragment.ARGUMENT_URI, data.getUri().toString());
        bundle.putString(TinyPlanetFragment.ARGUMENT_TITLE, data.getTitle());
        fragment.setArguments(bundle);
        fragment.show(getFragmentManager(), "tiny_planet");
    }

    private int currentUserInterfaceMode() {
        int mode = 10000;
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_photo)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_video)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_refocus)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_gcam)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_photosphere)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_panorama)) {
            mode = 10000;
        }
        if (this.mFilmstripVisible) {
            return 10000;
        }
        return mode;
    }

    private void openModule(CameraModule module) {
        module.init(this, isSecureCamera(), isCaptureIntent());
        module.hardResetSettings(this.mSettingsManager);
        if (!this.mPaused) {
            module.resume();
            UsageStatistics.instance().changeScreen(currentUserInterfaceMode(), 10000);
            updatePreviewVisibility();
        }
    }

    private void closeModule(CameraModule module) {
        module.pause();
        this.mCameraAppUI.clearModuleUI();
    }

    private void performDeletion() {
        if (this.mPendingDeletion) {
            hideUndoDeletionBar(false);
            this.mDataAdapter.executeDeletion();
        }
    }

    public void showUndoDeletionBar() {
        if (this.mPendingDeletion) {
            performDeletion();
        }
        Log.v(TAG, "showing undo bar");
        this.mPendingDeletion = true;
        if (this.mUndoDeletionBar == null) {
            ViewGroup v = (ViewGroup) getLayoutInflater().inflate(R.layout.undo_bar, (ViewGroup) this.mAboveFilmstripControlLayout, true);
            this.mUndoDeletionBar = (ViewGroup) v.findViewById(R.id.camera_undo_deletion_bar);
            View button = this.mUndoDeletionBar.findViewById(R.id.camera_undo_deletion_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CameraActivity.this.mDataAdapter.undoDataRemoval();
                    CameraActivity.this.hideUndoDeletionBar(true);
                }
            });
            this.mUndoDeletionBar.setClickable(true);
            button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v2, MotionEvent event) {
                    if (event.getActionMasked() == 0) {
                        CameraActivity.this.mIsUndoingDeletion = true;
                    } else if (event.getActionMasked() == 1) {
                        CameraActivity.this.mIsUndoingDeletion = false;
                    }
                    return false;
                }
            });
        }
        this.mUndoDeletionBar.setAlpha(0.0f);
        this.mUndoDeletionBar.setVisibility(0);
        this.mUndoDeletionBar.animate().setDuration(200L).alpha(1.0f).setListener(null).start();
    }

    private void hideUndoDeletionBar(boolean withAnimation) {
        Log.v(TAG, "Hiding undo deletion bar");
        this.mPendingDeletion = false;
        if (this.mUndoDeletionBar != null) {
            if (withAnimation) {
                this.mUndoDeletionBar.animate().setDuration(200L).alpha(0.0f).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        CameraActivity.this.mUndoDeletionBar.setVisibility(8);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                }).start();
            } else {
                this.mUndoDeletionBar.setVisibility(8);
            }
        }
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation != this.mLastRawOrientation) {
            Log.v(TAG, "orientation changed (from:to) " + this.mLastRawOrientation + ":" + orientation);
        }
        if (orientation != -1) {
            this.mLastRawOrientation = orientation;
            if (this.mCurrentModule != null) {
                this.mCurrentModule.onOrientationChanged(orientation);
            }
        }
    }

    public void setSwipingEnabled(boolean enable) {
        if (isCaptureIntent()) {
        }
    }

    public long getFirstPreviewTime() {
        if (!(this.mCurrentModule instanceof PhotoModule)) {
            return -1L;
        }
        long coverHiddenTime = getCameraAppUI().getCoverHiddenTime();
        if (coverHiddenTime != -1) {
            return coverHiddenTime - this.mOnCreateTime;
        }
        return -1L;
    }

    public long getAutoFocusTime() {
        if (this.mCurrentModule instanceof PhotoModule) {
            return ((PhotoModule) this.mCurrentModule).mAutoFocusTime;
        }
        return -1L;
    }

    public long getShutterLag() {
        if (this.mCurrentModule instanceof PhotoModule) {
            return ((PhotoModule) this.mCurrentModule).mShutterLag;
        }
        return -1L;
    }

    public long getShutterToPictureDisplayedTime() {
        if (this.mCurrentModule instanceof PhotoModule) {
            return ((PhotoModule) this.mCurrentModule).mShutterToPictureDisplayedTime;
        }
        return -1L;
    }

    public long getPictureDisplayedToJpegCallbackTime() {
        if (this.mCurrentModule instanceof PhotoModule) {
            return ((PhotoModule) this.mCurrentModule).mPictureDisplayedToJpegCallbackTime;
        }
        return -1L;
    }

    public long getJpegCallbackFinishTime() {
        if (this.mCurrentModule instanceof PhotoModule) {
            return ((PhotoModule) this.mCurrentModule).mJpegCallbackFinishTime;
        }
        return -1L;
    }

    public long getCaptureStartTime() {
        if (this.mCurrentModule instanceof PhotoModule) {
            return ((PhotoModule) this.mCurrentModule).mCaptureStartTime;
        }
        return -1L;
    }

    public boolean isRecording() {
        if (this.mCurrentModule instanceof VideoModule) {
            return ((VideoModule) this.mCurrentModule).isRecording();
        }
        return false;
    }

    public CameraAgent.CameraOpenCallback getCameraOpenErrorCallback() {
        return this.mCameraController;
    }

    public CameraModule getCurrentModule() {
        return this.mCurrentModule;
    }

    @Override
    public void showTutorial(AbstractTutorialOverlay tutorial) {
        this.mCameraAppUI.showTutorial(tutorial, getLayoutInflater());
    }

    @Override
    public void showErrorAndFinish(int messageId) {
        CameraUtil.showErrorAndFinish(this, messageId);
    }

    public void syncLocationManagerSetting() {
        Keys.syncLocationManager(this.mSettingsManager, this.mLocationManager);
    }

    private void keepScreenOnForAWhile() {
        if (!this.mKeepScreenOn) {
            this.mMainHandler.removeMessages(2);
            getWindow().addFlags(128);
            this.mMainHandler.sendEmptyMessageDelayed(2, SCREEN_DELAY_MS);
        }
    }

    private void resetScreenOn() {
        this.mKeepScreenOn = false;
        this.mMainHandler.removeMessages(2);
        getWindow().clearFlags(128);
    }

    private boolean startGallery() {
        if (this.mGalleryIntent != null) {
            try {
                UsageStatistics.instance().changeScreen(10000, 10000);
                Intent startGalleryIntent = new Intent(this.mGalleryIntent);
                int currentDataId = this.mFilmstripController.getCurrentId();
                LocalData currentLocalData = this.mDataAdapter.getLocalData(currentDataId);
                if (currentLocalData != null) {
                    GalleryHelper.setContentUri(startGalleryIntent, currentLocalData.getUri());
                }
                launchActivityByIntent(startGalleryIntent);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Failed to launch gallery activity, closing");
            }
        }
        return false;
    }

    private void setNfcBeamPushUriFromData(LocalData data) {
        Uri uri = data.getUri();
        if (uri != Uri.EMPTY) {
            this.mNfcPushUris[0] = uri;
        } else {
            this.mNfcPushUris[0] = null;
        }
    }

    private void updateUiByData(int dataId) {
        LocalData currentData = this.mDataAdapter.getLocalData(dataId);
        if (currentData == null) {
            Log.w(TAG, "Current data ID not found.");
            hideSessionProgress();
            return;
        }
        updateActionBarMenu(currentData);
        updateBottomControlsByData(currentData);
        if (isSecureCamera()) {
            this.mCameraAppUI.getFilmstripBottomControls().hideControls();
            return;
        }
        setNfcBeamPushUriFromData(currentData);
        if (!this.mDataAdapter.isMetadataUpdated(dataId)) {
            this.mDataAdapter.updateMetadata(dataId);
        }
    }

    private void updateBottomControlsByData(LocalData currentData) {
        int sessionProgress;
        int viewButtonVisibility;
        CameraAppUI.BottomPanel filmstripBottomPanel = this.mCameraAppUI.getFilmstripBottomControls();
        filmstripBottomPanel.showControls();
        filmstripBottomPanel.setEditButtonVisibility(currentData.isDataActionSupported(4));
        filmstripBottomPanel.setShareButtonVisibility(currentData.isDataActionSupported(8));
        filmstripBottomPanel.setDeleteButtonVisibility(currentData.isDataActionSupported(2));
        Uri contentUri = currentData.getUri();
        CaptureSessionManager sessionManager = getServices().getCaptureSessionManager();
        if (sessionManager.hasErrorMessage(contentUri)) {
            showProcessError(sessionManager.getErrorMesage(contentUri));
        } else {
            filmstripBottomPanel.hideProgressError();
            CaptureSession session = sessionManager.getSession(contentUri);
            if (session == null || (sessionProgress = session.getProgress()) < 0) {
                hideSessionProgress();
            } else {
                CharSequence progressMessage = session.getProgressMessage();
                showSessionProgress(progressMessage);
                updateSessionProgress(sessionProgress);
            }
        }
        if (PanoramaMetadataLoader.isPanoramaAndUseViewer(currentData)) {
            viewButtonVisibility = 1;
        } else if (RgbzMetadataLoader.hasRGBZData(currentData)) {
            viewButtonVisibility = 2;
        } else {
            viewButtonVisibility = 0;
        }
        filmstripBottomPanel.setTinyPlanetEnabled(PanoramaMetadataLoader.isPanorama360(currentData));
        filmstripBottomPanel.setViewerButtonVisibility(viewButtonVisibility);
    }

    private static class PeekAnimationHandler extends Handler {
        private final FrameLayout mAboveFilmstripControlLayout;
        private final Handler mMainHandler;

        private class DataAndCallback {
            Callback<Bitmap> mCallback;
            LocalData mData;

            public DataAndCallback(LocalData data, Callback<Bitmap> callback) {
                this.mData = data;
                this.mCallback = callback;
            }
        }

        public PeekAnimationHandler(Looper looper, Handler mainHandler, FrameLayout aboveFilmstripControlLayout) {
            super(looper);
            this.mMainHandler = mainHandler;
            this.mAboveFilmstripControlLayout = aboveFilmstripControlLayout;
        }

        public void startDecodingJob(LocalData data, Callback<Bitmap> callback) {
            obtainMessage(0, new DataAndCallback(data, callback)).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) throws IOException {
            final Bitmap bitmap;
            LocalData data = ((DataAndCallback) msg.obj).mData;
            final Callback<Bitmap> callback = ((DataAndCallback) msg.obj).mCallback;
            if (data != null && callback != null) {
                switch (data.getLocalDataType()) {
                    case 3:
                        try {
                            FileInputStream stream = new FileInputStream(data.getPath());
                            Point dim = CameraUtil.resizeToFill(data.getWidth(), data.getHeight(), data.getRotation(), this.mAboveFilmstripControlLayout.getWidth(), this.mAboveFilmstripControlLayout.getMeasuredHeight());
                            if (data.getRotation() % 180 != 0) {
                                int dummy = dim.x;
                                dim.x = dim.y;
                                dim.y = dummy;
                            }
                            bitmap = LocalDataUtil.loadImageThumbnailFromStream(stream, data.getWidth(), data.getHeight(), (int) (dim.x * 0.7f), (int) (((double) dim.y) * 0.7d), data.getRotation(), CameraActivity.MAX_PEEK_BITMAP_PIXELS);
                        } catch (FileNotFoundException e) {
                            Log.e(CameraActivity.TAG, "File not found:" + data.getPath());
                            return;
                        }
                        break;
                    case 4:
                        bitmap = LocalDataUtil.loadVideoThumbnail(data.getPath());
                        break;
                    case 5:
                        byte[] jpegData = Storage.getJpegForSession(data.getUri());
                        if (jpegData != null) {
                            bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
                        } else {
                            bitmap = null;
                        }
                        break;
                    default:
                        bitmap = null;
                        break;
                }
                if (bitmap != null) {
                    this.mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onCallback(bitmap);
                        }
                    });
                }
            }
        }
    }

    private void showDetailsDialog(int dataId) {
        MediaDetails details;
        LocalData data = this.mDataAdapter.getLocalData(dataId);
        if (data != null && (details = data.getMediaDetails(getAndroidContext())) != null) {
            Dialog detailDialog = DetailsDialog.create(this, details);
            detailDialog.show();
            UsageStatistics.instance().mediaInteraction(fileNameFromDataID(dataId), 10000, 10000, fileAgeFromDataID(dataId));
        }
    }

    private void updateActionBarMenu(LocalData data) {
        MenuItem detailsMenuItem;
        if (this.mActionBarMenu != null && (detailsMenuItem = this.mActionBarMenu.findItem(R.id.action_details)) != null) {
            int type = data.getLocalDataType();
            boolean showDetails = type == 3 || type == 4;
            detailsMenuItem.setVisible(showDetails);
        }
    }
}
