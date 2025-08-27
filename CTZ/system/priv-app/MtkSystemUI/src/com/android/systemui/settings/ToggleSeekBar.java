package com.android.systemui.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;

/* loaded from: classes.dex */
public class ToggleSeekBar extends SeekBar {
    private String mAccessibilityLabel;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    public ToggleSeekBar(Context context) {
        super(context);
        this.mEnforcedAdmin = null;
    }

    public ToggleSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnforcedAdmin = null;
    }

    public ToggleSeekBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mEnforcedAdmin = null;
    }

    @Override // android.widget.AbsSeekBar, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mEnforcedAdmin != null) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(RestrictedLockUtils.getShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin), 0);
            return true;
        }
        if (!isEnabled()) {
            setEnabled(true);
        }
        return super.onTouchEvent(motionEvent);
    }

    public void setAccessibilityLabel(String str) {
        this.mAccessibilityLabel = str;
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.mAccessibilityLabel != null) {
            accessibilityNodeInfo.setText(this.mAccessibilityLabel);
        }
    }

    public void setEnforcedAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        this.mEnforcedAdmin = enforcedAdmin;
    }
}
