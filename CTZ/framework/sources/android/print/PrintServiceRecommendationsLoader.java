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
        Message messageObtainMessage = this.mHandler.obtainMessage(0);
        messageObtainMessage.obj = this.mPrintManager.getPrintServiceRecommendations();
        this.mHandler.sendMessage(messageObtainMessage);
    }

    @Override
    protected void onStartLoading() {
        this.mListener = new PrintManager.PrintServiceRecommendationsChangeListener() {
            @Override
            public void onPrintServiceRecommendationsChanged() {
                PrintServiceRecommendationsLoader.this.queueNewResult();
            }
        };
        this.mPrintManager.addPrintServiceRecommendationsChangeListener(this.mListener, null);
        deliverResult(this.mPrintManager.getPrintServiceRecommendations());
    }

    @Override
    protected void onStopLoading() {
        if (this.mListener != null) {
            this.mPrintManager.removePrintServiceRecommendationsChangeListener(this.mListener);
            this.mListener = null;
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
        public void handleMessage(Message message) {
            if (PrintServiceRecommendationsLoader.this.isStarted()) {
                PrintServiceRecommendationsLoader.this.deliverResult((List) message.obj);
            }
        }
    }
}
