package com.mediatek.contacts.util;

import android.content.Intent;

public final class ContactsIntent {
    public static boolean contain(Intent intent) {
        if (intent == null) {
            Log.w("ContactsIntent", "[contain]intent is null,donothing!");
            return false;
        }
        String action = intent.getAction();
        Log.d("ContactsIntent", "[contain]action is:" + action);
        if (!"mediatek.intent.action.contacts.list.PICKMULTICONTACTS".equals(action) && !"mediatek.intent.action.contacts.list.PICKMULTIEMAILS".equals(action) && !"mediatek.intent.action.contacts.list.PICKMULTIPHONES".equals(action) && !"mediatek.intent.action.contacts.list.PICKMULTIPHONEANDEMAILS".equals(action) && !"mediatek.intent.action.contacts.list.PICKMULTIDATAS".equals(action) && !"mediatek.intent.action.contacts.list.PICKMULTIPHONEANDIMSANDSIPCONTACTS".equals(action)) {
            return false;
        }
        return true;
    }
}
