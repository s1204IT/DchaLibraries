package com.mediatek.contacts.group;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.simservice.SimProcessorService;
import com.mediatek.contacts.simservice.SimServiceUtils;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;

public class SimGroupUtils {
    private static SparseIntArray mSubIdError = new SparseIntArray();
    private static Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static void showToast(final int i) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsApplicationEx.getContactsApplication(), i, 1).show();
            }
        });
    }

    public static boolean deleteGroupInIcc(Context context, Intent intent, long j) {
        String stringExtra = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        int i = -1;
        int intExtra = intent.getIntExtra("subId", -1);
        if (intExtra <= 0 || TextUtils.isEmpty(stringExtra)) {
            Log.w("GroupsUtils", "[deleteGroupInIcc] subId:" + intExtra + ",groupLabel:" + Log.anonymize(stringExtra) + " have errors");
            return false;
        }
        try {
            int iHasExistGroup = ContactsGroupUtils.USIMGroup.hasExistGroup(intExtra, stringExtra);
            Log.d("GroupsUtils", "[deleteGroupInIcc]ugrpId:" + iHasExistGroup);
            i = iHasExistGroup;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (i > 0) {
            Uri uriBuild = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_GROUP_URI, j).buildUpon().appendQueryParameter("query_with_group_id", "true").build();
            Cursor cursorQuery = context.getContentResolver().query(uriBuild, new String[]{"_id", "index_in_sim"}, "indicate_phone_or_sim_contact = " + intExtra, null, null);
            StringBuilder sb = new StringBuilder();
            sb.append("[deleteGroupInIcc]groupUri:");
            sb.append(uriBuild);
            sb.append(". simId:");
            sb.append(intExtra);
            sb.append("|member count:");
            sb.append(cursorQuery == null ? "null" : Integer.valueOf(cursorQuery.getCount()));
            Log.d("GroupsUtils", sb.toString());
            while (cursorQuery != null) {
                try {
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                    int i2 = cursorQuery.getInt(1);
                    Log.d("GroupsUtils", "[deleteGroupInIcc]subId:" + intExtra + "ugrpId:" + i + "|simIndex:" + i2 + "|Result:" + ContactsGroupUtils.USIMGroup.deleteUSIMGroupMember(intExtra, i2, i) + " | contactid : " + cursorQuery.getLong(0));
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            int iDeleteUSIMGroup = ContactsGroupUtils.USIMGroup.deleteUSIMGroup(intExtra, stringExtra);
            Log.d("GroupsUtils", "[deleteGroupInIcc]error:" + iDeleteUSIMGroup);
            if (iDeleteUSIMGroup != 0) {
                showToast(R.string.delete_group_failure);
                return false;
            }
        }
        return true;
    }

    public static int updateGroupToIcc(Context context, Intent intent, Intent intent2) {
        int intExtra = intent.getIntExtra("subId", -1);
        String stringExtra = intent.getStringExtra("originalGroupName");
        String stringExtra2 = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        if (intExtra < 0) {
            Log.w("GroupsUtils", "[updateGroupToIcc] subId is error.subId:" + intExtra);
            return -1;
        }
        Log.d("GroupsUtils", "[updateGroupToIcc]groupName:" + Log.anonymize(stringExtra2) + "|originalName:" + Log.anonymize(stringExtra) + " |subId:" + intExtra);
        try {
            return ContactsGroupUtils.USIMGroup.syncUSIMGroupUpdate(intExtra, stringExtra, stringExtra2);
        } catch (RemoteException e) {
            Log.e("GroupsUtils", "[updateGroupToIcc]e : " + e);
            return -1;
        } catch (ContactsGroupUtils.USIMGroupException e2) {
            Log.e("GroupsUtils", "[updateGroupToIcc] catched USIMGroupException. ErrorType: " + e2.getErrorType());
            mSubIdError.put(e2.getErrorSubId(), e2.getErrorType());
            checkAllSlotErrors(intent2);
            return -1;
        }
    }

    public static int createGroupToIcc(Context context, Intent intent, Intent intent2) {
        String stringExtra = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        int intExtra = intent.getIntExtra("subId", -1);
        if (intExtra <= 0) {
            Log.w("GroupsUtils", "[createGroupToIcc] subId error..subId:" + intExtra);
            return -1;
        }
        try {
            return ContactsGroupUtils.USIMGroup.syncUSIMGroupNewIfMissing(intExtra, stringExtra);
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        } catch (ContactsGroupUtils.USIMGroupException e2) {
            Log.w("GroupsUtils", "[createGroupToIcc] create group fail type:" + e2.getErrorType() + ",fail subId:" + e2.getErrorSubId());
            mSubIdError.put(e2.getErrorSubId(), e2.getErrorType());
            checkAllSlotErrors(intent2);
            if (e2.getErrorType() == 1) {
                intent.putExtra(ContactSaveService.EXTRA_SAVE_MODE, 1);
            }
            return -1;
        }
    }

    public static int createGroupToIcc(String str, int i) {
        if (i <= 0) {
            Log.w("GroupsUtils", "[createGroupToIcc] subId error..subId:" + i);
            return -1;
        }
        try {
            return ContactsGroupUtils.USIMGroup.syncUSIMGroupNewIfMissing(i, str);
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        } catch (ContactsGroupUtils.USIMGroupException e2) {
            Log.w("GroupsUtils", "[createGroupToIcc] create group fail type:" + e2.getErrorType() + ",fail subId:" + e2.getErrorSubId());
            mSubIdError.put(e2.getErrorSubId(), e2.getErrorType());
            checkAllSlotErrors(null);
            return -1;
        }
    }

    public static boolean checkGroupNameExist(Context context, String str, String str2, String str3) {
        boolean z;
        if (TextUtils.isEmpty(str)) {
            showToastInt(R.string.name_needed);
            return false;
        }
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, new String[]{"_id"}, "title=? AND account_name =? AND account_type=? AND deleted=0", new String[]{str, str2, str3}, null);
        if (cursorQuery != null) {
            z = cursorQuery.getCount() > 0;
            cursorQuery.close();
        } else {
            z = false;
        }
        if (!z) {
            return true;
        }
        showToastInt(R.string.group_name_exists);
        return false;
    }

    public static void checkAllSlotErrors(Intent intent) {
        for (int i = 0; i < mSubIdError.size(); i++) {
            if (showMoveUSIMGroupErrorToast(mSubIdError.valueAt(i), mSubIdError.keyAt(i)) && intent != null) {
                intent.putExtra("haveToastDone", true);
            }
        }
        mSubIdError.clear();
    }

    public static boolean showMoveUSIMGroupErrorToast(int i, int i2) {
        String string;
        Log.d("GroupsUtils", "[showMoveUSIMGroupErrorToast]errCode:" + i + "|subId:" + i2);
        if (i == 3) {
            string = ContactsApplicationEx.getContactsApplication().getString(R.string.save_group_fail);
        } else {
            string = ContactsApplicationEx.getContactsApplication().getString(ContactsGroupUtils.USIMGroupException.getErrorToastId(i));
        }
        if (string != null) {
            Log.d("GroupsUtils", "[showMoveUSIMGroupErrorToast]toastMsg:" + string);
            showToastString(string);
            return true;
        }
        return false;
    }

    private static void showToastString(final String str) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsApplicationEx.getContactsApplication(), str, 1).show();
            }
        });
    }

    private static void showToastInt(final int i) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsApplicationEx.getContactsApplication(), i, 1).show();
            }
        });
    }

    public static Intent createNewGroupIntentForIcc(Context context, AccountWithDataSet accountWithDataSet, String str, long[] jArr, Class<? extends Activity> cls, String str2, int[] iArr, int i) {
        Log.d("GroupsUtils", "[CreateNewGroupIntentForIcc]");
        Intent intent = new Intent(context, (Class<?>) SimProcessorService.class);
        intent.putExtra("work_type", 3);
        intent.putExtra("subscription_key", i);
        intent.setAction("createGroup");
        intent.putExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE, accountWithDataSet.type);
        intent.putExtra(ContactSaveService.EXTRA_ACCOUNT_NAME, accountWithDataSet.name);
        intent.putExtra(ContactSaveService.EXTRA_DATA_SET, accountWithDataSet.dataSet);
        intent.putExtra(ContactSaveService.EXTRA_GROUP_LABEL, str);
        intent.putExtra(ContactSaveService.EXTRA_RAW_CONTACTS_TO_ADD, jArr);
        intent.putExtra("simIndexArray", iArr);
        intent.putExtra("subId", i);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str2);
        intent2.putExtra("subId", i);
        intent2.putExtra("addGroupName", str);
        intent.putExtra(ContactSaveService.EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    public static Intent createGroupUpdateIntentForIcc(Context context, long j, String str, long[] jArr, long[] jArr2, Class<? extends Activity> cls, String str2, String str3, int i, int[] iArr, int[] iArr2, AccountWithDataSet accountWithDataSet) {
        Log.d("GroupsUtils", "[createGroupUpdateIntentForIcc]");
        Intent intent = new Intent(context, (Class<?>) SimProcessorService.class);
        intent.putExtra("work_type", 3);
        intent.putExtra("subscription_key", i);
        intent.setAction("updateGroup");
        intent.putExtra(ContactSaveService.EXTRA_GROUP_ID, j);
        intent.putExtra(ContactSaveService.EXTRA_GROUP_LABEL, str);
        intent.putExtra(ContactSaveService.EXTRA_RAW_CONTACTS_TO_ADD, jArr);
        intent.putExtra(ContactSaveService.EXTRA_RAW_CONTACTS_TO_REMOVE, jArr2);
        intent.putExtra("subId", i);
        intent.putExtra("simIndexToAdd", iArr);
        intent.putExtra("simIndexToRemove", iArr2);
        intent.putExtra("originalGroupName", str3);
        intent.putExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE, accountWithDataSet.type);
        intent.putExtra(ContactSaveService.EXTRA_ACCOUNT_NAME, accountWithDataSet.name);
        intent.putExtra(ContactSaveService.EXTRA_DATA_SET, accountWithDataSet.dataSet);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str2);
        intent2.putExtra("subId", i);
        intent.putExtra(ContactSaveService.EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    public static Intent createGroupDeletionIntentForIcc(Context context, long j, int i, String str) {
        Log.d("GroupsUtils", "createGroupDeletionIntentForIcc");
        Intent intent = new Intent(context, (Class<?>) SimProcessorService.class);
        intent.putExtra("work_type", 3);
        intent.putExtra("subscription_key", i);
        intent.setAction("deleteGroup");
        intent.putExtra(ContactSaveService.EXTRA_GROUP_ID, j);
        intent.putExtra("subId", i);
        intent.putExtra(ContactSaveService.EXTRA_GROUP_LABEL, str);
        return intent;
    }

    public static Intent createUndoIntentForIcc(Context context, Intent intent, int i) {
        Log.d("GroupsUtils", "createUndoIntentForIcc");
        Intent intent2 = new Intent(context, (Class<?>) SimProcessorService.class);
        intent2.putExtra("work_type", 3);
        intent2.setAction(ContactSaveService.ACTION_UNDO);
        intent2.putExtra("subscription_key", i);
        intent2.putExtras(intent);
        return intent2;
    }

    public static Intent createGroupRenameIntentForIcc(Context context, long j, String str, Class<? extends Activity> cls, String str2, String str3, int i) {
        Log.d("GroupsUtils", "createGroupRenameIntentForIcc");
        Intent intent = new Intent(context, (Class<?>) SimProcessorService.class);
        intent.putExtra("work_type", 3);
        intent.setAction(ContactSaveService.ACTION_RENAME_GROUP);
        intent.putExtra("subId", i);
        intent.putExtra(ContactSaveService.EXTRA_GROUP_ID, j);
        intent.putExtra("originalGroupName", str3);
        intent.putExtra(ContactSaveService.EXTRA_GROUP_LABEL, str);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str2);
        intent.putExtra(ContactSaveService.EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    public static boolean checkServiceState(boolean z, int i, Context context) {
        if (context == null) {
            Log.e("GroupsUtils", "[checkServiceState]ignore due to context is null");
            return false;
        }
        if (i > 0 && SimServiceUtils.isServiceRunning(context, i)) {
            if (z) {
                Toast.makeText(context, R.string.msg_loading_sim_contacts_toast, 0).show();
            }
            return false;
        }
        if (ContactSaveService.isGroupTransactionProcessing()) {
            if (z) {
                MtkToast.toast(context, R.string.phone_book_busy);
            }
            return false;
        }
        Log.d("GroupsUtils", "[checkServiceState] service is idle now, subId=" + i);
        return true;
    }
}
