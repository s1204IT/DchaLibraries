package com.android.contacts;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import com.android.contacts.testing.InjectedServices;
import com.android.contactsbind.analytics.AnalyticsUtil;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsApplication extends Application {
    private static final boolean ENABLE_FRAGMENT_LOG = false;
    private static final boolean ENABLE_LOADER_LOG = false;
    public static final String STRICT_MODE_TAG = "ContactsStrictMode";
    private static InjectedServices sInjectedServices;
    private final ExecutorService mSingleTaskService = Executors.newSingleThreadExecutor();

    public static void injectServices(InjectedServices injectedServices) {
        sInjectedServices = injectedServices;
    }

    public static InjectedServices getInjectedServices() {
        return sInjectedServices;
    }

    @Override
    public ContentResolver getContentResolver() {
        ContentResolver contentResolver;
        if (sInjectedServices != null && (contentResolver = sInjectedServices.getContentResolver()) != null) {
            return contentResolver;
        }
        return super.getContentResolver();
    }

    @Override
    public SharedPreferences getSharedPreferences(String str, int i) {
        SharedPreferences sharedPreferences;
        if (sInjectedServices != null && (sharedPreferences = sInjectedServices.getSharedPreferences()) != null) {
            return sharedPreferences;
        }
        return super.getSharedPreferences(str, i);
    }

    @Override
    public Object getSystemService(String str) {
        Object systemService;
        if (sInjectedServices != null && (systemService = sInjectedServices.getSystemService(str)) != null) {
            return systemService;
        }
        return super.getSystemService(str);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "ContactsApplication.onCreate start");
        }
        if (Log.isLoggable(STRICT_MODE_TAG, 3)) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        }
        ContactsApplicationEx.onCreateEx(this);
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
        protected Void doInBackground(Void... voidArr) {
            PreferenceManager.getDefaultSharedPreferences(ContactsApplication.this);
            ContactsApplication.this.getContentResolver().getType(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, 1L));
            return null;
        }

        public void execute() {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }

    public static ContactsApplication getInstance() {
        return ContactsApplicationEx.getContactsApplication();
    }

    public ExecutorService getApplicationTaskService() {
        return this.mSingleTaskService;
    }
}
