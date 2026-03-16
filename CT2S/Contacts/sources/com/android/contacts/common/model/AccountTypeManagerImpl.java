package com.android.contacts.common.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncAdapterType;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimingLogger;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountTypeWithDataSet;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.account.ExchangeAccountType;
import com.android.contacts.common.model.account.ExternalAccountType;
import com.android.contacts.common.model.account.FallbackAccountType;
import com.android.contacts.common.model.account.GoogleAccountType;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

class AccountTypeManagerImpl extends AccountTypeManager implements OnAccountsUpdateListener, SyncStatusObserver {
    private AccountManager mAccountManager;
    private Context mContext;
    private AccountType mFallbackAccountType;
    private final InvitableAccountTypeCache mInvitableAccountTypeCache;
    private Handler mListenerHandler;
    private AccountType mPhoneAccountType;
    private AccountType mSimAccountType;
    private AccountType mSimAccountType2;
    private static final Map<AccountTypeWithDataSet, AccountType> EMPTY_UNMODIFIABLE_ACCOUNT_TYPE_MAP = Collections.unmodifiableMap(new HashMap());
    private static final Uri SAMPLE_CONTACT_URI = ContactsContract.Contacts.getLookupUri(1, "xxx");
    private static final Comparator<Account> ACCOUNT_COMPARATOR = new Comparator<Account>() {
        @Override
        public int compare(Account a, Account b) {
            String aDataSet = null;
            String bDataSet = null;
            if (a instanceof AccountWithDataSet) {
                aDataSet = ((AccountWithDataSet) a).dataSet;
            }
            if (b instanceof AccountWithDataSet) {
                bDataSet = ((AccountWithDataSet) b).dataSet;
            }
            if (Objects.equal(a.name, b.name) && Objects.equal(a.type, b.type) && Objects.equal(aDataSet, bDataSet)) {
                return 0;
            }
            if (b.name == null || b.type == null) {
                return -1;
            }
            if (a.name == null || a.type == null) {
                return 1;
            }
            int diff = a.name.compareTo(b.name);
            if (diff == 0) {
                int diff2 = a.type.compareTo(b.type);
                if (diff2 == 0) {
                    if (aDataSet != null) {
                        return bDataSet == null ? 1 : aDataSet.compareTo(bDataSet);
                    }
                    return -1;
                }
                return diff2;
            }
            return diff;
        }
    };
    private List<AccountWithDataSet> mAccounts = Lists.newArrayList();
    private List<AccountWithDataSet> mContactWritableAccounts = Lists.newArrayList();
    private List<AccountWithDataSet> mContactWritableAccountsWithoutSim = Lists.newArrayList();
    private List<AccountWithDataSet> mGroupWritableAccounts = Lists.newArrayList();
    private Map<AccountTypeWithDataSet, AccountType> mAccountTypesWithDataSets = Maps.newHashMap();
    private Map<AccountTypeWithDataSet, AccountType> mInvitableAccountTypes = EMPTY_UNMODIFIABLE_ACCOUNT_TYPE_MAP;
    private final AtomicBoolean mInvitablesCacheIsInitialized = new AtomicBoolean(false);
    private final AtomicBoolean mInvitablesTaskIsRunning = new AtomicBoolean(false);
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCheckFilterValidityRunnable = new Runnable() {
        @Override
        public void run() {
            ContactListFilterController.getInstance(AccountTypeManagerImpl.this.mContext).checkFilterValidity(true);
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = AccountTypeManagerImpl.this.mListenerHandler.obtainMessage(1, intent);
            AccountTypeManagerImpl.this.mListenerHandler.sendMessage(msg);
        }
    };
    private volatile CountDownLatch mInitializationLatch = new CountDownLatch(1);
    private HandlerThread mListenerThread = new HandlerThread("AccountChangeListener");

