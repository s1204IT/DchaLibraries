package com.android.gallery3d.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.EyePosition;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ClusterAlbumSet;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
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
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.util.PermissionHelper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AlbumSetPage extends ActivityState implements AbstractGalleryActivity.EjectListener, EyePosition.EyePositionListener, GalleryActionBar.ClusterRunner, MediaSet.SyncListener, SelectionManager.SelectionListener {
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
    private boolean mRestoreSelectionDone;
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
    private int mClusterType = -1;
    public boolean mLoadingFinished = false;
    public boolean mInitialized = false;
    private final GLView mRootPane = new GLView() {
        private final float[] mMatrix = new float[16];

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            AlbumSetPage.this.mEyePosition.resetPosition();
            int height = AlbumSetPage.this.mActionBar.getHeight() + AlbumSetPage.this.mConfig.paddingTop;
            int i5 = (i4 - i2) - AlbumSetPage.this.mConfig.paddingBottom;
            int i6 = i3 - i;
            if (AlbumSetPage.this.mShowDetails) {
                AlbumSetPage.this.mDetailsHelper.layout(i, height, i3, i4);
            } else {
                AlbumSetPage.this.mAlbumSetView.setHighlightItemPath(null);
            }
            AlbumSetPage.this.mSlotView.layout(0, height, i6, i5);
        }

        @Override
        protected void render(GLCanvas gLCanvas) {
            gLCanvas.save(2);
            GalleryUtils.setViewPointMatrix(this.mMatrix, (getWidth() / 2) + AlbumSetPage.this.mX, (getHeight() / 2) + AlbumSetPage.this.mY, AlbumSetPage.this.mZ);
            gLCanvas.multiplyMatrix(this.mMatrix, 0);
            super.render(gLCanvas);
            gLCanvas.restore();
        }
    };
    WeakReference<Toast> mEmptyAlbumToast = null;
    private boolean mNeedUpdateSelection = false;
    private Toast mWaitToast = null;
    private int mLayoutType = -1;
    private int mNeedDoClusterType = 0;
    private boolean mIsInMultiWindowMode = false;
    private MultiWindowListener mMultiWindowListener = new MultiWindowListener();

    @Override
    protected int getBackgroundColorId() {
        return R.color.albumset_background;
    }

    @Override
    public void onEyePositionChanged(float f, float f2, float f3) {
        this.mRootPane.lockRendering();
        this.mX = f;
        this.mY = f2;
        this.mZ = f3;
        if (FancyHelper.isFancyLayoutSupported() && !this.mActivity.isInMultiWindowMode()) {
            Log.d("Gallery2/AlbumSetPage", "<onEyePositionChanged> <Fancy> enter");
            this.mIsInMultiWindowMode = false;
            DisplayMetrics displayMetrics = getDisplayMetrics();
            FancyHelper.doFancyInitialization(displayMetrics.widthPixels, displayMetrics.heightPixels);
            int layoutType = this.mEyePosition.getLayoutType();
            if (this.mLayoutType != layoutType) {
                this.mLayoutType = layoutType;
                Log.d("Gallery2/AlbumSetPage", "<onEyePositionChanged> <Fancy> begin to switchLayout");
                this.mAlbumSetView.onEyePositionChanged(this.mLayoutType);
                this.mSlotView.switchLayout(this.mLayoutType);
            }
        }
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

    private void getSlotCenter(int i, int[] iArr) {
        Rect rect = new Rect();
        this.mRootPane.getBoundsOf(this.mSlotView, rect);
        Rect slotRect = this.mSlotView.getSlotRect(i);
        int scrollX = this.mSlotView.getScrollX();
        int scrollY = this.mSlotView.getScrollY();
        iArr[0] = (rect.left + ((slotRect.left + slotRect.right) / 2)) - scrollX;
        iArr[1] = (rect.top + ((slotRect.top + slotRect.bottom) / 2)) - scrollY;
    }

    public void onSingleTapUp(int i) {
        if (this.mIsActive) {
            if (this.mSelectionManager.inSelectionMode()) {
                MediaSet mediaSet = this.mAlbumSetDataAdapter.getMediaSet(i);
                if (mediaSet == null) {
                    return;
                }
                if (this.mRestoreSelectionDone) {
                    if (this.mActionModeHandler != null) {
                        this.mActionModeHandler.closeMenu();
                    }
                    this.mSelectionManager.toggle(mediaSet.getPath());
                    this.mSlotView.invalidate();
                    return;
                }
                if (this.mWaitToast == null) {
                    this.mWaitToast = Toast.makeText(this.mActivity, R.string.wait, 0);
                }
                this.mWaitToast.show();
                return;
            }
            if (!this.mAlbumSetDataAdapter.isActive(i)) {
                Log.d("Gallery2/AlbumSetPage", "<onSingleTapUp> slotIndex " + i + " is not active, return!");
                return;
            }
            this.mAlbumSetView.setPressedIndex(i);
            this.mAlbumSetView.setPressedUp();
            Log.d("Gallery2/AlbumSetPage", "onSingleTapUp() at " + System.currentTimeMillis());
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, i, 0));
            this.mAlbumSetView.setPressedIndex(-1);
        }
    }

    private static boolean albumShouldOpenInFilmstrip(MediaSet mediaSet) {
        ArrayList<MediaItem> mediaItem = mediaSet.getMediaItemCount() == 1 ? mediaSet.getMediaItem(0, 1) : null;
        return (mediaItem == null || mediaItem.isEmpty()) ? false : true;
    }

    private void showEmptyAlbumToast(int i) {
        Toast toast;
        if (this.mEmptyAlbumToast != null && (toast = this.mEmptyAlbumToast.get()) != null) {
            toast.show();
            return;
        }
        Toast toastMakeText = Toast.makeText(this.mActivity, R.string.empty_album, i);
        this.mEmptyAlbumToast = new WeakReference<>(toastMakeText);
        toastMakeText.show();
    }

    private void hideEmptyAlbumToast() {
        Toast toast;
        if (this.mEmptyAlbumToast == null || (toast = this.mEmptyAlbumToast.get()) == null) {
            return;
        }
        toast.cancel();
    }

    private void pickAlbum(int i) {
        MediaSet mediaSet;
        if (this.mIsActive && this.mAlbumSetDataAdapter.isActive(i) && (mediaSet = this.mAlbumSetDataAdapter.getMediaSet(i)) != null) {
            if (mediaSet.getTotalMediaItemCount() == 0) {
                showEmptyAlbumToast(0);
                return;
            }
            hideEmptyAlbumToast();
            String string = mediaSet.getPath().toString();
            Bundle bundle = new Bundle(getData());
            int[] iArr = new int[2];
            getSlotCenter(i, iArr);
            bundle.putIntArray("set-center", iArr);
            if (this.mGetAlbum && mediaSet.isLeafAlbum()) {
                AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
                abstractGalleryActivity.setResult(-1, new Intent().putExtra("album-path", mediaSet.getPath().toString()));
                abstractGalleryActivity.finish();
                return;
            }
            if (mediaSet.getSubMediaSetCount() > 0) {
                bundle.putString("media-path", string);
                this.mActivity.getStateManager().startStateForResult(AlbumSetPage.class, 1, bundle);
                return;
            }
            if (!this.mGetContent && albumShouldOpenInFilmstrip(mediaSet)) {
                bundle.putParcelable("open-animation-rect", this.mSlotView.getSlotRect(i, this.mRootPane));
                bundle.putInt("index-hint", 0);
                bundle.putString("media-set-path", string);
                bundle.putBoolean("start-in-filmstrip", true);
                bundle.putBoolean("in_camera_roll", mediaSet.isCameraRoll());
                bundle.putString("media-item-path", null);
                this.mActivity.getStateManager().startStateForResult(FilmstripPage.class, 2, bundle);
                this.mAlbumSetView.setPressedIndex(-1);
                return;
            }
            bundle.putString("media-path", string);
            bundle.putBoolean("cluster-menu", !this.mActivity.getStateManager().hasStateClass(AlbumPage.class));
            this.mActivity.getStateManager().startStateForResult(AlbumPage.class, 1, bundle);
        }
    }

    private void onDown(int i) {
        this.mAlbumSetView.setPressedIndex(i);
    }

    private void onUp(boolean z) {
        if (z) {
            this.mAlbumSetView.setPressedIndex(-1);
        } else {
            this.mAlbumSetView.setPressedUp();
        }
    }

    public void onLongTap(int i) {
        MediaSet mediaSet;
        if (this.mGetContent || this.mGetAlbum || (mediaSet = this.mAlbumSetDataAdapter.getMediaSet(i)) == null) {
            return;
        }
        if (this.mActionModeHandler != null) {
            this.mActionModeHandler.closeMenu();
        }
        this.mSelectionManager.setAutoLeaveSelectionMode(true);
        this.mSelectionManager.toggle(mediaSet.getPath());
        this.mSlotView.invalidate();
    }

    @Override
    public void doCluster(int i) {
        if (i == 4 && !PermissionHelper.checkAndRequestForLocationCluster(this.mActivity)) {
            Log.d("Gallery2/AlbumSetPage", "<doCluster> permission not granted");
            this.mNeedDoClusterType = i;
            return;
        }
        String strSwitchClusterPath = FilterUtils.switchClusterPath(this.mMediaSet.getPath().toString(), i);
        Bundle bundle = new Bundle(getData());
        bundle.putString("media-path", strSwitchClusterPath);
        bundle.putInt("selected-cluster", i);
        this.mActivity.getStateManager().switchState(this, AlbumSetPage.class, bundle);
    }

    @Override
    public void onCreate(Bundle bundle, Bundle bundle2) {
        super.onCreate(bundle, bundle2);
        initializeViews();
        initializeData(bundle);
        this.mInitialized = true;
        Context androidContext = this.mActivity.getAndroidContext();
        this.mGetContent = bundle.getBoolean("get-content", false);
        this.mGetAlbum = bundle.getBoolean("get-album", false);
        this.mTitle = bundle.getString("set-title");
        this.mClusterType = bundle.getInt("selected-cluster");
        this.mEyePosition = new EyePosition(androidContext, this);
        this.mDetailsSource = new MyDetailsSource();
        this.mActionBar = this.mActivity.getGalleryActionBar();
        if (this.mSlotView != null) {
            this.mSlotView.setActionBar(this.mActionBar);
        }
        this.mSelectedAction = bundle.getInt("selected-cluster", 1);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    AlbumSetPage.this.pickAlbum(message.arg1);
                    return;
                }
                throw new AssertionError(message.what);
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
        RelativeLayout relativeLayout;
        if (!GalleryUtils.isCameraAvailable(this.mActivity) || (relativeLayout = (RelativeLayout) this.mActivity.findViewById(R.id.gallery_root)) == null) {
            return false;
        }
        this.mCameraButton = new Button(this.mActivity);
        this.mCameraButton.setText(R.string.camera_label);
        this.mCameraButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.frame_overlay_gallery_camera, 0, 0);
        this.mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GalleryUtils.startCameraActivity(AlbumSetPage.this.mActivity);
            }
        });
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, -2);
        layoutParams.addRule(13);
        relativeLayout.addView(this.mCameraButton, layoutParams);
        return true;
    }

    private void cleanupCameraButton() {
        RelativeLayout relativeLayout;
        if (this.mCameraButton == null || (relativeLayout = (RelativeLayout) this.mActivity.findViewById(R.id.gallery_root)) == null) {
            return;
        }
        relativeLayout.removeView(this.mCameraButton);
        this.mCameraButton = null;
    }

    private void showCameraButton() {
        if (this.mCameraButton != null || setupCameraButton()) {
            this.mCameraButton.setVisibility(0);
        }
    }

    private void hideCameraButton() {
        if (this.mCameraButton == null) {
            return;
        }
        this.mCameraButton.setVisibility(8);
    }

    private void clearLoadingBit(int i) {
        this.mLoadingBits = (~i) & this.mLoadingBits;
        if (this.mLoadingBits == 0 && this.mIsActive && this.mAlbumSetDataAdapter.size() == 0) {
            if (this.mActivity.getStateManager().getStateCount() > 1) {
                Intent intent = new Intent();
                intent.putExtra("empty-album", true);
                setStateResult(-1, intent);
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

    private void setLoadingBit(int i) {
        this.mLoadingBits = i | this.mLoadingBits;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (FancyHelper.isFancyLayoutSupported()) {
            this.mAlbumSetDataAdapter.setFancyDataChangeListener(null);
            Log.d("Gallery2/AlbumSetPage", "<onPause> set FancyDataChangeListener as null");
        }
        this.mIsActive = false;
        if (this.mSelectionManager != null && this.mSelectionManager.inSelectionMode()) {
            this.mSelectionManager.saveSelection();
            this.mNeedUpdateSelection = false;
        }
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
        this.mActivity.setEjectListener(null);
        this.mActivity.setMultiWindowModeListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (FancyHelper.isFancyLayoutSupported()) {
            this.mAlbumSetDataAdapter.setFancyDataChangeListener(this.mSlotView);
            Log.d("Gallery2/AlbumSetPage", "<onResume> reset FancyDataChangeListener");
        }
        this.mIsActive = true;
        setContentPane(this.mRootPane);
        setLoadingBit(1);
        if (this.mSelectionManager != null && this.mSelectionManager.inSelectionMode()) {
            this.mNeedUpdateSelection = true;
            this.mRestoreSelectionDone = false;
        } else {
            this.mRestoreSelectionDone = true;
        }
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
        this.mActivity.setEjectListener(this);
        if (this.mClusterType == 4 && !PermissionHelper.checkLocationPermission(this.mActivity)) {
            Log.d("Gallery2/AlbumSetPage", "<onResume> CLUSTER_BY_LOCATION, permisison not granted, finish");
            PermissionHelper.showDeniedPrompt(this.mActivity);
            this.mActivity.getStateManager().finishState(this);
        } else {
            this.mActivity.setMultiWindowModeListener(this.mMultiWindowListener);
            this.mMultiWindowListener.onMultiWindowModeChanged(this.mActivity.isInMultiWindowMode());
        }
    }

    private void initializeData(Bundle bundle) {
        this.mMediaSet = this.mActivity.getDataManager().getMediaSet(bundle.getString("media-path"));
        this.mSelectionManager.setSourceMediaSet(this.mMediaSet);
        this.mAlbumSetDataAdapter = new AlbumSetDataLoader(this.mActivity, this.mMediaSet, 256);
        this.mAlbumSetDataAdapter.setLoadingListener(new MyLoadingListener());
        this.mAlbumSetView.setModel(this.mAlbumSetDataAdapter);
        if (FancyHelper.isFancyLayoutSupported()) {
            this.mAlbumSetDataAdapter.setFancyDataChangeListener(this.mSlotView);
            FancyHelper.initializeFancyThumbnailSizes(getDisplayMetrics());
        }
    }

    private void initializeViews() {
        this.mSelectionManager = new SelectionManager(this.mActivity, true);
        this.mSelectionManager.setSelectionListener(this);
        this.mConfig = Config.AlbumSetPage.get(this.mActivity);
        this.mSlotView = new SlotView(this.mActivity, this.mConfig.slotViewSpec, FancyHelper.isFancyLayoutSupported());
        if (FancyHelper.isFancyLayoutSupported()) {
            this.mSlotView.setPaddingSpec(this.mConfig.paddingTop, this.mConfig.paddingBottom);
            this.mSlotView.setMultiWindowSpec(Config.AlbumSetPage.getConfigInMultiWindow(this.mActivity).slotViewSpec);
        }
        this.mAlbumSetView = new AlbumSetSlotRenderer(this.mActivity, this.mSelectionManager, this.mSlotView, this.mConfig.labelSpec, this.mConfig.placeholderColor);
        this.mSlotView.setSlotRenderer(this.mAlbumSetView);
        this.mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int i) {
                AlbumSetPage.this.onDown(i);
            }

            @Override
            public void onUp(boolean z) {
                AlbumSetPage.this.onUp(z);
            }

            @Override
            public void onSingleTapUp(int i) {
                AlbumSetPage.this.onSingleTapUp(i);
            }

            @Override
            public void onLongTap(int i) {
                AlbumSetPage.this.onLongTap(i);
            }
        });
        this.mActionModeHandler = new ActionModeHandler(this.mActivity, this.mSelectionManager);
        this.mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem menuItem) {
                return AlbumSetPage.this.onItemSelected(menuItem);
            }

            @Override
            public boolean onPopUpItemClicked(int i) {
                return AlbumSetPage.this.mRestoreSelectionDone;
            }
        });
        this.mRootPane.addComponent(this.mSlotView);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
        boolean zHasStateClass = this.mActivity.getStateManager().hasStateClass(AlbumPage.class);
        MenuInflater supportMenuInflater = getSupportMenuInflater();
        if (this.mGetContent) {
            supportMenuInflater.inflate(R.menu.pickup, menu);
            this.mActionBar.setTitle(GalleryUtils.getSelectionModePrompt(this.mData.getInt("type-bits", 1)));
        } else if (this.mGetAlbum) {
            supportMenuInflater.inflate(R.menu.pickup, menu);
            this.mActionBar.setTitle(R.string.select_album);
        } else {
            supportMenuInflater.inflate(R.menu.albumset, menu);
            boolean z = this.mShowClusterMenu;
            this.mShowClusterMenu = !zHasStateClass;
            MenuItem menuItemFindItem = menu.findItem(R.id.action_select);
            if (menuItemFindItem != null) {
                if (!zHasStateClass && (this.mSelectedAction != 0 ? this.mSelectedAction : this.mActionBar.getClusterTypeAction()) == 1) {
                    menuItemFindItem.setTitle(R.string.select_album);
                } else {
                    menuItemFindItem.setTitle(R.string.select_group);
                }
            }
            menu.findItem(R.id.action_camera).setVisible(GalleryUtils.isCameraAvailable(abstractGalleryActivity));
            FilterUtils.setupMenuItems(this.mActionBar, this.mMediaSet.getPath(), false);
            Intent helpIntent = HelpUtils.getHelpIntent(abstractGalleryActivity);
            MenuItem menuItemFindItem2 = menu.findItem(R.id.action_general_help);
            menuItemFindItem2.setVisible(helpIntent != null);
            if (helpIntent != null) {
                menuItemFindItem2.setIntent(helpIntent);
            }
            if (this.mTitle != null) {
                this.mTitle = this.mMediaSet.getName();
                this.mSubtitle = GalleryActionBar.getClusterByTypeString(this.mActivity, this.mClusterType);
            }
            this.mActionBar.setTitle(this.mTitle);
            this.mActionBar.setSubtitle(this.mSubtitle);
            if (this.mShowClusterMenu != z) {
                if (this.mShowClusterMenu) {
                    this.mActionBar.enableClusterMenu(this.mSelectedAction, this);
                } else {
                    this.mActionBar.disableClusterMenu(true);
                }
            }
        }
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem menuItem) {
        AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_camera) {
            GalleryUtils.startCameraActivity(abstractGalleryActivity);
            return true;
        }
        if (itemId == R.id.action_select) {
            this.mSelectionManager.setAutoLeaveSelectionMode(false);
            this.mSelectionManager.enterSelectionMode();
            return true;
        }
        if (itemId != R.id.action_details) {
            if (itemId != R.id.action_cancel) {
                return false;
            }
            abstractGalleryActivity.setResult(0);
            abstractGalleryActivity.finish();
            return true;
        }
        if (this.mAlbumSetDataAdapter.size() != 0) {
            if (this.mShowDetails) {
                hideDetails();
            } else {
                showDetails();
            }
        } else {
            Toast.makeText(abstractGalleryActivity, abstractGalleryActivity.getText(R.string.no_albums_alert), 0).show();
        }
        return true;
    }

    @Override
    protected void onStateResult(int i, int i2, Intent intent) {
        if (i == 1) {
            this.mSlotView.startRisingAnimation();
        }
    }

    public String getSelectedString() {
        int i;
        int selectedCount = this.mSelectionManager.getSelectedCount();
        if (this.mActionBar.getClusterTypeAction() == 1) {
            i = R.plurals.number_of_albums_selected;
        } else {
            i = R.plurals.number_of_groups_selected;
        }
        return String.format(this.mActivity.getResources().getQuantityString(i, selectedCount), Integer.valueOf(selectedCount));
    }

    @Override
    public void onSelectionModeChange(int i) {
        switch (i) {
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
            case 4:
                this.mActionModeHandler.updateSupportedOperation();
                this.mRootPane.invalidate();
                break;
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean z) {
        this.mActionModeHandler.setTitle(getSelectedString());
        this.mActionModeHandler.updateSupportedOperation(path, z);
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
    public void onSyncDone(MediaSet mediaSet, final int i) {
        if (i == 2) {
            Log.d("Gallery2/AlbumSetPage", "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result=" + i);
        }
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot gLRoot = AlbumSetPage.this.mActivity.getGLRoot();
                gLRoot.lockRenderThread();
                try {
                    if (i == 0) {
                        AlbumSetPage.this.mInitialSynced = true;
                    }
                    AlbumSetPage.this.clearLoadingBit(2);
                    if (i == 2 && AlbumSetPage.this.mIsActive) {
                        Log.w("Gallery2/AlbumSetPage", "failed to load album set");
                    }
                } finally {
                    gLRoot.unlockRenderThread();
                }
            }
        });
    }

    private class MyLoadingListener implements LoadingListener {
        private MyLoadingListener() {
        }

        @Override
        public void onLoadingStarted() {
            AlbumSetPage.this.mLoadingFinished = false;
            AlbumSetPage.this.setLoadingBit(1);
        }

        @Override
        public void onLoadingFinished(boolean z) {
            int subMediaSetCount;
            AlbumSetPage.this.mLoadingFinished = true;
            AlbumSetPage.this.clearLoadingBit(1);
            boolean z2 = false;
            boolean z3 = AlbumSetPage.this.mSelectionManager != null && AlbumSetPage.this.mSelectionManager.inSelectionMode();
            if (AlbumSetPage.this.mMediaSet != null) {
                subMediaSetCount = AlbumSetPage.this.mMediaSet.getSubMediaSetCount();
            } else {
                subMediaSetCount = 0;
            }
            Log.d("Gallery2/AlbumSetPage", "<onLoadingFinished> set count=" + subMediaSetCount);
            Log.d("Gallery2/AlbumSetPage", "<onLoadingFinished> inSelectionMode=" + z3);
            AlbumSetPage.this.mSelectionManager.onSourceContentChanged();
            if (subMediaSetCount > 0 && z3) {
                if (AlbumSetPage.this.mNeedUpdateSelection) {
                    AlbumSetPage.this.mNeedUpdateSelection = false;
                    AlbumSetPage.this.mSelectionManager.restoreSelection();
                    z2 = true;
                }
                AlbumSetPage.this.mActionModeHandler.updateSupportedOperation();
                AlbumSetPage.this.mActionModeHandler.updateSelectionMenu();
            }
            if (!z2) {
                AlbumSetPage.this.mRestoreSelectionDone = true;
            }
            if (AlbumSetPage.this.mTitle != null && AlbumSetPage.this.mActionBar != null && !z3 && (AlbumSetPage.this.mMediaSet instanceof ClusterAlbumSet)) {
                String name = AlbumSetPage.this.mMediaSet.getName();
                String clusterByTypeString = GalleryActionBar.getClusterByTypeString(AlbumSetPage.this.mActivity, AlbumSetPage.this.mClusterType);
                if (!AlbumSetPage.this.mTitle.equalsIgnoreCase(name) || (clusterByTypeString != null && !clusterByTypeString.equalsIgnoreCase(AlbumSetPage.this.mSubtitle))) {
                    AlbumSetPage.this.mTitle = name;
                    AlbumSetPage.this.mSubtitle = clusterByTypeString;
                    AlbumSetPage.this.mActionBar.setTitle(AlbumSetPage.this.mTitle);
                    AlbumSetPage.this.mActionBar.setSubtitle(AlbumSetPage.this.mSubtitle);
                    Log.d("Gallery2/AlbumSetPage", "<onLoadingFinished> mTitle:" + AlbumSetPage.this.mTitle + "mSubtitle = " + AlbumSetPage.this.mSubtitle);
                    AlbumSetPage.this.mActionBar.notifyDataSetChanged();
                }
            }
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
            this.mIndex = AlbumSetPage.this.mAlbumSetDataAdapter.findSet(AlbumSetPage.this.mSelectionManager.getSelected(false).get(0));
            return this.mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            MediaSet mediaSet = AlbumSetPage.this.mAlbumSetDataAdapter.getMediaSet(this.mIndex);
            if (mediaSet != null) {
                AlbumSetPage.this.mAlbumSetView.setHighlightItemPath(mediaSet.getPath());
                return mediaSet.getDetails();
            }
            return null;
        }
    }

    @Override
    public void onEjectSdcard() {
        if (this.mSelectionManager != null && this.mSelectionManager.inSelectionMode()) {
            Log.d("Gallery2/AlbumSetPage", "<onEjectSdcard> leaveSelectionMode");
            this.mSelectionManager.leaveSelectionMode();
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

    private DisplayMetrics getDisplayMetrics() {
        WindowManager windowManager = (WindowManager) this.mActivity.getSystemService("window");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= 17) {
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        Log.d("Gallery2/AlbumSetPage", "<getDisplayMetrix> <Fancy> Display Metrics: " + displayMetrics.widthPixels + " x " + displayMetrics.heightPixels);
        return displayMetrics;
    }

    @Override
    protected void onSaveState(Bundle bundle) {
        String string = this.mActivity.getDataManager().toString();
        String strValueOf = String.valueOf(Process.myPid());
        bundle.putString("data-manager-object", string);
        bundle.putString("process-id", strValueOf);
        Log.d("Gallery2/AlbumSetPage", "<onSaveState> dataManager = " + string + ", processId = " + strValueOf);
    }

    @Override
    public void setProviderSensive(boolean z) {
        this.mAlbumSetDataAdapter.setSourceSensive(z);
    }

    @Override
    public void fakeProviderChange() {
        this.mAlbumSetDataAdapter.fakeSourceChange();
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (PermissionHelper.isAllPermissionsGranted(strArr, iArr)) {
            doCluster(this.mNeedDoClusterType);
        } else {
            PermissionHelper.showDeniedPromptIfNeeded(this.mActivity, "android.permission.ACCESS_FINE_LOCATION");
        }
    }

    private class MultiWindowListener implements AbstractGalleryActivity.MultiWindowModeListener {
        private MultiWindowListener() {
        }

        @Override
        public void onMultiWindowModeChanged(boolean z) {
            if (AlbumSetPage.this.mIsInMultiWindowMode != z) {
                AlbumSetPage.this.mRootPane.lockRendering();
                Log.d("Gallery2/AlbumSetPage", "<onMultiWindowModeChanged> isInMultiWindowMode: " + z);
                AlbumSetPage.this.mIsInMultiWindowMode = z;
                if (AlbumSetPage.this.mIsInMultiWindowMode) {
                    Log.d("Gallery2/AlbumSetPage", "<onMultiWindowModeChanged> switch to MULTI_WINDOW_LAYOUT");
                    AlbumSetPage.this.mLayoutType = 2;
                    AlbumSetPage.this.mAlbumSetView.onEyePositionChanged(AlbumSetPage.this.mLayoutType);
                    AlbumSetPage.this.mSlotView.switchLayout(AlbumSetPage.this.mLayoutType);
                } else {
                    Log.d("Gallery2/AlbumSetPage", "<onMultiWindowModeChanged> <Fancy> enter");
                    DisplayMetrics displayMetrics = AlbumSetPage.this.getDisplayMetrics();
                    FancyHelper.doFancyInitialization(displayMetrics.widthPixels, displayMetrics.heightPixels);
                    AlbumSetPage.this.mLayoutType = AlbumSetPage.this.mEyePosition.getLayoutType();
                    Log.d("Gallery2/AlbumSetPage", "<onMultiWindowModeChanged> <Fancy> begin to switchLayout");
                    AlbumSetPage.this.mAlbumSetView.onEyePositionChanged(AlbumSetPage.this.mLayoutType);
                    AlbumSetPage.this.mSlotView.switchLayout(AlbumSetPage.this.mLayoutType);
                }
                AlbumSetPage.this.mRootPane.unlockRendering();
            }
        }
    }
}
