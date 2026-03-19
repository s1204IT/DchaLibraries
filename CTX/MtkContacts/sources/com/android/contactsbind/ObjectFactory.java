package com.android.contactsbind;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import com.android.contacts.logging.Logger;
import com.android.contacts.util.DeviceLocalAccountTypeFactory;
import com.mediatek.contacts.model.MtkDeviceLocalAccountTypeFactory;

public class ObjectFactory {
    public static Logger getLogger() {
        return null;
    }

    public static DeviceLocalAccountTypeFactory getDeviceLocalAccountTypeFactory(Context context) {
        return new MtkDeviceLocalAccountTypeFactory(context);
    }

    public static Fragment getAssistantFragment() {
        return null;
    }

    public static Uri getWelcomeUri() {
        return null;
    }
}
