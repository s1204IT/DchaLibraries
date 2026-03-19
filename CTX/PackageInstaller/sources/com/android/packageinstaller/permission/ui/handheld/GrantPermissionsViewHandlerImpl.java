package com.android.packageinstaller.permission.ui.handheld;

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.packageinstaller.permission.ui.ButtonBarLayout;
import com.android.packageinstaller.permission.ui.GrantPermissionsViewHandler;
import com.android.packageinstaller.permission.ui.ManualLayoutFrame;

public class GrantPermissionsViewHandlerImpl implements View.OnClickListener, GrantPermissionsViewHandler {
    private final Activity mActivity;
    private Button mAllowButton;
    private final String mAppPackageName;
    private ButtonBarLayout mButtonBar;
    private ViewGroup mCurrentDesc;
    private TextView mCurrentGroupView;
    private ViewGroup mDescContainer;
    private ViewGroup mDialogContainer;
    private CheckBox mDoNotAskCheckbox;
    private boolean mDoNotAskChecked;
    private int mGroupCount;
    private Icon mGroupIcon;
    private int mGroupIndex;
    private CharSequence mGroupMessage;
    private String mGroupName;
    private ImageView mIconView;
    private TextView mMessageView;
    private Button mMoreInfoButton;
    private final boolean mPermissionReviewRequired;
    private GrantPermissionsViewHandler.ResultListener mResultListener;
    private ManualLayoutFrame mRootView;
    private boolean mShowDonNotAsk;

    public GrantPermissionsViewHandlerImpl(Activity activity, String str) {
        this.mActivity = activity;
        this.mAppPackageName = str;
        this.mPermissionReviewRequired = activity.getPackageManager().isPermissionReviewModeEnabled();
    }

    public GrantPermissionsViewHandlerImpl setResultListener(GrantPermissionsViewHandler.ResultListener resultListener) {
        this.mResultListener = resultListener;
        return this;
    }

    @Override
    public void saveInstanceState(Bundle bundle) {
        bundle.putString("ARG_GROUP_NAME", this.mGroupName);
        bundle.putInt("ARG_GROUP_COUNT", this.mGroupCount);
        bundle.putInt("ARG_GROUP_INDEX", this.mGroupIndex);
        bundle.putParcelable("ARG_GROUP_ICON", this.mGroupIcon);
        bundle.putCharSequence("ARG_GROUP_MESSAGE", this.mGroupMessage);
        bundle.putBoolean("ARG_GROUP_SHOW_DO_NOT_ASK", this.mShowDonNotAsk);
        bundle.putBoolean("ARG_GROUP_DO_NOT_ASK_CHECKED", this.mDoNotAskCheckbox.isChecked());
    }

    @Override
    public void loadInstanceState(Bundle bundle) {
        this.mGroupName = bundle.getString("ARG_GROUP_NAME");
        this.mGroupMessage = bundle.getCharSequence("ARG_GROUP_MESSAGE");
        this.mGroupIcon = (Icon) bundle.getParcelable("ARG_GROUP_ICON");
        this.mGroupCount = bundle.getInt("ARG_GROUP_COUNT");
        this.mGroupIndex = bundle.getInt("ARG_GROUP_INDEX");
        this.mShowDonNotAsk = bundle.getBoolean("ARG_GROUP_SHOW_DO_NOT_ASK");
        this.mDoNotAskChecked = bundle.getBoolean("ARG_GROUP_DO_NOT_ASK_CHECKED");
        updateDoNotAskCheckBox();
    }

    @Override
    public void updateUi(String str, int i, int i2, Icon icon, CharSequence charSequence, boolean z) {
        this.mGroupName = str;
        this.mGroupCount = i;
        this.mGroupIndex = i2;
        this.mGroupIcon = icon;
        this.mGroupMessage = charSequence;
        this.mShowDonNotAsk = z;
        this.mDoNotAskChecked = false;
        if (this.mIconView != null) {
            if (this.mGroupIndex > 0) {
                animateToPermission();
                return;
            }
            updateDescription();
            updateGroup();
            updateDoNotAskCheckBox();
        }
    }

    public void onConfigurationChanged() {
        this.mRootView.onConfigurationChanged();
    }

