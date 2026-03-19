package com.android.phone;

import android.app.Application;
import android.os.UserHandle;
import com.android.services.telephony.TelephonyGlobals;
import com.mediatek.phone.ext.ExtensionManager;

public class PhoneApp extends Application {
    PhoneGlobals mPhoneGlobals;
    TelephonyGlobals mTelephonyGlobals;

    @Override
    public void onCreate() {
        ExtensionManager.init(this);
        if (UserHandle.myUserId() == 0) {
            this.mPhoneGlobals = new PhoneGlobals(this);
            this.mPhoneGlobals.onCreate();
            this.mTelephonyGlobals = new TelephonyGlobals(this);
            this.mTelephonyGlobals.onCreate();
        }
        ExtensionManager.initPhoneHelper();
    }
}
