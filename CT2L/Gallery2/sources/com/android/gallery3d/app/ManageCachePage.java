package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.EyePosition;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.CacheStorageUsageInfo;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.ManageCacheDrawer;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;

public class ManageCachePage extends ActivityState implements View.OnClickListener, EyePosition.EyePositionListener, MenuExecutor.ProgressListener, SelectionManager.SelectionListener {
    private int mAlbumCountToMakeAvailableOffline;
    private AlbumSetDataLoader mAlbumSetDataAdapter;
    private CacheStorageUsageInfo mCacheStorageInfo;
    private EyePosition mEyePosition;
    private View mFooterContent;
    private Handler mHandler;
    private MediaSet mMediaSet;
    protected ManageCacheDrawer mSelectionDrawer;
    protected SelectionManager mSelectionManager;
    private SlotView mSlotView;
    private Future<Void> mUpdateStorageInfo;
    private float mX;
    private float mY;
    private float mZ;
    private boolean mLayoutReady = false;
    private GLView mRootPane = new GLView() {
        private float[] mMatrix = new float[16];

        @Override
        protected void renderBackground(GLCanvas view) {
            view.clearBuffer(getBackgroundColor());
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if (!ManageCachePage.this.mLayoutReady) {
                ManageCachePage.this.mHandler.sendEmptyMessage(2);
                return;
            }
            ManageCachePage.this.mLayoutReady = false;
            ManageCachePage.this.mEyePosition.resetPosition();
            int slotViewTop = ManageCachePage.this.mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top;
            View footer = ManageCachePage.this.mActivity.findViewById(R.id.footer);
            if (footer != null) {
                int[] location = {0, 0};
                footer.getLocationOnScreen(location);
                slotViewBottom = location[1];
            }
            ManageCachePage.this.mSlotView.layout(0, slotViewTop, right - left, slotViewBottom);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(2);
            GalleryUtils.setViewPointMatrix(this.mMatrix, (getWidth() / 2) + ManageCachePage.this.mX, (getHeight() / 2) + ManageCachePage.this.mY, ManageCachePage.this.mZ);
            canvas.multiplyMatrix(this.mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }
    };
    private ThreadPool.Job<Void> mUpdateStorageInfoJob = new ThreadPool.Job<Void>() {
        @Override
        public Void run(ThreadPool.JobContext jc) {
            ManageCachePage.this.mCacheStorageInfo.loadStorageInfo(jc);
            if (!jc.isCancelled()) {
                ManageCachePage.this.mHandler.sendEmptyMessage(1);
                return null;
            }
            return null;
        }
    };

    @Override
    protected int getBackgroundColorId() {
        return R.color.cache_background;
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

    private void onDown(int index) {
        this.mSelectionDrawer.setPressedIndex(index);
    }

    private void onUp() {
        this.mSelectionDrawer.setPressedIndex(-1);
    }

    public void onSingleTapUp(int slotIndex) {
        MediaSet targetSet = this.mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (targetSet != null) {
            if ((targetSet.getSupportedOperations() & NotificationCompat.FLAG_LOCAL_ONLY) == 0) {
                showToastForLocalAlbum();
                return;
            }
            Path path = targetSet.getPath();
            boolean isFullyCached = targetSet.getCacheFlag() == 2;
            boolean isSelected = this.mSelectionManager.isItemSelected(path);
            if (!isFullyCached) {
                if (isSelected) {
                    this.mAlbumCountToMakeAvailableOffline--;
                } else {
                    this.mAlbumCountToMakeAvailableOffline++;
                }
            }
            long sizeOfTarget = targetSet.getCacheSize();
            CacheStorageUsageInfo cacheStorageUsageInfo = this.mCacheStorageInfo;
            if (isFullyCached ^ isSelected) {
                sizeOfTarget = -sizeOfTarget;
            }
            cacheStorageUsageInfo.increaseTargetCacheSize(sizeOfTarget);
            refreshCacheStorageInfo();
            this.mSelectionManager.toggle(path);
            this.mSlotView.invalidate();
        }
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        this.mCacheStorageInfo = new CacheStorageUsageInfo(this.mActivity);
        initializeViews();
        initializeData(data);
        this.mEyePosition = new EyePosition(this.mActivity.getAndroidContext(), this);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        ManageCachePage.this.refreshCacheStorageInfo();
                        break;
                    case 2:
                        ManageCachePage.this.mLayoutReady = true;
                        removeMessages(2);
                        ManageCachePage.this.mRootPane.requestLayout();
                        break;
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        initializeFooterViews();
        FrameLayout layout = (FrameLayout) this.mActivity.findViewById(R.id.footer);
        if (layout.getVisibility() == 0) {
            layout.removeAllViews();
            layout.addView(this.mFooterContent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mAlbumSetDataAdapter.pause();
        this.mSelectionDrawer.pause();
        this.mEyePosition.pause();
        if (this.mUpdateStorageInfo != null) {
            this.mUpdateStorageInfo.cancel();
            this.mUpdateStorageInfo = null;
        }
        this.mHandler.removeMessages(1);
        FrameLayout layout = (FrameLayout) this.mActivity.findViewById(R.id.footer);
        layout.removeAllViews();
        layout.setVisibility(4);
    }

    @Override
    public void onResume() {
        super.onResume();
        setContentPane(this.mRootPane);
        this.mAlbumSetDataAdapter.resume();
        this.mSelectionDrawer.resume();
        this.mEyePosition.resume();
        this.mUpdateStorageInfo = this.mActivity.getThreadPool().submit(this.mUpdateStorageInfoJob);
        FrameLayout layout = (FrameLayout) this.mActivity.findViewById(R.id.footer);
        layout.addView(this.mFooterContent);
        layout.setVisibility(0);
    }

    private void initializeData(Bundle data) {
        String mediaPath = data.getString("media-path");
        this.mMediaSet = this.mActivity.getDataManager().getMediaSet(mediaPath);
        this.mSelectionManager.setSourceMediaSet(this.mMediaSet);
        this.mSelectionManager.setAutoLeaveSelectionMode(false);
        this.mSelectionManager.enterSelectionMode();
        this.mAlbumSetDataAdapter = new AlbumSetDataLoader(this.mActivity, this.mMediaSet, NotificationCompat.FLAG_LOCAL_ONLY);
        this.mSelectionDrawer.setModel(this.mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        Activity activity = this.mActivity;
        this.mSelectionManager = new SelectionManager(this.mActivity, true);
        this.mSelectionManager.setSelectionListener(this);
        Config.ManageCachePage config = Config.ManageCachePage.get((Context) activity);
        this.mSlotView = new SlotView(this.mActivity, config.slotViewSpec);
        this.mSelectionDrawer = new ManageCacheDrawer(this.mActivity, this.mSelectionManager, this.mSlotView, config.labelSpec, config.cachePinSize, config.cachePinMargin);
        this.mSlotView.setSlotRenderer(this.mSelectionDrawer);
        this.mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                ManageCachePage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                ManageCachePage.this.onUp();
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                ManageCachePage.this.onSingleTapUp(slotIndex);
            }
        });
        this.mRootPane.addComponent(this.mSlotView);
        initializeFooterViews();
    }

    private void initializeFooterViews() {
        Activity activity = this.mActivity;
        LayoutInflater inflater = activity.getLayoutInflater();
        this.mFooterContent = inflater.inflate(R.layout.manage_offline_bar, (ViewGroup) null);
        this.mFooterContent.findViewById(R.id.done).setOnClickListener(this);
        refreshCacheStorageInfo();
    }

    @Override
    public void onClick(View view) {
        Utils.assertTrue(view.getId() == R.id.done);
        GLRoot root = this.mActivity.getGLRoot();
        root.lockRenderThread();
        try {
            ArrayList<Path> ids = this.mSelectionManager.getSelected(false);
            if (ids.size() == 0) {
                onBackPressed();
                return;
            }
            showToast();
            MenuExecutor menuExecutor = new MenuExecutor(this.mActivity, this.mSelectionManager);
            menuExecutor.startAction(R.id.action_toggle_full_caching, R.string.process_caching_requests, this);
        } finally {
            root.unlockRenderThread();
        }
    }

    private void showToast() {
        if (this.mAlbumCountToMakeAvailableOffline > 0) {
            Activity activity = this.mActivity;
            Toast.makeText(activity, activity.getResources().getQuantityString(R.plurals.make_albums_available_offline, this.mAlbumCountToMakeAvailableOffline), 0).show();
        }
    }

    private void showToastForLocalAlbum() {
        Activity activity = this.mActivity;
        Toast.makeText(activity, activity.getResources().getString(R.string.try_to_set_local_album_available_offline), 0).show();
    }

    private void refreshCacheStorageInfo() {
        ProgressBar progressBar = (ProgressBar) this.mFooterContent.findViewById(R.id.progress);
        TextView status = (TextView) this.mFooterContent.findViewById(R.id.status);
        progressBar.setMax(10000);
        long totalBytes = this.mCacheStorageInfo.getTotalBytes();
        long usedBytes = this.mCacheStorageInfo.getUsedBytes();
        long expectedBytes = this.mCacheStorageInfo.getExpectedUsedBytes();
        long freeBytes = this.mCacheStorageInfo.getFreeBytes();
        Activity activity = this.mActivity;
        if (totalBytes == 0) {
            progressBar.setProgress(0);
            progressBar.setSecondaryProgress(0);
            String label = activity.getString(R.string.free_space_format, new Object[]{"-"});
            status.setText(label);
            return;
        }
        progressBar.setProgress((int) ((10000 * usedBytes) / totalBytes));
        progressBar.setSecondaryProgress((int) ((10000 * expectedBytes) / totalBytes));
        String label2 = activity.getString(R.string.free_space_format, new Object[]{Formatter.formatFileSize(activity, freeBytes)});
        status.setText(label2);
    }

    @Override
    public void onProgressComplete(int result) {
        onBackPressed();
    }

    @Override
    public void onProgressUpdate(int index) {
    }

    @Override
    public void onSelectionModeChange(int mode) {
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
    }

    @Override
    public void onConfirmDialogDismissed(boolean confirmed) {
    }

    @Override
    public void onConfirmDialogShown() {
    }

    @Override
    public void onProgressStart() {
    }
}
