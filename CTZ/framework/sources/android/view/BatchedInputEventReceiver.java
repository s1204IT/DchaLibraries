package android.view;

import android.os.Looper;

public class BatchedInputEventReceiver extends InputEventReceiver {
    private final BatchedInputRunnable mBatchedInputRunnable;
    private boolean mBatchedInputScheduled;
    Choreographer mChoreographer;

    public BatchedInputEventReceiver(InputChannel inputChannel, Looper looper, Choreographer choreographer) {
        super(inputChannel, looper);
        this.mBatchedInputRunnable = new BatchedInputRunnable();
        this.mChoreographer = choreographer;
    }

    @Override
    public void onBatchedInputEventPending() {
        scheduleBatchedInput();
    }

    @Override
    public void dispose() {
        unscheduleBatchedInput();
        super.dispose();
    }

    void doConsumeBatchedInput(long j) {
        if (this.mBatchedInputScheduled) {
            this.mBatchedInputScheduled = false;
            if (consumeBatchedInputEvents(j) && j != -1) {
                scheduleBatchedInput();
            }
        }
    }

    private void scheduleBatchedInput() {
        if (!this.mBatchedInputScheduled) {
            this.mBatchedInputScheduled = true;
            this.mChoreographer.postCallback(0, this.mBatchedInputRunnable, null);
        }
    }

    private void unscheduleBatchedInput() {
        if (this.mBatchedInputScheduled) {
            this.mBatchedInputScheduled = false;
            this.mChoreographer.removeCallbacks(0, this.mBatchedInputRunnable, null);
        }
    }

    private final class BatchedInputRunnable implements Runnable {
        private BatchedInputRunnable() {
        }

        @Override
        public void run() {
            BatchedInputEventReceiver.this.doConsumeBatchedInput(BatchedInputEventReceiver.this.mChoreographer.getFrameTimeNanos());
        }
    }
}
