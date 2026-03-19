package com.android.packageinstaller.permission.ui.television;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.ui.GrantPermissionsViewHandler;

public final class GrantPermissionsViewHandlerImpl implements View.OnClickListener, GrantPermissionsViewHandler {
    private Button mAllowButton;
    private final Context mContext;
    private TextView mCurrentGroupView;
    private String mGroupName;
    private Button mHardDenyButton;
    private ImageView mIconView;
    private TextView mMessageView;
    private GrantPermissionsViewHandler.ResultListener mResultListener;
    private LinearLayout mRootView;
    private Button mSoftDenyButton;

    public GrantPermissionsViewHandlerImpl(Context context, String str) {
        this.mContext = context;
    }

    public GrantPermissionsViewHandlerImpl setResultListener(GrantPermissionsViewHandler.ResultListener resultListener) {
        this.mResultListener = resultListener;
        return this;
    }

    @Override
    public View createView() {
        this.mRootView = (LinearLayout) LayoutInflater.from(this.mContext).inflate(R.layout.grant_permissions, (ViewGroup) null);
        this.mMessageView = (TextView) this.mRootView.findViewById(R.id.permission_message);
        this.mIconView = (ImageView) this.mRootView.findViewById(R.id.permission_icon);
        this.mCurrentGroupView = (TextView) this.mRootView.findViewById(R.id.current_page_text);
        this.mAllowButton = (Button) this.mRootView.findViewById(R.id.permission_allow_button);
        this.mSoftDenyButton = (Button) this.mRootView.findViewById(R.id.permission_deny_button);
        this.mHardDenyButton = (Button) this.mRootView.findViewById(R.id.permission_deny_dont_ask_again_button);
        this.mAllowButton.setOnClickListener(this);
        this.mSoftDenyButton.setOnClickListener(this);
        this.mHardDenyButton.setOnClickListener(this);
        return this.mRootView;
    }

    @Override
    public void updateWindowAttributes(WindowManager.LayoutParams layoutParams) {
        layoutParams.width = -1;
        layoutParams.height = -2;
        layoutParams.format = -1;
        layoutParams.gravity = 80;
        layoutParams.type = 2008;
        layoutParams.flags |= 128;
    }

    @Override
    public void updateUi(String str, int i, int i2, Icon icon, CharSequence charSequence, boolean z) {
        this.mGroupName = str;
        this.mMessageView.setText(charSequence);
        if (icon != null) {
            this.mIconView.setImageIcon(icon);
        }
        this.mHardDenyButton.setVisibility(z ? 0 : 8);
        if (i > 1) {
            this.mCurrentGroupView.setVisibility(0);
            this.mCurrentGroupView.setText(this.mContext.getString(R.string.current_permission_template, Integer.valueOf(i2 + 1), Integer.valueOf(i)));
        } else {
            this.mCurrentGroupView.setVisibility(4);
        }
    }

    @Override
    public void saveInstanceState(Bundle bundle) {
        bundle.putString("ARG_GROUP_NAME", this.mGroupName);
    }

    @Override
    public void loadInstanceState(Bundle bundle) {
        this.mGroupName = bundle.getString("ARG_GROUP_NAME");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        boolean z = true;
        boolean z2 = false;
        if (id == R.id.permission_allow_button) {
            z2 = true;
            z = false;
        } else if (id != R.id.permission_deny_dont_ask_again_button) {
            z = false;
        }
        if (this.mResultListener != null) {
            this.mResultListener.onPermissionGrantResult(this.mGroupName, z2, z);
        }
    }
}
