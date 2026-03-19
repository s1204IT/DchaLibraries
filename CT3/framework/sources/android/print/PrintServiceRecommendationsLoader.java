package android.print;

import android.content.Context;
import android.content.Loader;
import android.os.Handler;
import android.os.Message;
import android.print.PrintManager;
import android.printservice.recommendation.RecommendationInfo;
import com.android.internal.util.Preconditions;
import java.util.List;

public class PrintServiceRecommendationsLoader extends Loader<List<RecommendationInfo>> {
    private final Handler mHandler;
    private PrintManager.PrintServiceRecommendationsChangeListener mListener;
    private final PrintManager mPrintManager;

    public PrintServiceRecommendationsLoader(PrintManager printManager, Context context) {
        super((Context) Preconditions.checkNotNull(context));
        this.mHandler = new MyHandler();
        this.mPrintManager = (PrintManager) Preconditions.checkNotNull(printManager);
    }

    @Override
    protected void onForceLoad() {
        queueNewResult();
    }

    private void queueNewResult() {
        Message m = this.mHandler.obtainMessage(0);
        m.obj = this.mPrintManager.getPrintServiceRecommendations();
        this.mHandler.sendMessage(m);
    }

    @Override
    protected void onStartLoading() {
        this.mListener = new PrintManager.PrintServiceRecommendationsChangeListener() {
            @Override
            public void onPrintServiceRecommendationsChanged() {
                PrintServiceRecommendationsLoader.this.queueNewResult();
            }
        };
        this.mPrintManager.addPrintServiceRecommendationsChangeListener(this.mListener);
        deliverResult(this.mPrintManager.getPrintServiceRecommendations());
    }

    @Override
    protected void onStopLoading() {
        if (this.mListener != null) {
            this.mPrintManager.removePrintServiceRecommendationsChangeListener(this.mListener);
            this.mListener = null;
        }
        if (this.mHandler == null) {
            return;
        }
        this.mHandler.removeMessages(0);
    }

    @Override
    protected void onReset() {
        onStopLoading();
    }

    private class MyHandler extends Handler {
        public MyHandler() {
            super(PrintServiceRecommendationsLoader.this.getContext().getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (!PrintServiceRecommendationsLoader.this.isStarted()) {
                return;
            }
            PrintServiceRecommendationsLoader.this.deliverResult((List) msg.obj);
        }
    }
}
