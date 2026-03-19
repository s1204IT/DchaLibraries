package com.android.bips;

import android.net.Uri;
import android.print.PrintJobId;
import android.printservice.PrintJob;
import android.util.Log;
import com.android.bips.discovery.ConnectionListener;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.discovery.Discovery;
import com.android.bips.ipp.Backend;
import com.android.bips.ipp.CapabilitiesCache;
import com.android.bips.ipp.JobStatus;
import com.android.bips.jni.BackendConstants;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.p2p.P2pPrinterConnection;
import com.android.bips.p2p.P2pUtils;
import java.util.function.Consumer;

class LocalPrintJob implements ConnectionListener, Discovery.Listener, CapabilitiesCache.OnLocalPrinterCapabilities {
    private static final String TAG = LocalPrintJob.class.getSimpleName();
    private final Backend mBackend;
    private Consumer<LocalPrintJob> mCompleteConsumer;
    private P2pPrinterConnection mConnection;
    private DelayedAction mDiscoveryTimeout;
    private Uri mPath;
    private final PrintJob mPrintJob;
    private final BuiltInPrintService mPrintService;
    private int mState = 0;

    LocalPrintJob(BuiltInPrintService builtInPrintService, Backend backend, PrintJob printJob) {
        this.mPrintService = builtInPrintService;
        this.mBackend = backend;
        this.mPrintJob = printJob;
        this.mPrintJob.start();
        this.mPrintJob.block(builtInPrintService.getString(R.string.waiting_to_send));
    }

    void start(Consumer<LocalPrintJob> consumer) {
        if (this.mState != 0) {
            Log.w(TAG, "Invalid start state " + this.mState);
            return;
        }
        this.mPrintJob.start();
        this.mPrintService.lockWifi();
        this.mState = 1;
        this.mCompleteConsumer = consumer;
        this.mDiscoveryTimeout = this.mPrintService.delay(120000, new Runnable() {
            @Override
            public final void run() {
                LocalPrintJob.lambda$start$0(this.f$0);
            }
        });
        this.mPrintService.getDiscovery().start(this);
    }

    public static void lambda$start$0(LocalPrintJob localPrintJob) {
        if (localPrintJob.mState == 1) {
            localPrintJob.finish(false, localPrintJob.mPrintService.getString(R.string.printer_offline));
        }
    }

    void cancel() {
        switch (this.mState) {
            case BackendConstants.ALIGN_CENTER_HORIZONTAL:
            case 2:
                this.mState = 4;
                finish(false, null);
                break;
            case 3:
                this.mState = 4;
                this.mBackend.cancel();
                break;
        }
    }

    PrintJobId getPrintJobId() {
        return this.mPrintJob.getId();
    }

    @Override
    public void onPrinterFound(DiscoveredPrinter discoveredPrinter) {
        if (this.mState != 1 || !discoveredPrinter.getId(this.mPrintService).equals(this.mPrintJob.getInfo().getPrinterId())) {
            return;
        }
        if (P2pUtils.isP2p(discoveredPrinter)) {
            this.mConnection = new P2pPrinterConnection(this.mPrintService, discoveredPrinter, this);
            return;
        }
        if (P2pUtils.isOnConnectedInterface(this.mPrintService, discoveredPrinter) && this.mConnection == null) {
            this.mConnection = new P2pPrinterConnection(this.mPrintService, discoveredPrinter, this);
        }
        this.mPrintService.getDiscovery().stop(this);
        this.mState = 2;
        this.mPath = discoveredPrinter.path;
        this.mPrintService.getCapabilitiesCache().request(discoveredPrinter, true, this);
    }

    @Override
    public void onPrinterLost(DiscoveredPrinter discoveredPrinter) {
    }

    @Override
    public void onConnectionComplete(DiscoveredPrinter discoveredPrinter) {
        if (this.mState != 1) {
            return;
        }
        if (discoveredPrinter == null) {
            finish(false, this.mPrintService.getString(R.string.failed_printer_connection));
        } else if (this.mPrintJob.isBlocked()) {
            this.mPrintJob.start();
        }
    }

