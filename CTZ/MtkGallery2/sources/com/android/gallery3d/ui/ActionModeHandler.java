package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.PopupList;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.JobLimiter;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallerybasic.base.IActionMode;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.galleryportable.ActivityChooserModelWrapper;
import com.mediatek.galleryportable.ActivityChooserViewWrapper;
import java.util.ArrayList;
import java.util.Iterator;

public class ActionModeHandler implements ActionMode.Callback, PopupList.OnPopupItemClickListener {
    private static final String INTENT_MORE_THAN_MAX = "more than max";
    private static final String INTENT_NOT_READY = "intent not ready";
    private static final int MAX_SELECTED_ITEMS_FOR_PANORAMA_SHARE_INTENT = 10;
    private static final int MAX_SELECTED_ITEMS_FOR_SHARE_INTENT = 300;
    private static final int SHARE_URI_SIZE_LIMITATION = 30000;
    private static final int SUPPORT_MULTIPLE_MASK = 263;
    private static final String TAG = "Gallery2/ActionModeHandler";
    private ActionMode mActionMode;
    private IActionMode[] mActionModeExt;
    private final AbstractGalleryActivity mActivity;
    private ActivityChooserViewWrapper mActivityChooserView;
    private JobLimiter mComputerShareItemsJobLimiter;
    private MediaData[] mCurrentSelectDatas;
    private ActivityChooserModelWrapper mDataModel;
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
    private Toast mWaitToast = null;
    private ActivityChooserModelWrapper.OnChooseActivityListenerWrapper mChooseActivityListener = new ActivityChooserModelWrapper.OnChooseActivityListenerWrapper() {
        @Override
        public boolean onChooseActivity(ActivityChooserModelWrapper activityChooserModelWrapper, Intent intent) {
            Log.i(ActionModeHandler.TAG, "<onChooseActivity> intent=" + intent);
            if (ActionModeHandler.this.isNotReadyIntent(intent)) {
                ActionModeHandler.this.showWaitToast();
                Log.i(ActionModeHandler.TAG, "<onChooseActivity> still not ready, wait!");
                return true;
            }
            if (ActionModeHandler.this.isMoreThanMaxIntent(intent)) {
                Toast.makeText(ActionModeHandler.this.mActivity, R.string.share_limit, 0).show();
                Log.i(ActionModeHandler.TAG, "<onChooseActivity> shared too many item, abort!");
                return true;
            }
            Log.i(ActionModeHandler.TAG, "<onChooseActivity> start share");
            ActionModeHandler.this.mActivity.startActivity(intent);
            if (intent.getComponent() == null || intent.getComponent().getPackageName().indexOf("nfc") == -1) {
                ActionModeHandler.this.mSelectionManager.leaveSelectionMode();
            }
            return true;
        }
    };
    private Intent mIntent = null;

    public interface ActionModeListener {
        boolean onActionItemClicked(MenuItem menuItem);

        boolean onPopUpItemClicked(int i);
    }

    private static class GetAllPanoramaSupports implements MediaObject.PanoramaSupportCallback {
        private ThreadPool.JobContext mJobContext;
        private int mNumInfoRequired;
        public boolean mAllPanoramas = true;
        public boolean mAllPanorama360 = true;
        public boolean mHasPanorama360 = false;
        private Object mLock = new Object();

