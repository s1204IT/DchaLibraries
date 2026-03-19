package com.android.bluetooth.opp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.StaleDataException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.bluetooth.R;
import com.android.vcard.VCardConfig;

public class BluetoothOppTransferHistory extends Activity implements View.OnCreateContextMenuListener, AdapterView.OnItemClickListener {
    private static final String TAG = "BluetoothOppTransferHistory";
    private static final boolean V = Constants.VERBOSE;
    private static ClearHistory clearHistory;
    private int mContextMenuPosition;
    private int mDir;
    private int mIdColumnId;
    private ListView mListView;
    private BluetoothOppNotification mNotifier;
    private boolean mShowAllIncoming;
    private BluetoothOppTransferAdapter mTransferAdapter;
    private Cursor mTransferCursor;
    private boolean mContextMenu = false;
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothOppTransferHistory.V) {
                Log.v(BluetoothOppTransferHistory.TAG, "Received intent: " + action);
            }
            if (action.equals("android.intent.action.TIME_SET")) {
                BluetoothOppTransferHistory.this.mTransferAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        String str;
        super.onCreate(bundle);
        if (V) {
            Log.v(TAG, "onCreate ++");
        }
        setContentView(R.layout.bluetooth_transfers_page);
        this.mListView = (ListView) findViewById(R.id.list);
        this.mListView.setEmptyView(findViewById(R.id.empty));
        this.mShowAllIncoming = getIntent().getBooleanExtra("android.btopp.intent.extra.SHOW_ALL", false);
        int intExtra = getIntent().getIntExtra(BluetoothShare.DIRECTION, 0);
        this.mDir = intExtra;
        if (intExtra == 0) {
            setTitle(getText(R.string.outbound_history_title));
            str = "(direction == 0)";
        } else {
            if (this.mShowAllIncoming) {
                setTitle(getText(R.string.btopp_live_folder));
            } else {
                setTitle(getText(R.string.inbound_history_title));
            }
            str = "(direction == 1)";
        }
        String str2 = "status >= '200' AND " + str;
        if (!this.mShowAllIncoming) {
            str2 = str2 + " AND (" + BluetoothShare.VISIBILITY + " IS NULL OR " + BluetoothShare.VISIBILITY + " == '0')";
        }
        this.mTransferCursor = getContentResolver().query(BluetoothShare.CONTENT_URI, new String[]{"_id", BluetoothShare.FILENAME_HINT, "status", BluetoothShare.TOTAL_BYTES, BluetoothShare._DATA, "timestamp", BluetoothShare.VISIBILITY, BluetoothShare.DESTINATION, BluetoothShare.DIRECTION}, str2, null, "timestamp DESC");
        if (this.mTransferCursor != null) {
            this.mIdColumnId = this.mTransferCursor.getColumnIndexOrThrow("_id");
            this.mTransferAdapter = new BluetoothOppTransferAdapter(this, R.layout.bluetooth_transfer_item, this.mTransferCursor);
            this.mListView.setAdapter((ListAdapter) this.mTransferAdapter);
            this.mListView.setScrollBarStyle(16777216);
            this.mListView.setOnCreateContextMenuListener(this);
            this.mListView.setOnItemClickListener(this);
        }
        this.mNotifier = new BluetoothOppNotification(this);
        if (clearHistory != null && clearHistory.getStatus() != AsyncTask.Status.FINISHED) {
            clearHistory.showProgress(this);
        }
        this.mContextMenu = false;
        registerReceiver(this.mBluetoothReceiver, new IntentFilter("android.intent.action.TIME_SET"));
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (V) {
            Log.i(TAG, "onConfigurationChanged ++");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mTransferCursor != null && !this.mShowAllIncoming) {
            getMenuInflater().inflate(R.menu.transferhistory, menu);
            return true;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!this.mShowAllIncoming) {
            menu.findItem(R.id.transfer_menu_clear_all).setEnabled(isTransferComplete());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.transfer_menu_clear_all) {
            promptClearList();
            return true;
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        if (this.mTransferCursor.getCount() == 0) {
            Log.i(TAG, "History is already cleared, not clearing again");
            return true;
        }
        this.mTransferCursor.moveToPosition(this.mContextMenuPosition);
        int itemId = menuItem.getItemId();
        if (itemId != R.id.transfer_menu_clear) {
            if (itemId == R.id.transfer_menu_open) {
                openCompleteTransfer();
                updateNotificationWhenBtDisabled();
                return true;
            }
            return false;
        }
        BluetoothOppUtility.updateVisibilityToHidden(this, Uri.parse(BluetoothShare.CONTENT_URI + "/" + this.mTransferCursor.getInt(this.mIdColumnId)));
        updateNotificationWhenBtDisabled();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (clearHistory != null) {
            clearHistory.dismissProgress();
        }
        if (this.mTransferCursor != null) {
            this.mTransferCursor.close();
        }
        if (this.mBluetoothReceiver != null) {
            unregisterReceiver(this.mBluetoothReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (this.mTransferCursor != null) {
            this.mContextMenu = true;
            AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
            this.mTransferCursor.moveToPosition(adapterContextMenuInfo.position);
            this.mContextMenuPosition = adapterContextMenuInfo.position;
            String string = this.mTransferCursor.getString(this.mTransferCursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT));
            if (string == null) {
                string = getString(R.string.unknown_file);
            }
            contextMenu.setHeaderTitle(string);
            MenuInflater menuInflater = getMenuInflater();
            if (this.mShowAllIncoming) {
                menuInflater.inflate(R.menu.receivedfilescontextfinished, contextMenu);
            } else {
                menuInflater.inflate(R.menu.transferhistorycontextfinished, contextMenu);
            }
        }
    }

    private void promptClearList() {
        new AlertDialog.Builder(this).setTitle(R.string.transfer_clear_dlg_title).setMessage(R.string.transfer_clear_dlg_msg).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (BluetoothOppTransferHistory.clearHistory == null) {
                    ClearHistory unused = BluetoothOppTransferHistory.clearHistory = BluetoothOppTransferHistory.this.new ClearHistory();
                    BluetoothOppTransferHistory.clearHistory.execute(0);
                }
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private int getClearableCount() {
        if (V) {
            Log.i(TAG, "getClearableCount ++");
        }
        int i = 0;
        try {
            if (this.mTransferCursor != null && this.mTransferCursor.moveToFirst()) {
                while (!this.mTransferCursor.isAfterLast()) {
                    if (BluetoothShare.isStatusCompleted(this.mTransferCursor.getInt(this.mTransferCursor.getColumnIndexOrThrow("status")))) {
                        i++;
                    }
                    this.mTransferCursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getClearableCount error.");
            e.printStackTrace();
        }
        if (V) {
            Log.i(TAG, "getClearableCount return " + i);
        }
        return i;
    }

    private boolean isTransferComplete() {
        try {
            if (this.mTransferCursor.moveToFirst()) {
                while (!this.mTransferCursor.isAfterLast()) {
                    if (BluetoothShare.isStatusCompleted(this.mTransferCursor.getInt(this.mTransferCursor.getColumnIndexOrThrow("status")))) {
                        return true;
                    }
                    this.mTransferCursor.moveToNext();
                }
                return false;
            }
            return false;
        } catch (StaleDataException e) {
            return false;
        }
    }

    private void clearAllDownloads() {
        String str;
        if (this.mDir == 0) {
            str = "(direction == 0)";
        } else {
            str = "(direction == 1)";
        }
        String str2 = "status >= '200' AND " + str;
        if (!this.mShowAllIncoming) {
            str2 = str2 + " AND (" + BluetoothShare.VISIBILITY + " IS NULL OR " + BluetoothShare.VISIBILITY + " == '0')";
        }
        Cursor cursorQuery = getContentResolver().query(BluetoothShare.CONTENT_URI, new String[]{"_id"}, str2, null, "_id");
        if (cursorQuery == null) {
            Log.d(TAG, "clearAllDownloads::cursor == null");
            return;
        }
        if (cursorQuery.moveToFirst()) {
            while (!cursorQuery.isAfterLast()) {
                Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + cursorQuery.getInt(this.mIdColumnId));
                Log.i(TAG, "cout = " + cursorQuery.getCount() + " uri = " + uri);
                BluetoothOppUtility.updateVisibilityToHidden(this, uri);
                cursorQuery.moveToNext();
            }
            updateNotificationWhenBtDisabled();
        }
        cursorQuery.close();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (V) {
            Log.v(TAG, "onItemClick: ContextMenu = " + this.mContextMenu);
        }
        if (!this.mContextMenu) {
            this.mTransferCursor.moveToPosition(i);
            openCompleteTransfer();
            updateNotificationWhenBtDisabled();
        }
        this.mContextMenu = false;
    }

    private void openCompleteTransfer() {
        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + this.mTransferCursor.getInt(this.mIdColumnId));
        BluetoothOppTransferInfo bluetoothOppTransferInfoQueryRecord = BluetoothOppUtility.queryRecord(this, uri);
        if (bluetoothOppTransferInfoQueryRecord == null) {
            Log.e(TAG, "Error: Can not get data from db");
            return;
        }
        if (bluetoothOppTransferInfoQueryRecord.mDirection == 1 && BluetoothShare.isStatusSuccess(bluetoothOppTransferInfoQueryRecord.mStatus)) {
            BluetoothOppUtility.updateVisibilityToHidden(this, uri);
            BluetoothOppUtility.openReceivedFile(this, bluetoothOppTransferInfoQueryRecord.mFileName, bluetoothOppTransferInfoQueryRecord.mFileType, bluetoothOppTransferInfoQueryRecord.mTimeStamp, uri);
        } else {
            Intent intent = new Intent(this, (Class<?>) BluetoothOppTransferActivity.class);
            intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            intent.setDataAndNormalize(uri);
            startActivity(intent);
        }
    }

    private void updateNotificationWhenBtDisabled() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            if (V) {
                Log.v(TAG, "Bluetooth is not enabled, update notification manually.");
            }
            this.mNotifier.updateNotification();
        }
    }

    class ClearHistory extends AsyncTask<Integer, Integer, Boolean> {
        private ProgressDialog mDialog;

        ClearHistory() {
        }

        public void showProgress(Activity activity) {
            this.mDialog = new ProgressDialog(activity);
            this.mDialog.setMessage(BluetoothOppTransferHistory.this.getString(R.string.transfer_menu_clear_all) + "...");
            this.mDialog.setIndeterminate(true);
            this.mDialog.setCancelable(false);
            this.mDialog.show();
        }

        public void dismissProgress() {
            if (this.mDialog != null) {
                this.mDialog.dismiss();
            }
            this.mDialog = null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(BluetoothOppTransferHistory.this);
        }

        @Override
        protected Boolean doInBackground(Integer... numArr) {
            BluetoothOppTransferHistory.this.clearAllDownloads();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            dismissProgress();
            ClearHistory unused = BluetoothOppTransferHistory.clearHistory = null;
            super.onPostExecute(bool);
        }
    }
}
