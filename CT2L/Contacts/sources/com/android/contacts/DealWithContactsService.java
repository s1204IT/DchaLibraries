package com.android.contacts;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.contacts.activities.CancelActivity;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DealWithContactsService extends IntentService {
    private boolean mCanceled;
    private String mDisplaySimId;
    private int mMode;
    private NotificationManager mNotificationManager;
    private int mSlotId;
    private TelephonyManager mTelephonyManager;

    public DealWithContactsService() {
        super("DealWithContactsService");
        this.mCanceled = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.hasExtra("slot")) {
            this.mSlotId = intent.getIntExtra("slot", -1);
        } else {
            this.mSlotId = 0;
        }
        if (this.mTelephonyManager != null && this.mTelephonyManager.isMultiSimEnabled()) {
            this.mDisplaySimId = this.mSlotId == 0 ? "SIM1" : "SIM2";
        } else {
            this.mDisplaySimId = "SIM";
        }
        if ("com.android.contacts.ACTION_DELETE_CONTACTS".equals(intent.getAction())) {
            this.mMode = 2;
            deleteContacts(intent);
        } else if ("com.android.contacts.ACTION_EXPORT_TO_SIM".equals(intent.getAction())) {
            this.mMode = 1;
            exportToSIM(intent, this.mSlotId);
        } else if ("com.android.contacts.ACTION_IMPORT_FROM_SIM".equals(intent.getAction())) {
            this.mMode = 0;
            importFromSIM(intent);
        }
    }

    private void exportToSIM(Intent intent, int slotId) {
        Serializable data = intent.getSerializableExtra("android.contacts.extra.DATA");
        if (data != null) {
            HashMap<Integer, MARK> dataMap = (HashMap) data;
            Intent intentProgress = new Intent("com.android.contacts.PROGRESS");
            intentProgress.putExtra("slot", slotId);
            try {
                Iterator<Integer> it = dataMap.keySet().iterator();
                int sTotalCount = dataMap.size();
                int sSimErrCount = 0;
                int index = 0;
                Log.d("DealWithContactsService", " ExportAllPhoneContactsThread IOMap = " + dataMap);
                while (!this.mCanceled && it.hasNext()) {
                    Integer key = it.next();
                    if (dataMap.get(key).checked) {
                        if (!actuallyExportOnePhoneContact(dataMap.get(key), slotId)) {
                            sSimErrCount++;
                        } else {
                            index++;
                        }
                    }
                    intentProgress.putExtra("android.contacts.extra.PROGRESS", 1);
                    sendBroadcast(intentProgress);
                    doProgressNotification(dataMap.size(), index);
                }
                if (!this.mCanceled && sSimErrCount > 0) {
                    Intent intentExportError = new Intent("com.android.contacts.EXPORT_ERROR_OCCUR");
                    intentExportError.putExtra("android.contacts.extra.TOTAL_COUNT", sTotalCount);
                    intentExportError.putExtra("android.contacts.extra.SIM_ERROR_COUNT", sSimErrCount);
                    sendBroadcast(intentExportError);
                }
                if (sSimErrCount == 0) {
                    Intent intentDismissDialog = new Intent("com.android.contacts.DISMISS_DIALOG");
                    sendBroadcast(intentDismissDialog);
                }
                if (this.mCanceled) {
                    doCancelNotification();
                } else {
                    doFinishNotification();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean actuallyExportOnePhoneContact(MARK mark, int slotId) {
        long rawId = mark.raw_id;
        SimPhoneBookCommonUtil.AdnRecord adnRec = SimPhoneBookCommonUtil.getSimAdnViaRawId(getContentResolver(), rawId, SimPhoneBookCommonUtil.GET_ADN_MODE2);
        boolean ret = true;
        try {
            if (adnRec.isEmpty()) {
                return true;
            }
            try {
                int[] subs = SubscriptionManager.getSubId(slotId);
                if (1 == this.mTelephonyManager.getUiccAppType(subs[0])) {
                    for (int i = 0; i < adnRec.phones.size(); i++) {
                        if (!SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), "", "", "", "", "", null, adnRec.name, adnRec.phones.get(i), "", "", "", null)) {
                            ret = false;
                        } else {
                            ContentResolver resolver = getContentResolver();
                            String simAccountName = SimAccountType.getSimAccountName(slotId);
                            String simAccountType = SimAccountType.getSimAccountType(slotId);
                            ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
                            resolver.applyBatch("com.android.contacts", SimPhoneBookCommonUtil.setUpOperationListSim2Phone(resolver, operationList, simAccountName, simAccountType, adnRec.name, adnRec.phones.get(i), new String[]{"", ""}, "", "", null));
                        }
                    }
                } else {
                    int diviNum = adnRec.phones.size() > adnRec.emails.size() ? adnRec.phones.size() : adnRec.emails.size();
                    int i2 = 0;
                    while (i2 < diviNum) {
                        String phoneNum1 = i2 < adnRec.phones.size() ? adnRec.phones.get(i2) : "";
                        String phoneNum2 = i2 + 1 < adnRec.phones.size() ? adnRec.phones.get(i2 + 1) : "";
                        String email1 = i2 < adnRec.emails.size() ? adnRec.emails.get(i2) : "";
                        String email2 = i2 < adnRec.emails.size() ? adnRec.emails.get(i2 + 1) : "";
                        String emailsString = email2 == "" ? email1 : email1 + "," + email2;
                        if (!Telephony.Mms.isEmailAddress(email1) || !Telephony.Mms.isEmailAddress(email2)) {
                            Log.v("DealWithContactsService", "newEmail Pattern mismatch");
                        }
                        if (!SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), "", "", "", "", "", null, adnRec.name, phoneNum1, emailsString, phoneNum2, adnRec.name_sne, null)) {
                            ret = false;
                        } else {
                            ContentResolver resolver2 = getContentResolver();
                            String simAccountName2 = SimAccountType.getSimAccountName(slotId);
                            String simAccountType2 = SimAccountType.getSimAccountType(slotId);
                            ArrayList<ContentProviderOperation> operationList2 = new ArrayList<>();
                            resolver2.applyBatch("com.android.contacts", SimPhoneBookCommonUtil.setUpOperationListSim2Phone(resolver2, operationList2, simAccountName2, simAccountType2, adnRec.name, phoneNum1, new String[]{email1, email2}, phoneNum2, adnRec.name_sne, null));
                        }
                        i2 += 2;
                    }
                }
                return ret;
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        } catch (Throwable th) {
            return true;
        }
    }

    private void importFromSIM(Intent intent) {
        Serializable data = intent.getSerializableExtra("android.contacts.extra.DATA");
        if (data != null) {
            HashMap<Integer, MARK> dataMap = (HashMap) data;
            String accountName = intent.getStringExtra("android.contacts.extra.ACCOUNT_NAME");
            String accountType = intent.getStringExtra("android.contacts.extra.ACCOUNT_TYPE");
            Log.d("DealWithContactsService", "importFromSIM IOMap= " + dataMap + " accountName= " + accountName + " accountType= " + accountType);
            Intent intentProgress = new Intent("com.android.contacts.PROGRESS");
            intentProgress.putExtra("slot", this.mSlotId);
            int index = 0;
            try {
                Iterator<Integer> it = dataMap.keySet().iterator();
                while (!this.mCanceled && it.hasNext()) {
                    Integer key = it.next();
                    if (dataMap.get(key).checked) {
                        actuallyImportOneSimContact(dataMap.get(key), accountName, accountType);
                        index++;
                    }
                    intentProgress.putExtra("android.contacts.extra.PROGRESS", 1);
                    sendBroadcast(intentProgress);
                    doProgressNotification(dataMap.size(), index);
                }
                Intent intentDismissDialog = new Intent("com.android.contacts.DISMISS_DIALOG");
                sendBroadcast(intentDismissDialog);
                if (this.mCanceled) {
                    doCancelNotification();
                } else {
                    doFinishNotification();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void actuallyImportOneSimContact(MARK mark, String accountName, String accountType) {
        try {
            long rawId = mark.raw_id;
            ContentResolver resolver = getContentResolver();
            SimPhoneBookCommonUtil.AdnRecord adn = SimPhoneBookCommonUtil.getSimAdnViaRawId(resolver, rawId, SimPhoneBookCommonUtil.GET_ADN_MODE1);
            ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
            resolver.applyBatch("com.android.contacts", SimPhoneBookCommonUtil.setUpOperationListSim2Phone(resolver, operationList, accountName, accountType, adn.name, adn.phone, new String[]{adn.email, adn.email2}, adn.phone2, adn.name_sne, null));
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (RemoteException e2) {
            e2.printStackTrace();
        }
    }

    private void deleteContacts(Intent intent) {
        Serializable data = intent.getSerializableExtra("android.contacts.extra.DATA");
        if (data != null) {
            HashMap<Integer, MARK> dataMap = (HashMap) data;
            Log.d("DealWithContactsService", "deleteContacts DMap= " + dataMap);
            Intent intentProgress = new Intent("com.android.contacts.PROGRESS");
            intentProgress.putExtra("slot", this.mSlotId);
            try {
                ArrayList<MARK> marks = new ArrayList<>();
                Iterator<Integer> it = dataMap.keySet().iterator();
                int sTotalCount = dataMap.size();
                int sSimErrCount = 0;
                int index = 0;
                while (!this.mCanceled && it.hasNext()) {
                    Integer key = it.next();
                    if (dataMap.get(key).checked) {
                        marks.add(dataMap.get(key));
                        index++;
                    }
                    if (marks.size() >= 20) {
                        actuallyDeletePhoneContacts(marks);
                        marks.clear();
                        intentProgress.putExtra("android.contacts.extra.PROGRESS", 20);
                        sendBroadcast(intentProgress);
                        doProgressNotification(sTotalCount, index);
                    }
                }
                int size = marks.size();
                if (!this.mCanceled && size > 0) {
                    sSimErrCount = actuallyDeletePhoneContacts(marks);
                    intentProgress.putExtra("android.contacts.extra.PROGRESS", size);
                    sendBroadcast(intentProgress);
                    doProgressNotification(sTotalCount, index);
                }
                if (!this.mCanceled && sSimErrCount > 0) {
                    Intent intentDeleteError = new Intent("com.android.contacts.DELETE_ERROR_OCCUR");
                    intentDeleteError.putExtra("android.contacts.extra.TOTAL_COUNT", sTotalCount);
                    intentDeleteError.putExtra("android.contacts.extra.SIM_ERROR_COUNT", sSimErrCount);
                    sendBroadcast(intentDeleteError);
                }
                if (sSimErrCount == 0) {
                    Intent intentDismissDialog = new Intent("com.android.contacts.DISMISS_DIALOG");
                    intentDismissDialog.putExtra("android.contacts.extra.PROGRESS", size);
                    sendBroadcast(intentDismissDialog);
                }
                if (this.mCanceled) {
                    doCancelNotification();
                } else {
                    doFinishNotification();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int actuallyDeletePhoneContacts(ArrayList<MARK> marks) {
        String rawIds = "";
        String contactIds = "";
        int errCount = 0;
        for (MARK mark : marks) {
            if (rawIds.length() > 0) {
                rawIds = rawIds + ",";
                contactIds = contactIds + ",";
            }
            rawIds = rawIds + mark.raw_id;
            contactIds = contactIds + mark.contact_id;
        }
        Cursor rawCursor = getContentResolver().query(getSimUri(0), new String[]{"_id"}, "_id IN (" + rawIds + ") ", null, null);
        if (rawCursor == null) {
            return 0;
        }
        while (!this.mCanceled && rawCursor.moveToNext()) {
            if (!actuallyDeleteSimContact(0, rawCursor.getLong(0))) {
                errCount++;
            }
        }
        rawCursor.close();
        if (this.mTelephonyManager != null && this.mTelephonyManager.isMultiSimEnabled()) {
            Cursor rawCursor2 = getContentResolver().query(getSimUri(1), new String[]{"_id"}, "_id IN (" + rawIds + ") ", null, null);
            if (rawCursor2 != null) {
                while (rawCursor2.moveToNext()) {
                    actuallyDeleteSimContact(1, rawCursor2.getLong(0));
                }
            }
            rawCursor2.close();
        }
        if (!this.mCanceled) {
            getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, "contact_id IN (" + contactIds + ")", null);
        }
        return errCount;
    }

    private boolean actuallyDeleteSimContact(int slotId, long rawId) {
        boolean ret = true;
        if (-1 != rawId) {
            Cursor c1 = getContentResolver().query(getSimUri(slotId), new String[]{"display_name"}, "_id == ? ", new String[]{"" + rawId}, null);
            if (c1 != null && c1.moveToFirst()) {
                SimPhoneBookCommonUtil.AdnRecord adnRec = SimPhoneBookCommonUtil.getSimAdnViaRawId(getContentResolver(), rawId, SimPhoneBookCommonUtil.GET_ADN_MODE1);
                String oldGrpString = SimPhoneBookCommonUtil.getSimGrpStringViaRawId(getContentResolver(), rawId);
                String emailsString = adnRec.email + (adnRec.email2.isEmpty() ? "" : "," + adnRec.email2);
                if (!SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), adnRec.name, adnRec.phone, emailsString, adnRec.phone2, adnRec.name_sne, oldGrpString, "", "", "", "", "", "")) {
                    ret = false;
                } else {
                    getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, "_id IN (" + rawId + ")", null);
                }
            }
            if (c1 != null) {
                c1.close();
            }
        }
        return ret;
    }

    private Uri getSimUri(int slotId) {
        String simAccountName = SimAccountType.getSimAccountName(slotId);
        String simAccountType = SimAccountType.getSimAccountType(slotId);
        Uri uri = ContactsContract.RawContacts.CONTENT_URI;
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter("account_name", simAccountName);
        uriBuilder.appendQueryParameter("account_type", simAccountType);
        return uriBuilder.build();
    }

    private void doProgressNotification(int totalCount, int currentCount) {
        String ticker = null;
        String contentTitle = null;
        if (this.mMode == 0) {
            ticker = getString(R.string.importing_from_sim, new Object[]{this.mDisplaySimId});
            contentTitle = getString(R.string.importing_from_sim, new Object[]{this.mDisplaySimId});
        } else if (this.mMode == 1) {
            ticker = getString(R.string.exporting_to_sim, new Object[]{this.mDisplaySimId});
            contentTitle = getString(R.string.exporting_to_sim, new Object[]{this.mDisplaySimId});
        } else if (this.mMode == 2) {
            ticker = getString(R.string.deleting_contacts);
            contentTitle = getString(R.string.deleting_contacts);
        }
        Intent intent = new Intent(this, (Class<?>) CancelActivity.class);
        intent.putExtra("android.contacts.extra.MODE", this.mMode);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setOngoing(true).setProgress(totalCount, currentCount, totalCount == -1).setTicker(ticker).setContentTitle(contentTitle).setSmallIcon(android.R.drawable.stat_sys_warning).setContentIntent(PendingIntent.getActivity(this, 0, intent, 134217728));
        if (totalCount > 0) {
            builder.setContentText(String.valueOf((currentCount * 100) / totalCount) + "%");
        }
        Notification notification = builder.getNotification();
        this.mNotificationManager.notify("DeleteContactsServiceProgress", 1, notification);
    }

    private void doCancelNotification() {
        String ticker = null;
        String contentTitle = null;
        if (this.mMode == 0) {
            ticker = getString(R.string.cancel_import_from_sim, new Object[]{this.mDisplaySimId});
            contentTitle = getString(R.string.cancel_import_from_sim, new Object[]{this.mDisplaySimId});
        } else if (this.mMode == 1) {
            ticker = getString(R.string.cancel_export_to_sim, new Object[]{this.mDisplaySimId});
            contentTitle = getString(R.string.cancel_export_to_sim, new Object[]{this.mDisplaySimId});
        } else if (this.mMode == 2) {
            ticker = getString(R.string.cancel_delete_contacts);
            contentTitle = getString(R.string.cancel_delete_contacts);
        }
        Notification.Builder builder = new Notification.Builder(this);
        builder.setAutoCancel(true).setTicker(ticker).setContentTitle(contentTitle).setSmallIcon(android.R.drawable.stat_sys_warning).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0));
        Notification notification = builder.getNotification();
        this.mNotificationManager.notify("DeleteContactsServiceProgress", 1, notification);
    }

    private void doFinishNotification() {
        String ticker = null;
        String contentTitle = null;
        if (this.mMode == 0) {
            ticker = getString(R.string.finish_import_contacts, new Object[]{this.mDisplaySimId});
            contentTitle = getString(R.string.finish_import_contacts, new Object[]{this.mDisplaySimId});
        } else if (this.mMode == 1) {
            ticker = getString(R.string.finish_export_contacts, new Object[]{this.mDisplaySimId});
            contentTitle = getString(R.string.finish_export_contacts, new Object[]{this.mDisplaySimId});
        } else if (this.mMode == 2) {
            ticker = getString(R.string.finish_delete_contacts);
            contentTitle = getString(R.string.finish_delete_contacts);
        }
        Notification.Builder builder = new Notification.Builder(this);
        builder.setAutoCancel(true).setTicker(ticker).setContentTitle(contentTitle).setSmallIcon(android.R.drawable.stat_sys_warning).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0));
        Notification notification = builder.getNotification();
        this.mNotificationManager.notify("DeleteContactsServiceProgress", 1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mCanceled = true;
    }
}
