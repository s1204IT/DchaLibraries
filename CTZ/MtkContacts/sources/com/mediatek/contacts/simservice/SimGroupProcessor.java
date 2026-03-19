package com.mediatek.contacts.simservice;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import com.android.contacts.ContactSaveService;
import com.google.android.collect.Lists;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimProcessorManager;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimGroupProcessor extends SimProcessorBase {
    private static List<Listener> sListeners = new ArrayList();
    private Context mContext;
    private ContactSaveService.GroupsDao mGroupsDao;
    private Intent mIntent;
    private Uri mLookupUri;
    private Handler mMainHandler;
    private int mSubId;

    public interface Listener {
        void onSimGroupCompleted(Intent intent);
    }

    public static void registerListener(Listener listener) {
        if (!(listener instanceof Activity)) {
            throw new ClassCastException("Only activities can be registered to receive callback from " + SimProcessorService.class.getName());
        }
        Log.d("SimGroupProcessor", "[registerListener]listener added to SIMGroupProcessor: " + listener);
        sListeners.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        Log.d("SimGroupProcessor", "[unregisterListener]listener removed from SIMGroupProcessor: " + listener);
        sListeners.remove(listener);
    }

    public SimGroupProcessor(Context context, int i, Intent intent, SimProcessorManager.ProcessorCompleteListener processorCompleteListener) {
        super(intent, processorCompleteListener);
        this.mIntent = null;
        this.mLookupUri = null;
        this.mSubId = SubInfoUtils.getInvalidSubId();
        this.mContext = context;
        this.mSubId = i;
        this.mIntent = intent;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mGroupsDao = new SimGroupsDaoImpl(context, this.mSubId);
        Log.i("SimGroupProcessor", "[SIMGroupProcessor]new mSubId = " + this.mSubId);
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public void doWork() {
        if (this.mIntent == null) {
            Log.e("SimGroupProcessor", "[doWork]onHandleIntent: could not handle null intent");
            return;
        }
        String action = this.mIntent.getAction();
        Log.d("SimGroupProcessor", "[doWork]action = " + action);
        if ("createGroup".equals(action)) {
            createGroup(this.mIntent);
            return;
        }
        if (ContactSaveService.ACTION_RENAME_GROUP.equals(action)) {
            renameGroup(this.mIntent);
            return;
        }
        if ("deleteGroup".equals(action)) {
            deleteGroup(this.mIntent);
            return;
        }
        if ("updateGroup".equals(action)) {
            ContactSaveService.setGroupTransactionProcessing(true);
            updateGroup(this.mIntent);
            ContactSaveService.setGroupTransactionProcessing(false);
        } else if (ContactSaveService.ACTION_UNDO.equals(action)) {
            ContactSaveService.setGroupTransactionProcessing(true);
            undo(this.mIntent);
            ContactSaveService.setGroupTransactionProcessing(false);
        }
    }

    private void createGroup(Intent intent) {
        String stringExtra = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE);
        String stringExtra2 = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_NAME);
        String stringExtra3 = intent.getStringExtra(ContactSaveService.EXTRA_DATA_SET);
        String stringExtra4 = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        long[] longArrayExtra = intent.getLongArrayExtra(ContactSaveService.EXTRA_RAW_CONTACTS_TO_ADD);
        Log.d("SimGroupProcessor", "[createGroup] groupName:" + Log.anonymize(stringExtra4) + ", accountName:" + Log.anonymize(stringExtra2) + ", accountType:" + Log.anonymize(stringExtra));
        Intent intent2 = (Intent) intent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
        if (TextUtils.isEmpty(stringExtra4)) {
            Log.w("SimGroupProcessor", "[createGroup]Group name can't be empty!");
            intent2.putExtra(ContactSaveService.EXTRA_SAVE_MODE, 1);
            deliverCallback(intent2);
            return;
        }
        if (!SimGroupUtils.checkGroupNameExist(this.mContext, stringExtra4, stringExtra2, stringExtra)) {
            Log.w("SimGroupProcessor", "[createGroup]Group Name exist!");
            intent2.putExtra(ContactSaveService.EXTRA_SAVE_MODE, 1);
            deliverCallback(intent2);
            return;
        }
        int[] intArrayExtra = intent.getIntArrayExtra("simIndexArray");
        int iCreateGroupToIcc = -1;
        int intExtra = intent.getIntExtra("subId", -1);
        if (intExtra > 0 && (iCreateGroupToIcc = SimGroupUtils.createGroupToIcc(this.mContext, intent, intent2)) < 0) {
            Log.w("SimGroupProcessor", "[createGroup]createGroupToIcc fail!");
            intent2.putExtra(ContactSaveService.EXTRA_SAVE_MODE, 1);
            deliverCallback(intent2);
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("account_type", stringExtra);
        contentValues.put("account_name", stringExtra2);
        contentValues.put("data_set", stringExtra3);
        contentValues.put("title", stringExtra4);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Uri uriInsert = contentResolver.insert(ContactsContract.Groups.CONTENT_URI, contentValues);
        if (uriInsert == null) {
            Log.e("SimGroupProcessor", "[createGroup]Couldn't create group with label " + Log.anonymize(stringExtra4));
            return;
        }
        boolean zAddMembersToGroup = addMembersToGroup(contentResolver, longArrayExtra, ContentUris.parseId(uriInsert), intArrayExtra, intent, iCreateGroupToIcc);
        if (intExtra > 0 && !SubInfoUtils.isActiveForSubscriber(intExtra)) {
            Log.w("SimGroupProcessor", "[createGroup] Sim card is not ready");
            if (SimGroupUtils.showMoveUSIMGroupErrorToast(4, intExtra) && intent2 != null) {
                intent2.putExtra("haveToastDone", true);
            }
            deliverCallback(intent2);
            return;
        }
        contentValues.clear();
        contentValues.put("mimetype", "vnd.android.cursor.item/group_membership");
        contentValues.put("data1", Long.valueOf(ContentUris.parseId(uriInsert)));
        if (!zAddMembersToGroup) {
            uriInsert = null;
        }
        intent2.setData(uriInsert);
        intent2.putExtra("data", Lists.newArrayList(new ContentValues[]{contentValues}));
        deliverCallback(intent2);
    }

    private void renameGroup(Intent intent) {
        long longExtra = intent.getLongExtra(ContactSaveService.EXTRA_GROUP_ID, -1L);
        if (longExtra == -1) {
            Log.e("SimGroupProcessor", "[renameGroup]Invalid arguments for renameGroup request");
            return;
        }
        String stringExtra = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        Intent intent2 = (Intent) intent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
        if (TextUtils.isEmpty(stringExtra)) {
            Log.w("SimGroupProcessor", "[renameGroup]Group name can't be empty!");
            deliverCallback(intent2);
            return;
        }
        if (intent.getIntExtra("subId", -1) > 0 && SimGroupUtils.updateGroupToIcc(this.mContext, intent, intent2) < 0) {
            Log.w("SimGroupProcessor", "[renameGroup] update to Icc fail!");
            deliverCallback(intent2);
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", stringExtra);
        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, longExtra);
        Log.d("SimGroupProcessor", "[renameGroup]update to db, group uri = " + Log.anonymize(uriWithAppendedId));
        this.mContext.getContentResolver().update(uriWithAppendedId, contentValues, null, null);
        intent2.setData(uriWithAppendedId);
        deliverCallback(intent2);
    }

    private void deleteGroup(Intent intent) {
        if (ContactSaveService.sDeleteEndListener != null) {
            ContactSaveService.sDeleteEndListener.onDeleteStart();
        }
        long longExtra = intent.getLongExtra(ContactSaveService.EXTRA_GROUP_ID, -1L);
        if (longExtra == -1) {
            Log.e("SimGroupProcessor", "[deleteGroup]Invalid arguments for deleteGroup request");
        } else {
            Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, longExtra);
            int intExtra = intent.getIntExtra("subId", -1);
            Log.i("SimGroupProcessor", "[deleteGroup]get group undo data for undo function");
            Intent intent2 = new Intent(ContactSaveService.BROADCAST_GROUP_DELETED);
            Bundle bundleCaptureDeletionUndoData = this.mGroupsDao.captureDeletionUndoData(uriWithAppendedId);
            intent2.putExtra(ContactSaveService.EXTRA_UNDO_ACTION, "deleteGroup");
            intent2.putExtra(ContactSaveService.EXTRA_UNDO_DATA, bundleCaptureDeletionUndoData);
            intent2.putExtra("subId", intExtra);
            String stringExtra = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
            Log.i("SimGroupProcessor", "[deleteGroup]groupLabel:" + stringExtra + ",subId:" + intExtra);
            if (intExtra > 0 && !TextUtils.isEmpty(stringExtra) && !SimGroupUtils.deleteGroupInIcc(this.mContext, intent, longExtra)) {
                Log.w("SimGroupProcessor", "[deleteGroup] delete gorup in Icc is fail, return");
            } else {
                this.mGroupsDao.delete(uriWithAppendedId);
                LocalBroadcastManager.getInstance(this.mContext).sendBroadcast(intent2);
            }
        }
        if (ContactSaveService.sDeleteEndListener != null) {
            Log.d("SimGroupProcessor", "[deleteGroup]onDeleteEnd");
            ContactSaveService.sDeleteEndListener.onDeleteEnd();
        }
    }

    private void undo(Intent intent) {
        if ("deleteGroup".equals(intent.getStringExtra(ContactSaveService.EXTRA_UNDO_ACTION))) {
            this.mGroupsDao.undoDeletion(intent.getBundleExtra(ContactSaveService.EXTRA_UNDO_DATA));
        }
    }

    private void updateGroup(Intent intent) {
        int i;
        long longExtra = intent.getLongExtra(ContactSaveService.EXTRA_GROUP_ID, -1L);
        if (longExtra != -1) {
            Intent intent2 = (Intent) intent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
            String stringExtra = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
            String stringExtra2 = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE);
            String stringExtra3 = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_NAME);
            if (longExtra <= 0 || stringExtra == null || SimGroupUtils.checkGroupNameExist(this.mContext, stringExtra, stringExtra3, stringExtra2)) {
                int intExtra = intent.getIntExtra("subId", -1);
                if (intExtra > 0) {
                    int iUpdateGroupToIcc = SimGroupUtils.updateGroupToIcc(this.mContext, intent, intent2);
                    if (iUpdateGroupToIcc < 0) {
                        Log.w("SimGroupProcessor", "[updateGroup] groupIdInIcc fail!");
                        intent2.putExtra(ContactSaveService.EXTRA_SAVE_MODE, 1);
                        deliverCallback(intent2);
                        return;
                    }
                    i = iUpdateGroupToIcc;
                } else {
                    i = -1;
                }
                ContentResolver contentResolver = this.mContext.getContentResolver();
                Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, longExtra);
                if (stringExtra != null) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("title", stringExtra);
                    contentResolver.update(uriWithAppendedId, contentValues, null, null);
                }
                long[] longArrayExtra = intent.getLongArrayExtra(ContactSaveService.EXTRA_RAW_CONTACTS_TO_ADD);
                long[] longArrayExtra2 = intent.getLongArrayExtra(ContactSaveService.EXTRA_RAW_CONTACTS_TO_REMOVE);
                int[] intArrayExtra = intent.getIntArrayExtra("simIndexToAdd");
                boolean zRemoveMembersFromGroup = removeMembersFromGroup(contentResolver, longArrayExtra2, longExtra, intent.getIntArrayExtra("simIndexToRemove"), intExtra, i);
                boolean zAddMembersToGroup = addMembersToGroup(contentResolver, longArrayExtra, longExtra, intArrayExtra, intent, i);
                if (intExtra > 0 && !SubInfoUtils.isActiveForSubscriber(intExtra)) {
                    Log.w("SimGroupProcessor", "[updateGroup] Find sim not ready");
                    if (SimGroupUtils.showMoveUSIMGroupErrorToast(4, intExtra) && intent2 != null) {
                        intent2.putExtra("haveToastDone", true);
                    }
                    deliverCallback(intent2);
                    return;
                }
                Log.i("SimGroupProcessor", "[updateGroup]isAddSuccess:" + zAddMembersToGroup + ", isRemoveSuccess:" + zRemoveMembersFromGroup + ", groupUri:" + Log.anonymize(uriWithAppendedId));
                intent2.setData(uriWithAppendedId);
                deliverCallback(intent2);
                return;
            }
            Log.w("SimGroupProcessor", "[updateGroup] Group Name exist!");
            intent2.putExtra(ContactSaveService.EXTRA_SAVE_MODE, 1);
            deliverCallback(intent2);
            return;
        }
        Log.e("SimGroupProcessor", "[updateGroup]Invalid arguments for updateGroup request");
    }

    private static boolean addMembersToGroup(ContentResolver contentResolver, long[] jArr, long j, int[] iArr, Intent intent, int i) {
        ArrayList<ContentProviderOperation> arrayList;
        int i2;
        int i3 = 1;
        if (jArr == null) {
            Log.e("SimGroupProcessor", "[addMembersToGroup] no members to add");
            return true;
        }
        int intExtra = intent.getIntExtra("subId", -1);
        int length = jArr.length;
        boolean z = true;
        int i4 = -1;
        int i5 = 0;
        while (i5 < length) {
            long j2 = jArr[i5];
            i4 += i3;
            if (intExtra <= 0 || i < 0) {
                arrayList = new ArrayList<>();
                ContentProviderOperation.Builder builderNewAssertQuery = ContentProviderOperation.newAssertQuery(ContactsContract.Data.CONTENT_URI);
                try {
                    String[] strArr = new String[3];
                    strArr[0] = String.valueOf(j2);
                    i2 = 1;
                    try {
                        strArr[1] = "vnd.android.cursor.item/group_membership";
                        strArr[2] = String.valueOf(j);
                        builderNewAssertQuery.withSelection("raw_contact_id=? AND mimetype=? AND data1=?", strArr);
                        builderNewAssertQuery.withExpectedCount(0);
                        arrayList.add(builderNewAssertQuery.build());
                        ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                        builderNewInsert.withValue("raw_contact_id", Long.valueOf(j2));
                        builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                        builderNewInsert.withValue("data1", Long.valueOf(j));
                        arrayList.add(builderNewInsert.build());
                        if (arrayList.isEmpty()) {
                            try {
                                contentResolver.applyBatch("com.android.contacts", arrayList);
                            } catch (OperationApplicationException e) {
                                e = e;
                                Log.w("SimGroupProcessor", "[addMembersToGroup] Assert failed in adding raw contact ID " + String.valueOf(j2) + ". Already exists in group " + String.valueOf(j), e);
                                z = false;
                            } catch (RemoteException e2) {
                                e = e2;
                                Log.e("SimGroupProcessor", "[addMembersToGroup]Problem persisting user edits for raw contact ID " + String.valueOf(j2), e);
                                z = false;
                            }
                        }
                    } catch (OperationApplicationException e3) {
                        e = e3;
                    } catch (RemoteException e4) {
                        e = e4;
                    }
                } catch (OperationApplicationException e5) {
                    e = e5;
                    i2 = 1;
                } catch (RemoteException e6) {
                    e = e6;
                    i2 = 1;
                }
            } else {
                try {
                } catch (OperationApplicationException e7) {
                    e = e7;
                } catch (RemoteException e8) {
                    e = e8;
                }
                if (iArr[i4] >= 0) {
                    int i6 = iArr[i4];
                    if (!ContactsGroupUtils.USIMGroup.addUSIMGroupMember(intExtra, i6, i)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[addMembersToGroup] fail simIndex:");
                        sb.append(i6);
                        sb.append(",groupId:");
                        try {
                            sb.append(j);
                            Log.w("SimGroupProcessor", sb.toString());
                            i2 = i3;
                        } catch (OperationApplicationException e9) {
                            e = e9;
                            i2 = i3;
                            Log.w("SimGroupProcessor", "[addMembersToGroup] Assert failed in adding raw contact ID " + String.valueOf(j2) + ". Already exists in group " + String.valueOf(j), e);
                        } catch (RemoteException e10) {
                            e = e10;
                            i2 = i3;
                            Log.e("SimGroupProcessor", "[addMembersToGroup]Problem persisting user edits for raw contact ID " + String.valueOf(j2), e);
                        }
                        z = false;
                    }
                }
                arrayList = new ArrayList<>();
                ContentProviderOperation.Builder builderNewAssertQuery2 = ContentProviderOperation.newAssertQuery(ContactsContract.Data.CONTENT_URI);
                String[] strArr2 = new String[3];
                strArr2[0] = String.valueOf(j2);
                i2 = 1;
                strArr2[1] = "vnd.android.cursor.item/group_membership";
                strArr2[2] = String.valueOf(j);
                builderNewAssertQuery2.withSelection("raw_contact_id=? AND mimetype=? AND data1=?", strArr2);
                builderNewAssertQuery2.withExpectedCount(0);
                arrayList.add(builderNewAssertQuery2.build());
                ContentProviderOperation.Builder builderNewInsert2 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builderNewInsert2.withValue("raw_contact_id", Long.valueOf(j2));
                builderNewInsert2.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                builderNewInsert2.withValue("data1", Long.valueOf(j));
                arrayList.add(builderNewInsert2.build());
                if (arrayList.isEmpty()) {
                }
            }
            i5++;
            i3 = i2;
        }
        return z;
    }

    private boolean removeMembersFromGroup(ContentResolver contentResolver, long[] jArr, long j, int[] iArr, int i, int i2) {
        if (jArr != null) {
            boolean z = true;
            int i3 = -1;
            for (long j2 : jArr) {
                i3++;
                int i4 = iArr[i3];
                if (i > 0 && i4 >= 0 && i2 >= 0 && !ContactsGroupUtils.USIMGroup.deleteUSIMGroupMember(i, i4, i2)) {
                    Log.i("SimGroupProcessor", "[removeMembersFromGroup]Remove failed RawContactid: " + j2);
                    z = false;
                } else {
                    contentResolver.delete(ContactsContract.Data.CONTENT_URI, "raw_contact_id=? AND mimetype=? AND data1=?", new String[]{String.valueOf(j2), "vnd.android.cursor.item/group_membership", String.valueOf(j)});
                }
            }
            return z;
        }
        Log.w("SimGroupProcessor", "[removeMembersFromGroup]RawContacts to be removed is empty!");
        return true;
    }

    private void deliverCallbackOnUiThread(final Intent intent) {
        Log.d("SimGroupProcessor", "[deliverCallbackOnUiThread] callbackIntent call onSimGroupCompleted");
        if (this.mMainHandler == null) {
            Log.d("SimGroupProcessor", "[deliverCallbackOnUiThread] mMainHandler is null, can not callback");
            return;
        }
        for (final Listener listener : sListeners) {
            this.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSimGroupCompleted(intent);
                }
            });
        }
    }

    private void deliverCallback(Intent intent) {
        deliverCallbackOnUiThread(intent);
    }

    public static class SimGroupsDaoImpl extends ContactSaveService.GroupsDaoImpl {
        private int mSubId;

        public SimGroupsDaoImpl(Context context, int i) {
            super(context);
            this.mSubId = SubInfoUtils.getInvalidSubId();
            this.mSubId = i;
            Log.d("SimGroupsDaoImpl", "mSubId = " + this.mSubId);
        }

        @Override
        public Bundle captureDeletionUndoData(Uri uri) {
            return captureSimDeletionUndoData(super.captureDeletionUndoData(uri));
        }

        @Override
        public Uri undoDeletion(Bundle bundle) {
            if (!undoDeletionInSim(bundle)) {
                Log.d("SimGroupsDaoImpl", "undoDeletionInSim fail, no need undoDeletion in db, return");
                return null;
            }
            return super.undoDeletion(bundle);
        }

        private Bundle captureSimDeletionUndoData(Bundle bundle) {
            bundle.putIntArray("groupMemberIndexInSims", getIndexInSims(bundle));
            return bundle;
        }

        private int[] getIndexInSims(Bundle bundle) {
            ContentValues contentValues = (ContentValues) bundle.getParcelable(ContactSaveService.GroupsDaoImpl.KEY_GROUP_DATA);
            long[] longArray = bundle.getLongArray(ContactSaveService.GroupsDaoImpl.KEY_GROUP_MEMBERS);
            if (longArray == null || longArray.length == 0) {
                Log.d("SimGroupsDaoImpl", "[getIndexInSims] no raw contact, return null");
                return null;
            }
            Uri.Builder builderBuildUpon = ContactsContract.RawContacts.CONTENT_URI.buildUpon();
            String asString = contentValues.getAsString("account_name");
            String asString2 = contentValues.getAsString("account_type");
            if (asString != null && asString2 != null) {
                builderBuildUpon.appendQueryParameter("account_name", asString);
                builderBuildUpon.appendQueryParameter("account_type", asString2);
            }
            String asString3 = contentValues.getAsString("data_set");
            if (asString3 != null) {
                builderBuildUpon.appendQueryParameter("data_set", asString3);
            }
            Uri uriBuild = builderBuildUpon.build();
            String[] strArr = {"index_in_sim"};
            StringBuilder sb = new StringBuilder();
            String[] strArr2 = new String[longArray.length];
            for (int i = 0; i < longArray.length; i++) {
                if (i > 0) {
                    sb.append(" OR ");
                }
                sb.append("_id");
                sb.append("=?");
                strArr2[i] = Long.toString(longArray[i]);
            }
            Cursor cursorQuery = getContext().getContentResolver().query(uriBuild, strArr, sb.toString(), strArr2, null, null);
            if (cursorQuery == null || cursorQuery.getCount() < 1) {
                Log.d("SimGroupsDaoImpl", "[getIndexInSims] cursor is empty, return null");
                return null;
            }
            int[] iArr = new int[cursorQuery.getCount()];
            try {
                cursorQuery.moveToPosition(-1);
                int i2 = 0;
                while (cursorQuery.moveToNext()) {
                    iArr[i2] = cursorQuery.getInt(0);
                    i2++;
                }
                cursorQuery.close();
                Log.d("SimGroupsDaoImpl", "[getIndexInSims] rawContactIds = " + Arrays.toString(longArray) + ", indexInSims = " + Arrays.toString(iArr));
                return iArr;
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }

        private boolean undoDeletionInSim(Bundle bundle) {
            ContentValues contentValues = (ContentValues) bundle.getParcelable(ContactSaveService.GroupsDaoImpl.KEY_GROUP_DATA);
            if (contentValues == null) {
                Log.d("SimGroupsDaoImpl", "[undoSimDeletion]groupData is null");
                return false;
            }
            String asString = contentValues.getAsString("title");
            if (TextUtils.isEmpty(asString)) {
                Log.d("SimGroupsDaoImpl", "[undoSimDeletion]group name is empty");
                return false;
            }
            String asString2 = contentValues.getAsString("account_type");
            String asString3 = contentValues.getAsString("account_name");
            contentValues.getAsString("data_set");
            if (!SimGroupUtils.checkGroupNameExist(getContext(), asString, asString3, asString2)) {
                Log.w("SimGroupsDaoImpl", "[undoSimDeletion]Group Name exist!");
                return false;
            }
            int iCreateGroupToIcc = SimGroupUtils.createGroupToIcc(asString, this.mSubId);
            if (iCreateGroupToIcc < 0) {
                Log.w("SimGroupsDaoImpl", "[createGroup]createGroupToIcc fail!");
                return false;
            }
            return addMembersToGroupInSim(bundle.getIntArray("groupMemberIndexInSims"), iCreateGroupToIcc);
        }

        private boolean addMembersToGroupInSim(int[] iArr, int i) {
            if (iArr == null || iArr.length == 0) {
                Log.e("SimGroupsDaoImpl", "[addMembersToGroupInSim] no members to add");
                return true;
            }
            if (this.mSubId <= 0 || i < 0) {
                Log.e("SimGroupsDaoImpl", "[addMembersToGroupInSim] parameter invalid, mSubId=" + this.mSubId + ", groupIdInIcc=" + i);
                return false;
            }
            boolean z = true;
            for (int i2 : iArr) {
                if (!ContactsGroupUtils.USIMGroup.addUSIMGroupMember(this.mSubId, i2, i)) {
                    Log.w("SimGroupsDaoImpl", "[addMembersToGroupInSim] add fail, simIndex:" + i2 + ",groupIdInIcc:" + i);
                    z = false;
                }
            }
            return z;
        }
    }
}
