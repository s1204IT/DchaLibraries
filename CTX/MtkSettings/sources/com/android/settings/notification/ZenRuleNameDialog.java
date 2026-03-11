package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ZenRuleNameDialog extends InstrumentedDialogFragment {
    protected static PositiveClickListener mPositiveClickListener;

    public interface PositiveClickListener {
        void onOk(String str, Fragment fragment);
    }

    @Override
    public int getMetricsCategory() {
        return 1269;
    }

    public static void show(Fragment fragment, String str, Uri uri, PositiveClickListener positiveClickListener) {
        Bundle bundle = new Bundle();
        bundle.putString("zen_rule_name", str);
        bundle.putParcelable("extra_zen_condition_id", uri);
        mPositiveClickListener = positiveClickListener;
        ZenRuleNameDialog zenRuleNameDialog = new ZenRuleNameDialog();
        zenRuleNameDialog.setArguments(bundle);
        zenRuleNameDialog.setTargetFragment(fragment, 0);
        zenRuleNameDialog.show(fragment.getFragmentManager(), "ZenRuleNameDialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        Uri uri = (Uri) arguments.getParcelable("extra_zen_condition_id");
        final String string = arguments.getString("zen_rule_name");
        final boolean z = string == null;
        Context context = getContext();
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, (ViewGroup) null, false);
        final EditText editText = (EditText) viewInflate.findViewById(R.id.zen_mode_rule_name);
        if (!z) {
            editText.setText(string);
            editText.setSelection(editText.getText().length());
        }
        editText.setSelectAllOnFocus(true);
        return new AlertDialog.Builder(context).setTitle(getTitleResource(uri, z)).setView(viewInflate).setPositiveButton(z ? R.string.zen_mode_add : R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String strTrimmedText = ZenRuleNameDialog.this.trimmedText(editText);
                if (TextUtils.isEmpty(strTrimmedText)) {
                    return;
                }
                if (!z && string != null && string.equals(strTrimmedText)) {
                    return;
                }
                ZenRuleNameDialog.mPositiveClickListener.onOk(strTrimmedText, ZenRuleNameDialog.this.getTargetFragment());
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    public String trimmedText(EditText editText) {
        if (editText.getText() == null) {
            return null;
        }
        return editText.getText().toString().trim();
    }

    private int getTitleResource(Uri uri, boolean z) {
        boolean zIsValidEventConditionId = ZenModeConfig.isValidEventConditionId(uri);
        boolean zIsValidScheduleConditionId = ZenModeConfig.isValidScheduleConditionId(uri);
        if (z) {
            if (zIsValidEventConditionId) {
                return R.string.zen_mode_add_event_rule;
            }
            if (zIsValidScheduleConditionId) {
                return R.string.zen_mode_add_time_rule;
            }
        }
        return R.string.zen_mode_rule_name;
    }
}
