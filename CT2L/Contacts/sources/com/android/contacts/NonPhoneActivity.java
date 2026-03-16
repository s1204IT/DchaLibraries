package com.android.contacts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

public class NonPhoneActivity extends ContactsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String phoneNumber = getPhoneNumber();
        if (TextUtils.isEmpty(phoneNumber)) {
            finish();
            return;
        }
        NonPhoneDialogFragment fragment = new NonPhoneDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("PHONE_NUMBER", phoneNumber);
        fragment.setArguments(bundle);
        getFragmentManager().beginTransaction().add(fragment, "Fragment").commitAllowingStateLoss();
    }

    private String getPhoneNumber() {
        Uri data;
        if (getIntent() == null || (data = getIntent().getData()) == null) {
            return null;
        }
        String scheme = data.getScheme();
        if ("tel".equals(scheme)) {
            return getIntent().getData().getSchemeSpecificPart();
        }
        return null;
    }

    public static final class NonPhoneDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.NonPhoneDialogTheme).create();
            alertDialog.setTitle(R.string.non_phone_caption);
            alertDialog.setMessage(getArgumentPhoneNumber());
            alertDialog.setButton(-1, getActivity().getString(R.string.non_phone_add_to_contacts), this);
            alertDialog.setButton(-2, getActivity().getString(R.string.non_phone_close), this);
            return alertDialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                Intent intent = new Intent("android.intent.action.INSERT_OR_EDIT");
                intent.setType("vnd.android.cursor.item/contact");
                intent.putExtra("phone", getArgumentPhoneNumber());
                startActivity(intent);
            }
            dismiss();
        }

        private String getArgumentPhoneNumber() {
            return getArguments().getString("PHONE_NUMBER");
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
