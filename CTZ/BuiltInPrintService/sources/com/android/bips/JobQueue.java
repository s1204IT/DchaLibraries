package com.android.bips;

import android.print.PrintJobId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class JobQueue {
    private LocalPrintJob mCurrent;
    private final List<LocalPrintJob> mJobs = new ArrayList();

    JobQueue() {
    }

    void print(LocalPrintJob localPrintJob) {
        this.mJobs.add(localPrintJob);
        startNextJob();
    }

    void cancel(PrintJobId printJobId) {
        for (LocalPrintJob localPrintJob : this.mJobs) {
            if (localPrintJob.getPrintJobId().equals(printJobId)) {
                this.mJobs.remove(localPrintJob);
                localPrintJob.getPrintJob().cancel();
                return;
            }
        }
        if (this.mCurrent.getPrintJobId().equals(printJobId)) {
            this.mCurrent.cancel();
        }
    }

    private void startNextJob() {
        if (this.mJobs.isEmpty() || this.mCurrent != null) {
            return;
        }
        this.mCurrent = this.mJobs.remove(0);
        this.mCurrent.start(new Consumer() {
            @Override
            public final void accept(Object obj) {
                JobQueue.lambda$startNextJob$0(this.f$0, (LocalPrintJob) obj);
            }
        });
    }

    public static void lambda$startNextJob$0(JobQueue jobQueue, LocalPrintJob localPrintJob) {
        jobQueue.mCurrent = null;
        jobQueue.startNextJob();
    }
}
