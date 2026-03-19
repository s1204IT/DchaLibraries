package com.android.gallery3d.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ClusterAlbumSet;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.plugin.preload.SoOperater;
import java.util.ArrayList;
import java.util.Iterator;

public class MenuExecutor {
    private static final String BT_PRINT_ACTION = "mediatek.intent.action.PRINT";
    public static final int EXECUTION_RESULT_CANCEL = 3;
    public static final int EXECUTION_RESULT_FAIL = 2;
    public static final int EXECUTION_RESULT_SUCCESS = 1;
    private static final String IMAGE_MIME = "image/*";
    private static final int MSG_DO_SHARE = 4;
    private static final int MSG_TASK_COMPLETE = 1;
    private static final int MSG_TASK_START = 3;
    private static final int MSG_TASK_UPDATE = 2;
    private static final String TAG = "Gallery2/MenuExecutor";
    private final AbstractGalleryActivity mActivity;
    private ProgressDialog mDialog;
    private final Handler mHandler;
    private volatile boolean mHasCancelMultiOperation;
    private volatile boolean mIsMultiOperation;
    private boolean mIsOnDestory = false;
    private MediaOperation mMediaOperation = null;
    private boolean mPaused;
    private final SelectionManager mSelectionManager;
    private Future<?> mTask;
    private boolean mWaitOnStop;

    public interface ProgressListener {
        void onConfirmDialogDismissed(boolean z);

        void onConfirmDialogShown();

        void onProgressComplete(int i);

        void onProgressStart();

        void onProgressUpdate(int i);
    }

