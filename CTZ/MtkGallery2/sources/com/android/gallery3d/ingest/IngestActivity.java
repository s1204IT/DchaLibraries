package com.android.gallery3d.ingest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.IngestService;
import com.android.gallery3d.ingest.adapter.CheckBroker;
import com.android.gallery3d.ingest.adapter.MtpAdapter;
import com.android.gallery3d.ingest.adapter.MtpPagerAdapter;
import com.android.gallery3d.ingest.data.ImportTask;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpBitmapFetch;
import com.android.gallery3d.ingest.data.MtpDeviceIndex;
import com.android.gallery3d.ingest.ui.DateTileView;
import com.android.gallery3d.ingest.ui.IngestGridView;
import java.lang.ref.WeakReference;
import java.util.Collection;

@TargetApi(12)
public class IngestActivity extends Activity implements ImportTask.Listener, MtpDeviceIndex.ProgressListener {
    private MenuItem mActionMenuSwitcherItem;
    private ActionMode mActiveActionMode;
    private MtpAdapter mAdapter;
    private ViewPager mFullscreenPager;
    private IngestGridView mGridView;
    private Handler mHandler;
    private IngestService mHelperService;
    private MenuItem mMenuSwitcherItem;
    private MtpPagerAdapter mPagerAdapter;
    private PositionMappingCheckBroker mPositionMappingCheckBroker;
    private ProgressDialog mProgressDialog;
    private ProgressState mProgressState;
    private TextView mWarningText;
    private View mWarningView;
    private boolean mActive = false;
    private int mLastCheckedPosition = 0;
    private boolean mFullscreenPagerVisible = false;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(intent.getAction())) {
                IngestActivity.this.finish();
            }
        }
    };
    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            IngestActivity.this.mLastCheckedPosition = i;
            IngestActivity.this.mGridView.setItemChecked(i, !IngestActivity.this.mGridView.getCheckedItemPositions().get(i));
        }
    };
    private AbsListView.MultiChoiceModeListener mMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
        private boolean mIgnoreItemCheckedStateChanges = false;

        private void updateSelectedTitle(ActionMode actionMode) {
            int checkedItemCount = IngestActivity.this.mGridView.getCheckedItemCount();
            actionMode.setTitle(IngestActivity.this.getResources().getQuantityString(R.plurals.ingest_number_of_items_selected, checkedItemCount, Integer.valueOf(checkedItemCount)));
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long j, boolean z) {
            if (!this.mIgnoreItemCheckedStateChanges) {
                if (IngestActivity.this.mAdapter.itemAtPositionIsBucket(i)) {
                    SparseBooleanArray checkedItemPositions = IngestActivity.this.mGridView.getCheckedItemPositions();
                    boolean z2 = true;
                    this.mIgnoreItemCheckedStateChanges = true;
                    IngestActivity.this.mGridView.setItemChecked(i, false);
                    int positionForSection = IngestActivity.this.mAdapter.getPositionForSection(IngestActivity.this.mAdapter.getSectionForPosition(i) + 1);
                    if (positionForSection == i) {
                        positionForSection = IngestActivity.this.mAdapter.getCount();
                    }
                    int i2 = i + 1;
                    int i3 = i2;
                    while (true) {
                        if (i3 < positionForSection) {
                            if (!checkedItemPositions.get(i3)) {
                                break;
                            } else {
                                i3++;
                            }
                        } else {
                            z2 = false;
                            break;
                        }
                    }
                    while (i2 < positionForSection) {
                        if (checkedItemPositions.get(i2) != z2) {
                            IngestActivity.this.mGridView.setItemChecked(i2, z2);
                        }
                        i2++;
                    }
                    IngestActivity.this.mPositionMappingCheckBroker.onBulkCheckedChange();
                    this.mIgnoreItemCheckedStateChanges = false;
                } else {
                    IngestActivity.this.mPositionMappingCheckBroker.onCheckedChange(i, z);
                }
                IngestActivity.this.mLastCheckedPosition = i;
                updateSelectedTitle(actionMode);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return IngestActivity.this.onOptionsItemSelected(menuItem);
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.ingest_menu_item_list_selection, menu);
            updateSelectedTitle(actionMode);
            IngestActivity.this.mActiveActionMode = actionMode;
            IngestActivity.this.mActionMenuSwitcherItem = menu.findItem(R.id.ingest_switch_view);
            IngestActivity.this.setSwitcherMenuState(IngestActivity.this.mActionMenuSwitcherItem, IngestActivity.this.mFullscreenPagerVisible);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            IngestActivity.this.mActiveActionMode = null;
            IngestActivity.this.mActionMenuSwitcherItem = null;
            IngestActivity.this.mHandler.sendEmptyMessage(3);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            updateSelectedTitle(actionMode);
            return false;
        }
    };
    private DataSetObserver mMasterObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            if (IngestActivity.this.mPagerAdapter != null) {
                IngestActivity.this.mPagerAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onInvalidated() {
            if (IngestActivity.this.mPagerAdapter != null) {
                IngestActivity.this.mPagerAdapter.notifyDataSetChanged();
            }
        }
    };
    private ServiceConnection mHelperServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            IngestActivity.this.mHelperService = ((IngestService.LocalBinder) iBinder).getService();
            IngestActivity.this.mHelperService.setClientActivity(IngestActivity.this);
            MtpDeviceIndex index = IngestActivity.this.mHelperService.getIndex();
            IngestActivity.this.mAdapter.setMtpDeviceIndex(index);
            if (IngestActivity.this.mPagerAdapter != null) {
                IngestActivity.this.mPagerAdapter.setMtpDeviceIndex(index);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            IngestActivity.this.mHelperService = null;
        }
    };

    public IngestActivity() {
        this.mPositionMappingCheckBroker = new PositionMappingCheckBroker();
        this.mProgressState = new ProgressState();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        doBindHelperService();
        setContentView(R.layout.ingest_activity_item_list);
        this.mGridView = (IngestGridView) findViewById(R.id.ingest_gridview);
        this.mAdapter = new MtpAdapter(this);
        this.mAdapter.registerDataSetObserver(this.mMasterObserver);
        this.mGridView.setAdapter((ListAdapter) this.mAdapter);
        this.mGridView.setMultiChoiceModeListener(this.mMultiChoiceModeListener);
        this.mGridView.setOnItemClickListener(this.mOnItemClickListener);
        this.mGridView.setOnClearChoicesListener(this.mPositionMappingCheckBroker);
        this.mFullscreenPager = (ViewPager) findViewById(R.id.ingest_view_pager);
        this.mHandler = new ItemListHandler(this);
        MtpBitmapFetch.configureForContext(this);
        registerReceiver(this.mUsbReceiver, new IntentFilter("android.hardware.usb.action.USB_DEVICE_DETACHED"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.ingest_import_items) {
            if (this.mActiveActionMode != null) {
                this.mHelperService.importSelectedItems(this.mGridView.getCheckedItemPositions(), this.mAdapter);
                this.mActiveActionMode.finish();
            }
            return true;
        }
        if (itemId == R.id.ingest_switch_view) {
            setFullscreenPagerVisibility(!this.mFullscreenPagerVisible);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ingest_menu_item_list_selection, menu);
        this.mMenuSwitcherItem = menu.findItem(R.id.ingest_switch_view);
        menu.findItem(R.id.ingest_import_items).setVisible(false);
        setSwitcherMenuState(this.mMenuSwitcherItem, this.mFullscreenPagerVisible);
        return true;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(this.mUsbReceiver);
        doUnbindHelperService();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        DateTileView.refreshLocale();
        this.mActive = true;
        if (this.mHelperService != null) {
            this.mHelperService.setClientActivity(this);
        }
        updateWarningView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (this.mHelperService != null) {
            this.mHelperService.setClientActivity(null);
        }
        this.mActive = false;
        cleanupProgressDialog();
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        MtpBitmapFetch.configureForContext(this);
    }

    private void showWarningView(int i) {
        if (this.mWarningView == null) {
            this.mWarningView = findViewById(R.id.ingest_warning_view);
            this.mWarningText = (TextView) this.mWarningView.findViewById(R.id.ingest_warning_view_text);
        }
        this.mWarningText.setText(i);
        this.mWarningView.setVisibility(0);
        setFullscreenPagerVisibility(false);
        this.mGridView.setVisibility(8);
        setSwitcherMenuVisibility(false);
    }

    private void hideWarningView() {
        if (this.mWarningView != null) {
            this.mWarningView.setVisibility(8);
            setFullscreenPagerVisibility(false);
        }
        setSwitcherMenuVisibility(true);
    }

    private class PositionMappingCheckBroker extends CheckBroker implements IngestGridView.OnClearChoicesListener {
        private int mLastMappingGrid;
        private int mLastMappingPager;

        private PositionMappingCheckBroker() {
            this.mLastMappingPager = -1;
            this.mLastMappingGrid = -1;
        }

        private int mapPagerToGridPosition(int i) {
            if (i != this.mLastMappingPager) {
                this.mLastMappingPager = i;
                this.mLastMappingGrid = IngestActivity.this.mAdapter.translatePositionWithoutLabels(i);
            }
            return this.mLastMappingGrid;
        }

        private int mapGridToPagerPosition(int i) {
            if (i != this.mLastMappingGrid) {
                this.mLastMappingGrid = i;
                this.mLastMappingPager = IngestActivity.this.mPagerAdapter.translatePositionWithLabels(i);
            }
            return this.mLastMappingPager;
        }

        @Override
        public void setItemChecked(int i, boolean z) {
            IngestActivity.this.mGridView.setItemChecked(mapPagerToGridPosition(i), z);
        }

        @Override
        public void onCheckedChange(int i, boolean z) {
            if (IngestActivity.this.mPagerAdapter != null) {
                super.onCheckedChange(mapGridToPagerPosition(i), z);
            }
        }

        @Override
        public boolean isItemChecked(int i) {
            return IngestActivity.this.mGridView.getCheckedItemPositions().get(mapPagerToGridPosition(i));
        }

        @Override
        public void onClearChoices() {
            onBulkCheckedChange();
        }
    }

    private int pickFullscreenStartingPosition() {
        int firstVisiblePosition = this.mGridView.getFirstVisiblePosition();
        if (this.mLastCheckedPosition <= firstVisiblePosition || this.mLastCheckedPosition > this.mGridView.getLastVisiblePosition()) {
            return firstVisiblePosition;
        }
        return this.mLastCheckedPosition;
    }

    private void setSwitcherMenuState(MenuItem menuItem, boolean z) {
        if (menuItem == null) {
            return;
        }
        if (!z) {
            menuItem.setIcon(android.R.drawable.ic_menu_zoom);
            menuItem.setTitle(R.string.ingest_switch_photo_fullscreen);
        } else {
            menuItem.setIcon(android.R.drawable.ic_dialog_dialer);
            menuItem.setTitle(R.string.ingest_switch_photo_grid);
        }
    }

    private void setFullscreenPagerVisibility(boolean z) {
        this.mFullscreenPagerVisible = z;
        if (z) {
            if (this.mPagerAdapter == null) {
                this.mPagerAdapter = new MtpPagerAdapter(this, this.mPositionMappingCheckBroker);
                this.mPagerAdapter.setMtpDeviceIndex(this.mAdapter.getMtpDeviceIndex());
            }
            this.mFullscreenPager.setAdapter(this.mPagerAdapter);
            this.mFullscreenPager.setCurrentItem(this.mPagerAdapter.translatePositionWithLabels(pickFullscreenStartingPosition()), false);
        } else if (this.mPagerAdapter != null) {
            this.mGridView.setSelection(this.mAdapter.translatePositionWithoutLabels(this.mFullscreenPager.getCurrentItem()));
            this.mFullscreenPager.setAdapter(null);
        }
        this.mGridView.setVisibility(z ? 4 : 0);
        this.mFullscreenPager.setVisibility(z ? 0 : 4);
        if (this.mActionMenuSwitcherItem != null) {
            setSwitcherMenuState(this.mActionMenuSwitcherItem, z);
        }
        setSwitcherMenuState(this.mMenuSwitcherItem, z);
    }

    private void setSwitcherMenuVisibility(boolean z) {
        if (this.mActionMenuSwitcherItem != null) {
            this.mActionMenuSwitcherItem.setVisible(z);
        }
        if (this.mMenuSwitcherItem != null) {
            this.mMenuSwitcherItem.setVisible(z);
        }
    }

    private void updateWarningView() {
        if (!this.mAdapter.deviceConnected()) {
            showWarningView(R.string.ingest_no_device);
        } else if (this.mAdapter.indexReady() && this.mAdapter.getCount() == 0) {
            showWarningView(R.string.ingest_empty_device);
        } else {
            hideWarningView();
        }
    }

    private void uiThreadNotifyIndexChanged() {
        this.mAdapter.notifyDataSetChanged();
        if (this.mActiveActionMode != null) {
            this.mActiveActionMode.finish();
            this.mActiveActionMode = null;
        }
        updateWarningView();
    }

    protected void notifyIndexChanged() {
        this.mHandler.sendEmptyMessage(2);
    }

    private static class ProgressState {
        int current;
        int max;
        String message;
        String title;

        private ProgressState() {
        }

        public void reset() {
            this.title = null;
            this.message = null;
            this.current = 0;
            this.max = 0;
        }
    }

    @Override
    public void onObjectIndexed(IngestObjectInfo ingestObjectInfo, int i) {
        this.mProgressState.reset();
        this.mProgressState.max = 0;
        this.mProgressState.message = getResources().getQuantityString(R.plurals.ingest_number_of_items_scanned, i, Integer.valueOf(i));
        this.mHandler.sendEmptyMessage(0);
    }

    @Override
    public void onSortingStarted() {
        this.mProgressState.reset();
        this.mProgressState.max = 0;
        this.mProgressState.message = getResources().getString(R.string.ingest_sorting);
        this.mHandler.sendEmptyMessage(0);
    }

    @Override
    public void onIndexingFinished() {
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public void onImportProgress(int i, int i2, String str) {
        this.mProgressState.reset();
        this.mProgressState.max = i2;
        this.mProgressState.current = i;
        this.mProgressState.title = getResources().getString(R.string.ingest_importing);
        this.mHandler.sendEmptyMessage(0);
        this.mHandler.removeMessages(4);
        this.mHandler.sendEmptyMessageDelayed(4, 3000L);
    }

    @Override
    public void onImportFinish(Collection<IngestObjectInfo> collection, int i) {
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.removeMessages(4);
    }

    private ProgressDialog getProgressDialog() {
        if (this.mProgressDialog == null || !this.mProgressDialog.isShowing()) {
            this.mProgressDialog = new ProgressDialog(this);
            this.mProgressDialog.setCancelable(false);
        }
        return this.mProgressDialog;
    }

    private void updateProgressDialog() {
        ProgressDialog progressDialog = getProgressDialog();
        boolean z = this.mProgressState.max == 0;
        progressDialog.setIndeterminate(z);
        progressDialog.setProgressStyle(z ? 0 : 1);
        if (this.mProgressState.title != null) {
            progressDialog.setTitle(this.mProgressState.title);
        }
        if (this.mProgressState.message != null) {
            progressDialog.setMessage(this.mProgressState.message);
        }
        if (!z) {
            progressDialog.setProgress(this.mProgressState.current);
            progressDialog.setMax(this.mProgressState.max);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void makeProgressDialogIndeterminate() {
        getProgressDialog().setIndeterminate(true);
    }

    private void cleanupProgressDialog() {
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
            this.mProgressDialog = null;
        }
    }

    private static class ItemListHandler extends Handler {
        WeakReference<IngestActivity> mParentReference;

        public ItemListHandler(IngestActivity ingestActivity) {
            this.mParentReference = new WeakReference<>(ingestActivity);
        }

        @Override
        public void handleMessage(Message message) {
            IngestActivity ingestActivity = this.mParentReference.get();
            if (ingestActivity != null && ingestActivity.mActive) {
                switch (message.what) {
                    case 0:
                        ingestActivity.updateProgressDialog();
                        break;
                    case 1:
                        ingestActivity.cleanupProgressDialog();
                        break;
                    case 2:
                        ingestActivity.uiThreadNotifyIndexChanged();
                        break;
                    case 3:
                        ingestActivity.mPositionMappingCheckBroker.onBulkCheckedChange();
                        break;
                    case 4:
                        ingestActivity.makeProgressDialogIndeterminate();
                        break;
                }
            }
        }
    }

    private void doBindHelperService() {
        bindService(new Intent(getApplicationContext(), (Class<?>) IngestService.class), this.mHelperServiceConnection, 1);
    }

    private void doUnbindHelperService() {
        if (this.mHelperService != null) {
            this.mHelperService.setClientActivity(null);
            unbindService(this.mHelperServiceConnection);
        }
    }
}
