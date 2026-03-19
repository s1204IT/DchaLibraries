package android.os;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SynchronousResultReceiver extends ResultReceiver {
    private final CompletableFuture<Result> mFuture;
    private final String mName;

    public static class Result {
        public Bundle bundle;
        public int resultCode;

        public Result(int i, Bundle bundle) {
            this.resultCode = i;
            this.bundle = bundle;
        }
    }

    public SynchronousResultReceiver() {
        super((Handler) null);
        this.mFuture = new CompletableFuture<>();
        this.mName = null;
    }

    public SynchronousResultReceiver(String str) {
        super((Handler) null);
        this.mFuture = new CompletableFuture<>();
        this.mName = str;
    }

    @Override
    protected final void onReceiveResult(int i, Bundle bundle) {
        super.onReceiveResult(i, bundle);
        this.mFuture.complete(new Result(i, bundle));
    }

    public String getName() {
        return this.mName;
    }

    public Result awaitResult(long j) throws TimeoutException {
        long jCurrentTimeMillis = System.currentTimeMillis() + j;
        while (j >= 0) {
            try {
                return this.mFuture.get(j, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                j -= jCurrentTimeMillis - System.currentTimeMillis();
            } catch (ExecutionException e2) {
                throw new AssertionError("Error receiving response", e2);
            }
        }
        throw new TimeoutException();
    }
}
