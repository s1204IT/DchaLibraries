package com.android.printservice.recommendation;

import androidx.core.util.Preconditions;
import com.android.printservice.recommendation.PrintServicePlugin;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

class RemotePrintServicePlugin implements PrintServicePlugin.PrinterDiscoveryCallback {
    private boolean isRunning;
    private OnChangedListener mListener;
    private final PrintServicePlugin mPlugin;
    public final int name;
    public final CharSequence packageName;
    public final boolean recommendsMultiVendorService;
    private final Object mLock = new Object();
    private List<InetAddress> mPrinters = Collections.emptyList();

    public interface OnChangedListener {
        void onChanged();
    }

    public RemotePrintServicePlugin(PrintServicePlugin printServicePlugin, OnChangedListener onChangedListener, boolean z) throws PluginException {
        this.mListener = onChangedListener;
        this.mPlugin = printServicePlugin;
        this.recommendsMultiVendorService = z;
        try {
            this.name = Preconditions.checkArgumentPositive(this.mPlugin.getName(), "name");
            this.packageName = Preconditions.checkStringNotEmpty(this.mPlugin.getPackageName(), "packageName");
            this.isRunning = false;
        } catch (Throwable th) {
            throw new PluginException(this.mPlugin, "Cannot cache simple properties ", th);
        }
    }

    public void start() throws PluginException {
        try {
            synchronized (this.mLock) {
                this.isRunning = true;
                this.mPlugin.start(this);
            }
        } catch (Throwable th) {
            throw new PluginException(this.mPlugin, "Cannot start", th);
        }
    }

    public void stop() throws PluginException {
        try {
            synchronized (this.mLock) {
                this.mPlugin.stop();
                this.isRunning = false;
            }
        } catch (Throwable th) {
            throw new PluginException(this.mPlugin, "Cannot stop", th);
        }
    }

    public List<InetAddress> getPrinters() {
        return this.mPrinters;
    }

    @Override
    public void onChanged(List<InetAddress> list) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.isRunning);
            if (list == null) {
                this.mPrinters = Collections.emptyList();
            } else {
                this.mPrinters = (List) Preconditions.checkCollectionElementsNotNull(list, "discoveredPrinters");
            }
            this.mListener.onChanged();
        }
    }

    public class PluginException extends Exception {
        private PluginException(PrintServicePlugin printServicePlugin, String str, Throwable th) {
            super(printServicePlugin + ": " + str, th);
        }
    }
}
