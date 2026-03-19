package com.android.contacts;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.PinnedPositionsCompat;
import com.android.contacts.database.ContactUpdateUtils;
import com.android.contacts.database.SimContactDao;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.CPOWrapper;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.ContactDisplayUtils;
import com.android.contacts.util.ContactPhotoUtils;
import com.android.contacts.util.PermissionsUtil;
import com.android.contactsbind.FeedbackHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mediatek.contacts.ContactSaveServiceEx;
import com.mediatek.contacts.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContactSaveService extends IntentService {
    public static final String ACTION_CLEAR_PRIMARY = "clearPrimary";
    public static final String ACTION_CREATE_GROUP = "createGroup";
    public static final String ACTION_DELETE_CONTACT = "delete";
    public static final String ACTION_DELETE_GROUP = "deleteGroup";
    public static final String ACTION_DELETE_MULTIPLE_CONTACTS = "deleteMultipleContacts";
    public static final String ACTION_JOIN_CONTACTS = "joinContacts";
    public static final String ACTION_JOIN_SEVERAL_CONTACTS = "joinSeveralContacts";
    public static final String ACTION_NEW_RAW_CONTACT = "newRawContact";
    public static final String ACTION_RENAME_GROUP = "renameGroup";
    public static final String ACTION_SAVE_CONTACT = "saveContact";
    public static final String ACTION_SET_RINGTONE = "setRingtone";
    public static final String ACTION_SET_SEND_TO_VOICEMAIL = "sendToVoicemail";
    public static final String ACTION_SET_STARRED = "setStarred";
    public static final String ACTION_SET_SUPER_PRIMARY = "setSuperPrimary";
    public static final String ACTION_SLEEP = "sleep";
    public static final String ACTION_SPLIT_CONTACT = "splitContact";
    public static final String ACTION_UNDO = "undo";
    public static final String ACTION_UPDATE_GROUP = "updateGroup";
    public static final int BAD_ARGUMENTS = 3;
    public static final String BROADCAST_GROUP_DELETED = "groupDeleted";
    public static final String BROADCAST_LINK_COMPLETE = "linkComplete";
    public static final String BROADCAST_SERVICE_STATE_CHANGED = "serviceStateChanged";
    public static final String BROADCAST_UNLINK_COMPLETE = "unlinkComplete";
    public static final int CONTACTS_LINKED = 1;
    public static final int CONTACTS_SPLIT = 2;
    public static final int CP2_ERROR = 0;
    private static final boolean DEBUG = false;
    public static final String EXTRA_ACCOUNT = "account";
    public static final String EXTRA_ACCOUNT_NAME = "accountName";
    public static final String EXTRA_ACCOUNT_TYPE = "accountType";
    public static final String EXTRA_CALLBACK_INTENT = "callbackIntent";
    public static final String EXTRA_CONTACT_ID1 = "contactId1";
    public static final String EXTRA_CONTACT_ID2 = "contactId2";
    public static final String EXTRA_CONTACT_IDS = "contactIds";
    public static final String EXTRA_CONTACT_STATE = "state";
    public static final String EXTRA_CONTACT_URI = "contactUri";
    public static final String EXTRA_CONTENT_VALUES = "contentValues";
    public static final String EXTRA_CUSTOM_RINGTONE = "customRingtone";
    public static final String EXTRA_DATA_ID = "dataId";
    public static final String EXTRA_DATA_SET = "dataSet";
    public static final String EXTRA_DISPLAY_NAME = "extraDisplayName";
    public static final String EXTRA_DISPLAY_NAME_ARRAY = "extraDisplayNameArray";
    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_GROUP_LABEL = "groupLabel";
    public static final String EXTRA_HARD_SPLIT = "extraHardSplit";
    public static final String EXTRA_RAW_CONTACTS_TO_ADD = "rawContactsToAdd";
    public static final String EXTRA_RAW_CONTACTS_TO_REMOVE = "rawContactsToRemove";
    public static final String EXTRA_RAW_CONTACT_IDS = "rawContactIds";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_COUNT = "count";
    public static final String EXTRA_RESULT_RECEIVER = "resultReceiver";
    public static final String EXTRA_SAVE_IS_PROFILE = "saveIsProfile";
    public static final String EXTRA_SAVE_MODE = "saveMode";
    public static final String EXTRA_SAVE_SUCCEEDED = "saveSucceeded";
    public static final String EXTRA_SEND_TO_VOICEMAIL_FLAG = "sendToVoicemailFlag";
    public static final String EXTRA_SLEEP_DURATION = "sleepDuration";
    public static final String EXTRA_STARRED_FLAG = "starred";
    public static final String EXTRA_UNDO_ACTION = "undoAction";
    public static final String EXTRA_UNDO_DATA = "undoData";
    public static final String EXTRA_UPDATED_PHOTOS = "updatedPhotos";
    private static final int MAX_CONTACTS_PROVIDER_BATCH_SIZE = 499;
    private static final int MAX_OPERATIONS_SIZE = 400;
    private static final int PERSIST_TRIES = 3;
    public static final int RESULT_FAILURE = 2;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_UNKNOWN = 0;
    private static final String TAG = "ContactSaveService";
    public static DeleteEndListener sDeleteEndListener;
    private GroupsDao mGroupsDao;
    private Handler mMainHandler;
    private SimContactDao mSimContactDao;
    private static final HashSet<String> ALLOWED_DATA_COLUMNS = Sets.newHashSet("mimetype", "is_primary", "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10", "data11", "data12", "data13", "data14", "data15");
    private static final CopyOnWriteArrayList<Listener> sListeners = new CopyOnWriteArrayList<>();
    private static final State sState = new State();
    private static boolean sIsTransactionProcessing = false;

    private interface ContactEntityQuery {
        public static final int CONTACT_ID = 1;
        public static final int DATA_ID = 0;
        public static final int IS_SUPER_PRIMARY = 2;
        public static final String[] PROJECTION = {"data_id", "contact_id", "is_super_primary"};
        public static final String SELECTION = "mimetype = 'vnd.android.cursor.item/name' AND data1=display_name AND data1 IS NOT NULL  AND data1 != '' ";
    }

    public interface DeleteEndListener {
        void onDeleteEnd();

        void onDeleteStart();
    }

    public interface GroupsDao {
        Bundle captureDeletionUndoData(Uri uri);

        Uri create(String str, AccountWithDataSet accountWithDataSet);

        int delete(Uri uri);

        Uri undoDeletion(Bundle bundle);
    }

    private interface JoinContactQuery {
        public static final int CONTACT_ID = 1;
        public static final int DISPLAY_NAME_SOURCE = 2;
        public static final String[] PROJECTION = {"_id", "contact_id", "display_name_source"};
        public static final int _ID = 0;
    }

    public interface Listener {
        void onServiceCompleted(Intent intent);
    }

    public ContactSaveService() {
        super(TAG);
        setIntentRedelivery(true);
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mGroupsDao = new GroupsDaoImpl(this);
        this.mSimContactDao = SimContactDao.create(this);
    }

    public static void registerListener(Listener listener) {
        if (!(listener instanceof Activity)) {
            throw new ClassCastException("Only activities can be registered to receive callback from " + ContactSaveService.class.getName());
        }
        Log.d(TAG, "[registerListener] listener added to SaveService: " + listener);
        if (listener instanceof ContactEditorActivity) {
            for (Listener listener2 : sListeners) {
                if (listener2 instanceof ContactEditorActivity) {
                    Log.w(TAG, "[registerListener] only one ContactEditorActivity instance allowed,finish old one: " + listener2);
                    ((ContactEditorActivity) listener2).finish();
                }
            }
        }
        sListeners.add(0, listener);
    }

    public static boolean canUndo(Intent intent) {
        return intent.hasExtra(EXTRA_UNDO_DATA);
    }

    public static void unregisterListener(Listener listener) {
        sListeners.remove(listener);
    }

    public static State getState() {
        return sState;
    }

    private void notifyStateChanged() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_SERVICE_STATE_CHANGED));
    }

    public static boolean startService(Context context, Intent intent, int i) {
        int i2;
        try {
            context.startService(intent);
            return true;
        } catch (Exception e) {
            switch (i) {
                case 0:
                    i2 = R.string.contactSavedErrorToast;
                    break;
                case 1:
                    i2 = R.string.contactJoinErrorToast;
                    break;
                case 2:
                    i2 = R.string.contactUnlinkErrorToast;
                    break;
                default:
                    i2 = R.string.contactGenericErrorToast;
                    break;
            }
            Toast.makeText(context, i2, 0).show();
            return false;
        }
    }

    public static void startService(Context context, Intent intent) {
        try {
            context.startService(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.contactGenericErrorToast, 0).show();
        }
    }

    @Override
    public Object getSystemService(String str) {
        Object systemService = super.getSystemService(str);
        if (systemService != null) {
            return systemService;
        }
        return getApplicationContext().getSystemService(str);
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        sState.onStart(intent);
        notifyStateChanged();
        return super.onStartCommand(intent, i, i2);
    }

    @Override
    protected void onHandleIntent(Intent intent) throws Throwable {
        if (intent == null) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "onHandleIntent: could not handle null intent");
                return;
            }
            return;
        }
        if (!PermissionsUtil.hasPermission(this, "android.permission.WRITE_CONTACTS")) {
            Log.w(TAG, "No WRITE_CONTACTS permission, unable to write to CP2");
            showToast(R.string.contactSavedErrorToast);
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "[onHandleIntent]action = " + action);
        if (ACTION_NEW_RAW_CONTACT.equals(action)) {
            createRawContact(intent);
        } else if (ACTION_SAVE_CONTACT.equals(action)) {
            try {
                saveContact(intent);
            } catch (IllegalStateException e) {
                Log.w(TAG, "[onHandleIntent] IllegalStateException:" + e.toString());
                Intent intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                if (intent2 != null) {
                    intent2.putExtra(EXTRA_SAVE_SUCCEEDED, false);
                    intent2.setData(null);
                    deliverCallback(intent2);
                } else {
                    Log.w(TAG, "[onHandleIntent] IllegalStateException: callbackIntent == NULL!");
                }
            }
        } else if ("createGroup".equals(action)) {
            setGroupTransactionProcessing(true);
            createGroup(intent);
        } else if (ACTION_RENAME_GROUP.equals(action)) {
            renameGroup(intent);
        } else if ("deleteGroup".equals(action)) {
            deleteGroup(intent);
        } else if ("updateGroup".equals(action)) {
            setGroupTransactionProcessing(true);
            updateGroup(intent);
        } else if (ACTION_SET_STARRED.equals(action)) {
            setStarred(intent);
        } else if (ACTION_SET_SUPER_PRIMARY.equals(action)) {
            setSuperPrimary(intent);
        } else if (ACTION_CLEAR_PRIMARY.equals(action)) {
            clearPrimary(intent);
        } else if (ACTION_DELETE_MULTIPLE_CONTACTS.equals(action)) {
            deleteMultipleContacts(intent);
        } else if (ACTION_DELETE_CONTACT.equals(action)) {
            deleteContact(intent);
        } else if (ACTION_SPLIT_CONTACT.equals(action)) {
            splitContact(intent);
        } else if (ACTION_JOIN_CONTACTS.equals(action)) {
            joinContacts(intent);
        } else if (ACTION_JOIN_SEVERAL_CONTACTS.equals(action)) {
            joinSeveralContacts(intent);
        } else if (ACTION_SET_SEND_TO_VOICEMAIL.equals(action)) {
            setSendToVoicemail(intent);
        } else if (ACTION_SET_RINGTONE.equals(action)) {
            setRingtone(intent);
        } else if (ACTION_UNDO.equals(action)) {
            setGroupTransactionProcessing(true);
            undo(intent);
        } else if (ACTION_SLEEP.equals(action)) {
            sleepForDebugging(intent);
        }
        sState.onFinish(intent);
        notifyStateChanged();
        setGroupTransactionProcessing(false);
        Log.d(TAG, "[onHandleIntent] finished");
    }

    public static Intent createNewRawContactIntent(Context context, ArrayList<ContentValues> arrayList, AccountWithDataSet accountWithDataSet, Class<? extends Activity> cls, String str) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_NEW_RAW_CONTACT);
        if (accountWithDataSet != null) {
            intent.putExtra(EXTRA_ACCOUNT_NAME, accountWithDataSet.name);
            intent.putExtra(EXTRA_ACCOUNT_TYPE, accountWithDataSet.type);
            intent.putExtra(EXTRA_DATA_SET, accountWithDataSet.dataSet);
        }
        intent.putParcelableArrayListExtra(EXTRA_CONTENT_VALUES, arrayList);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str);
        intent.putExtra(EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    private void createRawContact(Intent intent) {
        String stringExtra = intent.getStringExtra(EXTRA_ACCOUNT_NAME);
        String stringExtra2 = intent.getStringExtra(EXTRA_ACCOUNT_TYPE);
        String stringExtra3 = intent.getStringExtra(EXTRA_DATA_SET);
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra(EXTRA_CONTENT_VALUES);
        Intent intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        arrayList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue("account_name", stringExtra).withValue("account_type", stringExtra2).withValue("data_set", stringExtra3).build());
        int size = parcelableArrayListExtra.size();
        for (int i = 0; i < size; i++) {
            ContentValues contentValues = (ContentValues) parcelableArrayListExtra.get(i);
            contentValues.keySet().retainAll(ALLOWED_DATA_COLUMNS);
            arrayList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValues(contentValues).build());
        }
        ContentResolver contentResolver = getContentResolver();
        try {
            intent2.setData(ContactsContract.RawContacts.getContactLookupUri(contentResolver, contentResolver.applyBatch("com.android.contacts", arrayList)[0].uri));
            deliverCallback(intent2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store new contact", e);
        }
    }

    public static Intent createSaveContactIntent(Context context, RawContactDeltaList rawContactDeltaList, String str, int i, boolean z, Class<? extends Activity> cls, String str2, long j, Uri uri) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(String.valueOf(j), uri);
        return createSaveContactIntent(context, rawContactDeltaList, str, i, z, cls, str2, bundle, null, null);
    }

    public static Intent createSaveContactIntent(Context context, RawContactDeltaList rawContactDeltaList, String str, int i, boolean z, Class<? extends Activity> cls, String str2, Bundle bundle, String str3, Long l) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SAVE_CONTACT);
        intent.putExtra(EXTRA_CONTACT_STATE, (Parcelable) rawContactDeltaList);
        intent.putExtra(EXTRA_SAVE_IS_PROFILE, z);
        intent.putExtra(EXTRA_SAVE_MODE, i);
        if (bundle != null) {
            intent.putExtra(EXTRA_UPDATED_PHOTOS, (Parcelable) bundle);
        }
        if (cls != null) {
            Intent intent2 = new Intent(context, cls);
            intent2.putExtra(str, i);
            if (str3 != null && l != null) {
                intent2.putExtra(str3, l);
            }
            intent2.setAction(str2);
            intent.putExtra(EXTRA_CALLBACK_INTENT, intent2);
        }
        return intent;
    }

    private void saveContact(Intent intent) throws Throwable {
        boolean z;
        Uri contactLookupUri;
        Intent intent2;
        ContentResolver contentResolver;
        int size;
        boolean z2;
        int i;
        ArrayList<CPOWrapper> arrayListBuildDiffWrapper;
        boolean z3;
        long rawContactId;
        RawContactDeltaList rawContactDeltaList;
        Cursor cursorQuery;
        Uri uriWithAppendedId;
        RawContactDeltaList rawContactDeltaList2 = (RawContactDeltaList) intent.getParcelableExtra(EXTRA_CONTACT_STATE);
        int i2 = 0;
        boolean booleanExtra = intent.getBooleanExtra(EXTRA_SAVE_IS_PROFILE, false);
        Bundle bundle = (Bundle) intent.getParcelableExtra(EXTRA_UPDATED_PHOTOS);
        Log.d(TAG, "[saveContact]isProfile = " + booleanExtra);
        if (rawContactDeltaList2 != null) {
            int i3 = -1;
            int intExtra = intent.getIntExtra(EXTRA_SAVE_MODE, -1);
            RawContactModifier.trimEmpty(rawContactDeltaList2, AccountTypeManager.getInstance(this));
            ContentResolver contentResolver2 = getContentResolver();
            RawContactDeltaList rawContactDeltaListMergeAfter = rawContactDeltaList2;
            int i4 = 0;
            long j = -1;
            Uri lookupUri = null;
            while (true) {
                int i5 = i4 + 1;
                if (i4 >= 3) {
                    z = true;
                    break;
                }
                try {
                    try {
                        arrayListBuildDiffWrapper = rawContactDeltaListMergeAfter.buildDiffWrapper();
                    } catch (OperationApplicationException e) {
                        e = e;
                        contentResolver = contentResolver2;
                        z = true;
                    }
                    try {
                        ArrayList<ContentProviderOperation> arrayListNewArrayList = Lists.newArrayList();
                        Iterator<CPOWrapper> it = arrayListBuildDiffWrapper.iterator();
                        while (it.hasNext()) {
                            arrayListNewArrayList.add(it.next().getOperation());
                        }
                        ContentProviderResult[] contentProviderResultArr = new ContentProviderResult[arrayListNewArrayList.size()];
                        int i6 = i2;
                        while (true) {
                            try {
                                if (i6 >= arrayListNewArrayList.size()) {
                                    z3 = false;
                                    break;
                                }
                                int iApplyDiffSubset = applyDiffSubset(arrayListNewArrayList, i6, contentProviderResultArr, contentResolver2);
                                if (iApplyDiffSubset == i3) {
                                    Log.w(TAG, "Resolver.applyBatch failed in saveContacts");
                                    z3 = true;
                                    break;
                                }
                                i6 += iApplyDiffSubset;
                            } catch (OperationApplicationException e2) {
                                e = e2;
                                contentResolver = contentResolver2;
                            }
                        }
                        if (z3) {
                            i4 = i5;
                            i2 = 0;
                        } else {
                            ContentResolver contentResolver3 = contentResolver2;
                            try {
                                rawContactId = getRawContactId(rawContactDeltaListMergeAfter, arrayListBuildDiffWrapper, contentProviderResultArr);
                            } catch (OperationApplicationException e3) {
                                e = e3;
                                contentResolver = contentResolver3;
                                z = true;
                                Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                StringBuilder sb = new StringBuilder("_id IN(");
                                size = rawContactDeltaListMergeAfter.size();
                                z2 = z;
                                while (i < size) {
                                }
                                sb.append(")");
                                if (z2) {
                                }
                            }
                            if (rawContactId != -1) {
                                long insertedRawContactId = getInsertedRawContactId(arrayListBuildDiffWrapper, contentProviderResultArr);
                                if (booleanExtra) {
                                    try {
                                        z = true;
                                        rawContactDeltaList = rawContactDeltaListMergeAfter;
                                        try {
                                            try {
                                                cursorQuery = contentResolver3.query(ContactsContract.Profile.CONTENT_URI, new String[]{"_id", "lookup"}, null, null, null);
                                            } catch (OperationApplicationException e4) {
                                                e = e4;
                                            }
                                            try {
                                            } catch (OperationApplicationException e5) {
                                                e = e5;
                                                rawContactDeltaListMergeAfter = rawContactDeltaList;
                                                contentResolver = contentResolver3;
                                                j = insertedRawContactId;
                                                Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                                StringBuilder sb2 = new StringBuilder("_id IN(");
                                                size = rawContactDeltaListMergeAfter.size();
                                                z2 = z;
                                                while (i < size) {
                                                }
                                                sb2.append(")");
                                                if (z2) {
                                                }
                                            }
                                        } catch (RemoteException e6) {
                                            e = e6;
                                            j = insertedRawContactId;
                                            FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                                            contactLookupUri = lookupUri;
                                            boolean z4 = false;
                                            if (bundle != null) {
                                                for (String str : bundle.keySet()) {
                                                    Uri uri = (Uri) bundle.getParcelable(str);
                                                    long j2 = Long.parseLong(str);
                                                    if (j2 < 0) {
                                                        j2 = j;
                                                    }
                                                    if (j2 < 0 || !saveUpdatedPhoto(j2, uri, intExtra)) {
                                                        z4 = false;
                                                    } else {
                                                        ContactSaveServiceEx.refreshPhotoCache(j2);
                                                    }
                                                }
                                            }
                                            intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                                            if (intent2 == null) {
                                                if (z4) {
                                                    intent2.putExtra(EXTRA_SAVE_SUCCEEDED, z);
                                                }
                                                intent2.setData(contactLookupUri);
                                                Log.d(TAG, "[saveContact]deliverCallback,callbackIntent = " + intent2);
                                                deliverCallback(intent2);
                                                return;
                                            }
                                            return;
                                        } catch (IllegalArgumentException e7) {
                                            e = e7;
                                            j = insertedRawContactId;
                                            FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                                            showToast(R.string.contactSavedErrorToast);
                                            contactLookupUri = lookupUri;
                                            boolean z42 = false;
                                            if (bundle != null) {
                                            }
                                            intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                                            if (intent2 == null) {
                                            }
                                        }
                                    } catch (OperationApplicationException e8) {
                                        e = e8;
                                        z = true;
                                        contentResolver = contentResolver3;
                                        j = insertedRawContactId;
                                        Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                        StringBuilder sb22 = new StringBuilder("_id IN(");
                                        size = rawContactDeltaListMergeAfter.size();
                                        z2 = z;
                                        while (i < size) {
                                        }
                                        sb22.append(")");
                                        if (z2) {
                                        }
                                    } catch (RemoteException e9) {
                                        e = e9;
                                        z = true;
                                    } catch (IllegalArgumentException e10) {
                                        e = e10;
                                        z = true;
                                    }
                                    if (cursorQuery == null) {
                                        rawContactDeltaListMergeAfter = rawContactDeltaList;
                                        i4 = i5;
                                        contentResolver2 = contentResolver3;
                                        j = insertedRawContactId;
                                        i2 = 0;
                                        i3 = -1;
                                    } else {
                                        try {
                                            if (cursorQuery.moveToFirst()) {
                                                try {
                                                    lookupUri = ContactsContract.Contacts.getLookupUri(cursorQuery.getLong(0), cursorQuery.getString(1));
                                                } catch (Throwable th) {
                                                    th = th;
                                                    cursorQuery.close();
                                                    throw th;
                                                }
                                            }
                                            cursorQuery.close();
                                            rawContactDeltaListMergeAfter = rawContactDeltaList;
                                            contactLookupUri = lookupUri;
                                            contentResolver = contentResolver3;
                                            if (contactLookupUri != null) {
                                                break;
                                            }
                                            try {
                                                if (!Log.isLoggable(TAG, 2)) {
                                                    break;
                                                }
                                                Log.v(TAG, "Saved contact. New URI: " + contactLookupUri);
                                                break;
                                            } catch (OperationApplicationException e11) {
                                                e = e11;
                                                lookupUri = contactLookupUri;
                                                j = insertedRawContactId;
                                                Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                                StringBuilder sb222 = new StringBuilder("_id IN(");
                                                size = rawContactDeltaListMergeAfter.size();
                                                z2 = z;
                                                while (i < size) {
                                                }
                                                sb222.append(")");
                                                if (z2) {
                                                }
                                            } catch (RemoteException e12) {
                                                e = e12;
                                                lookupUri = contactLookupUri;
                                                j = insertedRawContactId;
                                                FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                                                contactLookupUri = lookupUri;
                                                boolean z422 = false;
                                                if (bundle != null) {
                                                }
                                                intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                                                if (intent2 == null) {
                                                }
                                            } catch (IllegalArgumentException e13) {
                                                e = e13;
                                                lookupUri = contactLookupUri;
                                                j = insertedRawContactId;
                                                FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                                                showToast(R.string.contactSavedErrorToast);
                                                contactLookupUri = lookupUri;
                                                boolean z4222 = false;
                                                if (bundle != null) {
                                                }
                                                intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                                                if (intent2 == null) {
                                                }
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                } else {
                                    z = true;
                                    try {
                                        uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
                                        contentResolver = contentResolver3;
                                    } catch (OperationApplicationException e14) {
                                        e = e14;
                                        contentResolver = contentResolver3;
                                        j = insertedRawContactId;
                                        Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                        StringBuilder sb2222 = new StringBuilder("_id IN(");
                                        size = rawContactDeltaListMergeAfter.size();
                                        z2 = z;
                                        for (i = 0; i < size; i++) {
                                            Long rawContactId2 = rawContactDeltaListMergeAfter.getRawContactId(i);
                                            if (rawContactId2 != null && rawContactId2.longValue() != -1) {
                                                if (!z2) {
                                                    sb2222.append(',');
                                                }
                                                sb2222.append(rawContactId2);
                                                z2 = false;
                                            }
                                        }
                                        sb2222.append(")");
                                        if (z2) {
                                            throw new IllegalStateException("Version consistency failed for a new contact", e);
                                        }
                                        rawContactDeltaListMergeAfter = RawContactDeltaList.mergeAfter(RawContactDeltaList.fromQuery(booleanExtra ? ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI : ContactsContract.RawContactsEntity.CONTENT_URI, contentResolver, sb2222.toString(), null, null), rawContactDeltaListMergeAfter);
                                        if (booleanExtra) {
                                            Iterator<RawContactDelta> it2 = rawContactDeltaListMergeAfter.iterator();
                                            while (it2.hasNext()) {
                                                it2.next().setProfileQueryUri();
                                            }
                                        }
                                        contentResolver2 = contentResolver;
                                        i4 = i5;
                                        i2 = 0;
                                        i3 = -1;
                                    }
                                    try {
                                        contactLookupUri = ContactsContract.RawContacts.getContactLookupUri(contentResolver, uriWithAppendedId);
                                        if (contactLookupUri != null) {
                                        }
                                    } catch (OperationApplicationException e15) {
                                        e = e15;
                                        j = insertedRawContactId;
                                        Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                        StringBuilder sb22222 = new StringBuilder("_id IN(");
                                        size = rawContactDeltaListMergeAfter.size();
                                        z2 = z;
                                        while (i < size) {
                                        }
                                        sb22222.append(")");
                                        if (z2) {
                                        }
                                    }
                                }
                            } else {
                                contentResolver = contentResolver3;
                                z = true;
                                try {
                                    throw new IllegalStateException("Could not determine RawContact ID after save");
                                } catch (OperationApplicationException e16) {
                                    e = e16;
                                    Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                                    StringBuilder sb222222 = new StringBuilder("_id IN(");
                                    size = rawContactDeltaListMergeAfter.size();
                                    z2 = z;
                                    while (i < size) {
                                    }
                                    sb222222.append(")");
                                    if (z2) {
                                    }
                                } catch (RemoteException e17) {
                                    e = e17;
                                    FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                                    contactLookupUri = lookupUri;
                                    boolean z42222 = false;
                                    if (bundle != null) {
                                    }
                                    intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                                    if (intent2 == null) {
                                    }
                                } catch (IllegalArgumentException e18) {
                                    e = e18;
                                    FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                                    showToast(R.string.contactSavedErrorToast);
                                    contactLookupUri = lookupUri;
                                    boolean z422222 = false;
                                    if (bundle != null) {
                                    }
                                    intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                                    if (intent2 == null) {
                                    }
                                }
                            }
                        }
                    } catch (RemoteException e19) {
                        e = e19;
                        z = true;
                        FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                        contactLookupUri = lookupUri;
                        boolean z4222222 = false;
                        if (bundle != null) {
                        }
                        intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                        if (intent2 == null) {
                        }
                    } catch (IllegalArgumentException e20) {
                        e = e20;
                        z = true;
                        FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits", e);
                        showToast(R.string.contactSavedErrorToast);
                        contactLookupUri = lookupUri;
                        boolean z42222222 = false;
                        if (bundle != null) {
                        }
                        intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
                        if (intent2 == null) {
                        }
                    }
                } catch (RemoteException e21) {
                    e = e21;
                } catch (IllegalArgumentException e22) {
                    e = e22;
                }
            }
        } else {
            Log.e(TAG, "Invalid arguments for saveContact request");
        }
    }

    private int applyDiffSubset(ArrayList<ContentProviderOperation> arrayList, int i, ContentProviderResult[] contentProviderResultArr, ContentResolver contentResolver) throws RemoteException, OperationApplicationException {
        int iMin = Math.min(arrayList.size() - i, MAX_CONTACTS_PROVIDER_BATCH_SIZE);
        ArrayList<ContentProviderOperation> arrayList2 = new ArrayList<>();
        arrayList2.addAll(arrayList.subList(i, iMin + i));
        ContentProviderResult[] contentProviderResultArrApplyBatch = contentResolver.applyBatch("com.android.contacts", arrayList2);
        if (contentProviderResultArrApplyBatch == null || contentProviderResultArrApplyBatch.length + i > contentProviderResultArr.length) {
            return -1;
        }
        int length = contentProviderResultArrApplyBatch.length;
        int i2 = 0;
        while (i2 < length) {
            contentProviderResultArr[i] = contentProviderResultArrApplyBatch[i2];
            i2++;
            i++;
        }
        return contentProviderResultArrApplyBatch.length;
    }

    private boolean saveUpdatedPhoto(long j, Uri uri, int i) {
        return ContactPhotoUtils.savePhotoFromUriToUri(this, uri, Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, j), "display_photo"), i == 0);
    }

    private long getRawContactId(RawContactDeltaList rawContactDeltaList, ArrayList<CPOWrapper> arrayList, ContentProviderResult[] contentProviderResultArr) {
        long jFindRawContactId = rawContactDeltaList.findRawContactId();
        if (jFindRawContactId != -1) {
            return jFindRawContactId;
        }
        return getInsertedRawContactId(arrayList, contentProviderResultArr);
    }

    private long getInsertedRawContactId(ArrayList<CPOWrapper> arrayList, ContentProviderResult[] contentProviderResultArr) {
        if (contentProviderResultArr == null) {
            return -1L;
        }
        int size = arrayList.size();
        int length = contentProviderResultArr.length;
        for (int i = 0; i < size && i < length; i++) {
            CPOWrapper cPOWrapper = arrayList.get(i);
            if (CompatUtils.isInsertCompat(cPOWrapper) && cPOWrapper.getOperation().getUri().getEncodedPath().contains(ContactsContract.RawContacts.CONTENT_URI.getEncodedPath())) {
                return ContentUris.parseId(contentProviderResultArr[i].uri);
            }
        }
        return -1L;
    }

    public static Intent createNewGroupIntent(Context context, AccountWithDataSet accountWithDataSet, String str, long[] jArr, Class<? extends Activity> cls, String str2) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction("createGroup");
        intent.putExtra(EXTRA_ACCOUNT_TYPE, accountWithDataSet.type);
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountWithDataSet.name);
        intent.putExtra(EXTRA_DATA_SET, accountWithDataSet.dataSet);
        intent.putExtra(EXTRA_GROUP_LABEL, str);
        intent.putExtra(EXTRA_RAW_CONTACTS_TO_ADD, jArr);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str2);
        intent.putExtra(EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    private void createGroup(Intent intent) {
        String stringExtra = intent.getStringExtra(EXTRA_ACCOUNT_TYPE);
        String stringExtra2 = intent.getStringExtra(EXTRA_ACCOUNT_NAME);
        String stringExtra3 = intent.getStringExtra(EXTRA_DATA_SET);
        String stringExtra4 = intent.getStringExtra(EXTRA_GROUP_LABEL);
        long[] longArrayExtra = intent.getLongArrayExtra(EXTRA_RAW_CONTACTS_TO_ADD);
        Uri uriCreate = this.mGroupsDao.create(stringExtra4, new AccountWithDataSet(stringExtra2, stringExtra, stringExtra3));
        ContentResolver contentResolver = getContentResolver();
        if (uriCreate == null) {
            Log.e(TAG, "Couldn't create group with label " + stringExtra4);
            return;
        }
        addMembersToGroup(contentResolver, longArrayExtra, ContentUris.parseId(uriCreate));
        ContentValues contentValues = new ContentValues();
        contentValues.clear();
        contentValues.put("mimetype", "vnd.android.cursor.item/group_membership");
        contentValues.put("data1", Long.valueOf(ContentUris.parseId(uriCreate)));
        Intent intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
        intent2.setData(uriCreate);
        intent2.putExtra("data", Lists.newArrayList(contentValues));
        deliverCallback(intent2);
    }

    public static Intent createGroupRenameIntent(Context context, long j, String str, Class<? extends Activity> cls, String str2) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_RENAME_GROUP);
        intent.putExtra(EXTRA_GROUP_ID, j);
        intent.putExtra(EXTRA_GROUP_LABEL, str);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str2);
        intent.putExtra(EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    private void renameGroup(Intent intent) {
        long longExtra = intent.getLongExtra(EXTRA_GROUP_ID, -1L);
        String stringExtra = intent.getStringExtra(EXTRA_GROUP_LABEL);
        if (longExtra == -1) {
            Log.e(TAG, "Invalid arguments for renameGroup request");
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", stringExtra);
        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, longExtra);
        getContentResolver().update(uriWithAppendedId, contentValues, null, null);
        Intent intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
        intent2.setData(uriWithAppendedId);
        deliverCallback(intent2);
    }

    public static Intent createGroupDeletionIntent(Context context, long j) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction("deleteGroup");
        intent.putExtra(EXTRA_GROUP_ID, j);
        return intent;
    }

    private void deleteGroup(Intent intent) {
        if (sDeleteEndListener != null) {
            sDeleteEndListener.onDeleteStart();
        }
        long longExtra = intent.getLongExtra(EXTRA_GROUP_ID, -1L);
        if (longExtra == -1) {
            Log.e(TAG, "Invalid arguments for deleteGroup request");
            return;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, longExtra);
        Intent intent2 = new Intent(BROADCAST_GROUP_DELETED);
        Bundle bundleCaptureDeletionUndoData = this.mGroupsDao.captureDeletionUndoData(uriWithAppendedId);
        intent2.putExtra(EXTRA_UNDO_ACTION, "deleteGroup");
        intent2.putExtra(EXTRA_UNDO_DATA, bundleCaptureDeletionUndoData);
        this.mGroupsDao.delete(uriWithAppendedId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent2);
        if (sDeleteEndListener != null) {
            sDeleteEndListener.onDeleteEnd();
        }
    }

    public static Intent createUndoIntent(Context context, Intent intent) {
        Intent intent2 = new Intent(context, (Class<?>) ContactSaveService.class);
        intent2.setAction(ACTION_UNDO);
        intent2.putExtras(intent);
        return intent2;
    }

    private void undo(Intent intent) {
        if ("deleteGroup".equals(intent.getStringExtra(EXTRA_UNDO_ACTION))) {
            this.mGroupsDao.undoDeletion(intent.getBundleExtra(EXTRA_UNDO_DATA));
        }
    }

    public static Intent createGroupUpdateIntent(Context context, long j, String str, long[] jArr, long[] jArr2, Class<? extends Activity> cls, String str2) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction("updateGroup");
        intent.putExtra(EXTRA_GROUP_ID, j);
        intent.putExtra(EXTRA_GROUP_LABEL, str);
        intent.putExtra(EXTRA_RAW_CONTACTS_TO_ADD, jArr);
        intent.putExtra(EXTRA_RAW_CONTACTS_TO_REMOVE, jArr2);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str2);
        intent.putExtra(EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    private void updateGroup(Intent intent) {
        long longExtra = intent.getLongExtra(EXTRA_GROUP_ID, -1L);
        String stringExtra = intent.getStringExtra(EXTRA_GROUP_LABEL);
        long[] longArrayExtra = intent.getLongArrayExtra(EXTRA_RAW_CONTACTS_TO_ADD);
        long[] longArrayExtra2 = intent.getLongArrayExtra(EXTRA_RAW_CONTACTS_TO_REMOVE);
        if (longExtra == -1) {
            Log.e(TAG, "Invalid arguments for updateGroup request");
            return;
        }
        ContentResolver contentResolver = getContentResolver();
        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, longExtra);
        if (stringExtra != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", stringExtra);
            contentResolver.update(uriWithAppendedId, contentValues, null, null);
        }
        addMembersToGroup(contentResolver, longArrayExtra, longExtra);
        removeMembersFromGroup(contentResolver, longArrayExtra2, longExtra);
        Intent intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
        intent2.setData(uriWithAppendedId);
        deliverCallback(intent2);
    }

    private void addMembersToGroup(ContentResolver contentResolver, long[] jArr, long j) {
        if (jArr == null) {
            return;
        }
        for (long j2 : jArr) {
            try {
                ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
                ContentProviderOperation.Builder builderNewAssertQuery = ContentProviderOperation.newAssertQuery(ContactsContract.Data.CONTENT_URI);
                builderNewAssertQuery.withSelection("raw_contact_id=? AND mimetype=? AND data1=?", new String[]{String.valueOf(j2), "vnd.android.cursor.item/group_membership", String.valueOf(j)});
                builderNewAssertQuery.withExpectedCount(0);
                arrayList.add(builderNewAssertQuery.build());
                ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builderNewInsert.withValue("raw_contact_id", Long.valueOf(j2));
                builderNewInsert.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                builderNewInsert.withValue("data1", Long.valueOf(j));
                arrayList.add(builderNewInsert.build());
                if (!arrayList.isEmpty()) {
                    contentResolver.applyBatch("com.android.contacts", arrayList);
                }
            } catch (OperationApplicationException e) {
                FeedbackHelper.sendFeedback(this, TAG, "Assert failed in adding raw contact ID " + String.valueOf(j2) + ". Already exists in group " + String.valueOf(j), e);
            } catch (RemoteException e2) {
                FeedbackHelper.sendFeedback(this, TAG, "Problem persisting user edits for raw contact ID " + String.valueOf(j2), e2);
            }
        }
    }

    private static void removeMembersFromGroup(ContentResolver contentResolver, long[] jArr, long j) {
        if (jArr == null) {
            return;
        }
        for (long j2 : jArr) {
            contentResolver.delete(ContactsContract.Data.CONTENT_URI, "raw_contact_id=? AND mimetype=? AND data1=?", new String[]{String.valueOf(j2), "vnd.android.cursor.item/group_membership", String.valueOf(j)});
        }
    }

    public static Intent createSetStarredIntent(Context context, Uri uri, boolean z) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SET_STARRED);
        intent.putExtra("contactUri", uri);
        intent.putExtra(EXTRA_STARRED_FLAG, z);
        return intent;
    }

    private void setStarred(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("contactUri");
        boolean booleanExtra = intent.getBooleanExtra(EXTRA_STARRED_FLAG, false);
        if (uri == null) {
            Log.e(TAG, "Invalid arguments for setStarred request");
            return;
        }
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(EXTRA_STARRED_FLAG, Boolean.valueOf(booleanExtra));
        getContentResolver().update(uri, contentValues, null, null);
        Cursor cursorQuery = getContentResolver().query(uri, new String[]{"_id"}, null, null, null);
        if (cursorQuery == null) {
            return;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                long j = cursorQuery.getLong(0);
                if (j < 9223372034707292160L) {
                    PinnedPositionsCompat.undemote(getContentResolver(), j);
                }
            }
        } finally {
            cursorQuery.close();
        }
    }

    public static Intent createSetSendToVoicemail(Context context, Uri uri, boolean z) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SET_SEND_TO_VOICEMAIL);
        intent.putExtra("contactUri", uri);
        intent.putExtra(EXTRA_SEND_TO_VOICEMAIL_FLAG, z);
        return intent;
    }

    private void setSendToVoicemail(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("contactUri");
        boolean booleanExtra = intent.getBooleanExtra(EXTRA_SEND_TO_VOICEMAIL_FLAG, false);
        if (uri == null) {
            Log.e(TAG, "Invalid arguments for setRedirectToVoicemail");
            return;
        }
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("send_to_voicemail", Boolean.valueOf(booleanExtra));
        getContentResolver().update(uri, contentValues, null, null);
    }

    public static Intent createSetRingtone(Context context, Uri uri, String str) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SET_RINGTONE);
        intent.putExtra("contactUri", uri);
        intent.putExtra(EXTRA_CUSTOM_RINGTONE, str);
        return intent;
    }

    private void setRingtone(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("contactUri");
        String stringExtra = intent.getStringExtra(EXTRA_CUSTOM_RINGTONE);
        if (uri == null) {
            Log.e(TAG, "Invalid arguments for setRingtone");
            return;
        }
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("custom_ringtone", stringExtra);
        getContentResolver().update(uri, contentValues, null, null);
    }

    public static Intent createSetSuperPrimaryIntent(Context context, long j) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SET_SUPER_PRIMARY);
        intent.putExtra(EXTRA_DATA_ID, j);
        return intent;
    }

    private void setSuperPrimary(Intent intent) {
        long longExtra = intent.getLongExtra(EXTRA_DATA_ID, -1L);
        if (longExtra == -1) {
            Log.e(TAG, "Invalid arguments for setSuperPrimary request");
        } else {
            ContactUpdateUtils.setSuperPrimary(this, longExtra);
        }
    }

    public static Intent createClearPrimaryIntent(Context context, long j) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_CLEAR_PRIMARY);
        intent.putExtra(EXTRA_DATA_ID, j);
        return intent;
    }

    private void clearPrimary(Intent intent) {
        long longExtra = intent.getLongExtra(EXTRA_DATA_ID, -1L);
        if (longExtra == -1) {
            Log.e(TAG, "Invalid arguments for clearPrimary request");
            return;
        }
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("is_super_primary", (Integer) 0);
        contentValues.put("is_primary", (Integer) 0);
        getContentResolver().update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, longExtra), contentValues, null, null);
    }

    public static Intent createDeleteContactIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_DELETE_CONTACT);
        intent.putExtra("contactUri", uri);
        return intent;
    }

    public static Intent createDeleteMultipleContactsIntent(Context context, long[] jArr, String[] strArr) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_DELETE_MULTIPLE_CONTACTS);
        intent.putExtra(EXTRA_CONTACT_IDS, jArr);
        intent.putExtra(EXTRA_DISPLAY_NAME_ARRAY, strArr);
        return intent;
    }

    private void deleteContact(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("contactUri");
        if (uri == null) {
            Log.e(TAG, "Invalid arguments for deleteContact request");
        } else {
            getContentResolver().delete(uri, null, null);
        }
    }

    private void deleteMultipleContacts(Intent intent) {
        final String quantityString;
        Log.d(TAG, "[deleteMultipleContacts] ...");
        long[] longArrayExtra = intent.getLongArrayExtra(EXTRA_CONTACT_IDS);
        if (longArrayExtra == null) {
            Log.e(TAG, "Invalid arguments for deleteMultipleContacts request");
            return;
        }
        for (long j : longArrayExtra) {
            getContentResolver().delete(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, j), null, null);
        }
        String[] stringArrayExtra = intent.getStringArrayExtra(EXTRA_DISPLAY_NAME_ARRAY);
        if (longArrayExtra.length != stringArrayExtra.length || stringArrayExtra.length == 0) {
            quantityString = getResources().getQuantityString(R.plurals.contacts_deleted_toast, longArrayExtra.length);
        } else if (stringArrayExtra.length == 1) {
            quantityString = getResources().getString(R.string.contacts_deleted_one_named_toast, stringArrayExtra);
        } else if (stringArrayExtra.length == 2) {
            quantityString = getResources().getString(R.string.contacts_deleted_two_named_toast, stringArrayExtra);
        } else {
            quantityString = getResources().getString(R.string.contacts_deleted_many_named_toast, stringArrayExtra);
        }
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactSaveService.this, quantityString, 1).show();
            }
        });
    }

    public static Intent createSplitContactIntent(Context context, long[][] jArr, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SPLIT_CONTACT);
        intent.putExtra(EXTRA_RAW_CONTACT_IDS, (Serializable) jArr);
        intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
        return intent;
    }

    public static Intent createHardSplitContactIntent(Context context, long[][] jArr) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_SPLIT_CONTACT);
        intent.putExtra(EXTRA_RAW_CONTACT_IDS, (Serializable) jArr);
        intent.putExtra(EXTRA_HARD_SPLIT, true);
        return intent;
    }

    private void splitContact(Intent intent) {
        long[][] jArr = (long[][]) intent.getSerializableExtra(EXTRA_RAW_CONTACT_IDS);
        ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        boolean booleanExtra = intent.getBooleanExtra(EXTRA_HARD_SPLIT, false);
        if (jArr == null) {
            Log.e(TAG, "Invalid argument for splitContact request");
            if (resultReceiver != null) {
                resultReceiver.send(3, new Bundle());
                return;
            }
            return;
        }
        ContentResolver contentResolver = getContentResolver();
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>(MAX_CONTACTS_PROVIDER_BATCH_SIZE);
        for (int i = 0; i < jArr.length; i++) {
            for (int i2 = 0; i2 < jArr.length; i2++) {
                if (i != i2 && !buildSplitTwoContacts(arrayList, jArr[i], jArr[i2], booleanExtra) && resultReceiver != null) {
                    resultReceiver.send(0, new Bundle());
                    return;
                }
            }
        }
        if (arrayList.size() > 0 && !applyOperations(contentResolver, arrayList)) {
            if (resultReceiver != null) {
                resultReceiver.send(0, new Bundle());
            }
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_UNLINK_COMPLETE));
            if (resultReceiver != null) {
                resultReceiver.send(2, new Bundle());
            } else {
                showToast(R.string.contactUnlinkedToast);
            }
        }
    }

    private boolean buildSplitTwoContacts(ArrayList<ContentProviderOperation> arrayList, long[] jArr, long[] jArr2, boolean z) {
        if (jArr == null || jArr2 == null) {
            Log.e(TAG, "Invalid arguments for splitContact request");
            return false;
        }
        ContentResolver contentResolver = getContentResolver();
        for (long j : jArr) {
            for (long j2 : jArr2) {
                buildSplitContactDiff(arrayList, j, j2, z);
                if (arrayList.size() > 0 && arrayList.size() % MAX_CONTACTS_PROVIDER_BATCH_SIZE == 0) {
                    if (!applyOperations(contentResolver, arrayList)) {
                        return false;
                    }
                    arrayList.clear();
                }
            }
        }
        return true;
    }

    public static Intent createJoinContactsIntent(Context context, long j, long j2, Class<? extends Activity> cls, String str) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_JOIN_CONTACTS);
        intent.putExtra(EXTRA_CONTACT_ID1, j);
        intent.putExtra(EXTRA_CONTACT_ID2, j2);
        Intent intent2 = new Intent(context, cls);
        intent2.setAction(str);
        intent.putExtra(EXTRA_CALLBACK_INTENT, intent2);
        return intent;
    }

    public static Intent createJoinSeveralContactsIntent(Context context, long[] jArr, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, (Class<?>) ContactSaveService.class);
        intent.setAction(ACTION_JOIN_SEVERAL_CONTACTS);
        intent.putExtra(EXTRA_CONTACT_IDS, jArr);
        intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
        return intent;
    }

    public static Intent createJoinSeveralContactsIntent(Context context, long[] jArr) {
        return createJoinSeveralContactsIntent(context, jArr, null);
    }

    private void joinSeveralContacts(Intent intent) {
        int i;
        long[] longArrayExtra = intent.getLongArrayExtra(EXTRA_CONTACT_IDS);
        if (!ContactSaveServiceEx.containSimContact(longArrayExtra, getContentResolver())) {
            ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
            long[] rawContactIdsForAggregation = getRawContactIdsForAggregation(longArrayExtra);
            ?? separatedRawContactIds = getSeparatedRawContactIds(longArrayExtra);
            if (rawContactIdsForAggregation == null) {
                Log.e(TAG, "Invalid arguments for joinSeveralContacts request");
                if (resultReceiver != null) {
                    resultReceiver.send(3, new Bundle());
                    return;
                }
                return;
            }
            ContentResolver contentResolver = getContentResolver();
            ArrayList<ContentProviderOperation> arrayList = new ArrayList<>(MAX_CONTACTS_PROVIDER_BATCH_SIZE);
            for (int i2 = 0; i2 < rawContactIdsForAggregation.length; i2++) {
                for (int i3 = 0; i3 < rawContactIdsForAggregation.length; i3 = i + 1) {
                    if (i2 != i3) {
                        i = i3;
                        buildJoinContactDiff(arrayList, rawContactIdsForAggregation[i2], rawContactIdsForAggregation[i3]);
                    } else {
                        i = i3;
                    }
                    if (arrayList.size() > 0 && arrayList.size() % MAX_CONTACTS_PROVIDER_BATCH_SIZE == 0) {
                        if (!applyOperations(contentResolver, arrayList)) {
                            if (resultReceiver != null) {
                                resultReceiver.send(0, new Bundle());
                                return;
                            }
                            return;
                        }
                        arrayList.clear();
                    }
                }
            }
            if (arrayList.size() > 0 && !applyOperations(contentResolver, arrayList)) {
                if (resultReceiver != null) {
                    resultReceiver.send(0, new Bundle());
                    return;
                }
                return;
            }
            String strQueryNameOfLinkedContacts = queryNameOfLinkedContacts(longArrayExtra);
            if (strQueryNameOfLinkedContacts != null) {
                if (resultReceiver != null) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(EXTRA_RAW_CONTACT_IDS, separatedRawContactIds);
                    bundle.putString(EXTRA_DISPLAY_NAME, strQueryNameOfLinkedContacts);
                    resultReceiver.send(1, bundle);
                } else if (TextUtils.isEmpty(strQueryNameOfLinkedContacts)) {
                    showToast(R.string.contactsJoinedMessage);
                } else {
                    showToast(R.string.contactsJoinedNamedMessage, strQueryNameOfLinkedContacts);
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_LINK_COMPLETE));
                return;
            }
            if (resultReceiver != null) {
                resultReceiver.send(0, new Bundle());
            }
            showToast(R.string.contactJoinErrorToast);
            return;
        }
        showToast(R.string.batch_merge_sim_contacts_warning);
    }

    private String queryNameOfLinkedContacts(long[] jArr) {
        String string;
        String string2;
        StringBuilder sb = new StringBuilder("_id");
        sb.append(" IN (");
        String[] strArr = new String[jArr.length];
        for (int i = 0; i < jArr.length; i++) {
            strArr[i] = String.valueOf(jArr[i]);
            sb.append("?,");
        }
        sb.deleteCharAt(sb.length() - 1).append(')');
        Cursor cursorQuery = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{"_id", "display_name", "display_name_alt"}, sb.toString(), strArr, null);
        long j = 0;
        try {
            if (cursorQuery.moveToFirst()) {
                j = cursorQuery.getLong(0);
                string = cursorQuery.getString(1);
                string2 = cursorQuery.getString(2);
            } else {
                string = null;
                string2 = null;
            }
            while (cursorQuery.moveToNext()) {
                if (cursorQuery.getLong(0) != j) {
                    return null;
                }
            }
            String preferredDisplayName = ContactDisplayUtils.getPreferredDisplayName(string, string2, new ContactsPreferences(getApplicationContext()));
            if (preferredDisplayName == null) {
                preferredDisplayName = "";
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return preferredDisplayName;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    private boolean applyOperations(ContentResolver contentResolver, ArrayList<ContentProviderOperation> arrayList) {
        try {
            ContentProviderResult[] contentProviderResultArrApplyBatch = contentResolver.applyBatch("com.android.contacts", arrayList);
            for (ContentProviderResult contentProviderResult : contentProviderResultArrApplyBatch) {
                if (contentProviderResult.count.intValue() < 0) {
                    throw new OperationApplicationException();
                }
            }
            return true;
        } catch (OperationApplicationException | RemoteException e) {
            FeedbackHelper.sendFeedback(this, TAG, "Failed to apply aggregation exception batch", e);
            showToast(R.string.contactSavedErrorToast);
            return false;
        }
    }

    private void joinContacts(Intent intent) {
        long j;
        int i;
        int i2;
        int i3;
        long longExtra = intent.getLongExtra(EXTRA_CONTACT_ID1, -1L);
        long longExtra2 = intent.getLongExtra(EXTRA_CONTACT_ID2, -1L);
        long[] rawContactIdsForAggregation = getRawContactIdsForAggregation(longExtra, longExtra2);
        if (rawContactIdsForAggregation == null) {
            Log.e(TAG, "Invalid arguments for joinContacts request");
            return;
        }
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        int i4 = 0;
        int i5 = 0;
        while (i5 < rawContactIdsForAggregation.length) {
            int i6 = i4;
            while (i6 < rawContactIdsForAggregation.length) {
                if (i5 != i6) {
                    i = i6;
                    i2 = i4;
                    i3 = i5;
                    buildJoinContactDiff(arrayList, rawContactIdsForAggregation[i5], rawContactIdsForAggregation[i6]);
                } else {
                    i = i6;
                    i2 = i4;
                    i3 = i5;
                }
                if (arrayList.size() > MAX_OPERATIONS_SIZE) {
                    ContactSaveServiceEx.bufferOperations(arrayList, getContentResolver());
                }
                i6 = i + 1;
                i4 = i2;
                i5 = i3;
            }
            i5++;
        }
        int i7 = i4;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursorQuery = contentResolver.query(Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, longExtra), "entities"), ContactEntityQuery.PROJECTION, ContactEntityQuery.SELECTION, null, null);
        if (cursorQuery == null) {
            Log.e(TAG, "Unable to open Contacts DB cursor");
            showToast(R.string.contactSavedErrorToast);
            return;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                j = cursorQuery.getLong(i7);
            } else {
                j = -1;
            }
            cursorQuery.close();
            if (j != -1) {
                ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, j));
                builderNewUpdate.withValue("is_super_primary", 1);
                builderNewUpdate.withValue("is_primary", 1);
                arrayList.add(builderNewUpdate.build());
            }
            boolean zApplyOperations = applyOperations(contentResolver, arrayList);
            long[] jArr = new long[2];
            jArr[i7] = longExtra;
            jArr[1] = longExtra2;
            String strQueryNameOfLinkedContacts = queryNameOfLinkedContacts(jArr);
            Intent intent2 = (Intent) intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
            if (zApplyOperations && strQueryNameOfLinkedContacts != null) {
                if (TextUtils.isEmpty(strQueryNameOfLinkedContacts)) {
                    showToast(R.string.contactsJoinedMessage);
                } else {
                    Object[] objArr = new Object[1];
                    objArr[i7] = strQueryNameOfLinkedContacts;
                    showToast(R.string.contactsJoinedNamedMessage, objArr);
                }
                intent2.setData(ContactsContract.RawContacts.getContactLookupUri(contentResolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactIdsForAggregation[i7])));
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_LINK_COMPLETE));
            }
            deliverCallback(intent2);
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    private long[][] getSeparatedRawContactIds(long[] jArr) {
        long[][] jArr2 = new long[jArr.length][];
        for (int i = 0; i < jArr.length; i++) {
            jArr2[i] = getRawContactIds(jArr[i]);
        }
        return jArr2;
    }

    private long[] getRawContactIds(long j) {
        Cursor cursorQuery = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, JoinContactQuery.PROJECTION, "contact_id=" + String.valueOf(j), null, null);
        if (cursorQuery == null) {
            Log.e(TAG, "Unable to open Contacts DB cursor");
            return null;
        }
        try {
            long[] jArr = new long[cursorQuery.getCount()];
            for (int i = 0; i < jArr.length; i++) {
                cursorQuery.moveToPosition(i);
                jArr[i] = cursorQuery.getLong(0);
            }
            return jArr;
        } finally {
            cursorQuery.close();
        }
    }

    private long[] getRawContactIdsForAggregation(long[] jArr) {
        Cursor cursorQuery;
        if (jArr == null) {
            return null;
        }
        ContentResolver contentResolver = getContentResolver();
        StringBuilder sb = new StringBuilder();
        String[] strArr = new String[jArr.length];
        for (int i = 0; i < jArr.length; i++) {
            sb.append("contact_id=?");
            strArr[i] = String.valueOf(jArr[i]);
            if (jArr[i] == -1) {
                return null;
            }
            if (i != jArr.length - 1) {
                sb.append(" OR ");
            }
        }
        try {
            cursorQuery = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, JoinContactQuery.PROJECTION, sb.toString(), strArr, null);
        } catch (Exception e) {
            Log.e(TAG, "JoinContactQuery fail:", e);
            cursorQuery = null;
        }
        if (cursorQuery == null) {
            Log.e(TAG, "Unable to open Contacts DB cursor");
            showToast(R.string.contactSavedErrorToast);
            return null;
        }
        try {
            if (cursorQuery.getCount() < 2) {
                Log.e(TAG, "Not enough raw contacts to aggregate together.");
                return null;
            }
            long[] jArr2 = new long[cursorQuery.getCount()];
            for (int i2 = 0; i2 < jArr2.length; i2++) {
                cursorQuery.moveToPosition(i2);
                jArr2[i2] = cursorQuery.getLong(0);
            }
            return jArr2;
        } finally {
            cursorQuery.close();
        }
    }

    private long[] getRawContactIdsForAggregation(long j, long j2) {
        return getRawContactIdsForAggregation(new long[]{j, j2});
    }

    private void buildJoinContactDiff(ArrayList<ContentProviderOperation> arrayList, long j, long j2) {
        ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        builderNewUpdate.withValue(BaseAccountType.Attr.TYPE, 1);
        builderNewUpdate.withValue("raw_contact_id1", Long.valueOf(j));
        builderNewUpdate.withValue("raw_contact_id2", Long.valueOf(j2));
        arrayList.add(builderNewUpdate.build());
    }

    private void buildSplitContactDiff(ArrayList<ContentProviderOperation> arrayList, long j, long j2, boolean z) {
        int i;
        ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        if (z) {
            i = 2;
        } else {
            i = 0;
        }
        builderNewUpdate.withValue(BaseAccountType.Attr.TYPE, Integer.valueOf(i));
        builderNewUpdate.withValue("raw_contact_id1", Long.valueOf(j));
        builderNewUpdate.withValue("raw_contact_id2", Long.valueOf(j2));
        arrayList.add(builderNewUpdate.build());
    }

    public static Intent createSleepIntent(Context context, long j) {
        return new Intent(context, (Class<?>) ContactSaveService.class).setAction(ACTION_SLEEP).putExtra(EXTRA_SLEEP_DURATION, j);
    }

    private void sleepForDebugging(Intent intent) {
        long longExtra = intent.getLongExtra(EXTRA_SLEEP_DURATION, 1000L);
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "sleeping for " + longExtra + "ms");
        }
        try {
            Thread.sleep(longExtra);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "finished sleeping");
        }
    }

    private void showToast(int i, Object... objArr) {
        final String string = getResources().getString(i, objArr);
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactSaveService.this, string, 1).show();
            }
        });
    }

    private void showToast(final int i) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactSaveService.this, i, 1).show();
            }
        });
    }

    private void deliverCallback(final Intent intent) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(ContactSaveService.TAG, "[deliverCallback]run");
                ContactSaveService.this.deliverCallbackOnUiThread(intent);
            }
        });
    }

    void deliverCallbackOnUiThread(Intent intent) {
        for (Listener listener : sListeners) {
            if (intent.getComponent().equals(((Activity) listener).getIntent().getComponent())) {
                Log.d(TAG, "[deliverCallbackOnUiThread]listener.onServiceCompleted");
                listener.onServiceCompleted(intent);
                return;
            }
        }
    }

    public static void setDeleteEndListener(DeleteEndListener deleteEndListener) {
        sDeleteEndListener = deleteEndListener;
    }

    public static void removeDeleteEndListener(DeleteEndListener deleteEndListener) {
        sDeleteEndListener = null;
    }

    public static synchronized boolean isGroupTransactionProcessing() {
        return sIsTransactionProcessing;
    }

    public static synchronized void setGroupTransactionProcessing(boolean z) {
        sIsTransactionProcessing = z;
    }

    public static class GroupsDaoImpl implements GroupsDao {
        public static final String KEY_GROUP_DATA = "groupData";
        public static final String KEY_GROUP_MEMBERS = "groupMemberIds";
        private static final String TAG = "GroupsDao";
        private final ContentResolver contentResolver;
        private final Context context;

        public GroupsDaoImpl(Context context) {
            this(context, context.getContentResolver());
        }

        public GroupsDaoImpl(Context context, ContentResolver contentResolver) {
            this.context = context;
            this.contentResolver = contentResolver;
        }

        @Override
        public Bundle captureDeletionUndoData(Uri uri) {
            long id = ContentUris.parseId(uri);
            Bundle bundle = new Bundle();
            Cursor cursorQuery = this.contentResolver.query(uri, new String[]{"title", "notes", "group_visible", "account_type", "account_name", "data_set", "should_sync"}, "deleted=?", new String[]{"0"}, null);
            try {
                if (cursorQuery.moveToFirst()) {
                    ContentValues contentValues = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursorQuery, contentValues);
                    bundle.putParcelable(KEY_GROUP_DATA, contentValues);
                    cursorQuery.close();
                    Cursor cursorQuery2 = this.contentResolver.query(ContactsContract.Data.CONTENT_URI, new String[]{"raw_contact_id"}, "mimetype=? AND data1=?", new String[]{"vnd.android.cursor.item/group_membership", String.valueOf(id)}, null);
                    long[] jArr = new long[cursorQuery2.getCount()];
                    int i = 0;
                    while (cursorQuery2.moveToNext()) {
                        jArr[i] = cursorQuery2.getLong(0);
                        i++;
                    }
                    bundle.putLongArray(KEY_GROUP_MEMBERS, jArr);
                    return bundle;
                }
                return bundle;
            } finally {
                cursorQuery.close();
            }
        }

        @Override
        public Uri undoDeletion(Bundle bundle) {
            ContentValues contentValues = (ContentValues) bundle.getParcelable(KEY_GROUP_DATA);
            if (contentValues == null) {
                return null;
            }
            Uri uriInsert = this.contentResolver.insert(ContactsContract.Groups.CONTENT_URI, contentValues);
            long id = ContentUris.parseId(uriInsert);
            long[] longArray = bundle.getLongArray(KEY_GROUP_MEMBERS);
            if (longArray == null) {
                return uriInsert;
            }
            ContentValues[] contentValuesArr = new ContentValues[longArray.length];
            for (int i = 0; i < longArray.length; i++) {
                contentValuesArr[i] = new ContentValues();
                contentValuesArr[i].put("raw_contact_id", Long.valueOf(longArray[i]));
                contentValuesArr[i].put("mimetype", "vnd.android.cursor.item/group_membership");
                contentValuesArr[i].put("data1", Long.valueOf(id));
            }
            if (this.contentResolver.bulkInsert(ContactsContract.Data.CONTENT_URI, contentValuesArr) != longArray.length) {
                Log.e(TAG, "Could not recover some members for group deletion undo");
            }
            return uriInsert;
        }

        @Override
        public Uri create(String str, AccountWithDataSet accountWithDataSet) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", str);
            contentValues.put("account_name", accountWithDataSet.name);
            contentValues.put("account_type", accountWithDataSet.type);
            contentValues.put("data_set", accountWithDataSet.dataSet);
            return this.contentResolver.insert(ContactsContract.Groups.CONTENT_URI, contentValues);
        }

        @Override
        public int delete(Uri uri) {
            return this.contentResolver.delete(uri, null, null);
        }

        protected Context getContext() {
            return this.context;
        }
    }

    public static class State {
        private final CopyOnWriteArrayList<Intent> mPending;

        public State() {
            this.mPending = new CopyOnWriteArrayList<>();
        }

        public State(Collection<Intent> collection) {
            this.mPending = new CopyOnWriteArrayList<>(collection);
        }

        public boolean isIdle() {
            return this.mPending.isEmpty();
        }

        public Intent getCurrentIntent() {
            if (this.mPending.isEmpty()) {
                return null;
            }
            return this.mPending.get(0);
        }

        public Intent getNextIntentWithAction(String str) {
            for (Intent intent : this.mPending) {
                if (str.equals(intent.getAction())) {
                    return intent;
                }
            }
            return null;
        }

        public boolean isActionPending(String str) {
            return getNextIntentWithAction(str) != null;
        }

        private void onFinish(Intent intent) {
            if (!this.mPending.isEmpty() && this.mPending.get(0).getAction().equals(intent.getAction())) {
                this.mPending.remove(0);
            }
        }

        private void onStart(Intent intent) {
            if (intent.getAction() == null) {
                return;
            }
            this.mPending.add(intent);
        }
    }
}
