package com.android.documentsui.ui;

import android.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import com.android.documentsui.services.FileOperation;
import com.android.documentsui.services.FileOperations;

public class OperationProgressDialog {
    private final Activity mActivity;
    private final ProgressDialog mDialog;
    private final String mJobId;

    private OperationProgressDialog(Activity activity, String str, int i, int i2, final FileOperation fileOperation) {
        this.mActivity = activity;
        this.mJobId = str;
        this.mDialog = new ProgressDialog(this.mActivity);
        this.mDialog.setTitle(this.mActivity.getString(i));
        this.mDialog.setMessage(this.mActivity.getString(i2));
        this.mDialog.setProgress(0);
        this.mDialog.setMax(100);
        this.mDialog.setIndeterminate(true);
        this.mDialog.setProgressStyle(1);
        this.mDialog.setCanceledOnTouchOutside(false);
        this.mDialog.setButton(-2, activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i3) {
                OperationProgressDialog.lambda$new$0(this.f$0, dialogInterface, i3);
            }
        });
        this.mDialog.setButton(-3, activity.getString(com.android.documentsui.R.string.continue_in_background), new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i3) {
                this.f$0.mDialog.dismiss();
            }
        });
        fileOperation.addMessageListener(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        OperationProgressDialog.this.mDialog.setIndeterminate(false);
                        if (message.arg1 != -1) {
                            OperationProgressDialog.this.mDialog.setProgress(message.arg1);
                        }
                        if (message.arg2 > 0) {
                            OperationProgressDialog.this.mDialog.setMessage(OperationProgressDialog.this.mActivity.getString(com.android.documentsui.R.string.copy_remaining, new Object[]{DateUtils.formatDuration(message.arg2)}));
                        }
                        return true;
                    case 1:
                        fileOperation.removeMessageListener(this);
                        OperationProgressDialog.this.mDialog.dismiss();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    public static void lambda$new$0(OperationProgressDialog operationProgressDialog, DialogInterface dialogInterface, int i) {
        FileOperations.cancel(operationProgressDialog.mActivity, operationProgressDialog.mJobId);
        operationProgressDialog.mDialog.dismiss();
    }

    public static OperationProgressDialog create(Activity activity, String str, FileOperation fileOperation) {
        int i;
        int i2;
        switch (fileOperation.getOpType()) {
            case 1:
                i = com.android.documentsui.R.string.copy_notification_title;
                i2 = com.android.documentsui.R.string.copy_preparing;
                break;
            case 2:
                i = com.android.documentsui.R.string.extract_notification_title;
                i2 = com.android.documentsui.R.string.extract_preparing;
                break;
            case 3:
                i = com.android.documentsui.R.string.compress_notification_title;
                i2 = com.android.documentsui.R.string.compress_preparing;
                break;
            case 4:
                i = com.android.documentsui.R.string.move_notification_title;
                i2 = com.android.documentsui.R.string.move_preparing;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return new OperationProgressDialog(activity, str, i, i2, fileOperation);
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }

    public void show() {
        this.mDialog.show();
    }
}
