package com.android.bluetooth.map;

import android.util.Log;

public class BluetoothMapUtils {
    private static final boolean D = true;
    private static final long HANDLE_TYPE_EMAIL_MASK = 144115188075855872L;
    private static final long HANDLE_TYPE_MASK = 1080863910568919040L;
    private static final long HANDLE_TYPE_MMS_MASK = 72057594037927936L;
    private static final long HANDLE_TYPE_SMS_CDMA_MASK = 576460752303423488L;
    private static final long HANDLE_TYPE_SMS_GSM_MASK = 288230376151711744L;
    private static final String TAG = "MapUtils";
    private static final boolean V = false;

    public enum TYPE {
        EMAIL,
        SMS_GSM,
        SMS_CDMA,
        MMS
    }

    public static String getLongAsString(long v) {
        char[] result = new char[16];
        int v1 = (int) (v & (-1));
        int v2 = (int) ((v >> 32) & (-1));
        for (int i = 0; i < 8; i++) {
            int c = v2 & 15;
            result[7 - i] = (char) (c + (c < 10 ? 48 : 55));
            v2 >>= 4;
            int c2 = v1 & 15;
            result[15 - i] = (char) (c2 + (c2 < 10 ? 48 : 55));
            v1 >>= 4;
        }
        return new String(result);
    }

    public static String getMapHandle(long cpHandle, TYPE messageType) {
        switch (messageType) {
            case MMS:
                String mapHandle = getLongAsString(HANDLE_TYPE_MMS_MASK | cpHandle);
                return mapHandle;
            case SMS_GSM:
                String mapHandle2 = getLongAsString(HANDLE_TYPE_SMS_GSM_MASK | cpHandle);
                return mapHandle2;
            case SMS_CDMA:
                String mapHandle3 = getLongAsString(HANDLE_TYPE_SMS_CDMA_MASK | cpHandle);
                return mapHandle3;
            case EMAIL:
                String mapHandle4 = getLongAsString(HANDLE_TYPE_EMAIL_MASK | cpHandle);
                return mapHandle4;
            default:
                throw new IllegalArgumentException("Message type not supported");
        }
    }

    public static long getMsgHandleAsLong(String mapHandle) {
        return Long.parseLong(mapHandle, 16);
    }

    public static long getCpHandle(String mapHandle) {
        long cpHandle = getMsgHandleAsLong(mapHandle);
        Log.d(TAG, "-> MAP handle:" + mapHandle);
        long cpHandle2 = cpHandle & (-1080863910568919041L);
        Log.d(TAG, "->CP handle:" + cpHandle2);
        return cpHandle2;
    }

    public static TYPE getMsgTypeFromHandle(String mapHandle) {
        long cpHandle = getMsgHandleAsLong(mapHandle);
        if ((HANDLE_TYPE_MMS_MASK & cpHandle) != 0) {
            return TYPE.MMS;
        }
        if ((HANDLE_TYPE_EMAIL_MASK & cpHandle) != 0) {
            return TYPE.EMAIL;
        }
        if ((HANDLE_TYPE_SMS_GSM_MASK & cpHandle) != 0) {
            return TYPE.SMS_GSM;
        }
        if ((HANDLE_TYPE_SMS_CDMA_MASK & cpHandle) != 0) {
            return TYPE.SMS_CDMA;
        }
        throw new IllegalArgumentException("Message type not found in handle string.");
    }
}
