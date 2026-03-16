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
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;
import java.util.List;

public class PrinterRegistry {
    private final Activity mActivity;
    private final Handler mHandler;
    private OnPrintersChangeListener mOnPrintersChangeListener;
    private boolean mReady;
    private final Runnable mReadyCallback;
    private final List<PrinterInfo> mPrinters = new ArrayList();
    private final LoaderManager.LoaderCallbacks<List<PrinterInfo>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<PrinterInfo>>() {
        @Override
        public void onLoaderReset(Loader<List<PrinterInfo>> loader) {
            if (loader.getId() == 1) {
                PrinterRegistry.this.mPrinters.clear();
                if (PrinterRegistry.this.mOnPrintersChangeListener != null) {
                    PrinterRegistry.this.mHandler.obtainMessage(1, PrinterRegistry.this.mOnPrintersChangeListener).sendToTarget();
                }
            }
        }

        @Override
        public void onLoadFinished(Loader<List<PrinterInfo>> loader, List<PrinterInfo> printers) {
            if (loader.getId() == 1) {
                PrinterRegistry.this.mPrinters.clear();
                PrinterRegistry.this.mPrinters.addAll(printers);
                if (PrinterRegistry.this.mOnPrintersChangeListener != null) {
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = PrinterRegistry.this.mOnPrintersChangeListener;
                    args.arg2 = printers;
                    PrinterRegistry.this.mHandler.obtainMessage(0, args).sendToTarget();
                }
                if (!PrinterRegistry.this.mReady) {
                    PrinterRegistry.this.mReady = true;
                    if (PrinterRegistry.this.mReadyCallback != null) {
                        PrinterRegistry.this.mReadyCallback.run();
                    }
                }
            }
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int id, Bundle args) {
            if (id == 1) {
                return new FusedPrintersProvider(PrinterRegistry.this.mActivity);
            }
            return null;
        }
    };

    public interface OnPrintersChangeListener {
        void onPrintersChanged(List<PrinterInfo> list);

        void onPrintersInvalid();
    }

    public PrinterRegistry(Activity activity, Runnable readyCallback) {
        this.mActivity = activity;
        this.mReadyCallback = readyCallback;
        this.mHandler = new MyHandler(activity.getMainLooper());
        activity.getLoaderManager().initLoader(1, null, this.mLoaderCallbacks);
    }

    public void setOnPrintersChangeListener(OnPrintersChangeListener listener) {
        this.mOnPrintersChangeListener = listener;
    }

    public List<PrinterInfo> getPrinters() {
        return this.mPrinters;
    }

    public void addHistoricalPrinter(PrinterInfo printer) {
        FusedPrintersProvider provider = getPrinterProvider();
        if (provider != null) {
            getPrinterProvider().addHistoricalPrinter(printer);
        }
    }

    public void forgetFavoritePrinter(PrinterId printerId) {
        FusedPrintersProvider provider = getPrinterProvider();
        if (provider != null) {
            provider.forgetFavoritePrinter(printerId);
        }
    }

    public boolean isFavoritePrinter(PrinterId printerId) {
        FusedPrintersProvider provider = getPrinterProvider();
        if (provider != null) {
            return provider.isFavoritePrinter(printerId);
        }
        return false;
    }

    public void setTrackedPrinter(PrinterId printerId) {
        FusedPrintersProvider provider = getPrinterProvider();
        if (provider != null) {
            provider.setTrackedPrinter(printerId);
        }
    }

    public boolean areHistoricalPrintersLoaded() {
        FusedPrintersProvider provider = getPrinterProvider();
        if (provider != null) {
            return getPrinterProvider().areHistoricalPrintersLoaded();
        }
        return false;
    }

    private FusedPrintersProvider getPrinterProvider() {
        Loader<?> loader = this.mActivity.getLoaderManager().getLoader(1);
        return (FusedPrintersProvider) loader;
    }

    private static final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    SomeArgs args = (SomeArgs) message.obj;
                    OnPrintersChangeListener callback = (OnPrintersChangeListener) args.arg1;
                    List<PrinterInfo> printers = (List) args.arg2;
                    args.recycle();
                    callback.onPrintersChanged(printers);
                    break;
                case 1:
                    OnPrintersChangeListener callback2 = (OnPrintersChangeListener) message.obj;
                    callback2.onPrintersInvalid();
                    break;
            }
        }
    }
}
