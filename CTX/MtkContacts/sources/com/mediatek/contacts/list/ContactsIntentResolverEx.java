package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.contacts.util.Log;

public class ContactsIntentResolverEx extends ContactsIntentResolver {
    public ContactsIntentResolverEx(Activity activity) {
        super(activity);
    }

    @Override
    public ContactsRequest resolveIntent(Intent intent) {
        Log.i("ContactsIntentResolverEx", "[resolveIntent]intent: " + intent);
        if (ContactsIntent.contain(intent)) {
            String action = intent.getAction();
            Log.i("ContactsIntentResolverEx", "[resolveIntent]Called with action: " + action);
            ContactsRequest contactsRequest = new ContactsRequest();
            if ("mediatek.intent.action.contacts.list.PICKMULTICONTACTS".equals(action)) {
                contactsRequest.setActionCode(300);
                int intExtra = intent.getIntExtra("request_type", 0);
                Log.i("ContactsIntentResolverEx", "[resolveIntent]requestType: " + intExtra);
                if (intExtra == 1) {
                    contactsRequest.setActionCode(33554732);
                } else if (intExtra == 3) {
                    contactsRequest.setActionCode(16777516);
                }
            } else if ("mediatek.intent.action.contacts.list.PICKMULTIEMAILS".equals(action)) {
                contactsRequest.setActionCode(309);
            } else if ("mediatek.intent.action.contacts.list.PICKMULTIPHONES".equals(action)) {
                contactsRequest.setActionCode(305);
            } else if ("mediatek.intent.action.contacts.list.PICKMULTIPHONEANDEMAILS".equals(action)) {
                contactsRequest.setActionCode(306);
            } else if ("mediatek.intent.action.contacts.list.PICKMULTIDATAS".equals(action)) {
                contactsRequest.setActionCode(307);
            } else if ("mediatek.intent.action.contacts.list.PICKMULTIPHONEANDIMSANDSIPCONTACTS".equals(action)) {
                contactsRequest.setActionCode(310);
            }
            String stringExtra = intent.getStringExtra("com.android.contacts.extra.TITLE_EXTRA");
            if (stringExtra != null) {
                contactsRequest.setActivityTitle(stringExtra);
            }
            return contactsRequest;
        }
        return super.resolveIntent(intent);
    }
}
