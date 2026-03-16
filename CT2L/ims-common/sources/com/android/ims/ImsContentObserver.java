package com.android.ims;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.Rlog;

public class ImsContentObserver extends ContentObserver {
    private static final boolean DBG = true;
    public static final String KEY_KEY = "key";
    public static final String KEY_MO_SMS_ENCODING = "KEY_MO_SMS_ENCODING";
    public static final String KEY_MO_SMS_FORMAT = "KEY_MO_SMS_FORMAT";
    public static final String KEY_SMS_PREFERENCE = "KEY_SMS_PREFERENCE";
    public static final int MSG_SMS_FORMAT_UPDATE = 1010;
    public static final String SMS_ENCODING_ASCII_7BIT = "ascii7";
    public static final String SMS_ENCODING_GSM_7BIT = "gsm7";
    public static final String SMS_ENCODING_UCS2 = "ucs2";
    private static final String TAG = "ImsContentObserver";
    public ContentResolver mContentResolver;
    public Context mContext;
    private Handler mHandler;
    public static final Uri ims_uri = Uri.parse("content://com.marvell.ims.provider.settings/settings");
    public static final Uri imsSms_uri = Uri.parse("content://com.marvell.ims.provider.settings/settings/imssms");

    public ImsContentObserver(Context context, Handler handler) {
        super(handler);
        this.mContext = context;
        this.mHandler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {
        String result = null;
        ContentResolver cr = this.mContext.getContentResolver();
        Cursor c = cr.query(ims_uri, null, "key='KEY_MO_SMS_FORMAT'", null, null);
        if (c != null && c.getCount() > 0) {
            if (c.moveToFirst()) {
                result = c.getString(2);
            }
            c.close();
        }
        if (result != null) {
            this.mHandler.obtainMessage(MSG_SMS_FORMAT_UPDATE, result).sendToTarget();
            log("send sms format update message out");
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }
}