        public GetAllPanoramaSupports(ArrayList<MediaObject> arrayList, ThreadPool.JobContext jobContext) {
            this.mJobContext = jobContext;
            this.mNumInfoRequired = arrayList.size();
            Iterator<MediaObject> it = arrayList.iterator();
            while (it.hasNext()) {
                it.next().getPanoramaSupport(this);
            }
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2) {
            synchronized (this.mLock) {
                this.mNumInfoRequired--;
                boolean z3 = false;
                this.mAllPanoramas = z && this.mAllPanoramas;
                this.mAllPanorama360 = z2 && this.mAllPanorama360;
                if (this.mHasPanorama360 || z2) {
                    z3 = true;
                }
                this.mHasPanorama360 = z3;
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

    public ActionModeHandler(AbstractGalleryActivity abstractGalleryActivity, SelectionManager selectionManager) {
        this.mActivity = (AbstractGalleryActivity) Utils.checkNotNull(abstractGalleryActivity);
        this.mSelectionManager = (SelectionManager) Utils.checkNotNull(selectionManager);
        this.mMenuExecutor = new MenuExecutor(abstractGalleryActivity, selectionManager);
        this.mMainHandler = new Handler(abstractGalleryActivity.getMainLooper());
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this.mActivity.getAndroidContext());
    }

    public void startActionMode() {
        AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
        this.mActionMode = abstractGalleryActivity.startActionMode(this);
        View viewInflate = LayoutInflater.from(abstractGalleryActivity).inflate(R.layout.action_mode, (ViewGroup) null);
        this.mActionMode.setCustomView(viewInflate);
        this.mSelectionMenu = new SelectionMenu(abstractGalleryActivity, (Button) viewInflate.findViewById(R.id.selection_menu), this);
        updateSelectionMenu();
    }

    public void finishActionMode() {
        this.mActionMode.finish();
        if (this.mMenuTask != null) {
            this.mMenuTask.cancel();
            this.mMenuTask = null;
        }
        setNfcBeamPushUris(null);
    }

    public void setTitle(String str) {
        this.mSelectionMenu.setTitle(str);
    }

    public void setActionModeListener(ActionModeListener actionModeListener) {
        this.mListener = actionModeListener;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        WakeLockHoldingProgressListener wakeLockHoldingProgressListener;
        GLRoot gLRoot = this.mActivity.getGLRoot();
        gLRoot.lockRenderThread();
        try {
            if (this.mListener != null) {
                boolean zOnActionItemClicked = this.mListener.onActionItemClicked(menuItem);
                if (zOnActionItemClicked) {
                    this.mSelectionManager.leaveSelectionMode();
                    return zOnActionItemClicked;
                }
                for (IActionMode iActionMode : this.mActionModeExt) {
                    boolean zOnActionItemClicked2 = iActionMode.onActionItemClicked(actionMode, menuItem);
                    if (zOnActionItemClicked2) {
                        this.mSelectionManager.leaveSelectionMode();
                        return zOnActionItemClicked2;
                    }
                }
            }
            String quantityString = null;
            if (menuItem.getItemId() == R.id.action_delete) {
                quantityString = this.mActivity.getResources().getQuantityString(R.plurals.delete_selection, this.mSelectionManager.getSelectedCount());
                if (this.mDeleteProgressListener == null) {
                    this.mDeleteProgressListener = new WakeLockHoldingProgressListener(this.mActivity, "Gallery Delete Progress Listener");
                }
                wakeLockHoldingProgressListener = this.mDeleteProgressListener;
            } else {
                wakeLockHoldingProgressListener = null;
            }
            this.mMenuExecutor.onMenuClicked(menuItem, quantityString, wakeLockHoldingProgressListener);
            gLRoot.unlockRenderThread();
            return true;
        } finally {
            gLRoot.unlockRenderThread();
        }
    }

    @Override
    public boolean onPopupItemClick(int i) {
        GLRoot gLRoot = this.mActivity.getGLRoot();
        gLRoot.lockRenderThread();
        if (i == R.id.action_select_all) {
            try {
                if (this.mListener.onPopUpItemClicked(i)) {
                    this.mMenuExecutor.onMenuClicked(i, null, false, true);
                    updateSupportedOperation();
                    updateSelectionMenu();
                } else {
                    if (this.mWaitToast == null) {
                        this.mWaitToast = Toast.makeText(this.mActivity, R.string.wait, 0);
                    }
                    this.mWaitToast.show();
                }
            } finally {
                gLRoot.unlockRenderThread();
            }
        }
        return true;
    }

    public void updateSelectionMenu() {
        AlbumSetPage topState;
        int selectedCount = this.mSelectionManager.getSelectedCount();
        if (this.mActivity.getStateManager().getStateCount() != 0) {
            topState = this.mActivity.getStateManager().getTopState();
        } else {
            topState = 0;
        }
        setTitle((topState == 0 || !(topState instanceof AlbumSetPage)) ? String.format(this.mActivity.getResources().getQuantityString(R.plurals.number_of_items_selected, selectedCount), Integer.valueOf(selectedCount)) : topState.getSelectedString());
        this.mSelectionMenu.updateSelectAllMode(this.mSelectionManager.inSelectAllMode());
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        for (IActionMode iActionMode : this.mActionModeExt) {
            iActionMode.onPrepareActionMode(actionMode, menu);
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.operation, menu);
        if (this.mActionModeExt == null) {
            this.mActionModeExt = (IActionMode[]) FeatureManager.getInstance().getImplement(IActionMode.class, this.mActivity);
        }
        for (IActionMode iActionMode : this.mActionModeExt) {
            iActionMode.onCreateActionMode(actionMode, menu);
        }
        this.mMenu = menu;
        this.mShareMenuItem = menu.findItem(R.id.action_share);
        if (this.mShareMenuItem != null) {
            this.mShareActionProvider = (ShareActionProvider) this.mShareMenuItem.getActionProvider();
            this.mActivityChooserView = new ActivityChooserViewWrapper(this.mShareMenuItem);
            this.mShareActionProvider.setShareHistoryFileName("share_history.xml");
            this.mDataModel = new ActivityChooserModelWrapper(this.mActivity, "share_history.xml");
            if (this.mDataModel != null) {
                this.mDataModel.setOnChooseActivityListener(this.mChooseActivityListener);
            }
        }
        this.mActivity.getTheme().resolveAttribute(this.mActivity.getResources().getIdentifier("actionModeShareDrawable", "attr", "com.android.internal"), new TypedValue(), true);
        this.mActivityChooserView.setExpandActivityOverflowButtonDrawable(this.mActivity.getApplicationContext().getResources().getDrawable(R.drawable.ic_menu_share_holo_light));
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        for (IActionMode iActionMode : this.mActionModeExt) {
            iActionMode.onDestroyActionMode(actionMode);
        }
        this.mSelectionManager.leaveSelectionMode();
        if (this.mSelectionMenu != null) {
            this.mSelectionMenu.finish();
        }
    }

    private ArrayList<MediaObject> getSelectedMediaObjects(ThreadPool.JobContext jobContext) {
        ArrayList<Path> selected = this.mSelectionManager.getSelected(false);
        if (selected.isEmpty()) {
            return null;
        }
        ArrayList<MediaObject> arrayList = new ArrayList<>();
        DataManager dataManager = this.mActivity.getDataManager();
        for (Path path : selected) {
            if (jobContext.isCancelled()) {
                return null;
            }
            arrayList.add(dataManager.getMediaObject(path));
        }
        return arrayList;
    }

    private int computeMenuOptions(ArrayList<MediaObject> arrayList) {
        int mediaType = 0;
        if (arrayList == null) {
            return 0;
        }
        int i = -1;
        for (MediaObject mediaObject : arrayList) {
            int supportedOperations = mediaObject.getSupportedOperations();
            mediaType |= mediaObject.getMediaType();
            i &= supportedOperations;
        }
        if (arrayList.size() == 1) {
            if (!GalleryUtils.isEditorAvailable(this.mActivity, MenuExecutor.getMimeType(mediaType))) {
                return i & (-513);
            }
            return i;
        }
        return i & SUPPORT_MULTIPLE_MASK;
    }

    @TargetApi(16)
    private void setNfcBeamPushUris(Uri[] uriArr) {
        if (this.mNfcAdapter != null && !this.mActivity.isDestroyed() && ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
            this.mNfcAdapter.setBeamPushUrisCallback(null, this.mActivity);
            this.mNfcAdapter.setBeamPushUris(uriArr, this.mActivity);
            if (uriArr == null) {
                String packageName = this.mActivity.getPackageName();
                this.mNfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[]{NdefRecord.createUri(Uri.parse("http://play.google.com/store/apps/details?id=" + packageName + "&feature=beam")), NdefRecord.createApplicationRecord(packageName)}), this.mActivity, new Activity[0]);
            }
        }
    }

