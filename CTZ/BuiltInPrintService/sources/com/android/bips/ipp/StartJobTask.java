package com.android.bips.ipp;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrintJobInfo;
import android.printservice.PrintJob;
import android.util.Log;
import com.android.bips.jni.BackendConstants;
import com.android.bips.jni.LocalJobParams;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.jni.MediaSizes;
import com.android.bips.util.FileUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class StartJobTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = StartJobTask.class.getSimpleName();
    private final Backend mBackend;
    private final LocalPrinterCapabilities mCapabilities;
    private final Context mContext;
    private final Uri mDestination;
    private final PrintDocumentInfo mDocInfo;
    private final String mJobId;
    private final PrintJobInfo mJobInfo;
    private final LocalJobParams mJobParams = new LocalJobParams();
    private final MediaSizes mMediaSizes;
    private final ParcelFileDescriptor mSourceFileDescriptor;

    StartJobTask(Context context, Backend backend, Uri uri, PrintJob printJob, LocalPrinterCapabilities localPrinterCapabilities) {
        this.mContext = context;
        this.mBackend = backend;
        this.mDestination = uri;
        this.mCapabilities = localPrinterCapabilities;
        this.mJobId = printJob.getId().toString();
        this.mJobInfo = printJob.getInfo();
        this.mDocInfo = printJob.getDocument().getInfo();
        this.mSourceFileDescriptor = printJob.getDocument().getData();
        this.mMediaSizes = MediaSizes.getInstance(this.mContext);
    }

    private void populateJobParams() {
        PrintAttributes.MediaSize mediaSize = this.mJobInfo.getAttributes().getMediaSize();
        this.mJobParams.borderless = isBorderless() ? 1 : 0;
        this.mJobParams.duplex = getSides();
        this.mJobParams.num_copies = this.mJobInfo.getCopies();
        this.mJobParams.pdf_render_resolution = 300;
        this.mJobParams.fit_to_page = !getFillPage();
        this.mJobParams.fill_page = getFillPage();
        this.mJobParams.job_name = this.mJobInfo.getLabel();
        this.mJobParams.job_originating_user_name = Build.MODEL;
        this.mJobParams.auto_rotate = false;
        this.mJobParams.portrait_mode = mediaSize == null || mediaSize.isPortrait();
        this.mJobParams.landscape_mode = !this.mJobParams.portrait_mode;
        this.mJobParams.media_size = this.mMediaSizes.toMediaCode(mediaSize);
        this.mJobParams.media_type = getMediaType();
        this.mJobParams.color_space = getColorSpace();
        this.mJobParams.document_category = getDocumentCategory();
        this.mJobParams.job_margin_top = Math.max(this.mJobParams.job_margin_top, 0.0f);
        this.mJobParams.job_margin_left = Math.max(this.mJobParams.job_margin_left, 0.0f);
        this.mJobParams.job_margin_right = Math.max(this.mJobParams.job_margin_right, 0.0f);
        this.mJobParams.job_margin_bottom = Math.max(this.mJobParams.job_margin_bottom, 0.0f);
        this.mJobParams.alignment = 17;
    }

    @Override
    protected Integer doInBackground(Void... voidArr) {
        File file = new File(this.mContext.getFilesDir(), "jobs");
        if (!FileUtils.makeDirectory(file)) {
            Log.w(TAG, "makeDirectory failure");
            return -1;
        }
        File file2 = new File(file, this.mJobId + ".pdf");
        try {
            try {
                FileUtils.copy(new ParcelFileDescriptor.AutoCloseInputStream(this.mSourceFileDescriptor), new BufferedOutputStream(new FileOutputStream(file2)));
                String[] strArr = {file2.toString()};
                String str = this.mDestination.getHost() + this.mDestination.getPath();
                if (isCancelled()) {
                    file2.delete();
                    return -2;
                }
                if (this.mBackend.nativeGetDefaultJobParameters(this.mJobParams) != 0) {
                    file2.delete();
                    return -3;
                }
                if (isCancelled()) {
                    file2.delete();
                    return -2;
                }
                populateJobParams();
                this.mBackend.nativeGetFinalJobParameters(this.mJobParams, this.mCapabilities);
                if (isCancelled()) {
                    file2.delete();
                    return -2;
                }
                int iNativeStartJob = this.mBackend.nativeStartJob(Backend.getIp(str), this.mDestination.getPort(), "application/pdf", this.mJobParams, this.mCapabilities, strArr, null, this.mDestination.getScheme());
                if (iNativeStartJob >= 0) {
                    return Integer.valueOf(iNativeStartJob);
                }
                Log.w(TAG, "nativeStartJob failure: " + iNativeStartJob);
                file2.delete();
                return -3;
            } catch (IOException e) {
                Log.w(TAG, "Error while copying to " + file2, e);
                file2.delete();
                return -1;
            }
        } catch (Throwable th) {
            if (file2 != null) {
                file2.delete();
            }
            throw th;
        }
    }

    private boolean isBorderless() {
        return this.mCapabilities.borderless && this.mDocInfo.getContentType() == 1;
    }

    private int getSides() {
        if (this.mDocInfo.getContentType() == 1) {
            return 0;
        }
        int duplexMode = this.mJobInfo.getAttributes().getDuplexMode();
        if (duplexMode != 2) {
            return duplexMode != 4 ? 0 : 2;
        }
        return 1;
    }

    private boolean getFillPage() {
        return this.mDocInfo.getContentType() == 1;
    }

    private int getMediaType() {
        if (this.mDocInfo.getContentType() != 1) {
            return 0;
        }
        int i = 0;
        for (int i2 : this.mCapabilities.supportedMediaTypes) {
            if (i2 > i) {
                i = i2;
            }
        }
        return i;
    }

    private int getColorSpace() {
        if (this.mJobInfo.getAttributes().getColorMode() == 2) {
            return 1;
        }
        return 0;
    }

    private String getDocumentCategory() {
        if (this.mDocInfo.getContentType() == 1) {
            return BackendConstants.PRINT_DOCUMENT_CATEGORY__PHOTO;
        }
        return BackendConstants.PRINT_DOCUMENT_CATEGORY__DOCUMENT;
    }
}
