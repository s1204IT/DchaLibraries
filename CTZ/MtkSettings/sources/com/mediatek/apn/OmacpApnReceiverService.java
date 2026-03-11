package com.mediatek.apn;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import java.util.ArrayList;
import java.util.HashMap;

public class OmacpApnReceiverService extends IntentService {
    private static int sAuthType = -1;
    private String mApn;
    private String mApnId;
    private String mAuthType;
    private ArrayList<Intent> mIntentList;
    private boolean mIsMmsApn;
    private String mMcc;
    private String mMmsPort;
    private String mMmsProxy;
    private String mMmsc;
    private String mMnc;
    private String mName;
    private String mNapId;
    private String mNumeric;
    private String mPassword;
    private String mPort;
    private Uri mPreferedUri;
    private String mProxy;
    private String mProxyId;
    private boolean mResult;
    private String mServer;
    private int mSubId;
    private String mType;
    private Uri mUri;
    private String mUserName;

    public OmacpApnReceiverService() {
        super("OmacpApnReceiverService");
        this.mIsMmsApn = false;
        this.mResult = true;
    }

    @Override
    protected void onHandleIntent(Intent intent) throws Throwable {
        String action = intent.getAction();
        Log.d("OmacpApnReceiverService", "get action = " + action);
        if (!"com.mediatek.apn.action.start.omacpservice".equals(action)) {
            return;
        }
        this.mIntentList = ((Intent) intent.getParcelableExtra("android.intent.extra.INTENT")).getParcelableArrayListExtra("apn_setting_intent");
        if (this.mIntentList == null) {
            this.mResult = false;
            sendFeedback(this);
            Log.e("OmacpApnReceiverService", "mIntentList == null");
            return;
        }
        int size = this.mIntentList.size();
        Log.d("OmacpApnReceiverService", "apn list size is " + size);
        if (size <= 0) {
            this.mResult = false;
            sendFeedback(this);
            Log.e("OmacpApnReceiverService", "Intent list size is wrong");
            return;
        }
        if (!initState(this.mIntentList.get(0))) {
            sendFeedback(this);
            Log.e("OmacpApnReceiverService", "Can not get MCC+MNC");
            return;
        }
        this.mUri = Telephony.Carriers.CONTENT_URI;
        Log.d("OmacpApnReceiverService", "mUri = " + this.mUri + " mNumeric = " + this.mNumeric + " mPreferedUri = " + this.mPreferedUri);
        for (int i = 0; this.mResult && i < size; i++) {
            extractAPN(this.mIntentList.get(i), this);
            ContentValues contentValues = new ContentValues();
            validateProfile(contentValues);
            updateApn(this, this.mUri, this.mApn, this.mApnId, this.mName, contentValues, this.mNumeric, this.mPreferedUri);
        }
        sendFeedback(this);
    }

    private void sendFeedback(Context context) {
        Intent intent = new Intent();
        intent.setAction("com.mediatek.omacp.protected.settings.result");
        intent.putExtra("appId", "apn");
        intent.putExtra("result", this.mResult);
        context.sendBroadcast(intent);
    }

    private void validateProfile(ContentValues contentValues) {
        contentValues.put(ApnUtils.PROJECTION[1], this.mName);
        contentValues.put(ApnUtils.PROJECTION[2], ApnUtils.checkNotSet(this.mApn));
        contentValues.put(ApnUtils.PROJECTION[3], ApnUtils.checkNotSet(this.mProxy));
        contentValues.put(ApnUtils.PROJECTION[4], ApnUtils.checkNotSet(this.mPort));
        contentValues.put(ApnUtils.PROJECTION[5], ApnUtils.checkNotSet(this.mUserName));
        contentValues.put(ApnUtils.PROJECTION[6], ApnUtils.checkNotSet(this.mServer));
        contentValues.put(ApnUtils.PROJECTION[7], ApnUtils.checkNotSet(this.mPassword));
        contentValues.put(ApnUtils.PROJECTION[8], ApnUtils.checkNotSet(this.mMmsc));
        contentValues.put(ApnUtils.PROJECTION[9], this.mMcc);
        contentValues.put(ApnUtils.PROJECTION[10], this.mMnc);
        contentValues.put(ApnUtils.PROJECTION[12], ApnUtils.checkNotSet(this.mMmsProxy));
        contentValues.put(ApnUtils.PROJECTION[13], ApnUtils.checkNotSet(this.mMmsPort));
        contentValues.put(ApnUtils.PROJECTION[14], Integer.valueOf(sAuthType));
        contentValues.put(ApnUtils.PROJECTION[15], ApnUtils.checkNotSet(this.mType));
        contentValues.put(ApnUtils.PROJECTION[16], (Integer) 2);
        contentValues.put(ApnUtils.PROJECTION[17], ApnUtils.checkNotSet(this.mApnId));
        contentValues.put(ApnUtils.PROJECTION[18], ApnUtils.checkNotSet(this.mNapId));
        contentValues.put(ApnUtils.PROJECTION[19], ApnUtils.checkNotSet(this.mProxyId));
        contentValues.put(ApnUtils.PROJECTION[11], this.mNumeric);
    }

