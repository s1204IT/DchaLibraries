package com.android.settings.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.settings.R;

public abstract class ZenRuleNameDialog {
    private static final boolean DEBUG = ZenModeSettings.DEBUG;
    private final AlertDialog mDialog;
    private final EditText mEditText;
    private final boolean mIsNew;
    private final CharSequence mOriginalRuleName;

    public abstract void onOk(String str);

    public ZenRuleNameDialog(Context context, CharSequence ruleName) {
        this.mIsNew = ruleName == null;
        this.mOriginalRuleName = ruleName;
        View v = LayoutInflater.from(context).inflate(R.layout.zen_rule_name, (ViewGroup) null, false);
        this.mEditText = (EditText) v.findViewById(R.id.rule_name);
        if (!this.mIsNew) {
            this.mEditText.setText(ruleName);
        }
        this.mEditText.setSelectAllOnFocus(true);
        this.mDialog = new AlertDialog.Builder(context).setTitle(this.mIsNew ? R.string.zen_mode_add_rule : R.string.zen_mode_rule_name).setView(v).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = ZenRuleNameDialog.this.trimmedText();
                if (TextUtils.isEmpty(newName)) {
                    return;
                }
                if (!ZenRuleNameDialog.this.mIsNew && ZenRuleNameDialog.this.mOriginalRuleName != null && ZenRuleNameDialog.this.mOriginalRuleName.equals(newName)) {
                    return;
                }
                ZenRuleNameDialog.this.onOk(newName);
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    public void show() {
        this.mDialog.show();
    }

    public String trimmedText() {
        if (this.mEditText.getText() == null) {
            return null;
        }
        return this.mEditText.getText().toString().trim();
    }
}
