package com.android.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class ContactsIntentResolver {
    private final Activity mContext;

    public ContactsIntentResolver(Activity context) {
        this.mContext = context;
    }

    public ContactsRequest resolveIntent(Intent intent) {
        ContactsRequest request = new ContactsRequest();
        String action = intent.getAction();
        Log.i("ContactsIntentResolver", "Called with action: " + action);
        if ("com.android.contacts.action.LIST_DEFAULT".equals(action)) {
            request.setActionCode(10);
        } else if ("com.android.contacts.action.LIST_ALL_CONTACTS".equals(action)) {
            request.setActionCode(15);
        } else if ("com.android.contacts.action.LIST_CONTACTS_WITH_PHONES".equals(action)) {
            request.setActionCode(17);
        } else if ("com.android.contacts.action.LIST_STARRED".equals(action)) {
            request.setActionCode(30);
        } else if ("com.android.contacts.action.LIST_FREQUENT".equals(action)) {
            request.setActionCode(40);
        } else if ("com.android.contacts.action.LIST_STREQUENT".equals(action)) {
            request.setActionCode(50);
        } else if ("com.android.contacts.action.LIST_GROUP".equals(action)) {
            request.setActionCode(20);
        } else if ("android.intent.action.PICK".equals(action)) {
            String resolvedType = intent.resolveType(this.mContext);
            if ("vnd.android.cursor.dir/contact".equals(resolvedType)) {
                request.setActionCode(60);
            } else if ("vnd.android.cursor.dir/person".equals(resolvedType)) {
                request.setActionCode(60);
                request.setLegacyCompatibilityMode(true);
            } else if ("vnd.android.cursor.dir/phone_v2".equals(resolvedType)) {
                request.setActionCode(90);
            } else if ("vnd.android.cursor.dir/phone".equals(resolvedType)) {
                request.setActionCode(90);
                request.setLegacyCompatibilityMode(true);
            } else if ("vnd.android.cursor.dir/postal-address_v2".equals(resolvedType)) {
                request.setActionCode(100);
            } else if ("vnd.android.cursor.dir/postal-address".equals(resolvedType)) {
                request.setActionCode(100);
                request.setLegacyCompatibilityMode(true);
            } else if ("vnd.android.cursor.dir/email_v2".equals(resolvedType)) {
                request.setActionCode(105);
            }
        } else if ("android.intent.action.CREATE_SHORTCUT".equals(action)) {
            String component = intent.getComponent().getClassName();
            if (component.equals("alias.DialShortcut")) {
                request.setActionCode(120);
            } else if (component.equals("alias.MessageShortcut")) {
                request.setActionCode(130);
            } else {
                request.setActionCode(110);
            }
        } else if ("android.intent.action.GET_CONTENT".equals(action)) {
            String type = intent.getType();
            if ("vnd.android.cursor.item/contact".equals(type)) {
                request.setActionCode(70);
            } else if ("vnd.android.cursor.item/phone_v2".equals(type)) {
                request.setActionCode(90);
            } else if ("vnd.android.cursor.item/phone".equals(type)) {
                request.setActionCode(90);
                request.setLegacyCompatibilityMode(true);
            } else if ("vnd.android.cursor.item/postal-address_v2".equals(type)) {
                request.setActionCode(100);
            } else if ("vnd.android.cursor.item/postal-address".equals(type)) {
                request.setActionCode(100);
                request.setLegacyCompatibilityMode(true);
            } else if ("vnd.android.cursor.item/person".equals(type)) {
                request.setActionCode(70);
                request.setLegacyCompatibilityMode(true);
            }
        } else if ("android.intent.action.INSERT_OR_EDIT".equals(action)) {
            request.setActionCode(80);
        } else if ("android.intent.action.SEARCH".equals(action)) {
            String query = intent.getStringExtra("query");
            if (TextUtils.isEmpty(query)) {
                query = intent.getStringExtra("phone");
            }
            if (TextUtils.isEmpty(query)) {
                query = intent.getStringExtra("email");
            }
            request.setQueryString(query);
            request.setSearchMode(true);
        } else if ("android.intent.action.VIEW".equals(action)) {
            String resolvedType2 = intent.resolveType(this.mContext);
            if ("vnd.android.cursor.dir/contact".equals(resolvedType2) || "vnd.android.cursor.dir/person".equals(resolvedType2)) {
                request.setActionCode(15);
            } else {
                request.setActionCode(140);
                request.setContactUri(intent.getData());
                intent.setAction("android.intent.action.VIEW");
                intent.setData(null);
            }
        } else if ("com.android.contacts.action.FILTER_CONTACTS".equals(action)) {
            request.setActionCode(10);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                request.setQueryString(extras.getString("com.android.contacts.extra.FILTER_TEXT"));
                ContactsRequest originalRequest = (ContactsRequest) extras.get("originalRequest");
                if (originalRequest != null) {
                    request.copyFrom(originalRequest);
                }
            }
            request.setSearchMode(true);
        } else if ("android.provider.Contacts.SEARCH_SUGGESTION_CLICKED".equals(action)) {
            Uri data = intent.getData();
            request.setActionCode(140);
            request.setContactUri(data);
            intent.setAction("android.intent.action.VIEW");
            intent.setData(null);
        } else if ("com.android.contacts.action.JOIN_CONTACT".equals(action)) {
            request.setActionCode(150);
        }
        String title = intent.getStringExtra("com.android.contacts.extra.TITLE_EXTRA");
        if (title != null) {
            request.setActivityTitle(title);
        }
        return request;
    }
}