    private boolean verifyMccMnc() {
        if (this.mNumeric != null && this.mNumeric.length() > 4) {
            String strSubstring = this.mNumeric.substring(0, 3);
            String strSubstring2 = this.mNumeric.substring(3);
            this.mMcc = strSubstring;
            this.mMnc = strSubstring2;
            Log.d("OmacpApnReceiverService", "mcc&mnc is right , mMcc = " + this.mMcc + " mMnc = " + this.mMnc);
        } else {
            this.mResult = false;
            Log.d("OmacpApnReceiverService", "mcc&mnc is wrong , set mResult = false");
        }
        return this.mResult;
    }

    private void getPort(Intent intent) {
        this.mPort = null;
        ArrayList arrayList = (ArrayList) intent.getExtra("PORT");
        if (arrayList != null && arrayList.size() > 0) {
            this.mPort = (String) ((HashMap) arrayList.get(0)).get("PORTNBR");
        }
    }

    private void getNapAuthInfo(Intent intent) {
        this.mUserName = null;
        this.mPassword = null;
        this.mAuthType = null;
        sAuthType = -1;
        ArrayList arrayList = (ArrayList) intent.getExtra("NAPAUTHINFO");
        if (arrayList != null && arrayList.size() > 0) {
            HashMap map = (HashMap) arrayList.get(0);
            this.mUserName = (String) map.get("AUTHNAME");
            this.mPassword = (String) map.get("AUTHSECRET");
            this.mAuthType = (String) map.get("AUTHTYPE");
            if (this.mAuthType != null) {
                if ("PAP".equalsIgnoreCase(this.mAuthType)) {
                    sAuthType = 1;
                } else if ("CHAP".equalsIgnoreCase(this.mAuthType)) {
                    sAuthType = 2;
                } else {
                    sAuthType = 3;
                }
            }
        }
    }

    private void extractAPN(Intent intent, Context context) {
        this.mName = intent.getStringExtra("NAP-NAME");
        if (this.mName == null || this.mName.length() < 1) {
            this.mName = context.getResources().getString(R.string.error_name_empty);
        }
        this.mApn = intent.getStringExtra("NAP-ADDRESS");
        this.mProxy = intent.getStringExtra("PXADDR");
        getPort(intent);
        getNapAuthInfo(intent);
        this.mServer = intent.getStringExtra("SERVER");
        this.mMmsc = intent.getStringExtra("MMSC");
        this.mMmsProxy = intent.getStringExtra("MMS-PROXY");
        this.mMmsPort = intent.getStringExtra("MMS-PORT");
        this.mType = intent.getStringExtra("APN-TYPE");
        this.mApnId = intent.getStringExtra("APN-ID");
        this.mNapId = intent.getStringExtra("NAPID");
        this.mProxyId = intent.getStringExtra("PROXY-ID");
        this.mIsMmsApn = "mms".equalsIgnoreCase(this.mType);
        Log.d("OmacpApnReceiverService", "extractAPN: mName: " + this.mName + " | mApn: " + this.mApn + " | mProxy: " + this.mProxy + " | mServer: " + this.mServer + " | mMmsc: " + this.mMmsc + " | mMmsProxy: " + this.mMmsProxy + " | mMmsPort: " + this.mMmsPort + " | mType: " + this.mType + " | mApnId: " + this.mApnId + " | mNapId: " + this.mNapId + " | mMmsPort: " + this.mMmsPort + " | mProxyId: " + this.mProxyId + " | mIsMmsApn: " + this.mIsMmsApn);
    }

    private boolean setCurrentApn(Context context, long j, Uri uri) {
        int iUpdate;
        ContentValues contentValues = new ContentValues();
        contentValues.put("apn_id", Long.valueOf(j));
        try {
            iUpdate = context.getContentResolver().update(uri, contentValues, null, null);
            try {
                Log.d("OmacpApnReceiverService", "update preferred uri ,row = " + iUpdate);
            } catch (SQLException e) {
                Log.d("OmacpApnReceiverService", "SetCurrentApn SQLException happened!");
            }
        } catch (SQLException e2) {
            iUpdate = 0;
        }
        return iUpdate > 0;
    }

