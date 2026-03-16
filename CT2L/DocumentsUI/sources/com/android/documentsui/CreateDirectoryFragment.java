package com.android.documentsui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.android.documentsui.model.DocumentInfo;

public class CreateDirectoryFragment extends DialogFragment {
    public static void show(FragmentManager fm) {
        CreateDirectoryFragment dialog = new CreateDirectoryFragment();
        dialog.show(fm, "create_directory");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        context.getContentResolver();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
        View view = dialogInflater.inflate(R.layout.dialog_create_dir, (ViewGroup) null, false);
        final EditText text1 = (EditText) view.findViewById(android.R.id.text1);
        builder.setTitle(R.string.menu_create_dir);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String displayName = text1.getText().toString();
                DocumentsActivity activity = (DocumentsActivity) CreateDirectoryFragment.this.getActivity();
                DocumentInfo cwd = activity.getCurrentDirectory();
                CreateDirectoryFragment.this.new CreateDirectoryTask(activity, cwd, displayName).executeOnExecutor(ProviderExecutor.forAuthority(cwd.authority), new Void[0]);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        return builder.create();
    }

    private class CreateDirectoryTask extends AsyncTask<Void, Void, DocumentInfo> {
        private final DocumentsActivity mActivity;
        private final DocumentInfo mCwd;
        private final String mDisplayName;

        public CreateDirectoryTask(DocumentsActivity activity, DocumentInfo cwd, String displayName) {
            this.mActivity = activity;
            this.mCwd = cwd;
            this.mDisplayName = displayName;
        }

        @Override
        protected void onPreExecute() {
            this.mActivity.setPending(true);
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            DocumentInfo documentInfoFromUri;
            ContentResolver resolver = this.mActivity.getContentResolver();
            ContentProviderClient client = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, this.mCwd.derivedUri.getAuthority());
                Uri childUri = DocumentsContract.createDocument(client, this.mCwd.derivedUri, "vnd.android.document/directory", this.mDisplayName);
                documentInfoFromUri = DocumentInfo.fromUri(resolver, childUri);
            } catch (Exception e) {
                Log.w("Documents", "Failed to create directory", e);
                documentInfoFromUri = null;
            } finally {
                ContentProviderClient.releaseQuietly(client);
            }
            return documentInfoFromUri;
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (result != null) {
                this.mActivity.onDocumentPicked(result);
            } else {
                Toast.makeText(this.mActivity, R.string.create_error, 0).show();
            }
            this.mActivity.setPending(false);
        }
    }
}
