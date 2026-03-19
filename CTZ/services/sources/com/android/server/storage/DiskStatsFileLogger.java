package com.android.server.storage;

import android.content.pm.PackageStats;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.Log;
import com.android.server.storage.FileCollector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DiskStatsFileLogger {
    public static final String APP_CACHES_KEY = "cacheSizes";
    public static final String APP_CACHE_AGG_KEY = "cacheSize";
    public static final String APP_DATA_KEY = "appDataSizes";
    public static final String APP_DATA_SIZE_AGG_KEY = "appDataSize";
    public static final String APP_SIZES_KEY = "appSizes";
    public static final String APP_SIZE_AGG_KEY = "appSize";
    public static final String AUDIO_KEY = "audioSize";
    public static final String DOWNLOADS_KEY = "downloadsSize";
    public static final String LAST_QUERY_TIMESTAMP_KEY = "queryTime";
    public static final String MISC_KEY = "otherSize";
    public static final String PACKAGE_NAMES_KEY = "packageNames";
    public static final String PHOTOS_KEY = "photosSize";
    public static final String SYSTEM_KEY = "systemSize";
    private static final String TAG = "DiskStatsLogger";
    public static final String VIDEOS_KEY = "videosSize";
    private long mDownloadsSize;
    private List<PackageStats> mPackageStats;
    private FileCollector.MeasurementResult mResult;
    private long mSystemSize;

    public DiskStatsFileLogger(FileCollector.MeasurementResult measurementResult, FileCollector.MeasurementResult measurementResult2, List<PackageStats> list, long j) {
        this.mResult = measurementResult;
        this.mDownloadsSize = measurementResult2.totalAccountedSize();
        this.mSystemSize = j;
        this.mPackageStats = list;
    }

    public void dumpToFile(File file) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(file);
        JSONObject jsonRepresentation = getJsonRepresentation();
        if (jsonRepresentation != null) {
            printWriter.println(jsonRepresentation);
        }
        printWriter.close();
    }

    private JSONObject getJsonRepresentation() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put(LAST_QUERY_TIMESTAMP_KEY, System.currentTimeMillis());
            jSONObject.put(PHOTOS_KEY, this.mResult.imagesSize);
            jSONObject.put(VIDEOS_KEY, this.mResult.videosSize);
            jSONObject.put(AUDIO_KEY, this.mResult.audioSize);
            jSONObject.put(DOWNLOADS_KEY, this.mDownloadsSize);
            jSONObject.put(SYSTEM_KEY, this.mSystemSize);
            jSONObject.put(MISC_KEY, this.mResult.miscSize);
            addAppsToJson(jSONObject);
            return jSONObject;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    private void addAppsToJson(JSONObject jSONObject) throws JSONException {
        boolean z;
        Iterator<Map.Entry<String, PackageStats>> it;
        JSONArray jSONArray = new JSONArray();
        JSONArray jSONArray2 = new JSONArray();
        JSONArray jSONArray3 = new JSONArray();
        JSONArray jSONArray4 = new JSONArray();
        boolean zIsExternalStorageEmulated = Environment.isExternalStorageEmulated();
        Iterator<Map.Entry<String, PackageStats>> it2 = filterOnlyPrimaryUser().entrySet().iterator();
        long j = 0;
        long j2 = 0;
        long j3 = 0;
        while (it2.hasNext()) {
            PackageStats value = it2.next().getValue();
            long j4 = value.codeSize;
            JSONArray jSONArray5 = jSONArray3;
            JSONArray jSONArray6 = jSONArray4;
            long j5 = value.dataSize;
            JSONArray jSONArray7 = jSONArray;
            long j6 = value.cacheSize;
            if (zIsExternalStorageEmulated) {
                z = zIsExternalStorageEmulated;
                it = it2;
                j4 += value.externalCodeSize;
                j5 += value.externalDataSize;
                j6 += value.externalCacheSize;
            } else {
                z = zIsExternalStorageEmulated;
                it = it2;
            }
            j += j4;
            j3 += j5;
            j2 += j6;
            jSONArray7.put(value.packageName);
            jSONArray2.put(j4);
            jSONArray5.put(j5);
            jSONArray6.put(j6);
            jSONArray4 = jSONArray6;
            jSONArray3 = jSONArray5;
            jSONArray = jSONArray7;
            zIsExternalStorageEmulated = z;
            it2 = it;
        }
        jSONObject.put(PACKAGE_NAMES_KEY, jSONArray);
        jSONObject.put(APP_SIZES_KEY, jSONArray2);
        jSONObject.put(APP_CACHES_KEY, jSONArray4);
        jSONObject.put(APP_DATA_KEY, jSONArray3);
        jSONObject.put(APP_SIZE_AGG_KEY, j);
        jSONObject.put(APP_CACHE_AGG_KEY, j2);
        jSONObject.put(APP_DATA_SIZE_AGG_KEY, j3);
    }

    private ArrayMap<String, PackageStats> filterOnlyPrimaryUser() {
        ArrayMap<String, PackageStats> arrayMap = new ArrayMap<>();
        for (PackageStats packageStats : this.mPackageStats) {
            if (packageStats.userHandle == 0) {
                PackageStats packageStats2 = arrayMap.get(packageStats.packageName);
                if (packageStats2 != null) {
                    packageStats2.cacheSize += packageStats.cacheSize;
                    packageStats2.codeSize += packageStats.codeSize;
                    packageStats2.dataSize += packageStats.dataSize;
                    packageStats2.externalCacheSize += packageStats.externalCacheSize;
                    packageStats2.externalCodeSize += packageStats.externalCodeSize;
                    packageStats2.externalDataSize += packageStats.externalDataSize;
                } else {
                    arrayMap.put(packageStats.packageName, new PackageStats(packageStats));
                }
            }
        }
        return arrayMap;
    }
}
