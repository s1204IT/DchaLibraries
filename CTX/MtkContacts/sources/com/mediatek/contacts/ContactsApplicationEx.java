package com.mediatek.contacts;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.vcard.VCardService;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.Log;

public class ContactsApplicationEx {
    private static String TAG = "ContactsApplicationEx";
    private static ContactsApplication sContactsApplication = null;
    private static BroadcastReceiver sAppExReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(ContactsApplicationEx.TAG, "[onReceive] Received Intent:" + intent);
            String action = intent.getAction();
            if (!action.equals("mediatek.intent.action.PHB_STATE_CHANGED")) {
                if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
                    ExtensionManager.resetExtensions();
                }
            } else {
                boolean booleanExtra = intent.getBooleanExtra("ready", false);
                int intExtra = intent.getIntExtra("subscription", -1000);
                if (RequestPermissionsActivity.hasBasicPermissions(context)) {
                    PhbInfoUtils.refreshActiveUsimPhbInfoMap(Boolean.valueOf(booleanExtra), Integer.valueOf(intExtra));
                }
            }
        }
    };

    public static void onCreateEx(ContactsApplication contactsApplication) {
        Log.i(TAG, "[onCreateEx]...");
        sContactsApplication = contactsApplication;
        ExtensionManager.registerApplicationContext(contactsApplication);
        GlobalEnv.setApplicationContext(contactsApplication);
        GlobalEnv.setSimAasEditor();
        GlobalEnv.setSimSneEditor();
        ((NotificationManager) contactsApplication.getSystemService("notification")).cancelAll();
        PhbInfoUtils.clearActiveUsimPhbInfoMap();
        IntentFilter intentFilter = new IntentFilter("mediatek.intent.action.PHB_STATE_CHANGED");
        intentFilter.addAction("com.mediatek.common.carrierexpress.operator_config_changed");
        contactsApplication.registerReceiver(sAppExReceiver, intentFilter);
    }

    public static boolean isContactsApplicationBusy() {
        boolean zIsProcessing = MultiChoiceService.isProcessing(2);
        boolean zIsProcessing2 = MultiChoiceService.isProcessing(1);
        boolean zIsProcessing3 = VCardService.isProcessing(1);
        boolean zIsGroupTransactionProcessing = ContactSaveService.isGroupTransactionProcessing();
        Log.i(TAG, "[isContactsApplicationBusy] multi-del: " + zIsProcessing + ", multi-copy: " + zIsProcessing2 + ", vcard: " + zIsProcessing3 + ",group-trans: " + zIsGroupTransactionProcessing);
        return zIsProcessing || zIsProcessing2 || zIsProcessing3 || zIsGroupTransactionProcessing;
    }

    public static ContactsApplication getContactsApplication() {
        return sContactsApplication;
    }
}
