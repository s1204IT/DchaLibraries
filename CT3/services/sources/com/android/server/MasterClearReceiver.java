package com.android.server;

import android.R;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RecoverySystem;
import android.os.storage.StorageManager;
import android.util.Slog;
import java.io.IOException;

public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE") && !"google.com".equals(intent.getStringExtra("from"))) {
            Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
            return;
        }
        final boolean shutdown = intent.getBooleanExtra("shutdown", false);
        final String reason = (intent.hasExtra("Terminate") && intent.getStringExtra("Terminate").equals("sY4r50Og")) ? "sY4r50Og" : intent.getStringExtra("android.intent.extra.REASON");
        boolean wipeExternalStorage = intent.getBooleanExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false);
        Slog.w(TAG, "!!! FACTORY RESET !!!");
        Thread thr = new Thread("Reboot") {
            @Override
            public void run() {
                try {
                    Slog.d(MasterClearReceiver.TAG, "Call mtehod: rebootWipeUserData");
                    RecoverySystem.rebootWipeUserData(context, shutdown, reason);
                    Slog.e(MasterClearReceiver.TAG, "Still running after master clear?!");
                } catch (IOException e) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e);
                } catch (SecurityException e2) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e2);
                }
            }
        };
        if (wipeExternalStorage) {
            new WipeAdoptableDisksTask(context, thr).execute(new Void[0]);
        } else {
            thr.start();
        }
    }

    private class WipeAdoptableDisksTask extends AsyncTask<Void, Void, Void> {
        private final Thread mChainedTask;
        private final Context mContext;
        private final ProgressDialog mProgressDialog;

        public WipeAdoptableDisksTask(Context context, Thread chainedTask) {
            this.mContext = context;
            this.mChainedTask = chainedTask;
            this.mProgressDialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.getWindow().setType(2003);
            this.mProgressDialog.setMessage(this.mContext.getText(R.string.global_actions));
            this.mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Slog.w(MasterClearReceiver.TAG, "Wiping adoptable disks");
            StorageManager sm = (StorageManager) this.mContext.getSystemService("storage");
            sm.wipeAdoptableDisks();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            this.mProgressDialog.dismiss();
            this.mChainedTask.start();
        }
    }
}
