package com.mediatek.contacts;

import android.app.Application;
import android.content.Context;
import com.mediatek.contacts.ext.ContactsCustomizationUtils;
import com.mediatek.contacts.ext.IContactsCommonPresenceExtension;
import com.mediatek.contacts.ext.IContactsPickerExtension;
import com.mediatek.contacts.ext.IOp01Extension;
import com.mediatek.contacts.ext.IRcsExtension;
import com.mediatek.contacts.ext.IRcsRichUiExtension;
import com.mediatek.contacts.ext.IViewCustomExtension;
import com.mediatek.contacts.util.Log;

public final class ExtensionManager {
    private static IContactsCommonPresenceExtension sContactsCommonPresenceExtension;
    private static final String TAG = ExtensionManager.class.getSimpleName();
    private static ExtensionManager sInstance = null;
    private static Context sContext = null;

    private ExtensionManager() {
    }

    public static ExtensionManager getInstance() {
        if (sInstance == null) {
            createInstanceSynchronized();
        }
        return sInstance;
    }

    private static synchronized void createInstanceSynchronized() {
        if (sInstance == null) {
            sInstance = new ExtensionManager();
        }
    }

    public static void registerApplicationContext(Application application) {
        sContext = application.getApplicationContext();
    }

    public static IOp01Extension getOp01Extension() {
        return ContactsCustomizationUtils.getOp01Factory(sContext).makeOp01Ext(sContext);
    }

    public static IContactsCommonPresenceExtension getContactsCommonPresenceExtension() {
        if (sContactsCommonPresenceExtension == null) {
            synchronized (IContactsCommonPresenceExtension.class) {
                if (sContactsCommonPresenceExtension == null) {
                    sContactsCommonPresenceExtension = ContactsCustomizationUtils.getContactsPresenceFactory(sContext).makeContactsCommonPresenceExt(sContext);
                }
            }
        }
        return sContactsCommonPresenceExtension;
    }

    public static IContactsPickerExtension getContactsPickerExtension() {
        return ContactsCustomizationUtils.getContactsPickerFactory(sContext).makeContactsPickerExt(sContext);
    }

    public static IRcsExtension getRcsExtension() {
        return ContactsCustomizationUtils.getRcsFactory(sContext).makeRcsExt(sContext);
    }

    public static IViewCustomExtension getViewCustomExtension() {
        return ContactsCustomizationUtils.getWWOPRcsFactory(sContext).makeViewCustomExt(sContext);
    }

    public static IRcsRichUiExtension getRcsRichUiExtension() {
        return ContactsCustomizationUtils.getRcsRichUiFactory(sContext).makeRcsRichUiExt(sContext);
    }

    public static void resetExtensions() {
        Log.d(TAG, "[resetExtensions]");
        synchronized (IContactsCommonPresenceExtension.class) {
            sContactsCommonPresenceExtension = null;
        }
        ContactsCustomizationUtils.resetCustomizationFactory();
    }
}
