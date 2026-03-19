package com.android.phone;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.phone.settings.fdn.EditPinPreference;

public class CallBarringDeselectAllPreference extends EditPinPreference {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "CallBarringDeselectAllPreference";
    private Phone mPhone;
    private boolean mShowPassword;

    public CallBarringDeselectAllPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void showDialog(Bundle bundle) {
        ImsPhone imsPhone = this.mPhone != null ? (ImsPhone) this.mPhone.getImsPhone() : null;
        this.mShowPassword = imsPhone == null || !(imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled());
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        if (!this.mShowPassword && carrierConfigForSubId.getBoolean("mtk_show_call_barring_password_bool")) {
            Log.d(LOG_TAG, "Show password for CSFB operators");
            this.mShowPassword = DBG;
        }
        setDialogMessage(getContext().getString(this.mShowPassword ? R.string.messageCallBarring : R.string.call_barring_deactivate_all_no_password));
        Log.d(LOG_TAG, "showDialog: mShowPassword: " + this.mShowPassword);
        super.showDialog(bundle);
    }

    void init(Phone phone) {
        Log.d(LOG_TAG, "init: phoneId = " + phone.getPhoneId());
        this.mPhone = phone;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText editText = (EditText) view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setVisibility(this.mShowPassword ? 0 : 8);
        }
    }

    protected boolean needInputMethod() {
        return this.mShowPassword;
    }

    boolean isPasswordShown() {
        return this.mShowPassword;
    }
}
