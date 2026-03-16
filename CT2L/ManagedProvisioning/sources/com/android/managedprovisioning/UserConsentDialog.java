package com.android.managedprovisioning;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class UserConsentDialog extends DialogFragment {

    public interface ConsentCallback {
        void onDialogCancel();

        void onDialogConsent();
    }

    public static UserConsentDialog newInstance(int ownerType) {
        UserConsentDialog dialog = new UserConsentDialog();
        Bundle args = new Bundle();
        args.putInt("owner_type", ownerType);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int ownerType = getArguments().getInt("owner_type");
        if (ownerType != 1 && ownerType != 2) {
            throw new IllegalArgumentException("Illegal value for argument ownerType.");
        }
        final Dialog dialog = new Dialog(getActivity(), R.style.ManagedProvisioningDialogTheme);
        dialog.setContentView(R.layout.learn_more_dialog);
        dialog.setCanceledOnTouchOutside(false);
        TextView text1 = (TextView) dialog.findViewById(R.id.learn_more_text1);
        if (ownerType == 1) {
            text1.setText(R.string.admin_has_ability_to_monitor_profile);
        } else if (ownerType == 2) {
            text1.setText(R.string.admin_has_ability_to_monitor_device);
        }
        TextView linkText = (TextView) dialog.findViewById(R.id.learn_more_link);
        if (ownerType == 1) {
            linkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("https://support.google.com/android/work/answer/6090512"));
                    UserConsentDialog.this.getActivity().startActivity(browserIntent);
                }
            });
        } else if (ownerType == 2) {
            linkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent webIntent = new Intent(UserConsentDialog.this.getActivity(), (Class<?>) WebActivity.class);
                    webIntent.putExtra("extra_url", "https://support.google.com/android/work/answer/6090512");
                    webIntent.putExtra("extra_allowed_url_base", "https://support.google.com/");
                    UserConsentDialog.this.getActivity().startActivity(webIntent);
                }
            });
        }
        Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ((ConsentCallback) UserConsentDialog.this.getActivity()).onDialogConsent();
            }
        });
        Button negativeButton = (Button) dialog.findViewById(R.id.negative_button);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ((ConsentCallback) UserConsentDialog.this.getActivity()).onDialogCancel();
            }
        });
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ((ConsentCallback) getActivity()).onDialogCancel();
    }
}