    @Override
    public void onConnectionDelayed(boolean z) {
        if (this.mState != 1) {
            return;
        }
        if (z) {
            this.mPrintJob.block(this.mPrintService.getString(R.string.connect_hint_text));
        } else {
            this.mPrintJob.start();
        }
    }

    PrintJob getPrintJob() {
        return this.mPrintJob;
    }

    @Override
    public void onCapabilities(LocalPrinterCapabilities localPrinterCapabilities) {
        if (this.mState != 2) {
            return;
        }
        if (localPrinterCapabilities == null) {
            finish(false, this.mPrintService.getString(R.string.printer_offline));
            return;
        }
        if (this.mDiscoveryTimeout != null) {
            this.mDiscoveryTimeout.cancel();
        }
        this.mState = 3;
        this.mPrintJob.start();
        this.mBackend.print(this.mPath, this.mPrintJob, localPrinterCapabilities, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.handleJobStatus((JobStatus) obj);
            }
        });
    }

    private void handleJobStatus(JobStatus jobStatus) {
        byte b;
        String jobState = jobStatus.getJobState();
        int iHashCode = jobState.hashCode();
        byte b2 = 2;
        if (iHashCode != -2126712727) {
            if (iHashCode != -1456177988) {
                b = (iHashCode == 116043919 && jobState.equals(BackendConstants.JOB_STATE_RUNNING)) ? (byte) 2 : (byte) -1;
            } else if (jobState.equals(BackendConstants.JOB_STATE_BLOCKED)) {
                b = 1;
            }
        } else if (jobState.equals(BackendConstants.JOB_STATE_DONE)) {
            b = 0;
        }
        switch (b) {
            case BackendConstants.STATUS_OK:
                String jobResult = jobStatus.getJobResult();
                int iHashCode2 = jobResult.hashCode();
                if (iHashCode2 != -801089539) {
                    if (iHashCode2 != 623173441) {
                        b2 = (iHashCode2 == 671527411 && jobResult.equals(BackendConstants.JOB_DONE_OK)) ? (byte) 0 : (byte) -1;
                    } else if (jobResult.equals(BackendConstants.JOB_DONE_CANCELLED)) {
                        b2 = 1;
                    }
                } else if (!jobResult.equals(BackendConstants.JOB_DONE_CORRUPT)) {
                }
                switch (b2) {
                    case BackendConstants.STATUS_OK:
                        finish(true, null);
                        break;
                    case BackendConstants.ALIGN_CENTER_HORIZONTAL:
                        this.mState = 4;
                        finish(false, null);
                        break;
                    case 2:
                        finish(false, this.mPrintService.getString(R.string.unreadable_input));
                        break;
                    default:
                        finish(false, null);
                        break;
                }
                break;
            case BackendConstants.ALIGN_CENTER_HORIZONTAL:
                if (this.mState != 4) {
                    int blockedReasonId = jobStatus.getBlockedReasonId();
                    if (blockedReasonId == 0) {
                        blockedReasonId = R.string.printer_check;
                    }
                    this.mPrintJob.block(this.mPrintService.getString(blockedReasonId));
                    break;
                }
                break;
            case 2:
                if (this.mState != 4) {
                    this.mPrintJob.start();
                    break;
                }
                break;
        }
    }

    private void finish(boolean z, String str) {
        this.mPrintService.getDiscovery().stop(this);
        if (this.mDiscoveryTimeout != null) {
            this.mDiscoveryTimeout.cancel();
        }
        if (this.mConnection != null) {
            this.mConnection.close();
        }
        this.mPrintService.unlockWifi();
        this.mBackend.closeDocument();
        if (z) {
            this.mPrintJob.start();
            this.mPrintJob.complete();
        } else if (this.mState == 4) {
            this.mPrintJob.cancel();
        } else {
            this.mPrintJob.fail(str);
        }
        this.mState = 5;
        this.mCompleteConsumer.accept(this);
    }
}
