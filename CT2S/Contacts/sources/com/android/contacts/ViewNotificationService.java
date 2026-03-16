package com.android.contacts;

import android.app.Service;
import android.content.Intent;
import android.content.Loader;
import android.os.IBinder;
import android.util.Log;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;

public class ViewNotificationService extends Service {
    private static final String TAG = ViewNotificationService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        ContactLoader contactLoader = new ContactLoader(this, intent.getData(), true);
        contactLoader.registerListener(0, new Loader.OnLoadCompleteListener<Contact>() {
            @Override
            public void onLoadComplete(Loader<Contact> loader, Contact data) {
                try {
                    loader.reset();
                } catch (RuntimeException e) {
                    Log.e(ViewNotificationService.TAG, "Error reseting loader", e);
                }
                try {
                    ViewNotificationService.this.stopSelfResult(startId);
                } catch (RuntimeException e2) {
                    Log.e(ViewNotificationService.TAG, "Error stopping service", e2);
                }
            }
        });
        contactLoader.startLoading();
        return 3;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
