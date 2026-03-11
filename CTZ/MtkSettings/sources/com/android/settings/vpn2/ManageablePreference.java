package com.android.settings.vpn2;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.settings.R;
import com.android.settings.widget.GearPreference;

public abstract class ManageablePreference extends GearPreference {
    public static int STATE_NONE = -1;
    boolean mIsAlwaysOn;
    int mState;
    int mUserId;

    public ManageablePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsAlwaysOn = false;
        this.mState = STATE_NONE;
        setPersistent(false);
        setOrder(0);
        setUserId(UserHandle.myUserId());
    }

    public int getUserId() {
        return this.mUserId;
    }

    public void setUserId(int i) {
        this.mUserId = i;
        checkRestrictionAndSetDisabled("no_config_vpn", i);
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int i) {
        if (this.mState != i) {
            this.mState = i;
            updateSummary();
            notifyHierarchyChanged();
        }
    }

    public void setAlwaysOn(boolean z) {
        if (this.mIsAlwaysOn != z) {
            this.mIsAlwaysOn = z;
            updateSummary();
        }
    }

    protected void updateSummary() {
        Resources resources = getContext().getResources();
        String string = this.mState == STATE_NONE ? "" : resources.getStringArray(R.array.vpn_states)[this.mState];
        if (this.mIsAlwaysOn) {
            String string2 = resources.getString(R.string.vpn_always_on_summary_active);
            if (!TextUtils.isEmpty(string)) {
                string = resources.getString(R.string.join_two_unrelated_items, string, string2);
            } else {
                string = string2;
            }
        }
        setSummary(string);
    }
}
