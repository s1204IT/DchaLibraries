package com.android.server.location;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.backup.BackupManagerConstants;
import java.util.ArrayList;
import java.util.List;

class GnssSatelliteBlacklistHelper {
    private static final String BLACKLIST_DELIMITER = ",";
    private final GnssSatelliteBlacklistCallback mCallback;
    private final Context mContext;
    private static final String TAG = "GnssBlacklistHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    interface GnssSatelliteBlacklistCallback {
        void onUpdateSatelliteBlacklist(int[] iArr, int[] iArr2);
    }

    GnssSatelliteBlacklistHelper(Context context, Looper looper, GnssSatelliteBlacklistCallback gnssSatelliteBlacklistCallback) {
        this.mContext = context;
        this.mCallback = gnssSatelliteBlacklistCallback;
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("gnss_satellite_blacklist"), true, new ContentObserver(new Handler(looper)) {
            @Override
            public void onChange(boolean z) {
                GnssSatelliteBlacklistHelper.this.updateSatelliteBlacklist();
            }
        }, -1);
    }

    void updateSatelliteBlacklist() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "gnss_satellite_blacklist");
        if (string == null) {
            string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        if (DEBUG) {
            Log.d(TAG, String.format("Update GNSS satellite blacklist: %s", string));
        }
        try {
            List<Integer> satelliteBlacklist = parseSatelliteBlacklist(string);
            if (satelliteBlacklist.size() % 2 != 0) {
                Log.e(TAG, "blacklist string has odd number of values.Aborting updateSatelliteBlacklist");
                return;
            }
            int size = satelliteBlacklist.size() / 2;
            int[] iArr = new int[size];
            int[] iArr2 = new int[size];
            for (int i = 0; i < size; i++) {
                int i2 = i * 2;
                iArr[i] = satelliteBlacklist.get(i2).intValue();
                iArr2[i] = satelliteBlacklist.get(i2 + 1).intValue();
            }
            this.mCallback.onUpdateSatelliteBlacklist(iArr, iArr2);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Exception thrown when parsing blacklist string.", e);
        }
    }

    @VisibleForTesting
    static List<Integer> parseSatelliteBlacklist(String str) throws NumberFormatException {
        String[] strArrSplit = str.split(BLACKLIST_DELIMITER);
        ArrayList arrayList = new ArrayList(strArrSplit.length);
        for (String str2 : strArrSplit) {
            String strTrim = str2.trim();
            if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(strTrim)) {
                int i = Integer.parseInt(strTrim);
                if (i < 0) {
                    throw new NumberFormatException("Negative value is invalid.");
                }
                arrayList.add(Integer.valueOf(i));
            }
        }
        return arrayList;
    }
}
