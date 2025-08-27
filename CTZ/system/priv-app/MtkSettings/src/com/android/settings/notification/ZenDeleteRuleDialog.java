package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class ZenDeleteRuleDialog extends InstrumentedDialogFragment {
    protected static PositiveClickListener mPositiveClickListener;

    public interface PositiveClickListener {
        void onOk(String str);
    }

    public static void show(Fragment fragment, String str, String str2, PositiveClickListener positiveClickListener) {
        Bundle bundle = new Bundle();
        bundle.putString("zen_rule_name", str);
        bundle.putString("zen_rule_id", str2);
        mPositiveClickListener = positiveClickListener;
        ZenDeleteRuleDialog zenDeleteRuleDialog = new ZenDeleteRuleDialog();
        zenDeleteRuleDialog.setArguments(bundle);
        zenDeleteRuleDialog.setTargetFragment(fragment, 0);
        zenDeleteRuleDialog.show(fragment.getFragmentManager(), "ZenDeleteRuleDialog");
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1266;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        final Bundle arguments = getArguments();
        String string = arguments.getString("zen_rule_name");
        final String string2 = arguments.getString("zen_rule_id");
        AlertDialog alertDialogCreate = new AlertDialog.Builder(getContext()).setMessage(getString(R.string.zen_mode_delete_rule_confirmation, new Object[]{string})).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.zen_mode_delete_rule_button, new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.ZenDeleteRuleDialog.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (arguments != null) {
                    ZenDeleteRuleDialog.mPositiveClickListener.onOk(string2);
                }
            }
        }).create();
        View viewFindViewById = alertDialogCreate.findViewById(android.R.id.message);
        if (viewFindViewById != null) {
            viewFindViewById.setTextDirection(5);
        }
        return alertDialogCreate;
    }
}
