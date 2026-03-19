package com.mediatek.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.mediatek.settings.CallSettingUtils;

public class MobileDataDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private MobileDataDialogInterface mDialogInterface = null;

    public static void show(Intent intent, CallSettingUtils.DialogType dialogType, int i, FragmentManager fragmentManager) {
        MobileDataDialogFragment mobileDataDialogFragment = new MobileDataDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("intent", intent);
        bundle.putInt("subId", i);
        bundle.putSerializable("type", dialogType);
        mobileDataDialogFragment.setArguments(bundle);
        mobileDataDialogFragment.setCancelable(false);
        mobileDataDialogFragment.show(fragmentManager, "DataTrafficDialog");
    }

    public static void show(MobileDataDialogInterface mobileDataDialogInterface, int i, FragmentManager fragmentManager, String str, int i2, int i3) {
        MobileDataDialogFragment mobileDataDialogFragment = new MobileDataDialogFragment();
        mobileDataDialogFragment.setDialogInterface(mobileDataDialogInterface);
        Bundle bundle = new Bundle();
        bundle.putInt("subId", i);
        bundle.putSerializable("type", CallSettingUtils.DialogType.DATA_TRAFFIC);
        bundle.putString("number", str);
        bundle.putInt("time", i2);
        bundle.putInt("action", i3);
        mobileDataDialogFragment.setArguments(bundle);
        mobileDataDialogFragment.setCancelable(false);
        mobileDataDialogFragment.show(fragmentManager, "DataTrafficDialog");
    }

    private void setDialogInterface(MobileDataDialogInterface mobileDataDialogInterface) {
        this.mDialogInterface = mobileDataDialogInterface;
    }

    private String getMessage(CallSettingUtils.DialogType dialogType, int i) {
        String subDisplayName = PhoneUtils.getSubDisplayName(i);
        switch (dialogType) {
            case DATA_OPEN:
                return getResources().getString(R.string.volte_ss_not_available_tips_data, subDisplayName);
            case DATA_TRAFFIC:
                return getResources().getString(R.string.volte_ss_not_available_tips_data_traffic, subDisplayName);
            case DATA_ROAMING:
                return getResources().getString(R.string.volte_ss_not_available_tips_data_roaming, subDisplayName);
            default:
                return null;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        int i = getArguments().getInt("subId");
        CallSettingUtils.DialogType dialogType = (CallSettingUtils.DialogType) getArguments().getSerializable("type");
        Log.d("MobileDataDialogFragment", "type = " + dialogType);
        if (dialogType == null) {
            return null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getMessage(dialogType, i)).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(R.string.alert_dialog_ok, this).setNegativeButton(android.R.string.cancel, this);
        builder.setCancelable(false);
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.setCanceledOnTouchOutside(false);
        return alertDialogCreate;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Intent intent = (Intent) getArguments().getParcelable("intent");
            if (intent != null) {
                int intExtra = intent.getIntExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, -1);
                Log.d("MobileDataDialogFragment", "subId = " + intExtra);
                Activity activity = getActivity();
                if (activity == null || activity.isFinishing()) {
                    Log.e("MobileDataDialogFragment", "Activity is null or finishing");
                    return;
                } else if (SubscriptionManager.from(activity).isActiveSubId(intExtra)) {
                    getActivity().startActivity(intent);
                    return;
                } else {
                    Log.e("MobileDataDialogFragment", "Inactive subId");
                    return;
                }
            }
            if (this.mDialogInterface != null) {
                Log.d("MobileDataDialogFragment", "doAction = " + this.mDialogInterface);
                this.mDialogInterface.doAction(getArguments().getString("number"), getArguments().getInt("time"), getArguments().getInt("action"));
                return;
            }
            return;
        }
        if (this.mDialogInterface != null) {
            Log.d("MobileDataDialogFragment", "doCancel = " + this.mDialogInterface);
            this.mDialogInterface.doCancel();
        }
    }
}
