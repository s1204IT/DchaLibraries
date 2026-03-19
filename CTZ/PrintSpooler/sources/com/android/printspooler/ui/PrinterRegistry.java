package com.android.printspooler.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.print.PrinterId;
import android.print.PrinterInfo;
import java.util.ArrayList;
import java.util.List;

public class PrinterRegistry {
    private final Activity mActivity;
    private final Handler mHandler;
    private final int mLoaderId;
    private OnPrintersChangeListener mOnPrintersChangeListener;
    private boolean mReady;
    private final Runnable mReadyCallback;
    private final List<PrinterInfo> mPrinters = new ArrayList();
    private final LoaderManager.LoaderCallbacks<List<PrinterInfo>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<PrinterInfo>>() {
        @Override
        public void onLoaderReset(Loader<List<PrinterInfo>> loader) {
            PrinterRegistry.this.mPrinters.clear();
            PrinterRegistry.this.mHandler.obtainMessage(1).sendToTarget();
        }

        @Override
        public void onLoadFinished(Loader<List<PrinterInfo>> loader, List<PrinterInfo> list) {
            PrinterRegistry.this.mPrinters.clear();
            PrinterRegistry.this.mPrinters.addAll(list);
            PrinterRegistry.this.mHandler.obtainMessage(0, list).sendToTarget();
            if (!PrinterRegistry.this.mReady) {
                PrinterRegistry.this.mReady = true;
                if (PrinterRegistry.this.mReadyCallback != null) {
                    PrinterRegistry.this.mReadyCallback.run();
                }
            }
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int i, Bundle bundle) {
            return new FusedPrintersProvider(PrinterRegistry.this.mActivity, bundle.getInt(null));
        }
    };

    public interface OnPrintersChangeListener {
        void onPrintersChanged(List<PrinterInfo> list);

        void onPrintersInvalid();
    }

    public PrinterRegistry(Activity activity, Runnable runnable, int i, int i2) {
        this.mLoaderId = i;
        this.mActivity = activity;
        this.mReadyCallback = runnable;
        this.mHandler = new MyHandler(activity.getMainLooper());
        Bundle bundle = new Bundle(1);
        bundle.putInt(null, i2);
        activity.getLoaderManager().initLoader(i, bundle, this.mLoaderCallbacks);
    }

    public void setOnPrintersChangeListener(OnPrintersChangeListener onPrintersChangeListener) {
        this.mOnPrintersChangeListener = onPrintersChangeListener;
    }

    public List<PrinterInfo> getPrinters() {
        return this.mPrinters;
    }

    public void addHistoricalPrinter(PrinterInfo printerInfo) {
        if (getPrinterProvider() != null) {
            getPrinterProvider().addHistoricalPrinter(printerInfo);
        }
    }

    public void forgetFavoritePrinter(PrinterId printerId) {
        FusedPrintersProvider printerProvider = getPrinterProvider();
        if (printerProvider != null) {
            printerProvider.forgetFavoritePrinter(printerId);
        }
    }

    public boolean isFavoritePrinter(PrinterId printerId) {
        FusedPrintersProvider printerProvider = getPrinterProvider();
        if (printerProvider != null) {
            return printerProvider.isFavoritePrinter(printerId);
        }
        return false;
    }

    public void setTrackedPrinter(PrinterId printerId) {
        FusedPrintersProvider printerProvider = getPrinterProvider();
        if (printerProvider != null) {
            printerProvider.setTrackedPrinter(printerId);
        }
    }

    public boolean areHistoricalPrintersLoaded() {
        if (getPrinterProvider() != null) {
            return getPrinterProvider().areHistoricalPrintersLoaded();
        }
        return false;
    }

    private FusedPrintersProvider getPrinterProvider() {
        return (FusedPrintersProvider) this.mActivity.getLoaderManager().getLoader(this.mLoaderId);
    }

    private final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    List<PrinterInfo> list = (List) message.obj;
                    if (PrinterRegistry.this.mOnPrintersChangeListener != null) {
                        PrinterRegistry.this.mOnPrintersChangeListener.onPrintersChanged(list);
                    }
                    break;
                case 1:
                    if (PrinterRegistry.this.mOnPrintersChangeListener != null) {
                        PrinterRegistry.this.mOnPrintersChangeListener.onPrintersInvalid();
                    }
                    break;
            }
        }
    }
}
