package com.android.services.telephony.sip;

import android.app.Activity;
import android.content.Intent;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Parcelable;
import android.telecom.PhoneAccountHandle;
import android.util.Log;

public final class SipPhoneAccountSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        SipProfile sipProfileRetrieveSipProfileFromName;
        super.onCreate(bundle);
        Intent intent = getIntent();
        Log.i("SipSettingsActivity", "" + intent);
        if (intent != null) {
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
            Log.i("SipSettingsActivity", "" + phoneAccountHandle);
            if (phoneAccountHandle != null && (sipProfileRetrieveSipProfileFromName = new SipProfileDb(this).retrieveSipProfileFromName(SipUtil.getSipProfileNameFromPhoneAccount(phoneAccountHandle))) != null) {
                Intent intent2 = new Intent(this, (Class<?>) SipEditor.class);
                intent2.putExtra("sip_profile", (Parcelable) sipProfileRetrieveSipProfileFromName);
                startActivity(intent2);
            }
        }
        finish();
    }
}
