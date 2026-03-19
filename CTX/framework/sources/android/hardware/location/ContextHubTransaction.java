package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Handler;
import android.os.HandlerExecutor;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SystemApi
public class ContextHubTransaction<T> {
    public static final int RESULT_FAILED_AT_HUB = 5;
    public static final int RESULT_FAILED_BAD_PARAMS = 2;
    public static final int RESULT_FAILED_BUSY = 4;
    public static final int RESULT_FAILED_HAL_UNAVAILABLE = 8;
    public static final int RESULT_FAILED_SERVICE_INTERNAL_FAILURE = 7;
    public static final int RESULT_FAILED_TIMEOUT = 6;
    public static final int RESULT_FAILED_UNINITIALIZED = 3;
    public static final int RESULT_FAILED_UNKNOWN = 1;
    public static final int RESULT_SUCCESS = 0;
    private static final String TAG = "ContextHubTransaction";
    public static final int TYPE_DISABLE_NANOAPP = 3;
    public static final int TYPE_ENABLE_NANOAPP = 2;
    public static final int TYPE_LOAD_NANOAPP = 0;
    public static final int TYPE_QUERY_NANOAPPS = 4;
    public static final int TYPE_UNLOAD_NANOAPP = 1;
    private Response<T> mResponse;
    private int mTransactionType;
    private Executor mExecutor = null;
    private OnCompleteListener<T> mListener = null;
    private final CountDownLatch mDoneSignal = new CountDownLatch(1);
    private boolean mIsResponseSet = false;

    @FunctionalInterface
    public interface OnCompleteListener<L> {
        void onComplete(ContextHubTransaction<L> contextHubTransaction, Response<L> response);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Result {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static class Response<R> {
        private R mContents;
        private int mResult;

        Response(int i, R r) {
            this.mResult = i;
            this.mContents = r;
        }

        public int getResult() {
            return this.mResult;
        }

        public R getContents() {
            return this.mContents;
        }
    }

    ContextHubTransaction(int i) {
        this.mTransactionType = i;
    }

    public static String typeToString(int i, boolean z) {
        switch (i) {
            case 0:
                return z ? "Load" : "load";
            case 1:
                return z ? "Unload" : "unload";
            case 2:
                return z ? "Enable" : "enable";
            case 3:
                return z ? "Disable" : "disable";
            case 4:
                return z ? "Query" : "query";
            default:
                return z ? "Unknown" : "unknown";
        }
    }

    public int getType() {
        return this.mTransactionType;
    }

    public Response<T> waitForResponse(long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        if (!this.mDoneSignal.await(j, timeUnit)) {
            throw new TimeoutException("Timed out while waiting for transaction");
        }
        return this.mResponse;
    }

    public void setOnCompleteListener(OnCompleteListener<T> onCompleteListener, Executor executor) {
        synchronized (this) {
            Preconditions.checkNotNull(onCompleteListener, "OnCompleteListener cannot be null");
            Preconditions.checkNotNull(executor, "Executor cannot be null");
            if (this.mListener != null) {
                throw new IllegalStateException("Cannot set ContextHubTransaction listener multiple times");
            }
            this.mListener = onCompleteListener;
            this.mExecutor = executor;
            if (this.mDoneSignal.getCount() == 0) {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        ContextHubTransaction contextHubTransaction = this.f$0;
                        contextHubTransaction.mListener.onComplete(contextHubTransaction, contextHubTransaction.mResponse);
                    }
                });
            }
        }
    }

    public void setOnCompleteListener(OnCompleteListener<T> onCompleteListener) {
        setOnCompleteListener(onCompleteListener, new HandlerExecutor(Handler.getMain()));
    }

    void setResponse(Response<T> response) {
        synchronized (this) {
            Preconditions.checkNotNull(response, "Response cannot be null");
            if (this.mIsResponseSet) {
                throw new IllegalStateException("Cannot set response of ContextHubTransaction multiple times");
            }
            this.mResponse = response;
            this.mIsResponseSet = true;
            this.mDoneSignal.countDown();
            if (this.mListener != null) {
                this.mExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        ContextHubTransaction contextHubTransaction = this.f$0;
                        contextHubTransaction.mListener.onComplete(contextHubTransaction, contextHubTransaction.mResponse);
                    }
                });
            }
        }
    }
}
