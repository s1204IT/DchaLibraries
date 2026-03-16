package com.android.internal.telephony.cdma.sms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CdmaSmsPlugin {
    private static final String KEY_PRIORITY_INDICATION = "Sms3gpp2Priority";
    private static final String KEY_PRIORITY_INDICATION_DISPLAY = "SmsPriorityDisplay";
    private static final String KEY_PRIORITY_INDICATION_ENABLED = "Sms3gpp2PriorityEnabled";
    private static final String LOG_TAG = "CdmaSmsPlugin";
    private static final String SMS_3GPP2_CALLBACK_NUMBER_ENABLED = "Sms3gpp2CBEnabled";
    private Context mContext;
    private ContentResolver mCr;
    private TelephonyManager mTm;
    private static CdmaSmsPlugin sInstance = null;
    private static final Uri IMS_CONTENT_URI = Uri.parse("content://com.marvell.ims.provider.settings/settings");
    private static final String[] SMS_3GPP2_PRIORITY_INDICATIONS = {"Normal", "Interactive", "Urgent", "Emergency"};

    private CdmaSmsPlugin(Context ctx) {
        this.mContext = null;
        this.mTm = null;
        this.mCr = null;
        this.mContext = ctx;
        this.mTm = (TelephonyManager) ctx.getSystemService("phone");
        this.mCr = ctx.getContentResolver();
    }

    public static void createInstance(Context ctx) {
        sInstance = new CdmaSmsPlugin(ctx);
    }

    public static boolean isPriorityIndicationSet() {
        boolean enabled = false;
        if (sInstance == null) {
            Rlog.w(LOG_TAG, "No instance, Priority Indication set will be false.");
        } else if (sInstance.mCr == null) {
            Rlog.w(LOG_TAG, "No ContentResolver, Priority Indication set will be false.");
        } else {
            Cursor cursor = sInstance.mCr.query(IMS_CONTENT_URI, null, "key='Sms3gpp2PriorityEnabled'", null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    enabled = Boolean.valueOf(cursor.getString(2)).booleanValue();
                    Rlog.v(LOG_TAG, "Priority Indication set is " + enabled);
                } else {
                    Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is empty), Priority Indication set will be false.");
                }
                cursor.close();
            } else {
                Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is null), Priority Indication set will be false.");
            }
        }
        return enabled;
    }

    public static int getPriorityIndication() {
        int pi = 0;
        if (sInstance == null) {
            Rlog.w(LOG_TAG, "No instance, Priority Indication - Normal(0) will be encoded.");
        } else if (sInstance.mCr == null) {
            Rlog.w(LOG_TAG, "No ContentResolver, Priority Indication - Normal(0) will be encoded.");
        } else {
            Cursor cursor = sInstance.mCr.query(IMS_CONTENT_URI, null, "key='Sms3gpp2Priority'", null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    pi = Integer.valueOf(cursor.getString(2)).intValue();
                    Rlog.v(LOG_TAG, "Priority Indication " + pi + " will be encoded.");
                } else {
                    Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is empty), Priority Indication - Normal(0) will be encoded.");
                }
                cursor.close();
            } else {
                Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is null), Priority Indication - Normal(0) will be encoded.");
            }
        }
        return pi;
    }

    public static String getCallbackNumber() {
        if (sInstance == null) {
            Rlog.w(LOG_TAG, "No instance, Callback Number will NOT be encoded.");
            return null;
        }
        if (sInstance.mTm == null) {
            Rlog.w(LOG_TAG, "No TelephonyManager, Callback Number will NOT be encoded.");
            return null;
        }
        boolean enabled = false;
        String cn = null;
        Cursor cursor = sInstance.mCr.query(IMS_CONTENT_URI, null, "key='Sms3gpp2CBEnabled'", null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                enabled = Boolean.valueOf(cursor.getString(2)).booleanValue();
                Rlog.v(LOG_TAG, "Callback Number was " + (enabled ? "enabled" : "disabled") + ".");
            } else {
                Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is empty), Callback Number will NOT be encoded.");
            }
            cursor.close();
        } else {
            Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is null), Callback Number will NOT be encoded.");
        }
        if (enabled) {
            cn = sInstance.mTm.getLine1Number();
            Rlog.v(LOG_TAG, "Callback Number " + cn + " will be encoded.");
        }
        return cn;
    }

    public static void onSmsReceivedWithPriorityIndication(BearerData bd) {
        if (bd != null && bd.priorityIndicatorSet) {
            if (bd.priority < 0 || 3 < bd.priority) {
                Rlog.w(LOG_TAG, "Invalid Priority Indication " + bd.priority + ", MT SMS with Priority Indication will not be shown.");
                return;
            }
            boolean isShown = false;
            if (sInstance == null) {
                Rlog.w(LOG_TAG, "No instance, MT SMS with Priority Indication will not be shown.");
            } else if (sInstance.mCr == null) {
                Rlog.w(LOG_TAG, "No ContentResolver, MT SMS with Priority Indication will not be shown.");
            } else {
                Cursor cursor = sInstance.mCr.query(IMS_CONTENT_URI, null, "key='SmsPriorityDisplay'", null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        isShown = Boolean.valueOf(cursor.getString(2)).booleanValue();
                        Rlog.v(LOG_TAG, "MT SMS with Priority Indication will" + (isShown ? "" : " not") + " be shown.");
                    } else {
                        Rlog.w(LOG_TAG, "MT SMS with Priority Indication will not be shown.");
                    }
                    cursor.close();
                } else {
                    Rlog.w(LOG_TAG, "Can't find item in IMS database(cursor is null), MT SMS with Priority Indication will not be shown.");
                }
            }
            if (isShown) {
                String text = SMS_3GPP2_PRIORITY_INDICATIONS[bd.priority] + " SMS Received.";
                Toast.makeText(sInstance.mContext, text, 1).show();
            }
        }
    }
}
