package com.android.documentsui.dirlist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.Metrics;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.ui.Snackbars;

public class RenameDocumentFragment extends DialogFragment {
    private DialogInterface mDialog;
    private DocumentInfo mDocument;
    private EditText mEditText;
    private TextInputLayout mRenameInputWrapper;

    public static void show(FragmentManager fragmentManager, DocumentInfo documentInfo) {
        RenameDocumentFragment renameDocumentFragment = new RenameDocumentFragment();
        renameDocumentFragment.mDocument = documentInfo;
        renameDocumentFragment.show(fragmentManager, "rename_document");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View viewInflate = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_file_name, (ViewGroup) null, false);
        this.mEditText = (EditText) viewInflate.findViewById(android.R.id.text1);
        this.mRenameInputWrapper = (TextInputLayout) viewInflate.findViewById(R.id.rename_input_wrapper);
        builder.setTitle(R.string.menu_rename);
        builder.setView(viewInflate);
        builder.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public final void onShow(DialogInterface dialogInterface) {
                this.f$0.onShowDialog(dialogInterface);
            }
        });
        Shared.ensureKeyboardPresent(activity, alertDialogCreate);
        this.mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6 || (keyEvent != null && keyEvent.getKeyCode() == 66 && keyEvent.hasNoModifiers())) {
                    RenameDocumentFragment.this.renameDocuments(RenameDocumentFragment.this.mEditText.getText().toString());
                    return false;
                }
                return false;
            }
        });
        this.mEditText.requestFocus();
        return alertDialogCreate;
    }

    private void onShowDialog(DialogInterface dialogInterface) {
        this.mDialog = dialogInterface;
        ((AlertDialog) dialogInterface).getButton(-1).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onClickDialog(view);
            }
        });
    }

    private void onClickDialog(View view) {
        renameDocuments(this.mEditText.getText().toString());
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (bundle == null) {
            this.mEditText.setText(this.mDocument.displayName);
        } else {
            this.mDocument = (DocumentInfo) bundle.getParcelable("document");
        }
        selectFileName(this.mEditText);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        clearFileNameSelection(this.mEditText);
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("document", this.mDocument);
    }

    private boolean isValidDocumentName(String str) {
        return !str.isEmpty();
    }

    private void selectFileName(EditText editText) {
        String string = editText.getText().toString();
        int iLastIndexOf = string.lastIndexOf(".");
        if (iLastIndexOf == -1 || this.mDocument.isDirectory()) {
            iLastIndexOf = string.length();
        }
        editText.setSelection(0, iLastIndexOf);
    }

    private void clearFileNameSelection(EditText editText) {
        editText.setSelection(0, 0);
    }

    private void renameDocuments(String str) {
        BaseActivity baseActivity = (BaseActivity) getActivity();
        if (str.equals(this.mDocument.displayName)) {
            this.mDialog.dismiss();
            return;
        }
        if (!isValidDocumentName(str)) {
            Log.w("Documents", "Failed to rename file - invalid name:" + str);
            Snackbars.makeSnackbar(getActivity(), R.string.rename_error, -1).show();
            return;
        }
        if (baseActivity.getInjector().getModel().hasFileWithName(str)) {
            this.mRenameInputWrapper.setError(getContext().getString(R.string.name_conflict));
            selectFileName(this.mEditText);
            Metrics.logRenameFileError(getContext());
            return;
        }
        new RenameDocumentsTask(baseActivity, str).execute(this.mDocument);
    }

    private class RenameDocumentsTask extends AsyncTask<DocumentInfo, Void, DocumentInfo> {
        static final boolean $assertionsDisabled = false;
        private final BaseActivity mActivity;
        private final String mNewDisplayName;

        public RenameDocumentsTask(BaseActivity baseActivity, String str) {
            this.mActivity = baseActivity;
            this.mNewDisplayName = str;
        }

        @Override
        protected void onPreExecute() {
            this.mActivity.setPending(true);
        }

        @Override
        protected DocumentInfo doInBackground(DocumentInfo... documentInfoArr) {
            return this.mActivity.getInjector().actions.renameDocument(this.mNewDisplayName, documentInfoArr[0]);
        }

        @Override
        protected void onPostExecute(DocumentInfo documentInfo) {
            if (documentInfo != null) {
                Metrics.logRenameFileOperation(RenameDocumentFragment.this.getContext());
            } else {
                Snackbars.showRenameFailed(this.mActivity);
                Metrics.logRenameFileError(RenameDocumentFragment.this.getContext());
            }
            if (RenameDocumentFragment.this.mDialog != null) {
                RenameDocumentFragment.this.mDialog.dismiss();
            }
            this.mActivity.setPending(false);
        }
    }
}
