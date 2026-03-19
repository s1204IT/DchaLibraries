package com.android.bluetooth.opp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.util.Log;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.obex.HeaderSet;

public class Constants {
    static final String ACTION_ACCEPT = "android.btopp.intent.action.ACCEPT";
    static final String ACTION_BT_OPP_TRANSFER_DONE = "android.nfc.handover.intent.action.TRANSFER_DONE";
    static final String ACTION_BT_OPP_TRANSFER_PROGRESS = "android.nfc.handover.intent.action.TRANSFER_PROGRESS";
    static final String ACTION_COMPLETE_HIDE = "android.btopp.intent.action.HIDE_COMPLETE";
    static final String ACTION_DECLINE = "android.btopp.intent.action.DECLINE";
    static final String ACTION_HANDOVER_SEND = "android.nfc.handover.intent.action.HANDOVER_SEND";
    static final String ACTION_HANDOVER_SEND_MULTIPLE = "android.nfc.handover.intent.action.HANDOVER_SEND_MULTIPLE";
    static final String ACTION_HANDOVER_STARTED = "android.nfc.handover.intent.action.HANDOVER_STARTED";
    static final String ACTION_HIDE = "android.btopp.intent.action.HIDE";
    static final String ACTION_INCOMING_FILE_CONFIRM = "android.btopp.intent.action.CONFIRM";
    static final String ACTION_LIST = "android.btopp.intent.action.LIST";
    static final String ACTION_OPEN = "android.btopp.intent.action.OPEN";
    static final String ACTION_OPEN_INBOUND_TRANSFER = "android.btopp.intent.action.OPEN_INBOUND";
    static final String ACTION_OPEN_OUTBOUND_TRANSFER = "android.btopp.intent.action.OPEN_OUTBOUND";
    static final String ACTION_OPEN_RECEIVED_FILES = "android.btopp.intent.action.OPEN_RECEIVED_FILES";
    static final String ACTION_RETRY = "android.btopp.intent.action.RETRY";
    static final String ACTION_STOP_HANDOVER = "android.btopp.intent.action.STOP_HANDOVER_TRANSFER";
    static final String ACTION_WHITELIST_DEVICE = "android.btopp.intent.action.WHITELIST_DEVICE";
    static final int BATCH_STATUS_FAILED = 3;
    static final int BATCH_STATUS_FINISHED = 2;
    static final int BATCH_STATUS_PENDING = 0;
    static final int BATCH_STATUS_RUNNING = 1;
    static final String BLUETOOTHOPP_CHANNEL_PREFERENCE = "btopp_channels";
    static final String BLUETOOTHOPP_NAME_PREFERENCE = "btopp_names";
    static final int COUNT_HEADER_UNAVAILABLE = -1;
    static final String DEFAULT_STORE_SUBDIR = "/bluetooth";
    static final int DIRECTION_BLUETOOTH_INCOMING = 0;
    static final int DIRECTION_BLUETOOTH_OUTGOING = 1;
    static final String EXTRA_BT_OPP_ADDRESS = "android.nfc.handover.intent.extra.ADDRESS";
    static final String EXTRA_BT_OPP_OBJECT_COUNT = "android.nfc.handover.intent.extra.OBJECT_COUNT";
    static final String EXTRA_BT_OPP_TRANSFER_DIRECTION = "android.nfc.handover.intent.extra.TRANSFER_DIRECTION";
    static final String EXTRA_BT_OPP_TRANSFER_ID = "android.nfc.handover.intent.extra.TRANSFER_ID";
    static final String EXTRA_BT_OPP_TRANSFER_MIMETYPE = "android.nfc.handover.intent.extra.TRANSFER_MIME_TYPE";
    static final String EXTRA_BT_OPP_TRANSFER_PROGRESS = "android.nfc.handover.intent.extra.TRANSFER_PROGRESS";
    static final String EXTRA_BT_OPP_TRANSFER_STATUS = "android.nfc.handover.intent.extra.TRANSFER_STATUS";
    static final String EXTRA_BT_OPP_TRANSFER_URI = "android.nfc.handover.intent.extra.TRANSFER_URI";
    static final String EXTRA_SHOW_ALL_FILES = "android.btopp.intent.extra.SHOW_ALL";
    static final String FILENAME_SEQUENCE_SEPARATOR = "-";
    static final String HANDOVER_STATUS_PERMISSION = "android.permission.NFC_HANDOVER_STATUS";
    static final int HANDOVER_TRANSFER_STATUS_FAILURE = 1;
    static final int HANDOVER_TRANSFER_STATUS_SUCCESS = 0;
    static final int MAX_RECORDS_IN_DATABASE = 50;
    static final String MEDIA_SCANNED = "scanned";
    static final int MEDIA_SCANNED_NOT_SCANNED = 0;
    static final int MEDIA_SCANNED_SCANNED_FAILED = 2;
    static final int MEDIA_SCANNED_SCANNED_OK = 1;
    static final int NFC_ALIVE_CHECK_MS = 10000;
    public static final String TAG = "BluetoothOpp";
    static final String THIS_PACKAGE_NAME = "com.android.bluetooth";
    static final String[] ACCEPTABLE_SHARE_INBOUND_TYPES = {"image/*", "video/*", "audio/*", "text/x-vcard", "text/x-vcalendar", "text/calendar", "text/plain", "text/html", "text/xml", "application/zip", "application/vnd.ms-excel", "application/msword", "application/vnd.ms-powerpoint", "application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/x-hwp", "application/vnd.android.package-archive", "application/x-flac", "application/epub+zip"};
    static final boolean DEBUG = !SystemProperties.get("ro.build.type", "").equals("user");
    static final boolean VERBOSE = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");

