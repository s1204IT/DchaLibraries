package com.android.gallery3d.ingest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
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
    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View itemView, int position, long arg3) {
            IngestActivity.this.mLastCheckedPosition = position;
            IngestActivity.this.mGridView.setItemChecked(position, !IngestActivity.this.mGridView.getCheckedItemPositions().get(position));
        }
    };
    private AbsListView.MultiChoiceModeListener mMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
        private boolean mIgnoreItemCheckedStateChanges = false;

        private void updateSelectedTitle(ActionMode mode) {
            int count = IngestActivity.this.mGridView.getCheckedItemCount();
            mode.setTitle(IngestActivity.this.getResources().getQuantityString(R.plurals.ingest_number_of_items_selected, count, Integer.valueOf(count)));
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            if (!this.mIgnoreItemCheckedStateChanges) {
                if (IngestActivity.this.mAdapter.itemAtPositionIsBucket(position)) {
                    SparseBooleanArray checkedItems = IngestActivity.this.mGridView.getCheckedItemPositions();
                    this.mIgnoreItemCheckedStateChanges = true;
                    IngestActivity.this.mGridView.setItemChecked(position, false);
                    int nextSectionStart = IngestActivity.this.mAdapter.getPositionForSection(IngestActivity.this.mAdapter.getSectionForPosition(position) + 1);
                    if (nextSectionStart == position) {
                        nextSectionStart = IngestActivity.this.mAdapter.getCount();
                    }
                    boolean rangeValue = false;
                    int i = position + 1;
                    while (true) {
                        if (i >= nextSectionStart) {
                            break;
                        }
                        if (checkedItems.get(i)) {
                            i++;
                        } else {
                            rangeValue = true;
                            break;
                        }
                    }
                    for (int i2 = position + 1; i2 < nextSectionStart; i2++) {
                        if (checkedItems.get(i2) != rangeValue) {
                            IngestActivity.this.mGridView.setItemChecked(i2, rangeValue);
                        }
                    }
                    IngestActivity.this.mPositionMappingCheckBroker.onBulkCheckedChange();
                    this.mIgnoreItemCheckedStateChanges = false;
                } else {
                    IngestActivity.this.mPositionMappingCheckBroker.onCheckedChange(position, checked);
                }
                IngestActivity.this.mLastCheckedPosition = position;
                updateSelectedTitle(mode);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return IngestActivity.this.onOptionsItemSelected(item);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.ingest_menu_item_list_selection, menu);
            updateSelectedTitle(mode);
            IngestActivity.this.mActiveActionMode = mode;
            IngestActivity.this.mActionMenuSwitcherItem = menu.findItem(R.id.ingest_switch_view);
            IngestActivity.this.setSwitcherMenuState(IngestActivity.this.mActionMenuSwitcherItem, IngestActivity.this.mFullscreenPagerVisible);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            IngestActivity.this.mActiveActionMode = null;
            IngestActivity.this.mActionMenuSwitcherItem = null;
            IngestActivity.this.mHandler.sendEmptyMessage(3);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            updateSelectedTitle(mode);
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
        public void onServiceConnected(ComponentName className, IBinder service) {
            IngestActivity.this.mHelperService = ((IngestService.LocalBinder) service).getService();
            IngestActivity.this.mHelperService.setClientActivity(IngestActivity.this);
            MtpDeviceIndex index = IngestActivity.this.mHelperService.getIndex();
            IngestActivity.this.mAdapter.setMtpDeviceIndex(index);
            if (IngestActivity.this.mPagerAdapter != null) {
                IngestActivity.this.mPagerAdapter.setMtpDeviceIndex(index);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            IngestActivity.this.mHelperService = null;
        }
    };

    public IngestActivity() {
        this.mPositionMappingCheckBroker = new PositionMappingCheckBroker();
        this.mProgressState = new ProgressState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.System.getInt(getContentResolver(), "dcha_state", 0) != 0) {
            finish();
            return;
        }
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.ingest_import_items) {
            if (this.mActiveActionMode == null) {
                return true;
            }
            this.mHelperService.importSelectedItems(this.mGridView.getCheckedItemPositions(), this.mAdapter);
            this.mActiveActionMode.finish();
            return true;
        }
        if (id != R.id.ingest_switch_view) {
            return false;
        }
        setFullscreenPagerVisibility(this.mFullscreenPagerVisible ? false : true);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ingest_menu_item_list_selection, menu);
        this.mMenuSwitcherItem = menu.findItem(R.id.ingest_switch_view);
        menu.findItem(R.id.ingest_import_items).setVisible(false);
        setSwitcherMenuState(this.mMenuSwitcherItem, this.mFullscreenPagerVisible);
        return true;
    }

    @Override
    protected void onDestroy() {
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MtpBitmapFetch.configureForContext(this);
    }

    private void showWarningView(int textResId) {
        if (this.mWarningView == null) {
            this.mWarningView = findViewById(R.id.ingest_warning_view);
            this.mWarningText = (TextView) this.mWarningView.findViewById(R.id.ingest_warning_view_text);
        }
        this.mWarningText.setText(textResId);
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

        private int mapPagerToGridPosition(int position) {
            if (position != this.mLastMappingPager) {
                this.mLastMappingPager = position;
                this.mLastMappingGrid = IngestActivity.this.mAdapter.translatePositionWithoutLabels(position);
            }
            return this.mLastMappingGrid;
        }

        private int mapGridToPagerPosition(int position) {
            if (position != this.mLastMappingGrid) {
                this.mLastMappingGrid = position;
                this.mLastMappingPager = IngestActivity.this.mPagerAdapter.translatePositionWithLabels(position);
            }
            return this.mLastMappingPager;
        }

        @Override
        public void setItemChecked(int position, boolean checked) {
            IngestActivity.this.mGridView.setItemChecked(mapPagerToGridPosition(position), checked);
        }

        @Override
        public void onCheckedChange(int position, boolean checked) {
            if (IngestActivity.this.mPagerAdapter != null) {
                super.onCheckedChange(mapGridToPagerPosition(position), checked);
            }
        }

        @Override
        public boolean isItemChecked(int position) {
            return IngestActivity.this.mGridView.getCheckedItemPositions().get(mapPagerToGridPosition(position));
        }

        @Override
        public void onClearChoices() {
            onBulkCheckedChange();
        }
    }

    private int pickFullscreenStartingPosition() {
        int firstVisiblePosition = this.mGridView.getFirstVisiblePosition();
        return (this.mLastCheckedPosition <= firstVisiblePosition || this.mLastCheckedPosition > this.mGridView.getLastVisiblePosition()) ? firstVisiblePosition : this.mLastCheckedPosition;
    }

    private void setSwitcherMenuState(MenuItem menuItem, boolean inFullscreenMode) {
        if (menuItem != null) {
            if (!inFullscreenMode) {
                menuItem.setIcon(android.R.drawable.ic_menu_zoom);
                menuItem.setTitle(R.string.ingest_switch_photo_fullscreen);
            } else {
                menuItem.setIcon(android.R.drawable.ic_dialog_dialer);
                menuItem.setTitle(R.string.ingest_switch_photo_grid);
            }
        }
    }

    private void setFullscreenPagerVisibility(boolean visible) {
        this.mFullscreenPagerVisible = visible;
        if (visible) {
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
        this.mGridView.setVisibility(visible ? 4 : 0);
        this.mFullscreenPager.setVisibility(visible ? 0 : 4);
        if (this.mActionMenuSwitcherItem != null) {
            setSwitcherMenuState(this.mActionMenuSwitcherItem, visible);
        }
        setSwitcherMenuState(this.mMenuSwitcherItem, visible);
    }

    private void setSwitcherMenuVisibility(boolean visible) {
        if (this.mActionMenuSwitcherItem != null) {
            this.mActionMenuSwitcherItem.setVisible(visible);
        }
        if (this.mMenuSwitcherItem != null) {
            this.mMenuSwitcherItem.setVisible(visible);
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
    public void onObjectIndexed(IngestObjectInfo object, int numVisited) {
        this.mProgressState.reset();
        this.mProgressState.max = 0;
        this.mProgressState.message = getResources().getQuantityString(R.plurals.ingest_number_of_items_scanned, numVisited, Integer.valueOf(numVisited));
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
    public void onImportProgress(int visitedCount, int totalCount, String pathIfSuccessful) {
        this.mProgressState.reset();
        this.mProgressState.max = totalCount;
        this.mProgressState.current = visitedCount;
        this.mProgressState.title = getResources().getString(R.string.ingest_importing);
        this.mHandler.sendEmptyMessage(0);
        this.mHandler.removeMessages(4);
        this.mHandler.sendEmptyMessageDelayed(4, 3000L);
    }

    @Override
    public void onImportFinish(Collection<IngestObjectInfo> objectsNotImported, int numVisited) {
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
        ProgressDialog dialog = getProgressDialog();
        boolean indeterminate = this.mProgressState.max == 0;
        dialog.setIndeterminate(indeterminate);
        dialog.setProgressStyle(indeterminate ? 0 : 1);
        if (this.mProgressState.title != null) {
            dialog.setTitle(this.mProgressState.title);
        }
        if (this.mProgressState.message != null) {
            dialog.setMessage(this.mProgressState.message);
        }
        if (!indeterminate) {
            dialog.setProgress(this.mProgressState.current);
            dialog.setMax(this.mProgressState.max);
        }
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    private void makeProgressDialogIndeterminate() {
        ProgressDialog dialog = getProgressDialog();
        dialog.setIndeterminate(true);
    }

    private void cleanupProgressDialog() {
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
            this.mProgressDialog = null;
        }
    }

    private static class ItemListHandler extends Handler {
        WeakReference<IngestActivity> mParentReference;

        public ItemListHandler(IngestActivity parent) {
            this.mParentReference = new WeakReference<>(parent);
        }

        @Override
        public void handleMessage(Message message) {
            IngestActivity parent = this.mParentReference.get();
            if (parent != null && parent.mActive) {
                switch (message.what) {
                    case 0:
                        parent.updateProgressDialog();
                        break;
                    case 1:
                        parent.cleanupProgressDialog();
                        break;
                    case 2:
                        parent.uiThreadNotifyIndexChanged();
                        break;
                    case 3:
                        parent.mPositionMappingCheckBroker.onBulkCheckedChange();
                        break;
                    case 4:
                        parent.makeProgressDialogIndeterminate();
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
