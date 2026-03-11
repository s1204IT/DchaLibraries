package com.android.settings.vpn2;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.settings.GearPreference;
import com.android.settings.R;

public abstract class ManageablePreference extends GearPreference {
    public static int STATE_NONE = -1;
    boolean mIsAlwaysOn;
    int mState;
    int mUserId;

    public ManageablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsAlwaysOn = false;
        this.mState = STATE_NONE;
        setPersistent(false);
        setOrder(0);
        setUserId(UserHandle.myUserId());
    }

    public int getUserId() {
        return this.mUserId;
    }

    public void setUserId(int userId) {
        this.mUserId = userId;
        checkRestrictionAndSetDisabled("no_config_vpn", userId);
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int state) {
        if (this.mState == state) {
            return;
        }
        this.mState = state;
        updateSummary();
        notifyHierarchyChanged();
    }

    public void setAlwaysOn(boolean isEnabled) {
        if (this.mIsAlwaysOn == isEnabled) {
            return;
        }
        this.mIsAlwaysOn = isEnabled;
        updateSummary();
    }

    protected void updateSummary() {
        Resources res = getContext().getResources();
        String[] states = res.getStringArray(R.array.vpn_states);
        String summary = this.mState == STATE_NONE ? "" : states[this.mState];
        if (this.mIsAlwaysOn) {
            String alwaysOnString = res.getString(R.string.vpn_always_on_active);
            summary = TextUtils.isEmpty(summary) ? alwaysOnString : res.getString(R.string.join_two_unrelated_items, summary, alwaysOnString);
        }
        setSummary(summary);
    }
}
