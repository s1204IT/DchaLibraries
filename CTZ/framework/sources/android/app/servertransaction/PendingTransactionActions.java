package android.app.servertransaction;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.util.LogWriter;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;

public class PendingTransactionActions {
    private boolean mCallOnPostCreate;
    private Bundle mOldState;
    private boolean mReportRelaunchToWM;
    private boolean mRestoreInstanceState;
    private StopInfo mStopInfo;

    public PendingTransactionActions() {
        clear();
    }

    public void clear() {
        this.mRestoreInstanceState = false;
        this.mCallOnPostCreate = false;
        this.mOldState = null;
        this.mStopInfo = null;
    }

    public boolean shouldRestoreInstanceState() {
        return this.mRestoreInstanceState;
    }

    public void setRestoreInstanceState(boolean z) {
        this.mRestoreInstanceState = z;
    }

    public boolean shouldCallOnPostCreate() {
        return this.mCallOnPostCreate;
    }

    public void setCallOnPostCreate(boolean z) {
        this.mCallOnPostCreate = z;
    }

    public Bundle getOldState() {
        return this.mOldState;
    }

    public void setOldState(Bundle bundle) {
        this.mOldState = bundle;
    }

    public StopInfo getStopInfo() {
        return this.mStopInfo;
    }

    public void setStopInfo(StopInfo stopInfo) {
        this.mStopInfo = stopInfo;
    }

    public boolean shouldReportRelaunchToWindowManager() {
        return this.mReportRelaunchToWM;
    }

    public void setReportRelaunchToWindowManager(boolean z) {
        this.mReportRelaunchToWM = z;
    }

    public static class StopInfo implements Runnable {
        private static final String TAG = "ActivityStopInfo";
        private ActivityThread.ActivityClientRecord mActivity;
        private CharSequence mDescription;
        private PersistableBundle mPersistentState;
        private Bundle mState;

        public void setActivity(ActivityThread.ActivityClientRecord activityClientRecord) {
            this.mActivity = activityClientRecord;
        }

        public void setState(Bundle bundle) {
            this.mState = bundle;
        }

        public void setPersistentState(PersistableBundle persistableBundle) {
            this.mPersistentState = persistableBundle;
        }

        public void setDescription(CharSequence charSequence) {
            this.mDescription = charSequence;
        }

        @Override
        public void run() {
            try {
                if (ActivityThread.DEBUG_MEMORY_TRIM) {
                    Slog.v(TAG, "Reporting activity stopped: " + this.mActivity);
                }
                ActivityManager.getService().activityStopped(this.mActivity.token, this.mState, this.mPersistentState, this.mDescription);
            } catch (RemoteException e) {
                IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(new LogWriter(5, TAG), "  ");
                indentingPrintWriter.println("Bundle stats:");
                Bundle.dumpStats(indentingPrintWriter, this.mState);
                indentingPrintWriter.println("PersistableBundle stats:");
                Bundle.dumpStats(indentingPrintWriter, this.mPersistentState);
                if ((e instanceof TransactionTooLargeException) && this.mActivity.packageInfo.getTargetSdkVersion() < 24) {
                    Log.e(TAG, "App sent too much data in instance state, so it was ignored", e);
                    return;
                }
                throw e.rethrowFromSystemServer();
            }
        }
    }
}
