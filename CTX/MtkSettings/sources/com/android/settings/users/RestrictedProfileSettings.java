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

public class RestrictedProfileSettings extends AppRestrictionsFragment implements EditUserInfoController.OnContentChangedCallback {
    private ImageView mDeleteButton;
    private EditUserInfoController mEditUserInfoController = new EditUserInfoController();
    private View mHeaderView;
    private ImageView mUserIconView;
    private TextView mUserNameView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mEditUserInfoController.onRestoreInstanceState(bundle);
        }
        init(bundle);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        this.mHeaderView = setPinnedHeaderView(R.layout.user_info_header);
        this.mHeaderView.setOnClickListener(this);
        this.mUserIconView = (ImageView) this.mHeaderView.findViewById(android.R.id.icon);
        this.mUserNameView = (TextView) this.mHeaderView.findViewById(android.R.id.title);
        this.mDeleteButton = (ImageView) this.mHeaderView.findViewById(R.id.delete);
        this.mDeleteButton.setOnClickListener(this);
        super.onActivityCreated(bundle);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mEditUserInfoController.onSaveInstanceState(bundle);
    }

    @Override
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

    @Override
    public void startActivityForResult(Intent intent, int i) {
        this.mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, i);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        this.mEditUserInfoController.onActivityResult(i, i2, intent);
    }

    @Override
    public void onClick(View view) {
        if (view == this.mHeaderView) {
            showDialog(1);
        } else if (view == this.mDeleteButton) {
            showDialog(2);
        } else {
            super.onClick(view);
        }
    }

    @Override
    public Dialog onCreateDialog(int i) {
        if (i == 1) {
            return this.mEditUserInfoController.createDialog(this, this.mUserIconView.getDrawable(), this.mUserNameView.getText(), R.string.profile_info_settings_title, this, this.mUser);
        }
        if (i == 2) {
            return UserDialogs.createRemoveDialog(getActivity(), this.mUser.getIdentifier(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    RestrictedProfileSettings.this.removeUser();
                }
            });
        }
        return null;
    }

    @Override
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
        getView().post(new Runnable() {
            @Override
            public void run() {
                RestrictedProfileSettings.this.mUserManager.removeUser(RestrictedProfileSettings.this.mUser.getIdentifier());
                RestrictedProfileSettings.this.finishFragment();
            }
        });
    }

    @Override
    public void onPhotoChanged(Drawable drawable) {
        this.mUserIconView.setImageDrawable(drawable);
    }

    @Override
    public void onLabelChanged(CharSequence charSequence) {
        this.mUserNameView.setText(charSequence);
    }
}
