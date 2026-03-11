package jp.co.benesse.dcha.util;

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
        String kvsValue = getKvsValue(context, URI_TEST_ENVIRONMENT_INFO, CONNECT_ID_AKAMAI, null);
        if (!TextUtils.isEmpty(kvsValue)) {
            if (!kvsValue.endsWith("/")) {
                kvsValue = kvsValue + "/";
            }
            Logger.d(TAG, "result(kvs):", kvsValue);
            return kvsValue;
        }
        String urlType = getUrlType(getBuildID());
        String str = "https://townak.benesse.ne.jp/rel/B/sp_84/";
        if (!urlType.equals(OS_TYPE_001)) {
            if (!urlType.equals(OS_TYPE_002)) {
                if (urlType.equals(OS_TYPE_003)) {
                    str = "https://townak.benesse.ne.jp/test2/A/sp_84/";
                } else if (urlType.equals(OS_TYPE_004)) {
                    str = "https://townak.benesse.ne.jp/test2/B/sp_84/";
                } else if (!urlType.equals(OS_TYPE_005)) {
                    if (!urlType.equals(OS_TYPE_006)) {
                        if (urlType.equals(OS_TYPE_007)) {
                            str = "https://townak.benesse.ne.jp/test/A/sp_84/";
                        } else if (urlType.equals(OS_TYPE_008)) {
                            str = "https://townak.benesse.ne.jp/test/B/sp_84/";
                        } else if (!urlType.equals(OS_TYPE_009) && (urlType.equals(OS_TYPE_010) || (!urlType.equals(OS_TYPE_011) && !urlType.equals(OS_TYPE_012)))) {
                            str = AKAMAI_URL10;
                        }
                    }
                }
            }
        }
        Logger.d(TAG, "result:", str);
        return str;
    }

    protected String getBuildID() {
        return Build.ID;
    }

    protected String getUrlType(String str) {
        if (!TextUtils.isEmpty(str)) {
            String[] strArrSplit = str.split(VER_SPLIT);
            if (strArrSplit.length == 3) {
                return strArrSplit[2].replace('T', '0');
            }
        }
        return OS_TYPE_010;
    }

    protected String getKvsValue(Context context, Uri uri, String str, String str2) {
        if (context != null) {
            String[] strArr = {str};
            Cursor cursorQuery = null;
            try {
                cursorQuery = context.getContentResolver().query(uri, new String[]{COLUMN_KVS_VALUE}, COLUMN_KVS_SELECTION, strArr, null);
                if (cursorQuery != null && cursorQuery.moveToFirst()) {
                    str2 = cursorQuery.getString(cursorQuery.getColumnIndex(COLUMN_KVS_VALUE));
                }
            } catch (Exception unused) {
                if (cursorQuery != null) {
                }
            } catch (Throwable th) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
        return str2;
    }
}
