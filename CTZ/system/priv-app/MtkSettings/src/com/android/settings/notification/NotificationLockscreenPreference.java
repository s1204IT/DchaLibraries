package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class NotificationLockscreenPreference extends RestrictedListPreference {
    private RestrictedLockUtils.EnforcedAdmin mAdminRestrictingRemoteInput;
    private boolean mAllowRemoteInput;
    private Listener mListener;
    private boolean mRemoteInputCheckBoxEnabled;
    private boolean mShowRemoteInput;
    private int mUserId;

    public NotificationLockscreenPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRemoteInputCheckBoxEnabled = true;
        this.mUserId = UserHandle.myUserId();
    }

    @Override // android.support.v7.preference.DialogPreference, android.support.v7.preference.Preference
    protected void onClick() {
        Context context = getContext();
        if (!Utils.startQuietModeDialogIfNecessary(context, UserManager.get(context), this.mUserId)) {
            super.onClick();
        }
    }

    @Override // com.android.settings.RestrictedListPreference, com.android.settings.CustomListPreference
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        this.mListener = new Listener(onClickListener);
        builder.setSingleChoiceItems(createListAdapter(), getSelectedValuePos(), this.mListener);
        this.mShowRemoteInput = getEntryValues().length == 3;
        this.mAllowRemoteInput = Settings.Secure.getInt(getContext().getContentResolver(), "lock_screen_allow_remote_input", 0) != 0;
        builder.setView(R.layout.lockscreen_remote_input);
    }

    @Override // com.android.settings.CustomListPreference
    protected void onDialogCreated(Dialog dialog) {
        super.onDialogCreated(dialog);
        dialog.create();
        CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.lockscreen_remote_input);
        checkBox.setChecked(!this.mAllowRemoteInput);
        checkBox.setOnCheckedChangeListener(this.mListener);
        checkBox.setEnabled(this.mAdminRestrictingRemoteInput == null);
        dialog.findViewById(R.id.restricted_lock_icon_remote_input).setVisibility(this.mAdminRestrictingRemoteInput == null ? 8 : 0);
        if (this.mAdminRestrictingRemoteInput != null) {
            checkBox.setClickable(false);
            dialog.findViewById(android.R.id.autofill_save_title).setOnClickListener(this.mListener);
        }
    }

    @Override // com.android.settings.CustomListPreference
    protected void onDialogStateRestored(Dialog dialog, Bundle bundle) {
        super.onDialogStateRestored(dialog, bundle);
        int checkedItemPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
        View viewFindViewById = dialog.findViewById(android.R.id.autofill_save_title);
        viewFindViewById.setVisibility(checkboxVisibilityForSelectedIndex(checkedItemPosition, this.mShowRemoteInput));
        this.mListener.setView(viewFindViewById);
    }

    @Override // com.android.settings.RestrictedListPreference
    protected ListAdapter createListAdapter() {
        return new RestrictedListPreference.RestrictedArrayAdapter(getContext(), getEntries(), -1);
    }

    @Override // com.android.settings.CustomListPreference
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        Settings.Secure.putInt(getContext().getContentResolver(), "lock_screen_allow_remote_input", this.mAllowRemoteInput ? 1 : 0);
    }

    @Override // com.android.settings.CustomListPreference
    protected boolean isAutoClosePreference() {
        return false;
    }

    private int checkboxVisibilityForSelectedIndex(int i, boolean z) {
        return (i == 1 && z && this.mRemoteInputCheckBoxEnabled) ? 0 : 8;
    }

    private class Listener implements DialogInterface.OnClickListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        private final DialogInterface.OnClickListener mInner;
        private View mView;

        public Listener(DialogInterface.OnClickListener onClickListener) {
            this.mInner = onClickListener;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            this.mInner.onClick(dialogInterface, i);
            int checkedItemPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();
            if (this.mView != null) {
                this.mView.setVisibility(NotificationLockscreenPreference.this.checkboxVisibilityForSelectedIndex(checkedItemPosition, NotificationLockscreenPreference.this.mShowRemoteInput));
            }
        }

        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            NotificationLockscreenPreference.this.mAllowRemoteInput = !z;
        }

        public void setView(View view) {
            this.mView = view;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (view.getId() == 16908820) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(NotificationLockscreenPreference.this.getContext(), NotificationLockscreenPreference.this.mAdminRestrictingRemoteInput);
            }
        }
    }
}