    private Intent computePanoramaSharingIntent(ThreadPool.JobContext jobContext, int i) {
        ArrayList<Path> selected = this.mSelectionManager.getSelected(true, i);
        if (selected == null || selected.size() == 0) {
            return new Intent();
        }
        ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
        DataManager dataManager = this.mActivity.getDataManager();
        Intent intent = new Intent();
        for (Path path : selected) {
            if (jobContext.isCancelled()) {
                return null;
            }
            arrayList.add(dataManager.getContentUri(path));
        }
        int size = arrayList.size();
        if (size > 0) {
            if (size > 1) {
                intent.setAction("android.intent.action.SEND_MULTIPLE");
                intent.setType("application/vnd.google.panorama360+jpg");
                intent.putParcelableArrayListExtra("android.intent.extra.STREAM", arrayList);
            } else {
                intent.setAction("android.intent.action.SEND");
                intent.setType("application/vnd.google.panorama360+jpg");
                intent.putExtra("android.intent.extra.STREAM", arrayList.get(0));
            }
            intent.addFlags(1);
        }
        return intent;
    }

    private Intent computeSharingIntent(ThreadPool.JobContext jobContext, int i) {
        ArrayList<Path> selected = this.mSelectionManager.getSelected(jobContext, true, i);
        if (jobContext.isCancelled()) {
            Log.d(TAG, "<computeSharingIntent> jc.isCancelled() - 1");
            return null;
        }
        if (selected == null) {
            setNfcBeamPushUris(null);
            Log.d(TAG, "<computeSharingIntent> selected items exceeds max number!");
            return createMoreThanMaxIntent();
        }
        if (selected == null || selected.size() == 0) {
            setNfcBeamPushUris(null);
            return new Intent();
        }
        setCurrentSelectDatas(selected);
        for (IActionMode iActionMode : this.mActionModeExt) {
            iActionMode.onSelectionChange(this.mCurrentSelectDatas);
        }
        ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
        DataManager dataManager = this.mActivity.getDataManager();
        Intent intent = new Intent();
        Iterator<Path> it = selected.iterator();
        int length = 0;
        int mediaType = 0;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Path next = it.next();
            if (jobContext.isCancelled()) {
                Log.d(TAG, "<computeSharingIntent> jc.isCancelled() - 2");
                return null;
            }
            if ((dataManager.getSupportedOperations(next) & 4) != 0) {
                arrayList.add(dataManager.getContentUri(next));
                length += dataManager.getContentUri(next).toString().length();
                mediaType |= dataManager.getMediaType(next);
            }
            if (length > SHARE_URI_SIZE_LIMITATION) {
                Log.d(TAG, "<computeSharingIntent> totalUriSize > SHARE_URI_SIZE_LIMITATION");
                break;
            }
        }
        int size = arrayList.size();
        Log.d(TAG, "<computeSharingIntent> total share items = " + size);
        if (size > 0) {
            String mimeType = MenuExecutor.getMimeType(mediaType);
            if (size > 1) {
                intent.setAction("android.intent.action.SEND_MULTIPLE").setType(mimeType);
                intent.putParcelableArrayListExtra("android.intent.extra.STREAM", arrayList);
            } else {
                intent.setAction("android.intent.action.SEND").setType(mimeType);
                intent.putExtra("android.intent.extra.STREAM", arrayList.get(0));
            }
            intent.addFlags(1);
            setNfcBeamPushUris((Uri[]) arrayList.toArray(new Uri[arrayList.size()]));
            return intent;
        }
        setNfcBeamPushUris(null);
        return null;
    }

    public void updateSupportedOperation(Path path, boolean z) {
        updateSupportedOperation();
    }

    public void updateSupportedOperation() {
        if (this.mMenuTask != null && !this.mMenuTask.isDone()) {
            this.mMenuTask.cancel();
        }
        updateSelectionMenu();
        if (this.mSharePanoramaMenuItem != null) {
            this.mSharePanoramaMenuItem.setEnabled(false);
        }
        if (this.mShareMenuItem != null) {
            this.mShareMenuItem.setEnabled(false);
        }
        if (this.mShareActionProvider != null) {
            setShareIntent(createNotReadyIntent());
        }
        if (this.mComputerShareItemsJobLimiter == null) {
            this.mComputerShareItemsJobLimiter = new JobLimiter(this.mActivity.getThreadPool(), 1);
        }
        this.mMenuTask = this.mComputerShareItemsJobLimiter.submit(new ThreadPool.Job<Void>() {
            @Override
            public Void run(final ThreadPool.JobContext jobContext) {
                ActionModeHandler.this.mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!jobContext.isCancelled()) {
                            MenuExecutor.updateSupportedMenuEnabled(ActionModeHandler.this.mMenu, -1, false);
                        }
                    }
                });
                final ArrayList selectedMediaObjects = ActionModeHandler.this.getSelectedMediaObjects(jobContext);
                if (selectedMediaObjects == null) {
                    ActionModeHandler.this.mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (jobContext.isCancelled()) {
                                return;
                            }
                            MenuExecutor.updateMenuOperation(ActionModeHandler.this.mMenu, 0);
                        }
                    });
                    Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> selected == null, task done, return");
                    return null;
                }
                final int iComputeMenuOptions = ActionModeHandler.this.computeMenuOptions(selectedMediaObjects);
                if (jobContext.isCancelled()) {
                    Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> task is cancelled after computeMenuOptions, return");
                    return null;
                }
                final boolean z = (iComputeMenuOptions & 4) != 0;
                ActionModeHandler.this.mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!jobContext.isCancelled()) {
                            MenuExecutor.updateMenuOperation(ActionModeHandler.this.mMenu, iComputeMenuOptions);
                            MenuExecutor.updateSupportedMenuEnabled(ActionModeHandler.this.mMenu, -1, true);
                            if (ActionModeHandler.this.mShareMenuItem != null) {
                                if (selectedMediaObjects == null || selectedMediaObjects.size() == 0 || !z) {
                                    ActionModeHandler.this.mShareMenuItem.setShowAsAction(1);
                                    ActionModeHandler.this.mShareMenuItem.setEnabled(false);
                                    ActionModeHandler.this.mShareMenuItem.setVisible(false);
                                    ActionModeHandler.this.setShareIntent(null);
                                    return;
                                }
                                ActionModeHandler.this.mShareMenuItem.setShowAsAction(1);
                                ActionModeHandler.this.mShareMenuItem.setEnabled(false);
                                ActionModeHandler.this.mShareMenuItem.setVisible(true);
                                if (selectedMediaObjects.size() <= ActionModeHandler.MAX_SELECTED_ITEMS_FOR_SHARE_INTENT) {
                                    ActionModeHandler.this.setShareIntent(ActionModeHandler.this.createNotReadyIntent());
                                }
                            }
                        }
                    }
                });
                if (ActionModeHandler.this.mShareMenuItem == null || selectedMediaObjects == null || selectedMediaObjects.size() == 0) {
                    return null;
                }
                int size = selectedMediaObjects.size();
                boolean z2 = size < 10;
                boolean z3 = size <= ActionModeHandler.MAX_SELECTED_ITEMS_FOR_SHARE_INTENT;
                if (z2) {
                    new GetAllPanoramaSupports(selectedMediaObjects, jobContext);
                }
                if (z2) {
                    ActionModeHandler.this.computePanoramaSharingIntent(jobContext, 10);
                } else {
                    new Intent();
                }
                Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> computeSharingIntent begin");
                final Intent intentComputeSharingIntent = z3 ? ActionModeHandler.this.computeSharingIntent(jobContext, ActionModeHandler.MAX_SELECTED_ITEMS_FOR_SHARE_INTENT) : ActionModeHandler.this.createMoreThanMaxIntent();
                Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> computeSharingIntent end");
                if (!jobContext.isCancelled()) {
                    ActionModeHandler.this.mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (jobContext.isCancelled()) {
                                return;
                            }
                            ActionModeHandler.this.mShareMenuItem.setShowAsAction(1);
                            if (intentComputeSharingIntent != null) {
                                ActionModeHandler.this.mShareMenuItem.setEnabled(true);
                                ActionModeHandler.this.mShareMenuItem.setVisible(true);
                            } else {
                                ActionModeHandler.this.mShareMenuItem.setEnabled(false);
                                ActionModeHandler.this.mShareMenuItem.setVisible(false);
                            }
                            ActionModeHandler.this.setShareIntent(intentComputeSharingIntent);
                            if (iComputeMenuOptions == 0 || iComputeMenuOptions == 1 || iComputeMenuOptions == 5) {
                                Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> close menu, operation " + iComputeMenuOptions);
                                ActionModeHandler.this.closeMenu();
                            }
                        }
                    });
                    Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> task done, return");
                    return null;
                }
                Log.d(ActionModeHandler.TAG, "<updateSupportedOperation> task is cancelled after computeSharingIntent, return");
                return null;
            }
        }, null);
    }

    public void pause() {
        if (this.mMenuTask != null) {
            this.mMenuTask.cancel();
            this.mMenuTask = null;
        }
        this.mMenuExecutor.pause();
        if (this.mSelectionManager != null && this.mSelectionManager.inSelectionMode()) {
            MenuExecutor.updateSupportedMenuEnabled(this.mMenu, -1, false);
        }
    }

    public void destroy() {
        this.mMenuExecutor.destroy();
    }

    public void resume() {
        if (this.mSelectionManager.inSelectionMode()) {
            updateSupportedOperation();
        } else {
            setNfcBeamPushUris(null);
        }
        if (this.mDataModel != null) {
            this.mDataModel.setOnChooseActivityListener(this.mChooseActivityListener);
        }
        this.mMenuExecutor.resume();
    }

    private void showWaitToast() {
        if (this.mWaitToast == null) {
            this.mWaitToast = Toast.makeText(this.mActivity, R.string.wait, 0);
        }
        this.mWaitToast.show();
    }

    private Intent createMoreThanMaxIntent() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND_MULTIPLE").setType("*/*");
        intent.putExtra(INTENT_MORE_THAN_MAX, true);
        return intent;
    }

    private boolean isMoreThanMaxIntent(Intent intent) {
        return intent.getExtras() != null && intent.getExtras().getBoolean(INTENT_MORE_THAN_MAX, false);
    }

    private Intent createNotReadyIntent() {
        Intent intent = this.mIntent;
        if (intent == null) {
            intent = new Intent();
            intent.setAction("android.intent.action.SEND_MULTIPLE").setType("*/*");
        }
        intent.putExtra(INTENT_NOT_READY, true);
        return intent;
    }

    private boolean isNotReadyIntent(Intent intent) {
        return intent.getExtras() != null && intent.getExtras().getBoolean(INTENT_NOT_READY, false);
    }

    public void closeMenu() {
        if (this.mMenu != null) {
            this.mMenu.close();
        }
    }

    private void setShareIntent(Intent intent) {
        if (intent != null) {
            this.mShareActionProvider.setShareIntent(intent);
            this.mIntent = (Intent) intent.clone();
        } else {
            this.mIntent = null;
        }
    }

    private void setCurrentSelectDatas(ArrayList<Path> arrayList) {
        MediaData mediaData;
        ArrayList arrayList2 = new ArrayList();
        DataManager dataManager = this.mActivity.getDataManager();
        Iterator<Path> it = arrayList.iterator();
        while (it.hasNext()) {
            ?? mediaObject = dataManager.getMediaObject(it.next());
            if ((mediaObject instanceof MediaItem) && (mediaData = mediaObject.getMediaData()) != null) {
                arrayList2.add(mediaData);
            }
        }
        this.mCurrentSelectDatas = new MediaData[arrayList2.size()];
        arrayList2.toArray(this.mCurrentSelectDatas);
    }
}
