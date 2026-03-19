package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AppBridge;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.PhotoDataAdapter;
import com.android.gallery3d.app.PhotoPageBottomControls;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.ClusterAlbum;
import com.android.gallery3d.data.ComboAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.EmptyAlbumImage;
import com.android.gallery3d.data.FilterDeleteSet;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.SecureAlbum;
import com.android.gallery3d.data.SecureSource;
import com.android.gallery3d.data.SnailAlbum;
import com.android.gallery3d.data.SnailItem;
import com.android.gallery3d.data.SnailSource;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.UsageStatistics;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallerybasic.base.BackwardBottomController;
import com.mediatek.gallerybasic.base.IActionBar;
import com.mediatek.gallerybasic.base.IBottomControl;
import com.mediatek.galleryportable.ActivityChooserModelWrapper;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

public abstract class PhotoPage extends ActivityState implements AppBridge.Server, GalleryActionBar.OnAlbumModeSelectedListener, PhotoPageBottomControls.Delegate, PhotoView.Listener, ActivityChooserModelWrapper.OnChooseActivityListenerWrapper {
    private GalleryActionBar mActionBar;
    private IActionBar[] mActionBarExts;
    private AppBridge mAppBridge;
    private GalleryApp mApplication;
    private BackwardBottomController mBackwardBottomController;
    private PhotoPageBottomControls mBottomControls;
    private long mCurrentVersion;
    private boolean mDeleteIsFocus;
    private Path mDeletePath;
    private DetailsHelper mDetailsHelper;
    private ViewGroup mGalleryRoot;
    private Handler mHandler;
    private boolean mHaveImageEditor;
    private boolean mIsActive;
    private boolean mIsBackwardToggle;
    private boolean mIsMenuVisible;
    private boolean mIsPanorama;
    private boolean mIsPanorama360;
    private boolean mIsStartingVideoPlayer;
    private FilterDeleteSet mMediaSet;
    private MenuExecutor mMenuExecutor;
    private Model mModel;
    private MuteVideo mMuteVideo;
    private OrientationManager mOrientationManager;
    private String mOriginalSetPathString;
    private PhotoView mPhotoView;
    private SnailItem mScreenNailItem;
    private SnailAlbum mScreenNailSet;
    private SecureAlbum mSecureAlbum;
    private SelectionManager mSelectionManager;
    private String mSetPathString;
    private boolean mShowDetails;
    private boolean mShowSpinner;
    private Path mSnailSetPath;
    private boolean mStartInFilmstrip;
    private boolean mTreatBackAsUp;
    private int mCurrentIndex = 0;
    private boolean mShowBars = true;
    private volatile boolean mActionBarAllowed = true;
    private MediaItem mCurrentPhoto = null;
    private boolean mReadOnlyView = false;
    private boolean mHasCameraScreennailOrPlaceholder = false;
    private boolean mRecenterCameraOnResume = true;
    private long mCameraSwitchCutoff = 0;
    private boolean mSkipUpdateCurrentPhoto = false;
    private boolean mDeferredUpdateWaiting = false;
    private long mDeferUpdateUntil = Long.MAX_VALUE;
    private Uri[] mNfcPushUris = new Uri[1];
    private final MyMenuVisibilityListener mMenuVisibilityListener = new MyMenuVisibilityListener();
    private int mLastSystemUiVis = 0;
    private boolean mDisableBarChanges = false;
    private Uri mShareUriFromChooserView = null;
    private final MediaObject.PanoramaSupportCallback mUpdatePanoramaMenuItemsCallback = new MediaObject.PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2) {
            if (mediaObject == PhotoPage.this.mCurrentPhoto) {
                PhotoPage.this.mHandler.obtainMessage(16, z2 ? 1 : 0, 0, mediaObject).sendToTarget();
            }
        }
    };
    private final MediaObject.PanoramaSupportCallback mRefreshBottomControlsCallback = new MediaObject.PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2) {
            if (mediaObject == PhotoPage.this.mCurrentPhoto) {
                PhotoPage.this.mHandler.obtainMessage(8, z ? 1 : 0, z2 ? 1 : 0, mediaObject).sendToTarget();
            }
        }
    };
    private final MediaObject.PanoramaSupportCallback mUpdateShareURICallback = new MediaObject.PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2) {
            if (mediaObject == PhotoPage.this.mCurrentPhoto) {
                PhotoPage.this.mHandler.obtainMessage(15, z2 ? 1 : 0, 0, mediaObject).sendToTarget();
            }
        }
    };
    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PhotoPage.this.mActivity.finish();
        }
    };
    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            PhotoPage.this.mPhotoView.layout(0, 0, i3 - i, i4 - i2);
            if (PhotoPage.this.mShowDetails) {
                PhotoPage.this.mDetailsHelper.layout(i, PhotoPage.this.mActionBar.getHeight(), i3, i4);
            }
        }
    };
    private MenuExecutor.ProgressListener mConfirmDialogListener = new MenuExecutor.ProgressListener() {
        @Override
        public void onProgressUpdate(int i) {
        }

        @Override
        public void onProgressComplete(int i) {
        }

        @Override
        public void onConfirmDialogShown() {
            PhotoPage.this.mHandler.removeMessages(1);
        }

        @Override
        public void onConfirmDialogDismissed(boolean z) {
            PhotoPage.this.refreshHidingMessage();
        }

        @Override
        public void onProgressStart() {
        }
    };
    public boolean mLoadingFinished = false;
    private boolean mLaunchFromCamera = false;
    private boolean mPlaySecureVideo = false;
    private boolean mAllowAutoHideByHost = true;
    private int mSupportedOperations = 0;
    private boolean mIsAppBridgeFullScreenChangeEnabled = true;
    private IBottomControl[] mBottomControlExts = new IBottomControl[0];

    public interface Model extends PhotoView.Model {
        boolean isEmpty();

        void pause();

        void resume();

        void setCurrentPhoto(Path path, int i);
    }

    private class MyMenuVisibilityListener implements ActionBar.OnMenuVisibilityListener {
        private MyMenuVisibilityListener() {
        }

        @Override
        public void onMenuVisibilityChanged(boolean z) {
            PhotoPage.this.mIsMenuVisible = z;
            PhotoPage.this.refreshHidingMessage();
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.photo_background;
    }

    @Override
    public void onCreate(Bundle bundle, Bundle bundle2) {
        Path path;
        boolean zIsPanorama;
        super.onCreate(bundle, bundle2);
        this.mActionBar = this.mActivity.getGalleryActionBar();
        this.mSelectionManager = new SelectionManager(this.mActivity, false);
        this.mMenuExecutor = new MenuExecutor(this.mActivity, this.mSelectionManager);
        this.mPhotoView = new PhotoView(this.mActivity);
        this.mPhotoView.setListener(this);
        this.mRootPane.addComponent(this.mPhotoView);
        this.mApplication = (GalleryApp) this.mActivity.getApplication();
        this.mOrientationManager = this.mActivity.getOrientationManager();
        this.mActivity.getGLRoot().setOrientationSource(this.mOrientationManager);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                boolean z;
                switch (message.what) {
                    case 1:
                        if (PhotoPage.this.mIsActive) {
                            PhotoPage.this.hideBars();
                            return;
                        }
                        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<mHandler.MSG_HIDE_BARS> mIsActive = " + PhotoPage.this.mIsActive + ", not hideBars");
                        return;
                    case 2:
                    case 3:
                    case 13:
                    default:
                        throw new AssertionError(message.what);
                    case 4:
                        if (PhotoPage.this.mAppBridge != null) {
                            PhotoPage.this.mAppBridge.onFullScreenChanged(message.arg1 == 1);
                            return;
                        }
                        return;
                    case 5:
                        PhotoPage.this.updateBars();
                        return;
                    case 6:
                        PhotoPage.this.mActivity.getGLRoot().unfreeze();
                        return;
                    case 7:
                        PhotoPage.this.wantBars();
                        return;
                    case 8:
                        if (PhotoPage.this.mCurrentPhoto == message.obj && PhotoPage.this.mBottomControls != null) {
                            PhotoPage.this.mIsPanorama = message.arg1 == 1;
                            PhotoPage.this.mIsPanorama360 = message.arg2 == 1;
                            PhotoPage.this.mBottomControls.refresh();
                            return;
                        }
                        return;
                    case 9:
                        PhotoPage.this.mSkipUpdateCurrentPhoto = false;
                        if (PhotoPage.this.mPhotoView.getFilmMode()) {
                            if (SystemClock.uptimeMillis() >= PhotoPage.this.mCameraSwitchCutoff || PhotoPage.this.mMediaSet.getMediaItemCount() <= 1) {
                                if (PhotoPage.this.mAppBridge != null) {
                                    PhotoPage.this.mPhotoView.setFilmMode(false);
                                }
                                z = true;
                            } else {
                                PhotoPage.this.mPhotoView.switchToImage(1);
                                z = false;
                            }
                        } else {
                            z = true;
                        }
                        if (z) {
                            if (PhotoPage.this.mAppBridge != null || PhotoPage.this.mMediaSet.getTotalMediaItemCount() <= 1) {
                                PhotoPage.this.updateBars();
                                MediaItem mediaItem = PhotoPage.this.mModel.getMediaItem(0);
                                if (mediaItem != null) {
                                    PhotoPage.this.updateCurrentPhoto(mediaItem);
                                    return;
                                }
                                return;
                            }
                            PhotoPage.this.launchCamera();
                            PhotoPage.this.mPhotoView.stopUpdateEngineData();
                            PhotoPage.this.mPhotoView.switchToImage(1);
                            return;
                        }
                        return;
                    case 10:
                        return;
                    case 11:
                        MediaItem mediaItem2 = PhotoPage.this.mCurrentPhoto;
                        PhotoPage.this.mCurrentPhoto = null;
                        PhotoPage.this.updateCurrentPhoto(mediaItem2);
                        return;
                    case 12:
                        PhotoPage.this.updateUIForCurrentPhoto();
                        return;
                    case 14:
                        long jUptimeMillis = PhotoPage.this.mDeferUpdateUntil - SystemClock.uptimeMillis();
                        if (jUptimeMillis <= 0) {
                            PhotoPage.this.mDeferredUpdateWaiting = false;
                            PhotoPage.this.updateUIForCurrentPhoto();
                            return;
                        } else {
                            PhotoPage.this.mHandler.sendEmptyMessageDelayed(14, jUptimeMillis);
                            return;
                        }
                    case 15:
                        if (PhotoPage.this.mIsActive) {
                            boolean z2 = message.arg1 != 0;
                            Uri contentUri = PhotoPage.this.mCurrentPhoto.getContentUri();
                            PhotoPage.this.mActionBar.setShareIntents(z2 ? PhotoPage.createSharePanoramaIntent(contentUri) : null, PhotoPage.createShareIntent(PhotoPage.this.mCurrentPhoto), PhotoPage.this);
                            PhotoPage.this.setNfcBeamPushUri(contentUri);
                            return;
                        }
                        return;
                    case 16:
                        if (PhotoPage.this.mCurrentPhoto == message.obj) {
                            PhotoPage.this.updatePanoramaUI(message.arg1 != 0);
                            return;
                        }
                        return;
                }
            }
        };
        this.mSetPathString = bundle.getString("media-set-path");
        this.mLaunchFromCamera = bundle.getBoolean("isCamera", false);
        this.mReadOnlyView = bundle.getBoolean("read-only") && (this.mSetPathString == null || this.mSetPathString.equals(""));
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onCreate> mSetPathString = " + this.mSetPathString + ", mReadOnlyView = " + this.mReadOnlyView);
        this.mOriginalSetPathString = this.mSetPathString;
        setupNfcBeamPush();
        if (bundle.getString("media-item-path") != null) {
            path = Path.fromString(bundle.getString("media-item-path"));
        } else {
            path = null;
        }
        this.mTreatBackAsUp = bundle.getBoolean("treat-back-as-up", false);
        this.mStartInFilmstrip = bundle.getBoolean("start-in-filmstrip", false);
        bundle.getBoolean("in_camera_roll", false);
        this.mCurrentIndex = bundle.getInt("index-hint", 0);
        if (this.mSetPathString != null) {
            this.mShowSpinner = true;
            if (!this.mSetPathString.equals("/local/all/0") && SecureSource.isSecurePath(this.mSetPathString)) {
                com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onCreate> secure album");
                this.mFlags |= 32;
                this.mSecureAlbum = (SecureAlbum) this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
                this.mSecureAlbum.clearAll();
                ArrayList arrayList = (ArrayList) bundle.getSerializable("secureAlbum");
                if (arrayList != null) {
                    int size = arrayList.size();
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onCreate> albumCount " + size);
                    this.mActivity.registerReceiver(this.mScreenOffReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
                    for (int i = 0; i < size; i++) {
                        try {
                            String[] strArrSplit = ((String) arrayList.get(i)).split("\\+");
                            int length = strArrSplit.length;
                            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onCreate> albumItemSize " + length);
                            if (length == 2) {
                                int i2 = Integer.parseInt(strArrSplit[0].trim());
                                boolean z = Boolean.parseBoolean(strArrSplit[1].trim());
                                com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onCreate> secure item : id " + i2 + ", isVideo " + z);
                                this.mSecureAlbum.addMediaItem(z, i2);
                            }
                        } catch (NullPointerException e) {
                            com.android.gallery3d.util.Log.e("Gallery2/PhotoPage", "<onCreate> exception " + e);
                        } catch (NumberFormatException e2) {
                            com.android.gallery3d.util.Log.e("Gallery2/PhotoPage", "<onCreate> exception " + e2);
                        } catch (PatternSyntaxException e3) {
                            com.android.gallery3d.util.Log.e("Gallery2/PhotoPage", "<onCreate> exception " + e3);
                        }
                    }
                }
                this.mShowSpinner = false;
                this.mSetPathString = "/filter/empty/{" + this.mSetPathString + "}";
                this.mSetPathString = "/combo/item/{" + this.mSetPathString + "}";
            }
            this.mAppBridge = (AppBridge) bundle.getParcelable("app-bridge");
            if (this.mAppBridge != null) {
                this.mShowBars = false;
                this.mHasCameraScreennailOrPlaceholder = true;
                this.mAppBridge.setServer(this);
                int iNewId = SnailSource.newId();
                Path setPath = SnailSource.getSetPath(iNewId);
                path = SnailSource.getItemPath(iNewId);
                this.mScreenNailSet = (SnailAlbum) this.mActivity.getDataManager().getMediaObject(setPath);
                this.mScreenNailItem = (SnailItem) this.mActivity.getDataManager().getMediaObject(path);
                this.mScreenNailItem.setScreenNail(this.mAppBridge.attachScreenNail());
                if (bundle.getBoolean("show_when_locked", false)) {
                    this.mFlags |= 32;
                }
                if (!this.mSetPathString.equals("/local/all/0")) {
                    if (SecureSource.isSecurePath(this.mSetPathString)) {
                        this.mSecureAlbum = (SecureAlbum) this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
                        this.mShowSpinner = false;
                    }
                    this.mSetPathString = "/filter/empty/{" + this.mSetPathString + "}";
                }
                this.mSetPathString = "/combo/item/{" + setPath + "," + this.mSetPathString + "}";
            } else if (this.mLaunchFromCamera && this.mSecureAlbum == null) {
                this.mSetPathString = "/filter/empty/{" + this.mSetPathString + "}";
                StringBuilder sb = new StringBuilder();
                sb.append("<onCreate> launch from camera, not secure, mSetPathString = ");
                sb.append(this.mSetPathString);
                com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", sb.toString());
            }
            ?? mediaSet = this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
            if (this.mHasCameraScreennailOrPlaceholder && (mediaSet instanceof ComboAlbum)) {
                mediaSet.useNameOfChild(1);
            }
            if (mediaSet != 0 && (mediaSet instanceof ClusterAlbum)) {
                this.mPhotoView.setIsCluster(true);
            } else {
                this.mPhotoView.setIsCluster(false);
            }
            this.mSelectionManager.setSourceMediaSet(mediaSet);
            this.mSetPathString = "/filter/delete/{" + this.mSetPathString + "}";
            this.mMediaSet = (FilterDeleteSet) this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
            if (this.mMediaSet == null) {
                com.android.gallery3d.util.Log.w("Gallery2/PhotoPage", "failed to restore " + this.mSetPathString);
            }
            if (path == null) {
                int mediaItemCount = this.mMediaSet.getMediaItemCount();
                if (mediaItemCount > 0) {
                    if (this.mCurrentIndex >= mediaItemCount) {
                        this.mCurrentIndex = 0;
                    }
                    path = this.mMediaSet.getMediaItem(this.mCurrentIndex, 1).get(0).getPath();
                } else {
                    return;
                }
            }
            Path path2 = path;
            AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
            PhotoView photoView = this.mPhotoView;
            FilterDeleteSet filterDeleteSet = this.mMediaSet;
            int i3 = this.mCurrentIndex;
            int i4 = this.mAppBridge == null ? -1 : 0;
            if (this.mAppBridge != null) {
                zIsPanorama = this.mAppBridge.isPanorama();
            } else {
                zIsPanorama = false;
            }
            PhotoDataAdapter photoDataAdapter = new PhotoDataAdapter(abstractGalleryActivity, photoView, filterDeleteSet, path2, i3, i4, zIsPanorama, this.mAppBridge == null ? false : this.mAppBridge.isStaticCamera());
            this.mModel = photoDataAdapter;
            this.mPhotoView.setModel(this.mModel);
            photoDataAdapter.setDataListener(new PhotoDataAdapter.DataListener() {
                @Override
                public void onPhotoChanged(int i5, Path path3) {
                    MediaItem mediaItem;
                    int i6 = PhotoPage.this.mCurrentIndex;
                    PhotoPage.this.mCurrentIndex = i5;
                    if (PhotoPage.this.mHasCameraScreennailOrPlaceholder) {
                        if (PhotoPage.this.mCurrentIndex > 0) {
                            PhotoPage.this.mSkipUpdateCurrentPhoto = false;
                        }
                        if (i6 == 0 && PhotoPage.this.mCurrentIndex > 0) {
                            PhotoPage.this.onActionBarAllowed(true);
                            PhotoPage.this.mPhotoView.setFilmMode(false);
                            if (PhotoPage.this.mAppBridge != null) {
                                UsageStatistics.onEvent("CameraToFilmstrip", "Swipe", null);
                            }
                        } else if (i6 == 2 && PhotoPage.this.mCurrentIndex == 1) {
                            PhotoPage.this.mCameraSwitchCutoff = SystemClock.uptimeMillis() + 300;
                            PhotoPage.this.mPhotoView.stopScrolling();
                        } else if (i6 >= 1 && PhotoPage.this.mCurrentIndex == 0) {
                            PhotoPage.this.mPhotoView.setWantPictureCenterCallbacks(true);
                            PhotoPage.this.mSkipUpdateCurrentPhoto = true;
                        }
                    }
                    if (!PhotoPage.this.mSkipUpdateCurrentPhoto) {
                        if (path3 != null && (mediaItem = PhotoPage.this.mModel.getMediaItem(0)) != null) {
                            PhotoPage.this.updateCurrentPhoto(mediaItem);
                        }
                        PhotoPage.this.updateBars();
                    }
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onPhotoChanged> refreshHidingMessage");
                    PhotoPage.this.refreshHidingMessage();
                }

                @Override
                public void onLoadingFinished(boolean z2) {
                    PhotoPage.this.mLoadingFinished = true;
                    PhotoPage.this.refreshBottomControlsWhenReady();
                    PhotoPage.this.mSelectionManager.onSourceContentChanged();
                    if (!PhotoPage.this.mModel.isEmpty()) {
                        MediaItem mediaItem = PhotoPage.this.mModel.getMediaItem(0);
                        if (mediaItem != null) {
                            PhotoPage.this.updateCurrentPhoto(mediaItem);
                            return;
                        }
                        return;
                    }
                    if (PhotoPage.this.mIsActive && PhotoPage.this.mMediaSet.getNumberOfDeletions() == 0) {
                        PhotoPage.this.mPhotoView.pause();
                        PhotoPage.this.mActivity.getStateManager().finishState(PhotoPage.this);
                    }
                }

                @Override
                public void onLoadingStarted() {
                    PhotoPage.this.mLoadingFinished = false;
                }
            });
        } else {
            MediaItem mediaItem = (MediaItem) this.mActivity.getDataManager().getMediaObject(path);
            if (mediaItem == null) {
                Toast.makeText(this.mActivity, R.string.no_such_item, 1).show();
                this.mPhotoView.pause();
                this.mActivity.getStateManager().finishState(this);
                return;
            } else {
                this.mLoadingFinished = true;
                this.mModel = new SinglePhotoDataAdapter(this.mActivity, this.mPhotoView, mediaItem);
                this.mPhotoView.setModel(this.mModel);
                updateCurrentPhoto(mediaItem);
                this.mShowSpinner = false;
            }
        }
        this.mPhotoView.setFilmMode(this.mStartInFilmstrip && this.mMediaSet.getMediaItemCount() > 1);
        RelativeLayout relativeLayout = (RelativeLayout) this.mActivity.findViewById(this.mAppBridge != null ? R.id.content : R.id.gallery_root);
        if (relativeLayout != null && this.mSecureAlbum == null) {
            setupBottomControlExtension(this.mActivity);
            this.mBottomControls = new PhotoPageBottomControls();
            this.mBottomControls.setup(this, this.mActivity, relativeLayout);
        }
        setOnSystemUiVisibilityChangeListener();
    }

    @Override
    public void onPictureCenter(boolean z) {
        boolean z2;
        if (z || (this.mHasCameraScreennailOrPlaceholder && this.mAppBridge == null)) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.mPhotoView.setWantPictureCenterCallbacks(false);
        this.mHandler.removeMessages(9);
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(z2 ? 9 : 10);
    }

    @Override
    public boolean canDisplayBottomControls() {
        boolean z = this.mIsActive && !this.mPhotoView.canUndo();
        boolean z2 = z;
        for (IBottomControl iBottomControl : this.mBottomControlExts) {
            int iCanDisplayBottomControls = iBottomControl.canDisplayBottomControls();
            if (iCanDisplayBottomControls != 0) {
                z2 = z2 && iCanDisplayBottomControls == 1;
            }
        }
        return z2;
    }

    @Override
    public boolean canDisplayBottomControl(int i) {
        if (this.mCurrentPhoto == null) {
            return false;
        }
        switch (i) {
            case R.id.photopage_bottom_control_edit:
                return this.mHaveImageEditor && this.mShowBars && !this.mReadOnlyView && !this.mPhotoView.getFilmMode() && (this.mCurrentPhoto.getSupportedOperations() & 512) != 0 && this.mCurrentPhoto.getMediaType() == 2;
            case R.id.photopage_bottom_control_panorama:
                return this.mIsPanorama;
            case R.id.photopage_bottom_control_tiny_planet:
                return this.mHaveImageEditor && this.mShowBars && this.mIsPanorama360 && !this.mPhotoView.getFilmMode();
            default:
                boolean z = (!this.mShowBars || this.mReadOnlyView || this.mPhotoView.getFilmMode()) ? false : true;
                boolean z2 = z;
                for (IBottomControl iBottomControl : this.mBottomControlExts) {
                    int iCanDisplayBottomControlButton = iBottomControl.canDisplayBottomControlButton(i, this.mCurrentPhoto.getMediaData());
                    if (iCanDisplayBottomControlButton != 0) {
                        z2 = z2 && iCanDisplayBottomControlButton == 1;
                    }
                }
                return z2;
        }
    }

    @Override
    public void onBottomControlClicked(int i) {
        MediaItem mediaItem;
        switch (i) {
            case R.id.photopage_bottom_control_edit:
                if (this.mModel != null && (mediaItem = this.mModel.getMediaItem(0)) != null) {
                    File file = new File(mediaItem.getFilePath());
                    if (!file.exists()) {
                        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onBottomControlClicked> abort editing photo when not exists!");
                    } else if (!isSpaceEnough(file)) {
                        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onBottomControlClicked> no enough space, abort edit");
                        Toast.makeText(this.mActivity, this.mActivity.getString(R.string.storage_not_enough), 0).show();
                    } else {
                        launchPhotoEditor();
                    }
                }
                break;
            case R.id.photopage_bottom_control_panorama:
                this.mActivity.getPanoramaViewHelper().showPanorama(this.mCurrentPhoto.getContentUri());
                break;
            case R.id.photopage_bottom_control_tiny_planet:
                launchTinyPlanet();
                break;
            default:
                IBottomControl[] iBottomControlArr = this.mBottomControlExts;
                int length = iBottomControlArr.length;
                for (int i2 = 0; i2 < length && !iBottomControlArr[i2].onBottomControlButtonClicked(i, this.mCurrentPhoto.getMediaData()); i2++) {
                }
                break;
        }
    }

    @TargetApi(16)
    private void setupNfcBeamPush() {
        NfcAdapter defaultAdapter;
        if (ApiHelper.HAS_SET_BEAM_PUSH_URIS && (defaultAdapter = NfcAdapter.getDefaultAdapter(this.mActivity)) != null) {
            defaultAdapter.setBeamPushUris(null, this.mActivity);
            defaultAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent nfcEvent) {
                    return PhotoPage.this.mNfcPushUris;
                }
            }, this.mActivity);
        }
    }

    private void setNfcBeamPushUri(Uri uri) {
        if (this.mShareUriFromChooserView != null) {
            this.mNfcPushUris[0] = this.mShareUriFromChooserView;
            this.mShareUriFromChooserView = null;
        } else {
            this.mNfcPushUris[0] = uri;
        }
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<setNfcBeamPushUri> uri " + this.mNfcPushUris[0]);
    }

    private static Intent createShareIntent(MediaObject mediaObject) {
        return new Intent("android.intent.action.SEND").setType(MenuExecutor.getMimeType(mediaObject.getMediaType())).putExtra("android.intent.extra.STREAM", mediaObject.getContentUri()).addFlags(1);
    }

    private static Intent createSharePanoramaIntent(Uri uri) {
        return new Intent("android.intent.action.SEND").setType("application/vnd.google.panorama360+jpg").putExtra("android.intent.extra.STREAM", uri).addFlags(1);
    }

    private void overrideTransitionToEditor() {
        this.mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void launchTinyPlanet() {
        MediaItem mediaItem = this.mModel.getMediaItem(0);
        Intent intent = new Intent("com.android.camera.action.TINY_PLANET");
        intent.setClass(this.mActivity, FilterShowActivity.class);
        intent.setDataAndType(mediaItem.getContentUri(), mediaItem.getMimeType()).setFlags(1);
        intent.putExtra("launch-fullscreen", this.mActivity.isFullscreen());
        this.mActivity.startActivityForResult(intent, 4);
        overrideTransitionToEditor();
    }

    private void launchCamera() {
        this.mRecenterCameraOnResume = false;
        GalleryUtils.startCameraActivity(this.mActivity);
    }

    private void launchPhotoEditor() {
        if (this.mModel != null && this.mModel.getLoadingState(0) == 2) {
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<launchPhotoEditor> abort editing photo if loading fail!");
            Toast.makeText(this.mActivity, this.mActivity.getString(R.string.cannot_load_image), 0).show();
            return;
        }
        MediaItem mediaItem = this.mModel.getMediaItem(0);
        if (mediaItem == null || (mediaItem.getSupportedOperations() & 512) == 0) {
            return;
        }
        Intent intent = new Intent("action_nextgen_edit");
        intent.setDataAndType(mediaItem.getContentUri(), mediaItem.getMimeType()).setFlags(335544321);
        if (this.mActivity.getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
            intent.setAction("android.intent.action.EDIT");
        }
        intent.putExtra("launch-fullscreen", this.mActivity.isFullscreen());
        this.mActivity.startActivityForResult(Intent.createChooser(intent, null), 4);
        overrideTransitionToEditor();
    }

    private void launchSimpleEditor() {
        MediaItem mediaItem = this.mModel.getMediaItem(0);
        if (mediaItem == null || (mediaItem.getSupportedOperations() & 512) == 0 || BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("action_simple_edit");
        intent.setDataAndType(mediaItem.getContentUri(), mediaItem.getMimeType()).setFlags(1);
        if (this.mActivity.getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
            intent.setAction("android.intent.action.EDIT");
        }
        intent.putExtra("launch-fullscreen", this.mActivity.isFullscreen());
        this.mActivity.startActivityForResult(Intent.createChooser(intent, null), 4);
        overrideTransitionToEditor();
    }

    private void requestDeferredUpdate() {
        this.mDeferUpdateUntil = SystemClock.uptimeMillis() + 250;
        if (!this.mDeferredUpdateWaiting) {
            this.mDeferredUpdateWaiting = true;
            this.mHandler.sendEmptyMessageDelayed(14, 250L);
        }
    }

    private void updateUIForCurrentPhoto() {
        if (this.mCurrentPhoto == null) {
            return;
        }
        if ((this.mCurrentPhoto.getSupportedOperations() & 16384) != 0 && !this.mPhotoView.getFilmMode()) {
            this.mPhotoView.setWantPictureCenterCallbacks(true);
        }
        if (this.mIsActive && !(this.mCurrentPhoto instanceof SnailItem) && !(this.mCurrentPhoto instanceof EmptyAlbumImage)) {
            this.mActionBar.setShareIntents(null, createShareIntent(this.mCurrentPhoto), this);
        }
        updateMenuOperations();
        refreshBottomControlsWhenReady();
        if (this.mShowDetails) {
            this.mDetailsHelper.reloadDetails();
        }
        if (this.mSecureAlbum == null && (this.mCurrentPhoto.getSupportedOperations() & 4) != 0) {
            this.mCurrentPhoto.getPanoramaSupport(this.mUpdateShareURICallback);
        }
        if (this.mLaunchFromCamera && this.mCurrentPhoto != null && (this.mCurrentPhoto.getSupportedOperations() & 8192) != 0) {
            this.mPhotoView.setFilmMode(false);
        }
        updateScaleGesture();
    }

    private void updateCurrentPhoto(MediaItem mediaItem) {
        if (this.mCurrentPhoto == mediaItem && mediaItem.getDataVersion() == this.mCurrentVersion) {
            return;
        }
        this.mCurrentVersion = mediaItem.getDataVersion();
        this.mCurrentPhoto = mediaItem;
        if (this.mPhotoView.getFilmMode()) {
            requestDeferredUpdate();
        } else {
            updateUIForCurrentPhoto();
        }
    }

    private void updateMenuOperations() {
        Menu menu = this.mActionBar.getMenu();
        if (menu == null) {
            return;
        }
        MenuItem menuItemFindItem = menu.findItem(R.id.action_slideshow);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(this.mSecureAlbum == null && canDoSlideShow());
        }
        if (this.mCurrentPhoto == null) {
            return;
        }
        int supportedOperations = this.mCurrentPhoto.getSupportedOperations();
        if (this.mReadOnlyView) {
            supportedOperations &= -513;
        }
        if (this.mSecureAlbum != null) {
            supportedOperations &= 1;
        } else {
            this.mCurrentPhoto.getPanoramaSupport(this.mUpdatePanoramaMenuItemsCallback);
            if (!this.mHaveImageEditor) {
                supportedOperations &= -513;
            }
        }
        if (this.mCurrentPhoto.getMimeType() == null) {
            supportedOperations &= -2049;
        }
        new PrintHelper(this.mActivity.getAndroidContext());
        if (!PrintHelper.systemSupportsPrint()) {
            supportedOperations &= -131073;
        }
        this.mSupportedOperations = supportedOperations;
        MenuExecutor.updateMenuOperation(menu, supportedOperations);
        if (supportedOperations == 0) {
            menu.close();
        }
    }

    private boolean canDoSlideShow() {
        return (this.mMediaSet == null || this.mCurrentPhoto == null || this.mCurrentPhoto.getMediaType() != 2) ? false : true;
    }

    private void showBars() {
        if (this.mDisableBarChanges) {
            return;
        }
        onActionBarVisibilityChange(true);
        if (this.mShowBars) {
            return;
        }
        this.mShowBars = true;
        this.mOrientationManager.unlockOrientation();
        this.mActionBar.show();
        this.mActivity.getGLRoot().setLightsOutMode(false);
        if (this.mAllowAutoHideByHost) {
            refreshHidingMessage();
        }
        refreshBottomControlsWhenReady();
    }

    private void hideBars() {
        if (this.mDisableBarChanges) {
            return;
        }
        onActionBarVisibilityChange(false);
        if (this.mShowBars) {
            this.mShowBars = false;
            this.mActionBar.hide();
            this.mActivity.getGLRoot().setLightsOutMode(true);
            this.mHandler.removeMessages(1);
            refreshBottomControlsWhenReady();
        }
    }

    private void refreshHidingMessage() {
        this.mHandler.removeMessages(1);
        if (!this.mIsMenuVisible && !this.mPhotoView.getFilmMode()) {
            this.mHandler.sendEmptyMessageDelayed(1, 3500L);
        }
    }

    private boolean canShowBars() {
        return (this.mAppBridge == null || this.mCurrentIndex != 0 || this.mPhotoView.getFilmMode()) && this.mActionBarAllowed && this.mActivity.getResources().getConfiguration().touchscreen != 1;
    }

    private void wantBars() {
        if (canShowBars()) {
            showBars();
        }
    }

    private void toggleBars() {
        if (this.mShowBars) {
            hideBars();
        } else if (canShowBars()) {
            showBars();
        }
    }

    private void updateBars() {
        if (!canShowBars()) {
            hideBars();
        }
        updateActionBarTitle();
    }

    @Override
    protected void onBackPressed() {
        wantBars();
        boolean zOnBackPressed = this.mPhotoView.onBackPressed();
        for (IBottomControl iBottomControl : this.mBottomControlExts) {
            zOnBackPressed |= iBottomControl.onBackPressed();
        }
        if (zOnBackPressed) {
            return;
        }
        if (this.mShowDetails) {
            hideDetails();
            return;
        }
        if (this.mAppBridge == null || !switchWithCaptureAnimation(-1)) {
            setResult();
            if (this.mStartInFilmstrip && !this.mPhotoView.getFilmMode()) {
                this.mPhotoView.setFilmMode(true);
            } else if (this.mTreatBackAsUp) {
                onUpPressed();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void onUpPressed() {
        boolean zOnBackPressed = this.mPhotoView.onBackPressed();
        for (IBottomControl iBottomControl : this.mBottomControlExts) {
            zOnBackPressed |= iBottomControl.onBackPressed();
        }
        if (zOnBackPressed) {
            return;
        }
        if ((this.mStartInFilmstrip || this.mAppBridge != null) && !this.mPhotoView.getFilmMode()) {
            this.mPhotoView.setFilmMode(true);
            return;
        }
        if (this.mLaunchFromCamera && this.mCurrentPhoto != null && (this.mCurrentPhoto.getSupportedOperations() & 8192) != 0) {
            super.onBackPressed();
            return;
        }
        if (this.mLaunchFromCamera && this.mMediaSet.getMediaItemCount() >= 1 && !this.mPhotoView.getFilmMode()) {
            this.mPhotoView.setFilmMode(true);
            return;
        }
        if (this.mActivity.getStateManager().getStateCount() > 1) {
            setResult();
            super.onBackPressed();
        } else {
            if (this.mOriginalSetPathString == null) {
                return;
            }
            if (this.mAppBridge == null && !this.mLaunchFromCamera) {
                Bundle bundle = new Bundle(getData());
                bundle.putString("media-path", this.mOriginalSetPathString);
                bundle.putString("parent-media-path", this.mActivity.getDataManager().getTopSetPath(3));
                this.mActivity.getStateManager().switchState(this, AlbumPage.class, bundle);
                return;
            }
            GalleryUtils.startGalleryActivity(this.mActivity);
        }
    }

    private void setResult() {
        Intent intent = new Intent();
        intent.putExtra("return-index-hint", this.mCurrentIndex);
        setStateResult(-1, intent);
    }

    public boolean switchWithCaptureAnimation(int i) {
        return this.mPhotoView.switchWithCaptureAnimation(i);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        this.mActionBar.createActionBarMenu(R.menu.photo, menu);
        this.mActionBarExts = (IActionBar[]) FeatureManager.getInstance().getImplement(IActionBar.class, this.mActivity);
        for (IActionBar iActionBar : this.mActionBarExts) {
            iActionBar.onCreateOptionsMenu(this.mActivity.getActionBar(), menu);
        }
        this.mPhotoView.onCreateOptionsMenu(menu);
        this.mHaveImageEditor = GalleryUtils.isEditorAvailable(this.mActivity, "image/*");
        updateMenuOperations();
        updateActionBarTitle();
        updateUIForCurrentPhoto();
        return true;
    }

    private void switchToGrid() {
        if (this.mActivity.getStateManager().hasStateClass(AlbumPage.class)) {
            onUpPressed();
            return;
        }
        if (this.mOriginalSetPathString == null) {
            return;
        }
        Bundle bundle = new Bundle(getData());
        bundle.putString("media-path", this.mOriginalSetPathString);
        bundle.putString("parent-media-path", this.mActivity.getDataManager().getTopSetPath(3));
        bundle.putBoolean("cluster-menu", !this.mActivity.getStateManager().hasStateClass(AlbumPage.class) && this.mAppBridge == null);
        bundle.putBoolean("app-bridge", this.mAppBridge != null);
        this.mActivity.getTransitionStore().put("return-index-hint", Integer.valueOf(this.mAppBridge != null ? this.mCurrentIndex - 1 : this.mCurrentIndex));
        if (this.mHasCameraScreennailOrPlaceholder && this.mAppBridge != null) {
            this.mActivity.getStateManager().startState(AlbumPage.class, bundle);
        } else {
            this.mActivity.getStateManager().switchState(this, AlbumPage.class, bundle);
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem menuItem) {
        int i;
        if (this.mModel == null) {
            return true;
        }
        refreshHidingMessage();
        MediaItem mediaItem = this.mModel.getMediaItem(0);
        if ((mediaItem instanceof SnailItem) || mediaItem == null) {
            return true;
        }
        int currentIndex = this.mModel.getCurrentIndex();
        Path path = mediaItem.getPath();
        DataManager dataManager = this.mActivity.getDataManager();
        int itemId = menuItem.getItemId();
        if (itemId != 16908332 && !this.mLoadingFinished && this.mSetPathString != null) {
            Toast.makeText(this.mActivity, this.mActivity.getString(R.string.please_wait), 0).show();
            return true;
        }
        for (IActionBar iActionBar : this.mActionBarExts) {
            if (iActionBar.onOptionsItemSelected(menuItem, mediaItem.getMediaData())) {
                return true;
            }
        }
        String quantityString = null;
        if (itemId == 16908332) {
            onUpPressed();
            return true;
        }
        if (itemId == R.id.action_slideshow) {
            Bundle bundle = new Bundle();
            String string = this.mMediaSet.getPath().toString();
            if (this.mSnailSetPath != null) {
                string = string.replace(this.mSnailSetPath + ",", "");
                com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> action_slideshow | mediaSetPath: " + string);
            }
            bundle.putString("media-set-path", string);
            bundle.putString("media-item-path", path.toString());
            if (this.mHasCameraScreennailOrPlaceholder) {
                currentIndex--;
            }
            bundle.putInt("photo-index", currentIndex);
            bundle.putBoolean("repeat", true);
            this.mActivity.getStateManager().startStateForResult(SlideshowPage.class, 1, bundle);
            return true;
        }
        switch (itemId) {
            case R.id.action_delete:
                quantityString = this.mActivity.getResources().getQuantityString(R.plurals.delete_selection, 1);
                break;
            case R.id.action_edit:
                File file = new File(mediaItem.getFilePath());
                if (!file.exists()) {
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> abort editing photo when not exists!");
                    return true;
                }
                if (!isSpaceEnough(file)) {
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> abort editing photo when no enough space!");
                    Toast.makeText(this.mActivity, this.mActivity.getString(R.string.storage_not_enough), 0).show();
                    return true;
                }
                launchPhotoEditor();
                return true;
            case R.id.action_rotate_ccw:
            case R.id.action_rotate_cw:
            case R.id.action_setas:
            case R.id.action_show_on_map:
                break;
            case R.id.action_crop:
                File file2 = new File(mediaItem.getFilePath());
                if (!file2.exists()) {
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> abort cropping photo when not exists!");
                    return true;
                }
                if (!isSpaceEnough(file2)) {
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> abort cropping photo when no enough space!");
                    Toast.makeText(this.mActivity, this.mActivity.getString(R.string.storage_not_enough), 0).show();
                    return true;
                }
                AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setClass(abstractGalleryActivity, CropActivity.class);
                intent.setDataAndType(dataManager.getContentUri(path), mediaItem.getMimeType()).setFlags(1);
                if (PicasaSource.isPicasaImage(mediaItem)) {
                    i = 3;
                } else {
                    i = 2;
                }
                abstractGalleryActivity.startActivityForResult(intent, i);
                return true;
            case R.id.action_details:
                if (this.mShowDetails) {
                    hideDetails();
                } else {
                    showDetails();
                }
                return true;
            case R.id.action_simple_edit:
                launchSimpleEditor();
                return true;
            case R.id.action_trim:
                Intent intent2 = new Intent(this.mActivity, (Class<?>) TrimVideo.class);
                intent2.setData(dataManager.getContentUri(path));
                intent2.putExtra("media-item-path", mediaItem.getFilePath());
                this.mActivity.startActivityForResult(intent2, 6);
                return true;
            case R.id.action_mute:
                File file3 = new File(mediaItem.getFilePath());
                if (!file3.exists()) {
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> abort muting video when not exists!");
                    return true;
                }
                if (!isSpaceEnough(file3)) {
                    com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onItemSelected> abort muting video when no enough space!");
                    Toast.makeText(this.mActivity, this.mActivity.getString(R.string.storage_not_enough), 0).show();
                    return true;
                }
                this.mMuteVideo = new MuteVideo(mediaItem.getFilePath(), dataManager.getContentUri(path), this.mActivity);
                this.mMuteVideo.muteInBackground();
                return true;
            case R.id.print:
                this.mActivity.printSelectedImage(dataManager.getContentUri(path));
                return true;
            default:
                return this.mPhotoView.onOptionsItemSelected(menuItem);
        }
        this.mSelectionManager.deSelectAll();
        this.mSelectionManager.toggle(path);
        this.mMenuExecutor.onMenuClicked(menuItem, quantityString, this.mConfirmDialogListener);
        return true;
    }

    private void hideDetails() {
        this.mShowDetails = false;
        this.mDetailsHelper.hide();
    }

    private void showDetails() {
        this.mShowDetails = true;
        if (this.mDetailsHelper == null) {
            this.mDetailsHelper = new DetailsHelper(this.mActivity, this.mRootPane, new MyDetailsSource());
            this.mDetailsHelper.setCloseListener(new DetailsHelper.CloseListener() {
                @Override
                public void onClose() {
                    PhotoPage.this.hideDetails();
                }
            });
        }
        this.mDetailsHelper.show();
    }

    @Override
    public void onSingleTapConfirmed(int i, int i2) {
        MediaItem mediaItem;
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onSingleTapConfirmed>");
        if ((this.mAppBridge != null && this.mAppBridge.onSingleTapUp(i, i2)) || (mediaItem = this.mModel.getMediaItem(0)) == null || mediaItem == this.mScreenNailItem) {
            return;
        }
        int supportedOperations = mediaItem.getSupportedOperations();
        boolean z = (supportedOperations & 4096) != 0;
        boolean z2 = (supportedOperations & 8192) != 0;
        boolean z3 = (supportedOperations & 32768) != 0;
        if (this.mPlaySecureVideo) {
            z = true;
        }
        if (z2) {
            onActionBarAllowed(false);
            onBackPressed();
            return;
        }
        if (z) {
            this.mPlaySecureVideo = false;
            Intent intent = new Intent(this.mActivity, (Class<?>) GalleryActivity.class);
            intent.setFlags(335577088);
            this.mActivity.startActivity(intent);
            return;
        }
        if (z3) {
            launchCamera();
        } else if (!this.mIsStartingVideoPlayer) {
            toggleBars();
        }
    }

    @Override
    public void onActionBarAllowed(boolean z) {
        this.mActionBarAllowed = z;
        this.mHandler.sendEmptyMessage(5);
    }

    @Override
    public void onActionBarWanted() {
        this.mHandler.sendEmptyMessage(7);
    }

    @Override
    public void onFullScreenChanged(boolean z) {
        this.mHandler.obtainMessage(4, z ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void onDeleteImage(Path path, int i) {
        onCommitDeleteImage();
        this.mDeletePath = path;
        this.mDeleteIsFocus = i == 0;
        this.mMediaSet.addDeletion(path, this.mModel.getCurrentIndex() + i);
    }

    @Override
    public void onUndoDeleteImage() {
        if (this.mDeletePath == null) {
            return;
        }
        if (this.mDeleteIsFocus) {
            this.mModel.setFocusHintPath(this.mDeletePath);
        }
        this.mMediaSet.removeDeletion(this.mDeletePath);
        this.mDeletePath = null;
    }

    @Override
    public void onCommitDeleteImage() {
        if (this.mDeletePath == null) {
            return;
        }
        this.mMenuExecutor.startSingleItemAction(R.id.action_delete, this.mDeletePath);
        this.mDeletePath = null;
    }

    public void playVideo(Activity activity, Uri uri, String str) {
        try {
            Intent intentPutExtra = new Intent("android.intent.action.VIEW").setDataAndType(uri, "video/*").putExtra("android.intent.extra.TITLE", str).putExtra("treat-up-as-back", true);
            intentPutExtra.putExtra("mediatek.intent.extra.ENABLE_VIDEO_LIST", true);
            activity.startActivityForResult(intentPutExtra, 5);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err), 0).show();
        }
    }

    @Override
    protected void onStateResult(int i, int i2, Intent intent) {
        for (IBottomControl iBottomControl : this.mBottomControlExts) {
            iBottomControl.onActivityResult(i, i2, intent);
        }
        this.mIsStartingVideoPlayer = false;
        if (i == 4) {
            this.mActivity.getDataManager().broadcastUpdatePicture();
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onStateResult> send broadcast to camera to update thumbnail");
        }
        if (i2 == 0 && i != 5) {
        }
        this.mRecenterCameraOnResume = false;
        switch (i) {
            case 1:
                if (intent != null) {
                    String stringExtra = intent.getStringExtra("media-item-path");
                    int intExtra = intent.getIntExtra("photo-index", 0);
                    if (stringExtra != null) {
                        this.mModel.setCurrentPhoto(Path.fromString(stringExtra), intExtra);
                    }
                    break;
                }
                break;
            case 2:
                if (i2 == -1) {
                    setCurrentPhotoByIntentEx(intent);
                }
                break;
            case 3:
                if (i2 == -1) {
                    Context androidContext = this.mActivity.getAndroidContext();
                    Toast.makeText(androidContext, androidContext.getString(R.string.crop_saved, androidContext.getString(R.string.folder_edited_online_photos)), 0).show();
                }
                break;
            case 4:
                setCurrentPhotoByIntentEx(intent);
                break;
            default:
                if (intent != null) {
                    redirectCurrentMedia(intent.getData(), true);
                }
                break;
        }
    }

    @Override
    public void onPause() {
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onPause> begin");
        if (this.mModel != null) {
            this.mNotSetActionBarVisibiltyWhenResume = this.mModel.isCamera(0) && !this.mPhotoView.getFilmMode();
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onPause> mNotSetActionBarVisibiltyWhenResume = " + this.mNotSetActionBarVisibiltyWhenResume);
        }
        super.onPause();
        this.mIsActive = false;
        this.mActivity.getGLRoot().unfreeze();
        this.mHandler.removeMessages(6);
        DetailsHelper.pause();
        if (this.mShowDetails) {
            hideDetails();
        }
        if (this.mModel != null) {
            this.mModel.pause();
        }
        this.mPhotoView.pause();
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(8);
        refreshBottomControlsWhenReady();
        this.mActionBar.removeOnMenuVisibilityListener(this.mMenuVisibilityListener);
        if (this.mShowSpinner) {
            this.mActionBar.disableAlbumModeMenu(false);
        }
        this.mActionBar.resetOnChooseActivityListener();
        onCommitDeleteImage();
        this.mMenuExecutor.pause();
        if (this.mMediaSet != null) {
            this.mMediaSet.clearDeletion();
            this.mMediaSet.resetDeletion();
        }
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onPause> end");
    }

    @Override
    public void onCurrentImageUpdated() {
        this.mActivity.getGLRoot().unfreeze();
        updateMenuOperationWhenLoadingFail();
    }

    @Override
    public void onFilmModeChanged(boolean z) {
        refreshBottomControlsWhenReady();
        if (this.mShowSpinner) {
            if (z) {
                this.mActionBar.enableAlbumModeMenu(0, this);
            } else {
                this.mActionBar.disableAlbumModeMenu(true);
            }
        }
        if (z) {
            this.mHandler.removeMessages(1);
            UsageStatistics.onContentViewChanged("Gallery", "FilmstripPage");
        } else {
            refreshHidingMessage();
            if (this.mAppBridge == null || this.mCurrentIndex > 0) {
                UsageStatistics.onContentViewChanged("Gallery", "SinglePhotoPage");
            } else {
                UsageStatistics.onContentViewChanged("Camera", "Unknown");
            }
        }
        updateActionBarTitle();
    }

    private void transitionFromAlbumPageIfNeeded() {
        TransitionStore transitionStore = this.mActivity.getTransitionStore();
        int iIntValue = ((Integer) transitionStore.get("albumpage-transition", 0)).intValue();
        if (iIntValue == 0 && this.mAppBridge != null && this.mRecenterCameraOnResume) {
            this.mCurrentIndex = 0;
            this.mPhotoView.resetToFirstPicture();
        } else {
            int iIntValue2 = ((Integer) transitionStore.get("index-hint", -1)).intValue();
            if (iIntValue2 >= 0) {
                if (this.mHasCameraScreennailOrPlaceholder) {
                    iIntValue2++;
                }
                if (iIntValue2 < this.mMediaSet.getMediaItemCount()) {
                    this.mCurrentIndex = iIntValue2;
                    this.mModel.moveTo(this.mCurrentIndex);
                }
            }
        }
        if (iIntValue == 2) {
            this.mPhotoView.setFilmMode(this.mStartInFilmstrip || this.mAppBridge != null);
        } else if (iIntValue == 4) {
            this.mPhotoView.setFilmMode(false);
        }
    }

    @Override
    protected void onResume() {
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onResume> begin");
        super.onResume();
        if (this.mModel == null) {
            this.mPhotoView.pause();
            this.mActivity.getStateManager().finishState(this);
            return;
        }
        transitionFromAlbumPageIfNeeded();
        this.mIsActive = true;
        setContentPane(this.mRootPane);
        this.mModel.resume();
        this.mPhotoView.resume();
        this.mActionBar.addOnMenuVisibilityListener(this.mMenuVisibilityListener);
        updateActionBarTitle();
        if (this.mShowSpinner && this.mPhotoView.getFilmMode()) {
            this.mActionBar.enableAlbumModeMenu(0, this);
        }
        if (!this.mShowBars) {
            this.mActionBar.hide();
            if (this.mAppBridge == null || this.mCurrentIndex != 0 || this.mPhotoView.getFilmMode()) {
                this.mActivity.getGLRoot().setLightsOutMode(true);
            } else {
                this.mActivity.getGLRoot().setLightsOutMode(false);
            }
        }
        boolean zIsEditorAvailable = GalleryUtils.isEditorAvailable(this.mActivity, "image/*");
        if (zIsEditorAvailable != this.mHaveImageEditor) {
            this.mHaveImageEditor = zIsEditorAvailable;
            updateMenuOperations();
        }
        this.mRecenterCameraOnResume = true;
        this.mHandler.sendEmptyMessageDelayed(6, 250L);
        refreshHidingMessage();
        setOnSystemUiVisibilityChangeListener();
        updateUIForCurrentPhoto();
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onResume> end");
    }

    @Override
    protected void onDestroy() {
        if (this.mAppBridge != null) {
            this.mAppBridge.setServer(null);
            this.mScreenNailItem.setScreenNail(null);
            this.mAppBridge.detachScreenNail();
            this.mAppBridge = null;
            this.mScreenNailSet = null;
            this.mScreenNailItem = null;
        }
        this.mActivity.getGLRoot().setOrientationSource(null);
        if (this.mBottomControls != null) {
            this.mBottomControls.cleanup();
        }
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
        if (this.mSecureAlbum != null) {
            this.mActivity.unregisterReceiver(this.mScreenOffReceiver);
        }
        this.mPhotoView.destroy();
        this.mNfcPushUris[0] = null;
        if (this.mMuteVideo != null) {
            this.mMuteVideo.cancelMute();
        }
        if (this.mShowSpinner) {
            this.mActionBar.disableAlbumModeMenu(true);
        }
        super.onDestroy();
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private MyDetailsSource() {
        }

        @Override
        public MediaDetails getDetails() {
            return PhotoPage.this.mModel.getMediaItem(0).getDetails();
        }

        @Override
        public int size() {
            if (PhotoPage.this.mMediaSet == null) {
                return 1;
            }
            if (!PhotoPage.this.mHasCameraScreennailOrPlaceholder) {
                return PhotoPage.this.mMediaSet.getMediaItemCount();
            }
            return PhotoPage.this.mMediaSet.getMediaItemCount() - 1;
        }

        @Override
        public int setIndex() {
            if (PhotoPage.this.mHasCameraScreennailOrPlaceholder) {
                return PhotoPage.this.mModel.getCurrentIndex() - 1;
            }
            return PhotoPage.this.mModel.getCurrentIndex();
        }
    }

    @Override
    public void onAlbumModeSelected(int i) {
        if (i == 1) {
            switchToGrid();
        }
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (this.mBottomControls == null) {
            return;
        }
        MediaItem mediaItem = this.mCurrentPhoto;
        if (mediaItem == null) {
            this.mHandler.obtainMessage(8, 0, 0, mediaItem).sendToTarget();
        } else {
            mediaItem.getPanoramaSupport(this.mRefreshBottomControlsCallback);
        }
    }

    private void updatePanoramaUI(boolean z) {
        MenuItem menuItemFindItem;
        Menu menu = this.mActionBar.getMenu();
        if (menu == null) {
            return;
        }
        MenuExecutor.updateMenuForPanorama(menu, z, z);
        if (z) {
            MenuItem menuItemFindItem2 = menu.findItem(R.id.action_share);
            if (menuItemFindItem2 != null) {
                menuItemFindItem2.setShowAsAction(0);
                menuItemFindItem2.setTitle(this.mActivity.getResources().getString(R.string.share_as_photo));
                return;
            }
            return;
        }
        if ((this.mCurrentPhoto.getSupportedOperations() & 4) != 0 && (menuItemFindItem = menu.findItem(R.id.action_share)) != null) {
            menuItemFindItem.setShowAsAction(1);
            menuItemFindItem.setTitle(this.mActivity.getResources().getString(R.string.share));
        }
    }

    @Override
    public void onUndoBarVisibilityChanged(boolean z) {
        refreshBottomControlsWhenReady();
    }

    private static String getMediaTypeString(MediaItem mediaItem) {
        if (mediaItem.getMediaType() == 4) {
            return "Video";
        }
        if (mediaItem.getMediaType() == 2) {
            return "Photo";
        }
        return "Unknown:" + mediaItem.getMediaType();
    }

    private void updateScaleGesture() {
        if (this.mCurrentPhoto == null) {
            return;
        }
        if (this.mCurrentPhoto.getMediaType() == 4) {
            this.mPhotoView.setScalingEnabled(false);
            return;
        }
        if (this.mLaunchFromCamera && this.mCurrentPhoto != null && (this.mCurrentPhoto.getSupportedOperations() & 8192) != 0) {
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<updateScaleGesture> setScalingEnabled(false)");
            this.mPhotoView.setScalingEnabled(false);
        } else {
            this.mPhotoView.setScalingEnabled(true);
        }
    }

    private void updateMenuOperationWhenLoadingFail() {
        if (this.mModel != null && 2 == this.mModel.getLoadingState(0)) {
            MenuExecutor.updateMenuOperation(this.mActionBar.getMenu(), this.mSupportedOperations & (-131627));
        }
    }

    @Override
    protected void onSaveState(Bundle bundle) {
        Path path;
        this.mData.putInt("index-hint", this.mCurrentIndex);
        if (this.mCurrentPhoto != null && (path = this.mCurrentPhoto.getPath()) != null) {
            this.mData.putString("media-item-path", path.toString());
        }
    }

    private void redirectCurrentMedia(Uri uri, boolean z) {
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<redirectCurrentMedia> uri=" + uri + ", fromActivity=" + z);
        if (uri == null) {
            com.android.gallery3d.util.Log.e("Gallery2/PhotoPage", "<redirectCurrentMedia> redirect current media, null uri");
            return;
        }
        Intent data = new Intent().setData(uri);
        if (z) {
            setCurrentPhotoByIntentEx(data);
            return;
        }
        Path pathFindPathByUri = this.mApplication.getDataManager().findPathByUri(data.getData(), data.getType());
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<redirectCurrentMedia> type=" + data.getType() + ", path=" + pathFindPathByUri);
        if (pathFindPathByUri != null) {
            this.mData.putString("media-item-path", pathFindPathByUri.toString());
            this.mData.putBoolean("start-in-filmstrip", this.mPhotoView.getFilmMode());
            this.mData.putInt("index-hint", this.mCurrentIndex);
            this.mActivity.getDataManager().broadcastUpdatePicture();
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<redirectCurrentMedia> mSetPathString=" + this.mSetPathString);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    StateManager stateManager = PhotoPage.this.mActivity.getStateManager();
                    if (stateManager.getStateCount() > 0 && stateManager.getTopState() == PhotoPage.this) {
                        PhotoPage.this.mActivity.getStateManager().switchState(PhotoPage.this, SinglePhotoPage.class, PhotoPage.this.mData);
                    }
                }
            });
        }
    }

    private boolean onActionBarVisibilityChange(boolean z) {
        if (this.mIsBackwardToggle) {
            return false;
        }
        if (z) {
            return this.mPhotoView.onActionBarVisibilityChange(z);
        }
        this.mPhotoView.onActionBarVisibilityChange(false);
        return false;
    }

    private long getAvailableSpace(String str) {
        StatFs statFs = new StatFs(str);
        long availableBlocks = ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize());
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<getAvailableSpace> path " + str + ", availableSize(MB) " + ((availableBlocks / 1024) / 1024));
        return availableBlocks;
    }

    private boolean isSpaceEnough(File file) {
        long j;
        if (FeatureConfig.IS_GMO_RAM_OPTIMIZE) {
            j = 9437184;
        } else {
            j = 50331648;
        }
        if (getAvailableSpace(file.getPath()) < file.length() + j) {
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<isSpaceEnough> space is not enough!!!");
            return false;
        }
        return true;
    }

    private void updateActionBarTitle() {
        if (this.mPhotoView == null || this.mActionBar == null) {
            return;
        }
        try {
            if (this.mActivity.getStateManager().getTopState() != this) {
                return;
            }
            boolean z = false;
            if (this.mPhotoView.getFilmMode()) {
                this.mActionBar.setDisplayOptions(this.mSecureAlbum == null && this.mSetPathString != null, false);
                this.mActionBar.setTitle(this.mMediaSet != null ? this.mMediaSet.getName() : "");
                if (this.mShowSpinner) {
                    this.mActionBar.enableAlbumModeMenu(0, this);
                    return;
                }
                return;
            }
            GalleryActionBar galleryActionBar = this.mActionBar;
            if (this.mSecureAlbum == null && this.mSetPathString != null) {
                z = true;
            }
            galleryActionBar.setDisplayOptions(z, true);
            this.mActionBar.setTitle(this.mCurrentPhoto != null ? this.mCurrentPhoto.getName() : "");
        } catch (AssertionError e) {
            com.android.gallery3d.util.Log.v("Gallery2/PhotoPage", "no state in State Manager when updates actionbar title");
        }
    }

    @Override
    public void onSingleTapUp(int i, int i2) {
        MediaItem mediaItem;
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onSingleTapUp>");
        if ((this.mAppBridge != null && this.mAppBridge.onSingleTapUp(i, i2)) || (mediaItem = this.mModel.getMediaItem(0)) == null || mediaItem == this.mScreenNailItem) {
            return;
        }
        boolean z = (mediaItem.getSupportedOperations() & 128) != 0;
        if (z) {
            int width = this.mPhotoView.getWidth();
            int height = this.mPhotoView.getHeight();
            z = Math.abs(i - (width / 2)) * 12 <= width && Math.abs(i2 - (height / 2)) * 12 <= height;
        }
        if (z) {
            if (this.mSecureAlbum == null) {
                this.mIsStartingVideoPlayer = true;
                playVideo(this.mActivity, mediaItem.getPlayUri(), mediaItem.getName());
            } else {
                this.mPlaySecureVideo = true;
            }
        }
    }

    private void setCurrentPhotoByIntentEx(Intent intent) {
        String string;
        if (intent == null) {
            com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<setCurrentPhotoByIntentEx> inetnt is null, return");
            return;
        }
        Path pathFindPathByUri = this.mApplication.getDataManager().findPathByUri(intent.getData(), intent.getType());
        if (pathFindPathByUri != null && (string = pathFindPathByUri.toString()) != null) {
            ImageCacheService.sForceObsoletePath = string;
            this.mModel.setCurrentPhoto(Path.fromString(string), this.mCurrentIndex);
            this.mActivity.getDataManager().broadcastUpdatePicture();
        }
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<setCurrentPhotoByIntentEx> intent.getData()=" + intent.getData());
        Intent intentCreateShareIntent = createShareIntent(intent.getData(), 2);
        if (this.mActionBar != null) {
            this.mActionBar.setShareIntents(null, intentCreateShareIntent, this);
        }
    }

    private static Intent createShareIntent(Uri uri, int i) {
        return new Intent("android.intent.action.SEND").setType(MenuExecutor.getMimeType(i)).putExtra("android.intent.extra.STREAM", uri).addFlags(1);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mCurrentPhoto != null) {
            for (IActionBar iActionBar : this.mActionBarExts) {
                iActionBar.onPrepareOptionsMenu(menu, this.mCurrentPhoto.getMediaData());
            }
        }
        if (this.mSupportedOperations == 0) {
            return super.onPrepareOptionsMenu(menu);
        }
        updateMenuOperationWhenLoadingFail();
        this.mPhotoView.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onChooseActivity(ActivityChooserModelWrapper activityChooserModelWrapper, Intent intent) {
        long jCurrentTimeMillis;
        com.android.gallery3d.util.Log.d("Gallery2/PhotoPage", "<onChooseActivity>");
        if (!this.mLoadingFinished) {
            Toast.makeText(this.mActivity, this.mActivity.getString(R.string.please_wait), 0).show();
            com.android.gallery3d.util.Log.i("Gallery2/PhotoPage", "<onChooseActivity> not finish loading, show toast, return");
            return true;
        }
        long dateInMs = this.mCurrentPhoto.getDateInMs();
        String mediaTypeString = getMediaTypeString(this.mCurrentPhoto);
        if (dateInMs > 0) {
            jCurrentTimeMillis = System.currentTimeMillis() - dateInMs;
        } else {
            jCurrentTimeMillis = -1;
        }
        UsageStatistics.onEvent("Gallery", "Share", mediaTypeString, jCurrentTimeMillis);
        this.mActivity.startActivity(intent);
        return true;
    }

    private void setOnSystemUiVisibilityChangeListener() {
        ((GLRootView) this.mActivity.getGLRoot()).setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                int i2 = PhotoPage.this.mLastSystemUiVis ^ i;
                PhotoPage.this.mLastSystemUiVis = i;
                if ((i2 & 4) != 0 && (i & 4) == 0) {
                    PhotoPage.this.wantBars();
                }
            }
        });
    }

    private void setupBottomControlExtension(Context context) {
        this.mBackwardBottomController = new BackwardBottomController() {
            @Override
            public void refresh(boolean z) {
                if (z) {
                    PhotoPage.this.mBottomControls.hideContainer();
                }
                PhotoPage.this.mBottomControls.refresh();
                PhotoPage.this.mPhotoView.freshLayers(z);
                if (z) {
                    PhotoPage.this.mActivity.invalidateOptionsMenu();
                }
            }

            @Override
            public int addButton(Drawable drawable) {
                return PhotoPage.this.mBottomControls.addButtonToContainer(drawable);
            }
        };
        this.mBottomControlExts = (IBottomControl[]) FeatureManager.getInstance().getImplement(IBottomControl.class, context, context.getResources());
        this.mGalleryRoot = (RelativeLayout) this.mActivity.findViewById(this.mAppBridge != null ? R.id.content : R.id.gallery_root);
        for (IBottomControl iBottomControl : this.mBottomControlExts) {
            iBottomControl.init(this.mGalleryRoot, this.mBackwardBottomController);
        }
    }

    @Override
    public void onBottomControlCreated() {
        for (IBottomControl iBottomControl : this.mBottomControlExts) {
            iBottomControl.onBottomControlCreated();
        }
    }
}
