package com.android.systemui.qs.car;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.QSFooter;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.car.CarQSFragment;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.UserInfoController;

public class CarQSFooter extends RelativeLayout implements QSFooter, UserInfoController.OnUserInfoChangedListener {
    private ImageView mMultiUserAvatar;
    private MultiUserSwitch mMultiUserSwitch;
    private UserInfoController mUserInfoController;
    private TextView mUserName;
    private CarQSFragment.UserSwitchCallback mUserSwitchCallback;

    public CarQSFooter(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) this.mMultiUserSwitch.findViewById(R.id.multi_user_avatar);
        this.mUserName = (TextView) findViewById(R.id.user_name);
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        this.mMultiUserSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                CarQSFooter.lambda$onFinishInflate$0(this.f$0, view);
            }
        });
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                CarQSFooter.lambda$onFinishInflate$2(view);
            }
        });
    }

    public static void lambda$onFinishInflate$0(CarQSFooter carQSFooter, View view) {
        if (carQSFooter.mUserSwitchCallback == null) {
            Log.e("CarQSFooter", "CarQSFooter not properly set up; cannot display user switcher.");
        } else if (!carQSFooter.mUserSwitchCallback.isShowing()) {
            carQSFooter.mUserSwitchCallback.show();
        } else {
            carQSFooter.mUserSwitchCallback.hide();
        }
    }

    static void lambda$onFinishInflate$2(View view) {
        ActivityStarter activityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        if (!((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class)).isCurrentUserSetup()) {
            activityStarter.postQSRunnableDismissingKeyguard(new Runnable() {
                @Override
                public final void run() {
                    CarQSFooter.lambda$onFinishInflate$1();
                }
            });
        } else {
            activityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
        }
    }

    static void lambda$onFinishInflate$1() {
    }

    @Override
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        this.mMultiUserAvatar.setImageDrawable(drawable);
        this.mUserName.setText(str);
    }

    @Override
    public void setQSPanel(QSPanel qSPanel) {
        if (qSPanel != null) {
            this.mMultiUserSwitch.setQsPanel(qSPanel);
        }
    }

    public void setUserSwitchCallback(CarQSFragment.UserSwitchCallback userSwitchCallback) {
        this.mUserSwitchCallback = userSwitchCallback;
    }

    @Override
    public void setListening(boolean z) {
        if (z) {
            this.mUserInfoController.addCallback(this);
        } else {
            this.mUserInfoController.removeCallback(this);
        }
    }

    @Override
    public void setExpandClickListener(View.OnClickListener onClickListener) {
    }

    @Override
    public void setExpanded(boolean z) {
    }

    @Override
    public void setExpansion(float f) {
    }

    @Override
    public void setKeyguardShowing(boolean z) {
    }
}