    private ProgressDialog createProgressDialog(Context context, int i, int i2) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(i);
        progressDialog.setMax(i2);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        if (this.mIsMultiOperation) {
            progressDialog.setButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i3) {
                    MenuExecutor.this.mHasCancelMultiOperation = true;
                }
            });
        }
        if (i2 > 1) {
            progressDialog.setProgressStyle(1);
        }
        return progressDialog;
    }

    public MenuExecutor(AbstractGalleryActivity abstractGalleryActivity, SelectionManager selectionManager) {
        this.mActivity = (AbstractGalleryActivity) Utils.checkNotNull(abstractGalleryActivity);
        this.mSelectionManager = (SelectionManager) Utils.checkNotNull(selectionManager);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        MenuExecutor.this.stopTaskAndDismissDialog(message.arg2);
                        if (message.obj != null) {
                            ((ProgressListener) message.obj).onProgressComplete(message.arg1);
                        }
                        MenuExecutor.this.mSelectionManager.leaveSelectionMode();
                        break;
                    case 2:
                        if (MenuExecutor.this.mDialog != null && !MenuExecutor.this.mPaused) {
                            MenuExecutor.this.mDialog.setProgress(message.arg1);
                        }
                        if (message.obj != null) {
                            ((ProgressListener) message.obj).onProgressUpdate(message.arg1);
                        }
                        break;
                    case 3:
                        if (message.obj != null) {
                            ((ProgressListener) message.obj).onProgressStart();
                        }
                        break;
                    case 4:
                        MenuExecutor.this.mActivity.startActivity((Intent) message.obj);
                        break;
                }
            }
        };
    }

    private void stopTaskAndDismissDialog(int i) {
        if (i != 0 && this.mMediaOperation != null && this.mMediaOperation.hashCode() != i) {
            Log.d(TAG, "<stopTaskAndDismissDialog> jobHashCode : " + i + ", mMediaOperation : " + this.mMediaOperation + ", mMediaOperation.hashCode() : " + this.mMediaOperation.hashCode());
            return;
        }
        if ((!this.mIsMultiOperation || this.mIsOnDestory) && this.mTask != null) {
            if (!this.mWaitOnStop) {
                this.mTask.cancel();
            }
            if (this.mDialog != null && this.mDialog.isShowing()) {
                try {
                    this.mDialog.dismiss();
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "While stopTaskAndDismissDialog catch :", e);
                }
            }
            this.mDialog = null;
            this.mTask = null;
        }
    }

    public void resume() {
        this.mPaused = false;
        if (this.mDialog != null) {
            this.mDialog.show();
        }
    }

    public void pause() {
        this.mPaused = true;
        if (this.mDialog == null || !this.mDialog.isShowing()) {
            return;
        }
        this.mDialog.hide();
    }

    public void destroy() {
        this.mIsOnDestory = true;
        stopTaskAndDismissDialog(0);
        this.mIsOnDestory = false;
    }

    private void onProgressUpdate(int i, ProgressListener progressListener) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, i, 0, progressListener));
    }

    private void onProgressStart(ProgressListener progressListener) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, progressListener));
    }

    private void onProgressComplete(int i, ProgressListener progressListener, int i2) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, i, i2, progressListener));
    }

    public static void updateMenuOperation(Menu menu, int i) {
        boolean z = (i & 1) != 0;
        boolean z2 = (i & 2) != 0;
        boolean z3 = (i & 8) != 0;
        boolean z4 = (i & 2048) != 0;
        boolean z5 = (65536 & i) != 0;
        boolean z6 = (i & 4) != 0;
        boolean z7 = (i & 32) != 0;
        boolean z8 = (i & 16) != 0;
        int i2 = i & 256;
        boolean z9 = (i & 512) != 0;
        boolean z10 = (i & SoOperater.STEP) != 0;
        boolean zSystemSupportsPrint = PrintHelper.systemSupportsPrint() & ((i & 131072) != 0);
        setMenuItemVisible(menu, R.id.action_delete, z);
        setMenuItemVisible(menu, R.id.action_rotate_ccw, z2);
        setMenuItemVisible(menu, R.id.action_rotate_cw, z2);
        setMenuItemVisible(menu, R.id.action_crop, z3);
        setMenuItemVisible(menu, R.id.action_trim, z4);
        setMenuItemVisible(menu, R.id.action_mute, z5);
        setMenuItemVisible(menu, R.id.action_share_panorama, false);
        setMenuItemVisible(menu, R.id.action_share, z6);
        setMenuItemVisible(menu, R.id.action_setas, z7);
        setMenuItemVisible(menu, R.id.action_show_on_map, z8);
        setMenuItemVisible(menu, R.id.action_edit, z9);
        setMenuItemVisible(menu, R.id.action_details, z10);
        setMenuItemVisible(menu, R.id.print, zSystemSupportsPrint);
    }

    public static void updateMenuForPanorama(Menu menu, boolean z, boolean z2) {
        setMenuItemVisible(menu, R.id.action_share_panorama, z);
        if (z2) {
            setMenuItemVisible(menu, R.id.action_rotate_ccw, false);
            setMenuItemVisible(menu, R.id.action_rotate_cw, false);
        }
    }

    private static void setMenuItemVisible(Menu menu, int i, boolean z) {
        MenuItem menuItemFindItem = menu.findItem(i);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(z);
        }
    }

    private Path getSingleSelectedPath() {
        ArrayList<Path> selected = this.mSelectionManager.getSelected(true);
        Utils.assertTrue(selected.size() == 1);
        return selected.get(0);
    }

    private Intent getIntentBySingleSelectedPath(String str) {
        DataManager dataManager = this.mActivity.getDataManager();
        Path singleSelectedPath = getSingleSelectedPath();
        return new Intent(str).setDataAndType(dataManager.getContentUri(singleSelectedPath), getMimeType(dataManager.getMediaType(singleSelectedPath)));
    }

    private void onMenuClicked(int i, ProgressListener progressListener) {
        onMenuClicked(i, progressListener, false, true);
    }

    public void onMenuClicked(int i, ProgressListener progressListener, boolean z, boolean z2) {
        int i2;
        if (i == R.id.action_select_all) {
            if (this.mSelectionManager.inSelectAllMode()) {
                this.mSelectionManager.deSelectAll();
                return;
            } else {
                this.mSelectionManager.selectAll();
                return;
            }
        }
        if (i != R.id.action_show_on_map) {
            switch (i) {
                case R.id.action_delete:
                    i2 = R.string.delete;
                    break;
                case R.id.action_edit:
                    Intent flags = getIntentBySingleSelectedPath("android.intent.action.EDIT").setFlags(1);
                    flags.setFlags(335544321);
                    this.mActivity.startActivity(Intent.createChooser(flags, null));
                    return;
                case R.id.action_rotate_ccw:
                    i2 = R.string.rotate_left;
                    break;
                case R.id.action_rotate_cw:
                    i2 = R.string.rotate_right;
                    break;
                case R.id.action_crop:
                    this.mActivity.startActivity(getIntentBySingleSelectedPath("com.android.camera.action.CROP"));
                    return;
                case R.id.action_setas:
                    Intent intentAddFlags = getIntentBySingleSelectedPath("android.intent.action.ATTACH_DATA").addFlags(1);
                    intentAddFlags.putExtra("mimeType", intentAddFlags.getType());
                    AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
                    abstractGalleryActivity.startActivity(Intent.createChooser(intentAddFlags, abstractGalleryActivity.getString(R.string.set_as)));
                    return;
                default:
                    return;
            }
        } else {
            i2 = R.string.show_on_map;
        }
        startAction(i, i2, progressListener, z, z2);
    }

    private class ConfirmDialogListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private final int mActionId;
        private final ProgressListener mListener;

        public ConfirmDialogListener(int i, ProgressListener progressListener) {
            this.mActionId = i;
            this.mListener = progressListener;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                if (this.mListener != null) {
                    this.mListener.onConfirmDialogDismissed(true);
                }
                MenuExecutor.this.onMenuClicked(this.mActionId, this.mListener);
            } else if (this.mListener != null) {
                this.mListener.onConfirmDialogDismissed(false);
            }
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            if (this.mListener != null) {
                this.mListener.onConfirmDialogDismissed(false);
            }
        }
    }

    public void onMenuClicked(MenuItem menuItem, String str, ProgressListener progressListener) {
        int itemId = menuItem.getItemId();
        if (str != null) {
            if (progressListener != null) {
                progressListener.onConfirmDialogShown();
            }
            ConfirmDialogListener confirmDialogListener = new ConfirmDialogListener(itemId, progressListener);
            new AlertDialog.Builder(this.mActivity.getAndroidContext()).setMessage(str).setOnCancelListener(confirmDialogListener).setPositiveButton(R.string.ok, confirmDialogListener).setNegativeButton(R.string.cancel, confirmDialogListener).create().show();
            return;
        }
        onMenuClicked(itemId, progressListener);
    }

    public void startAction(int i, int i2, ProgressListener progressListener) {
        startAction(i, i2, progressListener, false, true);
    }

    public void startAction(int i, int i2, ProgressListener progressListener, boolean z, boolean z2) {
        ArrayList<Path> selected = this.mSelectionManager.getSelected(false);
        stopTaskAndDismissDialog(0);
        this.mIsMultiOperation = selected.size() > 1;
        AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
        if (z2) {
            this.mDialog = createProgressDialog(abstractGalleryActivity, i2, selected.size());
            appendMessageForSingleId(this.mDialog, selected);
            this.mDialog.show();
        } else {
            this.mDialog = null;
        }
        this.mMediaOperation = new MediaOperation(i, selected, progressListener);
        this.mTask = this.mActivity.getBatchServiceThreadPoolIfAvailable().submit(this.mMediaOperation, null);
        this.mWaitOnStop = z;
    }

    public void startSingleItemAction(int i, Path path) {
        ArrayList arrayList = new ArrayList(1);
        arrayList.add(path);
        this.mDialog = null;
        this.mMediaOperation = new MediaOperation(i, arrayList, null);
        this.mTask = this.mActivity.getBatchServiceThreadPoolIfAvailable().submit(this.mMediaOperation, null);
        this.mWaitOnStop = false;
    }

    public static String getMimeType(int i) {
        if (i == 2) {
            return IMAGE_MIME;
        }
        if (i == 4) {
            return "video/*";
        }
        return "*/*";
    }

    private boolean execute(DataManager dataManager, ThreadPool.JobContext jobContext, int i, Path path) {
        Log.v(TAG, "Execute cmd: " + i + " for " + path);
        long jCurrentTimeMillis = System.currentTimeMillis();
        int i2 = 2;
        switch (i) {
            case R.id.action_toggle_full_caching:
                MediaObject mediaObject = dataManager.getMediaObject(path);
                if (mediaObject.getCacheFlag() == 2) {
                    i2 = 1;
                }
                mediaObject.cache(i2);
                break;
            case R.id.action_delete:
                dataManager.delete(path);
                break;
            case R.id.action_rotate_ccw:
                dataManager.rotate(path, -90);
                break;
            case R.id.action_rotate_cw:
                dataManager.rotate(path, 90);
                break;
            case R.id.action_show_on_map:
                double[] dArr = new double[2];
                ((MediaItem) dataManager.getMediaObject(path)).getLatLong(dArr);
                if (GalleryUtils.isValidLocation(dArr[0], dArr[1])) {
                    GalleryUtils.showOnMap(this.mActivity, dArr[0], dArr[1]);
                }
                break;
            default:
                throw new AssertionError();
        }
        Log.v(TAG, "It takes " + (System.currentTimeMillis() - jCurrentTimeMillis) + " ms to execute cmd for " + path);
        return true;
    }

    private class MediaOperation implements ThreadPool.Job<Void> {
        private final ArrayList<Path> mItems;
        private final ProgressListener mListener;
        private final int mOperation;

        public MediaOperation(int i, ArrayList<Path> arrayList, ProgressListener progressListener) {
            this.mOperation = i;
            this.mItems = arrayList;
            this.mListener = progressListener;
        }

        @Override
        public Void run(ThreadPool.JobContext jobContext) throws Throwable {
            ActivityState topState;
            int i;
            int i2;
            DataManager dataManager = MenuExecutor.this.mActivity.getDataManager();
            boolean z = this.mOperation == R.id.action_delete;
            if (MenuExecutor.this.mActivity.getStateManager().getStateCount() >= 1) {
                topState = MenuExecutor.this.mActivity.getStateManager().getTopState();
                topState.setProviderSensive(false);
            } else {
                topState = null;
            }
            try {
                try {
                    MenuExecutor.this.onProgressStart(this.mListener);
                    Iterator<Path> it = this.mItems.iterator();
                    i = 1;
                    int i3 = 0;
                    while (true) {
                        try {
                            i2 = 3;
                            if (!it.hasNext()) {
                                i2 = i;
                                break;
                            }
                            Path next = it.next();
                            if (jobContext.isCancelled() || MenuExecutor.this.mHasCancelMultiOperation) {
                                break;
                            }
                            if (z && "cluster".equals(next.getPrefix())) {
                                Log.w(MenuExecutor.TAG, "deleting cluster, use special logic for culster object!");
                                ClusterAlbumSet.setClusterDeleteOperation(true);
                            }
                            if (jobContext.isCancelled()) {
                                break;
                            }
                            if (!MenuExecutor.this.execute(dataManager, jobContext, this.mOperation, next)) {
                                i = 2;
                            }
                            if (!MenuExecutor.this.mPaused) {
                                int i4 = i3 + 1;
                                MenuExecutor.this.onProgressUpdate(i3, this.mListener);
                                i3 = i4;
                            }
                        } catch (Throwable th) {
                            th = th;
                            Log.e(MenuExecutor.TAG, "failed to execute operation " + this.mOperation + " : " + th);
                            if (z) {
                                boolean clusterDeleteOperation = ClusterAlbumSet.getClusterDeleteOperation();
                                ClusterAlbumSet.setClusterDeleteOperation(false);
                                if (clusterDeleteOperation) {
                                    Log.w(MenuExecutor.TAG, "deleting cluster complete, force reload all!");
                                    dataManager.forceRefreshAll();
                                }
                            }
                            MenuExecutor.this.mHasCancelMultiOperation = false;
                            MenuExecutor.this.mIsMultiOperation = false;
                            MenuExecutor.this.onProgressComplete(i, this.mListener, hashCode());
                            if (topState != null) {
                            }
                            return null;
                        }
                    }
                    if (z) {
                        boolean clusterDeleteOperation2 = ClusterAlbumSet.getClusterDeleteOperation();
                        ClusterAlbumSet.setClusterDeleteOperation(false);
                        if (clusterDeleteOperation2) {
                            Log.w(MenuExecutor.TAG, "deleting cluster complete, force reload all!");
                            dataManager.forceRefreshAll();
                        }
                    }
                    MenuExecutor.this.mHasCancelMultiOperation = false;
                    MenuExecutor.this.mIsMultiOperation = false;
                    MenuExecutor.this.onProgressComplete(i2, this.mListener, hashCode());
                } catch (Throwable th2) {
                    th = th2;
                    if (z) {
                        boolean clusterDeleteOperation3 = ClusterAlbumSet.getClusterDeleteOperation();
                        ClusterAlbumSet.setClusterDeleteOperation(false);
                        if (clusterDeleteOperation3) {
                            Log.w(MenuExecutor.TAG, "deleting cluster complete, force reload all!");
                            dataManager.forceRefreshAll();
                        }
                    }
                    MenuExecutor.this.mHasCancelMultiOperation = false;
                    MenuExecutor.this.mIsMultiOperation = false;
                    MenuExecutor.this.onProgressComplete(1, this.mListener, hashCode());
                    if (topState != null) {
                        topState.setProviderSensive(true);
                        topState.fakeProviderChange();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                if (z) {
                }
                MenuExecutor.this.mHasCancelMultiOperation = false;
                MenuExecutor.this.mIsMultiOperation = false;
                MenuExecutor.this.onProgressComplete(1, this.mListener, hashCode());
                if (topState != null) {
                }
                throw th;
            }
            if (topState != null) {
                topState.setProviderSensive(true);
                topState.fakeProviderChange();
            }
            return null;
        }
    }

    public static void updateSupportedMenuEnabled(Menu menu, int i, boolean z) {
        boolean z2 = (i & 1) != 0;
        boolean z3 = (i & 2) != 0;
        boolean z4 = (i & 8) != 0;
        boolean z5 = (i & 4) != 0;
        boolean z6 = (i & 32) != 0;
        boolean z7 = (i & 16) != 0;
        int i2 = i & 256;
        boolean z8 = (i & 512) != 0;
        boolean z9 = (i & SoOperater.STEP) != 0;
        int i3 = i & 131072;
        if (z2) {
            setMenuItemEnable(menu, R.id.action_delete, z);
        }
        if (z3) {
            setMenuItemEnable(menu, R.id.action_rotate_ccw, z);
            setMenuItemEnable(menu, R.id.action_rotate_cw, z);
        }
        if (z4) {
            setMenuItemEnable(menu, R.id.action_crop, z);
        }
        if (z5) {
            setMenuItemEnable(menu, R.id.action_share, z);
        }
        if (z6) {
            setMenuItemEnable(menu, R.id.action_setas, z);
        }
        if (z7) {
            setMenuItemEnable(menu, R.id.action_show_on_map, z);
        }
        if (z8) {
            setMenuItemEnable(menu, R.id.action_edit, z);
        }
        if (z9) {
            setMenuItemEnable(menu, R.id.action_details, z);
        }
    }

    private static void setMenuItemEnable(Menu menu, int i, boolean z) {
        MenuItem menuItemFindItem = menu.findItem(i);
        if (menuItemFindItem != null) {
            menuItemFindItem.setEnabled(z);
        }
    }

    private void printImageViaBT(Uri uri) {
        if (uri == null) {
            Log.d(TAG, "<printImageViaBT> uri is null, return!!!");
            return;
        }
        Intent intent = new Intent();
        intent.setAction(BT_PRINT_ACTION);
        intent.addCategory("android.intent.category.ALTERNATIVE");
        intent.setType(IMAGE_MIME);
        intent.putExtra("android.intent.extra.STREAM", uri);
        try {
            Log.d(TAG, "<printImageViaBT> uri: " + uri);
            this.mActivity.startActivity(Intent.createChooser(intent, this.mActivity.getText(R.string.print_image)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.mActivity, R.string.no_way_to_print, 0).show();
        }
    }

    private void appendMessageForSingleId(ProgressDialog progressDialog, ArrayList<Path> arrayList) {
        if (arrayList.size() == 1) {
            String name = null;
            ?? mediaObject = this.mActivity.getDataManager().getMediaObject(arrayList.get(0));
            if (mediaObject == 0) {
                return;
            }
            if (mediaObject instanceof MediaItem) {
                name = mediaObject.getName();
            } else if (mediaObject instanceof MediaSet) {
                name = mediaObject.getName();
            }
            if (name != null && this.mDialog != null) {
                this.mDialog.setMessage(name);
            }
        }
    }
}
