package android.print;

import android.content.Context;
import android.content.Loader;
import android.os.Handler;
import android.os.Message;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;
import com.android.internal.util.Preconditions;
import java.util.List;

public class PrintServicesLoader extends Loader<List<PrintServiceInfo>> {
    private Handler mHandler;
    private PrintManager.PrintServicesChangeListener mListener;
    private final PrintManager mPrintManager;
    private final int mSelectionFlags;

    public PrintServicesLoader(PrintManager printManager, Context context, int selectionFlags) {
        super((Context) Preconditions.checkNotNull(context));
        this.mPrintManager = (PrintManager) Preconditions.checkNotNull(printManager);
        this.mSelectionFlags = Preconditions.checkFlagsArgument(selectionFlags, 3);
    }

    @Override
    protected void onForceLoad() {
        queueNewResult();
    }

    private void queueNewResult() {
        if (this.mHandler == null) {
            return;
        }
        Message m = this.mHandler.obtainMessage(0);
        m.obj = this.mPrintManager.getPrintServices(this.mSelectionFlags);
        this.mHandler.sendMessage(m);
    }

    @Override
    protected void onStartLoading() {
        this.mHandler = new MyHandler();
        this.mListener = new PrintManager.PrintServicesChangeListener() {
            @Override
            public void onPrintServicesChanged() {
                PrintServicesLoader.this.queueNewResult();
            }
        };
        this.mPrintManager.addPrintServicesChangeListener(this.mListener);
        deliverResult(this.mPrintManager.getPrintServices(this.mSelectionFlags));
    }

    @Override
    protected void onStopLoading() {
        if (this.mListener != null) {
            this.mPrintManager.removePrintServicesChangeListener(this.mListener);
            this.mListener = null;
        }
        if (this.mHandler == null) {
            return;
        }
        this.mHandler.removeMessages(0);
        this.mHandler = null;
    }

    @Override
    protected void onReset() {
        onStopLoading();
    }

    private class MyHandler extends Handler {
        public MyHandler() {
            super(PrintServicesLoader.this.getContext().getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!PrintServicesLoader.this.isStarted()) {
                return;
            }
            PrintServicesLoader.this.deliverResult((List) msg.obj);
        }
    }
}
