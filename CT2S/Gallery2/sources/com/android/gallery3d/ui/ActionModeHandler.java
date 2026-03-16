package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ShareActionProvider;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PopupList;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;

public class ActionModeHandler implements ActionMode.Callback, PopupList.OnPopupItemClickListener {
    private ActionMode mActionMode;
    private final AbstractGalleryActivity mActivity;
    private WakeLockHoldingProgressListener mDeleteProgressListener;
    private ActionModeListener mListener;
    private final Handler mMainHandler;
    private Menu mMenu;
    private final MenuExecutor mMenuExecutor;
    private Future<?> mMenuTask;
    private final NfcAdapter mNfcAdapter;
    private final SelectionManager mSelectionManager;
    private SelectionMenu mSelectionMenu;
    private ShareActionProvider mShareActionProvider;
    private MenuItem mShareMenuItem;
    private ShareActionProvider mSharePanoramaActionProvider;
    private MenuItem mSharePanoramaMenuItem;
    private final ShareActionProvider.OnShareTargetSelectedListener mShareTargetSelectedListener = new ShareActionProvider.OnShareTargetSelectedListener() {
        @Override
        public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
            ActionModeHandler.this.mSelectionManager.leaveSelectionMode();
            return false;
        }
    };

    public interface ActionModeListener {
        boolean onActionItemClicked(MenuItem menuItem);
    }

    private static class GetAllPanoramaSupports implements MediaObject.PanoramaSupportCallback {
        private ThreadPool.JobContext mJobContext;
        private int mNumInfoRequired;
        public boolean mAllPanoramas = true;
        public boolean mAllPanorama360 = true;
        public boolean mHasPanorama360 = false;
        private Object mLock = new Object();

        public GetAllPanoramaSupports(ArrayList<MediaObject> mediaObjects, ThreadPool.JobContext jc) {
            this.mJobContext = jc;
            this.mNumInfoRequired = mediaObjects.size();
            for (MediaObject mediaObject : mediaObjects) {
                mediaObject.getPanoramaSupport(this);
            }
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama, boolean isPanorama360) {
            synchronized (this.mLock) {
                this.mNumInfoRequired--;
                this.mAllPanoramas = isPanorama && this.mAllPanoramas;
                this.mAllPanorama360 = isPanorama360 && this.mAllPanorama360;
                this.mHasPanorama360 = this.mHasPanorama360 || isPanorama360;
                if (this.mNumInfoRequired == 0 || this.mJobContext.isCancelled()) {
                    this.mLock.notifyAll();
                }
            }
        }

        public void waitForPanoramaSupport() {
            synchronized (this.mLock) {
                while (this.mNumInfoRequired != 0 && !this.mJobContext.isCancelled()) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public ActionModeHandler(AbstractGalleryActivity activity, SelectionManager selectionManager) {
        this.mActivity = (AbstractGalleryActivity) Utils.checkNotNull(activity);
        this.mSelectionManager = (SelectionManager) Utils.checkNotNull(selectionManager);
        this.mMenuExecutor = new MenuExecutor(activity, selectionManager);
        this.mMainHandler = new Handler(activity.getMainLooper());
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this.mActivity.getAndroidContext());
    }

    public void startActionMode() {
        Activity a = this.mActivity;
        this.mActionMode = a.startActionMode(this);
        View customView = LayoutInflater.from(a).inflate(R.layout.action_mode, (ViewGroup) null);
        this.mActionMode.setCustomView(customView);
        this.mSelectionMenu = new SelectionMenu(a, (Button) customView.findViewById(R.id.selection_menu), this);
        updateSelectionMenu();
    }

    public void finishActionMode() {
        this.mActionMode.finish();
    }

    public void setTitle(String title) {
        this.mSelectionMenu.setTitle(title);
    }

    public void setActionModeListener(ActionModeListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean result;
        GLRoot root = this.mActivity.getGLRoot();
        root.lockRenderThread();
        try {
            if (this.mListener != null && (result = this.mListener.onActionItemClicked(item))) {
                this.mSelectionManager.leaveSelectionMode();
                return result;
            }
            MenuExecutor.ProgressListener listener = null;
            String confirmMsg = null;
            int action = item.getItemId();
            if (action == R.id.action_delete) {
                confirmMsg = this.mActivity.getResources().getQuantityString(R.plurals.delete_selection, this.mSelectionManager.getSelectedCount());
                if (this.mDeleteProgressListener == null) {
                    this.mDeleteProgressListener = new WakeLockHoldingProgressListener(this.mActivity, "Gallery Delete Progress Listener");
                }
                listener = this.mDeleteProgressListener;
            }
            this.mMenuExecutor.onMenuClicked(item, confirmMsg, listener);
            root.unlockRenderThread();
            return true;
        } finally {
            root.unlockRenderThread();
        }
    }

    @Override
    public boolean onPopupItemClick(int itemId) {
        GLRoot root = this.mActivity.getGLRoot();
        root.lockRenderThread();
        if (itemId == R.id.action_select_all) {
            try {
                updateSupportedOperation();
                this.mMenuExecutor.onMenuClicked(itemId, null, false, true);
            } finally {
                root.unlockRenderThread();
            }
        }
        return true;
    }

    private void updateSelectionMenu() {
        int count = this.mSelectionManager.getSelectedCount();
        String format = this.mActivity.getResources().getQuantityString(R.plurals.number_of_items_selected, count);
        setTitle(String.format(format, Integer.valueOf(count)));
        this.mSelectionMenu.updateSelectAllMode(this.mSelectionManager.inSelectAllMode());
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.operation, menu);
        this.mMenu = menu;
        this.mSharePanoramaMenuItem = menu.findItem(R.id.action_share_panorama);
        if (this.mSharePanoramaMenuItem != null) {
            this.mSharePanoramaActionProvider = (ShareActionProvider) this.mSharePanoramaMenuItem.getActionProvider();
            this.mSharePanoramaActionProvider.setOnShareTargetSelectedListener(this.mShareTargetSelectedListener);
            this.mSharePanoramaActionProvider.setShareHistoryFileName("panorama_share_history.xml");
        }
        this.mShareMenuItem = menu.findItem(R.id.action_share);
        if (this.mShareMenuItem != null) {
            this.mShareActionProvider = (ShareActionProvider) this.mShareMenuItem.getActionProvider();
            this.mShareActionProvider.setOnShareTargetSelectedListener(this.mShareTargetSelectedListener);
            this.mShareActionProvider.setShareHistoryFileName("share_history.xml");
            return true;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.mSelectionManager.leaveSelectionMode();
    }

    private ArrayList<MediaObject> getSelectedMediaObjects(ThreadPool.JobContext jc) {
        ArrayList<Path> unexpandedPaths = this.mSelectionManager.getSelected(false);
        if (unexpandedPaths.isEmpty()) {
            return null;
        }
        ArrayList<MediaObject> selected = new ArrayList<>();
        DataManager manager = this.mActivity.getDataManager();
        for (Path path : unexpandedPaths) {
            if (jc.isCancelled()) {
                return null;
            }
            selected.add(manager.getMediaObject(path));
        }
        return selected;
    }

    private int computeMenuOptions(ArrayList<MediaObject> selected) {
        int operation = -1;
        int type = 0;
        for (MediaObject mediaObject : selected) {
            int support = mediaObject.getSupportedOperations();
            type |= mediaObject.getMediaType();
            operation &= support;
        }
        switch (selected.size()) {
            case 1:
                String mimeType = MenuExecutor.getMimeType(type);
                if (!GalleryUtils.isEditorAvailable(this.mActivity, mimeType)) {
                    return operation & (-513);
                }
                return operation;
            default:
                return operation & 263;
        }
    }

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private void setNfcBeamPushUris(Uri[] uris) {
        if (this.mNfcAdapter != null && ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
            this.mNfcAdapter.setBeamPushUrisCallback(null, this.mActivity);
            this.mNfcAdapter.setBeamPushUris(uris, this.mActivity);
        }
    }

    private Intent computePanoramaSharingIntent(ThreadPool.JobContext jc, int maxItems) {
        ArrayList<Path> expandedPaths = this.mSelectionManager.getSelected(true, maxItems);
        if (expandedPaths == null || expandedPaths.size() == 0) {
            return new Intent();
        }
        ArrayList<Uri> uris = new ArrayList<>();
        DataManager manager = this.mActivity.getDataManager();
        Intent intent = new Intent();
        for (Path path : expandedPaths) {
            if (jc.isCancelled()) {
                return null;
            }
            uris.add(manager.getContentUri(path));
        }
        int size = uris.size();
        if (size > 0) {
            if (size > 1) {
                intent.setAction("android.intent.action.SEND_MULTIPLE");
                intent.setType("application/vnd.google.panorama360+jpg");
                intent.putParcelableArrayListExtra("android.intent.extra.STREAM", uris);
            } else {
                intent.setAction("android.intent.action.SEND");
                intent.setType("application/vnd.google.panorama360+jpg");
                intent.putExtra("android.intent.extra.STREAM", uris.get(0));
            }
            intent.addFlags(1);
            return intent;
        }
        return intent;
    }

    private Intent computeSharingIntent(ThreadPool.JobContext jc, int maxItems) {
        ArrayList<Path> expandedPaths = this.mSelectionManager.getSelected(true, maxItems);
        if (expandedPaths == null || expandedPaths.size() == 0) {
            setNfcBeamPushUris(null);
            return new Intent();
        }
        ArrayList<Uri> uris = new ArrayList<>();
        DataManager manager = this.mActivity.getDataManager();
        int type = 0;
        Intent intent = new Intent();
        for (Path path : expandedPaths) {
            if (jc.isCancelled()) {
                return null;
            }
            int support = manager.getSupportedOperations(path);
            type |= manager.getMediaType(path);
            if ((support & 4) != 0) {
                uris.add(manager.getContentUri(path));
            }
        }
        int size = uris.size();
        if (size > 0) {
            String mimeType = MenuExecutor.getMimeType(type);
            if (size > 1) {
                intent.setAction("android.intent.action.SEND_MULTIPLE").setType(mimeType);
                intent.putParcelableArrayListExtra("android.intent.extra.STREAM", uris);
            } else {
                intent.setAction("android.intent.action.SEND").setType(mimeType);
                intent.putExtra("android.intent.extra.STREAM", uris.get(0));
            }
            intent.addFlags(1);
            setNfcBeamPushUris((Uri[]) uris.toArray(new Uri[uris.size()]));
            return intent;
        }
        setNfcBeamPushUris(null);
        return intent;
    }

    public void updateSupportedOperation(Path path, boolean selected) {
        updateSupportedOperation();
    }

    public void updateSupportedOperation() {
        if (this.mMenuTask != null) {
            this.mMenuTask.cancel();
        }
        updateSelectionMenu();
        if (this.mSharePanoramaMenuItem != null) {
            this.mSharePanoramaMenuItem.setEnabled(false);
        }
        if (this.mShareMenuItem != null) {
            this.mShareMenuItem.setEnabled(false);
        }
        this.mMenuTask = this.mActivity.getThreadPool().submit(new ThreadPool.Job<Void>() {
            @Override
            public Void run(final ThreadPool.JobContext jc) {
                final Intent share_panorama_intent;
                final Intent share_intent;
                ArrayList<MediaObject> selected = ActionModeHandler.this.getSelectedMediaObjects(jc);
                if (selected == null) {
                    ActionModeHandler.this.mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ActionModeHandler.this.mMenuTask = null;
                            if (!jc.isCancelled()) {
                                MenuExecutor.updateMenuOperation(ActionModeHandler.this.mMenu, 0);
                            }
                        }
                    });
                } else {
                    final int operation = ActionModeHandler.this.computeMenuOptions(selected);
                    if (!jc.isCancelled()) {
                        int numSelected = selected.size();
                        final boolean canSharePanoramas = numSelected < 10;
                        final boolean canShare = numSelected < 300;
                        final GetAllPanoramaSupports supportCallback = canSharePanoramas ? new GetAllPanoramaSupports(selected, jc) : null;
                        if (canSharePanoramas) {
                            share_panorama_intent = ActionModeHandler.this.computePanoramaSharingIntent(jc, 10);
                        } else {
                            share_panorama_intent = new Intent();
                        }
                        if (canShare) {
                            share_intent = ActionModeHandler.this.computeSharingIntent(jc, 300);
                        } else {
                            share_intent = new Intent();
                        }
                        if (canSharePanoramas) {
                            supportCallback.waitForPanoramaSupport();
                        }
                        if (!jc.isCancelled()) {
                            ActionModeHandler.this.mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ActionModeHandler.this.mMenuTask = null;
                                    if (!jc.isCancelled()) {
                                        MenuExecutor.updateMenuOperation(ActionModeHandler.this.mMenu, operation);
                                        MenuExecutor.updateMenuForPanorama(ActionModeHandler.this.mMenu, canSharePanoramas && supportCallback.mAllPanorama360, canSharePanoramas && supportCallback.mHasPanorama360);
                                        if (ActionModeHandler.this.mSharePanoramaMenuItem != null) {
                                            ActionModeHandler.this.mSharePanoramaMenuItem.setEnabled(true);
                                            if (!canSharePanoramas || !supportCallback.mAllPanorama360) {
                                                ActionModeHandler.this.mSharePanoramaMenuItem.setVisible(false);
                                                ActionModeHandler.this.mShareMenuItem.setShowAsAction(1);
                                                ActionModeHandler.this.mShareMenuItem.setTitle(ActionModeHandler.this.mActivity.getResources().getString(R.string.share));
                                            } else {
                                                ActionModeHandler.this.mShareMenuItem.setShowAsAction(0);
                                                ActionModeHandler.this.mShareMenuItem.setTitle(ActionModeHandler.this.mActivity.getResources().getString(R.string.share_as_photo));
                                            }
                                            ActionModeHandler.this.mSharePanoramaActionProvider.setShareIntent(share_panorama_intent);
                                        }
                                        if (ActionModeHandler.this.mShareMenuItem != null) {
                                            ActionModeHandler.this.mShareMenuItem.setEnabled(canShare);
                                            ActionModeHandler.this.mShareActionProvider.setShareIntent(share_intent);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
                return null;
            }
        });
    }

    public void pause() {
        if (this.mMenuTask != null) {
            this.mMenuTask.cancel();
            this.mMenuTask = null;
        }
        this.mMenuExecutor.pause();
    }

    public void destroy() {
        this.mMenuExecutor.destroy();
    }

    public void resume() {
        if (this.mSelectionManager.inSelectionMode()) {
            updateSupportedOperation();
        }
        this.mMenuExecutor.resume();
    }
}
