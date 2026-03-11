package com.android.settings.datausage;

import android.R;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.NetworkPolicyManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.Checkable;
import com.android.settings.CustomDialogPreference;
import com.android.settings.Utils;
import com.android.settings.dashboard.conditional.BackgroundDataCondition;
import com.android.settings.dashboard.conditional.ConditionManager;

public class RestrictBackgroundDataPreference extends CustomDialogPreference {
    private boolean mChecked;
    private NetworkPolicyManager mPolicyManager;

    @Override
    public void onAttached() {
        super.onAttached();
        this.mPolicyManager = NetworkPolicyManager.from(getContext());
        setChecked(this.mPolicyManager.getRestrictBackground());
    }

    public void setRestrictBackground(boolean restrictBackground) {
        this.mPolicyManager.setRestrictBackground(restrictBackground);
        setChecked(restrictBackground);
        ((BackgroundDataCondition) ConditionManager.get(getContext()).getCondition(BackgroundDataCondition.class)).refreshState();
    }

    private void setChecked(boolean checked) {
        if (this.mChecked == checked) {
            return;
        }
        this.mChecked = checked;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View viewFindViewById = holder.findViewById(R.id.switch_widget);
        viewFindViewById.setClickable(false);
        ((Checkable) viewFindViewById).setChecked(this.mChecked);
    }

    @Override
    protected void performClick(View view) {
        if (this.mChecked) {
            setRestrictBackground(false);
        } else {
            super.performClick(view);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        builder.setTitle(com.android.settings.R.string.data_usage_restrict_background_title);
        if (Utils.hasMultipleUsers(getContext())) {
            builder.setMessage(com.android.settings.R.string.data_usage_restrict_background_multiuser);
        } else {
            builder.setMessage(com.android.settings.R.string.data_usage_restrict_background);
        }
        builder.setPositiveButton(R.string.ok, listener);
        builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which != -1) {
            return;
        }
        setRestrictBackground(true);
    }
}
