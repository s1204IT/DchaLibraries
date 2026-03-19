package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import java.util.Date;

public class CarrierInfoManager {
    private static final String KEY_TYPE = "KEY_TYPE";
    private static final String LOG_TAG = "CarrierInfoManager";
    private static final int RESET_CARRIER_KEY_RATE_LIMIT = 43200000;
    private long mLastAccessResetCarrierKey = 0;

    public static ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i, Context context) throws Throwable {
        Cursor cursorQuery;
        String simOperator = ((TelephonyManager) context.getSystemService("phone")).getSimOperator();
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(LOG_TAG, "Invalid networkOperator: " + simOperator);
            return null;
        }
        String strSubstring = simOperator.substring(0, 3);
        String strSubstring2 = simOperator.substring(3);
        ?? r0 = LOG_TAG;
        Log.i(LOG_TAG, "using values for mnc, mcc: " + strSubstring2 + "," + strSubstring);
        try {
            try {
                cursorQuery = context.getContentResolver().query(Telephony.CarrierColumns.CONTENT_URI, new String[]{"public_key", "expiration_time", "key_identifier"}, "mcc=? and mnc=? and key_type=?", new String[]{strSubstring, strSubstring2, String.valueOf(i)}, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            if (cursorQuery.getCount() > 1) {
                                Log.e(LOG_TAG, "More than 1 row found for the keyType: " + i);
                            }
                            ImsiEncryptionInfo imsiEncryptionInfo = new ImsiEncryptionInfo(strSubstring, strSubstring2, i, cursorQuery.getString(2), cursorQuery.getBlob(0), new Date(cursorQuery.getLong(1)));
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return imsiEncryptionInfo;
                        }
                    } catch (IllegalArgumentException e) {
                        e = e;
                        Log.e(LOG_TAG, "Bad arguments:" + e);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return null;
                    } catch (Exception e2) {
                        e = e2;
                        Log.e(LOG_TAG, "Query failed:" + e);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return null;
                    }
                }
                Log.d(LOG_TAG, "No rows found for keyType: " + i);
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return null;
            } catch (Throwable th) {
                th = th;
                if (r0 != 0) {
                    r0.close();
                }
                throw th;
            }
        } catch (IllegalArgumentException e3) {
            e = e3;
            cursorQuery = null;
        } catch (Exception e4) {
            e = e4;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            r0 = 0;
            if (r0 != 0) {
            }
            throw th;
        }
    }

    public static void updateOrInsertCarrierKey(ImsiEncryptionInfo imsiEncryptionInfo, Context context, int i) {
        byte[] encoded = imsiEncryptionInfo.getPublicKey().getEncoded();
        ContentResolver contentResolver = context.getContentResolver();
        TelephonyMetrics telephonyMetrics = TelephonyMetrics.getInstance();
        ContentValues contentValues = new ContentValues();
        contentValues.put("mcc", imsiEncryptionInfo.getMcc());
        contentValues.put("mnc", imsiEncryptionInfo.getMnc());
        contentValues.put("key_type", Integer.valueOf(imsiEncryptionInfo.getKeyType()));
        contentValues.put("key_identifier", imsiEncryptionInfo.getKeyIdentifier());
        contentValues.put("public_key", encoded);
        contentValues.put("expiration_time", Long.valueOf(imsiEncryptionInfo.getExpirationTime().getTime()));
        boolean z = false;
        try {
            try {
                Log.i(LOG_TAG, "Inserting imsiEncryptionInfo into db");
                contentResolver.insert(Telephony.CarrierColumns.CONTENT_URI, contentValues);
            } catch (SQLiteConstraintException e) {
                Log.i(LOG_TAG, "Insert failed, updating imsiEncryptionInfo into db");
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("public_key", encoded);
                contentValues2.put("expiration_time", Long.valueOf(imsiEncryptionInfo.getExpirationTime().getTime()));
                contentValues2.put("key_identifier", imsiEncryptionInfo.getKeyIdentifier());
                try {
                    if (contentResolver.update(Telephony.CarrierColumns.CONTENT_URI, contentValues2, "mcc=? and mnc=? and key_type=?", new String[]{imsiEncryptionInfo.getMcc(), imsiEncryptionInfo.getMnc(), String.valueOf(imsiEncryptionInfo.getKeyType())}) == 0) {
                        Log.d(LOG_TAG, "Error updating values:" + imsiEncryptionInfo);
                    } else {
                        z = true;
                    }
                } catch (Exception e2) {
                    Log.d(LOG_TAG, "Error updating values:" + imsiEncryptionInfo + e2);
                }
                telephonyMetrics.writeCarrierKeyEvent(i, imsiEncryptionInfo.getKeyType(), z);
            } catch (Exception e3) {
                Log.d(LOG_TAG, "Error inserting/updating values:" + imsiEncryptionInfo + e3);
                telephonyMetrics.writeCarrierKeyEvent(i, imsiEncryptionInfo.getKeyType(), z);
            }
        } finally {
            telephonyMetrics.writeCarrierKeyEvent(i, imsiEncryptionInfo.getKeyType(), true);
        }
    }

    public static void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo, Context context, int i) {
        Log.i(LOG_TAG, "inserting carrier key: " + imsiEncryptionInfo);
        updateOrInsertCarrierKey(imsiEncryptionInfo, context, i);
    }

    public void resetCarrierKeysForImsiEncryption(Context context, int i) {
        Log.i(LOG_TAG, "resetting carrier key");
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - this.mLastAccessResetCarrierKey < 43200000) {
            Log.i(LOG_TAG, "resetCarrierKeysForImsiEncryption: Access rate exceeded");
            return;
        }
        this.mLastAccessResetCarrierKey = jCurrentTimeMillis;
        deleteCarrierInfoForImsiEncryption(context);
        Intent intent = new Intent("com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD");
        intent.putExtra("phone", i);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public static void deleteCarrierInfoForImsiEncryption(Context context) {
        Log.i(LOG_TAG, "deleting carrier key from db");
        String simOperator = ((TelephonyManager) context.getSystemService("phone")).getSimOperator();
        if (!TextUtils.isEmpty(simOperator)) {
            try {
                context.getContentResolver().delete(Telephony.CarrierColumns.CONTENT_URI, "mcc=? and mnc=?", new String[]{simOperator.substring(0, 3), simOperator.substring(3)});
                return;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Delete failed" + e);
                return;
            }
        }
        Log.e(LOG_TAG, "Invalid networkOperator: " + simOperator);
    }

    public static void deleteAllCarrierKeysForImsiEncryption(Context context) {
        Log.i(LOG_TAG, "deleting ALL carrier keys from db");
        try {
            context.getContentResolver().delete(Telephony.CarrierColumns.CONTENT_URI, null, null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Delete failed" + e);
        }
    }
}
