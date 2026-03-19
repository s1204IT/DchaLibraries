package com.android.bips.ipp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.printservice.PrintJob;
import android.text.TextUtils;
import com.android.bips.R;
import com.android.bips.ipp.JobStatus;
import com.android.bips.jni.BackendConstants;
import com.android.bips.jni.JobCallback;
import com.android.bips.jni.JobCallbackParams;
import com.android.bips.jni.LocalJobParams;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.jni.PdfRender;
import com.android.bips.util.FileUtils;
import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;

public class Backend implements JobCallback {
    private static final String TAG = Backend.class.getSimpleName();
    private final Context mContext;
    private JobStatus mCurrentJobStatus;
    private Consumer<JobStatus> mJobStatusListener;
    private final Handler mMainHandler;
    private AsyncTask<Void, Void, Integer> mStartTask;

    native int nativeCancelJob(int i);

    native int nativeEndJob(int i);

    native int nativeExit();

    native int nativeGetCapabilities(String str, int i, String str2, String str3, long j, LocalPrinterCapabilities localPrinterCapabilities);

    native int nativeGetDefaultJobParameters(LocalJobParams localJobParams);

    native int nativeGetFinalJobParameters(LocalJobParams localJobParams, LocalPrinterCapabilities localPrinterCapabilities);

    native int nativeInit(JobCallback jobCallback, String str, int i);

    native void nativeSetSourceInfo(String str, String str2, String str3);

    native int nativeStartJob(String str, int i, String str2, LocalJobParams localJobParams, LocalPrinterCapabilities localPrinterCapabilities, String[] strArr, String str3, String str4);

    public Backend(Context context) {
        this.mContext = context;
        this.mMainHandler = new Handler(context.getMainLooper());
        PdfRender.getInstance(this.mContext);
        System.loadLibrary(BackendConstants.WPRINT_LIBRARY_PREFIX);
        nativeInit(this, context.getApplicationInfo().dataDir, Build.VERSION.SDK_INT);
        nativeSetSourceInfo(context.getString(R.string.app_name).toLowerCase(Locale.US), getApplicationVersion(context).toLowerCase(Locale.US), BackendConstants.WPRINT_APPLICATION_ID.toLowerCase(Locale.US));
    }

    private String getApplicationVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "(unknown)";
        }
    }

    public GetCapabilitiesTask getCapabilities(Uri uri, long j, boolean z, final Consumer<LocalPrinterCapabilities> consumer) {
        GetCapabilitiesTask getCapabilitiesTask = new GetCapabilitiesTask(this, uri, j, z) {
            @Override
            protected void onPostExecute(LocalPrinterCapabilities localPrinterCapabilities) {
                consumer.accept(localPrinterCapabilities);
            }
        };
        getCapabilitiesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        return getCapabilitiesTask;
    }

    public void print(Uri uri, PrintJob printJob, LocalPrinterCapabilities localPrinterCapabilities, Consumer<JobStatus> consumer) {
        this.mJobStatusListener = consumer;
        this.mCurrentJobStatus = new JobStatus();
        this.mStartTask = new StartJobTask(this.mContext, this, uri, printJob, localPrinterCapabilities) {
            @Override
            public void onCancelled(Integer num) {
                onPostExecute((Integer) (-2));
            }

            @Override
            protected void onPostExecute(Integer num) {
                Backend.this.mStartTask = null;
                if (num.intValue() <= 0) {
                    if (Backend.this.mJobStatusListener != null) {
                        String str = BackendConstants.JOB_DONE_ERROR;
                        if (num.intValue() == -2) {
                            str = BackendConstants.JOB_DONE_CANCELLED;
                        } else if (num.intValue() == -1) {
                            str = BackendConstants.JOB_DONE_CORRUPT;
                        }
                        Backend.this.mCurrentJobStatus = new JobStatus.Builder().setJobState(BackendConstants.JOB_STATE_DONE).setJobResult(str).build();
                        Backend.this.mJobStatusListener.accept(Backend.this.mCurrentJobStatus);
                        Backend.this.mJobStatusListener = null;
                        return;
                    }
                    return;
                }
                Backend.this.mCurrentJobStatus = new JobStatus.Builder(Backend.this.mCurrentJobStatus).setId(num.intValue()).build();
            }
        };
        this.mStartTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void cancel() {
        if (this.mStartTask != null) {
            this.mStartTask.cancel(true);
        } else if (this.mCurrentJobStatus != null && this.mCurrentJobStatus.getId() != -1) {
            new CancelJobTask(this, this.mCurrentJobStatus.getId()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public void closeDocument() {
        PdfRender.getInstance(this.mContext).closeDocument();
    }

    public void close() {
        nativeExit();
        PdfRender.getInstance(this.mContext).close();
    }

    @Override
    public void jobCallback(final int i, final JobCallbackParams jobCallbackParams) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public final void run() {
                Backend.lambda$jobCallback$0(this.f$0, i, jobCallbackParams);
            }
        });
    }

    public static void lambda$jobCallback$0(Backend backend, int i, JobCallbackParams jobCallbackParams) {
        JobStatus.Builder builder = new JobStatus.Builder(backend.mCurrentJobStatus);
        builder.setId(jobCallbackParams.jobId);
        if (!TextUtils.isEmpty(jobCallbackParams.printerState)) {
            backend.updateBlockedReasons(builder, jobCallbackParams);
        } else if (!TextUtils.isEmpty(jobCallbackParams.jobState)) {
            builder.setJobState(jobCallbackParams.jobState);
            if (!TextUtils.isEmpty(jobCallbackParams.jobDoneResult)) {
                builder.setJobResult(jobCallbackParams.jobDoneResult);
            }
            backend.updateBlockedReasons(builder, jobCallbackParams);
        }
        backend.mCurrentJobStatus = builder.build();
        if (backend.mJobStatusListener != null) {
            backend.mJobStatusListener.accept(backend.mCurrentJobStatus);
        }
        if (backend.mCurrentJobStatus.isJobDone()) {
            backend.nativeEndJob(i);
            backend.mCurrentJobStatus = new JobStatus();
            backend.mJobStatusListener = null;
            FileUtils.deleteAll(new File(backend.mContext.getFilesDir(), "jobs"));
        }
    }

    private void updateBlockedReasons(JobStatus.Builder builder, JobCallbackParams jobCallbackParams) {
        if (jobCallbackParams.blockedReasons != null && jobCallbackParams.blockedReasons.length > 0) {
            builder.clearBlockedReasons();
            for (String str : jobCallbackParams.blockedReasons) {
                if (!TextUtils.isEmpty(str)) {
                    builder.addBlockedReason(str);
                }
            }
        }
    }

    static String getIp(String str) {
        int iIndexOf = str.indexOf(47);
        return iIndexOf == -1 ? str : str.substring(0, iIndexOf);
    }
}
