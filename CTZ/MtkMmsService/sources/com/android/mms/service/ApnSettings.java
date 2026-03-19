package com.android.mms.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.NetworkUtils;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import com.android.mms.service.exception.ApnException;
import java.net.URI;
import java.net.URISyntaxException;

public class ApnSettings {
    private static final String[] APN_PROJECTION = {"type", "mmsc", "mmsproxy", "mmsport", "name", "apn", "bearer_bitmask", "protocol", "roaming_protocol", "authtype", "mvno_type", "mvno_match_data", "proxy", "port", "server", "user", "password"};
    private final String mDebugText;
    private final String mProxyAddress;
    private final int mProxyPort;
    private final String mServiceCenter;

    public static ApnSettings load(Context context, String str, int i, String str2) throws Throwable {
        String str3;
        String[] strArr;
        Cursor cursorQuery;
        LogUtil.i(str2, "Loading APN using name " + str);
        String strTrim = str != null ? str.trim() : null;
        if (TextUtils.isEmpty(strTrim)) {
            str3 = null;
            strArr = null;
        } else {
            str3 = "apn=?";
            strArr = new String[]{strTrim};
        }
        try {
            cursorQuery = SqliteWrapper.query(context, context.getContentResolver(), Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "/subId/" + i), APN_PROJECTION, str3, strArr, (String) null);
            if (cursorQuery != null) {
                int i2 = 80;
                while (cursorQuery.moveToNext()) {
                    try {
                        if (isValidApnType(cursorQuery.getString(0), "mms")) {
                            String strTrimWithNullCheck = trimWithNullCheck(cursorQuery.getString(1));
                            if (!TextUtils.isEmpty(strTrimWithNullCheck)) {
                                String strTrimV4AddrZeros = NetworkUtils.trimV4AddrZeros(strTrimWithNullCheck);
                                try {
                                    new URI(strTrimV4AddrZeros);
                                    String strTrimWithNullCheck2 = trimWithNullCheck(cursorQuery.getString(2));
                                    if (!TextUtils.isEmpty(strTrimWithNullCheck2)) {
                                        strTrimWithNullCheck2 = NetworkUtils.trimV4AddrZeros(strTrimWithNullCheck2);
                                        String strTrimWithNullCheck3 = trimWithNullCheck(cursorQuery.getString(3));
                                        if (!TextUtils.isEmpty(strTrimWithNullCheck3)) {
                                            try {
                                                i2 = Integer.parseInt(strTrimWithNullCheck3);
                                            } catch (NumberFormatException e) {
                                                LogUtil.e(str2, "Invalid port " + strTrimWithNullCheck3 + ", use 80");
                                            }
                                        }
                                    }
                                    ApnSettings apnSettings = new ApnSettings(strTrimV4AddrZeros, strTrimWithNullCheck2, i2, getDebugText(cursorQuery));
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    return apnSettings;
                                } catch (URISyntaxException e2) {
                                    throw new ApnException("Invalid MMSC url " + strTrimV4AddrZeros);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw new ApnException("Can not find valid APN");
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private static String getDebugText(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        sb.append("APN [");
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String columnName = cursor.getColumnName(i);
            String string = cursor.getString(i);
            if (!TextUtils.isEmpty(string)) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(columnName);
                sb.append('=');
                sb.append(string);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String trimWithNullCheck(String str) {
        if (str != null) {
            return str.trim();
        }
        return null;
    }

    public ApnSettings(String str, String str2, int i, String str3) {
        this.mServiceCenter = str;
        this.mProxyAddress = str2;
        this.mProxyPort = i;
        this.mDebugText = str3;
    }

    public String getMmscUrl() {
        return this.mServiceCenter;
    }

    public String getProxyAddress() {
        return this.mProxyAddress;
    }

    public int getProxyPort() {
        return this.mProxyPort;
    }

    public boolean isProxySet() {
        return !TextUtils.isEmpty(this.mProxyAddress);
    }

    private static boolean isValidApnType(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }
        for (String str3 : str.split(",")) {
            String strTrim = str3.trim();
            if (strTrim.equals(str2) || strTrim.equals("*")) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return this.mDebugText;
    }
}
