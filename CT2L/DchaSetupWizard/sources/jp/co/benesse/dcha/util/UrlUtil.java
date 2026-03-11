package jp.co.benesse.dcha.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

public class UrlUtil {
    private static final String AKAMAI_URL1 = "https://townak.benesse.ne.jp/test2/A/sp_84/";
    private static final String AKAMAI_URL10 = "https://townak.benesse.ne.jp/rel/A/sp_84/";
    private static final String AKAMAI_URL11 = "https://townak.benesse.ne.jp/rel/B/sp_84/";
    private static final String AKAMAI_URL12 = "https://townak.benesse.ne.jp/rel/B/sp_84/";
    private static final String AKAMAI_URL2 = "https://townak.benesse.ne.jp/test2/B/sp_84/";
    private static final String AKAMAI_URL3 = "https://townak.benesse.ne.jp/test2/A/sp_84/";
    private static final String AKAMAI_URL4 = "https://townak.benesse.ne.jp/test2/B/sp_84/";
    private static final String AKAMAI_URL5 = "https://townak.benesse.ne.jp/test/A/sp_84/";
    private static final String AKAMAI_URL6 = "https://townak.benesse.ne.jp/test/B/sp_84/";
    private static final String AKAMAI_URL7 = "https://townak.benesse.ne.jp/test/A/sp_84/";
    private static final String AKAMAI_URL8 = "https://townak.benesse.ne.jp/test/B/sp_84/";
    private static final String AKAMAI_URL9 = "https://townak.benesse.ne.jp/rel/B/sp_84/";
    private static final String COLUMN_KVS_SELECTION = "key=?";
    private static final String COLUMN_KVS_VALUE = "value";
    private static final String CONNECT_ID_AKAMAI = "townak";
    private static final String OS_TYPE_001 = "001";
    private static final String OS_TYPE_002 = "092";
    private static final String OS_TYPE_003 = "003";
    private static final String OS_TYPE_004 = "094";
    private static final String OS_TYPE_005 = "005";
    private static final String OS_TYPE_006 = "096";
    private static final String OS_TYPE_007 = "007";
    private static final String OS_TYPE_008 = "098";
    private static final String OS_TYPE_009 = "099";
    private static final String OS_TYPE_010 = "000";
    private static final String OS_TYPE_011 = "011";
    private static final String OS_TYPE_012 = "012";
    private static final String TAG = UrlUtil.class.getSimpleName();
    private static final Uri URI_TEST_ENVIRONMENT_INFO = Uri.parse("content://jp.co.benesse.dcha.databox.db.KvsProvider/kvs/test.environment.info");
    private static final String VER_SPLIT = "\\.";
    private static final int VER_SPLIT_NUM = 3;

    public String getUrlAkamai(Context context) {
        String url = getKvsValue(context, URI_TEST_ENVIRONMENT_INFO, CONNECT_ID_AKAMAI, null);
        if (!TextUtils.isEmpty(url)) {
            if (!url.endsWith("/")) {
                url = String.valueOf(url) + "/";
            }
            Logger.d(TAG, "result(kvs):", url);
            return url;
        }
        String result = AKAMAI_URL10;
        String version = getBuildID();
        String uryType = getUrlType(version);
        if (uryType.equals(OS_TYPE_001)) {
            result = "https://townak.benesse.ne.jp/test2/A/sp_84/";
        } else if (uryType.equals(OS_TYPE_002)) {
            result = "https://townak.benesse.ne.jp/test2/B/sp_84/";
        } else if (uryType.equals(OS_TYPE_003)) {
            result = "https://townak.benesse.ne.jp/test2/A/sp_84/";
        } else if (uryType.equals(OS_TYPE_004)) {
            result = "https://townak.benesse.ne.jp/test2/B/sp_84/";
        } else if (uryType.equals(OS_TYPE_005)) {
            result = "https://townak.benesse.ne.jp/test/A/sp_84/";
        } else if (uryType.equals(OS_TYPE_006)) {
            result = "https://townak.benesse.ne.jp/test/B/sp_84/";
        } else if (uryType.equals(OS_TYPE_007)) {
            result = "https://townak.benesse.ne.jp/test/A/sp_84/";
        } else if (uryType.equals(OS_TYPE_008)) {
            result = "https://townak.benesse.ne.jp/test/B/sp_84/";
        } else if (uryType.equals(OS_TYPE_009)) {
            result = "https://townak.benesse.ne.jp/rel/B/sp_84/";
        } else if (uryType.equals(OS_TYPE_010)) {
            result = AKAMAI_URL10;
        } else if (uryType.equals(OS_TYPE_011) || uryType.equals(OS_TYPE_012)) {
            result = "https://townak.benesse.ne.jp/rel/B/sp_84/";
        }
        Logger.d(TAG, "result:", result);
        return result;
    }

    protected String getBuildID() {
        String version = Build.ID;
        return version;
    }

    protected String getUrlType(String version) {
        if (TextUtils.isEmpty(version)) {
            return OS_TYPE_010;
        }
        String[] splitUrlType = version.split(VER_SPLIT);
        if (splitUrlType.length != 3) {
            return OS_TYPE_010;
        }
        String urlType = splitUrlType[2].replace('T', '0');
        return urlType;
    }

    protected String getKvsValue(Context context, Uri uri, String key, String defaultValue) {
        String value = defaultValue;
        if (context != null) {
            String[] projection = {COLUMN_KVS_VALUE};
            String[] selectionArgs = {key};
            Cursor cursor = null;
            try {
                ContentResolver cr = context.getContentResolver();
                cursor = cr.query(uri, projection, COLUMN_KVS_SELECTION, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    value = cursor.getString(cursor.getColumnIndex(COLUMN_KVS_VALUE));
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        }
        return value;
    }
}
