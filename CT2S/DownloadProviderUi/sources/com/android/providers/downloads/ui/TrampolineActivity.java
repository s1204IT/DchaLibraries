package com.android.providers.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.providers.downloads.OpenHelper;
import libcore.io.IoUtils;

public class TrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = ContentUris.parseId(getIntent().getData());
        DownloadManager dm = (DownloadManager) getSystemService("download");
        dm.setAccessAllDownloads(true);
        Cursor cursor = dm.query(new DownloadManager.Query().setFilterById(id));
        try {
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                int reason = cursor.getInt(cursor.getColumnIndexOrThrow("reason"));
                IoUtils.closeQuietly(cursor);
                Log.d("DownloadManager", "Found " + id + " with status " + status + ", reason " + reason);
                switch (status) {
                    case 1:
                    case 2:
                        sendRunningDownloadClickedBroadcast(id);
                        finish();
                        return;
                    case 4:
                        if (reason == 3) {
                            PausedDialogFragment.show(getFragmentManager(), id);
                            return;
                        } else {
                            sendRunningDownloadClickedBroadcast(id);
                            finish();
                            return;
                        }
                    case 8:
                        if (!OpenHelper.startViewIntent(this, id, 0)) {
                            Toast.makeText(this, R.string.download_no_application_title, 0).show();
                        }
                        finish();
                        return;
                    case 16:
                        FailedDialogFragment.show(getFragmentManager(), id, reason);
                        return;
                    default:
                        return;
                }
            }
            Toast.makeText(this, R.string.dialog_file_missing_body, 0).show();
            finish();
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private void sendRunningDownloadClickedBroadcast(long id) {
        Intent intent = new Intent("android.intent.action.DOWNLOAD_LIST");
        intent.setPackage("com.android.providers.downloads");
        intent.putExtra("extra_click_download_ids", new long[]{id});
        sendBroadcast(intent);
    }

    public static class PausedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id) {
            PausedDialogFragment dialog = new PausedDialogFragment();
            Bundle args = new Bundle();
            args.putLong("id", id);
            dialog.setArguments(args);
            dialog.show(fm, "paused");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DownloadManager dm = (DownloadManager) context.getSystemService("download");
            dm.setAccessAllDownloads(true);
            final long id = getArguments().getLong("id");
            AlertDialog.Builder builder = new AlertDialog.Builder(context, 3);
            builder.setTitle(R.string.dialog_title_queued_body);
            builder.setMessage(R.string.dialog_queued_body);
            builder.setPositiveButton(R.string.keep_queued_download, (DialogInterface.OnClickListener) null);
            builder.setNegativeButton(R.string.remove_download, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dm.remove(id);
                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finish();
        }
    }

    public static class FailedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id, int reason) {
            FailedDialogFragment dialog = new FailedDialogFragment();
            Bundle args = new Bundle();
            args.putLong("id", id);
            args.putInt("reason", reason);
            dialog.setArguments(args);
            dialog.show(fm, "failed");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DownloadManager dm = (DownloadManager) context.getSystemService("download");
            dm.setAccessAllDownloads(true);
            final long id = getArguments().getLong("id");
            int reason = getArguments().getInt("reason");
            AlertDialog.Builder builder = new AlertDialog.Builder(context, 3);
            builder.setTitle(R.string.dialog_title_not_available);
            switch (reason) {
                case 1006:
                    builder.setMessage(R.string.dialog_insufficient_space_on_external);
                    break;
                case 1007:
                    builder.setMessage(R.string.dialog_media_not_found);
                    break;
                case 1008:
                    builder.setMessage(R.string.dialog_cannot_resume);
                    break;
                case 1009:
                    builder.setMessage(R.string.dialog_file_already_exists);
                    break;
                default:
                    builder.setMessage(R.string.dialog_failed_body);
                    break;
            }
            builder.setNegativeButton(R.string.delete_download, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dm.remove(id);
                }
            });
            builder.setPositiveButton(R.string.retry_download, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dm.restartDownload(new long[]{id});
                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finish();
        }
    }
}
