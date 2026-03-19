package com.android.contacts.model.account;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Loader;
import android.os.Bundle;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.util.concurrent.ListenableFutureLoader;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;
import com.mediatek.contacts.util.Log;
import java.util.List;

public class AccountsLoader extends ListenableFutureLoader<List<AccountInfo>> {
    private final AccountTypeManager mAccountTypeManager;
    private final Predicate<AccountInfo> mFilter;

    public interface AccountsListener {
        void onAccountsLoaded(List<AccountInfo> list);
    }

    public AccountsLoader(Context context, Predicate<AccountInfo> predicate) {
        super(context, new IntentFilter(AccountTypeManager.BROADCAST_ACCOUNTS_CHANGED));
        this.mAccountTypeManager = AccountTypeManager.getInstance(context);
        this.mFilter = predicate;
    }

    @Override
    protected ListenableFuture<List<AccountInfo>> loadData() {
        Log.d("AccountsLoader", "[loadData]");
        return this.mAccountTypeManager.filterAccountsAsync(this.mFilter);
    }

    @Override
    protected boolean isSameData(List<AccountInfo> list, List<AccountInfo> list2) {
        return Objects.equal(AccountInfo.extractAccounts(list), AccountInfo.extractAccounts(list2));
    }

    public static <FragmentType extends Fragment & AccountsListener> void loadAccounts(FragmentType fragmenttype, int i, Predicate<AccountInfo> predicate) {
        loadAccounts(fragmenttype.getActivity(), fragmenttype.getLoaderManager(), i, predicate, fragmenttype);
    }

    public static <ActivityType extends Activity & AccountsListener> void loadAccounts(ActivityType activitytype, int i, Predicate<AccountInfo> predicate) {
        loadAccounts(activitytype, activitytype.getLoaderManager(), i, predicate, activitytype);
    }

    private static void loadAccounts(final Context context, LoaderManager loaderManager, int i, final Predicate<AccountInfo> predicate, final AccountsListener accountsListener) {
        loaderManager.initLoader(i, null, new LoaderManager.LoaderCallbacks<List<AccountInfo>>() {
            @Override
            public Loader<List<AccountInfo>> onCreateLoader(int i2, Bundle bundle) {
                Log.d("AccountsLoader", "[onCreateLoader]");
                return new AccountsLoader(context, predicate);
            }

            @Override
            public void onLoadFinished(Loader<List<AccountInfo>> loader, List<AccountInfo> list) {
                accountsListener.onAccountsLoaded(list);
            }

            @Override
            public void onLoaderReset(Loader<List<AccountInfo>> loader) {
            }
        });
    }
}
