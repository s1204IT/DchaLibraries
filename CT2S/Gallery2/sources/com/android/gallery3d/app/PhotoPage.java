package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AppBridge;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.PhotoDataAdapter;
import com.android.gallery3d.app.PhotoPageBottomControls;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.ComboAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.FilterDeleteSet;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
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

public abstract class PhotoPage extends ActivityState implements ShareActionProvider.OnShareTargetSelectedListener, AppBridge.Server, GalleryActionBar.OnAlbumModeSelectedListener, PhotoPageBottomControls.Delegate, PhotoView.Listener {
    private GalleryActionBar mActionBar;
    private AppBridge mAppBridge;
    private GalleryApp mApplication;
    private PhotoPageBottomControls mBottomControls;
    private boolean mDeleteIsFocus;
    private Path mDeletePath;
    private DetailsHelper mDetailsHelper;
    private Handler mHandler;
    private boolean mHaveImageEditor;
    private boolean mIsActive;
    private boolean mIsMenuVisible;
    private boolean mIsPanorama;
    private boolean mIsPanorama360;
    private FilterDeleteSet mMediaSet;
    private MenuExecutor mMenuExecutor;
    private Model mModel;
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
    private final MediaObject.PanoramaSupportCallback mUpdatePanoramaMenuItemsCallback = new MediaObject.PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama, boolean isPanorama360) {
            if (mediaObject == PhotoPage.this.mCurrentPhoto) {
                PhotoPage.this.mHandler.obtainMessage(16, isPanorama360 ? 1 : 0, 0, mediaObject).sendToTarget();
            }
        }
    };
    private final MediaObject.PanoramaSupportCallback mRefreshBottomControlsCallback = new MediaObject.PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama, boolean isPanorama360) {
            if (mediaObject == PhotoPage.this.mCurrentPhoto) {
                PhotoPage.this.mHandler.obtainMessage(8, isPanorama ? 1 : 0, isPanorama360 ? 1 : 0, mediaObject).sendToTarget();
            }
        }
    };
    private final MediaObject.PanoramaSupportCallback mUpdateShareURICallback = new MediaObject.PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama, boolean isPanorama360) {
            if (mediaObject == PhotoPage.this.mCurrentPhoto) {
                PhotoPage.this.mHandler.obtainMessage(15, isPanorama360 ? 1 : 0, 0, mediaObject).sendToTarget();
            }
        }
    };
    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            PhotoPage.this.mPhotoView.layout(0, 0, right - left, bottom - top);
            if (PhotoPage.this.mShowDetails) {
                PhotoPage.this.mDetailsHelper.layout(left, PhotoPage.this.mActionBar.getHeight(), right, bottom);
            }
        }
    };
    private MenuExecutor.ProgressListener mConfirmDialogListener = new MenuExecutor.ProgressListener() {
        @Override
        public void onProgressUpdate(int index) {
        }

        @Override
        public void onProgressComplete(int result) {
        }

        @Override
        public void onConfirmDialogShown() {
            PhotoPage.this.mHandler.removeMessages(1);
        }

        @Override
        public void onConfirmDialogDismissed(boolean confirmed) {
            PhotoPage.this.refreshHidingMessage();
        }

        @Override
        public void onProgressStart() {
        }
    };

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
        public void onMenuVisibilityChanged(boolean isVisible) {
            PhotoPage.this.mIsMenuVisible = isVisible;
            PhotoPage.this.refreshHidingMessage();
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.photo_background;
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
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
                switch (message.what) {
                    case 1:
                        PhotoPage.this.hideBars();
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
                    case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                        PhotoPage.this.mActivity.getGLRoot().unfreeze();
                        return;
                    case 7:
                        PhotoPage.this.wantBars();
                        return;
                    case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                        if (PhotoPage.this.mCurrentPhoto == message.obj && PhotoPage.this.mBottomControls != null) {
                            PhotoPage.this.mIsPanorama = message.arg1 == 1;
                            PhotoPage.this.mIsPanorama360 = message.arg2 == 1;
                            PhotoPage.this.mBottomControls.refresh();
                            return;
                        }
                        return;
                    case 9:
                        PhotoPage.this.mSkipUpdateCurrentPhoto = false;
                        boolean stayedOnCamera = false;
                        if (PhotoPage.this.mPhotoView.getFilmMode()) {
                            if (SystemClock.uptimeMillis() >= PhotoPage.this.mCameraSwitchCutoff || PhotoPage.this.mMediaSet.getMediaItemCount() <= 1) {
                                if (PhotoPage.this.mAppBridge != null) {
                                    PhotoPage.this.mPhotoView.setFilmMode(false);
                                }
                                stayedOnCamera = true;
                            } else {
                                PhotoPage.this.mPhotoView.switchToImage(1);
                            }
                        } else {
                            stayedOnCamera = true;
                        }
                        if (stayedOnCamera) {
                            if (PhotoPage.this.mAppBridge != null || PhotoPage.this.mMediaSet.getTotalMediaItemCount() <= 1) {
                                PhotoPage.this.updateBars();
                                PhotoPage.this.updateCurrentPhoto(PhotoPage.this.mModel.getMediaItem(0));
                                return;
                            } else {
                                PhotoPage.this.launchCamera();
                                PhotoPage.this.mPhotoView.switchToImage(1);
                                return;
                            }
                        }
                        return;
                    case 10:
                        if (!PhotoPage.this.mPhotoView.getFilmMode() && PhotoPage.this.mCurrentPhoto != null && (PhotoPage.this.mCurrentPhoto.getSupportedOperations() & 16384) != 0) {
                            PhotoPage.this.mPhotoView.setFilmMode(true);
                            return;
                        }
                        return;
                    case 11:
                        MediaItem photo = PhotoPage.this.mCurrentPhoto;
                        PhotoPage.this.mCurrentPhoto = null;
                        PhotoPage.this.updateCurrentPhoto(photo);
                        return;
                    case 12:
                        PhotoPage.this.updateUIForCurrentPhoto();
                        return;
                    case 14:
                        long nextUpdate = PhotoPage.this.mDeferUpdateUntil - SystemClock.uptimeMillis();
                        if (nextUpdate <= 0) {
                            PhotoPage.this.mDeferredUpdateWaiting = false;
                            PhotoPage.this.updateUIForCurrentPhoto();
                            return;
                        } else {
                            PhotoPage.this.mHandler.sendEmptyMessageDelayed(14, nextUpdate);
                            return;
                        }
                    case 15:
                        if (PhotoPage.this.mCurrentPhoto == message.obj) {
                            boolean isPanorama360 = message.arg1 != 0;
                            Uri contentUri = PhotoPage.this.mCurrentPhoto.getContentUri();
                            Intent panoramaIntent = null;
                            if (isPanorama360) {
                                panoramaIntent = PhotoPage.createSharePanoramaIntent(contentUri);
                            }
                            Intent shareIntent = PhotoPage.createShareIntent(PhotoPage.this.mCurrentPhoto);
                            PhotoPage.this.mActionBar.setShareIntents(panoramaIntent, shareIntent, PhotoPage.this);
                            PhotoPage.this.setNfcBeamPushUri(contentUri);
                            return;
                        }
                        return;
                    case NotificationCompat.FLAG_AUTO_CANCEL:
                        if (PhotoPage.this.mCurrentPhoto == message.obj) {
                            boolean isPanorama3602 = message.arg1 != 0;
                            PhotoPage.this.updatePanoramaUI(isPanorama3602);
                            return;
                        }
                        return;
                }
            }
        };
        this.mSetPathString = data.getString("media-set-path");
        this.mReadOnlyView = data.getBoolean("read-only");
        this.mOriginalSetPathString = this.mSetPathString;
        setupNfcBeamPush();
        String itemPathString = data.getString("media-item-path");
        Path itemPath = itemPathString != null ? Path.fromString(data.getString("media-item-path")) : null;
        this.mTreatBackAsUp = data.getBoolean("treat-back-as-up", false);
        this.mStartInFilmstrip = data.getBoolean("start-in-filmstrip", false);
        boolean inCameraRoll = data.getBoolean("in_camera_roll", false);
        this.mCurrentIndex = data.getInt("index-hint", 0);
        if (this.mSetPathString != null) {
            this.mShowSpinner = true;
            this.mAppBridge = (AppBridge) data.getParcelable("app-bridge");
            if (this.mAppBridge != null) {
                this.mShowBars = false;
                this.mHasCameraScreennailOrPlaceholder = true;
                this.mAppBridge.setServer(this);
                int id = SnailSource.newId();
                Path screenNailSetPath = SnailSource.getSetPath(id);
                Path screenNailItemPath = SnailSource.getItemPath(id);
                this.mScreenNailSet = (SnailAlbum) this.mActivity.getDataManager().getMediaObject(screenNailSetPath);
                this.mScreenNailItem = (SnailItem) this.mActivity.getDataManager().getMediaObject(screenNailItemPath);
                this.mScreenNailItem.setScreenNail(this.mAppBridge.attachScreenNail());
                if (data.getBoolean("show_when_locked", false)) {
                    this.mFlags |= 32;
                }
                if (!this.mSetPathString.equals("/local/all/0")) {
                    if (SecureSource.isSecurePath(this.mSetPathString)) {
                        this.mSecureAlbum = (SecureAlbum) this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
                        this.mShowSpinner = false;
                    }
                    this.mSetPathString = "/filter/empty/{" + this.mSetPathString + "}";
                }
                this.mSetPathString = "/combo/item/{" + screenNailSetPath + "," + this.mSetPathString + "}";
                itemPath = screenNailItemPath;
            } else if (inCameraRoll && GalleryUtils.isCameraAvailable(this.mActivity)) {
                this.mSetPathString = "/combo/item/{/filter/camera_shortcut," + this.mSetPathString + "}";
                this.mCurrentIndex++;
                this.mHasCameraScreennailOrPlaceholder = true;
            }
            MediaSet originalSet = this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
            if (this.mHasCameraScreennailOrPlaceholder && (originalSet instanceof ComboAlbum)) {
                ((ComboAlbum) originalSet).useNameOfChild(1);
            }
            this.mSelectionManager.setSourceMediaSet(originalSet);
            this.mSetPathString = "/filter/delete/{" + this.mSetPathString + "}";
            this.mMediaSet = (FilterDeleteSet) this.mActivity.getDataManager().getMediaSet(this.mSetPathString);
            if (this.mMediaSet == null) {
                Log.w("PhotoPage", "failed to restore " + this.mSetPathString);
            }
            if (itemPath == null) {
                int mediaItemCount = this.mMediaSet.getMediaItemCount();
                if (mediaItemCount > 0) {
                    if (this.mCurrentIndex >= mediaItemCount) {
                        this.mCurrentIndex = 0;
                    }
                    itemPath = this.mMediaSet.getMediaItem(this.mCurrentIndex, 1).get(0).getPath();
                } else {
                    return;
                }
            }
            PhotoDataAdapter pda = new PhotoDataAdapter(this.mActivity, this.mPhotoView, this.mMediaSet, itemPath, this.mCurrentIndex, this.mAppBridge == null ? -1 : 0, this.mAppBridge == null ? false : this.mAppBridge.isPanorama(), this.mAppBridge == null ? false : this.mAppBridge.isStaticCamera());
            this.mModel = pda;
            this.mPhotoView.setModel(this.mModel);
            pda.setDataListener(new PhotoDataAdapter.DataListener() {
                @Override
                public void onPhotoChanged(int index, Path item) {
                    MediaItem photo;
                    int oldIndex = PhotoPage.this.mCurrentIndex;
                    PhotoPage.this.mCurrentIndex = index;
                    if (PhotoPage.this.mHasCameraScreennailOrPlaceholder) {
                        if (PhotoPage.this.mCurrentIndex > 0) {
                            PhotoPage.this.mSkipUpdateCurrentPhoto = false;
                        }
                        if (oldIndex == 0 && PhotoPage.this.mCurrentIndex > 0 && !PhotoPage.this.mPhotoView.getFilmMode()) {
                            PhotoPage.this.mPhotoView.setFilmMode(true);
                            if (PhotoPage.this.mAppBridge != null) {
                                UsageStatistics.onEvent("CameraToFilmstrip", "Swipe", null);
                            }
                        } else if (oldIndex == 2 && PhotoPage.this.mCurrentIndex == 1) {
                            PhotoPage.this.mCameraSwitchCutoff = SystemClock.uptimeMillis() + 300;
                            PhotoPage.this.mPhotoView.stopScrolling();
                        } else if (oldIndex >= 1 && PhotoPage.this.mCurrentIndex == 0) {
                            PhotoPage.this.mPhotoView.setWantPictureCenterCallbacks(true);
                            PhotoPage.this.mSkipUpdateCurrentPhoto = true;
                        }
                    }
                    if (!PhotoPage.this.mSkipUpdateCurrentPhoto) {
                        if (item != null && (photo = PhotoPage.this.mModel.getMediaItem(0)) != null) {
                            PhotoPage.this.updateCurrentPhoto(photo);
                        }
                        PhotoPage.this.updateBars();
                    }
                    PhotoPage.this.refreshHidingMessage();
                }

                @Override
                public void onLoadingFinished(boolean loadingFailed) {
                    if (!PhotoPage.this.mModel.isEmpty()) {
                        MediaItem photo = PhotoPage.this.mModel.getMediaItem(0);
                        if (photo != null) {
                            PhotoPage.this.updateCurrentPhoto(photo);
                            return;
                        }
                        return;
                    }
                    if (PhotoPage.this.mIsActive && PhotoPage.this.mMediaSet.getNumberOfDeletions() == 0) {
                        PhotoPage.this.mActivity.getStateManager().finishState(PhotoPage.this);
                    }
                }

                @Override
                public void onLoadingStarted() {
                }
            });
        } else {
            MediaItem mediaItem = (MediaItem) this.mActivity.getDataManager().getMediaObject(itemPath);
            this.mModel = new SinglePhotoDataAdapter(this.mActivity, this.mPhotoView, mediaItem);
            this.mPhotoView.setModel(this.mModel);
            updateCurrentPhoto(mediaItem);
            this.mShowSpinner = false;
        }
        this.mPhotoView.setFilmMode(this.mStartInFilmstrip && this.mMediaSet.getMediaItemCount() > 1);
        RelativeLayout galleryRoot = (RelativeLayout) this.mActivity.findViewById(this.mAppBridge != null ? R.id.content : R.id.gallery_root);
        if (galleryRoot != null && this.mSecureAlbum == null) {
            this.mBottomControls = new PhotoPageBottomControls(this, this.mActivity, galleryRoot);
        }
        ((GLRootView) this.mActivity.getGLRoot()).setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int diff = PhotoPage.this.mLastSystemUiVis ^ visibility;
                PhotoPage.this.mLastSystemUiVis = visibility;
                if ((diff & 4) != 0 && (visibility & 4) == 0) {
                    PhotoPage.this.showBars();
                }
            }
        });
    }

    @Override
    public void onPictureCenter(boolean isCamera) {
        boolean isCamera2 = isCamera || (this.mHasCameraScreennailOrPlaceholder && this.mAppBridge == null);
        this.mPhotoView.setWantPictureCenterCallbacks(false);
        this.mHandler.removeMessages(9);
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(isCamera2 ? 9 : 10);
    }

    @Override
    public boolean canDisplayBottomControls() {
        return this.mIsActive && !this.mPhotoView.canUndo();
    }

    @Override
    public boolean canDisplayBottomControl(int control) {
        if (this.mCurrentPhoto == null) {
            return false;
        }
        switch (control) {
            case R.id.photopage_bottom_control_edit:
                return this.mHaveImageEditor && this.mShowBars && !this.mReadOnlyView && !this.mPhotoView.getFilmMode() && (this.mCurrentPhoto.getSupportedOperations() & NotificationCompat.FLAG_GROUP_SUMMARY) != 0 && this.mCurrentPhoto.getMediaType() == 2;
            case R.id.photopage_bottom_control_panorama:
                return this.mIsPanorama;
            case R.id.photopage_bottom_control_tiny_planet:
                return this.mHaveImageEditor && this.mShowBars && this.mIsPanorama360 && !this.mPhotoView.getFilmMode();
            default:
                return false;
        }
    }

    @Override
    public void onBottomControlClicked(int control) {
        switch (control) {
            case R.id.photopage_bottom_control_edit:
                launchPhotoEditor();
                break;
            case R.id.photopage_bottom_control_panorama:
                this.mActivity.getPanoramaViewHelper().showPanorama(this.mCurrentPhoto.getContentUri());
                break;
            case R.id.photopage_bottom_control_tiny_planet:
                launchTinyPlanet();
                break;
        }
    }

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private void setupNfcBeamPush() {
        NfcAdapter adapter;
        if (ApiHelper.HAS_SET_BEAM_PUSH_URIS && (adapter = NfcAdapter.getDefaultAdapter(this.mActivity)) != null) {
            adapter.setBeamPushUris(null, this.mActivity);
            adapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent event) {
                    return PhotoPage.this.mNfcPushUris;
                }
            }, this.mActivity);
        }
    }

    private void setNfcBeamPushUri(Uri uri) {
        this.mNfcPushUris[0] = uri;
    }

    private static Intent createShareIntent(MediaObject mediaObject) {
        int type = mediaObject.getMediaType();
        return new Intent("android.intent.action.SEND").setType(MenuExecutor.getMimeType(type)).putExtra("android.intent.extra.STREAM", mediaObject.getContentUri()).addFlags(1);
    }

    private static Intent createSharePanoramaIntent(Uri contentUri) {
        return new Intent("android.intent.action.SEND").setType("application/vnd.google.panorama360+jpg").putExtra("android.intent.extra.STREAM", contentUri).addFlags(1);
    }

    private void overrideTransitionToEditor() {
        this.mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void launchTinyPlanet() {
        MediaItem current = this.mModel.getMediaItem(0);
        Intent intent = new Intent("com.android.camera.action.TINY_PLANET");
        intent.setClass(this.mActivity, FilterShowActivity.class);
        intent.setDataAndType(current.getContentUri(), current.getMimeType()).setFlags(1);
        intent.putExtra("launch-fullscreen", this.mActivity.isFullscreen());
        this.mActivity.startActivityForResult(intent, 4);
        overrideTransitionToEditor();
    }

    private void launchCamera() {
        this.mRecenterCameraOnResume = false;
        GalleryUtils.startCameraActivity(this.mActivity);
    }

    private void launchPhotoEditor() {
        MediaItem current = this.mModel.getMediaItem(0);
        if (current != null && (current.getSupportedOperations() & NotificationCompat.FLAG_GROUP_SUMMARY) != 0) {
            Intent intent = new Intent("action_nextgen_edit");
            intent.setDataAndType(current.getContentUri(), current.getMimeType()).setFlags(1);
            if (this.mActivity.getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
                intent.setAction("android.intent.action.EDIT");
            }
            intent.putExtra("launch-fullscreen", this.mActivity.isFullscreen());
            this.mActivity.startActivityForResult(Intent.createChooser(intent, null), 4);
            overrideTransitionToEditor();
        }
    }

    private void launchSimpleEditor() {
        MediaItem current = this.mModel.getMediaItem(0);
        if (current != null && (current.getSupportedOperations() & NotificationCompat.FLAG_GROUP_SUMMARY) != 0) {
            Intent intent = new Intent("action_simple_edit");
            intent.setDataAndType(current.getContentUri(), current.getMimeType()).setFlags(1);
            if (this.mActivity.getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
                intent.setAction("android.intent.action.EDIT");
            }
            intent.putExtra("launch-fullscreen", this.mActivity.isFullscreen());
            this.mActivity.startActivityForResult(Intent.createChooser(intent, null), 4);
            overrideTransitionToEditor();
        }
    }

    private void requestDeferredUpdate() {
        this.mDeferUpdateUntil = SystemClock.uptimeMillis() + 250;
        if (!this.mDeferredUpdateWaiting) {
            this.mDeferredUpdateWaiting = true;
            this.mHandler.sendEmptyMessageDelayed(14, 250L);
        }
    }

    private void updateUIForCurrentPhoto() {
        if (this.mCurrentPhoto != null) {
            if ((this.mCurrentPhoto.getSupportedOperations() & 16384) != 0 && !this.mPhotoView.getFilmMode()) {
                this.mPhotoView.setWantPictureCenterCallbacks(true);
            }
            updateMenuOperations();
            refreshBottomControlsWhenReady();
            if (this.mShowDetails) {
                this.mDetailsHelper.reloadDetails();
            }
            if (this.mSecureAlbum == null && (this.mCurrentPhoto.getSupportedOperations() & 4) != 0) {
                this.mCurrentPhoto.getPanoramaSupport(this.mUpdateShareURICallback);
            }
        }
    }

    private void updateCurrentPhoto(MediaItem photo) {
        if (this.mCurrentPhoto != photo) {
            this.mCurrentPhoto = photo;
            if (this.mPhotoView.getFilmMode()) {
                requestDeferredUpdate();
            } else {
                updateUIForCurrentPhoto();
            }
        }
    }

    private void updateMenuOperations() {
        Menu menu = this.mActionBar.getMenu();
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.action_slideshow);
            if (item != null) {
                item.setVisible(this.mSecureAlbum == null && canDoSlideShow());
            }
            if (this.mCurrentPhoto != null) {
                int supportedOperations = this.mCurrentPhoto.getSupportedOperations();
                if (this.mReadOnlyView) {
                    supportedOperations ^= NotificationCompat.FLAG_GROUP_SUMMARY;
                }
                if (this.mSecureAlbum != null) {
                    supportedOperations &= 1;
                } else {
                    this.mCurrentPhoto.getPanoramaSupport(this.mUpdatePanoramaMenuItemsCallback);
                    if (!this.mHaveImageEditor) {
                        supportedOperations &= -513;
                    }
                }
                MenuExecutor.updateMenuOperation(menu, supportedOperations);
            }
        }
    }

    private boolean canDoSlideShow() {
        return (this.mMediaSet == null || this.mCurrentPhoto == null || this.mCurrentPhoto.getMediaType() != 2) ? false : true;
    }

    private void showBars() {
        if (!this.mShowBars) {
            this.mShowBars = true;
            this.mOrientationManager.unlockOrientation();
            this.mActionBar.show();
            this.mActivity.getGLRoot().setLightsOutMode(false);
            refreshHidingMessage();
            refreshBottomControlsWhenReady();
        }
    }

    private void hideBars() {
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
        if ((this.mAppBridge != null && this.mCurrentIndex == 0 && !this.mPhotoView.getFilmMode()) || !this.mActionBarAllowed) {
            return false;
        }
        Configuration config = this.mActivity.getResources().getConfiguration();
        return config.touchscreen != 1;
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
    }

    @Override
    protected void onBackPressed() {
        showBars();
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
        if ((this.mStartInFilmstrip || this.mAppBridge != null) && !this.mPhotoView.getFilmMode()) {
            this.mPhotoView.setFilmMode(true);
            return;
        }
        if (this.mActivity.getStateManager().getStateCount() > 1) {
            setResult();
            super.onBackPressed();
        } else if (this.mOriginalSetPathString != null) {
            if (this.mAppBridge == null) {
                Bundle data = new Bundle(getData());
                data.putString("media-path", this.mOriginalSetPathString);
                data.putString("parent-media-path", this.mActivity.getDataManager().getTopSetPath(3));
                this.mActivity.getStateManager().switchState(this, AlbumPage.class, data);
                return;
            }
            GalleryUtils.startGalleryActivity(this.mActivity);
        }
    }

    private void setResult() {
        Intent result = new Intent();
        result.putExtra("return-index-hint", this.mCurrentIndex);
        setStateResult(-1, result);
    }

    public boolean switchWithCaptureAnimation(int offset) {
        return this.mPhotoView.switchWithCaptureAnimation(offset);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        this.mActionBar.createActionBarMenu(R.menu.photo, menu);
        this.mHaveImageEditor = GalleryUtils.isEditorAvailable(this.mActivity, "image/*");
        updateMenuOperations();
        this.mActionBar.setTitle(this.mMediaSet != null ? this.mMediaSet.getName() : "");
        return true;
    }

    private void switchToGrid() {
        if (this.mActivity.getStateManager().hasStateClass(AlbumPage.class)) {
            onUpPressed();
            return;
        }
        if (this.mOriginalSetPathString != null) {
            Bundle data = new Bundle(getData());
            data.putString("media-path", this.mOriginalSetPathString);
            data.putString("parent-media-path", this.mActivity.getDataManager().getTopSetPath(3));
            boolean inAlbum = this.mActivity.getStateManager().hasStateClass(AlbumPage.class);
            data.putBoolean("cluster-menu", !inAlbum && this.mAppBridge == null);
            data.putBoolean("app-bridge", this.mAppBridge != null);
            this.mActivity.getTransitionStore().put("return-index-hint", Integer.valueOf(this.mAppBridge != null ? this.mCurrentIndex - 1 : this.mCurrentIndex));
            if (this.mHasCameraScreennailOrPlaceholder && this.mAppBridge != null) {
                this.mActivity.getStateManager().startState(AlbumPage.class, data);
            } else {
                this.mActivity.getStateManager().switchState(this, AlbumPage.class, data);
            }
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        if (this.mModel == null) {
            return true;
        }
        refreshHidingMessage();
        MediaItem current = this.mModel.getMediaItem(0);
        if (!(current instanceof SnailItem) && current != null) {
            int currentIndex = this.mModel.getCurrentIndex();
            Path path = current.getPath();
            DataManager manager = this.mActivity.getDataManager();
            int action = item.getItemId();
            String confirmMsg = null;
            switch (action) {
                case android.R.id.home:
                    onUpPressed();
                    return true;
                case R.id.action_slideshow:
                    Bundle data = new Bundle();
                    data.putString("media-set-path", this.mMediaSet.getPath().toString());
                    data.putString("media-item-path", path.toString());
                    data.putInt("photo-index", currentIndex);
                    data.putBoolean("repeat", true);
                    this.mActivity.getStateManager().startStateForResult(SlideshowPage.class, 1, data);
                    return true;
                case R.id.action_delete:
                    confirmMsg = this.mActivity.getResources().getQuantityString(R.plurals.delete_selection, 1);
                    break;
                case R.id.action_edit:
                    launchPhotoEditor();
                    return true;
                case R.id.action_rotate_ccw:
                case R.id.action_rotate_cw:
                case R.id.action_setas:
                case R.id.action_show_on_map:
                    break;
                case R.id.action_crop:
                    Activity activity = this.mActivity;
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setClass(activity, CropActivity.class);
                    intent.setDataAndType(manager.getContentUri(path), current.getMimeType()).setFlags(1);
                    activity.startActivityForResult(intent, PicasaSource.isPicasaImage(current) ? 3 : 2);
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
                    intent2.setData(manager.getContentUri(path));
                    intent2.putExtra("media-item-path", current.getFilePath());
                    this.mActivity.startActivityForResult(intent2, 6);
                    return true;
                case R.id.action_mute:
                    MuteVideo muteVideo = new MuteVideo(current.getFilePath(), manager.getContentUri(path), this.mActivity);
                    muteVideo.muteInBackground();
                    return true;
                case R.id.print:
                    this.mActivity.printSelectedImage(manager.getContentUri(path));
                    return true;
                default:
                    return false;
            }
            this.mSelectionManager.deSelectAll();
            this.mSelectionManager.toggle(path);
            this.mMenuExecutor.onMenuClicked(item, confirmMsg, this.mConfirmDialogListener);
            return true;
        }
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

    private static Intent buildShowGifIntent(MediaItem item) {
        Intent gifIntent = new Intent("android.intent.action.VIEW");
        gifIntent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.ShowGifActivity");
        String filePath = item.getContentUri().toString();
        gifIntent.putExtra("gif_url", filePath);
        return gifIntent;
    }

    @Override
    public void onSingleTapUp(int x, int y) {
        MediaItem item;
        if ((this.mAppBridge == null || !this.mAppBridge.onSingleTapUp(x, y)) && (item = this.mModel.getMediaItem(0)) != null && item != this.mScreenNailItem) {
            int supported = item.getSupportedOperations();
            boolean playVideo = (supported & 128) != 0;
            boolean unlock = (supported & FragmentTransaction.TRANSIT_ENTER_MASK) != 0;
            boolean goBack = (supported & FragmentTransaction.TRANSIT_EXIT_MASK) != 0;
            boolean launchCamera = (32768 & supported) != 0;
            if (playVideo) {
                int w = this.mPhotoView.getWidth();
                int h = this.mPhotoView.getHeight();
                playVideo = Math.abs(x - (w / 2)) * 12 <= w && Math.abs(y - (h / 2)) * 12 <= h;
            }
            if (playVideo) {
                if (this.mSecureAlbum == null) {
                    playVideo(this.mActivity, item.getPlayUri(), item.getName());
                    return;
                } else {
                    this.mActivity.getStateManager().finishState(this);
                    return;
                }
            }
            if (goBack) {
                onBackPressed();
                return;
            }
            if (unlock) {
                Intent intent = new Intent(this.mActivity, (Class<?>) GalleryActivity.class);
                intent.putExtra("dismiss-keyguard", true);
                this.mActivity.startActivity(intent);
            } else if (launchCamera) {
                launchCamera();
            } else if (BitmapUtils.isGifPicture(item.getMimeType())) {
                Log.d("PhotoPage", "Display gif picture, mime type: " + item.getMimeType());
                this.mActivity.getAndroidContext().startActivity(buildShowGifIntent(item));
            } else {
                toggleBars();
            }
        }
    }

    @Override
    public void onActionBarAllowed(boolean allowed) {
        this.mActionBarAllowed = allowed;
        this.mHandler.sendEmptyMessage(5);
    }

    @Override
    public void onActionBarWanted() {
        this.mHandler.sendEmptyMessage(7);
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        Message m = this.mHandler.obtainMessage(4, full ? 1 : 0, 0);
        m.sendToTarget();
    }

    @Override
    public void onDeleteImage(Path path, int offset) {
        onCommitDeleteImage();
        this.mDeletePath = path;
        this.mDeleteIsFocus = offset == 0;
        this.mMediaSet.addDeletion(path, this.mCurrentIndex + offset);
    }

    @Override
    public void onUndoDeleteImage() {
        if (this.mDeletePath != null) {
            if (this.mDeleteIsFocus) {
                this.mModel.setFocusHintPath(this.mDeletePath);
            }
            this.mMediaSet.removeDeletion(this.mDeletePath);
            this.mDeletePath = null;
        }
    }

    @Override
    public void onCommitDeleteImage() {
        if (this.mDeletePath != null) {
            this.mMenuExecutor.startSingleItemAction(R.id.action_delete, this.mDeletePath);
            this.mDeletePath = null;
        }
    }

    public void playVideo(Activity activity, Uri uri, String title) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW").setDataAndType(uri, "video/*").putExtra("android.intent.extra.TITLE", title).putExtra("treat-up-as-back", true);
            activity.startActivityForResult(intent, 5);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err), 0).show();
        }
    }

    private void setCurrentPhotoByIntent(Intent intent) {
        Path path;
        Path albumPath;
        if (intent != null && (path = this.mApplication.getDataManager().findPathByUri(intent.getData(), intent.getType())) != null && (albumPath = this.mApplication.getDataManager().getDefaultSetOf(path)) != null) {
            if (!albumPath.equalsIgnoreCase(this.mOriginalSetPathString)) {
                Bundle data = new Bundle(getData());
                data.putString("media-set-path", albumPath.toString());
                data.putString("media-item-path", path.toString());
                this.mActivity.getStateManager().startState(SinglePhotoPage.class, data);
                return;
            }
            this.mModel.setCurrentPhoto(path, this.mCurrentIndex);
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != 0) {
            this.mRecenterCameraOnResume = false;
            switch (requestCode) {
                case 1:
                    if (data != null) {
                        String path = data.getStringExtra("media-item-path");
                        int index = data.getIntExtra("photo-index", 0);
                        if (path != null) {
                            this.mModel.setCurrentPhoto(Path.fromString(path), index);
                        }
                    }
                    break;
                case 2:
                    if (resultCode == -1) {
                        setCurrentPhotoByIntent(data);
                    }
                    break;
                case 3:
                    if (resultCode == -1) {
                        Context context = this.mActivity.getAndroidContext();
                        String message = context.getString(R.string.crop_saved, context.getString(R.string.folder_edited_online_photos));
                        Toast.makeText(context, message, 0).show();
                    }
                    break;
                case 4:
                    setCurrentPhotoByIntent(data);
                    break;
            }
        }
    }

    @Override
    public void onPause() {
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
            this.mActionBar.disableAlbumModeMenu(true);
        }
        onCommitDeleteImage();
        this.mMenuExecutor.pause();
        if (this.mMediaSet != null) {
            this.mMediaSet.clearDeletion();
        }
    }

    @Override
    public void onCurrentImageUpdated() {
        this.mActivity.getGLRoot().unfreeze();
    }

    @Override
    public void onFilmModeChanged(boolean enabled) {
        refreshBottomControlsWhenReady();
        if (this.mShowSpinner) {
            if (enabled) {
                this.mActionBar.enableAlbumModeMenu(0, this);
            } else {
                this.mActionBar.disableAlbumModeMenu(true);
            }
        }
        if (enabled) {
            this.mHandler.removeMessages(1);
            UsageStatistics.onContentViewChanged("Gallery", "FilmstripPage");
            return;
        }
        refreshHidingMessage();
        if (this.mAppBridge == null || this.mCurrentIndex > 0) {
            UsageStatistics.onContentViewChanged("Gallery", "SinglePhotoPage");
        } else {
            UsageStatistics.onContentViewChanged("Camera", "Unknown");
        }
    }

    private void transitionFromAlbumPageIfNeeded() {
        TransitionStore transitions = this.mActivity.getTransitionStore();
        int albumPageTransition = ((Integer) transitions.get("albumpage-transition", 0)).intValue();
        if (albumPageTransition == 0 && this.mAppBridge != null && this.mRecenterCameraOnResume) {
            this.mCurrentIndex = 0;
            this.mPhotoView.resetToFirstPicture();
        } else {
            int resumeIndex = ((Integer) transitions.get("index-hint", -1)).intValue();
            if (resumeIndex >= 0) {
                if (this.mHasCameraScreennailOrPlaceholder) {
                    resumeIndex++;
                }
                if (resumeIndex < this.mMediaSet.getMediaItemCount()) {
                    this.mCurrentIndex = resumeIndex;
                    this.mModel.moveTo(this.mCurrentIndex);
                }
            }
        }
        if (albumPageTransition == 2) {
            this.mPhotoView.setFilmMode(this.mStartInFilmstrip || this.mAppBridge != null);
        } else if (albumPageTransition == 4) {
            this.mPhotoView.setFilmMode(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mModel == null) {
            this.mActivity.getStateManager().finishState(this);
            return;
        }
        transitionFromAlbumPageIfNeeded();
        this.mActivity.getGLRoot().freeze();
        this.mIsActive = true;
        setContentPane(this.mRootPane);
        this.mModel.resume();
        this.mPhotoView.resume();
        this.mActionBar.setDisplayOptions(this.mSecureAlbum == null && this.mSetPathString != null, false);
        this.mActionBar.addOnMenuVisibilityListener(this.mMenuVisibilityListener);
        refreshBottomControlsWhenReady();
        if (this.mShowSpinner && this.mPhotoView.getFilmMode()) {
            this.mActionBar.enableAlbumModeMenu(0, this);
        }
        if (!this.mShowBars) {
            this.mActionBar.hide();
            this.mActivity.getGLRoot().setLightsOutMode(true);
        }
        boolean haveImageEditor = GalleryUtils.isEditorAvailable(this.mActivity, "image/*");
        if (haveImageEditor != this.mHaveImageEditor) {
            this.mHaveImageEditor = haveImageEditor;
            updateMenuOperations();
        }
        this.mRecenterCameraOnResume = true;
        this.mHandler.sendEmptyMessageDelayed(6, 250L);
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
        this.mHandler.removeCallbacksAndMessages(null);
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
            if (PhotoPage.this.mMediaSet != null) {
                return PhotoPage.this.mMediaSet.getMediaItemCount();
            }
            return 1;
        }

        @Override
        public int setIndex() {
            return PhotoPage.this.mModel.getCurrentIndex();
        }
    }

    @Override
    public void onAlbumModeSelected(int mode) {
        if (mode == 1) {
            switchToGrid();
        }
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (this.mBottomControls != null) {
            MediaObject currentPhoto = this.mCurrentPhoto;
            if (currentPhoto == null) {
                this.mHandler.obtainMessage(8, 0, 0, currentPhoto).sendToTarget();
            } else {
                currentPhoto.getPanoramaSupport(this.mRefreshBottomControlsCallback);
            }
        }
    }

    private void updatePanoramaUI(boolean isPanorama360) {
        MenuItem item;
        Menu menu = this.mActionBar.getMenu();
        if (menu != null) {
            MenuExecutor.updateMenuForPanorama(menu, isPanorama360, isPanorama360);
            if (isPanorama360) {
                MenuItem item2 = menu.findItem(R.id.action_share);
                if (item2 != null) {
                    item2.setShowAsAction(0);
                    item2.setTitle(this.mActivity.getResources().getString(R.string.share_as_photo));
                    return;
                }
                return;
            }
            if ((this.mCurrentPhoto.getSupportedOperations() & 4) != 0 && (item = menu.findItem(R.id.action_share)) != null) {
                item.setShowAsAction(1);
                item.setTitle(this.mActivity.getResources().getString(R.string.share));
            }
        }
    }

    @Override
    public void onUndoBarVisibilityChanged(boolean visible) {
        refreshBottomControlsWhenReady();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        long timestampMillis = this.mCurrentPhoto.getDateInMs();
        String mediaType = getMediaTypeString(this.mCurrentPhoto);
        UsageStatistics.onEvent("Gallery", "Share", mediaType, timestampMillis > 0 ? System.currentTimeMillis() - timestampMillis : -1L);
        return false;
    }

    private static String getMediaTypeString(MediaItem item) {
        if (item.getMediaType() == 4) {
            return "Video";
        }
        if (item.getMediaType() == 2) {
            return "Photo";
        }
        return "Unknown:" + item.getMediaType();
    }
}
