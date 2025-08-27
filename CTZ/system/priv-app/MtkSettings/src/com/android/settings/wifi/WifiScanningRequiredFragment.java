package com.android.settings.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.HelpUtils;

/* loaded from: classes.dex */
public class WifiScanningRequiredFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    public static WifiScanningRequiredFragment newInstance() {
        return new WifiScanningRequiredFragment();
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder negativeButton = new AlertDialog.Builder(getContext()).setTitle(R.string.wifi_settings_scanning_required_title).setView(R.layout.wifi_settings_scanning_required_view).setPositiveButton(R.string.wifi_settings_scanning_required_turn_on, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
        addButtonIfNeeded(negativeButton);
        return negativeButton.create();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1373;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        if (i == -3) {
            openHelpPage();
        } else if (i == -1) {
            Settings.Global.putInt(contentResolver, "wifi_scan_always_enabled", 1);
            Toast.makeText(context, context.getString(R.string.wifi_settings_scanning_required_enabled), 0).show();
            getTargetFragment().onActivityResult(getTargetRequestCode(), -1, null);
        }
    }

    void addButtonIfNeeded(AlertDialog.Builder builder) {
        if (!TextUtils.isEmpty(getContext().getString(R.string.help_uri_wifi_scanning_required))) {
            builder.setNeutralButton(R.string.learn_more, this);
        }
    }

    private void openHelpPage() {
        Intent helpIntent = getHelpIntent(getContext());
        if (helpIntent != null) {
            try {
                startActivity(helpIntent);
            } catch (ActivityNotFoundException e) {
                Log.e("WifiScanReqFrag", "Activity was not found for intent, " + helpIntent.toString());
            }
        }
    }

    Intent getHelpIntent(Context context) {
        return HelpUtils.getHelpIntent(context, context.getString(R.string.help_uri_wifi_scanning_required), context.getClass().getName());
    }
}
