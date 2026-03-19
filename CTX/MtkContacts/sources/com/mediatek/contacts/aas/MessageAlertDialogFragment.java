package com.mediatek.contacts.aas;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.mediatek.contacts.util.Log;

public class MessageAlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private String mText = null;

    public interface AlertConfirmedListener {
        void onMessageAlertConfirmed(String str);
    }

    public static MessageAlertDialogFragment newInstance(int i, int i2, boolean z, String str) {
        MessageAlertDialogFragment messageAlertDialogFragment = new MessageAlertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("title", i);
        bundle.putInt("message", i2);
        bundle.putBoolean("is_own_cancle", z);
        bundle.putString("updated_aas_name", str);
        messageAlertDialogFragment.setArguments(bundle);
        return messageAlertDialogFragment;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Log.d("MessageAlertDialogFragment", "[onSaveInstanceState] updated_aas_name = " + Log.anonymize(getArguments().getString("updated_aas_name")));
        bundle.putAll(getArguments());
        super.onSaveInstanceState(bundle);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Log.d("MessageAlertDialogFragment", "[onCreateDialog] dialog tag = " + getTag());
        if (bundle == null) {
            bundle = getArguments();
        }
        int i = bundle.getInt("title");
        int i2 = bundle.getInt("message");
        boolean z = bundle.getBoolean("is_own_cancle");
        this.mText = bundle.getString("updated_aas_name");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(R.attr.alertDialogIcon).setTitle(i).setMessage(i2).setPositiveButton(R.string.ok, this);
        if (z) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i3) {
                }
            });
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (!(getActivity() instanceof AasTagActivity)) {
            Log.w("MessageAlertDialogFragment", "[onClick] not AasTagActivity, do nothing");
            return;
        }
        AlertConfirmedListener alertConfirmedListener = ((AasTagActivity) getActivity()).getAlertConfirmedListener(getTag());
        if (alertConfirmedListener != null) {
            alertConfirmedListener.onMessageAlertConfirmed(this.mText);
        }
    }
}
