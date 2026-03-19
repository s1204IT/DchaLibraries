package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;
import com.android.bluetooth.map.BluetoothMapContentObserver;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardEntry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CallLogPullRequest extends PullRequest {
    private static final boolean DBG = true;
    private static final String TAG = "PbapCallLogPullRequest";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd'T'HHmmss";
    private static final String TIMESTAMP_PROPERTY = "X-IRMC-CALL-DATETIME";
    private static final boolean VDBG = false;
    private final Account mAccount;
    private HashMap<String, Integer> mCallCounter;
    private Context mContext;

    public CallLogPullRequest(Context context, String str, HashMap<String, Integer> map, Account account) {
        this.mContext = context;
        this.path = str;
        this.mCallCounter = map;
        this.mAccount = account;
    }

    @Override
    public void onPullComplete() {
        int i;
        if (this.mEntries == null) {
            Log.e(TAG, "onPullComplete entries is null.");
            return;
        }
        Log.d(TAG, "onPullComplete");
        try {
            try {
                if (this.path.equals(PbapClientConnectionHandler.ICH_PATH)) {
                    i = 1;
                } else if (this.path.equals(PbapClientConnectionHandler.OCH_PATH)) {
                    i = 2;
                } else {
                    if (!this.path.equals(PbapClientConnectionHandler.MCH_PATH)) {
                        Log.w(TAG, "Unknown path type:" + this.path);
                        synchronized (this) {
                            notify();
                        }
                        return;
                    }
                    i = 3;
                }
                ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
                for (VCardEntry vCardEntry : this.mEntries) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, Integer.valueOf(i));
                    contentValues.put("subscription_id", Integer.valueOf(this.mAccount.hashCode()));
                    List<VCardEntry.PhoneData> phoneList = vCardEntry.getPhoneList();
                    if (phoneList == null || phoneList.get(0).getNumber().equals(";")) {
                        contentValues.put("number", "");
                    } else {
                        String number = phoneList.get(0).getNumber();
                        contentValues.put("number", number);
                        if (this.mCallCounter.get(number) != null) {
                            this.mCallCounter.put(number, Integer.valueOf(this.mCallCounter.get(number).intValue() + 1));
                        } else {
                            this.mCallCounter.put(number, 1);
                        }
                    }
                    List<Pair<String, String>> unknownXData = vCardEntry.getUnknownXData();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
                    if (unknownXData != null) {
                        for (Pair<String, String> pair : unknownXData) {
                            if (((String) pair.first).startsWith(TIMESTAMP_PROPERTY)) {
                                try {
                                    contentValues.put(BluetoothMapContract.MessageColumns.DATE, Long.valueOf(simpleDateFormat.parse((String) pair.second).getTime()));
                                } catch (ParseException e) {
                                    Log.d(TAG, "Failed to parse date ");
                                }
                            }
                        }
                    }
                    arrayList.add(ContentProviderOperation.newInsert(CallLog.Calls.CONTENT_URI).withValues(contentValues).withYieldAllowed(true).build());
                }
                this.mContext.getContentResolver().applyBatch("call_log", arrayList);
                Log.d(TAG, "Updated call logs.");
                if (i == 2) {
                    updateTimesContacted();
                }
                synchronized (this) {
                    notify();
                }
            } catch (OperationApplicationException | RemoteException e2) {
                Log.d(TAG, "Failed to update call log for path=" + this.path, e2);
                synchronized (this) {
                    notify();
                }
            }
        } catch (Throwable th) {
            synchronized (this) {
                notify();
                throw th;
            }
        }
    }

    private void updateTimesContacted() {
        for (String str : this.mCallCounter.keySet()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("times_contacted", this.mCallCounter.get(str));
            Cursor cursorQuery = this.mContext.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)), null, null, null);
            if (cursorQuery != null && cursorQuery.getCount() > 0) {
                cursorQuery.moveToNext();
                this.mContext.getContentResolver().update(ContactsContract.RawContacts.CONTENT_URI, contentValues, "contact_id=" + cursorQuery.getString(cursorQuery.getColumnIndex("contact_id")), null);
            }
        }
        Log.d(TAG, "Updated TIMES_CONTACTED");
    }
}
