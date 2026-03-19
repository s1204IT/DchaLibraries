package android.app.servertransaction;

import android.app.ActivityThread;
import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.util.IntArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;

public class TransactionExecutor {
    private static final boolean DEBUG_RESOLVER = false;
    private static final String TAG = "TransactionExecutor";
    private ClientTransactionHandler mTransactionHandler;
    private PendingTransactionActions mPendingActions = new PendingTransactionActions();
    private TransactionExecutorHelper mHelper = new TransactionExecutorHelper();

    public TransactionExecutor(ClientTransactionHandler clientTransactionHandler) {
        this.mTransactionHandler = clientTransactionHandler;
    }

    public void execute(ClientTransaction clientTransaction) {
        log("Start resolving transaction for client: " + this.mTransactionHandler + ", token: " + clientTransaction.getActivityToken());
        executeCallbacks(clientTransaction);
        executeLifecycleState(clientTransaction);
        this.mPendingActions.clear();
        log("End resolving transaction");
    }

    @VisibleForTesting
    public void executeCallbacks(ClientTransaction clientTransaction) {
        List<ClientTransactionItem> callbacks = clientTransaction.getCallbacks();
        if (callbacks == null) {
            return;
        }
        log("Resolving callbacks");
        IBinder activityToken = clientTransaction.getActivityToken();
        ActivityThread.ActivityClientRecord activityClient = this.mTransactionHandler.getActivityClient(activityToken);
        ActivityLifecycleItem lifecycleStateRequest = clientTransaction.getLifecycleStateRequest();
        int targetState = lifecycleStateRequest != null ? lifecycleStateRequest.getTargetState() : -1;
        int iLastCallbackRequestingState = TransactionExecutorHelper.lastCallbackRequestingState(clientTransaction);
        int size = callbacks.size();
        ActivityThread.ActivityClientRecord activityClient2 = activityClient;
        int i = 0;
        while (i < size) {
            ClientTransactionItem clientTransactionItem = callbacks.get(i);
            log("Resolving callback: " + clientTransactionItem);
            int postExecutionState = clientTransactionItem.getPostExecutionState();
            int closestPreExecutionState = this.mHelper.getClosestPreExecutionState(activityClient2, clientTransactionItem.getPostExecutionState());
            if (closestPreExecutionState != -1) {
                cycleToPath(activityClient2, closestPreExecutionState);
            }
            clientTransactionItem.execute(this.mTransactionHandler, activityToken, this.mPendingActions);
            clientTransactionItem.postExecute(this.mTransactionHandler, activityToken, this.mPendingActions);
            if (activityClient2 == null) {
                activityClient2 = this.mTransactionHandler.getActivityClient(activityToken);
            }
            if (postExecutionState != -1 && activityClient2 != null) {
                cycleToPath(activityClient2, postExecutionState, i == iLastCallbackRequestingState && targetState == postExecutionState);
            }
            i++;
        }
    }

    private void executeLifecycleState(ClientTransaction clientTransaction) {
        ActivityLifecycleItem lifecycleStateRequest = clientTransaction.getLifecycleStateRequest();
        if (lifecycleStateRequest == null) {
            return;
        }
        log("Resolving lifecycle state: " + lifecycleStateRequest);
        IBinder activityToken = clientTransaction.getActivityToken();
        ActivityThread.ActivityClientRecord activityClient = this.mTransactionHandler.getActivityClient(activityToken);
        if (activityClient == null) {
            return;
        }
        cycleToPath(activityClient, lifecycleStateRequest.getTargetState(), true);
        lifecycleStateRequest.execute(this.mTransactionHandler, activityToken, this.mPendingActions);
        lifecycleStateRequest.postExecute(this.mTransactionHandler, activityToken, this.mPendingActions);
    }

    @VisibleForTesting
    public void cycleToPath(ActivityThread.ActivityClientRecord activityClientRecord, int i) {
        cycleToPath(activityClientRecord, i, false);
    }

    private void cycleToPath(ActivityThread.ActivityClientRecord activityClientRecord, int i, boolean z) {
        int lifecycleState = activityClientRecord.getLifecycleState();
        log("Cycle from: " + lifecycleState + " to: " + i + " excludeLastState:" + z);
        performLifecycleSequence(activityClientRecord, this.mHelper.getLifecyclePath(lifecycleState, i, z));
    }

    private void performLifecycleSequence(ActivityThread.ActivityClientRecord activityClientRecord, IntArray intArray) {
        int size = intArray.size();
        for (int i = 0; i < size; i++) {
            int i2 = intArray.get(i);
            log("Transitioning to state: " + i2);
            switch (i2) {
                case 1:
                    this.mTransactionHandler.handleLaunchActivity(activityClientRecord, this.mPendingActions, null);
                    break;
                case 2:
                    this.mTransactionHandler.handleStartActivity(activityClientRecord, this.mPendingActions);
                    break;
                case 3:
                    this.mTransactionHandler.handleResumeActivity(activityClientRecord.token, false, activityClientRecord.isForward, "LIFECYCLER_RESUME_ACTIVITY");
                    break;
                case 4:
                    this.mTransactionHandler.handlePauseActivity(activityClientRecord.token, false, false, 0, this.mPendingActions, "LIFECYCLER_PAUSE_ACTIVITY");
                    break;
                case 5:
                    this.mTransactionHandler.handleStopActivity(activityClientRecord.token, false, 0, this.mPendingActions, false, "LIFECYCLER_STOP_ACTIVITY");
                    break;
                case 6:
                    this.mTransactionHandler.handleDestroyActivity(activityClientRecord.token, false, 0, false, "performLifecycleSequence. cycling to:" + intArray.get(size - 1));
                    break;
                case 7:
                    this.mTransactionHandler.performRestartActivity(activityClientRecord.token, false);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected lifecycle state: " + i2);
            }
        }
    }

    private static void log(String str) {
    }
}
