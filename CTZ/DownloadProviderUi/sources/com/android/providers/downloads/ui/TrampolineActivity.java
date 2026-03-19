package com.android.providers.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;
import com.android.providers.downloads.OpenHelper;
import com.android.providers.downloads.RawDocumentsHelper;
import libcore.io.IoUtils;

public class TrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Uri data = getIntent().getData();
        if (RawDocumentsHelper.isRawDocId(DocumentsContract.getDocumentId(data))) {
            if (!RawDocumentsHelper.startViewIntent(this, data)) {
                Toast.makeText(this, R.string.download_no_application_title, 0).show();
            }
            finish();
            return;
        }
        long id = ContentUris.parseId(data);
        DownloadManager downloadManager = (DownloadManager) getSystemService("download");
        downloadManager.setAccessAllDownloads(true);
        Cursor cursorQuery = downloadManager.query(new DownloadManager.Query().setFilterById(id));
        try {
            if (cursorQuery.moveToFirst()) {
                int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("status"));
                int i2 = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("reason"));
                long j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("total_size"));
                IoUtils.closeQuietly(cursorQuery);
                Log.d("DownloadManager", "Found " + id + " with status " + i + ", reason " + i2);
                if (i == 4) {
                    if (i2 == 3) {
                        PausedDialogFragment.show(getFragmentManager(), id, j);
                        return;
                    } else {
                        sendRunningDownloadClickedBroadcast(id);
                        finish();
                        return;
                    }
                }
                if (i == 8) {
                    if (!OpenHelper.startViewIntent(this, id, 0)) {
                        Toast.makeText(this, R.string.download_no_application_title, 0).show();
                    }
                    finish();
                    return;
                } else {
                    if (i != 16) {
                        switch (i) {
                            case 1:
                            case 2:
                                sendRunningDownloadClickedBroadcast(id);
                                finish();
                                return;
                            default:
                                return;
                        }
                    }
                    FailedDialogFragment.show(getFragmentManager(), id, i2);
                    return;
                }
            }
            Toast.makeText(this, R.string.dialog_file_missing_body, 0).show();
            finish();
        } finally {
            IoUtils.closeQuietly(cursorQuery);
        }
    }

    private void sendRunningDownloadClickedBroadcast(long j) {
        Intent intent = new Intent("android.intent.action.DOWNLOAD_LIST");
        intent.setPackage("com.android.providers.downloads");
        intent.putExtra("extra_click_download_ids", new long[]{j});
        sendBroadcast(intent);
    }

    public static class PausedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fragmentManager, long j, long j2) {
            PausedDialogFragment pausedDialogFragment = new PausedDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putLong("id", j);
            bundle.putLong("size", j2);
            pausedDialogFragment.setArguments(bundle);
            pausedDialogFragment.show(fragmentManager, "paused");
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            final DownloadManager downloadManager = (DownloadManager) activity.getSystemService("download");
            downloadManager.setAccessAllDownloads(true);
            final long j = getArguments().getLong("id");
            long j2 = getArguments().getLong("size");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, android.R.style.Theme.DeviceDefault.Light.Dialog.Alert);
            builder.setTitle(R.string.dialog_title_queued_body);
            builder.setMessage(R.string.dialog_queued_body);
            Long maxBytesOverMobile = DownloadManager.getMaxBytesOverMobile(activity);
            if (maxBytesOverMobile != null && j2 > maxBytesOverMobile.longValue()) {
                builder.setPositiveButton(R.string.keep_queued_download, (DialogInterface.OnClickListener) null);
            } else {
                builder.setPositiveButton(R.string.start_now_download, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        downloadManager.forceDownload(new long[]{j});
                    }
                });
            }
            builder.setNegativeButton(R.string.remove_download, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    downloadManager.remove(j);
                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            super.onDismiss(dialogInterface);
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public static class FailedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fragmentManager, long j, int i) {
            FailedDialogFragment failedDialogFragment = new FailedDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putLong("id", j);
            bundle.putInt("reason", i);
            failedDialogFragment.setArguments(bundle);
            failedDialogFragment.show(fragmentManager, "failed");
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            final DownloadManager downloadManager = (DownloadManager) activity.getSystemService("download");
            downloadManager.setAccessAllDownloads(true);
            final long j = getArguments().getLong("id");
            int i = getArguments().getInt("reason");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, android.R.style.Theme.DeviceDefault.Light.Dialog.Alert);
            builder.setTitle(R.string.dialog_title_not_available);
            switch (i) {
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
                public void onClick(DialogInterface dialogInterface, int i2) {
                    downloadManager.remove(j);
                }
            });
            builder.setPositiveButton(R.string.retry_download, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    downloadManager.restartDownload(new long[]{j});
                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            super.onDismiss(dialogInterface);
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
