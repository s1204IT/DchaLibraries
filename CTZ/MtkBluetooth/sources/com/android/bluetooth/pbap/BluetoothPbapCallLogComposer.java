package com.android.bluetooth.pbap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.map.BluetoothMapContentObserver;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardConstants;
import com.android.vcard.VCardUtils;
import java.util.Arrays;

public class BluetoothPbapCallLogComposer {
    private static final int CALLER_NAME_COLUMN_INDEX = 3;
    private static final int CALLER_NUMBERLABEL_COLUMN_INDEX = 5;
    private static final int CALLER_NUMBERTYPE_COLUMN_INDEX = 4;
    private static final int CALL_TYPE_COLUMN_INDEX = 2;
    private static final int DATE_COLUMN_INDEX = 1;
    private static final String FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO = "Failed to get database information";
    private static final String FAILURE_REASON_NOT_INITIALIZED = "The vCard composer object is not correctly initialized";
    private static final String FAILURE_REASON_NO_ENTRY = "There's no exportable in the database";
    private static final String FAILURE_REASON_UNSUPPORTED_URI = "The Uri vCard composer received is not supported by the composer.";
    private static final String NO_ERROR = "No error";
    private static final int NUMBER_COLUMN_INDEX = 0;
    private static final int NUMBER_PRESENTATION_COLUMN_INDEX = 6;
    private static final String TAG = "CallLogComposer";
    private static final String VCARD_PROPERTY_CALLTYPE_INCOMING = "RECEIVED";
    private static final String VCARD_PROPERTY_CALLTYPE_MISSED = "MISSED";
    private static final String VCARD_PROPERTY_CALLTYPE_OUTGOING = "DIALED";
    private static final String VCARD_PROPERTY_X_TIMESTAMP = "X-IRMC-CALL-DATETIME";
    private static final String[] sCallLogProjection = {"number", BluetoothMapContract.MessageColumns.DATE, BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, "name", "numbertype", "numberlabel", "presentation"};
    private ContentResolver mContentResolver;
    private final Context mContext;
    private Cursor mCursor;
    private String mErrorReason = "No error";
    private boolean mTerminateIsCalled;

    public BluetoothPbapCallLogComposer(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public boolean init(Uri uri, String str, String[] strArr, String str2) {
        if (CallLog.Calls.CONTENT_URI.equals(uri)) {
            this.mCursor = this.mContentResolver.query(uri, sCallLogProjection, str, strArr, str2);
            if (this.mCursor == null) {
                this.mErrorReason = "Failed to get database information";
                return false;
            }
            if (this.mCursor.getCount() == 0 || !this.mCursor.moveToFirst()) {
                try {
                    try {
                        this.mCursor.close();
                    } catch (SQLiteException e) {
                        Log.e(TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
                    }
                    return false;
                } finally {
                    this.mErrorReason = "There's no exportable in the database";
                    this.mCursor = null;
                }
            }
            return true;
        }
        this.mErrorReason = "The Uri vCard composer received is not supported by the composer.";
        return false;
    }

    public String createOneEntry(boolean z) {
        if (this.mCursor == null || this.mCursor.isAfterLast()) {
            this.mErrorReason = "The vCard composer object is not correctly initialized";
            return null;
        }
        try {
            return createOneCallLogEntryInternal(z);
        } finally {
            this.mCursor.moveToNext();
        }
    }

    private String createOneCallLogEntryInternal(boolean z) {
        VCardBuilder vCardBuilder = new VCardBuilder((z ? VCardConfig.VCARD_TYPE_V21_GENERIC : VCardConfig.VCARD_TYPE_V30_GENERIC) | VCardConfig.FLAG_REFRAIN_PHONE_NUMBER_FORMATTING);
        String string = this.mCursor.getString(3);
        String string2 = this.mCursor.getString(0);
        int i = this.mCursor.getInt(6);
        if (TextUtils.isEmpty(string)) {
            string = "";
        }
        if (i != 1) {
            string = "";
            string2 = this.mContext.getString(R.string.unknownNumber);
        }
        boolean z2 = !VCardUtils.containsOnlyPrintableAscii(string);
        vCardBuilder.appendLine(VCardConstants.PROPERTY_FN, string, z2, false);
        vCardBuilder.appendLine(VCardConstants.PROPERTY_N, string, z2, false);
        int i2 = this.mCursor.getInt(4);
        String string3 = this.mCursor.getString(5);
        if (TextUtils.isEmpty(string3)) {
            string3 = Integer.toString(i2);
        }
        vCardBuilder.appendTelLine(Integer.valueOf(i2), string3, string2, false);
        tryAppendCallHistoryTimeStampField(vCardBuilder);
        return vCardBuilder.toString();
    }

    public String composeVCardForPhoneOwnNumber(int i, String str, String str2, boolean z) {
        VCardBuilder vCardBuilder = new VCardBuilder((z ? VCardConfig.VCARD_TYPE_V21_GENERIC : VCardConfig.VCARD_TYPE_V30_GENERIC) | VCardConfig.FLAG_REFRAIN_PHONE_NUMBER_FORMATTING);
        boolean z2 = true;
        if (VCardUtils.containsOnlyPrintableAscii(str)) {
            z2 = false;
        }
        vCardBuilder.appendLine(VCardConstants.PROPERTY_FN, str, z2, false);
        vCardBuilder.appendLine(VCardConstants.PROPERTY_N, str, z2, false);
        if (!TextUtils.isEmpty(str2)) {
            vCardBuilder.appendTelLine(Integer.valueOf(i), Integer.toString(i), str2, false);
        }
        return vCardBuilder.toString();
    }

    private String toRfc2455Format(long j) {
        Time time = new Time();
        time.set(j);
        return time.format2445();
    }

    private void tryAppendCallHistoryTimeStampField(VCardBuilder vCardBuilder) {
        String str;
        int i = this.mCursor.getInt(2);
        if (i != 5) {
            switch (i) {
                case 1:
                    str = VCARD_PROPERTY_CALLTYPE_INCOMING;
                    break;
                case 2:
                    str = VCARD_PROPERTY_CALLTYPE_OUTGOING;
                    break;
                case 3:
                    str = VCARD_PROPERTY_CALLTYPE_MISSED;
                    break;
                default:
                    Log.w(TAG, "Call log type not correct.");
                    return;
            }
        }
        vCardBuilder.appendLine(VCARD_PROPERTY_X_TIMESTAMP, Arrays.asList(str), toRfc2455Format(this.mCursor.getLong(1)));
    }

    public void terminate() {
        if (this.mCursor != null) {
            try {
                this.mCursor.close();
            } catch (SQLiteException e) {
                Log.e(TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
            }
            this.mCursor = null;
        }
        this.mTerminateIsCalled = true;
    }

    public void finalize() {
        if (!this.mTerminateIsCalled) {
            terminate();
        }
    }

    public int getCount() {
        if (this.mCursor == null) {
            return 0;
        }
        return this.mCursor.getCount();
    }

    public boolean isAfterLast() {
        if (this.mCursor == null) {
            return false;
        }
        return this.mCursor.isAfterLast();
    }

    public String getErrorReason() {
        return this.mErrorReason;
    }
}
