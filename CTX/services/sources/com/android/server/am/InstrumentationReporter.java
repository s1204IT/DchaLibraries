package com.android.server.am;

import android.app.IInstrumentationWatcher;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Process;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;

public class InstrumentationReporter {
    static final boolean DEBUG = false;
    static final int REPORT_TYPE_FINISHED = 1;
    static final int REPORT_TYPE_STATUS = 0;
    static final String TAG = "ActivityManager";
    final Object mLock = new Object();
    ArrayList<Report> mPendingReports;
    Thread mThread;

    final class MyThread extends Thread {
        public MyThread() {
            super("InstrumentationReporter");
        }

        @Override
        public void run() {
            boolean z;
            Process.setThreadPriority(0);
            while (true) {
                z = false;
                while (true) {
                    synchronized (InstrumentationReporter.this.mLock) {
                        ArrayList<Report> arrayList = InstrumentationReporter.this.mPendingReports;
                        InstrumentationReporter.this.mPendingReports = null;
                        if (arrayList != null && !arrayList.isEmpty()) {
                            break;
                        }
                        if (!z) {
                            try {
                                InstrumentationReporter.this.mLock.wait(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                            } catch (InterruptedException e) {
                            }
                        } else {
                            InstrumentationReporter.this.mThread = null;
                            return;
                        }
                    }
                    z = true;
                }
            }
            int i = i + 1;
            z = true;
        }
    }

    final class Report {
        final ComponentName mName;
        final int mResultCode;
        final Bundle mResults;
        final int mType;
        final IInstrumentationWatcher mWatcher;

        Report(int i, IInstrumentationWatcher iInstrumentationWatcher, ComponentName componentName, int i2, Bundle bundle) {
            this.mType = i;
            this.mWatcher = iInstrumentationWatcher;
            this.mName = componentName;
            this.mResultCode = i2;
            this.mResults = bundle;
        }
    }

    public void reportStatus(IInstrumentationWatcher iInstrumentationWatcher, ComponentName componentName, int i, Bundle bundle) {
        report(new Report(0, iInstrumentationWatcher, componentName, i, bundle));
    }

    public void reportFinished(IInstrumentationWatcher iInstrumentationWatcher, ComponentName componentName, int i, Bundle bundle) {
        report(new Report(1, iInstrumentationWatcher, componentName, i, bundle));
    }

    private void report(Report report) {
        synchronized (this.mLock) {
            if (this.mThread == null) {
                this.mThread = new MyThread();
                this.mThread.start();
            }
            if (this.mPendingReports == null) {
                this.mPendingReports = new ArrayList<>();
            }
            this.mPendingReports.add(report);
            this.mLock.notifyAll();
        }
    }
}
