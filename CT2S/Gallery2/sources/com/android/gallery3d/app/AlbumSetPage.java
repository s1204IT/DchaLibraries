package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.EyePosition;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.settings.GallerySettings;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.HelpUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AlbumSetPage extends ActivityState implements EyePosition.EyePositionListener, GalleryActionBar.ClusterRunner, MediaSet.SyncListener, SelectionManager.SelectionListener {
    private GalleryActionBar mActionBar;
    private ActionModeHandler mActionModeHandler;
    private AlbumSetDataLoader mAlbumSetDataAdapter;
    private AlbumSetSlotRenderer mAlbumSetView;
    private Button mCameraButton;
    private Config.AlbumSetPage mConfig;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private EyePosition mEyePosition;
    private boolean mGetAlbum;
    private boolean mGetContent;
    private Handler mHandler;
    private MediaSet mMediaSet;
    private int mSelectedAction;
    protected SelectionManager mSelectionManager;
    private boolean mShowClusterMenu;
    private boolean mShowDetails;
    private SlotView mSlotView;
    private String mSubtitle;
    private String mTitle;
    private float mX;
    private float mY;
    private float mZ;
    private boolean mIsActive = false;
    private Future<Integer> mSyncTask = null;
    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;
    private boolean mShowedEmptyToastForSelf = false;
    private final GLView mRootPane = new GLView() {
        private final float[] mMatrix = new float[16];

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            AlbumSetPage.this.mEyePosition.resetPosition();
            int slotViewTop = AlbumSetPage.this.mActionBar.getHeight() + AlbumSetPage.this.mConfig.paddingTop;
            int slotViewBottom = (bottom - top) - AlbumSetPage.this.mConfig.paddingBottom;
            int slotViewRight = right - left;
            if (AlbumSetPage.this.mShowDetails) {
                AlbumSetPage.this.mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                AlbumSetPage.this.mAlbumSetView.setHighlightItemPath(null);
            }
            AlbumSetPage.this.mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(2);
            GalleryUtils.setViewPointMatrix(this.mMatrix, (getWidth() / 2) + AlbumSetPage.this.mX, (getHeight() / 2) + AlbumSetPage.this.mY, AlbumSetPage.this.mZ);
            canvas.multiplyMatrix(this.mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }
    };
    WeakReference<Toast> mEmptyAlbumToast = null;

    @Override
    protected int getBackgroundColorId() {
        return R.color.albumset_background;
    }

    @Override
    public void onEyePositionChanged(float x, float y, float z) {
        this.mRootPane.lockRendering();
        this.mX = x;
        this.mY = y;
        this.mZ = z;
        this.mRootPane.unlockRendering();
        this.mRootPane.invalidate();
    }

    @Override
    public void onBackPressed() {
        if (this.mShowDetails) {
            hideDetails();
        } else if (this.mSelectionManager.inSelectionMode()) {
            this.mSelectionManager.leaveSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    private void getSlotCenter(int slotIndex, int[] center) {
        Rect offset = new Rect();
        this.mRootPane.getBoundsOf(this.mSlotView, offset);
        Rect r = this.mSlotView.getSlotRect(slotIndex);
        int scrollX = this.mSlotView.getScrollX();
        int scrollY = this.mSlotView.getScrollY();
        center[0] = (offset.left + ((r.left + r.right) / 2)) - scrollX;
        center[1] = (offset.top + ((r.top + r.bottom) / 2)) - scrollY;
    }

    public void onSingleTapUp(int slotIndex) {
        if (this.mIsActive) {
            if (this.mSelectionManager.inSelectionMode()) {
                MediaSet targetSet = this.mAlbumSetDataAdapter.getMediaSet(slotIndex);
                if (targetSet != null) {
                    this.mSelectionManager.toggle(targetSet.getPath());
                    this.mSlotView.invalidate();
                    return;
                }
                return;
            }
            this.mAlbumSetView.setPressedIndex(slotIndex);
            this.mAlbumSetView.setPressedUp();
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, slotIndex, 0), 180L);
        }
    }

    private static boolean albumShouldOpenInFilmstrip(MediaSet album) {
        int itemCount = album.getMediaItemCount();
        ArrayList<MediaItem> list = itemCount == 1 ? album.getMediaItem(0, 1) : null;
        return (list == null || list.isEmpty()) ? false : true;
    }

    private void showEmptyAlbumToast(int toastLength) {
        Toast toast;
        if (this.mEmptyAlbumToast != null && (toast = this.mEmptyAlbumToast.get()) != null) {
            toast.show();
            return;
        }
        Toast toast2 = Toast.makeText(this.mActivity, R.string.empty_album, toastLength);
        this.mEmptyAlbumToast = new WeakReference<>(toast2);
        toast2.show();
    }

    private void hideEmptyAlbumToast() {
        Toast toast;
        if (this.mEmptyAlbumToast == null || (toast = this.mEmptyAlbumToast.get()) == null) {
            return;
        }
        toast.cancel();
    }

    private void pickAlbum(int slotIndex) {
        MediaSet targetSet;
        if (this.mIsActive && (targetSet = this.mAlbumSetDataAdapter.getMediaSet(slotIndex)) != null) {
            if (targetSet.getTotalMediaItemCount() == 0) {
                showEmptyAlbumToast(0);
                return;
            }
            hideEmptyAlbumToast();
            String mediaPath = targetSet.getPath().toString();
            Bundle data = new Bundle(getData());
            int[] center = new int[2];
            getSlotCenter(slotIndex, center);
            data.putIntArray("set-center", center);
            if (this.mGetAlbum && targetSet.isLeafAlbum()) {
                Activity activity = this.mActivity;
                Intent result = new Intent().putExtra("album-path", targetSet.getPath().toString());
                activity.setResult(-1, result);
                activity.finish();
                return;
            }
            if (targetSet.getSubMediaSetCount() > 0) {
                data.putString("media-path", mediaPath);
                this.mActivity.getStateManager().startStateForResult(AlbumSetPage.class, 1, data);
                return;
            }
            if (!this.mGetContent && albumShouldOpenInFilmstrip(targetSet)) {
                data.putParcelable("open-animation-rect", this.mSlotView.getSlotRect(slotIndex, this.mRootPane));
                data.putInt("index-hint", 0);
                data.putString("media-set-path", mediaPath);
                data.putBoolean("start-in-filmstrip", true);
                data.putBoolean("in_camera_roll", targetSet.isCameraRoll());
                this.mActivity.getStateManager().startStateForResult(FilmstripPage.class, 2, data);
                return;
            }
            data.putString("media-path", mediaPath);
            boolean inAlbum = this.mActivity.getStateManager().hasStateClass(AlbumPage.class);
            data.putBoolean("cluster-menu", inAlbum ? false : true);
            this.mActivity.getStateManager().startStateForResult(AlbumPage.class, 1, data);
        }
    }

    private void onDown(int index) {
        this.mAlbumSetView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            this.mAlbumSetView.setPressedIndex(-1);
        } else {
            this.mAlbumSetView.setPressedUp();
        }
    }

    public void onLongTap(int slotIndex) {
        MediaSet set;
        if (!this.mGetContent && !this.mGetAlbum && (set = this.mAlbumSetDataAdapter.getMediaSet(slotIndex)) != null) {
            this.mSelectionManager.setAutoLeaveSelectionMode(true);
            this.mSelectionManager.toggle(set.getPath());
            this.mSlotView.invalidate();
        }
    }

    @Override
    public void doCluster(int clusterType) {
        String basePath = this.mMediaSet.getPath().toString();
        String newPath = FilterUtils.switchClusterPath(basePath, clusterType);
        Bundle data = new Bundle(getData());
        data.putString("media-path", newPath);
        data.putInt("selected-cluster", clusterType);
        this.mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        initializeViews();
        initializeData(data);
        Context context = this.mActivity.getAndroidContext();
        this.mGetContent = data.getBoolean("get-content", false);
        this.mGetAlbum = data.getBoolean("get-album", false);
        this.mTitle = data.getString("set-title");
        this.mSubtitle = data.getString("set-subtitle");
        this.mEyePosition = new EyePosition(context, this);
        this.mDetailsSource = new MyDetailsSource();
        this.mActionBar = this.mActivity.getGalleryActionBar();
        this.mSelectedAction = data.getInt("selected-cluster", 1);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        AlbumSetPage.this.pickAlbum(message.arg1);
                        return;
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupCameraButton();
        this.mActionModeHandler.destroy();
    }

    private boolean setupCameraButton() {
        RelativeLayout galleryRoot;
        if (!GalleryUtils.isCameraAvailable(this.mActivity) || (galleryRoot = (RelativeLayout) this.mActivity.findViewById(R.id.gallery_root)) == null) {
            return false;
        }
        this.mCameraButton = new Button(this.mActivity);
        this.mCameraButton.setText(R.string.camera_label);
        this.mCameraButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.frame_overlay_gallery_camera, 0, 0);
        this.mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                GalleryUtils.startCameraActivity(AlbumSetPage.this.mActivity);
            }
        });
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(-2, -2);
        lp.addRule(13);
        galleryRoot.addView(this.mCameraButton, lp);
        return true;
    }

    private void cleanupCameraButton() {
        RelativeLayout galleryRoot;
        if (this.mCameraButton != null && (galleryRoot = (RelativeLayout) this.mActivity.findViewById(R.id.gallery_root)) != null) {
            galleryRoot.removeView(this.mCameraButton);
            this.mCameraButton = null;
        }
    }

    private void showCameraButton() {
        if (this.mCameraButton != null || setupCameraButton()) {
            this.mCameraButton.setVisibility(0);
        }
    }

    private void hideCameraButton() {
        if (this.mCameraButton != null) {
            this.mCameraButton.setVisibility(8);
        }
    }

    private void clearLoadingBit(int loadingBit) {
        this.mLoadingBits &= loadingBit ^ (-1);
        if (this.mLoadingBits == 0 && this.mIsActive && this.mAlbumSetDataAdapter.size() == 0) {
            if (this.mActivity.getStateManager().getStateCount() > 1) {
                Intent result = new Intent();
                result.putExtra("empty-album", true);
                setStateResult(-1, result);
                this.mActivity.getStateManager().finishState(this);
                return;
            }
            this.mShowedEmptyToastForSelf = true;
            showEmptyAlbumToast(1);
            this.mSlotView.invalidate();
            showCameraButton();
            return;
        }
        if (this.mShowedEmptyToastForSelf) {
            this.mShowedEmptyToastForSelf = false;
            hideEmptyAlbumToast();
            hideCameraButton();
        }
    }

    private void setLoadingBit(int loadingBit) {
        this.mLoadingBits |= loadingBit;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mIsActive = false;
        this.mAlbumSetDataAdapter.pause();
        this.mAlbumSetView.pause();
        this.mActionModeHandler.pause();
        this.mEyePosition.pause();
        DetailsHelper.pause();
        this.mActionBar.disableClusterMenu(false);
        if (this.mSyncTask != null) {
            this.mSyncTask.cancel();
            this.mSyncTask = null;
            clearLoadingBit(2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIsActive = true;
        setContentPane(this.mRootPane);
        setLoadingBit(1);
        this.mAlbumSetDataAdapter.resume();
        this.mAlbumSetView.resume();
        this.mEyePosition.resume();
        this.mActionModeHandler.resume();
        if (this.mShowClusterMenu) {
            this.mActionBar.enableClusterMenu(this.mSelectedAction, this);
        }
        if (!this.mInitialSynced) {
            setLoadingBit(2);
            this.mSyncTask = this.mMediaSet.requestSync(this);
        }
    }

    private void initializeData(Bundle data) {
        String mediaPath = data.getString("media-path");
        this.mMediaSet = this.mActivity.getDataManager().getMediaSet(mediaPath);
        this.mSelectionManager.setSourceMediaSet(this.mMediaSet);
        this.mAlbumSetDataAdapter = new AlbumSetDataLoader(this.mActivity, this.mMediaSet, NotificationCompat.FLAG_LOCAL_ONLY);
        this.mAlbumSetDataAdapter.setLoadingListener(new MyLoadingListener());
        this.mAlbumSetView.setModel(this.mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        this.mSelectionManager = new SelectionManager(this.mActivity, true);
        this.mSelectionManager.setSelectionListener(this);
        this.mConfig = Config.AlbumSetPage.get(this.mActivity);
        this.mSlotView = new SlotView(this.mActivity, this.mConfig.slotViewSpec);
        this.mAlbumSetView = new AlbumSetSlotRenderer(this.mActivity, this.mSelectionManager, this.mSlotView, this.mConfig.labelSpec, this.mConfig.placeholderColor);
        this.mSlotView.setSlotRenderer(this.mAlbumSetView);
        this.mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumSetPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumSetPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumSetPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumSetPage.this.onLongTap(slotIndex);
            }
        });
        this.mActionModeHandler = new ActionModeHandler(this.mActivity, this.mSelectionManager);
        this.mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return AlbumSetPage.this.onItemSelected(item);
            }
        });
        this.mRootPane.addComponent(this.mSlotView);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        Activity activity = this.mActivity;
        boolean inAlbum = this.mActivity.getStateManager().hasStateClass(AlbumPage.class);
        MenuInflater inflater = getSupportMenuInflater();
        if (this.mGetContent) {
            inflater.inflate(R.menu.pickup, menu);
            int typeBits = this.mData.getInt("type-bits", 1);
            this.mActionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
            return true;
        }
        if (this.mGetAlbum) {
            inflater.inflate(R.menu.pickup, menu);
            this.mActionBar.setTitle(R.string.select_album);
            return true;
        }
        inflater.inflate(R.menu.albumset, menu);
        boolean wasShowingClusterMenu = this.mShowClusterMenu;
        this.mShowClusterMenu = !inAlbum;
        boolean selectAlbums = !inAlbum && this.mActionBar.getClusterTypeAction() == 1;
        MenuItem selectItem = menu.findItem(R.id.action_select);
        selectItem.setTitle(activity.getString(selectAlbums ? R.string.select_album : R.string.select_group));
        MenuItem cameraItem = menu.findItem(R.id.action_camera);
        cameraItem.setVisible(GalleryUtils.isCameraAvailable(activity));
        FilterUtils.setupMenuItems(this.mActionBar, this.mMediaSet.getPath(), false);
        Intent helpIntent = HelpUtils.getHelpIntent(activity);
        MenuItem helpItem = menu.findItem(R.id.action_general_help);
        helpItem.setVisible(helpIntent != null);
        if (helpIntent != null) {
            helpItem.setIntent(helpIntent);
        }
        this.mActionBar.setTitle(this.mTitle);
        this.mActionBar.setSubtitle(this.mSubtitle);
        if (this.mShowClusterMenu != wasShowingClusterMenu) {
            if (this.mShowClusterMenu) {
                this.mActionBar.enableClusterMenu(this.mSelectedAction, this);
                return true;
            }
            this.mActionBar.disableClusterMenu(true);
            return true;
        }
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        Activity activity = this.mActivity;
        switch (item.getItemId()) {
            case R.id.action_camera:
                GalleryUtils.startCameraActivity(activity);
                return true;
            case R.id.action_select:
                this.mSelectionManager.setAutoLeaveSelectionMode(false);
                this.mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_manage_offline:
                Bundle data = new Bundle();
                String mediaPath = this.mActivity.getDataManager().getTopSetPath(3);
                data.putString("media-path", mediaPath);
                this.mActivity.getStateManager().startState(ManageCachePage.class, data);
                return true;
            case R.id.action_sync_picasa_albums:
                PicasaSource.requestSync(activity);
                return true;
            case R.id.action_settings:
                activity.startActivity(new Intent(activity, (Class<?>) GallerySettings.class));
                return true;
            case R.id.action_details:
                if (this.mAlbumSetDataAdapter.size() != 0) {
                    if (this.mShowDetails) {
                        hideDetails();
                        return true;
                    }
                    showDetails();
                    return true;
                }
                Toast.makeText(activity, activity.getText(R.string.no_albums_alert), 0).show();
                return true;
            case R.id.action_cancel:
                activity.setResult(0);
                activity.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getBooleanExtra("empty-album", false)) {
            showEmptyAlbumToast(0);
        }
        switch (requestCode) {
            case 1:
                this.mSlotView.startRisingAnimation();
                break;
        }
    }

    private String getSelectedString() {
        int count = this.mSelectionManager.getSelectedCount();
        int action = this.mActionBar.getClusterTypeAction();
        int string = action == 1 ? R.plurals.number_of_albums_selected : R.plurals.number_of_groups_selected;
        String format = this.mActivity.getResources().getQuantityString(string, count);
        return String.format(format, Integer.valueOf(count));
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case 1:
                this.mActionBar.disableClusterMenu(true);
                this.mActionModeHandler.startActionMode();
                performHapticFeedback(0);
                break;
            case 2:
                this.mActionModeHandler.finishActionMode();
                if (this.mShowClusterMenu) {
                    this.mActionBar.enableClusterMenu(this.mSelectedAction, this);
                }
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
        this.mActionModeHandler.setTitle(getSelectedString());
        this.mActionModeHandler.updateSupportedOperation(path, selected);
    }

    private void hideDetails() {
        this.mShowDetails = false;
        this.mDetailsHelper.hide();
        this.mAlbumSetView.setHighlightItemPath(null);
        this.mSlotView.invalidate();
    }

    private void showDetails() {
        this.mShowDetails = true;
        if (this.mDetailsHelper == null) {
            this.mDetailsHelper = new DetailsHelper(this.mActivity, this.mRootPane, this.mDetailsSource);
            this.mDetailsHelper.setCloseListener(new DetailsHelper.CloseListener() {
                @Override
                public void onClose() {
                    AlbumSetPage.this.hideDetails();
                }
            });
        }
        this.mDetailsHelper.show();
    }

    @Override
    public void onSyncDone(MediaSet mediaSet, final int resultCode) {
        if (resultCode == 2) {
            Log.d("AlbumSetPage", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result=" + resultCode);
        }
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = AlbumSetPage.this.mActivity.getGLRoot();
                root.lockRenderThread();
                try {
                    if (resultCode == 0) {
                        AlbumSetPage.this.mInitialSynced = true;
                    }
                    AlbumSetPage.this.clearLoadingBit(2);
                    if (resultCode == 2 && AlbumSetPage.this.mIsActive) {
                        Log.w("AlbumSetPage", "failed to load album set");
                    }
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

    private class MyLoadingListener implements LoadingListener {
        private MyLoadingListener() {
        }

        @Override
        public void onLoadingStarted() {
            AlbumSetPage.this.setLoadingBit(1);
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            AlbumSetPage.this.clearLoadingBit(1);
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        private MyDetailsSource() {
        }

        @Override
        public int size() {
            return AlbumSetPage.this.mAlbumSetDataAdapter.size();
        }

        @Override
        public int setIndex() {
            Path id = AlbumSetPage.this.mSelectionManager.getSelected(false).get(0);
            this.mIndex = AlbumSetPage.this.mAlbumSetDataAdapter.findSet(id);
            return this.mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            MediaObject item = AlbumSetPage.this.mAlbumSetDataAdapter.getMediaSet(this.mIndex);
            if (item == null) {
                return null;
            }
            AlbumSetPage.this.mAlbumSetView.setHighlightItemPath(item.getPath());
            return item.getDetails();
        }
    }
}
