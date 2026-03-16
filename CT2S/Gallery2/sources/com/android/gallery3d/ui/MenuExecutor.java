package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;
import java.util.Iterator;

public class MenuExecutor {
    private final AbstractGalleryActivity mActivity;
    private ProgressDialog mDialog;
    private final Handler mHandler;
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

    private static ProgressDialog createProgressDialog(Context context, int titleId, int progressMax) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(titleId);
        dialog.setMax(progressMax);
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        if (progressMax > 1) {
            dialog.setProgressStyle(1);
        }
        return dialog;
    }

    public MenuExecutor(AbstractGalleryActivity activity, SelectionManager selectionManager) {
        this.mActivity = (AbstractGalleryActivity) Utils.checkNotNull(activity);
        this.mSelectionManager = (SelectionManager) Utils.checkNotNull(selectionManager);
        this.mHandler = new SynchronizedHandler(this.mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        MenuExecutor.this.stopTaskAndDismissDialog();
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressComplete(message.arg1);
                        }
                        MenuExecutor.this.mSelectionManager.leaveSelectionMode();
                        break;
                    case 2:
                        if (MenuExecutor.this.mDialog != null && !MenuExecutor.this.mPaused) {
                            MenuExecutor.this.mDialog.setProgress(message.arg1);
                        }
                        if (message.obj != null) {
                            ProgressListener listener2 = (ProgressListener) message.obj;
                            listener2.onProgressUpdate(message.arg1);
                        }
                        break;
                    case 3:
                        if (message.obj != null) {
                            ProgressListener listener3 = (ProgressListener) message.obj;
                            listener3.onProgressStart();
                        }
                        break;
                    case 4:
                        MenuExecutor.this.mActivity.startActivity((Intent) message.obj);
                        break;
                }
            }
        };
    }

    private void stopTaskAndDismissDialog() {
        if (this.mTask != null) {
            if (!this.mWaitOnStop) {
                this.mTask.cancel();
            }
            if (this.mDialog != null && this.mDialog.isShowing()) {
                this.mDialog.dismiss();
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
        stopTaskAndDismissDialog();
    }

    private void onProgressUpdate(int index, ProgressListener listener) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, index, 0, listener));
    }

    private void onProgressStart(ProgressListener listener) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, listener));
    }

    private void onProgressComplete(int result, ProgressListener listener) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, result, 0, listener));
    }

    public static void updateMenuOperation(Menu menu, int supported) {
        boolean supportDelete = (supported & 1) != 0;
        boolean supportRotate = (supported & 2) != 0;
        boolean supportCrop = (supported & 8) != 0;
        boolean supportTrim = (supported & 2048) != 0;
        boolean supportMute = (65536 & supported) != 0;
        boolean supportShare = (supported & 4) != 0;
        boolean supportSetAs = (supported & 32) != 0;
        boolean supportShowOnMap = (supported & 16) != 0;
        if ((supported & NotificationCompat.FLAG_LOCAL_ONLY) != 0) {
        }
        boolean supportEdit = (supported & NotificationCompat.FLAG_GROUP_SUMMARY) != 0;
        boolean supportInfo = (supported & 1024) != 0;
        boolean supportPrint = (131072 & supported) != 0;
        boolean supportPrint2 = supportPrint & PrintHelper.systemSupportsPrint();
        setMenuItemVisible(menu, R.id.action_delete, supportDelete);
        setMenuItemVisible(menu, R.id.action_rotate_ccw, supportRotate);
        setMenuItemVisible(menu, R.id.action_rotate_cw, supportRotate);
        setMenuItemVisible(menu, R.id.action_crop, supportCrop);
        setMenuItemVisible(menu, R.id.action_trim, supportTrim);
        setMenuItemVisible(menu, R.id.action_mute, supportMute);
        setMenuItemVisible(menu, R.id.action_share_panorama, false);
        setMenuItemVisible(menu, R.id.action_share, supportShare);
        setMenuItemVisible(menu, R.id.action_setas, supportSetAs);
        setMenuItemVisible(menu, R.id.action_show_on_map, supportShowOnMap);
        setMenuItemVisible(menu, R.id.action_edit, supportEdit);
        setMenuItemVisible(menu, R.id.action_details, supportInfo);
        setMenuItemVisible(menu, R.id.print, supportPrint2);
    }

    public static void updateMenuForPanorama(Menu menu, boolean shareAsPanorama360, boolean disablePanorama360Options) {
        setMenuItemVisible(menu, R.id.action_share_panorama, shareAsPanorama360);
        if (disablePanorama360Options) {
            setMenuItemVisible(menu, R.id.action_rotate_ccw, false);
            setMenuItemVisible(menu, R.id.action_rotate_cw, false);
        }
    }

    private static void setMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    private Path getSingleSelectedPath() {
        ArrayList<Path> ids = this.mSelectionManager.getSelected(true);
        Utils.assertTrue(ids.size() == 1);
        return ids.get(0);
    }

    private Intent getIntentBySingleSelectedPath(String action) {
        DataManager manager = this.mActivity.getDataManager();
        Path path = getSingleSelectedPath();
        String mimeType = getMimeType(manager.getMediaType(path));
        return new Intent(action).setDataAndType(manager.getContentUri(path), mimeType);
    }

    private void onMenuClicked(int action, ProgressListener listener) {
        onMenuClicked(action, listener, false, true);
    }

    public void onMenuClicked(int action, ProgressListener listener, boolean waitOnStop, boolean showDialog) {
        int title;
        switch (action) {
            case R.id.action_select_all:
                if (this.mSelectionManager.inSelectAllMode()) {
                    this.mSelectionManager.deSelectAll();
                    return;
                } else {
                    this.mSelectionManager.selectAll();
                    return;
                }
            case R.id.action_delete:
                title = R.string.delete;
                break;
            case R.id.action_edit:
                this.mActivity.startActivity(Intent.createChooser(getIntentBySingleSelectedPath("android.intent.action.EDIT").setFlags(1), null));
                return;
            case R.id.action_rotate_ccw:
                title = R.string.rotate_left;
                break;
            case R.id.action_rotate_cw:
                title = R.string.rotate_right;
                break;
            case R.id.action_crop:
                this.mActivity.startActivity(getIntentBySingleSelectedPath("com.android.camera.action.CROP"));
                return;
            case R.id.action_setas:
                Intent intent = getIntentBySingleSelectedPath("android.intent.action.ATTACH_DATA").addFlags(1);
                intent.putExtra("mimeType", intent.getType());
                Activity activity = this.mActivity;
                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.set_as)));
                return;
            case R.id.action_show_on_map:
                title = R.string.show_on_map;
                break;
            default:
                return;
        }
        startAction(action, title, listener, waitOnStop, showDialog);
    }

    private class ConfirmDialogListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private final int mActionId;
        private final ProgressListener mListener;

        public ConfirmDialogListener(int actionId, ProgressListener listener) {
            this.mActionId = actionId;
            this.mListener = listener;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                if (this.mListener != null) {
                    this.mListener.onConfirmDialogDismissed(true);
                }
                MenuExecutor.this.onMenuClicked(this.mActionId, this.mListener);
            } else if (this.mListener != null) {
                this.mListener.onConfirmDialogDismissed(false);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (this.mListener != null) {
                this.mListener.onConfirmDialogDismissed(false);
            }
        }
    }

    public void onMenuClicked(MenuItem menuItem, String confirmMsg, ProgressListener listener) {
        int action = menuItem.getItemId();
        if (confirmMsg != null) {
            if (listener != null) {
                listener.onConfirmDialogShown();
            }
            ConfirmDialogListener cdl = new ConfirmDialogListener(action, listener);
            new AlertDialog.Builder(this.mActivity.getAndroidContext()).setMessage(confirmMsg).setOnCancelListener(cdl).setPositiveButton(R.string.ok, cdl).setNegativeButton(R.string.cancel, cdl).create().show();
            return;
        }
        onMenuClicked(action, listener);
    }

    public void startAction(int action, int title, ProgressListener listener) {
        startAction(action, title, listener, false, true);
    }

    public void startAction(int action, int title, ProgressListener listener, boolean waitOnStop, boolean showDialog) {
        ArrayList<Path> ids = this.mSelectionManager.getSelected(false);
        stopTaskAndDismissDialog();
        Activity activity = this.mActivity;
        if (showDialog) {
            this.mDialog = createProgressDialog(activity, title, ids.size());
            this.mDialog.show();
        } else {
            this.mDialog = null;
        }
        MediaOperation operation = new MediaOperation(action, ids, listener);
        this.mTask = this.mActivity.getBatchServiceThreadPoolIfAvailable().submit(operation, null);
        this.mWaitOnStop = waitOnStop;
    }

    public void startSingleItemAction(int action, Path targetPath) {
        ArrayList<Path> ids = new ArrayList<>(1);
        ids.add(targetPath);
        this.mDialog = null;
        MediaOperation operation = new MediaOperation(action, ids, null);
        this.mTask = this.mActivity.getBatchServiceThreadPoolIfAvailable().submit(operation, null);
        this.mWaitOnStop = false;
    }

    public static String getMimeType(int type) {
        switch (type) {
            case 2:
                return "image/*";
            case 3:
            default:
                return "*/*";
            case 4:
                return "video/*";
        }
    }

    private boolean execute(DataManager manager, ThreadPool.JobContext jc, int cmd, Path path) {
        int cacheFlag;
        Log.v("MenuExecutor", "Execute cmd: " + cmd + " for " + path);
        long startTime = System.currentTimeMillis();
        switch (cmd) {
            case R.id.action_toggle_full_caching:
                MediaObject obj = manager.getMediaObject(path);
                int cacheFlag2 = obj.getCacheFlag();
                if (cacheFlag2 == 2) {
                    cacheFlag = 1;
                } else {
                    cacheFlag = 2;
                }
                obj.cache(cacheFlag);
                break;
            case R.id.action_delete:
                manager.delete(path);
                break;
            case R.id.action_rotate_ccw:
                manager.rotate(path, -90);
                break;
            case R.id.action_rotate_cw:
                manager.rotate(path, 90);
                break;
            case R.id.action_show_on_map:
                MediaItem item = (MediaItem) manager.getMediaObject(path);
                double[] latlng = new double[2];
                item.getLatLong(latlng);
                if (GalleryUtils.isValidLocation(latlng[0], latlng[1])) {
                    GalleryUtils.showOnMap(this.mActivity, latlng[0], latlng[1]);
                }
                break;
            default:
                throw new AssertionError();
        }
        Log.v("MenuExecutor", "It takes " + (System.currentTimeMillis() - startTime) + " ms to execute cmd for " + path);
        return true;
    }

    private class MediaOperation implements ThreadPool.Job<Void> {
        private final ArrayList<Path> mItems;
        private final ProgressListener mListener;
        private final int mOperation;

        public MediaOperation(int operation, ArrayList<Path> items, ProgressListener listener) {
            this.mOperation = operation;
            this.mItems = items;
            this.mListener = listener;
        }

        @Override
        public Void run(ThreadPool.JobContext jc) throws Throwable {
            Iterator<Path> it;
            int index;
            int index2 = 0;
            DataManager manager = MenuExecutor.this.mActivity.getDataManager();
            int result = 1;
            try {
                try {
                    MenuExecutor.this.onProgressStart(this.mListener);
                    it = this.mItems.iterator();
                } catch (Throwable th) {
                    th = th;
                }
                while (true) {
                    try {
                        index = index2;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                    if (!it.hasNext()) {
                        break;
                    }
                    Path id = it.next();
                    if (!jc.isCancelled()) {
                        if (!MenuExecutor.this.execute(manager, jc, this.mOperation, id)) {
                            result = 2;
                        }
                        index2 = index + 1;
                        MenuExecutor.this.onProgressUpdate(index, this.mListener);
                    } else {
                        result = 3;
                        break;
                    }
                    Log.e("MenuExecutor", "failed to execute operation " + this.mOperation + " : " + th);
                    MenuExecutor.this.onProgressComplete(result, this.mListener);
                    return null;
                }
                MenuExecutor.this.onProgressComplete(result, this.mListener);
                return null;
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }
}