    private void animateOldContent(Runnable runnable) {
        Interpolator interpolatorLoadInterpolator = AnimationUtils.loadInterpolator(this.mActivity, R.interpolator.fast_out_linear_in);
        this.mIconView.animate().scaleX(0.0f).scaleY(0.0f).setDuration(200L).setInterpolator(interpolatorLoadInterpolator).start();
        this.mCurrentDesc.animate().alpha(0.0f).setDuration(200L).setInterpolator(interpolatorLoadInterpolator).withEndAction(runnable).start();
        if (!this.mShowDonNotAsk && this.mDoNotAskCheckbox.getVisibility() == 0) {
            this.mDoNotAskCheckbox.animate().alpha(0.0f).setDuration(200L).setInterpolator(interpolatorLoadInterpolator).start();
        }
    }

    private void attachNewContent(final Runnable runnable) {
        this.mCurrentDesc = (ViewGroup) LayoutInflater.from(this.mActivity).inflate(com.android.packageinstaller.R.layout.permission_description, this.mDescContainer, false);
        this.mDescContainer.removeAllViews();
        this.mDescContainer.addView(this.mCurrentDesc);
        this.mDialogContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                GrantPermissionsViewHandlerImpl.this.mDialogContainer.removeOnLayoutChangeListener(this);
                GrantPermissionsViewHandlerImpl.this.mCurrentDesc.setTranslationX(GrantPermissionsViewHandlerImpl.this.mDescContainer.getWidth());
                float height = ((i8 - i6) - GrantPermissionsViewHandlerImpl.this.mButtonBar.getHeight()) / GrantPermissionsViewHandlerImpl.this.mDescContainer.getHeight();
                GrantPermissionsViewHandlerImpl.this.mDescContainer.setScaleY(height);
                GrantPermissionsViewHandlerImpl.this.mDescContainer.setTranslationY((i6 - i2) + (((int) ((GrantPermissionsViewHandlerImpl.this.mDescContainer.getHeight() * height) - GrantPermissionsViewHandlerImpl.this.mDescContainer.getHeight())) / 2));
                GrantPermissionsViewHandlerImpl.this.mDescContainer.animate().translationY(0.0f).scaleY(1.0f).setInterpolator(AnimationUtils.loadInterpolator(GrantPermissionsViewHandlerImpl.this.mActivity, R.interpolator.linear_out_slow_in)).setDuration(300L).withEndAction(runnable).start();
            }
        });
        this.mMessageView = (TextView) this.mCurrentDesc.findViewById(com.android.packageinstaller.R.id.permission_message);
        this.mIconView = (ImageView) this.mCurrentDesc.findViewById(com.android.packageinstaller.R.id.permission_icon);
        boolean z = this.mDoNotAskCheckbox.getVisibility() == 0;
        updateDescription();
        updateGroup();
        updateDoNotAskCheckBox();
        if (!z && this.mShowDonNotAsk) {
            this.mDoNotAskCheckbox.setAlpha(0.0f);
        }
    }

    private void animateNewContent() {
        Interpolator interpolatorLoadInterpolator = AnimationUtils.loadInterpolator(this.mActivity, R.interpolator.linear_out_slow_in);
        this.mCurrentDesc.animate().translationX(0.0f).setDuration(300L).setInterpolator(interpolatorLoadInterpolator).start();
        if (this.mShowDonNotAsk && this.mDoNotAskCheckbox.getVisibility() == 0 && this.mDoNotAskCheckbox.getAlpha() < 1.0f) {
            this.mDoNotAskCheckbox.setAlpha(0.0f);
            this.mDoNotAskCheckbox.animate().alpha(1.0f).setDuration(300L).setInterpolator(interpolatorLoadInterpolator).start();
        }
    }

    private void animateToPermission() {
        animateOldContent(new Runnable() {
            @Override
            public void run() {
                GrantPermissionsViewHandlerImpl.this.attachNewContent(new Runnable() {
                    @Override
                    public void run() {
                        GrantPermissionsViewHandlerImpl.this.animateNewContent();
                    }
                });
            }
        });
    }

    @Override
    public View createView() {
        this.mRootView = (ManualLayoutFrame) LayoutInflater.from(this.mActivity).inflate(com.android.packageinstaller.R.layout.grant_permissions, (ViewGroup) null);
        this.mButtonBar = (ButtonBarLayout) this.mRootView.findViewById(com.android.packageinstaller.R.id.button_group);
        this.mButtonBar.setAllowStacking(true);
        this.mMessageView = (TextView) this.mRootView.findViewById(com.android.packageinstaller.R.id.permission_message);
        this.mIconView = (ImageView) this.mRootView.findViewById(com.android.packageinstaller.R.id.permission_icon);
        this.mCurrentGroupView = (TextView) this.mRootView.findViewById(com.android.packageinstaller.R.id.current_page_text);
        this.mDoNotAskCheckbox = (CheckBox) this.mRootView.findViewById(com.android.packageinstaller.R.id.do_not_ask_checkbox);
        this.mAllowButton = (Button) this.mRootView.findViewById(com.android.packageinstaller.R.id.permission_allow_button);
        this.mAllowButton.setOnClickListener(this);
        if (this.mPermissionReviewRequired) {
            this.mMoreInfoButton = (Button) this.mRootView.findViewById(com.android.packageinstaller.R.id.permission_more_info_button);
            this.mMoreInfoButton.setVisibility(0);
            this.mMoreInfoButton.setOnClickListener(this);
        }
        this.mDialogContainer = (ViewGroup) this.mRootView.findViewById(com.android.packageinstaller.R.id.dialog_container);
        this.mDescContainer = (ViewGroup) this.mRootView.findViewById(com.android.packageinstaller.R.id.desc_container);
        this.mCurrentDesc = (ViewGroup) this.mRootView.findViewById(com.android.packageinstaller.R.id.perm_desc_root);
        this.mRootView.findViewById(com.android.packageinstaller.R.id.permission_deny_button).setOnClickListener(this);
        this.mDoNotAskCheckbox.setOnClickListener(this);
        if (this.mGroupName != null) {
            updateDescription();
            updateGroup();
            updateDoNotAskCheckBox();
        }
        return this.mRootView;
    }

    public void updateWindowAttributes(WindowManager.LayoutParams layoutParams) {
    }

    private void updateDescription() {
        if (this.mGroupIcon != null) {
            this.mIconView.setImageDrawable(this.mGroupIcon.loadDrawable(this.mActivity));
        }
        this.mMessageView.setText(this.mGroupMessage);
    }

    private void updateGroup() {
        if (this.mGroupCount > 1) {
            this.mCurrentGroupView.setVisibility(0);
            this.mCurrentGroupView.setText(this.mActivity.getString(com.android.packageinstaller.R.string.current_permission_template, new Object[]{Integer.valueOf(this.mGroupIndex + 1), Integer.valueOf(this.mGroupCount)}));
        } else {
            this.mCurrentGroupView.setVisibility(8);
        }
    }

    private void updateDoNotAskCheckBox() {
        if (this.mShowDonNotAsk) {
            this.mDoNotAskCheckbox.setVisibility(0);
            this.mDoNotAskCheckbox.setOnClickListener(this);
            this.mDoNotAskCheckbox.setChecked(this.mDoNotAskChecked);
            this.mAllowButton.setEnabled(true ^ this.mDoNotAskChecked);
            return;
        }
        this.mDoNotAskCheckbox.setVisibility(8);
        this.mDoNotAskCheckbox.setOnClickListener(null);
        this.mAllowButton.setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case com.android.packageinstaller.R.id.do_not_ask_checkbox:
                this.mAllowButton.setEnabled(!this.mDoNotAskCheckbox.isChecked());
                break;
            case com.android.packageinstaller.R.id.permission_allow_button:
                if (this.mResultListener != null) {
                    view.performAccessibilityAction(128, null);
                    this.mResultListener.onPermissionGrantResult(this.mGroupName, true, false);
                }
                break;
            case com.android.packageinstaller.R.id.permission_deny_button:
                this.mAllowButton.setEnabled(true);
                if (this.mResultListener != null) {
                    view.performAccessibilityAction(128, null);
                    this.mResultListener.onPermissionGrantResult(this.mGroupName, false, this.mShowDonNotAsk && this.mDoNotAskCheckbox.isChecked());
                }
                break;
            case com.android.packageinstaller.R.id.permission_more_info_button:
                Intent intent = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
                intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppPackageName);
                intent.putExtra("com.android.packageinstaller.extra.ALL_PERMISSIONS", true);
                this.mActivity.startActivity(intent);
                break;
        }
    }
}
