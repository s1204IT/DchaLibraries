package com.android.documentsui.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.base.ConfirmationCallback;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.picker.OverwriteConfirmFragment;
import com.android.documentsui.services.FileOperation;
import com.android.documentsui.ui.DialogController;
import java.util.List;

public interface DialogController {
    void confirmDelete(List<DocumentInfo> list, ConfirmationCallback confirmationCallback);

    void confirmOverwrite(FragmentManager fragmentManager, DocumentInfo documentInfo);

    void showDocumentsClipped(int i);

    void showFileOperationStatus(int i, int i2, int i3);

    void showNoApplicationFound();

    void showOperationUnsupported();

    void showProgressDialog(String str, FileOperation fileOperation);

    void showViewInArchivesUnsupported();

    public static final class RuntimeDialogController implements DialogController {
        static final boolean $assertionsDisabled = false;
        private final Activity mActivity;
        private OperationProgressDialog mCurrentProgressDialog = null;
        private final Features mFeatures;
        private final MessageBuilder mMessages;

        public RuntimeDialogController(Features features, Activity activity, MessageBuilder messageBuilder) {
            this.mFeatures = features;
            this.mActivity = activity;
            this.mMessages = messageBuilder;
        }

        @Override
        public void confirmDelete(List<DocumentInfo> list, final ConfirmationCallback confirmationCallback) {
            TextView textView = (TextView) this.mActivity.getLayoutInflater().inflate(R.layout.dialog_delete_confirmation, (ViewGroup) null);
            textView.setText(this.mMessages.generateDeleteMessage(list));
            final AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mActivity).setView(textView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    confirmationCallback.accept(0);
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public final void onShow(DialogInterface dialogInterface) {
                    DialogController.RuntimeDialogController.lambda$confirmDelete$0(alertDialogCreate, dialogInterface);
                }
            });
            alertDialogCreate.show();
        }

        static void lambda$confirmDelete$0(AlertDialog alertDialog, DialogInterface dialogInterface) {
            Button button = alertDialog.getButton(-1);
            button.setFocusable(true);
            button.requestFocus();
        }

        @Override
        public void showFileOperationStatus(int i, int i2, int i3) {
            if (i == 1) {
                showOperationUnsupported();
                return;
            }
            if (i == 2) {
                Snackbars.showOperationFailed(this.mActivity);
                return;
            }
            if (i3 == 0 || shouldShowProgressDialogForOperation(i2)) {
                return;
            }
            switch (i2) {
                case 1:
                    Snackbars.showCopy(this.mActivity, i3);
                    return;
                case 2:
                    Snackbars.showExtract(this.mActivity, i3);
                    return;
                case 3:
                    Snackbars.showCompress(this.mActivity, i3);
                    return;
                case 4:
                    Snackbars.showMove(this.mActivity, i3);
                    return;
                case 5:
                    Snackbars.showDelete(this.mActivity, i3);
                    return;
                default:
                    throw new UnsupportedOperationException("Unsupported Operation: " + i2);
            }
        }

        private boolean shouldShowProgressDialogForOperation(int i) {
            if (i == 5) {
                return false;
            }
            return this.mFeatures.isJobProgressDialogEnabled();
        }

        @Override
        public void showProgressDialog(String str, FileOperation fileOperation) {
            if (!shouldShowProgressDialogForOperation(fileOperation.getOpType())) {
                return;
            }
            if (this.mCurrentProgressDialog != null) {
                this.mCurrentProgressDialog.dismiss();
            }
            this.mCurrentProgressDialog = OperationProgressDialog.create(this.mActivity, str, fileOperation);
            this.mCurrentProgressDialog.show();
        }

        @Override
        public void showNoApplicationFound() {
            Snackbars.makeSnackbar(this.mActivity, R.string.toast_no_application, -1).show();
        }

        @Override
        public void showOperationUnsupported() {
            Snackbars.showOperationRejected(this.mActivity);
        }

        @Override
        public void showViewInArchivesUnsupported() {
            Snackbars.makeSnackbar(this.mActivity, R.string.toast_view_in_archives_unsupported, -1).show();
        }

        @Override
        public void showDocumentsClipped(int i) {
            Snackbars.showDocumentsClipped(this.mActivity, i);
        }

        @Override
        public void confirmOverwrite(FragmentManager fragmentManager, DocumentInfo documentInfo) {
            OverwriteConfirmFragment.show(fragmentManager, documentInfo);
        }
    }

    static DialogController create(Features features, Activity activity, MessageBuilder messageBuilder) {
        return new RuntimeDialogController(features, activity, messageBuilder);
    }
}