    static void updateShareStatus(Context context, int i, int i2) {
        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + i);
        ContentValues contentValues = new ContentValues();
        contentValues.put("status", Integer.valueOf(i2));
        context.getContentResolver().update(uri, contentValues, null, null);
        sendIntentIfCompleted(context, uri, i2);
    }

    static void sendIntentIfCompleted(Context context, Uri uri, int i) {
        if (VERBOSE) {
            Log.d(TAG, "sendIntentIfCompleted uri = " + uri + "status = " + i);
        }
        if (BluetoothShare.isStatusCompleted(i)) {
            Intent intent = new Intent(BluetoothShare.TRANSFER_COMPLETED_ACTION);
            intent.setClassName(THIS_PACKAGE_NAME, BluetoothOppReceiver.class.getName());
            intent.setDataAndNormalize(uri);
            context.sendBroadcast(intent);
        }
    }

    static boolean mimeTypeMatches(String str, String[] strArr) {
        for (String str2 : strArr) {
            if (mimeTypeMatches(str, str2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mimeTypeMatches(String str, String str2) {
        return Pattern.compile(str2.replaceAll("\\*", "\\.\\*"), 2).matcher(str).matches();
    }

    static void logHeader(HeaderSet headerSet) {
        Log.v(TAG, "Dumping HeaderSet " + headerSet.toString());
        try {
            Log.v(TAG, "COUNT : " + headerSet.getHeader(BluetoothShare.STATUS_RUNNING));
            Log.v(TAG, "NAME : " + headerSet.getHeader(1));
            Log.v(TAG, "TYPE : " + headerSet.getHeader(66));
            Log.v(TAG, "LENGTH : " + headerSet.getHeader(195));
            Log.v(TAG, "TIME_ISO_8601 : " + headerSet.getHeader(68));
            Log.v(TAG, "TIME_4_BYTE : " + headerSet.getHeader(196));
            Log.v(TAG, "DESCRIPTION : " + headerSet.getHeader(5));
            Log.v(TAG, "TARGET : " + headerSet.getHeader(70));
            Log.v(TAG, "HTTP : " + headerSet.getHeader(71));
            Log.v(TAG, "WHO : " + headerSet.getHeader(74));
            Log.v(TAG, "OBJECT_CLASS : " + headerSet.getHeader(79));
            Log.v(TAG, "APPLICATION_PARAMETER : " + headerSet.getHeader(76));
        } catch (IOException e) {
            Log.e(TAG, "dump HeaderSet error " + e);
        }
    }
}
