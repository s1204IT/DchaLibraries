package com.mediatek.contacts.ext;

import android.content.Context;
import android.util.Log;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class ContactsCustomizationUtils {
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sRcsInfoList;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sRcsRichUiInfoList;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sWwopRcsFactoryInfoList;
    static ContactsCustomizationFactoryBase sFactory = null;
    static ContactsCustomizationFactoryBase sRcsFactory = null;
    static ContactsCustomizationFactoryBase sRcsRichUiFactory = null;
    static ContactsCustomizationFactoryBase sWwopRcsFactory = null;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sFactoryInfoList = new ArrayList();

    static {
        Log.d("ContactsCustomizationUtils", "Init contacts plugin list begin");
        sFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Op01Contacts.apk", "com.mediatek.contacts.plugin.Op01ContactsCustomizationFactory", "com.mediatek.contacts.plugin", "OP01"));
        sFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP07Contacts.apk", "com.mediatek.contacts.plugin.Op07ContactsCustomizationFactory", "com.mediatek.contacts.plugin", "OP07"));
        sFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP08Contacts.apk", "com.mediatek.contacts.plugin.Op08ContactsCustomizationFactory", "com.mediatek.contacts.plugin", "OP08"));
        sFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP12Contacts.apk", "com.mediatek.contacts.plugin.Op12ContactsCustomizationFactory", "com.mediatek.contacts.plugin", "OP12"));
        sFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Op18Contacts.apk", "com.mediatek.op18.contacts.OP18ContactsCustomizationFactory", "com.mediatek.op18.contacts", "OP18"));
        Log.d("ContactsCustomizationUtils", "Init contacts plugin list end");
        sRcsInfoList = new ArrayList();
        sRcsInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("RCSContacts.apk", "com.mediatek.rcs.contacts.ext.Op01ContactsCustomizationFactory", "com.mediatek.rcs.contacts.ext", "OP01"));
        sRcsRichUiInfoList = new ArrayList();
        sRcsRichUiInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("RCSPhone.apk", "com.mediatek.rcs.incallui.ext.Op01ContactsCustomizationFactory", "com.mediatek.rcs.incallui.ext", "OP01"));
        sWwopRcsFactoryInfoList = new ArrayList();
        sWwopRcsFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Rcse.apk", "com.mediatek.rcse.plugin.contacts.RCSeContactsCustomizationFactory", "com.mediatek.rcs", "OP03"));
        sWwopRcsFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Rcse.apk", "com.mediatek.rcse.plugin.contacts.RCSeContactsCustomizationFactory", "com.mediatek.rcs", "OP06"));
        sWwopRcsFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Rcse.apk", "com.mediatek.rcse.plugin.contacts.RCSeContactsCustomizationFactory", "com.mediatek.rcs", "OP07"));
        sWwopRcsFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Rcse.apk", "com.mediatek.rcse.plugin.contacts.RCSeContactsCustomizationFactory", "com.mediatek.rcs", "OP08"));
    }

    public static synchronized ContactsCustomizationFactoryBase getFactory(Context context) {
        if (sFactory == null) {
            sFactory = (ContactsCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sFactoryInfoList);
            if (sFactory == null) {
                sFactory = new ContactsCustomizationFactoryBase();
            }
        }
        return sFactory;
    }

    public static synchronized ContactsCustomizationFactoryBase getOp01Factory(Context context) {
        return getFactory(context);
    }

    public static synchronized ContactsCustomizationFactoryBase getContactsPresenceFactory(Context context) {
        return getFactory(context);
    }

    public static synchronized ContactsCustomizationFactoryBase getContactsPickerFactory(Context context) {
        return getFactory(context);
    }

    public static synchronized ContactsCustomizationFactoryBase getRcsFactory(Context context) {
        if (sRcsFactory == null) {
            sRcsFactory = (ContactsCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sRcsInfoList);
            if (sRcsFactory == null) {
                sRcsFactory = new ContactsCustomizationFactoryBase();
            }
        }
        return sRcsFactory;
    }

    public static synchronized ContactsCustomizationFactoryBase getRcsRichUiFactory(Context context) {
        if (sRcsRichUiFactory == null) {
            sRcsRichUiFactory = (ContactsCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sRcsRichUiInfoList);
            if (sRcsRichUiFactory == null) {
                sRcsRichUiFactory = new ContactsCustomizationFactoryBase();
            }
        }
        return sRcsRichUiFactory;
    }

    public static synchronized ContactsCustomizationFactoryBase getWWOPRcsFactory(Context context) {
        if (sWwopRcsFactory == null) {
            sWwopRcsFactory = (ContactsCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sWwopRcsFactoryInfoList);
            if (sWwopRcsFactory == null) {
                sWwopRcsFactory = new ContactsCustomizationFactoryBase();
            }
        }
        return sWwopRcsFactory;
    }

    public static synchronized void resetCustomizationFactory() {
        sFactory = null;
        sRcsFactory = null;
        sRcsRichUiFactory = null;
        sWwopRcsFactory = null;
    }
}
