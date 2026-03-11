package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class UserTile extends QSTileImpl<QSTile.State> implements UserInfoController.OnUserInfoChangedListener {
    private Pair<String, Drawable> mLastUpdate;
    private final UserInfoController mUserInfoController;
    private final UserSwitcherController mUserSwitcherController;

    public UserTile(QSHost qSHost) {
        super(qSHost);
        this.mUserSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("android.settings.USER_SETTINGS");
    }

    @Override
    protected void handleClick() {
        showDetail(true);
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return this.mUserSwitcherController.userDetailAdapter;
    }

    @Override
    public int getMetricsCategory() {
        return 260;
    }

    @Override
    public void handleSetListening(boolean z) {
        if (z) {
            this.mUserInfoController.addCallback(this);
        } else {
            this.mUserInfoController.removeCallback(this);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object obj) {
        final Pair<String, Drawable> pair = obj != null ? (Pair) obj : this.mLastUpdate;
        if (pair != null) {
            state.label = (CharSequence) pair.first;
            state.contentDescription = (CharSequence) pair.first;
            state.icon = new QSTile.Icon() {
                @Override
                public Drawable getDrawable(Context context) {
                    return (Drawable) pair.second;
                }
            };
        }
    }

    @Override
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        this.mLastUpdate = new Pair<>(str, drawable);
        refreshState(this.mLastUpdate);
    }
}
