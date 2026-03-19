package com.android.contacts.database;

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.util.ArrayMap;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.model.SimCard;
import com.android.contacts.model.SimContact;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.util.SharedPreferenceUtil;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimContactDaoImpl extends SimContactDao {
    private static final int IMPORT_MAX_BATCH_SIZE = 300;
    static final int QUERY_MAX_BATCH_SIZE = 100;
    private static final String TAG = "SimContactDao";
    private final Context mContext;
    private final ContentResolver mResolver;
    private final TelephonyManager mTelephonyManager;
    public static final Uri ICC_CONTENT_URI = Uri.parse("content://icc/adn");
    public static String _ID = "_id";
    public static String NAME = "name";
    public static String NUMBER = "number";
    public static String EMAILS = "emails";
    private static final Object SIM_READ_LOCK = new Object();

    public SimContactDaoImpl(Context context) {
        this(context, context.getContentResolver(), (TelephonyManager) context.getSystemService("phone"));
    }

    public SimContactDaoImpl(Context context, ContentResolver contentResolver, TelephonyManager telephonyManager) {
        this.mContext = context;
        this.mResolver = contentResolver;
        this.mTelephonyManager = telephonyManager;
    }

    public Context getContext() {
        return this.mContext;
    }

    @Override
    public boolean canReadSimContacts() {
        return hasTelephony() && hasPermissions() && this.mTelephonyManager.getSimState() == 5;
    }

    @Override
    public List<SimCard> getSimCards() {
        List<SimCard> listSingletonList;
        if (!canReadSimContacts()) {
            return Collections.emptyList();
        }
        if (CompatUtils.isMSIMCompatible()) {
            listSingletonList = getSimCardsFromSubscriptions();
        } else {
            listSingletonList = Collections.singletonList(SimCard.create(this.mTelephonyManager, this.mContext.getString(R.string.single_sim_display_label)));
        }
        return SharedPreferenceUtil.restoreSimStates(this.mContext, listSingletonList);
    }

    @Override
    public ArrayList<SimContact> loadContactsForSim(SimCard simCard) {
        if (simCard.hasValidSubscriptionId()) {
            return loadSimContacts(simCard.getSubscriptionId());
        }
        return loadSimContacts();
    }

    public ArrayList<SimContact> loadSimContacts(int i) {
        return loadFrom(ICC_CONTENT_URI.buildUpon().appendPath("subId").appendPath(String.valueOf(i)).build());
    }

    public ArrayList<SimContact> loadSimContacts() {
        return loadFrom(ICC_CONTENT_URI);
    }

    @Override
    public ContentProviderResult[] importContacts(List<SimContact> list, AccountWithDataSet accountWithDataSet) throws RemoteException, OperationApplicationException {
        if (list.size() < IMPORT_MAX_BATCH_SIZE) {
            return importBatch(list, accountWithDataSet);
        }
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (i < list.size()) {
            int size = list.size();
            int i2 = i + IMPORT_MAX_BATCH_SIZE;
            arrayList.addAll(Arrays.asList(importBatch(list.subList(i, Math.min(size, i2)), accountWithDataSet)));
            i = i2;
        }
        return (ContentProviderResult[]) arrayList.toArray(new ContentProviderResult[arrayList.size()]);
    }

    @Override
    public void persistSimState(SimCard simCard) {
        SharedPreferenceUtil.persistSimStates(this.mContext, Collections.singletonList(simCard));
    }

    @Override
    public void persistSimStates(List<SimCard> list) {
        SharedPreferenceUtil.persistSimStates(this.mContext, list);
    }

    @Override
    public SimCard getSimBySubscriptionId(int i) {
        List<SimCard> listRestoreSimStates = SharedPreferenceUtil.restoreSimStates(this.mContext, getSimCards());
        if (i == -1 && !listRestoreSimStates.isEmpty()) {
            return listRestoreSimStates.get(0);
        }
        for (SimCard simCard : getSimCards()) {
            if (simCard.getSubscriptionId() == i) {
                return simCard;
            }
        }
        return null;
    }

    @Override
    public Map<AccountWithDataSet, Set<SimContact>> findAccountsOfExistingSimContacts(List<SimContact> list) {
        ArrayMap arrayMap = new ArrayMap();
        int i = 0;
        while (i < list.size()) {
            int i2 = i + 100;
            findAccountsOfExistingSimContacts(list.subList(i, Math.min(list.size(), i2)), arrayMap);
            i = i2;
        }
        return arrayMap;
    }

    private void findAccountsOfExistingSimContacts(List<SimContact> list, Map<AccountWithDataSet, Set<SimContact>> map) {
        HashMap map2 = new HashMap();
        Collections.sort(list, SimContact.compareByPhoneThenName());
        Cursor cursorQueryRawContactsForSimContacts = queryRawContactsForSimContacts(list);
        while (cursorQueryRawContactsForSimContacts.moveToNext()) {
            try {
                int iFindByPhoneAndName = SimContact.findByPhoneAndName(list, DataQuery.getPhoneNumber(cursorQueryRawContactsForSimContacts), DataQuery.getDisplayName(cursorQueryRawContactsForSimContacts));
                if (iFindByPhoneAndName >= 0) {
                    SimContact simContact = list.get(iFindByPhoneAndName);
                    long rawContactId = DataQuery.getRawContactId(cursorQueryRawContactsForSimContacts);
                    if (!map2.containsKey(Long.valueOf(rawContactId))) {
                        map2.put(Long.valueOf(rawContactId), new ArrayList());
                    }
                    ((List) map2.get(Long.valueOf(rawContactId))).add(simContact);
                }
            } catch (Throwable th) {
                cursorQueryRawContactsForSimContacts.close();
                throw th;
            }
        }
        cursorQueryRawContactsForSimContacts.close();
        Cursor cursorQueryAccountsOfRawContacts = queryAccountsOfRawContacts(map2.keySet());
        while (cursorQueryAccountsOfRawContacts.moveToNext()) {
            try {
                AccountWithDataSet account = AccountQuery.getAccount(cursorQueryAccountsOfRawContacts);
                long id = AccountQuery.getId(cursorQueryAccountsOfRawContacts);
                if (!map.containsKey(account)) {
                    map.put(account, new HashSet());
                }
                Iterator it = ((List) map2.get(Long.valueOf(id))).iterator();
                while (it.hasNext()) {
                    map.get(account).add((SimContact) it.next());
                }
            } finally {
                cursorQueryAccountsOfRawContacts.close();
            }
        }
    }

    private ContentProviderResult[] importBatch(List<SimContact> list, AccountWithDataSet accountWithDataSet) throws RemoteException, OperationApplicationException {
        return this.mResolver.applyBatch("com.android.contacts", createImportOperations(list, accountWithDataSet));
    }

    @TargetApi(22)
    private List<SimCard> getSimCardsFromSubscriptions() {
        List<SubscriptionInfo> activeSubscriptionInfoList = ((SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service")).getActiveSubscriptionInfoList();
        ArrayList arrayList = new ArrayList();
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (it.hasNext()) {
            arrayList.add(SimCard.create(it.next()));
        }
        return arrayList;
    }

    private List<SimContact> getContactsForSim(SimCard simCard) {
        List<SimContact> contacts = simCard.getContacts();
        return contacts != null ? contacts : loadContactsForSim(simCard);
    }

    private ArrayList<SimContact> loadFrom(Uri uri) {
        synchronized (SIM_READ_LOCK) {
            Cursor cursorQuery = this.mResolver.query(uri, null, null, null, null);
            if (cursorQuery == null) {
                return new ArrayList<>(0);
            }
            try {
                return loadFromCursor(cursorQuery);
            } finally {
                cursorQuery.close();
            }
        }
    }

    private ArrayList<SimContact> loadFromCursor(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(_ID);
        int columnIndex2 = cursor.getColumnIndex(NAME);
        int columnIndex3 = cursor.getColumnIndex(NUMBER);
        int columnIndex4 = cursor.getColumnIndex(EMAILS);
        ArrayList<SimContact> arrayList = new ArrayList<>();
        while (cursor.moveToNext()) {
            SimContact simContact = new SimContact(cursor.getLong(columnIndex), cursor.getString(columnIndex2), cursor.getString(columnIndex3), parseEmails(cursor.getString(columnIndex4)));
            if (simContact.hasName() || simContact.hasPhone() || simContact.hasEmails()) {
                arrayList.add(simContact);
            }
        }
        return arrayList;
    }

    private Cursor queryRawContactsForSimContacts(List<SimContact> list) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int i2 = 0;
        for (SimContact simContact : list) {
            if (simContact.hasPhone()) {
                i++;
            } else if (simContact.hasName()) {
                i2++;
            }
        }
        ArrayList arrayList = new ArrayList(i + 1);
        sb.append('(');
        sb.append("mimetype");
        sb.append("=? AND ");
        arrayList.add("vnd.android.cursor.item/phone_v2");
        sb.append("data1");
        sb.append(" IN (");
        sb.append(Joiner.on(',').join(Collections.nCopies(i, '?')));
        sb.append(')');
        for (SimContact simContact2 : list) {
            if (simContact2.hasPhone()) {
                arrayList.add(simContact2.getPhone());
            }
        }
        sb.append(')');
        if (i2 > 0) {
            sb.append(" OR (");
            sb.append("mimetype");
            sb.append("=? AND ");
            arrayList.add("vnd.android.cursor.item/name");
            sb.append("display_name");
            sb.append(" IN (");
            sb.append(Joiner.on(',').join(Collections.nCopies(i2, '?')));
            sb.append(')');
            for (SimContact simContact3 : list) {
                if (!simContact3.hasPhone() && simContact3.hasName()) {
                    arrayList.add(simContact3.getName());
                }
            }
            sb.append(')');
        }
        return this.mResolver.query(ContactsContract.Data.CONTENT_URI.buildUpon().appendQueryParameter("visible_contacts_only", "true").build(), DataQuery.PROJECTION, sb.toString(), (String[]) arrayList.toArray(new String[arrayList.size()]), null);
    }

    private Cursor queryAccountsOfRawContacts(Set<Long> set) {
        StringBuilder sb = new StringBuilder();
        String[] strArr = new String[set.size()];
        sb.append("_id");
        sb.append(" IN (");
        int i = 0;
        sb.append(Joiner.on(',').join(Collections.nCopies(strArr.length, '?')));
        sb.append(")");
        Iterator<Long> it = set.iterator();
        while (it.hasNext()) {
            strArr[i] = String.valueOf(it.next().longValue());
            i++;
        }
        return this.mResolver.query(ContactsContract.RawContacts.CONTENT_URI, AccountQuery.PROJECTION, sb.toString(), strArr, null);
    }

    private ArrayList<ContentProviderOperation> createImportOperations(List<SimContact> list, AccountWithDataSet accountWithDataSet) {
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        Iterator<SimContact> it = list.iterator();
        while (it.hasNext()) {
            it.next().appendCreateContactOperations(arrayList, accountWithDataSet);
        }
        return arrayList;
    }

    private String[] parseEmails(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return str.split(",");
    }

    private boolean hasTelephony() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.telephony");
    }

    private boolean hasPermissions() {
        return PermissionsUtil.hasContactsPermissions(this.mContext) && PermissionsUtil.hasPhonePermissions(this.mContext);
    }

    public static class DebugImpl extends SimContactDaoImpl {
        private SparseArray<SimCard> mCardsBySubscription;
        private List<SimCard> mSimCards;

        public DebugImpl(Context context) {
            super(context);
            this.mSimCards = new ArrayList();
            this.mCardsBySubscription = new SparseArray<>();
        }

        public DebugImpl addSimCard(SimCard simCard) {
            this.mSimCards.add(simCard);
            this.mCardsBySubscription.put(simCard.getSubscriptionId(), simCard);
            return this;
        }

        @Override
        public List<SimCard> getSimCards() {
            return SharedPreferenceUtil.restoreSimStates(getContext(), this.mSimCards);
        }

        @Override
        public ArrayList<SimContact> loadContactsForSim(SimCard simCard) {
            return new ArrayList<>(simCard.getContacts());
        }

        @Override
        public boolean canReadSimContacts() {
            return true;
        }
    }

    private static final class DataQuery {
        public static final String[] PROJECTION = {"raw_contact_id", "data1", "display_name", "mimetype"};

        public static long getRawContactId(Cursor cursor) {
            return cursor.getLong(0);
        }

        public static String getPhoneNumber(Cursor cursor) {
            if (isPhoneNumber(cursor)) {
                return cursor.getString(1);
            }
            return null;
        }

        public static String getDisplayName(Cursor cursor) {
            return cursor.getString(2);
        }

        public static boolean isPhoneNumber(Cursor cursor) {
            return "vnd.android.cursor.item/phone_v2".equals(cursor.getString(3));
        }
    }

    private static final class AccountQuery {
        public static final String[] PROJECTION = {"_id", "account_name", "account_type", "data_set"};

        public static long getId(Cursor cursor) {
            return cursor.getLong(0);
        }

        public static AccountWithDataSet getAccount(Cursor cursor) {
            return new AccountWithDataSet(cursor.getString(1), cursor.getString(2), cursor.getString(3));
        }
    }
}
