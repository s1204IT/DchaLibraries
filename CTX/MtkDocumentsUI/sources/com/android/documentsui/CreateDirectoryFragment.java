package com.android.documentsui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.ui.Snackbars;

public class CreateDirectoryFragment extends DialogFragment {
    public static void show(FragmentManager fragmentManager) {
        CreateDirectoryFragment createDirectoryFragment = new CreateDirectoryFragment();
        Log.d("Documents", "show fragment the new way, for AOSP bug");
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        fragmentTransactionBeginTransaction.add(createDirectoryFragment, "create_directory");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Activity activity = getActivity();
        activity.getContentResolver();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View viewInflate = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_file_name, (ViewGroup) null, false);
        final EditText editText = (EditText) viewInflate.findViewById(android.R.id.text1);
        builder.setTitle(R.string.menu_create_dir);
        builder.setView(viewInflate);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CreateDirectoryFragment.this.createDirectory(editText.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        final AlertDialog alertDialogCreate = builder.create();
        Shared.ensureKeyboardPresent(activity, alertDialogCreate);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6 || (keyEvent != null && keyEvent.getKeyCode() == 66 && keyEvent.hasNoModifiers())) {
                    CreateDirectoryFragment.this.createDirectory(editText.getText().toString());
                    alertDialogCreate.dismiss();
                    return true;
                }
                return false;
            }
        });
        return alertDialogCreate;
    }

    private void createDirectory(String str) {
        BaseActivity baseActivity = (BaseActivity) getActivity();
        DocumentInfo currentDirectory = baseActivity.getCurrentDirectory();
        new CreateDirectoryTask(baseActivity, currentDirectory, str).executeOnExecutor(ProviderExecutor.forAuthority(currentDirectory.authority), new Void[0]);
    }

    private class CreateDirectoryTask extends AsyncTask<Void, Void, DocumentInfo> {
        private final BaseActivity mActivity;
        private final DocumentInfo mCwd;
        private final String mDisplayName;

        public CreateDirectoryTask(BaseActivity baseActivity, DocumentInfo documentInfo, String str) {
            this.mActivity = baseActivity;
            this.mCwd = documentInfo;
            this.mDisplayName = str;
        }

        @Override
        protected void onPreExecute() {
            this.mActivity.setPending(true);
        }

        @Override
        protected DocumentInfo doInBackground(Void... voidArr) throws Throwable {
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
            ContentResolver contentResolver = this.mActivity.getContentResolver();
            try {
                contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(contentResolver, this.mCwd.derivedUri.getAuthority());
                try {
                    try {
                        DocumentInfo documentInfoFromUri = DocumentInfo.fromUri(contentResolver, DocumentsContract.createDocument(contentProviderClientAcquireUnstableProviderOrThrow, this.mCwd.derivedUri, "vnd.android.document/directory", this.mDisplayName));
                        if (!documentInfoFromUri.isDirectory()) {
                            documentInfoFromUri = null;
                        }
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                        return documentInfoFromUri;
                    } catch (Exception e) {
                        e = e;
                        Log.w("Documents", "Failed to create directory", e);
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                    ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
                contentProviderClientAcquireUnstableProviderOrThrow = null;
            } catch (Throwable th2) {
                th = th2;
                contentProviderClientAcquireUnstableProviderOrThrow = null;
                ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                throw th;
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo documentInfo) {
            if (documentInfo != null) {
                this.mActivity.onDirectoryCreated(documentInfo);
                Metrics.logCreateDirOperation(CreateDirectoryFragment.this.getContext());
            } else {
                Snackbars.makeSnackbar(this.mActivity, R.string.create_error, -1).show();
                Metrics.logCreateDirError(CreateDirectoryFragment.this.getContext());
            }
            this.mActivity.setPending(false);
        }
    }
}
