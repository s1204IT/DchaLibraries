package com.android.bluetooth.opp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
    private static final boolean V = false;
    private boolean mContextMenu = false;
    private int mContextMenuPosition;
    private int mIdColumnId;
    private ListView mListView;
    private BluetoothOppNotification mNotifier;
    private boolean mShowAllIncoming;
    private BluetoothOppTransferAdapter mTransferAdapter;
    private Cursor mTransferCursor;

    @Override
    public void onCreate(Bundle icicle) {
        String direction;
        super.onCreate(icicle);
        setContentView(R.layout.bluetooth_transfers_page);
        this.mListView = (ListView) findViewById(R.id.list);
        this.mListView.setEmptyView(findViewById(R.id.empty));
        this.mShowAllIncoming = getIntent().getBooleanExtra(Constants.EXTRA_SHOW_ALL_FILES, false);
        int dir = getIntent().getIntExtra(BluetoothShare.DIRECTION, 0);
        if (dir == 0) {
            setTitle(getText(R.string.outbound_history_title));
            direction = "(direction == 0)";
        } else {
            if (this.mShowAllIncoming) {
                setTitle(getText(R.string.btopp_live_folder));
            } else {
                setTitle(getText(R.string.inbound_history_title));
            }
            direction = "(direction == 1)";
        }
        String selection = "status >= '200' AND " + direction;
        if (!this.mShowAllIncoming) {
            selection = selection + " AND (" + BluetoothShare.VISIBILITY + " IS NULL OR " + BluetoothShare.VISIBILITY + " == '0')";
        }
        this.mTransferCursor = managedQuery(BluetoothShare.CONTENT_URI, new String[]{"_id", BluetoothShare.FILENAME_HINT, BluetoothShare.STATUS, BluetoothShare.TOTAL_BYTES, BluetoothShare._DATA, "timestamp", BluetoothShare.VISIBILITY, BluetoothShare.DESTINATION, BluetoothShare.DIRECTION}, selection, "timestamp DESC");
        if (this.mTransferCursor != null) {
            this.mIdColumnId = this.mTransferCursor.getColumnIndexOrThrow("_id");
            this.mTransferAdapter = new BluetoothOppTransferAdapter(this, R.layout.bluetooth_transfer_item, this.mTransferCursor);
            this.mListView.setAdapter((ListAdapter) this.mTransferAdapter);
            this.mListView.setScrollBarStyle(16777216);
            this.mListView.setOnCreateContextMenuListener(this);
            this.mListView.setOnItemClickListener(this);
        }
        this.mNotifier = new BluetoothOppNotification(this);
        this.mContextMenu = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mTransferCursor != null && !this.mShowAllIncoming) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.transferhistory, menu);
            return true;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!this.mShowAllIncoming) {
            boolean showClear = getClearableCount() > 0;
            menu.findItem(R.id.transfer_menu_clear_all).setEnabled(showClear);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.transfer_menu_clear_all:
                promptClearList();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        this.mTransferCursor.moveToPosition(this.mContextMenuPosition);
        switch (item.getItemId()) {
            case R.id.transfer_menu_open:
                openCompleteTransfer();
                updateNotificationWhenBtDisabled();
                break;
            case R.id.transfer_menu_clear:
                int sessionId = this.mTransferCursor.getInt(this.mIdColumnId);
                Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + sessionId);
                BluetoothOppUtility.updateVisibilityToHidden(this, contentUri);
                updateNotificationWhenBtDisabled();
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (this.mTransferCursor != null) {
            this.mContextMenu = true;
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            this.mTransferCursor.moveToPosition(info.position);
            this.mContextMenuPosition = info.position;
            String fileName = this.mTransferCursor.getString(this.mTransferCursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT));
            if (fileName == null) {
                fileName = getString(R.string.unknown_file);
            }
            menu.setHeaderTitle(fileName);
            MenuInflater inflater = getMenuInflater();
            if (this.mShowAllIncoming) {
                inflater.inflate(R.menu.receivedfilescontextfinished, menu);
            } else {
                inflater.inflate(R.menu.transferhistorycontextfinished, menu);
            }
        }
    }

    private void promptClearList() {
        new AlertDialog.Builder(this).setTitle(R.string.transfer_clear_dlg_title).setMessage(R.string.transfer_clear_dlg_msg).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                BluetoothOppTransferHistory.this.clearAllDownloads();
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private int getClearableCount() {
        int count = 0;
        if (this.mTransferCursor.moveToFirst()) {
            while (!this.mTransferCursor.isAfterLast()) {
                int statusColumnId = this.mTransferCursor.getColumnIndexOrThrow(BluetoothShare.STATUS);
                int status = this.mTransferCursor.getInt(statusColumnId);
                if (BluetoothShare.isStatusCompleted(status)) {
                    count++;
                }
                this.mTransferCursor.moveToNext();
            }
        }
        return count;
    }

    private void clearAllDownloads() {
        if (this.mTransferCursor.moveToFirst()) {
            while (!this.mTransferCursor.isAfterLast()) {
                int sessionId = this.mTransferCursor.getInt(this.mIdColumnId);
                Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + sessionId);
                BluetoothOppUtility.updateVisibilityToHidden(this, contentUri);
                this.mTransferCursor.moveToNext();
            }
            updateNotificationWhenBtDisabled();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!this.mContextMenu) {
            this.mTransferCursor.moveToPosition(position);
            openCompleteTransfer();
            updateNotificationWhenBtDisabled();
        }
        this.mContextMenu = false;
    }

    private void openCompleteTransfer() {
        int sessionId = this.mTransferCursor.getInt(this.mIdColumnId);
        Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + sessionId);
        BluetoothOppTransferInfo transInfo = BluetoothOppUtility.queryRecord(this, contentUri);
        if (transInfo == null) {
            Log.e(TAG, "Error: Can not get data from db");
            return;
        }
        if (transInfo.mDirection == 1 && BluetoothShare.isStatusSuccess(transInfo.mStatus)) {
            BluetoothOppUtility.updateVisibilityToHidden(this, contentUri);
            BluetoothOppUtility.openReceivedFile(this, transInfo.mFileName, transInfo.mFileType, transInfo.mTimeStamp, contentUri);
        } else {
            Intent in = new Intent(this, (Class<?>) BluetoothOppTransferActivity.class);
            in.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            in.setDataAndNormalize(contentUri);
            startActivity(in);
        }
    }

    private void updateNotificationWhenBtDisabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
            this.mNotifier.updateNotification();
        }
    }
}
