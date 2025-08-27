package com.mediatek.settings.wfd;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.R;

/* loaded from: classes.dex */
public final class WfdChangeResolutionFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private int mCurrentResolution = 0;
    private int mWhichIndex = 0;

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        this.mCurrentResolution = Settings.Global.getInt(getActivity().getContentResolver(), "wifi_display_max_resolution", 0);
        Log.d("@M_WfdChangeResolutionFragment", "create dialog, current resolution is " + this.mCurrentResolution);
        int iIndexOf = WfdChangeResolution.DEVICE_RESOLUTION_LIST.indexOf(Integer.valueOf(this.mCurrentResolution));
        this.mWhichIndex = iIndexOf;
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.wfd_change_resolution_menu_title).setSingleChoiceItems(R.array.wfd_resolution_entry, iIndexOf, this).setPositiveButton(android.R.string.ok, this).create();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            int iIntValue = WfdChangeResolution.DEVICE_RESOLUTION_LIST.get(this.mWhichIndex).intValue();
            Log.d("@M_WfdChangeResolutionFragment", "User click ok button, set resolution as " + iIntValue);
            Settings.Global.putInt(getActivity().getContentResolver(), "wifi_display_max_resolution", iIntValue);
            return;
        }
        this.mWhichIndex = i;
        Log.d("@M_WfdChangeResolutionFragment", "User select the item " + this.mWhichIndex);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        if (!WfdChangeResolution.DEVICE_RESOLUTION_LIST.contains(Integer.valueOf(this.mCurrentResolution))) {
            dismiss();
        }
    }
}
