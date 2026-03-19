package com.android.bips.discovery;

import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.bips.BuiltInPrintService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class SavedDiscovery extends Discovery {
    private final File mCacheFile;
    private final List<DiscoveredPrinter> mSavedPrinters;
    private static final String TAG = SavedDiscovery.class.getSimpleName();
    private static final List<String> PRINTER_LIST_NAMES = Arrays.asList("printers", "manualPrinters");

    SavedDiscovery(BuiltInPrintService builtInPrintService) {
        super(builtInPrintService);
        this.mCacheFile = new File(builtInPrintService.getCacheDir(), getClass().getSimpleName() + ".json");
        this.mSavedPrinters = load();
    }

    boolean addSavedPrinter(DiscoveredPrinter discoveredPrinter) {
        DiscoveredPrinter discoveredPrinterFind = find(discoveredPrinter.getUri());
        if (discoveredPrinterFind != null) {
            if (discoveredPrinter.equals(discoveredPrinterFind)) {
                return false;
            }
            this.mSavedPrinters.remove(discoveredPrinterFind);
        }
        this.mSavedPrinters.add(0, discoveredPrinter);
        save();
        return true;
    }

    private DiscoveredPrinter find(Uri uri) {
        for (DiscoveredPrinter discoveredPrinter : this.mSavedPrinters) {
            if (discoveredPrinter.getUri().equals(uri)) {
                return discoveredPrinter;
            }
        }
        return null;
    }

    @Override
    public void removeSavedPrinter(Uri uri) {
        for (DiscoveredPrinter discoveredPrinter : this.mSavedPrinters) {
            if (discoveredPrinter.path.equals(uri)) {
                this.mSavedPrinters.remove(discoveredPrinter);
                save();
                return;
            }
        }
    }

    @Override
    public List<DiscoveredPrinter> getSavedPrinters() {
        return Collections.unmodifiableList(this.mSavedPrinters);
    }

    private List<DiscoveredPrinter> load() {
        ArrayList arrayList = new ArrayList();
        if (!this.mCacheFile.exists()) {
            return arrayList;
        }
        try {
            JsonReader jsonReader = new JsonReader(new BufferedReader(new FileReader(this.mCacheFile)));
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    if (PRINTER_LIST_NAMES.contains(jsonReader.nextName())) {
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            arrayList.add(new DiscoveredPrinter(jsonReader));
                        }
                        jsonReader.endArray();
                    }
                }
                jsonReader.endObject();
            } finally {
                $closeResource(null, jsonReader);
            }
        } catch (IOException | IllegalStateException e) {
            Log.w(TAG, "Error while loading from " + this.mCacheFile, e);
        }
        return arrayList;
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

    private void save() {
        if (this.mCacheFile.exists()) {
            this.mCacheFile.delete();
        }
        try {
            JsonWriter jsonWriter = new JsonWriter(new BufferedWriter(new FileWriter(this.mCacheFile)));
            try {
                jsonWriter.beginObject();
                jsonWriter.name(PRINTER_LIST_NAMES.get(0));
                jsonWriter.beginArray();
                Iterator<DiscoveredPrinter> it = this.mSavedPrinters.iterator();
                while (it.hasNext()) {
                    it.next().write(jsonWriter);
                }
                jsonWriter.endArray();
                jsonWriter.endObject();
            } finally {
                $closeResource(null, jsonWriter);
            }
        } catch (IOException | NullPointerException e) {
            Log.w(TAG, "Error while storing to " + this.mCacheFile, e);
        }
    }
}
