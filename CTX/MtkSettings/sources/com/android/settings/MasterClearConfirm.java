package com.android.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.oemlock.OemLockManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settingslib.RestrictedLockUtils;

public class MasterClearConfirm extends InstrumentedFragment {
    private View mContentView;
    private boolean mEraseEsims;
    private boolean mEraseSdCard;
    private View.OnClickListener mFinalClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            if (!Utils.isCharging(MasterClearConfirm.this.getActivity().registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")))) {
                MasterClearConfirm.this.showNeedToConnectAcDialog();
                return;
            }
            final PersistentDataBlockManager persistentDataBlockManager = (PersistentDataBlockManager) MasterClearConfirm.this.getActivity().getSystemService("persistent_data_block");
            OemLockManager oemLockManager = (OemLockManager) MasterClearConfirm.this.getActivity().getSystemService("oem_lock");
            if (persistentDataBlockManager == null || oemLockManager.isOemUnlockAllowed() || !Utils.isDeviceProvisioned(MasterClearConfirm.this.getActivity())) {
                MasterClearConfirm.this.doMasterClear();
            } else {
                new AsyncTask<Void, Void, Void>() {
                    int mOldOrientation;
                    ProgressDialog mProgressDialog;

                    @Override
                    protected Void doInBackground(Void... voidArr) {
                        persistentDataBlockManager.wipe();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void r2) {
                        this.mProgressDialog.hide();
                        if (MasterClearConfirm.this.getActivity() != null) {
                            MasterClearConfirm.this.getActivity().setRequestedOrientation(this.mOldOrientation);
                            MasterClearConfirm.this.doMasterClear();
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        this.mProgressDialog = getProgressDialog();
                        this.mProgressDialog.show();
                        this.mOldOrientation = MasterClearConfirm.this.getActivity().getRequestedOrientation();
                        MasterClearConfirm.this.getActivity().setRequestedOrientation(14);
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
        public void onClick(DialogInterface dialogInterface, int i) {
            MasterClearConfirm.this.getActivity().finish();
        }
    };

    private void doMasterClear() {
        Intent intent = new Intent("android.intent.action.FACTORY_RESET");
        intent.setPackage("android");
        intent.addFlags(268435456);
        intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", this.mEraseSdCard);
        intent.putExtra("com.android.internal.intent.extra.WIPE_ESIMS", this.mEraseEsims);
        getActivity().sendBroadcast(intent);
    }

    private void establishFinalConfirmationState() {
        this.mContentView.findViewById(R.id.execute_master_clear).setOnClickListener(this.mFinalClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_factory_reset", UserHandle.myUserId());
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_factory_reset", UserHandle.myUserId())) {
            return layoutInflater.inflate(R.layout.master_clear_disallowed_screen, (ViewGroup) null);
        }
        if (enforcedAdminCheckIfRestrictionEnforced != null) {
            new ActionDisabledByAdminDialogHelper(getActivity()).prepareDialogBuilder("no_factory_reset", enforcedAdminCheckIfRestrictionEnforced).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public final void onDismiss(DialogInterface dialogInterface) {
                    this.f$0.getActivity().finish();
                }
            }).show();
            return new View(getActivity());
        }
        this.mContentView = layoutInflater.inflate(R.layout.master_clear_confirm, (ViewGroup) null);
        establishFinalConfirmationState();
        setAccessibilityTitle();
        return this.mContentView;
    }

    private void setAccessibilityTitle() {
        CharSequence title = getActivity().getTitle();
        TextView textView = (TextView) this.mContentView.findViewById(R.id.master_clear_confirm);
        if (textView != null) {
            getActivity().setTitle(Utils.createAccessibleSequence(title, title + "," + textView.getText()));
        }
    }

    private void showNeedToConnectAcDialog() {
        Resources resources = getActivity().getResources();
        new AlertDialog.Builder(getActivity()).setTitle(resources.getText(R.string.master_clear_title)).setMessage(resources.getText(R.string.master_clear_need_ac_message)).setPositiveButton(resources.getText(R.string.master_clear_need_ac_label), this.mNeedToConnectAcListener).show();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle arguments = getArguments();
        boolean z = false;
        this.mEraseSdCard = arguments != null && arguments.getBoolean("erase_sd");
        if (arguments != null && arguments.getBoolean("erase_esim")) {
            z = true;
        }
        this.mEraseEsims = z;
    }

    @Override
    public int getMetricsCategory() {
        return 67;
    }
}
