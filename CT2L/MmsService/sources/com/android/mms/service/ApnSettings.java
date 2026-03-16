package com.android.mms.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.NetworkUtils;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import com.android.mms.service.exception.ApnException;
import java.net.URI;
import java.net.URISyntaxException;

public class ApnSettings {
    private static final String[] APN_PROJECTION = {"type", "mmsc", "mmsproxy", "mmsport", "name", "apn", "bearer", "protocol", "roaming_protocol", "authtype", "mvno_type", "mvno_match_data", "proxy", "port", "server", "user", "password"};
    private final String mDebugText;
    private final String mProxyAddress;
    private final int mProxyPort;
    private final String mServiceCenter;

    public static ApnSettings load(Context context, String apnName, int subId) throws ApnException {
        if (Log.isLoggable("MmsService", 2)) {
            Log.v("MmsService", "ApnSettings: apnName " + apnName);
        }
        String selection = null;
        String[] selectionArgs = null;
        String apnName2 = apnName != null ? apnName.trim() : null;
        if (!TextUtils.isEmpty(apnName2)) {
            selection = "apn=?";
            selectionArgs = new String[]{apnName2};
        }
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "/subId/" + subId), APN_PROJECTION, selection, selectionArgs, (String) null);
            if (cursor != null) {
                int proxyPort = -1;
                while (cursor.moveToNext()) {
                    if (isValidApnType(cursor.getString(0), "mms")) {
                        String mmscUrl = trimWithNullCheck(cursor.getString(1));
                        if (!TextUtils.isEmpty(mmscUrl)) {
                            String mmscUrl2 = NetworkUtils.trimV4AddrZeros(mmscUrl);
                            try {
                                new URI(mmscUrl2);
                                String proxyAddress = trimWithNullCheck(cursor.getString(2));
                                if (!TextUtils.isEmpty(proxyAddress)) {
                                    proxyAddress = NetworkUtils.trimV4AddrZeros(proxyAddress);
                                    String portString = trimWithNullCheck(cursor.getString(3));
                                    if (portString != null) {
                                        try {
                                            proxyPort = Integer.parseInt(portString);
                                        } catch (NumberFormatException e) {
                                            Log.e("MmsService", "Invalid port " + portString);
                                            throw new ApnException("Invalid port " + portString);
                                        }
                                    }
                                }
                                return new ApnSettings(mmscUrl2, proxyAddress, proxyPort, getDebugText(cursor));
                            } catch (URISyntaxException e2) {
                                throw new ApnException("Invalid MMSC url " + mmscUrl2);
                            }
                        }
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            throw new ApnException("Can not find valid APN");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static String getDebugText(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        sb.append("APN [");
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String name = cursor.getColumnName(i);
            String value = cursor.getString(i);
            if (!TextUtils.isEmpty(value)) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(name).append('=').append(value);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String trimWithNullCheck(String value) {
        if (value != null) {
            return value.trim();
        }
        return null;
    }

    public ApnSettings(String mmscUrl, String proxyAddr, int proxyPort, String debugText) {
        this.mServiceCenter = mmscUrl;
        this.mProxyAddress = proxyAddr;
        this.mProxyPort = proxyPort;
        this.mDebugText = debugText;
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

    private static boolean isValidApnType(String types, String requestType) {
        if (TextUtils.isEmpty(types)) {
            return true;
        }
        String[] arr$ = types.split(",");
        for (String str : arr$) {
            String type = str.trim();
            if (type.equals(requestType) || type.equals("*")) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return this.mDebugText;
    }
}
