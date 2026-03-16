package com.android.settings.users;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            this.mEditUserInfoController.onRestoreInstanceState(icicle);
        }
        init(icicle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (this.mHeaderView == null) {
            this.mHeaderView = LayoutInflater.from(getActivity()).inflate(R.layout.user_info_header, (ViewGroup) null);
            setPinnedHeaderView(this.mHeaderView);
            this.mHeaderView.setOnClickListener(this);
            this.mUserIconView = (ImageView) this.mHeaderView.findViewById(android.R.id.icon);
            this.mUserNameView = (TextView) this.mHeaderView.findViewById(android.R.id.title);
            this.mDeleteButton = (ImageView) this.mHeaderView.findViewById(R.id.delete);
            this.mDeleteButton.setOnClickListener(this);
            getListView().setFastScrollEnabled(true);
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mEditUserInfoController.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        UserInfo info = Utils.getExistingUser(this.mUserManager, this.mUser);
        if (info == null) {
            finishFragment();
        } else {
            ((TextView) this.mHeaderView.findViewById(android.R.id.title)).setText(info.name);
            ((ImageView) this.mHeaderView.findViewById(android.R.id.icon)).setImageDrawable(getCircularUserIcon());
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        this.mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.mEditUserInfoController.onActivityResult(requestCode, resultCode, data);
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
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == 1) {
            return this.mEditUserInfoController.createDialog(this, this.mUserIconView.getDrawable(), this.mUserNameView.getText(), R.string.profile_info_settings_title, this, this.mUser);
        }
        if (dialogId == 2) {
            return Utils.createRemoveConfirmationDialog(getActivity(), this.mUser.getIdentifier(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RestrictedProfileSettings.this.removeUser();
                }
            });
        }
        return null;
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
    public void onPhotoChanged(Drawable photo) {
        this.mUserIconView.setImageDrawable(photo);
    }

    @Override
    public void onLabelChanged(CharSequence label) {
        this.mUserNameView.setText(label);
    }
}
