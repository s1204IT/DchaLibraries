package com.android.contacts;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import com.android.contacts.ContactSaveService;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.testing.InjectedServices;

public abstract class ContactsActivity extends TransactionSafeActivity implements ContactSaveService.Listener {
    private ContentResolver mContentResolver;

    @Override
    public ContentResolver getContentResolver() {
        if (this.mContentResolver == null) {
            InjectedServices services = ContactsApplication.getInjectedServices();
            if (services != null) {
                this.mContentResolver = services.getContentResolver();
            }
            if (this.mContentResolver == null) {
                this.mContentResolver = super.getContentResolver();
            }
        }
        return this.mContentResolver;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences prefs;
        InjectedServices services = ContactsApplication.getInjectedServices();
        return (services == null || (prefs = services.getSharedPreferences()) == null) ? super.getSharedPreferences(name, mode) : prefs;
    }

    @Override
    public Object getSystemService(String name) {
        Object service = super.getSystemService(name);
        return service != null ? service : getApplicationContext().getSystemService(name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ContactSaveService.registerListener(this);
        SimPhonebookService.registerListener(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        ContactSaveService.unregisterListener(this);
        SimPhonebookService.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onServiceCompleted(Intent callbackIntent) {
        onNewIntent(callbackIntent);
    }

    public <T extends View> T getView(int i) {
        T t = (T) findViewById(i);
        if (t == null) {
            throw new IllegalArgumentException("view 0x" + Integer.toHexString(i) + " doesn't exist");
        }
        return t;
    }
}
