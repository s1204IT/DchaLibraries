package com.android.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AdnLoadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.d("AdnLoadReceiver", "ACTION_BOOT_COMPLETED");
            new Thread() {
                @Override
                public void run() {
                    ContentValues cv = new ContentValues();
                    cv.put("account_name", "Phone");
                    cv.put("account_type", "Phone");
                    cv.put("ungrouped_visible", (Boolean) true);
                    context.getContentResolver().insert(ContactsContract.Settings.CONTENT_URI, cv);
                    TelephonyManager telManager = TelephonyManager.from(context);
                    if (!telManager.hasIccCard(0)) {
                        Log.d("AdnLoadReceiver", "No SIM card, Clear SIM Groups and SIM Contacts");
                        Intent serviceIntent = new Intent(context, (Class<?>) SimPhonebookService.class);
                        serviceIntent.setAction("cleanSimPhonebook");
                        context.startService(serviceIntent);
                    }
                    if (telManager.isMultiSimEnabled() && !telManager.hasIccCard(1)) {
                        Log.d("AdnLoadReceiver", "No SIM card2, Clear SIM Groups and SIM Contacts2");
                        Intent serviceIntent2 = new Intent(context, (Class<?>) SimPhonebookService2.class);
                        serviceIntent2.setAction("cleanSimPhonebook");
                        context.startService(serviceIntent2);
                    }
                }
            }.start();
            return;
        }
        if (intent.getAction().equals("android.intent.action.CP_PHONEBOOK_INITED")) {
            int slotId = intent.getIntExtra("slot", 0);
            Log.d("AdnLoadReceiver", "CP_PHONEBOOK_INITED: " + slotId);
            long timeSimReady = System.currentTimeMillis();
            Log.d("AdnLoadReceiver", "[+]timeCPPhonebookInited:" + timeSimReady);
            startLoadAdn(context, slotId);
            return;
        }
        if (intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED") && context.getResources().getBoolean(android.R.^attr-private.dotColor)) {
            int slotId2 = intent.getIntExtra("slot", 0);
            Log.d("AdnLoadReceiver", "receive ACTION_SIM_STATE_CHANGED: " + slotId2);
            TelephonyManager tm = TelephonyManager.from(context);
            if (tm != null && 1 == tm.getSimState(slotId2)) {
                Log.d("AdnLoadReceiver", "Swap card, Clear SIM Groups and SIM Contacts");
                startSwapAdn(context, slotId2);
            }
        }
    }

    private void startLoadAdn(Context context, int slotId) {
        Intent serviceIntent;
        if (1 == slotId) {
            serviceIntent = new Intent(context, (Class<?>) SimPhonebookService2.class);
        } else {
            serviceIntent = new Intent(context, (Class<?>) SimPhonebookService.class);
        }
        serviceIntent.setAction("loadSimPhonebook");
        Log.d("AdnLoadReceiver", "SIM ready, start load ADN: " + slotId);
        context.startService(serviceIntent);
    }

    private void startSwapAdn(Context context, int slotId) {
        Intent serviceIntent;
        ContentValues cv = new ContentValues();
        cv.put("account_name", "Phone");
        cv.put("account_type", "Phone");
        cv.put("ungrouped_visible", (Boolean) true);
        context.getContentResolver().insert(ContactsContract.Settings.CONTENT_URI, cv);
        TelephonyManager telManager = TelephonyManager.from(context);
        if (telManager.isMultiSimEnabled() && 1 == slotId) {
            serviceIntent = new Intent(context, (Class<?>) SimPhonebookService2.class);
        } else {
            serviceIntent = new Intent(context, (Class<?>) SimPhonebookService.class);
        }
        serviceIntent.setAction("swapSimPhonebook");
        context.startService(serviceIntent);
    }
}
