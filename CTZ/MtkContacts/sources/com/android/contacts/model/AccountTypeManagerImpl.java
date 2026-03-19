package com.android.contacts.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import com.android.contacts.R;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountTypeProvider;
import com.android.contacts.model.account.AccountTypeWithDataSet;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.FallbackAccountType;
import com.android.contacts.model.account.GoogleAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.concurrent.ContactsExecutors;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.mediatek.contacts.lettertiles.LetterTileDrawableEx;
import com.mediatek.contacts.model.AccountTypeManagerEx;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

class AccountTypeManagerImpl extends AccountTypeManager implements OnAccountsUpdateListener, SyncStatusObserver {
    private final AccountManager mAccountManager;
    private ListenableFuture<AccountTypeProvider> mAccountTypesFuture;
    private final Context mContext;
    private final AccountType mFallbackAccountType;
    private final DeviceLocalAccountLocator mLocalAccountLocator;
    private ListenableFuture<List<AccountWithDataSet>> mLocalAccountsFuture;
    private AccountTypeProvider mTypeProvider;
    private List<AccountWithDataSet> mLocalAccounts = new ArrayList();
    private List<AccountWithDataSet> mAccountManagerAccounts = new ArrayList();
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Function<AccountTypeProvider, List<AccountWithDataSet>> mAccountsExtractor = new Function<AccountTypeProvider, List<AccountWithDataSet>>() {
        @Override
        public List<AccountWithDataSet> apply(AccountTypeProvider accountTypeProvider) {
            Log.i("AccountTypeManager", "mAccountsExtractor.apply()");
            return AccountTypeManagerImpl.this.getAccountsWithDataSets(AccountTypeManagerImpl.this.mAccountManager.getAccounts(), accountTypeProvider);
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("AccountTypeManager", "[onReceive]intent = " + intent);
            if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                LetterTileDrawableEx.clearSimIconBitmaps();
            }
            AccountTypeManagerImpl.this.reloadAccountTypes();
            AccountTypeManagerImpl.this.reloadLocalAccounts();
        }
    };
    private final ListeningExecutorService mExecutor = ContactsExecutors.getDefaultThreadPoolExecutor();
    private final Executor mMainThreadExecutor = ContactsExecutors.newHandlerExecutor(this.mMainThreadHandler);

    public AccountTypeManagerImpl(Context context) {
        this.mContext = context;
        this.mLocalAccountLocator = DeviceLocalAccountLocator.create(context);
        this.mTypeProvider = new AccountTypeProvider(context);
        this.mFallbackAccountType = new FallbackAccountType(context);
        this.mAccountManager = AccountManager.get(this.mContext);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter2);
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        AccountTypeManagerEx.registerReceiverOnSimStateAndInfoChanged(this.mContext, this.mBroadcastReceiver);
        this.mAccountManager.addOnAccountsUpdatedListener(this, this.mMainThreadHandler, false);
        ContentResolver.addStatusChangeListener(1, this);
        loadAccountTypes();
    }

    @Override
    public void onStatusChanged(int i) {
        Log.i("AccountTypeManager", "onStatusChanged()");
        reloadAccountTypesIfNeeded();
    }

    @Override
    public void onAccountsUpdated(Account[] accountArr) {
        Log.i("AccountTypeManager", "onAccountsUpdated()");
        reloadLocalAccounts();
        maybeNotifyAccountsUpdated(this.mAccountManagerAccounts, getAccountsWithDataSets(accountArr, this.mTypeProvider));
    }

    private void maybeNotifyAccountsUpdated(List<AccountWithDataSet> list, List<AccountWithDataSet> list2) {
        if (Objects.equal(list, list2)) {
            return;
        }
        list.clear();
        list.addAll(list2);
        Log.i("AccountTypeManager", "[maybeNotifyAccountsUpdated] notifyAccountsChanged()");
        notifyAccountsChanged();
    }

    private void notifyAccountsChanged() {
        ContactListFilterController.getInstance(this.mContext).checkFilterValidity(true);
        LocalBroadcastManager.getInstance(this.mContext).sendBroadcast(new Intent(BROADCAST_ACCOUNTS_CHANGED));
    }

    private synchronized void startLoadingIfNeeded() {
        if (this.mTypeProvider == null && this.mAccountTypesFuture == null) {
            reloadAccountTypesIfNeeded();
        }
        if (this.mLocalAccountsFuture == null) {
            reloadLocalAccounts();
        }
    }

    private synchronized void loadAccountTypes() {
        this.mTypeProvider = new AccountTypeProvider(this.mContext);
        this.mAccountTypesFuture = this.mExecutor.submit((Callable) new Callable<AccountTypeProvider>() {
            @Override
            public AccountTypeProvider call() throws Exception {
                Log.i("AccountTypeManager", "loadAccountTypes.call()");
                AccountTypeManagerImpl.this.getAccountsWithDataSets(AccountTypeManagerImpl.this.mAccountManager.getAccounts(), AccountTypeManagerImpl.this.mTypeProvider);
                return AccountTypeManagerImpl.this.mTypeProvider;
            }
        });
    }

    private FutureCallback<List<AccountWithDataSet>> newAccountsUpdatedCallback(final List<AccountWithDataSet> list) {
        return new FutureCallback<List<AccountWithDataSet>>() {
            @Override
            public void onSuccess(List<AccountWithDataSet> list2) {
                Iterator<AccountWithDataSet> it = list2.iterator();
                while (it.hasNext()) {
                    Log.i("AccountTypeManager", "[FutureCallback] AccountWithDataSet:" + Log.anonymize(it.next().name));
                }
                AccountTypeManagerImpl.this.maybeNotifyAccountsUpdated(list, list2);
            }

            @Override
            public void onFailure(Throwable th) {
                Log.i("AccountTypeManager", "[FutureCallback] onFailure()");
            }
        };
    }

    private synchronized void reloadAccountTypesIfNeeded() {
        if (this.mTypeProvider == null || this.mTypeProvider.shouldUpdate(this.mAccountManager.getAuthenticatorTypes(), ContentResolver.getSyncAdapterTypes())) {
            reloadAccountTypes();
        }
    }

    private synchronized void reloadAccountTypes() {
        loadAccountTypes();
        Futures.addCallback(Futures.transform(this.mAccountTypesFuture, this.mAccountsExtractor), newAccountsUpdatedCallback(this.mAccountManagerAccounts), this.mMainThreadExecutor);
    }

    private synchronized void loadLocalAccounts() {
        this.mLocalAccountsFuture = this.mExecutor.submit((Callable) new Callable<List<AccountWithDataSet>>() {
            @Override
            public List<AccountWithDataSet> call() throws Exception {
                return AccountTypeManagerImpl.this.mLocalAccountLocator.getDeviceLocalAccounts();
            }
        });
    }

    private synchronized void reloadLocalAccounts() {
        loadLocalAccounts();
        Futures.addCallback(this.mLocalAccountsFuture, newAccountsUpdatedCallback(this.mLocalAccounts), this.mMainThreadExecutor);
    }

    @Override
    public ListenableFuture<List<AccountInfo>> getAccountsAsync() {
        return getAllAccountsAsyncInternal();
    }

    private synchronized ListenableFuture<List<AccountInfo>> getAllAccountsAsyncInternal() {
        final AccountTypeProvider accountTypeProvider;
        Log.i("AccountTypeManager", "getAllAccountsAsyncInternal()");
        startLoadingIfNeeded();
        accountTypeProvider = this.mTypeProvider;
        return Futures.transform(Futures.nonCancellationPropagating(Futures.successfulAsList(Futures.transform(this.mAccountTypesFuture, this.mAccountsExtractor), this.mLocalAccountsFuture)), new Function<List<List<AccountWithDataSet>>, List<AccountInfo>>() {
            @Override
            public List<AccountInfo> apply(List<List<AccountWithDataSet>> list) {
                Log.i("AccountTypeManager", "getAllAccountsAsyncInternal().apply()");
                Preconditions.checkArgument(list.size() == 2, "List should have exactly 2 elements");
                ArrayList arrayList = new ArrayList();
                for (AccountWithDataSet accountWithDataSet : list.get(0)) {
                    Log.i("AccountTypeManager", "getAllAccountsAsyncInternal().account(0):" + Log.anonymize(accountWithDataSet.name));
                    arrayList.add(accountTypeProvider.getTypeForAccount(accountWithDataSet).wrapAccount(AccountTypeManagerImpl.this.mContext, accountWithDataSet));
                }
                for (AccountWithDataSet accountWithDataSet2 : list.get(1)) {
                    Log.i("AccountTypeManager", "getAllAccountsAsyncInternal().account(1):" + Log.anonymize(accountWithDataSet2.name));
                    arrayList.add(accountTypeProvider.getTypeForAccount(accountWithDataSet2).wrapAccount(AccountTypeManagerImpl.this.mContext, accountWithDataSet2));
                }
                AccountInfo.sortAccounts(null, arrayList);
                return arrayList;
            }
        });
    }

    @Override
    public ListenableFuture<List<AccountInfo>> filterAccountsAsync(final Predicate<AccountInfo> predicate) {
        return Futures.transform(getAllAccountsAsyncInternal(), new Function<List<AccountInfo>, List<AccountInfo>>() {
            @Override
            public List<AccountInfo> apply(List<AccountInfo> list) {
                Log.i("AccountTypeManager", "filterAccountsAsync().apply()");
                return new ArrayList(Collections2.filter(list, predicate));
            }
        }, this.mExecutor);
    }

    @Override
    public AccountInfo getAccountInfoForAccount(AccountWithDataSet accountWithDataSet) {
        Log.i("AccountTypeManager", "getAccountInfoForAccount()");
        if (accountWithDataSet == null) {
            return null;
        }
        AccountType typeForAccount = this.mTypeProvider.getTypeForAccount(accountWithDataSet);
        if (typeForAccount == null) {
            typeForAccount = this.mFallbackAccountType;
        }
        return typeForAccount.wrapAccount(this.mContext, accountWithDataSet);
    }

    private List<AccountWithDataSet> getAccountsWithDataSets(Account[] accountArr, AccountTypeProvider accountTypeProvider) {
        StringBuilder sb = new StringBuilder();
        sb.append("getAccountsWithDataSets() count=");
        sb.append(accountArr.length);
        Log.i("AccountTypeManager", sb.toString());
        ArrayList arrayList = new ArrayList();
        for (Account account : accountArr) {
            Iterator<AccountType> it = accountTypeProvider.getAccountTypes(account.type).iterator();
            while (it.hasNext()) {
                arrayList.add(new AccountWithDataSet(account.name, account.type, it.next().dataSet));
            }
        }
        return arrayList;
    }

    @Override
    public Account getDefaultGoogleAccount() {
        Log.i("AccountTypeManager", "getDefaultGoogleAccount()");
        return getDefaultGoogleAccount(this.mAccountManager, this.mContext.getSharedPreferences(this.mContext.getPackageName(), 0), this.mContext.getResources().getString(R.string.contact_editor_default_account_key));
    }

    @Override
    public List<AccountInfo> getWritableGoogleAccounts() {
        Log.i("AccountTypeManager", "getWritableGoogleAccounts()");
        Account[] accountsByType = this.mAccountManager.getAccountsByType(GoogleAccountType.ACCOUNT_TYPE);
        ArrayList arrayList = new ArrayList();
        for (Account account : accountsByType) {
            AccountWithDataSet accountWithDataSet = new AccountWithDataSet(account.name, account.type, null);
            AccountType typeForAccount = this.mTypeProvider.getTypeForAccount(accountWithDataSet);
            if (typeForAccount != null) {
                arrayList.add(typeForAccount.wrapAccount(this.mContext, accountWithDataSet));
            }
        }
        return arrayList;
    }

    @Override
    public boolean hasNonLocalAccount() {
        Account[] accounts = this.mAccountManager.getAccounts();
        if (accounts == null) {
            Log.i("AccountTypeManager", "hasNonLocalAccount() accounts == null");
            return false;
        }
        for (Account account : accounts) {
            if (this.mTypeProvider.supportsContactsSyncing(account.type)) {
                Log.i("AccountTypeManager", "hasNonLocalAccount() true");
                return true;
            }
        }
        Log.i("AccountTypeManager", "hasNonLocalAccount() false");
        return false;
    }

    @Override
    public DataKind getKindOrFallback(AccountType accountType, String str) {
        DataKind kindForMimetype;
        if (accountType != null) {
            kindForMimetype = accountType.getKindForMimetype(str);
        } else {
            kindForMimetype = null;
        }
        if (kindForMimetype == null) {
            kindForMimetype = this.mFallbackAccountType.getKindForMimetype(str);
        }
        if (kindForMimetype == null && Log.isLoggable("AccountTypeManager", 3)) {
            Log.d("AccountTypeManager", "Unknown type=" + accountType + ", mime=" + str);
        }
        return kindForMimetype;
    }

    @Override
    public boolean exists(AccountWithDataSet accountWithDataSet) {
        for (Account account : this.mAccountManager.getAccountsByType(accountWithDataSet.type)) {
            if (account.name.equals(accountWithDataSet.name)) {
                return this.mTypeProvider.getTypeForAccount(accountWithDataSet) != null;
            }
        }
        return false;
    }

    @Override
    public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
        AccountType type = this.mTypeProvider.getType(accountTypeWithDataSet.accountType, accountTypeWithDataSet.dataSet);
        return type != null ? type : this.mFallbackAccountType;
    }
}
