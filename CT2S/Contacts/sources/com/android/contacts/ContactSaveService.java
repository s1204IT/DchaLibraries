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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;
import com.android.contacts.common.ContactTileLoaderFactory;
import com.android.contacts.common.database.ContactUpdateUtils;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import com.android.contacts.util.ContactPhotoUtils;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContactSaveService extends IntentService {
    private static final HashSet<String> ALLOWED_DATA_COLUMNS = Sets.newHashSet(new String[]{"mimetype", "is_primary", "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10", "data11", "data12", "data13", "data14", "data15"});
    private static final CopyOnWriteArrayList<Listener> sListeners = new CopyOnWriteArrayList<>();
    private Handler mMainHandler;
    private Intent simLoadcallbackIntent;

    private interface JoinContactQuery {
        public static final String[] PROJECTION = {"_id", "contact_id", "name_verified", "display_name_source"};
    }

    public interface Listener {
        void onServiceCompleted(Intent intent);
    }

    public ContactSaveService() {
        super("ContactSaveService");
        this.simLoadcallbackIntent = null;
        setIntentRedelivery(true);
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    public static void registerListener(Listener listener) {
        if (!(listener instanceof Activity)) {
            throw new ClassCastException("Only activities can be registered to receive callback from " + ContactSaveService.class.getName());
        }
        sListeners.add(0, listener);
    }

    public static void unregisterListener(Listener listener) {
        sListeners.remove(listener);
    }

    @Override
    public Object getSystemService(String name) {
        Object service = super.getSystemService(name);
        return service != null ? service : getApplicationContext().getSystemService(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.d("ContactSaveService", "onHandleIntent: could not handle null intent");
            return;
        }
        String action = intent.getAction();
        if ("newRawContact".equals(action)) {
            createRawContact(intent);
            return;
        }
        if ("saveContact".equals(action)) {
            saveContact(intent);
            return;
        }
        if ("createGroup".equals(action)) {
            createGroup(intent);
            return;
        }
        if ("renameGroup".equals(action)) {
            renameGroup(intent);
            return;
        }
        if ("deleteGroup".equals(action)) {
            deleteGroup(intent);
            return;
        }
        if ("updateGroup".equals(action)) {
            updateGroup(intent);
            return;
        }
        if ("setStarred".equals(action)) {
            setStarred(intent);
            return;
        }
        if ("setSuperPrimary".equals(action)) {
            setSuperPrimary(intent);
            return;
        }
        if ("clearPrimary".equals(action)) {
            clearPrimary(intent);
            return;
        }
        if ("delete".equals(action)) {
            deleteContact(intent);
            return;
        }
        if ("joinContacts".equals(action)) {
            joinContacts(intent);
            return;
        }
        if ("sendToVoicemail".equals(action)) {
            setSendToVoicemail(intent);
        } else if ("setRingtone".equals(action)) {
            setRingtone(intent);
        } else if ("saveSimContact".equals(action)) {
            saveSimContact(intent);
        }
    }

    public static Intent createNewRawContactIntent(Context context, ArrayList<ContentValues> values, AccountWithDataSet account, Class<? extends Activity> callbackActivity, String callbackAction) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("newRawContact");
        if (account != null) {
            serviceIntent.putExtra("accountName", account.name);
            serviceIntent.putExtra("accountType", account.type);
            serviceIntent.putExtra("dataSet", account.dataSet);
        }
        serviceIntent.putParcelableArrayListExtra("contentValues", values);
        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(callbackAction);
        serviceIntent.putExtra("callbackIntent", callbackIntent);
        return serviceIntent;
    }

    private void createRawContact(Intent intent) {
        String accountName = intent.getStringExtra("accountName");
        String accountType = intent.getStringExtra("accountType");
        String dataSet = intent.getStringExtra("dataSet");
        List<ContentValues> valueList = intent.getParcelableArrayListExtra("contentValues");
        Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue("account_name", accountName).withValue("account_type", accountType).withValue("data_set", dataSet).build());
        int size = valueList.size();
        for (int i = 0; i < size; i++) {
            ContentValues values = valueList.get(i);
            values.keySet().retainAll(ALLOWED_DATA_COLUMNS);
            operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference("raw_contact_id", 0).withValues(values).build());
        }
        ContentResolver resolver = getContentResolver();
        try {
            ContentProviderResult[] results = resolver.applyBatch("com.android.contacts", operations);
            Uri rawContactUri = results[0].uri;
            callbackIntent.setData(ContactsContract.RawContacts.getContactLookupUri(resolver, rawContactUri));
            deliverCallback(callbackIntent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store new contact", e);
        }
    }

    public static Intent createSaveContactIntent(Context context, RawContactDeltaList state, RawContactDeltaList stateOld, String saveModeExtraKey, int saveMode, boolean isProfile, Class<? extends Activity> callbackActivity, String callbackAction, long rawContactId, Uri updatedPhotoPath) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(String.valueOf(rawContactId), updatedPhotoPath);
        return createSaveContactIntent(context, state, stateOld, saveModeExtraKey, saveMode, isProfile, callbackActivity, callbackAction, bundle);
    }

    public static Intent createSaveContactIntent(Context context, RawContactDeltaList state, RawContactDeltaList stateOld, String saveModeExtraKey, int saveMode, boolean isProfile, Class<? extends Activity> callbackActivity, String callbackAction, Bundle updatedPhotos) {
        return createSaveContactIntent(0, context, state, (RawContactDeltaList) null, saveModeExtraKey, saveMode, isProfile, callbackActivity, callbackAction, updatedPhotos);
    }

    public static Intent createSaveContactIntent(int slotId, Context context, RawContactDeltaList state, RawContactDeltaList stateOld, String saveModeExtraKey, int saveMode, boolean isProfile, Class<? extends Activity> callbackActivity, String callbackAction, Bundle updatedPhotos) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.putExtra("state", (Parcelable) state);
        String accountType = state.get(0).getValues().getAsString("account_type");
        if (accountType != null && (accountType.equals("com.android.contact.sim") || accountType.equals("com.android.contact.sim2"))) {
            serviceIntent.setAction("saveSimContact");
            serviceIntent.putExtra("stateOld", (Parcelable) stateOld);
            serviceIntent.putExtra("slot", slotId);
        } else {
            serviceIntent.setAction("saveContact");
        }
        serviceIntent.putExtra("saveIsProfile", isProfile);
        if (updatedPhotos != null) {
            serviceIntent.putExtra("updatedPhotos", (Parcelable) updatedPhotos);
        }
        if (callbackActivity != null) {
            Intent callbackIntent = new Intent(context, callbackActivity);
            callbackIntent.putExtra(saveModeExtraKey, saveMode);
            callbackIntent.setAction(callbackAction);
            serviceIntent.putExtra("callbackIntent", callbackIntent);
        }
        return serviceIntent;
    }

    private void saveContact(Intent intent) {
        RawContactDeltaList state = (RawContactDeltaList) intent.getParcelableExtra("state");
        boolean isProfile = intent.getBooleanExtra("saveIsProfile", false);
        Bundle updatedPhotos = (Bundle) intent.getParcelableExtra("updatedPhotos");
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        RawContactModifier.trimEmpty(state, accountTypes);
        Uri lookupUri = null;
        ContentResolver resolver = getContentResolver();
        boolean succeeded = false;
        long insertedRawContactId = -1;
        int tries = 0;
        while (true) {
            int tries2 = tries;
            tries = tries2 + 1;
            if (tries2 >= 3) {
                break;
            }
            try {
                ArrayList<ContentProviderOperation> diff = state.buildDiff();
                Log.v("ContactSaveService", "Content Provider Operations:");
                for (ContentProviderOperation operation : diff) {
                    Log.v("ContactSaveService", operation.toString());
                }
                ContentProviderResult[] results = null;
                if (!diff.isEmpty()) {
                    results = resolver.applyBatch("com.android.contacts", diff);
                }
                long rawContactId = getRawContactId(state, diff, results);
                if (rawContactId == -1) {
                    throw new IllegalStateException("Could not determine RawContact ID after save");
                }
                insertedRawContactId = getInsertedRawContactId(diff, results);
                if (isProfile) {
                    Cursor c = resolver.query(ContactsContract.Profile.CONTENT_URI, new String[]{"_id", "lookup"}, null, null, null);
                    try {
                        if (c.moveToFirst()) {
                            long contactId = c.getLong(0);
                            String lookupKey = c.getString(1);
                            lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
                        }
                    } finally {
                        c.close();
                    }
                } else {
                    Uri rawContactUri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
                    lookupUri = ContactsContract.RawContacts.getContactLookupUri(resolver, rawContactUri);
                }
                Log.v("ContactSaveService", "Saved contact. New URI: " + lookupUri);
                succeeded = true;
            } catch (OperationApplicationException e) {
                Log.w("ContactSaveService", "Version consistency failed, re-parenting: " + e.toString());
                StringBuilder sb = new StringBuilder("_id IN(");
                boolean first = true;
                int count = state.size();
                for (int i = 0; i < count; i++) {
                    Long rawContactId2 = state.getRawContactId(i);
                    if (rawContactId2 != null && rawContactId2.longValue() != -1) {
                        if (!first) {
                            sb.append(',');
                        }
                        sb.append(rawContactId2);
                        first = false;
                    }
                }
                sb.append(")");
                if (first) {
                    throw new IllegalStateException("Version consistency failed for a new contact", e);
                }
                RawContactDeltaList newState = RawContactDeltaList.fromQuery(isProfile ? ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI : ContactsContract.RawContactsEntity.CONTENT_URI, resolver, sb.toString(), null, null);
                state = RawContactDeltaList.mergeAfter(newState, state);
                if (isProfile) {
                    for (RawContactDelta delta : state) {
                        delta.setProfileQueryUri();
                    }
                }
            } catch (RemoteException e2) {
                Log.e("ContactSaveService", "Problem persisting user edits", e2);
            } catch (IllegalArgumentException e3) {
                Log.e("ContactSaveService", "Problem persisting user edits", e3);
                showToast(R.string.contactSavedErrorToast);
            }
        }
        if (updatedPhotos != null) {
            for (String key : updatedPhotos.keySet()) {
                Uri photoUri = (Uri) updatedPhotos.getParcelable(key);
                long rawContactId3 = Long.parseLong(key);
                if (rawContactId3 < 0) {
                    rawContactId3 = insertedRawContactId;
                    if (rawContactId3 == -1) {
                        throw new IllegalStateException("Could not determine RawContact ID for image insertion");
                    }
                }
                if (!saveUpdatedPhoto(rawContactId3, photoUri)) {
                    succeeded = false;
                }
            }
        }
        Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
        if (callbackIntent != null) {
            if (succeeded) {
                callbackIntent.putExtra("saveSucceeded", true);
            }
            callbackIntent.setData(lookupUri);
            deliverCallback(callbackIntent);
        }
    }

    private void saveSimContact(Intent intent) {
        int slotId;
        ArrayList<ContentProviderOperation> diff;
        ContentProviderResult[] results;
        boolean succeeded = false;
        if (intent.hasExtra("slot")) {
            slotId = intent.getIntExtra("slot", -1);
        } else {
            slotId = 0;
        }
        RawContactDeltaList state = (RawContactDeltaList) intent.getParcelableExtra("state");
        RawContactDeltaList stateOld = (RawContactDeltaList) intent.getParcelableExtra("stateOld");
        Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
        boolean isProfile = intent.getBooleanExtra("saveIsProfile", false);
        Log.v("ContactSaveService", "mState is old ");
        SimPhoneBookCommonUtil.AdnRecord oldAdnRec = SimPhonebookUtil.getSimAdnViaState(stateOld);
        Log.v("ContactSaveService", "--> oldAdnRec = ");
        Log.v("ContactSaveService", oldAdnRec.toString());
        String oldEmailsString = oldAdnRec.email + (oldAdnRec.email2.isEmpty() ? "" : "," + oldAdnRec.email2);
        Log.v("ContactSaveService", "--> oldEmailsString = " + oldEmailsString);
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        RawContactModifier.trimEmpty(state, accountTypes);
        Uri lookupUri = null;
        ContentResolver resolver = getContentResolver();
        int tries = 0;
        while (true) {
            int tries2 = tries + 1;
            if (tries >= 3) {
                break;
            }
            try {
                diff = state.buildDiff();
                Log.v("ContactSaveService", "---> state = " + state);
                Log.v("ContactSaveService", "---> diff = " + diff);
                Log.v("ContactSaveService", "Content Provider Operations:");
                for (ContentProviderOperation operation : diff) {
                    Log.v("ContactSaveService", operation.toString());
                }
                results = null;
            } catch (OperationApplicationException e) {
                Log.w("ContactSaveService", "Version consistency failed, re-parenting: " + e.toString());
                StringBuilder sb = new StringBuilder("_id IN(");
                boolean first = true;
                int count = state.size();
                for (int i = 0; i < count; i++) {
                    Long rawContactId = state.getRawContactId(i);
                    if (rawContactId != null && rawContactId.longValue() != -1) {
                        if (!first) {
                            sb.append(',');
                        }
                        sb.append(rawContactId);
                        first = false;
                    }
                }
                sb.append(")");
                if (first) {
                    throw new IllegalStateException("Version consistency failed for a new contact");
                }
                RawContactDeltaList newState = RawContactDeltaList.fromQuery(isProfile ? ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI : ContactsContract.RawContactsEntity.CONTENT_URI, resolver, sb.toString(), null, null);
                state = RawContactDeltaList.mergeAfter(newState, state);
                if (isProfile) {
                    for (RawContactDelta delta : state) {
                        delta.setProfileQueryUri();
                    }
                }
                tries = tries2;
            } catch (RemoteException e2) {
                Log.e("ContactSaveService", "Problem persisting user edits", e2);
            }
            if (diff.isEmpty()) {
                break;
            }
            Log.v("ContactSaveService", "mState is new ");
            SimPhoneBookCommonUtil.AdnRecord newAdnRec = SimPhonebookUtil.getSimAdnViaState(state);
            Log.v("ContactSaveService", "--> newAdnRec = ");
            Log.v("ContactSaveService", newAdnRec.toString());
            String newEmailsString = newAdnRec.email + (newAdnRec.email2.isEmpty() ? "" : "," + newAdnRec.email2);
            Log.v("ContactSaveService", "--> newEmailsString = " + newEmailsString);
            if (!SimPhoneBookCommonUtil.isPatternMatch("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*", newAdnRec.email) || !SimPhoneBookCommonUtil.isPatternMatch("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*", newAdnRec.email2)) {
                break;
            }
            String oldGrpString = "";
            if (stateOld != null) {
                long oldRawContactId = stateOld.findRawContactId();
                Log.d("ContactSaveService", "---> oldRawContactId = " + oldRawContactId);
                if (oldRawContactId == -1) {
                    throw new IllegalStateException("Could not determine RawContact ID after save");
                }
                oldGrpString = SimPhoneBookCommonUtil.getSimGrpStringViaRawId(resolver, oldRawContactId);
            }
            Log.v("ContactSaveService", "---> oldGrpString = " + oldGrpString);
            if ("".equals(newAdnRec.name) && "".equals(newAdnRec.phone) && !"".equals(newEmailsString)) {
                showToast(R.string.contactSavedOnlyWithEmailErrorToast);
                break;
            } else {
                if (SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), oldAdnRec.name, oldAdnRec.phone, oldEmailsString, oldAdnRec.phone2, oldAdnRec.name_sne, oldGrpString, newAdnRec.name, newAdnRec.phone, newEmailsString, newAdnRec.phone2, newAdnRec.name_sne, oldGrpString)) {
                    results = resolver.applyBatch("com.android.contacts", diff);
                    break;
                }
                tries = tries2;
            }
        }
        if (callbackIntent != null) {
            if (succeeded) {
                callbackIntent.putExtra("saveSucceeded", true);
            }
            callbackIntent.setData(lookupUri);
            deliverCallback(callbackIntent);
        }
    }

    private boolean saveUpdatedPhoto(long rawContactId, Uri photoUri) {
        Uri outputUri = Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId), "display_photo");
        return ContactPhotoUtils.savePhotoFromUriToUri(this, photoUri, outputUri, true);
    }

    private long getRawContactId(RawContactDeltaList state, ArrayList<ContentProviderOperation> diff, ContentProviderResult[] results) {
        long existingRawContactId = state.findRawContactId();
        return existingRawContactId != -1 ? existingRawContactId : getInsertedRawContactId(diff, results);
    }

    private long getInsertedRawContactId(ArrayList<ContentProviderOperation> diff, ContentProviderResult[] results) {
        if (results == null) {
            return -1L;
        }
        int diffSize = diff.size();
        int numResults = results.length;
        for (int i = 0; i < diffSize && i < numResults; i++) {
            ContentProviderOperation operation = diff.get(i);
            if (operation.getType() == 1 && operation.getUri().getEncodedPath().contains(ContactsContract.RawContacts.CONTENT_URI.getEncodedPath())) {
                return ContentUris.parseId(results[i].uri);
            }
        }
        return -1L;
    }

    public static Intent createNewGroupIntent(Context context, AccountWithDataSet account, String label, long[] rawContactsToAdd, Class<? extends Activity> callbackActivity, String callbackAction) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("createGroup");
        serviceIntent.putExtra("accountType", account.type);
        serviceIntent.putExtra("accountName", account.name);
        serviceIntent.putExtra("dataSet", account.dataSet);
        serviceIntent.putExtra("groupLabel", label);
        serviceIntent.putExtra("rawContactsToAdd", rawContactsToAdd);
        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(callbackAction);
        serviceIntent.putExtra("callbackIntent", callbackIntent);
        return serviceIntent;
    }

    private void createGroup(Intent intent) {
        Log.d("ContactSaveService", " createGroup ()");
        String accountType = intent.getStringExtra("accountType");
        String accountName = intent.getStringExtra("accountName");
        String dataSet = intent.getStringExtra("dataSet");
        String label = intent.getStringExtra("groupLabel");
        long[] rawContactsToAdd = intent.getLongArrayExtra("rawContactsToAdd");
        ContentValues values = new ContentValues();
        values.put("account_type", accountType);
        values.put("account_name", accountName);
        values.put("data_set", dataSet);
        values.put("title", label);
        ContentResolver resolver = getContentResolver();
        Uri groupUri = resolver.insert(ContactsContract.Groups.CONTENT_URI, values);
        if (groupUri == null) {
            Log.e("ContactSaveService", "Couldn't create group with label " + label);
            return;
        }
        addMembersToGroup(resolver, rawContactsToAdd, ContentUris.parseId(groupUri));
        values.clear();
        values.put("mimetype", "vnd.android.cursor.item/group_membership");
        values.put("data1", Long.valueOf(ContentUris.parseId(groupUri)));
        Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
        callbackIntent.setData(groupUri);
        callbackIntent.putExtra("data", Lists.newArrayList(new ContentValues[]{values}));
        deliverCallback(callbackIntent);
    }

    private void renameGroup(Intent intent) {
        long groupId = intent.getLongExtra("groupId", -1L);
        String label = intent.getStringExtra("groupLabel");
        if (groupId == -1) {
            Log.e("ContactSaveService", "Invalid arguments for renameGroup request");
            return;
        }
        ContentValues values = new ContentValues();
        values.put("title", label);
        Uri groupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId);
        getContentResolver().update(groupUri, values, null, null);
        Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
        callbackIntent.setData(groupUri);
        deliverCallback(callbackIntent);
    }

    public static Intent createGroupDeletionIntent(Context context, long groupId) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("deleteGroup");
        serviceIntent.putExtra("groupId", groupId);
        return serviceIntent;
    }

    private void deleteGroup(Intent intent) {
        long groupId = intent.getLongExtra("groupId", -1L);
        if (groupId == -1) {
            Log.e("ContactSaveService", "Invalid arguments for deleteGroup request");
        } else {
            getContentResolver().delete(ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId), null, null);
        }
    }

    public static Intent createGroupUpdateIntent(Context context, long groupId, String newLabel, long[] rawContactsToAdd, long[] rawContactsToRemove, Class<? extends Activity> callbackActivity, String callbackAction, boolean isSimGroup) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("updateGroup");
        serviceIntent.putExtra("groupId", groupId);
        serviceIntent.putExtra("groupLabel", newLabel);
        serviceIntent.putExtra("rawContactsToAdd", rawContactsToAdd);
        serviceIntent.putExtra("rawContactsToRemove", rawContactsToRemove);
        serviceIntent.putExtra("isSimGroup", isSimGroup);
        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(callbackAction);
        serviceIntent.putExtra("callbackIntent", callbackIntent);
        return serviceIntent;
    }

    private void updateGroup(Intent intent) {
        int[] subs;
        Log.d("ContactSaveService", "updateGroup()");
        long groupId = intent.getLongExtra("groupId", -1L);
        String label = intent.getStringExtra("groupLabel");
        long[] rawContactsToAdd = intent.getLongArrayExtra("rawContactsToAdd");
        long[] rawContactsToRemove = intent.getLongArrayExtra("rawContactsToRemove");
        boolean isSimGroup = intent.getBooleanExtra("isSimGroup", false);
        if (groupId == -1) {
            Log.e("ContactSaveService", "Invalid arguments for updateGroup request");
            return;
        }
        ContentResolver resolver = getContentResolver();
        Uri groupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId);
        if (label != null) {
            if (isSimGroup) {
                Cursor grpCursor = resolver.query(ContactsContract.Groups.CONTENT_URI, new String[]{"sourceid", "account_type"}, "_id=" + groupId, null, null);
                if (grpCursor != null && grpCursor.moveToFirst() && grpCursor.getString(0) != null) {
                    String grpId = grpCursor.getString(0);
                    ContentValues cv = new ContentValues();
                    cv.put("grpTag", label);
                    cv.put("grpId", grpId);
                    Log.v("ContactSaveService", "update gsd");
                    Uri adnGrpUri = null;
                    if (grpCursor.getString(0) != null && grpCursor.getString(1).equals("com.android.contact.sim")) {
                        int[] subs2 = SubscriptionManager.getSubId(0);
                        if (subs2 != null) {
                            adnGrpUri = ContentUris.withAppendedId(ContactTileLoaderFactory.ADN_GRP_URI_SUB, subs2[0]);
                        }
                    } else if (grpCursor.getString(0) != null && grpCursor.getString(1).equals("com.android.contact.sim2") && (subs = SubscriptionManager.getSubId(1)) != null) {
                        adnGrpUri = ContentUris.withAppendedId(ContactTileLoaderFactory.ADN_GRP_URI_SUB, subs[0]);
                    }
                    int ret = 0;
                    if (adnGrpUri != null) {
                        ret = resolver.update(adnGrpUri, cv, null, null);
                    }
                    if (ret > 0) {
                        ContentValues values = new ContentValues();
                        values.put("title", label);
                        resolver.update(groupUri, values, null, null);
                    } else {
                        showToast(R.string.groupTagSavedErrorToast);
                    }
                }
                if (grpCursor != null) {
                    grpCursor.close();
                }
            } else {
                ContentValues values2 = new ContentValues();
                values2.put("title", label);
                resolver.update(groupUri, values2, null, null);
            }
        }
        addMembersToGroup(resolver, rawContactsToAdd, groupId);
        removeMembersFromGroup(resolver, rawContactsToRemove, groupId);
        Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
        callbackIntent.setData(groupUri);
        deliverCallback(callbackIntent);
    }

    private void addMembersToGroup(ContentResolver resolver, long[] rawContactsToAdd, long groupId) {
        if (rawContactsToAdd != null) {
            for (long rawContactId : rawContactsToAdd) {
                try {
                    ArrayList<ContentProviderOperation> rawContactOperations = new ArrayList<>();
                    ContentProviderOperation.Builder assertBuilder = ContentProviderOperation.newAssertQuery(ContactsContract.Data.CONTENT_URI);
                    assertBuilder.withSelection("raw_contact_id=? AND mimetype=? AND data1=?", new String[]{String.valueOf(rawContactId), "vnd.android.cursor.item/group_membership", String.valueOf(groupId)});
                    assertBuilder.withExpectedCount(0);
                    rawContactOperations.add(assertBuilder.build());
                    Cursor rawContactIdCursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"account_name"}, "_id=" + rawContactId, null, null);
                    if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                        String accountName = rawContactIdCursor.getString(0);
                        rawContactIdCursor.close();
                        if (accountName != null) {
                            if (accountName.equals(SimAccountType.ACCOUNT_NAME)) {
                                if (!addMembersToSimGroup(0, resolver, rawContactId, groupId)) {
                                }
                            } else if (!accountName.equals("Sim2") || addMembersToSimGroup(1, resolver, rawContactId, groupId)) {
                            }
                        }
                    } else {
                        if (rawContactIdCursor != null) {
                            rawContactIdCursor.close();
                        }
                        ContentProviderOperation.Builder insertBuilder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                        insertBuilder.withValue("raw_contact_id", Long.valueOf(rawContactId));
                        insertBuilder.withValue("mimetype", "vnd.android.cursor.item/group_membership");
                        insertBuilder.withValue("data1", Long.valueOf(groupId));
                        rawContactOperations.add(insertBuilder.build());
                        for (ContentProviderOperation operation : rawContactOperations) {
                            Log.v("ContactSaveService", operation.toString());
                        }
                        if (!rawContactOperations.isEmpty()) {
                            resolver.applyBatch("com.android.contacts", rawContactOperations);
                        }
                    }
                } catch (OperationApplicationException e) {
                    Log.w("ContactSaveService", "Assert failed in adding raw contact ID " + String.valueOf(rawContactId) + ". Already exists in group " + String.valueOf(groupId), e);
                } catch (RemoteException e2) {
                    Log.e("ContactSaveService", "Problem persisting user edits for raw contact ID " + String.valueOf(rawContactId), e2);
                }
            }
        }
    }

    private void removeMembersFromGroup(ContentResolver resolver, long[] rawContactsToRemove, long groupId) {
        if (rawContactsToRemove != null) {
            for (long rawContactId : rawContactsToRemove) {
                Cursor rawContactIdCursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"account_name"}, "_id=" + rawContactId, null, null);
                if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                    String accountName = rawContactIdCursor.getString(0);
                    rawContactIdCursor.close();
                    if (accountName != null) {
                        if (accountName.equals(SimAccountType.ACCOUNT_NAME)) {
                            if (!removeMembersFromSimGroup(0, resolver, rawContactId, groupId)) {
                            }
                        } else if (!accountName.equals("Sim2") || removeMembersFromSimGroup(1, resolver, rawContactId, groupId)) {
                        }
                    }
                } else {
                    if (rawContactIdCursor != null) {
                        rawContactIdCursor.close();
                    }
                    resolver.delete(ContactsContract.Data.CONTENT_URI, "raw_contact_id=? AND mimetype=? AND data1=?", new String[]{String.valueOf(rawContactId), "vnd.android.cursor.item/group_membership", String.valueOf(groupId)});
                }
            }
        }
    }

    private boolean addMembersToSimGroup(int slotId, ContentResolver resolver, long rawContactId, long groupId) {
        String groupIndex = "";
        SimPhoneBookCommonUtil.AdnRecord adnRec = SimPhoneBookCommonUtil.getSimAdnViaRawId(resolver, rawContactId, SimPhoneBookCommonUtil.GET_ADN_MODE1);
        String oldGrpString = SimPhoneBookCommonUtil.getSimGrpStringViaRawId(resolver, rawContactId);
        Log.d("ContactSaveService", "----> addMembersToSimGroup oldGrpString = " + oldGrpString);
        Cursor c = resolver.query(ContactsContract.Groups.CONTENT_URI, new String[]{"sourceid"}, "_id=" + groupId, null, null);
        if (c != null && c.moveToFirst()) {
            Log.d("ContactSaveService", "----> add group index = " + c.getString(0));
            groupIndex = c.getString(0);
        }
        String newGrpString = oldGrpString + groupIndex.toString();
        Log.d("ContactSaveService", "----> addMembersToSimGroup newGrpString = " + newGrpString);
        c.close();
        String emailsString = adnRec.email + (adnRec.email2.isEmpty() ? "" : "," + adnRec.email2);
        return SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), adnRec.name, adnRec.phone, emailsString, adnRec.phone2, adnRec.name_sne, oldGrpString, adnRec.name, adnRec.phone, emailsString, adnRec.phone2, adnRec.name_sne, newGrpString);
    }

    private boolean removeMembersFromSimGroup(int slotId, ContentResolver resolver, long rawContactId, long groupId) {
        String groupIndex = "";
        SimPhoneBookCommonUtil.AdnRecord adnRec = SimPhoneBookCommonUtil.getSimAdnViaRawId(resolver, rawContactId, SimPhoneBookCommonUtil.GET_ADN_MODE1);
        String oldGrpString = SimPhoneBookCommonUtil.getSimGrpStringViaRawId(resolver, rawContactId);
        Log.d("ContactSaveService", "----> removeMembersFromSimGroup oldGrpString = " + oldGrpString);
        Cursor c = resolver.query(ContactsContract.Groups.CONTENT_URI, new String[]{"sourceid"}, "_id=" + groupId, null, null);
        if (c != null && c.moveToFirst()) {
            Log.d("ContactSaveService", "----> remove group index = " + c.getString(0));
            groupIndex = c.getString(0);
        }
        String newGrpString = oldGrpString.replaceAll(groupIndex.toString() + ",", "");
        Log.d("ContactSaveService", "----> removeMembersFromSimGroup  newGrpString = " + newGrpString);
        c.close();
        String emailsString = adnRec.email + (adnRec.email2.isEmpty() ? "" : "," + adnRec.email2);
        return SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), adnRec.name, adnRec.phone, emailsString, adnRec.phone2, adnRec.name_sne, oldGrpString, adnRec.name, adnRec.phone, emailsString, adnRec.phone2, adnRec.name_sne, newGrpString);
    }

    public static Intent createSetStarredIntent(Context context, Uri contactUri, boolean value) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("setStarred");
        serviceIntent.putExtra("contactUri", contactUri);
        serviceIntent.putExtra("starred", value);
        return serviceIntent;
    }

    private void setStarred(Intent intent) {
        Uri contactUri = (Uri) intent.getParcelableExtra("contactUri");
        boolean value = intent.getBooleanExtra("starred", false);
        if (contactUri == null) {
            Log.e("ContactSaveService", "Invalid arguments for setStarred request");
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put("starred", Boolean.valueOf(value));
        getContentResolver().update(contactUri, values, null, null);
        Cursor c = getContentResolver().query(contactUri, new String[]{"_id"}, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    long id = c.getLong(0);
                    if (id < 9223372034707292160L) {
                        getContentResolver().call(ContactsContract.AUTHORITY_URI, "undemote", String.valueOf(id), (Bundle) null);
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    public static Intent createSetSendToVoicemail(Context context, Uri contactUri, boolean value) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("sendToVoicemail");
        serviceIntent.putExtra("contactUri", contactUri);
        serviceIntent.putExtra("sendToVoicemailFlag", value);
        return serviceIntent;
    }

    private void setSendToVoicemail(Intent intent) {
        Uri contactUri = (Uri) intent.getParcelableExtra("contactUri");
        boolean value = intent.getBooleanExtra("sendToVoicemailFlag", false);
        if (contactUri == null) {
            Log.e("ContactSaveService", "Invalid arguments for setRedirectToVoicemail");
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put("send_to_voicemail", Boolean.valueOf(value));
        getContentResolver().update(contactUri, values, null, null);
    }

    public static Intent createSetRingtone(Context context, Uri contactUri, String value) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("setRingtone");
        serviceIntent.putExtra("contactUri", contactUri);
        serviceIntent.putExtra("customRingtone", value);
        return serviceIntent;
    }

    private void setRingtone(Intent intent) {
        Uri contactUri = (Uri) intent.getParcelableExtra("contactUri");
        String value = intent.getStringExtra("customRingtone");
        if (contactUri == null) {
            Log.e("ContactSaveService", "Invalid arguments for setRingtone");
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put("custom_ringtone", value);
        getContentResolver().update(contactUri, values, null, null);
    }

    public static Intent createSetSuperPrimaryIntent(Context context, long dataId) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("setSuperPrimary");
        serviceIntent.putExtra("dataId", dataId);
        return serviceIntent;
    }

    private void setSuperPrimary(Intent intent) {
        long dataId = intent.getLongExtra("dataId", -1L);
        if (dataId == -1) {
            Log.e("ContactSaveService", "Invalid arguments for setSuperPrimary request");
        } else {
            ContactUpdateUtils.setSuperPrimary(this, dataId);
        }
    }

    public static Intent createClearPrimaryIntent(Context context, long dataId) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("clearPrimary");
        serviceIntent.putExtra("dataId", dataId);
        return serviceIntent;
    }

    private void clearPrimary(Intent intent) {
        long dataId = intent.getLongExtra("dataId", -1L);
        if (dataId == -1) {
            Log.e("ContactSaveService", "Invalid arguments for clearPrimary request");
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put("is_super_primary", (Integer) 0);
        values.put("is_primary", (Integer) 0);
        getContentResolver().update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId), values, null, null);
    }

    public static Intent createDeleteContactIntent(Context context, Uri contactUri) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("delete");
        serviceIntent.putExtra("contactUri", contactUri);
        return serviceIntent;
    }

    private void deleteContact(Intent intent) {
        String accountType;
        int slotId;
        Uri contactUri = (Uri) intent.getParcelableExtra("contactUri");
        if (contactUri == null) {
            Log.e("ContactSaveService", "Invalid arguments for deleteContact request");
            return;
        }
        Log.d("ContactSaveService", " delete contact uri = " + contactUri);
        Cursor c = getContentResolver().query(contactUri, new String[]{"name_raw_contact_id"}, null, null, null);
        if (c != null && c.moveToFirst()) {
            int rawContactId = c.getInt(0);
            if (-1 != rawContactId) {
                ContentResolver resolver = getContentResolver();
                Cursor c2 = getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[]{"data1"}, "raw_contact_id == ? AND mimetype == ?", new String[]{"" + rawContactId, "vnd.android.cursor.item/group_membership"}, null);
                while (c2 != null && c2.moveToNext()) {
                    long groupId = c2.getLong(0);
                    Log.d("ContactSaveService", "delete contact, remove contact groupId = " + groupId);
                    if (groupId != -1) {
                        long[] rawContactIdGroup = {rawContactId};
                        removeMembersFromGroup(getContentResolver(), rawContactIdGroup, groupId);
                    }
                }
                c2.close();
                Cursor c1 = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"display_name", "account_type"}, "_id == ?", new String[]{"" + rawContactId}, null);
                if (c1 != null && c1.moveToFirst() && (accountType = c1.getString(1)) != null && (accountType.equals("com.android.contact.sim") || accountType.equals("com.android.contact.sim2"))) {
                    if (accountType.equals("com.android.contact.sim")) {
                        slotId = 0;
                    } else {
                        slotId = 1;
                    }
                    SimPhoneBookCommonUtil.AdnRecord adnRec = SimPhoneBookCommonUtil.getSimAdnViaRawId(resolver, rawContactId, SimPhoneBookCommonUtil.GET_ADN_MODE1);
                    String oldGrpString = SimPhoneBookCommonUtil.getSimGrpStringViaRawId(resolver, rawContactId);
                    String emailsString = adnRec.email + (adnRec.email2.isEmpty() ? "" : "," + adnRec.email2);
                    if (!SimPhoneBookCommonUtil.updateSimContact(slotId, getContentResolver(), adnRec.name, adnRec.phone, emailsString, adnRec.phone2, adnRec.name_sne, oldGrpString, "", "", "", "", "", "")) {
                        c1.close();
                        c.close();
                        return;
                    }
                }
                c1.close();
            }
            c.close();
        }
        getContentResolver().delete(contactUri, null, null);
    }

    public static Intent createJoinContactsIntent(Context context, long contactId1, long contactId2, boolean contactWritable, Class<? extends Activity> callbackActivity, String callbackAction) {
        Intent serviceIntent = new Intent(context, (Class<?>) ContactSaveService.class);
        serviceIntent.setAction("joinContacts");
        serviceIntent.putExtra("contactId1", contactId1);
        serviceIntent.putExtra("contactId2", contactId2);
        serviceIntent.putExtra("contactWritable", contactWritable);
        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(callbackAction);
        serviceIntent.putExtra("callbackIntent", callbackIntent);
        return serviceIntent;
    }

    private void joinContacts(Intent intent) {
        long contactId1 = intent.getLongExtra("contactId1", -1L);
        long contactId2 = intent.getLongExtra("contactId2", -1L);
        boolean writable = intent.getBooleanExtra("contactWritable", false);
        if (contactId1 == -1 || contactId2 == -1) {
            Log.e("ContactSaveService", "Invalid arguments for joinContacts request");
            return;
        }
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(ContactsContract.RawContacts.CONTENT_URI, JoinContactQuery.PROJECTION, "contact_id=? OR contact_id=?", new String[]{String.valueOf(contactId1), String.valueOf(contactId2)}, null);
        if (c == null) {
            Log.e("ContactSaveService", "Unable to open Contacts DB cursor");
            showToast(R.string.contactSavedErrorToast);
            return;
        }
        long verifiedNameRawContactId = -1;
        try {
            if (c.getCount() != 0) {
                int maxDisplayNameSource = -1;
                long[] rawContactIds = new long[c.getCount()];
                for (int i = 0; i < rawContactIds.length; i++) {
                    c.moveToPosition(i);
                    long rawContactId = c.getLong(0);
                    rawContactIds[i] = rawContactId;
                    int nameSource = c.getInt(3);
                    if (nameSource > maxDisplayNameSource) {
                        maxDisplayNameSource = nameSource;
                    }
                }
                if (writable) {
                    for (int i2 = 0; i2 < rawContactIds.length; i2++) {
                        c.moveToPosition(i2);
                        if (c.getLong(1) == contactId1 && c.getInt(3) == maxDisplayNameSource && (verifiedNameRawContactId == -1 || c.getInt(2) != 0)) {
                            verifiedNameRawContactId = c.getLong(0);
                        }
                    }
                }
                c.close();
                ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                for (int i3 = 0; i3 < rawContactIds.length; i3++) {
                    for (int j = 0; j < rawContactIds.length; j++) {
                        if (i3 != j) {
                            buildJoinContactDiff(operations, rawContactIds[i3], rawContactIds[j]);
                        }
                    }
                }
                if (verifiedNameRawContactId != -1) {
                    ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, verifiedNameRawContactId));
                    builder.withValue("name_verified", 1);
                    operations.add(builder.build());
                }
                boolean success = false;
                try {
                    resolver.applyBatch("com.android.contacts", operations);
                    showToast(R.string.contactsJoinedMessage);
                    success = true;
                } catch (OperationApplicationException e) {
                    Log.e("ContactSaveService", "Failed to apply aggregation exception batch", e);
                    showToast(R.string.contactSavedErrorToast);
                } catch (RemoteException e2) {
                    Log.e("ContactSaveService", "Failed to apply aggregation exception batch", e2);
                    showToast(R.string.contactSavedErrorToast);
                }
                Intent callbackIntent = (Intent) intent.getParcelableExtra("callbackIntent");
                if (success) {
                    Uri uri = ContactsContract.RawContacts.getContactLookupUri(resolver, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactIds[0]));
                    callbackIntent.setData(uri);
                }
                deliverCallback(callbackIntent);
            }
        } finally {
            c.close();
        }
    }

    private void buildJoinContactDiff(ArrayList<ContentProviderOperation> operations, long rawContactId1, long rawContactId2) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        builder.withValue("type", 1);
        builder.withValue("raw_contact_id1", Long.valueOf(rawContactId1));
        builder.withValue("raw_contact_id2", Long.valueOf(rawContactId2));
        operations.add(builder.build());
    }

    private void showToast(final int message) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactSaveService.this, message, 1).show();
            }
        });
    }

    private void deliverCallback(final Intent callbackIntent) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                ContactSaveService.this.deliverCallbackOnUiThread(callbackIntent);
            }
        });
    }

    void deliverCallbackOnUiThread(Intent callbackIntent) {
        for (Listener listener : sListeners) {
            if (callbackIntent.getComponent().equals(((Activity) listener).getIntent().getComponent())) {
                listener.onServiceCompleted(callbackIntent);
                return;
            }
        }
    }
}
