package com.android.settings.users;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.users.EditUserInfoController;

/* loaded from: classes.dex */
public class RestrictedProfileSettings extends AppRestrictionsFragment implements EditUserInfoController.OnContentChangedCallback {
    private ImageView mDeleteButton;
    private EditUserInfoController mEditUserInfoController = new EditUserInfoController();
    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mEditUserInfoController.onRestoreInstanceState(bundle);
        }
        init(bundle);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        this.mHeaderView = setPinnedHeaderView(R.layout.user_info_header);
        this.mHeaderView.setOnClickListener(this);
        this.mUserIconView = (ImageView) this.mHeaderView.findViewById(android.R.id.icon);
        this.mUserNameView = (TextView) this.mHeaderView.findViewById(android.R.id.title);
        this.mDeleteButton = (ImageView) this.mHeaderView.findViewById(R.id.delete);
        this.mDeleteButton.setOnClickListener(this);
        super.onActivityCreated(bundle);
    }

    @Override // com.android.settings.users.AppRestrictionsFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mEditUserInfoController.onSaveInstanceState(bundle);
    }

    @Override // com.android.settings.users.AppRestrictionsFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        UserInfo existingUser = Utils.getExistingUser(this.mUserManager, this.mUser);
        if (existingUser == null) {
            finishFragment();
        } else {
            ((TextView) this.mHeaderView.findViewById(android.R.id.title)).setText(existingUser.name);
            ((ImageView) this.mHeaderView.findViewById(android.R.id.icon)).setImageDrawable(com.android.settingslib.Utils.getUserIcon(getActivity(), this.mUserManager, existingUser));
        }
    }

    @Override // android.app.Fragment
    public void startActivityForResult(Intent intent, int i) {
        this.mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, i);
    }

    @Override // com.android.settings.users.AppRestrictionsFragment, android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        this.mEditUserInfoController.onActivityResult(i, i2, intent);
    }

    @Override // com.android.settings.users.AppRestrictionsFragment, android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.mHeaderView) {
            showDialog(1);
        } else if (view == this.mDeleteButton) {
            showDialog(2);
        } else {
            super.onClick(view);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        if (i == 1) {
            return this.mEditUserInfoController.createDialog(this, this.mUserIconView.getDrawable(), this.mUserNameView.getText(), R.string.profile_info_settings_title, this, this.mUser);
        }
        if (i == 2) {
            return UserDialogs.createRemoveDialog(getActivity(), this.mUser.getIdentifier(), new DialogInterface.OnClickListener() { // from class: com.android.settings.users.RestrictedProfileSettings.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    RestrictedProfileSettings.this.removeUser();
                }
            });
        }
        return null;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        switch (i) {
            case 1:
                return 590;
            case 2:
                return 591;
            default:
                return 0;
        }
    }

    private void removeUser() {
        getView().post(new Runnable() { // from class: com.android.settings.users.RestrictedProfileSettings.2
            @Override // java.lang.Runnable
            public void run() {
                RestrictedProfileSettings.this.mUserManager.removeUser(RestrictedProfileSettings.this.mUser.getIdentifier());
                RestrictedProfileSettings.this.finishFragment();
            }
        });
    }

    @Override // com.android.settings.users.EditUserInfoController.OnContentChangedCallback
    public void onPhotoChanged(Drawable drawable) {
        this.mUserIconView.setImageDrawable(drawable);
    }

    @Override // com.android.settings.users.EditUserInfoController.OnContentChangedCallback
    public void onLabelChanged(CharSequence charSequence) {
        this.mUserNameView.setText(charSequence);
    }
}
