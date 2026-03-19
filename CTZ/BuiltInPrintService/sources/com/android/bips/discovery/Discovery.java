package com.android.bips.discovery;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.android.bips.BuiltInPrintService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Discovery {
    private static final String TAG = Discovery.class.getSimpleName();
    private final BuiltInPrintService mPrintService;
    private final List<Listener> mListeners = new CopyOnWriteArrayList();
    private final Map<Uri, DiscoveredPrinter> mPrinters = new HashMap();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mStarted = false;

    public interface Listener {
        void onPrinterFound(DiscoveredPrinter discoveredPrinter);

        void onPrinterLost(DiscoveredPrinter discoveredPrinter);
    }

    abstract void onStart();

    abstract void onStop();

    Discovery(BuiltInPrintService builtInPrintService) {
        this.mPrintService = builtInPrintService;
    }

    public void start(Listener listener) {
        this.mListeners.add(listener);
        if (!this.mPrinters.isEmpty()) {
            if (!this.mListeners.contains(listener)) {
                return;
            }
            Iterator it = new ArrayList(this.mPrinters.values()).iterator();
            while (it.hasNext()) {
                listener.onPrinterFound((DiscoveredPrinter) it.next());
            }
        }
        start();
    }

    public void stop(Listener listener) {
        this.mListeners.remove(listener);
        if (this.mListeners.isEmpty()) {
            stop();
        }
    }

    boolean isStarted() {
        return this.mStarted;
    }

    BuiltInPrintService getPrintService() {
        return this.mPrintService;
    }

    Handler getHandler() {
        return this.mHandler;
    }

    private void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            onStart();
        }
    }

    private void stop() {
        if (this.mStarted) {
            this.mStarted = false;
            onStop();
            this.mPrinters.clear();
            this.mHandler.removeCallbacksAndMessages(null);
        }
    }

    void printerFound(DiscoveredPrinter discoveredPrinter) {
        if (Objects.equals(this.mPrinters.get(discoveredPrinter.getUri()), discoveredPrinter)) {
            return;
        }
        this.mPrinters.put(discoveredPrinter.getUri(), discoveredPrinter);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPrinterFound(discoveredPrinter);
        }
    }

    void printerLost(Uri uri) {
        DiscoveredPrinter discoveredPrinterRemove = this.mPrinters.remove(uri);
        if (discoveredPrinterRemove == null) {
            return;
        }
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPrinterLost(discoveredPrinterRemove);
        }
    }

    void allPrintersLost() {
        Iterator it = new ArrayList(this.mPrinters.keySet()).iterator();
        while (it.hasNext()) {
            printerLost((Uri) it.next());
        }
    }

    public Collection<DiscoveredPrinter> getPrinters() {
        return this.mPrinters.values();
    }

    public DiscoveredPrinter getPrinter(Uri uri) {
        return this.mPrinters.get(uri);
    }

    Collection<Discovery> getChildren() {
        return Collections.singleton(this);
    }

    public Collection<DiscoveredPrinter> getSavedPrinters() {
        ArrayList arrayList = new ArrayList();
        for (Discovery discovery : getChildren()) {
            if (discovery != this) {
                arrayList.addAll(discovery.getSavedPrinters());
            }
        }
        return arrayList;
    }

    public void removeSavedPrinter(Uri uri) {
        for (Discovery discovery : getChildren()) {
            if (discovery != this) {
                discovery.removeSavedPrinter(uri);
            }
        }
    }
}
