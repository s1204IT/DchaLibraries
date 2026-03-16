package com.android.contacts;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.testing.InjectedServices;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;

public final class ContactsApplication extends Application {
    private static InjectedServices sInjectedServices;
    private ContactPhotoManager mContactPhotoManager;

    public static void injectServices(InjectedServices services) {
        sInjectedServices = services;
    }

    public static InjectedServices getInjectedServices() {
        return sInjectedServices;
    }

    @Override
    public ContentResolver getContentResolver() {
        ContentResolver resolver;
        return (sInjectedServices == null || (resolver = sInjectedServices.getContentResolver()) == null) ? super.getContentResolver() : resolver;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences prefs;
        return (sInjectedServices == null || (prefs = sInjectedServices.getSharedPreferences()) == null) ? super.getSharedPreferences(name, mode) : prefs;
    }

    @Override
    public Object getSystemService(String name) {
        Object service;
        if (sInjectedServices == null || (service = sInjectedServices.getSystemService(name)) == null) {
            if ("contactPhotos".equals(name)) {
                if (this.mContactPhotoManager == null) {
                    this.mContactPhotoManager = ContactPhotoManager.createContactPhotoManager(this);
                    registerComponentCallbacks(this.mContactPhotoManager);
                    this.mContactPhotoManager.preloadPhotosInBackground();
                }
                Object service2 = this.mContactPhotoManager;
                return service2;
            }
            Object service3 = super.getSystemService(name);
            return service3;
        }
        return service;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "ContactsApplication.onCreate start");
        }
        if (Log.isLoggable("ContactsStrictMode", 3)) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        }
        new DelayedInitializer().execute();
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "ContactsApplication.onCreate finish");
        }
        AnalyticsUtil.initialize(this);
    }

    private class DelayedInitializer extends AsyncTask<Void, Void, Void> {
        private DelayedInitializer() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            Context context = ContactsApplication.this;
            PreferenceManager.getDefaultSharedPreferences(context);
            AccountTypeManager.getInstance(context);
            ContactsApplication.this.getContentResolver().getType(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, 1L));
            return null;
        }

        public void execute() {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }
}
