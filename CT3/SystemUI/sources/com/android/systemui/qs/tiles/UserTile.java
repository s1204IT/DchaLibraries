package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.util.Pair;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class UserTile extends QSTile<QSTile.State> implements UserInfoController.OnUserInfoChangedListener {
    private Pair<String, Drawable> mLastUpdate;
    private final UserInfoController mUserInfoController;
    private final UserSwitcherController mUserSwitcherController;

    public UserTile(QSTile.Host host) {
        super(host);
        this.mUserSwitcherController = host.getUserSwitcherController();
        this.mUserInfoController = host.getUserInfoController();
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.settings.USER_SETTINGS");
    }

    @Override
    protected void handleClick() {
        showDetail(true);
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mUserSwitcherController.userDetailAdapter;
    }

    @Override
    public int getMetricsCategory() {
        return 260;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mUserInfoController.addListener(this);
        } else {
            this.mUserInfoController.remListener(this);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object arg) {
        final Pair<String, Drawable> p = arg != null ? (Pair) arg : this.mLastUpdate;
        if (p == null) {
            return;
        }
        state.label = (CharSequence) p.first;
        state.contentDescription = (CharSequence) p.first;
        state.icon = new QSTile.Icon() {
            @Override
            public Drawable getDrawable(Context context) {
                return (Drawable) p.second;
            }
        };
    }

    @Override
    public void onUserInfoChanged(String name, Drawable picture) {
        this.mLastUpdate = new Pair<>(name, picture);
        refreshState(this.mLastUpdate);
    }
}
