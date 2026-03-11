package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;

public class TimeoutListPreference extends RestrictedListPreference {
    private RestrictedLockUtils.EnforcedAdmin mAdmin;
    private final CharSequence[] mInitialEntries;
    private final CharSequence[] mInitialValues;

    public TimeoutListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInitialEntries = getEntries();
        this.mInitialValues = getEntryValues();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        if (this.mAdmin != null) {
            builder.setView(R.layout.admin_disabled_other_options_footer);
        } else {
            builder.setView((View) null);
        }
    }

    @Override
    protected void onDialogCreated(Dialog dialog) {
        super.onDialogCreated(dialog);
        dialog.create();
        if (this.mAdmin == null) {
            return;
        }
        View footerView = dialog.findViewById(R.id.admin_disabled_other_options);
        footerView.findViewById(R.id.admin_more_details_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(TimeoutListPreference.this.getContext(), TimeoutListPreference.this.mAdmin);
            }
        });
    }

    public void removeUnusableTimeouts(long maxTimeout, RestrictedLockUtils.EnforcedAdmin admin) {
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService("device_policy");
        if (dpm == null) {
            return;
        }
        if (admin == null && this.mAdmin == null && !isDisabledByAdmin()) {
            return;
        }
        if (admin == null) {
            maxTimeout = Long.MAX_VALUE;
        }
        ArrayList<CharSequence> revisedEntries = new ArrayList<>();
        ArrayList<CharSequence> revisedValues = new ArrayList<>();
        for (int i = 0; i < this.mInitialValues.length; i++) {
            long timeout = Long.parseLong(this.mInitialValues[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(this.mInitialEntries[i]);
                revisedValues.add(this.mInitialValues[i]);
            }
        }
        if (revisedValues.size() == 0) {
            setDisabledByAdmin(admin);
            return;
        }
        setDisabledByAdmin(null);
        if (revisedEntries.size() == getEntries().length) {
            return;
        }
        int userPreference = Integer.parseInt(getValue());
        setEntries((CharSequence[]) revisedEntries.toArray(new CharSequence[0]));
        setEntryValues((CharSequence[]) revisedValues.toArray(new CharSequence[0]));
        this.mAdmin = admin;
        if (userPreference <= maxTimeout) {
            setValue(String.valueOf(userPreference));
        } else {
            if (revisedValues.size() <= 0 || Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString()) != maxTimeout) {
                return;
            }
            setValue(String.valueOf(maxTimeout));
        }
    }
}
