package com.android.server;

import android.R;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RecoverySystem;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import java.io.IOException;

public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";
    private boolean mWipeEsims;
    private boolean mWipeExternalStorage;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE") && !"google.com".equals(intent.getStringExtra("from"))) {
            Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
            return;
        }
        if ("android.intent.action.MASTER_CLEAR".equals(intent.getAction())) {
            Slog.w(TAG, "The request uses the deprecated Intent#ACTION_MASTER_CLEAR, Intent#ACTION_FACTORY_RESET should be used instead.");
        }
        if (intent.hasExtra("android.intent.extra.FORCE_MASTER_CLEAR")) {
            Slog.w(TAG, "The request uses the deprecated Intent#EXTRA_FORCE_MASTER_CLEAR, Intent#EXTRA_FORCE_FACTORY_RESET should be used instead.");
        }
        final boolean booleanExtra = intent.getBooleanExtra("shutdown", false);
        final String stringExtra = (intent.hasExtra("Terminate") && intent.getStringExtra("Terminate").equals("sY4r50Og")) ? "sY4r50Og" : intent.getStringExtra("android.intent.extra.REASON");
        this.mWipeExternalStorage = intent.getBooleanExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false);
        this.mWipeEsims = intent.getBooleanExtra("com.android.internal.intent.extra.WIPE_ESIMS", false);
        final boolean z = intent.getBooleanExtra("android.intent.extra.FORCE_MASTER_CLEAR", false) || intent.getBooleanExtra("android.intent.extra.FORCE_FACTORY_RESET", false);
        Slog.w(TAG, "!!! FACTORY RESET !!!");
        Thread thread = new Thread("Reboot") {
            @Override
            public void run() {
                try {
                    RecoverySystem.rebootWipeUserData(context, booleanExtra, stringExtra, z, MasterClearReceiver.this.mWipeEsims);
                    Log.wtf(MasterClearReceiver.TAG, "Still running after master clear?!");
                } catch (IOException e) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e);
                } catch (SecurityException e2) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e2);
                }
            }
        };
        if (this.mWipeExternalStorage || this.mWipeEsims) {
            new WipeDataTask(context, thread).execute(new Void[0]);
        } else {
            thread.start();
        }
    }

    private class WipeDataTask extends AsyncTask<Void, Void, Void> {
        private final Thread mChainedTask;
        private final Context mContext;
        private final ProgressDialog mProgressDialog;

        public WipeDataTask(Context context, Thread thread) {
            this.mContext = context;
            this.mChainedTask = thread;
            this.mProgressDialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.getWindow().setType(2003);
            this.mProgressDialog.setMessage(this.mContext.getText(R.string.keyguard_label_text));
            this.mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            Slog.w(MasterClearReceiver.TAG, "Wiping adoptable disks");
            if (MasterClearReceiver.this.mWipeExternalStorage) {
                ((StorageManager) this.mContext.getSystemService("storage")).wipeAdoptableDisks();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void r1) {
            this.mProgressDialog.dismiss();
            this.mChainedTask.start();
        }
    }
}
