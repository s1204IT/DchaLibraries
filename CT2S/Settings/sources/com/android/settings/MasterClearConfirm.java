package com.android.settings;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.os.storage.ExternalStorageFormatter;

public class MasterClearConfirm extends Fragment {
    private View mContentView;
    private boolean mEraseSdCard;
    private View.OnClickListener mFinalClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Utils.isMonkeyRunning()) {
                IntentFilter filter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
                Intent batteryStatus = MasterClearConfirm.this.getActivity().registerReceiver(null, filter);
                if (!Utils.isCharging(batteryStatus)) {
                    MasterClearConfirm.this.showNeedToConnectAcDialog();
                    return;
                }
                final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager) MasterClearConfirm.this.getActivity().getSystemService("persistent_data_block");
                if (pdbManager == null || pdbManager.getOemUnlockEnabled() || Settings.Global.getInt(MasterClearConfirm.this.getActivity().getContentResolver(), "device_provisioned", 0) == 0) {
                    MasterClearConfirm.this.doMasterClear();
                    return;
                }
                final ProgressDialog progressDialog = getProgressDialog();
                progressDialog.show();
                final int oldOrientation = MasterClearConfirm.this.getActivity().getRequestedOrientation();
                MasterClearConfirm.this.getActivity().setRequestedOrientation(14);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        pdbManager.wipe();
                        return null;
                    }

                    @Override
                    public void onPostExecute(Void aVoid) {
                        progressDialog.hide();
                        MasterClearConfirm.this.getActivity().setRequestedOrientation(oldOrientation);
                        MasterClearConfirm.this.doMasterClear();
                    }
                }.execute(new Void[0]);
            }
        }

        private ProgressDialog getProgressDialog() {
            ProgressDialog progressDialog = new ProgressDialog(MasterClearConfirm.this.getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(MasterClearConfirm.this.getActivity().getString(R.string.master_clear_progress_title));
            progressDialog.setMessage(MasterClearConfirm.this.getActivity().getString(R.string.master_clear_progress_text));
            return progressDialog;
        }
    };
    private DialogInterface.OnClickListener mNeedToConnectAcListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            MasterClearConfirm.this.getActivity().finish();
        }
    };

    public void doMasterClear() {
        if (this.mEraseSdCard) {
            Intent intent = new Intent("com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET");
            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            getActivity().startService(intent);
            return;
        }
        Intent intent2 = new Intent("android.intent.action.MASTER_CLEAR");
        intent2.addFlags(268435456);
        intent2.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        getActivity().sendBroadcast(intent2);
    }

    private void establishFinalConfirmationState() {
        this.mContentView.findViewById(R.id.execute_master_clear).setOnClickListener(this.mFinalClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (UserManager.get(getActivity()).hasUserRestriction("no_factory_reset")) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, (ViewGroup) null);
        }
        this.mContentView = inflater.inflate(R.layout.master_clear_confirm, (ViewGroup) null);
        establishFinalConfirmationState();
        return this.mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        this.mEraseSdCard = args != null && args.getBoolean("erase_sd");
    }

    public void showNeedToConnectAcDialog() {
        Resources res = getActivity().getResources();
        new AlertDialog.Builder(getActivity()).setTitle(res.getText(R.string.master_clear_title)).setMessage(res.getText(R.string.master_clear_need_ac_message)).setPositiveButton(res.getText(R.string.master_clear_need_ac_label), this.mNeedToConnectAcListener).show();
    }
}
