package com.android.contacts;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.contacts.ContactSaveService;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.common.ContactTileLoaderFactory;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimPhonebookService extends IntentService {
    private static final String[] SIM_COLUMNS = {"name", "number", "emails", "anrs", "snes", "grps"};
    private static final String[] SIM_GROUP_COLUMNS = {"id", "name"};
    private static final CopyOnWriteArrayList<ContactSaveService.Listener> sListeners = new CopyOnWriteArrayList<>();
    private Handler mMainHandler;
    protected int mSlotId;

    public static void registerListener(ContactSaveService.Listener listener) {
        if (!(listener instanceof Activity)) {
            throw new ClassCastException("Only activities can be registered to receive callback from " + ContactSaveService.class.getName());
        }
        sListeners.add(0, listener);
    }

    public static void unregisterListener(ContactSaveService.Listener listener) {
        sListeners.remove(listener);
    }

    private void deliverCallback(final Intent callbackIntent) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                SimPhonebookService.this.deliverCallbackOnUiThread(callbackIntent);
            }
        });
    }

    void deliverCallbackOnUiThread(Intent callbackIntent) {
        for (ContactSaveService.Listener listener : sListeners) {
            if (callbackIntent.getComponent().equals(((Activity) listener).getIntent().getComponent())) {
                listener.onServiceCompleted(callbackIntent);
                return;
            }
        }
    }

    public SimPhonebookService() {
        super("SimPhonebookService");
        this.mSlotId = 0;
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if ("loadSimPhonebook".equals(action)) {
            initSimPhonebook(intent);
        } else if ("cleanSimPhonebook".equals(action)) {
            cleanSimPhonebook(intent);
        } else if ("swapSimPhonebook".equals(action)) {
            swapSimPhonebook(intent);
        }
    }

    private void swapSimPhonebook(Intent intent) {
        cleanSimPhonebook(intent);
        Intent callbackIntent = new Intent(this, (Class<?>) PeopleActivity.class);
        callbackIntent.setAction("reboot");
        deliverCallback(callbackIntent);
    }

    boolean clearSimContactsOrGroups(int slotId, int type) {
        Uri uri;
        try {
            Log.d("SimPhonebookService", "clearSimContactsOrGroups(): " + slotId);
            new ContentValues();
            getContentResolver();
            new ArrayList();
            if (type == 1) {
                uri = Uri.parse(ContactsContract.RawContacts.CONTENT_URI.toString() + "?caller_is_syncadapter=true");
            } else if (type == 2) {
                uri = Uri.parse(ContactsContract.Groups.CONTENT_URI.toString() + "?caller_is_syncadapter=true");
            } else {
                return false;
            }
            String simAccountName = SimAccountType.getSimAccountName(slotId);
            String simAccountType = SimAccountType.getSimAccountType(slotId);
            Uri.Builder uriBuilder = uri.buildUpon();
            uriBuilder.appendQueryParameter("account_name", simAccountName);
            uriBuilder.appendQueryParameter("account_type", simAccountType);
            ContentProviderOperation.newDelete(uriBuilder.build());
            getContentResolver().delete(uriBuilder.build(), null, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected int getSlotId() {
        return this.mSlotId;
    }

    private boolean isAirplanMode() {
        return Settings.System.getInt(getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private void initSimPhonebook(Intent intent) {
        int slotId = getSlotId();
        Log.d("SimPhonebookService", "initSimPhonebook(): " + slotId);
        TelephonyManager tm = TelephonyManager.from(this);
        if (!isAirplanMode() && tm.hasIccCard(slotId) && SimPhoneBookCommonUtil.getPBInitCount(getSlotId()) == 0) {
            importSimContactsTask(slotId);
        }
        Log.d("SimPhonebookService", "initSimPhonebook(), end: " + slotId);
        Intent simIntent = new Intent("android.intent.action.SIM_CONTACTS_LOADED");
        sendBroadcast(simIntent);
    }

    private void cleanSimPhonebook(Intent intent) {
        int slotId = getSlotId();
        Log.d("SimPhonebookService", "cleanSimPhonebook(): " + slotId);
        clearSimContactsOrGroups(slotId, 1);
        clearSimContactsOrGroups(slotId, 2);
    }

    private void importSimContactsTask(int slotId) {
        if (SimPhoneBookCommonUtil.isSimEnabled(slotId)) {
            clearSimContactsOrGroups(slotId, 1);
            clearSimContactsOrGroups(slotId, 2);
            initSimContacts(slotId);
            AccountTypeManager mAccountTypes = AccountTypeManager.getInstance(this);
            mAccountTypes.onAccountsUpdated(null);
        }
    }

    void initSimContacts(int slotId) {
        Log.d("SimPhonebookService", "initSimContacts(): " + slotId);
        long beginSimLoaded = System.currentTimeMillis();
        Cursor data = null;
        int[] subs = SubscriptionManager.getSubId(slotId);
        if (subs == null) {
            Log.e("SimPhonebookService", "initSimContacts() get invalid subid");
        }
        Uri adnGrpUri = ContentUris.withAppendedId(ContactTileLoaderFactory.ADN_GRP_URI_SUB, subs[0]);
        Uri adnUri = ContentUris.withAppendedId(ContactTileLoaderFactory.ADN_URI_SUB, subs[0]);
        String simAccountName = SimAccountType.getSimAccountName(slotId);
        String simAccountType = SimAccountType.getSimAccountType(slotId);
        try {
            try {
                Log.v("SimPhonebookService", "getBaseContext() = [" + getBaseContext() + "]");
                Cursor data2 = getContentResolver().query(adnGrpUri, SIM_GROUP_COLUMNS, null, null, "name ASC");
                while (data2.moveToNext()) {
                    String index = data2.getString(0);
                    String name = data2.getString(1);
                    Log.v("SimPhonebookService", "group index = [" + index + "]");
                    Log.v("SimPhonebookService", "group name = [" + name + "]");
                    String label = name.isEmpty() ? "(SIM Group) [" + index + "]" : name;
                    ContentValues values = new ContentValues();
                    values.put("account_type", simAccountType);
                    values.put("account_name", simAccountName);
                    values.put("title", label);
                    values.put("group_is_read_only", (Boolean) true);
                    values.put("sourceid", index);
                    getContentResolver().insert(ContactsContract.Groups.CONTENT_URI, values);
                }
                data2.close();
                Cursor data3 = getContentResolver().query(adnUri, SIM_COLUMNS, null, null, "name ASC");
                if (data3 == null) {
                    long timeSimLoaded = System.currentTimeMillis();
                    Log.d("SimPhonebookService", "[+]timeSimLoaded:" + (timeSimLoaded - beginSimLoaded));
                    if (data3 != null) {
                        data3.close();
                        return;
                    }
                    return;
                }
                Log.d("SimPhonebookService", "loadFromEf cost: " + (System.currentTimeMillis() - beginSimLoaded));
                ContentValues cv1 = new ContentValues();
                cv1.put("account_name", simAccountName);
                cv1.put("account_type", simAccountType);
                cv1.put("ungrouped_visible", (Boolean) true);
                getContentResolver().insert(ContactsContract.Settings.CONTENT_URI, cv1);
                ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
                int previousResult = 0;
                while (data3.moveToNext()) {
                    String name2 = data3.getString(0);
                    String number = data3.getString(1);
                    String emailString = data3.getString(2);
                    String anrString = data3.getString(3);
                    String sneString = data3.getString(4);
                    String grpString = data3.getString(5);
                    String[] emails = new String[2];
                    String anr = null;
                    String sne = null;
                    Log.v("SimPhonebookService", "name = " + name2);
                    Log.v("SimPhonebookService", "number = " + number);
                    Log.v("SimPhonebookService", "emailString = " + emailString);
                    Log.v("SimPhonebookService", "anrString = " + anrString);
                    Log.v("SimPhonebookService", "sneString = " + sneString);
                    Log.v("SimPhonebookService", "grpString = " + grpString);
                    if (emailString != null && emailString.endsWith(",")) {
                        emails = emailString.substring(0, emailString.length() - 1).split(",", 2);
                    }
                    if (sneString != null && sneString.endsWith(",")) {
                        sne = sneString.substring(0, sneString.length() - 1);
                    }
                    if (anrString != null && anrString.endsWith(",")) {
                        anr = anrString.substring(0, anrString.length() - 1);
                    }
                    ContentResolver resolver = getContentResolver();
                    ArrayList<ContentProviderOperation> subList = SimPhoneBookCommonUtil.setUpOperationListSim2Phone(resolver, new ArrayList<>(), simAccountName, simAccountType, name2, number, emails, anr, sne, grpString, previousResult);
                    operationList.addAll(subList);
                    previousResult += subList.size();
                    if (operationList.size() > 100) {
                        Log.d("SimPhonebookService", "operationList size: " + operationList.size());
                        resolver.applyBatch("com.android.contacts", operationList);
                        operationList = new ArrayList<>();
                        previousResult = 0;
                        Thread.sleep(2000L);
                    }
                }
                Log.d("SimPhonebookService", "operationList size: " + operationList.size());
                if (operationList.size() > 0) {
                    getContentResolver().applyBatch("com.android.contacts", operationList);
                }
                long timeSimLoaded2 = System.currentTimeMillis();
                Log.d("SimPhonebookService", "[+]timeSimLoaded:" + (timeSimLoaded2 - beginSimLoaded));
                if (data3 != null) {
                    data3.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                long timeSimLoaded3 = System.currentTimeMillis();
                Log.d("SimPhonebookService", "[+]timeSimLoaded:" + (timeSimLoaded3 - beginSimLoaded));
                if (0 != 0) {
                    data.close();
                }
            }
        } catch (Throwable th) {
            long timeSimLoaded4 = System.currentTimeMillis();
            Log.d("SimPhonebookService", "[+]timeSimLoaded:" + (timeSimLoaded4 - beginSimLoaded));
            if (0 != 0) {
                data.close();
            }
            throw th;
        }
    }
}