    private void updateApn(Context context, Uri uri, String str, String str2, String str3, ContentValues contentValues, String str4, Uri uri2) throws Throwable {
        long jReplaceApn = UtilsExt.getApnSettingsExt(context).replaceApn(replaceApn(context, uri, str, str2, str3, contentValues, str4), context, uri, str, str3, contentValues, str4);
        Log.d("OmacpApnReceiverService", "replace number = " + jReplaceApn);
        if (jReplaceApn == -1) {
            try {
                Uri uriInsert = context.getContentResolver().insert(uri, addMVNOItem(contentValues));
                if (uriInsert != null) {
                    Log.d("OmacpApnReceiverService", "uri = " + uriInsert);
                    if (uriInsert.getPathSegments().size() == 2) {
                        long j = Long.parseLong(uriInsert.getLastPathSegment());
                        try {
                            Log.d("OmacpApnReceiverService", "insert row id = " + j);
                            jReplaceApn = j;
                        } catch (SQLException e) {
                            jReplaceApn = j;
                            Log.d("OmacpApnReceiverService", "insert SQLException happened!");
                            this.mResult = false;
                        }
                    }
                }
            } catch (SQLException e2) {
            }
        }
        Log.d("OmacpApnReceiverService", "insert number = " + jReplaceApn);
        if (this.mIsMmsApn) {
            if (jReplaceApn == -1) {
                this.mResult = false;
                Log.d("OmacpApnReceiverService", "mms, insertNum is APN_NO_UPDATE ,mResult = false");
                return;
            }
            return;
        }
        if (jReplaceApn == -1) {
            this.mResult = false;
            Log.d("OmacpApnReceiverService", "not mms, insertNum is APN_NO_UPDATE, mResult = false");
        } else {
            if (jReplaceApn == 0) {
                this.mResult = true;
                Log.d("OmacpApnReceiverService", "not mms, insertNum is APN_EXIST, mResult = true");
                return;
            }
            this.mResult = setCurrentApn(context, jReplaceApn, uri2);
            Log.d("OmacpApnReceiverService", "set current apn result, mResult = " + this.mResult);
        }
    }

    ContentValues addMVNOItem(ContentValues contentValues) {
        contentValues.put("mvno_type", ApnUtils.checkNotSet(null));
        contentValues.put("mvno_match_data", ApnUtils.checkNotSet(null));
        return contentValues;
    }

    private boolean initState(Intent intent) {
        this.mSubId = intent.getIntExtra("subId", -1);
        if (this.mSubId == -1) {
            Log.w("OmacpApnReceiverService", "Need to check reason not pass subId");
            this.mSubId = SubscriptionManager.getDefaultSubscriptionId();
        }
        this.mNumeric = TelephonyManager.getDefault().getSimOperator(this.mSubId);
        this.mPreferedUri = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers/preferapn/subId/"), this.mSubId);
        Log.d("OmacpApnReceiverService", "initState: mSimId: " + this.mSubId + " | mNumeric: " + this.mNumeric + " | mPreferedUri: " + this.mPreferedUri);
        return verifyMccMnc();
    }

    public long replaceApn(Context context, Uri uri, String str, String str2, String str3, ContentValues contentValues, String str4) throws Throwable {
        Cursor cursorQuery;
        String str5 = "numeric=\"" + str4 + "\" and omacpid<>''";
        Log.d("OmacpApnReceiverService", "name " + str3 + " numeric = " + str4 + " apnId = " + str2);
        try {
            cursorQuery = context.getContentResolver().query(uri, new String[]{"_id", "omacpid"}, str5, null, "name ASC");
            long j = -1;
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() != 0) {
                        cursorQuery.moveToFirst();
                        while (true) {
                            if (cursorQuery.isAfterLast()) {
                                break;
                            }
                            Log.d("OmacpApnReceiverService", "apnId " + str2 + " getApnId = " + cursorQuery.getString(1));
                            if (str2.equals(cursorQuery.getString(1))) {
                                j = 0;
                                break;
                            }
                            cursorQuery.moveToNext();
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return j;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            Log.d("OmacpApnReceiverService", "cursor is null , or cursor.getCount() == 0 return");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return -1L;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }
}
