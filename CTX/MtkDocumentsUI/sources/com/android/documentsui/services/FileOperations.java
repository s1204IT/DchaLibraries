package com.android.documentsui.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import com.android.documentsui.base.SharedMinimal;

public final class FileOperations {
    private static final IdBuilder idBuilder = new IdBuilder();

    @FunctionalInterface
    public interface Callback {
        void onOperationResult(int i, int i2, int i3);
    }

    public static String createJobId() {
        return idBuilder.getNext();
    }

    public static String start(Context context, FileOperation fileOperation, Callback callback, String str) {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperations", "Handling generic 'start' call.");
        }
        if (str == null) {
            str = createJobId();
        }
        Intent intentCreateBaseIntent = createBaseIntent(context, str, fileOperation);
        callback.onOperationResult(0, fileOperation.getOpType(), fileOperation.getSrc().getItemCount());
        context.startService(intentCreateBaseIntent);
        return str;
    }

    public static void cancel(Activity activity, String str) {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperations", "Attempting to canceling operation: " + str);
        }
        Intent intent = new Intent(activity, (Class<?>) FileOperationService.class);
        intent.putExtra("com.android.documentsui.CANCEL", true);
        intent.putExtra("com.android.documentsui.JOB_ID", str);
        activity.startService(intent);
    }

    public static Intent createBaseIntent(Context context, String str, FileOperation fileOperation) {
        Intent intent = new Intent(context, (Class<?>) FileOperationService.class);
        intent.putExtra("com.android.documentsui.JOB_ID", str);
        intent.putExtra("com.android.documentsui.OPERATION", fileOperation);
        return intent;
    }

    private static final class IdBuilder {
        private long mLastJobTime;
        private int mSubId;

        private IdBuilder() {
        }

        public synchronized String getNext() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (jElapsedRealtime == this.mLastJobTime) {
                this.mSubId++;
            } else {
                this.mSubId = 0;
            }
            this.mLastJobTime = jElapsedRealtime;
            return String.valueOf(this.mLastJobTime) + "-" + String.valueOf(this.mSubId);
        }
    }
}
