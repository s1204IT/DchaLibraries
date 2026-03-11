package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SecuritySettings;

public class OwnerInfoSettings extends DialogFragment implements DialogInterface.OnClickListener {
    private LockPatternUtils mLockPatternUtils;
    private EditText mOwnerInfo;
    private int mUserId;
    private View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUserId = UserHandle.myUserId();
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.mView = LayoutInflater.from(getActivity()).inflate(R.layout.ownerinfo, (ViewGroup) null);
        initView();
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.owner_info_settings_title).setView(this.mView).setPositiveButton(R.string.save, this).setNegativeButton(R.string.cancel, this).show();
    }

    private void initView() {
        String info = this.mLockPatternUtils.getOwnerInfo(this.mUserId);
        this.mOwnerInfo = (EditText) this.mView.findViewById(R.id.owner_info_edit_text);
        if (TextUtils.isEmpty(info)) {
            return;
        }
        this.mOwnerInfo.setText(info);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != -1) {
            return;
        }
        String info = this.mOwnerInfo.getText().toString();
        this.mLockPatternUtils.setOwnerInfoEnabled(!TextUtils.isEmpty(info), this.mUserId);
        this.mLockPatternUtils.setOwnerInfo(info, this.mUserId);
        if (!(getTargetFragment() instanceof SecuritySettings.SecuritySubSettings)) {
            return;
        }
        ((SecuritySettings.SecuritySubSettings) getTargetFragment()).updateOwnerInfo();
    }

    public static void show(Fragment parent) {
        if (parent.isAdded()) {
            OwnerInfoSettings dialog = new OwnerInfoSettings();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "ownerInfo");
        }
    }
}
