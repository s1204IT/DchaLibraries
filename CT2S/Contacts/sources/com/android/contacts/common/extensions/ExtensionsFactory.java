package com.android.contacts.common.extensions;

import java.util.Properties;

public class ExtensionsFactory {
    private static String TAG = "ExtensionsFactory";
    private static Properties sProperties = null;
    private static ExtendedPhoneDirectoriesManager mExtendedPhoneDirectoriesManager = null;

    public static ExtendedPhoneDirectoriesManager getExtendedPhoneDirectoriesManager() {
        return mExtendedPhoneDirectoriesManager;
    }
}
