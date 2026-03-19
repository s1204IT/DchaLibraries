package com.android.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PersistableBundle;
import com.android.internal.telephony.Phone;
import com.mediatek.phone.ext.ExtensionManager;

public class RoamingDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private RoamingDialogListener mListener;
    private Phone mPhone;

    public interface RoamingDialogListener {
        void onPositiveButtonClick(DialogFragment dialogFragment);
    }

    public void setPhone(Phone phone) {
        this.mPhone = phone;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment fragmentFindFragmentById = getFragmentManager().findFragmentById(R.id.network_setting_content);
        try {
            this.mListener = (RoamingDialogListener) fragmentFindFragmentById;
        } catch (ClassCastException e) {
            throw new ClassCastException(fragmentFindFragmentById.toString() + "must implement RoamingDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        int i;
        PersistableBundle carrierConfigForSubId;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (this.mPhone != null && (carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId())) != null && carrierConfigForSubId.getBoolean("check_pricing_with_carrier_data_roaming_bool")) {
            i = R.string.roaming_check_price_warning;
        } else {
            i = R.string.roaming_alert_title;
        }
        builder.setMessage(getResources().getString(R.string.roaming_warning)).setTitle(i).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this);
        ExtensionManager.getMobileNetworkSettingsExt().customizeDataRoamingAlertDialog(builder, this.mPhone.getSubId());
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            this.mListener.onPositiveButtonClick(this);
        }
    }
}
