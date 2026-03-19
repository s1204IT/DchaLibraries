package com.android.documentsui.ui;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.base.Shared;

public final class Snackbars {
    public static final void showDocumentsClipped(Activity activity, int i) {
        makeSnackbar(activity, Shared.getQuantityString(activity, R.plurals.clipboard_files_clipped, i), -1).show();
    }

    public static final void showMove(Activity activity, int i) {
        makeSnackbar(activity, Shared.getQuantityString(activity, R.plurals.move_begin, i), -1).show();
    }

    public static final void showCopy(Activity activity, int i) {
        makeSnackbar(activity, Shared.getQuantityString(activity, R.plurals.copy_begin, i), -1).show();
    }

    public static final void showCompress(Activity activity, int i) {
        makeSnackbar(activity, Shared.getQuantityString(activity, R.plurals.compress_begin, i), -1).show();
    }

    public static final void showExtract(Activity activity, int i) {
        makeSnackbar(activity, Shared.getQuantityString(activity, R.plurals.extract_begin, i), -1).show();
    }

    public static final void showDelete(Activity activity, int i) {
        makeSnackbar(activity, Shared.getQuantityString(activity, R.plurals.deleting, i), -1).show();
    }

    public static final void showOperationRejected(Activity activity) {
        makeSnackbar(activity, R.string.file_operation_rejected, -1).show();
    }

    public static final void showOperationFailed(Activity activity) {
        makeSnackbar(activity, R.string.file_operation_error, -1).show();
    }

    public static final void showRenameFailed(Activity activity) {
        makeSnackbar(activity, R.string.rename_error, -1).show();
    }

    public static final void showInspectorError(Activity activity) {
        Snackbar.make(activity.findViewById(R.id.fragment_container), R.string.inspector_load_error, -2).show();
    }

    public static final void showCustomTextWithImage(Activity activity, String str, int i) {
        Snackbar snackbarMakeSnackbar = makeSnackbar(activity, str, -1);
        TextView textView = (TextView) snackbarMakeSnackbar.getView().findViewById(R.id.snackbar_text);
        textView.setGravity(1);
        textView.setTextAlignment(4);
        textView.setCompoundDrawablesWithIntrinsicBounds(i, 0, 0, 0);
        snackbarMakeSnackbar.show();
    }

    public static final Snackbar makeSnackbar(Activity activity, int i, int i2) {
        return makeSnackbar(activity, activity.getResources().getText(i), i2);
    }

    public static final Snackbar makeSnackbar(Activity activity, CharSequence charSequence, int i) {
        return Snackbar.make(activity.findViewById(R.id.coordinator_layout), charSequence, i);
    }
}
