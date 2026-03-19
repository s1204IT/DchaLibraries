package com.android.contacts;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import com.android.contacts.ContactSaveService;
import com.android.contacts.activities.AppCompatTransactionSafeActivity;
import com.android.contacts.testing.InjectedServices;

public abstract class AppCompatContactsActivity extends AppCompatTransactionSafeActivity implements ContactSaveService.Listener {
    private ContentResolver mContentResolver;

    @Override
    public ContentResolver getContentResolver() {
        if (this.mContentResolver == null) {
            InjectedServices injectedServices = ContactsApplication.getInjectedServices();
            if (injectedServices != null) {
                this.mContentResolver = injectedServices.getContentResolver();
            }
            if (this.mContentResolver == null) {
                this.mContentResolver = super.getContentResolver();
            }
        }
        return this.mContentResolver;
    }

    @Override
    public SharedPreferences getSharedPreferences(String str, int i) {
        SharedPreferences sharedPreferences;
        InjectedServices injectedServices = ContactsApplication.getInjectedServices();
        if (injectedServices != null && (sharedPreferences = injectedServices.getSharedPreferences()) != null) {
            return sharedPreferences;
        }
        return super.getSharedPreferences(str, i);
    }

    @Override
    public Object getSystemService(String str) {
        Object systemService = super.getSystemService(str);
        if (systemService != null) {
            return systemService;
        }
        return getApplicationContext().getSystemService(str);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        ContactSaveService.registerListener(this);
        super.onCreate(bundle);
    }

    @Override
    protected void onDestroy() {
        ContactSaveService.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onServiceCompleted(Intent intent) {
        onNewIntent(intent);
    }

    public <T extends Fragment> T getFragment(int i) {
        T t = (T) getFragmentManager().findFragmentById(i);
        if (t == null) {
            throw new IllegalArgumentException("fragment 0x" + Integer.toHexString(i) + " doesn't exist");
        }
        return t;
    }

    public <T extends View> T getView(int i) {
        T t = (T) findViewById(i);
        if (t == null) {
            throw new IllegalArgumentException("view 0x" + Integer.toHexString(i) + " doesn't exist");
        }
        return t;
    }
}
