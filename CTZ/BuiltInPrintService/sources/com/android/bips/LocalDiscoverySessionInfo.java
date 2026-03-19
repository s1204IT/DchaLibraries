package com.android.bips;

import android.print.PrinterId;
import android.printservice.PrintService;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.bips.jni.BackendConstants;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class LocalDiscoverySessionInfo {
    private final File mCacheFile;
    private final List<PrinterId> mKnownGood = new ArrayList();
    private List<PrinterId> mPriority = new ArrayList();
    private final PrintService mService;
    private static final String TAG = LocalDiscoverySessionInfo.class.getSimpleName();
    private static final String CACHE_FILE = TAG + ".json";

    LocalDiscoverySessionInfo(PrintService printService) throws Exception {
        this.mService = printService;
        this.mCacheFile = new File(printService.getCacheDir(), CACHE_FILE);
        load();
    }

    private void load() throws Exception {
        if (!this.mCacheFile.exists()) {
            return;
        }
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(this.mCacheFile));
            Throwable th = null;
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String strNextName = jsonReader.nextName();
                    byte b = -1;
                    int iHashCode = strNextName.hashCode();
                    if (iHashCode != -1165461084) {
                        if (iHashCode == 1550851744 && strNextName.equals("knownGood")) {
                            b = 0;
                        }
                    } else if (strNextName.equals("priority")) {
                        b = 1;
                    }
                    switch (b) {
                        case BackendConstants.STATUS_OK:
                            this.mKnownGood.addAll(loadPrinterIds(jsonReader));
                            break;
                        case BackendConstants.ALIGN_CENTER_HORIZONTAL:
                            this.mPriority.addAll(loadPrinterIds(jsonReader));
                            break;
                        default:
                            jsonReader.skipValue();
                            break;
                    }
                }
                jsonReader.endObject();
            } finally {
                $closeResource(th, jsonReader);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to read info from " + CACHE_FILE, e);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private List<PrinterId> loadPrinterIds(JsonReader jsonReader) throws IOException {
        ArrayList arrayList = new ArrayList();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            arrayList.add(this.mService.generatePrinterId(jsonReader.nextString()));
        }
        jsonReader.endArray();
        return arrayList;
    }

    void save() {
        try {
            JsonWriter jsonWriter = new JsonWriter(new FileWriter(this.mCacheFile));
            try {
                jsonWriter.beginObject();
                jsonWriter.name("knownGood");
                savePrinterIds(jsonWriter, this.mKnownGood, 50);
                jsonWriter.name("priority");
                savePrinterIds(jsonWriter, this.mPriority, this.mPriority.size());
                jsonWriter.endObject();
            } finally {
                $closeResource(null, jsonWriter);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to write known good list", e);
        }
    }

    private void savePrinterIds(JsonWriter jsonWriter, List<PrinterId> list, int i) throws IOException {
        jsonWriter.beginArray();
        int iMin = Math.min(i, list.size());
        for (int i2 = 0; i2 < iMin; i2++) {
            jsonWriter.value(list.get(i2).getLocalId());
        }
        jsonWriter.endArray();
    }

    boolean isKnownGood(PrinterId printerId) {
        return this.mKnownGood.contains(printerId);
    }

    void setKnownGood(PrinterId printerId) {
        this.mKnownGood.remove(printerId);
        this.mKnownGood.add(0, printerId);
    }
}
