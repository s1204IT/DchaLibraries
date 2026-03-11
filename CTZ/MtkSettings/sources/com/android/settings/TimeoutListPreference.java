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

    public TimeoutListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInitialEntries = getEntries();
        this.mInitialValues = getEntryValues();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        super.onPrepareDialogBuilder(builder, onClickListener);
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
        if (this.mAdmin != null) {
            dialog.findViewById(R.id.admin_disabled_other_options).findViewById(R.id.admin_more_details_link).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(TimeoutListPreference.this.getContext(), TimeoutListPreference.this.mAdmin);
                }
            });
        }
    }

    public void removeUnusableTimeouts(long j, RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        if (((DevicePolicyManager) getContext().getSystemService("device_policy")) == null) {
            return;
        }
        if (enforcedAdmin == null && this.mAdmin == null && !isDisabledByAdmin()) {
            return;
        }
        if (enforcedAdmin == null) {
            j = Long.MAX_VALUE;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (int i = 0; i < this.mInitialValues.length; i++) {
            if (Long.parseLong(this.mInitialValues[i].toString()) <= j) {
                arrayList.add(this.mInitialEntries[i]);
                arrayList2.add(this.mInitialValues[i]);
            }
        }
        if (arrayList2.size() == 0) {
            setDisabledByAdmin(enforcedAdmin);
            return;
        }
        setDisabledByAdmin(null);
        if (arrayList.size() != getEntries().length) {
            int i2 = Integer.parseInt(getValue());
            setEntries((CharSequence[]) arrayList.toArray(new CharSequence[0]));
            setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[0]));
            this.mAdmin = enforcedAdmin;
            if (i2 <= j) {
                setValue(String.valueOf(i2));
            } else if (arrayList2.size() > 0 && Long.parseLong(((CharSequence) arrayList2.get(arrayList2.size() - 1)).toString()) == j) {
                setValue(String.valueOf(j));
            }
        }
    }
}
