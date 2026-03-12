package com.android.keyguard;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardHostView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class KeyguardMultiUserSelectorView extends FrameLayout implements View.OnClickListener {
    private KeyguardMultiUserAvatar mActiveUserAvatar;
    private KeyguardHostView.UserSwitcherCallback mCallback;
    Comparator<UserInfo> mOrderAddedComparator;
    private ViewGroup mUsersGrid;

    public KeyguardMultiUserSelectorView(Context context) {
        this(context, null, 0);
    }

    public KeyguardMultiUserSelectorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardMultiUserSelectorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOrderAddedComparator = new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo lhs, UserInfo rhs) {
                return lhs.serialNumber - rhs.serialNumber;
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        this.mUsersGrid = (ViewGroup) findViewById(R.id.keyguard_users_grid);
        this.mUsersGrid.removeAllViews();
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setCallback(KeyguardHostView.UserSwitcherCallback callback) {
        this.mCallback = callback;
    }

    public void addUsers(Collection<UserInfo> userList) {
        UserInfo activeUser;
        try {
            activeUser = ActivityManagerNative.getDefault().getCurrentUser();
        } catch (RemoteException e) {
            activeUser = null;
        }
        ArrayList<UserInfo> users = new ArrayList<>((Collection<? extends UserInfo>) userList);
        Collections.sort(users, this.mOrderAddedComparator);
        for (UserInfo user : users) {
            if (user.supportsSwitchTo()) {
                KeyguardMultiUserAvatar uv = createAndAddUser(user);
                if (user.id == activeUser.id) {
                    this.mActiveUserAvatar = uv;
                }
                uv.setActive(false, false, null);
            }
        }
        this.mActiveUserAvatar.lockPressed(true);
    }

    public void finalizeActiveUserView(boolean animate) {
        if (animate) {
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    KeyguardMultiUserSelectorView.this.finalizeActiveUserNow(true);
                }
            }, 500L);
        } else {
            finalizeActiveUserNow(animate);
        }
    }

    void finalizeActiveUserNow(boolean animate) {
        this.mActiveUserAvatar.lockPressed(false);
        this.mActiveUserAvatar.setActive(true, animate, null);
    }

    private KeyguardMultiUserAvatar createAndAddUser(UserInfo user) {
        KeyguardMultiUserAvatar uv = KeyguardMultiUserAvatar.fromXml(R.layout.keyguard_multi_user_avatar, this.mContext, this, user);
        this.mUsersGrid.addView(uv);
        return uv;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != 3 && this.mCallback != null) {
            this.mCallback.userActivity();
            return false;
        }
        return false;
    }

    private void setAllClickable(boolean clickable) {
        for (int i = 0; i < this.mUsersGrid.getChildCount(); i++) {
            View v = this.mUsersGrid.getChildAt(i);
            v.setClickable(clickable);
            v.setPressed(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof KeyguardMultiUserAvatar) {
            final KeyguardMultiUserAvatar avatar = (KeyguardMultiUserAvatar) v;
            if (avatar.isClickable()) {
                if (this.mActiveUserAvatar == avatar) {
                    this.mCallback.showUnlockHint();
                    return;
                }
                this.mCallback.hideSecurityView(100);
                setAllClickable(false);
                avatar.lockPressed(true);
                this.mActiveUserAvatar.setActive(false, true, new Runnable() {
                    @Override
                    public void run() {
                        KeyguardMultiUserSelectorView.this.mActiveUserAvatar = avatar;
                        try {
                            ActivityManagerNative.getDefault().switchUser(avatar.getUserInfo().id);
                        } catch (RemoteException re) {
                            Log.e("KeyguardMultiUserSelectorView", "Couldn't switch user " + re);
                        }
                    }
                });
            }
        }
    }
}