    public AccountTypeManagerImpl(Context context) {
        this.mContext = context;
        this.mFallbackAccountType = new FallbackAccountType(context);
        this.mSimAccountType = new SimAccountType(context, 0);
        this.mSimAccountType2 = new SimAccountType(context, 1);
        this.mPhoneAccountType = new FallbackAccountType(context);
        this.mAccountManager = AccountManager.get(this.mContext);
        this.mListenerThread.start();
        this.mListenerHandler = new Handler(this.mListenerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        AccountTypeManagerImpl.this.loadAccountsInBackground();
                        break;
                    case 1:
                        AccountTypeManagerImpl.this.processBroadcastIntent((Intent) msg.obj);
                        break;
                }
            }
        };
        this.mInvitableAccountTypeCache = new InvitableAccountTypeCache();
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        IntentFilter simFilter = new IntentFilter();
        simFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        simFilter.addAction("android.intent.action.SIM_CONTACTS_LOADED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, simFilter);
        this.mAccountManager.addOnAccountsUpdatedListener(this, this.mListenerHandler, false);
        ContentResolver.addStatusChangeListener(1, this);
        this.mListenerHandler.sendEmptyMessage(0);
    }

    @Override
    public void onStatusChanged(int which) {
        this.mListenerHandler.sendEmptyMessage(0);
    }

    public void processBroadcastIntent(Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
            String status = intent.getStringExtra("ss");
            if ("READY".equals(status)) {
                this.mListenerHandler.sendEmptyMessage(0);
                return;
            }
            return;
        }
        this.mListenerHandler.sendEmptyMessage(0);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        loadAccountsInBackground();
    }

    void ensureAccountsLoaded() {
        CountDownLatch latch = this.mInitializationLatch;
        if (latch == null) {
            return;
        }
        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void loadAccountsInBackground() {
        List<AccountType> accountTypes;
        AccountType accountType;
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "AccountTypeManager.loadAccountsInBackground start");
        }
        TimingLogger timings = new TimingLogger("AccountTypeManager", "loadAccountsInBackground");
        long startTime = SystemClock.currentThreadTimeMillis();
        long startTimeWall = SystemClock.elapsedRealtime();
        Map<AccountTypeWithDataSet, AccountType> accountTypesByTypeAndDataSet = Maps.newHashMap();
        Map<String, List<AccountType>> accountTypesByType = Maps.newHashMap();
        List<AccountWithDataSet> allAccounts = Lists.newArrayList();
        List<AccountWithDataSet> contactWritableAccounts = Lists.newArrayList();
        List<AccountWithDataSet> contactWritableAccountsWithoutSim = Lists.newArrayList();
        List<AccountWithDataSet> groupWritableAccounts = Lists.newArrayList();
        Set<String> extensionPackages = Sets.newHashSet();
        AccountManager am = this.mAccountManager;
        SyncAdapterType[] syncs = ContentResolver.getSyncAdapterTypes();
        AuthenticatorDescription[] auths = am.getAuthenticatorTypes();
        for (SyncAdapterType sync : syncs) {
            if ("com.android.contacts".equals(sync.authority)) {
                String type = sync.accountType;
                AuthenticatorDescription auth = findAuthenticator(auths, type);
                if (auth == null) {
                    Log.w("AccountTypeManager", "No authenticator found for type=" + type + ", ignoring it.");
                } else {
                    if ("com.google".equals(type)) {
                        accountType = new GoogleAccountType(this.mContext, auth.packageName);
                    } else if (ExchangeAccountType.isExchangeType(type)) {
                        accountType = new ExchangeAccountType(this.mContext, auth.packageName, type);
                    } else {
                        Log.d("AccountTypeManager", "Registering external account type=" + type + ", packageName=" + auth.packageName);
                        accountType = new ExternalAccountType(this.mContext, auth.packageName, false);
                    }
                    if (!accountType.isInitialized()) {
                        if (accountType.isEmbedded()) {
                            throw new IllegalStateException("Problem initializing embedded type " + accountType.getClass().getCanonicalName());
                        }
                    } else {
                        accountType.accountType = auth.type;
                        accountType.titleRes = auth.labelId;
                        accountType.iconRes = auth.iconId;
                        addAccountType(accountType, accountTypesByTypeAndDataSet, accountTypesByType);
                        extensionPackages.addAll(accountType.getExtensionPackageNames());
                    }
                }
            }
        }
        if (!extensionPackages.isEmpty()) {
            Log.d("AccountTypeManager", "Registering " + extensionPackages.size() + " extension packages");
            for (String extensionPackage : extensionPackages) {
                ExternalAccountType accountType2 = new ExternalAccountType(this.mContext, extensionPackage, true);
                if (accountType2.isInitialized()) {
                    if (!accountType2.hasContactsMetadata()) {
                        Log.w("AccountTypeManager", "Skipping extension package " + extensionPackage + " because it doesn't have the CONTACTS_STRUCTURE metadata");
                    } else if (TextUtils.isEmpty(accountType2.accountType)) {
                        Log.w("AccountTypeManager", "Skipping extension package " + extensionPackage + " because the CONTACTS_STRUCTURE metadata doesn't have the accountType attribute");
                    } else {
                        Log.d("AccountTypeManager", "Registering extension package account type=" + accountType2.accountType + ", dataSet=" + accountType2.dataSet + ", packageName=" + extensionPackage);
                        addAccountType(accountType2, accountTypesByTypeAndDataSet, accountTypesByType);
                    }
                }
            }
        }
        timings.addSplit("Loaded account types");
        Account[] accounts = this.mAccountManager.getAccounts();
        int len$ = accounts.length;
        int i$ = 0;
        while (true) {
            int i$2 = i$;
            if (i$2 >= len$) {
                break;
            }
            Account account = accounts[i$2];
            boolean syncable = ContentResolver.getIsSyncable(account, "com.android.contacts") > 0;
            if (syncable && (accountTypes = accountTypesByType.get(account.type)) != null) {
                for (AccountType accountType3 : accountTypes) {
                    AccountWithDataSet accountWithDataSet = new AccountWithDataSet(account.name, account.type, accountType3.dataSet);
                    allAccounts.add(accountWithDataSet);
                    if (accountType3.areContactsWritable()) {
                        contactWritableAccounts.add(accountWithDataSet);
                        contactWritableAccountsWithoutSim.add(accountWithDataSet);
                    }
                    if (accountType3.isGroupMembershipEditable()) {
                        groupWritableAccounts.add(accountWithDataSet);
                    }
                }
            }
            i$ = i$2 + 1;
        }
        TelephonyManager tm = TelephonyManager.from(this.mContext);
        if (SimPhoneBookCommonUtil.isSimEnabled(0)) {
            Log.d("AccountTypeManager", " add sim account");
            AccountWithDataSet simAccountWithDataSet = new AccountWithDataSet(SimAccountType.ACCOUNT_NAME, "com.android.contact.sim", null);
            allAccounts.add(simAccountWithDataSet);
            contactWritableAccounts.add(simAccountWithDataSet);
            addAccountType(this.mSimAccountType, accountTypesByTypeAndDataSet, accountTypesByType);
        }
        if (SimPhoneBookCommonUtil.isSimEnabled(1) && tm.isMultiSimEnabled()) {
            Log.d("AccountTypeManager", " add sim account2");
            AccountWithDataSet simAccountWithDataSet2 = new AccountWithDataSet("Sim2", "com.android.contact.sim2", null);
            allAccounts.add(simAccountWithDataSet2);
            contactWritableAccounts.add(simAccountWithDataSet2);
            addAccountType(this.mSimAccountType2, accountTypesByTypeAndDataSet, accountTypesByType);
        }
        Log.d("AccountTypeManager", " add Phone account");
        AccountWithDataSet accountWithDataSet2 = new AccountWithDataSet("Phone", "Phone", null);
        allAccounts.add(accountWithDataSet2);
        contactWritableAccounts.add(accountWithDataSet2);
        contactWritableAccountsWithoutSim.add(accountWithDataSet2);
        addAccountType(this.mPhoneAccountType, accountTypesByTypeAndDataSet, accountTypesByType);
        Collections.sort(allAccounts, ACCOUNT_COMPARATOR);
        Collections.sort(contactWritableAccounts, ACCOUNT_COMPARATOR);
        Collections.sort(contactWritableAccountsWithoutSim, ACCOUNT_COMPARATOR);
        Collections.sort(groupWritableAccounts, ACCOUNT_COMPARATOR);
        timings.addSplit("Loaded accounts");
        synchronized (this) {
            this.mAccountTypesWithDataSets = accountTypesByTypeAndDataSet;
            this.mAccounts = allAccounts;
            this.mContactWritableAccounts = contactWritableAccounts;
            this.mContactWritableAccountsWithoutSim = contactWritableAccountsWithoutSim;
            this.mGroupWritableAccounts = groupWritableAccounts;
            this.mInvitableAccountTypes = findAllInvitableAccountTypes(this.mContext, allAccounts, accountTypesByTypeAndDataSet);
        }
        timings.dumpToLog();
        long endTimeWall = SystemClock.elapsedRealtime();
        long endTime = SystemClock.currentThreadTimeMillis();
        Log.i("AccountTypeManager", "Loaded meta-data for " + this.mAccountTypesWithDataSets.size() + " account types, " + this.mAccounts.size() + " accounts in " + (endTimeWall - startTimeWall) + "ms(wall) " + (endTime - startTime) + "ms(cpu)");
        if (this.mInitializationLatch != null) {
            this.mInitializationLatch.countDown();
            this.mInitializationLatch = null;
        }
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "AccountTypeManager.loadAccountsInBackground finish");
        }
        this.mMainThreadHandler.post(this.mCheckFilterValidityRunnable);
    }

    private void addAccountType(AccountType accountType, Map<AccountTypeWithDataSet, AccountType> accountTypesByTypeAndDataSet, Map<String, List<AccountType>> accountTypesByType) {
        accountTypesByTypeAndDataSet.put(accountType.getAccountTypeAndDataSet(), accountType);
        List<AccountType> accountsForType = accountTypesByType.get(accountType.accountType);
        if (accountsForType == null) {
            accountsForType = Lists.newArrayList();
        }
        accountsForType.add(accountType);
        accountTypesByType.put(accountType.accountType, accountsForType);
    }

    protected static AuthenticatorDescription findAuthenticator(AuthenticatorDescription[] auths, String accountType) {
        for (AuthenticatorDescription auth : auths) {
            if (accountType.equals(auth.type)) {
                return auth;
            }
        }
        return null;
    }

    @Override
    public List<AccountWithDataSet> getAccounts(boolean contactWritableOnly) {
        ensureAccountsLoaded();
        return contactWritableOnly ? this.mContactWritableAccounts : this.mAccounts;
    }

    @Override
    public List<AccountWithDataSet> getGroupWritableAccounts() {
        ensureAccountsLoaded();
        return this.mGroupWritableAccounts;
    }

    @Override
    public List<AccountWithDataSet> getWritableAccountsWithoutSim() {
        ensureAccountsLoaded();
        return this.mContactWritableAccountsWithoutSim;
    }

    @Override
    public DataKind getKindOrFallback(AccountType type, String mimeType) {
        ensureAccountsLoaded();
        DataKind kind = null;
        if (type != null) {
            kind = type.getKindForMimetype(mimeType);
        }
        if (kind == null) {
            kind = this.mFallbackAccountType.getKindForMimetype(mimeType);
        }
        if (kind == null && Log.isLoggable("AccountTypeManager", 3)) {
            Log.d("AccountTypeManager", "Unknown type=" + type + ", mime=" + mimeType);
        }
        return kind;
    }

    @Override
    public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
        AccountType type;
        ensureAccountsLoaded();
        synchronized (this) {
            type = this.mAccountTypesWithDataSets.get(accountTypeWithDataSet);
            if (type != null) {
                Log.d("AccountTypeManager", "--> type.accountType = " + type.accountType);
            }
            if (type == null) {
                type = this.mFallbackAccountType;
            }
        }
        return type;
    }

    private Map<AccountTypeWithDataSet, AccountType> getAllInvitableAccountTypes() {
        ensureAccountsLoaded();
        return this.mInvitableAccountTypes;
    }

    @Override
    public Map<AccountTypeWithDataSet, AccountType> getUsableInvitableAccountTypes() {
        ensureAccountsLoaded();
        if (!this.mInvitablesCacheIsInitialized.get()) {
            this.mInvitableAccountTypeCache.setCachedValue(findUsableInvitableAccountTypes(this.mContext));
            this.mInvitablesCacheIsInitialized.set(true);
        } else if (this.mInvitableAccountTypeCache.isExpired() && this.mInvitablesTaskIsRunning.compareAndSet(false, true)) {
            new FindInvitablesTask().execute(new Void[0]);
        }
        return this.mInvitableAccountTypeCache.getCachedValue();
    }

    static Map<AccountTypeWithDataSet, AccountType> findAllInvitableAccountTypes(Context context, Collection<AccountWithDataSet> accounts, Map<AccountTypeWithDataSet, AccountType> accountTypesByTypeAndDataSet) {
        HashMap<AccountTypeWithDataSet, AccountType> result = Maps.newHashMap();
        for (AccountWithDataSet account : accounts) {
            AccountTypeWithDataSet accountTypeWithDataSet = account.getAccountTypeWithDataSet();
            AccountType type = accountTypesByTypeAndDataSet.get(accountTypeWithDataSet);
            if (type != null && !result.containsKey(accountTypeWithDataSet)) {
                if (Log.isLoggable("AccountTypeManager", 3)) {
                    Log.d("AccountTypeManager", "Type " + accountTypeWithDataSet + " inviteClass=" + type.getInviteContactActivityClassName());
                }
                if (!TextUtils.isEmpty(type.getInviteContactActivityClassName())) {
                    result.put(accountTypeWithDataSet, type);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<AccountTypeWithDataSet, AccountType> findUsableInvitableAccountTypes(Context context) {
        Map<? extends AccountTypeWithDataSet, ? extends AccountType> allInvitables = getAllInvitableAccountTypes();
        if (allInvitables.isEmpty()) {
            return EMPTY_UNMODIFIABLE_ACCOUNT_TYPE_MAP;
        }
        HashMap<AccountTypeWithDataSet, AccountType> result = Maps.newHashMap();
        result.putAll(allInvitables);
        PackageManager packageManager = context.getPackageManager();
        for (AccountTypeWithDataSet accountTypeWithDataSet : allInvitables.keySet()) {
            AccountType accountType = allInvitables.get(accountTypeWithDataSet);
            Intent invitableIntent = MoreContactUtils.getInvitableIntent(accountType, SAMPLE_CONTACT_URI);
            if (invitableIntent == null) {
                result.remove(accountTypeWithDataSet);
            } else {
                ResolveInfo resolveInfo = packageManager.resolveActivity(invitableIntent, 65536);
                if (resolveInfo == null) {
                    result.remove(accountTypeWithDataSet);
                } else if (!accountTypeWithDataSet.hasData(context)) {
                    result.remove(accountTypeWithDataSet);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public List<AccountType> getAccountTypes(boolean contactWritableOnly) {
        ensureAccountsLoaded();
        List<AccountType> accountTypes = Lists.newArrayList();
        synchronized (this) {
            for (AccountType type : this.mAccountTypesWithDataSets.values()) {
                if (!contactWritableOnly || type.areContactsWritable()) {
                    accountTypes.add(type);
                }
            }
        }
        return accountTypes;
    }

    private class FindInvitablesTask extends AsyncTask<Void, Void, Map<AccountTypeWithDataSet, AccountType>> {
        private FindInvitablesTask() {
        }

        @Override
        protected Map<AccountTypeWithDataSet, AccountType> doInBackground(Void... params) {
            return AccountTypeManagerImpl.this.findUsableInvitableAccountTypes(AccountTypeManagerImpl.this.mContext);
        }

        @Override
        protected void onPostExecute(Map<AccountTypeWithDataSet, AccountType> accountTypes) {
            AccountTypeManagerImpl.this.mInvitableAccountTypeCache.setCachedValue(accountTypes);
            AccountTypeManagerImpl.this.mInvitablesTaskIsRunning.set(false);
        }
    }

    private static final class InvitableAccountTypeCache {
        private Map<AccountTypeWithDataSet, AccountType> mInvitableAccountTypes;
        private long mTimeLastSet;

        private InvitableAccountTypeCache() {
        }

        public boolean isExpired() {
            return SystemClock.elapsedRealtime() - this.mTimeLastSet > 60000;
        }

        public Map<AccountTypeWithDataSet, AccountType> getCachedValue() {
            return this.mInvitableAccountTypes;
        }

        public void setCachedValue(Map<AccountTypeWithDataSet, AccountType> map) {
            this.mInvitableAccountTypes = map;
            this.mTimeLastSet = SystemClock.elapsedRealtime();
        }
    }
}
