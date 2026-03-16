package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
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
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
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

public class AlbumPage extends ActivityState implements GalleryActionBar.ClusterRunner, GalleryActionBar.OnAlbumModeSelectedListener, MediaSet.SyncListener, SelectionManager.SelectionListener {
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
        public Rect getPosition(int index) {
            Rect rect = AlbumPage.this.mSlotView.getSlotRect(index);
            Rect bounds = AlbumPage.this.mSlotView.bounds();
            rect.offset(bounds.left - AlbumPage.this.mSlotView.getScrollX(), bounds.top - AlbumPage.this.mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = AlbumPage.this.mSlotView.getVisibleStart();
            int end = AlbumPage.this.mSlotView.getVisibleEnd();
            for (int i = start; i < end; i++) {
                MediaItem item = AlbumPage.this.mAlbumDataAdapter.get(i);
                if (item != null && item.getPath() == path) {
                    return i;
                }
            }
            return -1;
        }
    };
    private final GLView mRootPane = new GLView() {
        private final float[] mMatrix = new float[16];

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int slotViewTop = AlbumPage.this.mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;
            if (AlbumPage.this.mShowDetails) {
                AlbumPage.this.mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                AlbumPage.this.mAlbumView.setHighlightItemPath(null);
            }
            AlbumPage.this.mOpenCenter.setReferencePosition(0, slotViewTop);
            AlbumPage.this.mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
            GalleryUtils.setViewPointMatrix(this.mMatrix, (right - left) / 2, (bottom - top) / 2, -AlbumPage.this.mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(2);
            canvas.multiplyMatrix(this.mMatrix, 0);
            super.render(canvas);
            if (AlbumPage.this.mResumeEffect != null) {
                boolean more = AlbumPage.this.mResumeEffect.draw(canvas);
                if (!more) {
                    AlbumPage.this.mResumeEffect = null;
                    AlbumPage.this.mAlbumView.setSlotFilter(null);
                }
                invalidate();
            }
            canvas.restore();
        }
    };

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
            Bundle data = new Bundle(getData());
            data.putString("media-path", this.mParentMediaSetString);
            this.mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
        }
    }

    private void onDown(int index) {
        this.mAlbumView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            this.mAlbumView.setPressedIndex(-1);
        } else {
            this.mAlbumView.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (this.mIsActive) {
            if (this.mSelectionManager.inSelectionMode()) {
                MediaItem item = this.mAlbumDataAdapter.get(slotIndex);
                if (item != null) {
                    this.mSelectionManager.toggle(item.getPath());
                    this.mSlotView.invalidate();
                    return;
                }
                return;
            }
            this.mAlbumView.setPressedIndex(slotIndex);
            this.mAlbumView.setPressedUp();
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0, slotIndex, 0), 180L);
        }
    }

    private void pickPhoto(int slotIndex) {
        pickPhoto(slotIndex, false);
    }

    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
        if (this.mIsActive) {
            if (!startInFilmstrip) {
                this.mActivity.getGLRoot().setLightsOutMode(true);
            }
            MediaItem item = this.mAlbumDataAdapter.get(slotIndex);
            if (item != null) {
                if (this.mGetContent) {
                    onGetContent(item);
                    return;
                }
                if (this.mLaunchedFromPhotoPage) {
                    TransitionStore transitions = this.mActivity.getTransitionStore();
                    transitions.put("albumpage-transition", 4);
                    transitions.put("index-hint", Integer.valueOf(slotIndex));
                    onBackPressed();
                    return;
                }
                Bundle data = new Bundle();
                data.putInt("index-hint", slotIndex);
                data.putParcelable("open-animation-rect", this.mSlotView.getSlotRect(slotIndex, this.mRootPane));
                data.putString("media-set-path", this.mMediaSetPath.toString());
                data.putString("media-item-path", item.getPath().toString());
                data.putInt("albumpage-transition", 1);
                data.putBoolean("start-in-filmstrip", startInFilmstrip);
                data.putBoolean("in_camera_roll", this.mMediaSet.isCameraRoll());
                if (startInFilmstrip) {
                    this.mActivity.getStateManager().switchState(this, FilmstripPage.class, data);
                } else {
                    this.mActivity.getStateManager().startStateForResult(SinglePhotoPage.class, 2, data);
                }
            }
        }
    }

    private void onGetContent(MediaItem item) {
        DataManager dm = this.mActivity.getDataManager();
        Activity activity = this.mActivity;
        if (this.mData.getString("crop") != null) {
            Uri uri = dm.getContentUri(item.getPath());
            Intent intent = new Intent("com.android.camera.action.CROP", uri).addFlags(33554432).putExtras(getData());
            if (this.mData.getParcelable("output") == null) {
                intent.putExtra("return-data", true);
            }
            activity.startActivity(intent);
            activity.finish();
            return;
        }
        activity.setResult(-1, new Intent((String) null, item.getContentUri()).addFlags(1));
        activity.finish();
    }

    public void onLongTap(int slotIndex) {
        MediaItem item;
        if (!this.mGetContent && (item = this.mAlbumDataAdapter.get(slotIndex)) != null) {
            this.mSelectionManager.setAutoLeaveSelectionMode(true);
            this.mSelectionManager.toggle(item.getPath());
            this.mSlotView.invalidate();
        }
    }

    @Override
    public void doCluster(int clusterType) {
        String basePath = this.mMediaSet.getPath().toString();
        String newPath = FilterUtils.newClusterPath(basePath, clusterType);
        Bundle data = new Bundle(getData());
        data.putString("media-path", newPath);
        if (this.mShowClusterMenu) {
            Context context = this.mActivity.getAndroidContext();
            data.putString("set-title", this.mMediaSet.getName());
            data.putString("set-subtitle", GalleryActionBar.getClusterByTypeString(context, clusterType));
        }
        this.mActivity.getStateManager().startStateForResult(AlbumSetPage.class, 3, data);
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        this.mUserDistance = GalleryUtils.meterToPixel(0.3f);
        initializeViews();
        initializeData(data);
        this.mGetContent = data.getBoolean("get-content", false);
        this.mShowClusterMenu = data.getBoolean("cluster-menu", false);
        this.mDetailsSource = new MyDetailsSource();
        this.mActivity.getAndroidContext();
        if (data.getBoolean("auto-select-all")) {
            this.mSelectionManager.selectAll();
        }
        this.mLaunchedFromPhotoPage = this.mActivity.getStateManager().hasStateClass(FilmstripPage.class);
        this.mInCameraApp = data.getBoolean("app-bridge", false);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        AlbumPage.this.pickPhoto(message.arg1);
                        return;
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };
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
        boolean enableHomeButton = (this.mActivity.getStateManager().getStateCount() > 1) | (this.mParentMediaSetString != null);
        GalleryActionBar actionBar = this.mActivity.getGalleryActionBar();
        actionBar.setDisplayOptions(enableHomeButton, false);
        if (!this.mGetContent) {
            actionBar.enableAlbumModeMenu(1, this);
        }
        setLoadingBit(1);
        this.mLoadingFailed = false;
        this.mAlbumDataAdapter.resume();
        this.mAlbumView.resume();
        this.mAlbumView.setPressedIndex(-1);
        this.mActionModeHandler.resume();
        if (!this.mInitialSynced) {
            setLoadingBit(2);
            this.mSyncTask = this.mMediaSet.requestSync(this);
        }
        this.mInCameraAndWantQuitOnPause = this.mInCameraApp;
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mIsActive = false;
        if (this.mSelectionManager.inSelectionMode()) {
            this.mSelectionManager.leaveSelectionMode();
        }
        this.mAlbumView.setSlotFilter(null);
        this.mActionModeHandler.pause();
        this.mAlbumDataAdapter.pause();
        this.mAlbumView.pause();
        DetailsHelper.pause();
        if (!this.mGetContent) {
            this.mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
        }
        if (this.mSyncTask != null) {
            this.mSyncTask.cancel();
            this.mSyncTask = null;
            clearLoadingBit(2);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mAlbumDataAdapter != null) {
            this.mAlbumDataAdapter.setLoadingListener(null);
        }
        this.mActionModeHandler.destroy();
    }

    private void initializeViews() {
        this.mSelectionManager = new SelectionManager(this.mActivity, false);
        this.mSelectionManager.setSelectionListener(this);
        Config.AlbumPage config = Config.AlbumPage.get(this.mActivity);
        this.mSlotView = new SlotView(this.mActivity, config.slotViewSpec);
        this.mAlbumView = new AlbumSlotRenderer(this.mActivity, this.mSlotView, this.mSelectionManager, config.placeholderColor);
        this.mSlotView.setSlotRenderer(this.mAlbumView);
        this.mRootPane.addComponent(this.mSlotView);
        this.mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumPage.this.onLongTap(slotIndex);
            }
        });
        this.mActionModeHandler = new ActionModeHandler(this.mActivity, this.mSelectionManager);
        this.mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return AlbumPage.this.onItemSelected(item);
            }
        });
    }

    private void initializeData(Bundle data) {
        this.mMediaSetPath = Path.fromString(data.getString("media-path"));
        this.mParentMediaSetString = data.getString("parent-media-path");
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
        GalleryActionBar actionBar = this.mActivity.getGalleryActionBar();
        MenuInflater inflator = getSupportMenuInflater();
        if (this.mGetContent) {
            inflator.inflate(R.menu.pickup, menu);
            int typeBits = this.mData.getInt("type-bits", 1);
            actionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
        } else {
            inflator.inflate(R.menu.album, menu);
            actionBar.setTitle(this.mMediaSet.getName());
            FilterUtils.setupMenuItems(actionBar, this.mMediaSetPath, true);
            menu.findItem(R.id.action_group_by).setVisible(this.mShowClusterMenu);
            menu.findItem(R.id.action_camera).setVisible(MediaSetUtils.isCameraSource(this.mMediaSetPath) && GalleryUtils.isCameraAvailable(this.mActivity));
        }
        actionBar.setSubtitle(null);
        return true;
    }

    private void prepareAnimationBackToFilmstrip(int slotIndex) {
        if (this.mAlbumDataAdapter != null && this.mAlbumDataAdapter.isActive(slotIndex)) {
            MediaItem item = this.mAlbumDataAdapter.get(slotIndex);
            if (item != null) {
                TransitionStore transitions = this.mActivity.getTransitionStore();
                transitions.put("index-hint", Integer.valueOf(slotIndex));
                transitions.put("open-animation-rect", this.mSlotView.getSlotRect(slotIndex, this.mRootPane));
            }
        }
    }

    private void switchToFilmstrip() {
        if (this.mAlbumDataAdapter.size() >= 1) {
            int targetPhoto = this.mSlotView.getVisibleStart();
            prepareAnimationBackToFilmstrip(targetPhoto);
            if (this.mLaunchedFromPhotoPage) {
                onBackPressed();
            } else {
                pickPhoto(targetPhoto, true);
            }
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onUpPressed();
                return true;
            case R.id.action_camera:
                GalleryUtils.startCameraActivity(this.mActivity);
                return true;
            case R.id.action_slideshow:
                this.mInCameraAndWantQuitOnPause = false;
                Bundle data = new Bundle();
                data.putString("media-set-path", this.mMediaSetPath.toString());
                data.putBoolean("repeat", true);
                this.mActivity.getStateManager().startStateForResult(SlideshowPage.class, 1, data);
                return true;
            case R.id.action_select:
                this.mSelectionManager.setAutoLeaveSelectionMode(false);
                this.mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_group_by:
                this.mActivity.getGalleryActionBar().showClusterDialog(this);
                return true;
            case R.id.action_details:
                if (this.mShowDetails) {
                    hideDetails();
                    return true;
                }
                showDetails();
                return true;
            case R.id.action_cancel:
                this.mActivity.getStateManager().finishState(this);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        switch (request) {
            case 1:
                if (data != null) {
                    this.mFocusIndex = data.getIntExtra("photo-index", 0);
                    this.mSlotView.setCenterIndex(this.mFocusIndex);
                }
                break;
            case 2:
                if (data != null) {
                    this.mFocusIndex = data.getIntExtra("return-index-hint", 0);
                    this.mSlotView.makeSlotVisible(this.mFocusIndex);
                }
                break;
            case 3:
                this.mSlotView.startRisingAnimation();
                break;
        }
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case 1:
                this.mActionModeHandler.startActionMode();
                performHapticFeedback(0);
                break;
            case 2:
                this.mActionModeHandler.finishActionMode();
                this.mRootPane.invalidate();
                break;
            case 3:
                this.mActionModeHandler.updateSupportedOperation();
                this.mRootPane.invalidate();
                break;
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        int count = this.mSelectionManager.getSelectedCount();
        String format = this.mActivity.getResources().getQuantityString(R.plurals.number_of_items_selected, count);
        this.mActionModeHandler.setTitle(String.format(format, Integer.valueOf(count)));
        this.mActionModeHandler.updateSupportedOperation(path, selected);
    }

    @Override
    public void onSyncDone(MediaSet mediaSet, final int resultCode) {
        Log.d("AlbumPage", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result=" + resultCode);
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = AlbumPage.this.mActivity.getGLRoot();
                root.lockRenderThread();
                AlbumPage.this.mSyncResult = resultCode;
                try {
                    if (resultCode == 0) {
                        AlbumPage.this.mInitialSynced = true;
                    }
                    AlbumPage.this.clearLoadingBit(2);
                    AlbumPage.this.showSyncErrorIfNecessary(AlbumPage.this.mLoadingFailed);
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

    private void showSyncErrorIfNecessary(boolean loadingFailed) {
        if (this.mLoadingBits == 0 && this.mSyncResult == 2 && this.mIsActive) {
            if (loadingFailed || this.mAlbumDataAdapter.size() == 0) {
                Toast.makeText(this.mActivity, R.string.sync_album_error, 1).show();
            }
        }
    }

    private void setLoadingBit(int loadTaskBit) {
        this.mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        this.mLoadingBits &= loadTaskBit ^ (-1);
        if (this.mLoadingBits == 0 && this.mIsActive && this.mAlbumDataAdapter.size() == 0) {
            Intent result = new Intent();
            result.putExtra("empty-album", true);
            setStateResult(-1, result);
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
        public void onLoadingFinished(boolean loadingFailed) {
            AlbumPage.this.clearLoadingBit(1);
            AlbumPage.this.mLoadingFailed = loadingFailed;
            AlbumPage.this.showSyncErrorIfNecessary(loadingFailed);
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
            Path id = AlbumPage.this.mSelectionManager.getSelected(false).get(0);
            this.mIndex = AlbumPage.this.mAlbumDataAdapter.findItem(id);
            return this.mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            MediaObject item = AlbumPage.this.mAlbumDataAdapter.get(this.mIndex);
            if (item == null) {
                return null;
            }
            AlbumPage.this.mAlbumView.setHighlightItemPath(item.getPath());
            return item.getDetails();
        }
    }

    @Override
    public void onAlbumModeSelected(int mode) {
        if (mode == 0) {
            switchToFilmstrip();
        }
    }
}
