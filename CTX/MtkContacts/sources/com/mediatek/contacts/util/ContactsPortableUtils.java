package com.mediatek.contacts.util;

public class ContactsPortableUtils {
    public static final boolean MTK_STORAGE_SUPPORT = isMtkStorageSupport();
    public static final boolean MTK_TELEPHONY_SUPPORT = isMtkTelephonySupport();
    public static final boolean MTK_PHONE_BOOK_SUPPORT = isMtkPhoneBookSupport();

    private static boolean isMtkStorageSupport() {
        try {
            Class.forName("com.mediatek.storage.StorageManagerEx");
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("ContactsPortableUtils", "StorageManagerEx class not found!");
            return false;
        }
    }

    private static boolean isMtkTelephonySupport() {
        try {
            Class.forName("com.mediatek.internal.telephony.IMtkTelephonyEx");
            Class.forName("com.mediatek.internal.telephony.phb.AlphaTag");
            Class.forName("com.mediatek.internal.telephony.phb.UsimGroup");
            Class.forName("com.mediatek.telephony.TelephonyManagerEx");
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("ContactsPortableUtils", "MTK telephony class not found!");
            return false;
        }
    }

    private static boolean isMtkPhoneBookSupport() {
        try {
            Class<?> cls = Class.forName("com.mediatek.internal.telephony.phb.IMtkIccPhoneBook");
            cls.getDeclaredMethod("getUsimAasList", Integer.TYPE);
            cls.getDeclaredMethod("getUsimGroups", Integer.TYPE);
            cls.getDeclaredMethod("hasSne", Integer.TYPE);
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("ContactsPortableUtils", "Android phonebook class not found!");
            return false;
        } catch (NoSuchMethodException e2) {
            Log.e("ContactsPortableUtils", "MTK phonebook api not found!");
            return false;
        }
    }
}
