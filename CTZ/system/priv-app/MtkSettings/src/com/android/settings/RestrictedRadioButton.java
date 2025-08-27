package com.android.settings;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.RadioButton;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class RestrictedRadioButton extends RadioButton {
    private Context mContext;
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    public RestrictedRadioButton(Context context) {
        this(context, null);
    }

    public RestrictedRadioButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.radioButtonStyle);
    }

    public RestrictedRadioButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RestrictedRadioButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mContext = context;
    }

    @Override // android.widget.CompoundButton, android.view.View
    public boolean performClick() {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin);
            return true;
        }
        return super.performClick();
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        boolean z = enforcedAdmin != null;
        this.mEnforcedAdmin = enforcedAdmin;
        if (this.mDisabledByAdmin != z) {
            this.mDisabledByAdmin = z;
            RestrictedLockUtils.setTextViewAsDisabledByAdmin(this.mContext, this, this.mDisabledByAdmin);
            if (this.mDisabledByAdmin) {
                getButtonDrawable().setColorFilter(this.mContext.getColor(R.color.disabled_text_color), PorterDuff.Mode.MULTIPLY);
            } else {
                getButtonDrawable().clearColorFilter();
            }
        }
    }

    public boolean isDisabledByAdmin() {
        return this.mDisabledByAdmin;
    }
}
