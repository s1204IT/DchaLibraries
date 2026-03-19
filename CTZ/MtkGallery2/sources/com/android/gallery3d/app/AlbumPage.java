package com.android.gallery3d.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ClusterAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.util.GalleryPluginUtils;
import com.mediatek.gallery3d.util.PermissionHelper;
import java.util.ArrayList;

public class AlbumPage extends ActivityState implements AbstractGalleryActivity.EjectListener, GalleryActionBar.ClusterRunner, GalleryActionBar.OnAlbumModeSelectedListener, MediaSet.SyncListener, SelectionManager.SelectionListener {
    private ActionModeHandler mActionModeHandler;
    private AlbumDataLoader mAlbumDataAdapter;
    private AlbumSlotRenderer mAlbumView;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private boolean mGetContent;
    private Handler mHandler;
    private boolean mInCameraAndWantQuitOnPause;
    private boolean mInCameraApp;
    private boolean mLaunchedFromPhotoPage;
    private boolean mLoadingFailed;
    private MediaSet mMediaSet;
    private Path mMediaSetPath;
    private String mParentMediaSetString;
    private boolean mRestoreSelectionDone;
    private PhotoFallbackEffect mResumeEffect;
    protected SelectionManager mSelectionManager;
    private boolean mShowClusterMenu;
    private boolean mShowDetails;
    private SlotView mSlotView;
    private int mSyncResult;
    private float mUserDistance;
    private boolean mIsActive = false;
    private int mFocusIndex = 0;
    private Future<Integer> mSyncTask = null;
    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;
    private RelativePosition mOpenCenter = new RelativePosition();
    private PhotoFallbackEffect.PositionProvider mPositionProvider = new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int i) {
            Rect slotRect = AlbumPage.this.mSlotView.getSlotRect(i);
            Rect rectBounds = AlbumPage.this.mSlotView.bounds();
            slotRect.offset(rectBounds.left - AlbumPage.this.mSlotView.getScrollX(), rectBounds.top - AlbumPage.this.mSlotView.getScrollY());
            return slotRect;
        }

        @Override
        public int getItemIndex(Path path) {
            int visibleEnd = AlbumPage.this.mSlotView.getVisibleEnd();
            for (int visibleStart = AlbumPage.this.mSlotView.getVisibleStart(); visibleStart < visibleEnd; visibleStart++) {
                MediaItem mediaItem = AlbumPage.this.mAlbumDataAdapter.get(visibleStart);
                if (mediaItem != null && mediaItem.getPath() == path) {
                    return visibleStart;
                }
            }
            return -1;
        }
    };
    private final GLView mRootPane = new GLView() {
        private final float[] mMatrix = new float[16];

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            int height = AlbumPage.this.mActivity.getGalleryActionBar().getHeight();
            int i5 = i4 - i2;
            int i6 = i3 - i;
            if (AlbumPage.this.mShowDetails) {
                AlbumPage.this.mDetailsHelper.layout(i, height, i3, i4);
            } else {
                AlbumPage.this.mAlbumView.setHighlightItemPath(null);
            }
            AlbumPage.this.mOpenCenter.setReferencePosition(0, height);
            AlbumPage.this.mSlotView.layout(0, height, i6, i5);
            GalleryUtils.setViewPointMatrix(this.mMatrix, i6 / 2, i5 / 2, -AlbumPage.this.mUserDistance);
        }

        @Override
        protected void render(GLCanvas gLCanvas) {
            gLCanvas.save(2);
            gLCanvas.multiplyMatrix(this.mMatrix, 0);
            super.render(gLCanvas);
            if (AlbumPage.this.mResumeEffect != null) {
                if (!AlbumPage.this.mResumeEffect.draw(gLCanvas)) {
                    AlbumPage.this.mResumeEffect = null;
                    AlbumPage.this.mAlbumView.setSlotFilter(null);
                }
                invalidate();
            }
            gLCanvas.restore();
        }
    };
    private boolean mNeedUpdateSelection = false;
    private Toast mWaitToast = null;
    private int mNeedDoClusterType = 0;

    @Override
    protected int getBackgroundColorId() {
        return R.color.album_background;
    }

    @Override
    protected void onBackPressed() {
        if (this.mShowDetails) {
            hideDetails();
            return;
        }
        if (this.mSelectionManager.inSelectionMode()) {
            this.mSelectionManager.leaveSelectionMode();
            return;
        }
        if (this.mLaunchedFromPhotoPage) {
            this.mActivity.getTransitionStore().putIfNotPresent("albumpage-transition", 2);
        }
        if (this.mInCameraApp) {
            super.onBackPressed();
        } else {
            onUpPressed();
        }
    }

    private void onUpPressed() {
        if (this.mInCameraApp) {
            GalleryUtils.startGalleryActivity(this.mActivity);
            return;
        }
        if (this.mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
        } else if (this.mParentMediaSetString != null) {
            Bundle bundle = new Bundle(getData());
            bundle.putString("media-path", this.mParentMediaSetString);
            this.mActivity.getStateManager().switchState(this, AlbumSetPage.class, bundle);
        }
    }

    private void onDown(int i) {
        this.mAlbumView.setPressedIndex(i);
    }

    private void onUp(boolean z) {
        if (z) {
            this.mAlbumView.setPressedIndex(-1);
        } else {
            this.mAlbumView.setPressedUp();
        }
    }

    private void onSingleTapUp(int i) {
        Log.d("Gallery2/AlbumPage", "<onSingleTapUp> slotIndex = " + i);
        if (this.mIsActive) {
            if (GalleryPluginUtils.getGalleryPickerPlugin().onSingleTapUp(this.mSlotView, this.mAlbumDataAdapter.get(i))) {
                Log.d("Gallery2/AlbumPage", "<onSingleTapUp> plugin handled onSingleTapUp, return");
                return;
            }
            if (this.mSelectionManager.inSelectionMode()) {
                MediaItem mediaItem = this.mAlbumDataAdapter.get(i);
                if (mediaItem == null) {
                    return;
                }
                if (this.mRestoreSelectionDone) {
                    if (this.mActionModeHandler != null) {
                        this.mActionModeHandler.closeMenu();
                    }
                    this.mSelectionManager.toggle(mediaItem.getPath());
                    this.mSlotView.invalidate();
                    return;
                }
                if (this.mWaitToast == null) {
                    this.mWaitToast = Toast.makeText(this.mActivity, R.string.wait, 0);
                }
                this.mWaitToast.show();
                return;
            }
            this.mAlbumView.setPressedIndex(i);
            this.mAlbumView.setPressedUp();
            this.mHandler.sendMessage(this.mHandler.obtainMessage(0, i, 0));
        }
    }

    private void pickPhoto(int i) {
        pickPhoto(i, false);
    }

    private void pickPhoto(int i, boolean z) {
        MediaItem mediaItem;
        Log.d("Gallery2/AlbumPage", "<pickPhoto> slotIndex = " + i + ", startInFilmstrip = " + z);
        if (this.mIsActive && (mediaItem = this.mAlbumDataAdapter.get(i)) != null) {
            if (!z) {
                this.mActivity.getGLRoot().setLightsOutMode(true);
            }
            if (this.mGetContent) {
                onGetContent(mediaItem);
                return;
            }
            if (this.mLaunchedFromPhotoPage) {
                TransitionStore transitionStore = this.mActivity.getTransitionStore();
                transitionStore.put("albumpage-transition", 4);
                transitionStore.put("index-hint", Integer.valueOf(i));
                onBackPressed();
                return;
            }
            if (!z && canBePlayed(mediaItem)) {
                Log.d("Gallery2/AlbumPage", "<pickPhoto> item.getName()");
                playVideo(this.mActivity, mediaItem.getPlayUri(), mediaItem.getName());
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putInt("index-hint", i);
            bundle.putParcelable("open-animation-rect", this.mSlotView.getSlotRect(i, this.mRootPane));
            bundle.putString("media-set-path", this.mMediaSetPath.toString());
            bundle.putString("media-item-path", mediaItem.getPath().toString());
            bundle.putInt("albumpage-transition", 1);
            bundle.putBoolean("start-in-filmstrip", z);
            bundle.putBoolean("in_camera_roll", this.mMediaSet.isCameraRoll());
            bundle.putBoolean("isCamera", this.mInCameraApp);
            if (z) {
                this.mActivity.getStateManager().switchState(this, FilmstripPage.class, bundle);
            } else {
                this.mActivity.getStateManager().startStateForResult(SinglePhotoPage.class, 2, bundle);
            }
        }
    }

    private void onGetContent(MediaItem mediaItem) {
        DataManager dataManager = this.mActivity.getDataManager();
        AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
        if (this.mData.getString("crop") != null) {
            Uri contentUri = dataManager.getContentUri(mediaItem.getPath());
            Intent intentPutExtras = new Intent("com.android.camera.action.CROP", contentUri).addFlags(33554432).putExtras(getData());
            if (this.mData.getParcelable("output") == null) {
                intentPutExtras.putExtra("return-data", true);
            }
            Log.d("Gallery2/AlbumPage", "<onGetContent> start CropActivity for extra crop, uri: " + contentUri);
            abstractGalleryActivity.startActivity(intentPutExtras);
            abstractGalleryActivity.finish();
            return;
        }
        abstractGalleryActivity.setResult(-1, new Intent((String) null, mediaItem.getContentUri()).addFlags(1));
        Log.d("Gallery2/AlbumPage", "<onGetContent> return uri: " + mediaItem.getContentUri());
        abstractGalleryActivity.finish();
    }

    public void onLongTap(int i) {
        MediaItem mediaItem;
        if (this.mGetContent || (mediaItem = this.mAlbumDataAdapter.get(i)) == null) {
            return;
        }
        if (this.mActionModeHandler != null) {
            this.mActionModeHandler.closeMenu();
        }
        this.mSelectionManager.setAutoLeaveSelectionMode(true);
        this.mSelectionManager.toggle(mediaItem.getPath());
        this.mSlotView.invalidate();
    }

    @Override
    public void doCluster(int i) {
        if (i == 4 && !PermissionHelper.checkAndRequestForLocationCluster(this.mActivity)) {
            Log.d("Gallery2/AlbumPage", "<doCluster> permission not granted");
            this.mNeedDoClusterType = i;
            return;
        }
        String strNewClusterPath = FilterUtils.newClusterPath(this.mMediaSet.getPath().toString(), i);
        Bundle bundle = new Bundle(getData());
        bundle.putString("media-path", strNewClusterPath);
        if (this.mShowClusterMenu) {
            this.mActivity.getAndroidContext();
            bundle.putString("set-title", this.mMediaSet.getName());
            bundle.putInt("selected-cluster", i);
        }
        this.mActivity.getStateManager().startStateForResult(AlbumSetPage.class, 3, bundle);
    }

    @Override
    protected void onCreate(Bundle bundle, Bundle bundle2) {
        super.onCreate(bundle, bundle2);
        this.mUserDistance = GalleryUtils.meterToPixel(0.3f);
        initializeViews();
        initializeData(bundle);
        this.mGetContent = bundle.getBoolean("get-content", false);
        this.mShowClusterMenu = bundle.getBoolean("cluster-menu", false);
        this.mDetailsSource = new MyDetailsSource();
        this.mActivity.getAndroidContext();
        if (bundle.getBoolean("auto-select-all")) {
            this.mSelectionManager.selectAll();
        }
        this.mLaunchedFromPhotoPage = this.mActivity.getStateManager().hasStateClass(FilmstripPage.class);
        this.mInCameraApp = bundle.getBoolean("isCamera", false);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 0) {
                    AlbumPage.this.pickPhoto(message.arg1);
                    return;
                }
                throw new AssertionError(message.what);
            }
        };
        this.mActionModeHandler = GalleryPluginUtils.getGalleryPickerPlugin().onCreate(this.mActivity, bundle, this.mActionModeHandler, this.mSelectionManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mIsActive = true;
        this.mResumeEffect = (PhotoFallbackEffect) this.mActivity.getTransitionStore().get("resume_animation");
        if (this.mResumeEffect != null) {
            this.mAlbumView.setSlotFilter(this.mResumeEffect);
            this.mResumeEffect.setPositionProvider(this.mPositionProvider);
            this.mResumeEffect.start();
        }
        setContentPane(this.mRootPane);
        setLoadingBit(1);
        this.mLoadingFailed = false;
        if (this.mSelectionManager.inSelectionMode()) {
            this.mNeedUpdateSelection = true;
            this.mRestoreSelectionDone = false;
        } else {
            this.mRestoreSelectionDone = true;
        }
        this.mAlbumDataAdapter.resume();
        this.mAlbumView.resume();
        this.mAlbumView.setPressedIndex(-1);
        this.mActionModeHandler.resume();
        if (!this.mInitialSynced) {
            setLoadingBit(2);
            this.mSyncTask = this.mMediaSet.requestSync(this);
        }
        this.mInCameraAndWantQuitOnPause = this.mInCameraApp;
        this.mActivity.setEjectListener(this);
        GalleryPluginUtils.getGalleryPickerPlugin().onResume(this.mSelectionManager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mIsActive = false;
        this.mAlbumView.setSlotFilter(null);
        this.mActionModeHandler.pause();
        this.mAlbumDataAdapter.pause();
        this.mAlbumView.pause();
        DetailsHelper.pause();
        this.mActivity.getGalleryActionBar().removeAlbumModeListener();
        if (this.mSyncTask != null) {
            this.mSyncTask.cancel();
            this.mSyncTask = null;
            clearLoadingBit(2);
        }
        this.mActivity.setEjectListener(null);
        if (this.mSelectionManager.inSelectionMode()) {
            this.mSelectionManager.saveSelection();
            this.mNeedUpdateSelection = false;
        }
        GalleryPluginUtils.getGalleryPickerPlugin().onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mAlbumDataAdapter != null) {
            this.mAlbumDataAdapter.setLoadingListener(null);
        }
        this.mActionModeHandler.destroy();
        GalleryPluginUtils.getGalleryPickerPlugin().onDestroy();
    }

    private void initializeViews() {
        this.mSelectionManager = new SelectionManager(this.mActivity, false);
        this.mSelectionManager.setSelectionListener(this);
        Config.AlbumPage albumPage = Config.AlbumPage.get(this.mActivity);
        this.mSlotView = new SlotView(this.mActivity, albumPage.slotViewSpec, FancyHelper.isFancyLayoutSupported());
        if (FancyHelper.isFancyLayoutSupported()) {
            this.mSlotView.switchLayout(0);
        }
        this.mAlbumView = new AlbumSlotRenderer(this.mActivity, this.mSlotView, this.mSelectionManager, albumPage.placeholderColor);
        this.mSlotView.setSlotRenderer(this.mAlbumView);
        this.mRootPane.addComponent(this.mSlotView);
        this.mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int i) {
                AlbumPage.this.onDown(i);
            }

            @Override
            public void onUp(boolean z) {
                AlbumPage.this.onUp(z);
            }

            @Override
            public void onSingleTapUp(int i) {
                AlbumPage.this.onSingleTapUp(i);
            }

            @Override
            public void onLongTap(int i) {
                AlbumPage.this.onLongTap(i);
            }
        });
        this.mActionModeHandler = new ActionModeHandler(this.mActivity, this.mSelectionManager);
        this.mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem menuItem) {
                return AlbumPage.this.onItemSelected(menuItem);
            }

            @Override
            public boolean onPopUpItemClicked(int i) {
                return AlbumPage.this.mRestoreSelectionDone;
            }
        });
    }

    private void initializeData(Bundle bundle) {
        this.mMediaSetPath = Path.fromString(bundle.getString("media-path"));
        this.mParentMediaSetString = bundle.getString("parent-media-path");
        this.mMediaSet = this.mActivity.getDataManager().getMediaSet(this.mMediaSetPath);
        if (this.mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", this.mMediaSetPath);
        }
        this.mSelectionManager.setSourceMediaSet(this.mMediaSet);
        this.mAlbumDataAdapter = new AlbumDataLoader(this.mActivity, this.mMediaSet);
        this.mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        this.mAlbumView.setModel(this.mAlbumDataAdapter);
    }

    private void showDetails() {
        this.mShowDetails = true;
        if (this.mDetailsHelper == null) {
            this.mDetailsHelper = new DetailsHelper(this.mActivity, this.mRootPane, this.mDetailsSource);
            this.mDetailsHelper.setCloseListener(new DetailsHelper.CloseListener() {
                @Override
                public void onClose() {
                    AlbumPage.this.hideDetails();
                }
            });
        }
        this.mDetailsHelper.show();
    }

    private void hideDetails() {
        this.mShowDetails = false;
        this.mDetailsHelper.hide();
        this.mAlbumView.setHighlightItemPath(null);
        this.mSlotView.invalidate();
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        GalleryActionBar galleryActionBar = this.mActivity.getGalleryActionBar();
        MenuInflater supportMenuInflater = getSupportMenuInflater();
        boolean z = false;
        galleryActionBar.setDisplayOptions((this.mActivity.getStateManager().getStateCount() > 1) | (this.mParentMediaSetString != null), false);
        if (this.mGetContent) {
            supportMenuInflater.inflate(R.menu.pickup, menu);
            galleryActionBar.setTitle(GalleryUtils.getSelectionModePrompt(this.mData.getInt("type-bits", 1)));
        } else {
            supportMenuInflater.inflate(R.menu.album, menu);
            Log.d("Gallery2/AlbumPage", "<onCreateActionBar> setTitle:" + this.mMediaSet.getName());
            galleryActionBar.setTitle(this.mMediaSet.getName());
            galleryActionBar.enableAlbumModeMenu(1, this);
            FilterUtils.setupMenuItems(galleryActionBar, this.mMediaSetPath, true);
            menu.findItem(R.id.action_group_by).setVisible(this.mShowClusterMenu);
            MenuItem menuItemFindItem = menu.findItem(R.id.action_camera);
            if (MediaSetUtils.isCameraSource(this.mMediaSetPath) && GalleryUtils.isCameraAvailable(this.mActivity)) {
                z = true;
            }
            menuItemFindItem.setVisible(z);
        }
        galleryActionBar.setSubtitle(null);
        GalleryPluginUtils.getGalleryPickerPlugin().onCreateActionBar(menu);
        return true;
    }

    private void prepareAnimationBackToFilmstrip(int i) {
        if (this.mAlbumDataAdapter == null || !this.mAlbumDataAdapter.isActive(i) || this.mAlbumDataAdapter.get(i) == null) {
            return;
        }
        TransitionStore transitionStore = this.mActivity.getTransitionStore();
        transitionStore.put("index-hint", Integer.valueOf(i));
        transitionStore.put("open-animation-rect", this.mSlotView.getSlotRect(i, this.mRootPane));
    }

    private void switchToFilmstrip() {
        if (this.mAlbumDataAdapter.size() < 1) {
            return;
        }
        int visibleStart = this.mSlotView.getVisibleStart();
        prepareAnimationBackToFilmstrip(visibleStart);
        if (this.mLaunchedFromPhotoPage) {
            onBackPressed();
        } else {
            pickPhoto(visibleStart, true);
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem menuItem) {
        if (GalleryPluginUtils.getGalleryPickerPlugin().onItemSelected(menuItem)) {
            return true;
        }
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            onUpPressed();
            return true;
        }
        if (itemId == R.id.action_details) {
            if (this.mShowDetails) {
                hideDetails();
            } else {
                showDetails();
            }
            return true;
        }
        if (itemId == R.id.action_cancel) {
            this.mActivity.getStateManager().finishState(this);
            return true;
        }
        switch (itemId) {
            case R.id.action_camera:
                GalleryUtils.startCameraActivity(this.mActivity);
                return true;
            case R.id.action_slideshow:
                this.mInCameraAndWantQuitOnPause = false;
                Bundle bundle = new Bundle();
                bundle.putString("media-set-path", this.mMediaSetPath.toString());
                bundle.putBoolean("repeat", true);
                this.mActivity.getStateManager().startStateForResult(SlideshowPage.class, 1, bundle);
                return true;
            case R.id.action_select:
                this.mSelectionManager.setAutoLeaveSelectionMode(false);
                this.mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_group_by:
                this.mActivity.getGalleryActionBar().showClusterDialog(this);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onStateResult(int i, int i2, Intent intent) {
        switch (i) {
            case 1:
                if (intent != null) {
                    this.mFocusIndex = intent.getIntExtra("photo-index", 0);
                    this.mSlotView.setCenterIndex(this.mFocusIndex);
                    break;
                }
                break;
            case 2:
                if (intent != null) {
                    this.mFocusIndex = intent.getIntExtra("return-index-hint", 0);
                    this.mSlotView.makeSlotVisible(this.mFocusIndex);
                    break;
                }
                break;
            case 3:
                this.mSlotView.startRisingAnimation();
                break;
        }
    }

    @Override
    public void onSelectionModeChange(int i) {
        switch (i) {
            case 1:
                this.mActionModeHandler.startActionMode();
                performHapticFeedback(0);
                break;
            case 2:
                this.mActionModeHandler.finishActionMode();
                this.mRootPane.invalidate();
                break;
            case 3:
            case 4:
                this.mActionModeHandler.updateSupportedOperation();
                this.mRootPane.invalidate();
                break;
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean z) {
        int selectedCount = this.mSelectionManager.getSelectedCount();
        this.mActionModeHandler.setTitle(String.format(this.mActivity.getResources().getQuantityString(R.plurals.number_of_items_selected, selectedCount), Integer.valueOf(selectedCount)));
        this.mActionModeHandler.updateSupportedOperation(path, z);
    }

    @Override
    public void onSyncDone(MediaSet mediaSet, final int i) {
        Log.d("Gallery2/AlbumPage", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result=" + i);
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot gLRoot = AlbumPage.this.mActivity.getGLRoot();
                gLRoot.lockRenderThread();
                AlbumPage.this.mSyncResult = i;
                try {
                    if (i == 0) {
                        AlbumPage.this.mInitialSynced = true;
                    }
                    AlbumPage.this.clearLoadingBit(2);
                    AlbumPage.this.showSyncErrorIfNecessary(AlbumPage.this.mLoadingFailed);
                } finally {
                    gLRoot.unlockRenderThread();
                }
            }
        });
    }

    private void showSyncErrorIfNecessary(boolean z) {
        if (this.mLoadingBits == 0 && this.mSyncResult == 2 && this.mIsActive) {
            if (z || this.mAlbumDataAdapter.size() == 0) {
                Toast.makeText(this.mActivity, R.string.sync_album_error, 1).show();
            }
        }
    }

    private void setLoadingBit(int i) {
        this.mLoadingBits = i | this.mLoadingBits;
    }

    private void clearLoadingBit(int i) {
        this.mLoadingBits = (~i) & this.mLoadingBits;
        if (this.mLoadingBits == 0 && this.mIsActive && this.mAlbumDataAdapter.size() == 0) {
            Intent intent = new Intent();
            intent.putExtra("empty-album", true);
            setStateResult(-1, intent);
            this.mActivity.getStateManager().finishState(this);
        }
    }

    private class MyLoadingListener implements LoadingListener {
        private MyLoadingListener() {
        }

        @Override
        public void onLoadingStarted() {
            AlbumPage.this.setLoadingBit(1);
            AlbumPage.this.mLoadingFailed = false;
        }

        @Override
        public void onLoadingFinished(boolean z) {
            int mediaItemCount;
            AlbumPage.this.clearLoadingBit(1);
            AlbumPage.this.mLoadingFailed = z;
            AlbumPage.this.showSyncErrorIfNecessary(z);
            boolean zInSelectionMode = AlbumPage.this.mSelectionManager.inSelectionMode();
            boolean z2 = false;
            if (AlbumPage.this.mMediaSet != null) {
                mediaItemCount = AlbumPage.this.mMediaSet.getMediaItemCount();
            } else {
                mediaItemCount = 0;
            }
            Log.d("Gallery2/AlbumPage", "onLoadingFinished: item count=" + mediaItemCount);
            AlbumPage.this.mSelectionManager.onSourceContentChanged();
            if (mediaItemCount > 0 && zInSelectionMode) {
                if (AlbumPage.this.mNeedUpdateSelection) {
                    AlbumPage.this.mNeedUpdateSelection = false;
                    AlbumPage.this.mSelectionManager.restoreSelection();
                    z2 = true;
                }
                AlbumPage.this.mActionModeHandler.updateSupportedOperation();
                AlbumPage.this.mActionModeHandler.updateSelectionMenu();
            }
            if (!z2) {
                AlbumPage.this.mRestoreSelectionDone = true;
            }
            if (!zInSelectionMode && (AlbumPage.this.mMediaSet instanceof ClusterAlbum)) {
                GalleryActionBar galleryActionBar = AlbumPage.this.mActivity.getGalleryActionBar();
                String name = AlbumPage.this.mMediaSet.getName();
                Log.d("Gallery2/AlbumPage", "<onLoadingFinished> setTitle:" + name);
                galleryActionBar.setTitle(name);
                galleryActionBar.notifyDataSetChanged();
            }
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        private MyDetailsSource() {
        }

        @Override
        public int size() {
            return AlbumPage.this.mAlbumDataAdapter.size();
        }

        @Override
        public int setIndex() {
            ArrayList<Path> selected = AlbumPage.this.mSelectionManager.getSelected(false);
            if (selected != null && selected.size() > 0) {
                this.mIndex = AlbumPage.this.mAlbumDataAdapter.findItem(AlbumPage.this.mSelectionManager.getSelected(false).get(0));
                return this.mIndex;
            }
            return -1;
        }

        @Override
        public MediaDetails getDetails() {
            MediaItem mediaItem = AlbumPage.this.mAlbumDataAdapter.get(this.mIndex);
            if (mediaItem != null) {
                AlbumPage.this.mAlbumView.setHighlightItemPath(mediaItem.getPath());
                return mediaItem.getDetails();
            }
            return null;
        }
    }

    @Override
    public void onAlbumModeSelected(int i) {
        if (i == 0) {
            switchToFilmstrip();
        }
    }

    @Override
    public void onSelectionRestoreDone() {
        if (!this.mIsActive) {
            return;
        }
        this.mRestoreSelectionDone = true;
        this.mActionModeHandler.updateSupportedOperation();
        this.mActionModeHandler.updateSelectionMenu();
    }

    @Override
    public void onEjectSdcard() {
        if (this.mSelectionManager.inSelectionMode()) {
            Log.d("Gallery2/AlbumPage", "<onEjectSdcard> leaveSelectionMode");
            this.mSelectionManager.leaveSelectionMode();
        }
    }

    public void playVideo(Activity activity, Uri uri, String str) {
        Log.d("Gallery2/AlbumPage", "<playVideo> enter playVideo");
        try {
            activity.startActivityForResult(new Intent("android.intent.action.VIEW").setDataAndType(uri, "video/*").putExtra("android.intent.extra.TITLE", str).putExtra("treat-up-as-back", true), 5);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err), 0).show();
        }
    }

    private boolean canBePlayed(MediaItem mediaItem) {
        return (mediaItem.getSupportedOperations() & 128) != 0 && 4 == mediaItem.getMediaType();
    }

    @Override
    public void setProviderSensive(boolean z) {
        this.mAlbumDataAdapter.setSourceSensive(z);
    }

    @Override
    public void fakeProviderChange() {
        this.mAlbumDataAdapter.fakeSourceChange();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (PermissionHelper.isAllPermissionsGranted(strArr, iArr)) {
            doCluster(this.mNeedDoClusterType);
        } else {
            PermissionHelper.showDeniedPromptIfNeeded(this.mActivity, "android.permission.ACCESS_FINE_LOCATION");
        }
    }
}
