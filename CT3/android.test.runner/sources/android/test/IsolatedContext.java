package android.test;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.OperationCanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Deprecated
public class IsolatedContext extends ContextWrapper {
    private List<Intent> mBroadcastIntents;
    private final MockAccountManager mMockAccountManager;
    private ContentResolver mResolver;

    public IsolatedContext(ContentResolver resolver, Context targetContext) {
        super(targetContext);
        this.mBroadcastIntents = Lists.newArrayList();
        this.mResolver = resolver;
        this.mMockAccountManager = new MockAccountManager();
    }

    public List<Intent> getAndClearBroadcastIntents() {
        List<Intent> intents = this.mBroadcastIntents;
        this.mBroadcastIntents = Lists.newArrayList();
        return intents;
    }

    @Override
    public ContentResolver getContentResolver() {
        return this.mResolver;
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return false;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
    }

    @Override
    public void sendBroadcast(Intent intent) {
        this.mBroadcastIntents.add(intent);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        this.mBroadcastIntents.add(intent);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        return 0;
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return 0;
    }

    @Override
    public Object getSystemService(String name) {
        if ("account".equals(name)) {
            return this.mMockAccountManager;
        }
        return null;
    }

    private class MockAccountManager extends AccountManager {
        public MockAccountManager() {
            super(IsolatedContext.this, null, null);
        }

        @Override
        public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately) {
        }

        @Override
        public Account[] getAccounts() {
            return new Account[0];
        }

        @Override
        public AccountManagerFuture<Account[]> getAccountsByTypeAndFeatures(String type, String[] features, AccountManagerCallback<Account[]> callback, Handler handler) {
            return new MockAccountManagerFuture(new Account[0]);
        }

        @Override
        public String blockingGetAuthToken(Account account, String authTokenType, boolean notifyAuthFailure) throws OperationCanceledException, IOException, AuthenticatorException {
            return null;
        }

        private class MockAccountManagerFuture<T> implements AccountManagerFuture<T> {
            T mResult;

            public MockAccountManagerFuture(T result) {
                this.mResult = result;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public T getResult() throws OperationCanceledException, IOException, AuthenticatorException {
                return this.mResult;
            }

            @Override
            public T getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
                return getResult();
            }
        }
    }

    @Override
    public File getFilesDir() {
        return new File("/dev/null");
    }
}
