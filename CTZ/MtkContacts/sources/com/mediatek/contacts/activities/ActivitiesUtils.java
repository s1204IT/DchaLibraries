package com.mediatek.contacts.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.telephony.PhoneNumberUtils;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.model.AccountTypeManager;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.simservice.SimEditProcessor;
import com.mediatek.contacts.util.Log;

public class ActivitiesUtils {
    public static Handler initHandler(final Activity activity) {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                String string;
                int i = message.arg1;
                Bundle data = message.getData();
                if (data != null) {
                    string = data.getString("content");
                } else {
                    string = null;
                }
                ActivitiesUtils.onShowToast(activity, string, i);
            }
        };
        SimEditProcessor.registerListener((SimEditProcessor.Listener) activity, handler);
        return handler;
    }

    public static void onShowToast(Activity activity, String str, int i) {
        Log.d("ActivitiesUtils", "[onShowToast]msg: " + str + " ,resId: " + i);
        if (str != null) {
            Toast.makeText(activity, str, 0).show();
        } else if (i != -1) {
            Toast.makeText(activity, i, 0).show();
        }
    }

    public static void setPickerFragmentAccountType(Activity activity, ContactEntryListFragment<?> contactEntryListFragment) {
        if (contactEntryListFragment instanceof ContactPickerFragment) {
            int intExtra = activity.getIntent().getIntExtra("account_type", 0);
            Log.d("ActivitiesUtils", "[setPickerFragmentAccountType]accountTypeShow:" + intExtra);
            ((ContactPickerFragment) contactEntryListFragment).setAccountType(intExtra);
        }
    }

    public static boolean checkSimNumberValid(Activity activity, String str) {
        if (str == null || PhoneNumberUtils.isGlobalPhoneNumber(str)) {
            return false;
        }
        Toast.makeText(activity.getApplicationContext(), R.string.sim_invalid_number, 0).show();
        activity.finish();
        return true;
    }

    public static boolean doImport(Context context) {
        Log.i("ActivitiesUtils", "[doImport]...");
        if (MultiChoiceService.isProcessing(2)) {
            Toast.makeText(context, R.string.contact_delete_all_tips, 0).show();
            return true;
        }
        Intent intent = new Intent(context, (Class<?>) ContactImportExportActivity.class);
        intent.putExtra("CALLING_ACTIVITY", PeopleActivity.class.getName());
        intent.putExtra("CALLING_TYPE", "Import");
        context.startActivity(intent);
        return true;
    }

    public static boolean conferenceCall(Activity activity) {
        Log.i("ActivitiesUtils", "[conferenceCall]...");
        Intent intent = new Intent();
        intent.setClassName(activity, "com.mediatek.contacts.list.ContactListMultiChoiceActivity").setAction("mediatek.intent.action.contacts.list.PICKMULTIPHONEANDIMSANDSIPCONTACTS");
        intent.putExtra("CONFERENCE_SENDER", "CONTACTS");
        activity.startActivity(intent);
        return true;
    }

    private static int getAvailableStorageCount(Activity activity) {
        StorageManager storageManager = (StorageManager) activity.getApplicationContext().getSystemService("storage");
        if (storageManager == null) {
            Log.w("ActivitiesUtils", "[getAvailableStorageCount]storageManager is null,return 0.");
            return 0;
        }
        int i = 0;
        for (StorageVolume storageVolume : storageManager.getVolumeList()) {
            if ("mounted".equals(storageManager.getVolumeState(storageVolume.getPath()))) {
                i++;
            }
        }
        Log.d("ActivitiesUtils", "[getAvailableStorageCount]storageCount = " + i);
        return i;
    }

    public static boolean showImportExportMenu(Activity activity) {
        int availableStorageCount = getAvailableStorageCount(activity);
        int size = AccountTypeManager.getInstance(activity).getAccounts(false).size();
        Log.d("ActivitiesUtils", "[showImportExportMenu]availableStorageCount = " + availableStorageCount + ",accountSize = " + size);
        return availableStorageCount != 0 || size > 1;
    }
}
