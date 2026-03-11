package com.android.settings.accounts;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.Utils;
import java.io.IOException;

public class AddAccountSettings extends Activity {
    private PendingIntent mPendingIntent;
    private UserHandle mUserHandle;
    private final AccountManagerCallback<Bundle> mCallback = new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            boolean done = true;
            try {
                try {
                    try {
                        Bundle bundle = future.getResult();
                        Intent intent = (Intent) bundle.get("intent");
                        if (intent != null) {
                            done = false;
                            Bundle addAccountOptions = new Bundle();
                            addAccountOptions.putParcelable("pendingIntent", AddAccountSettings.this.mPendingIntent);
                            addAccountOptions.putBoolean("hasMultipleUsers", Utils.hasMultipleUsers(AddAccountSettings.this));
                            addAccountOptions.putParcelable("android.intent.extra.USER", AddAccountSettings.this.mUserHandle);
                            intent.putExtras(addAccountOptions);
                            AddAccountSettings.this.startActivityForResultAsUser(intent, 2, AddAccountSettings.this.mUserHandle);
                        } else {
                            AddAccountSettings.this.setResult(-1);
                            if (AddAccountSettings.this.mPendingIntent != null) {
                                AddAccountSettings.this.mPendingIntent.cancel();
                                AddAccountSettings.this.mPendingIntent = null;
                            }
                        }
                        if (Log.isLoggable("AccountSettings", 2)) {
                            Log.v("AccountSettings", "account added: " + bundle);
                        }
                        if (done) {
                            AddAccountSettings.this.finish();
                        }
                    } catch (OperationCanceledException e) {
                        if (Log.isLoggable("AccountSettings", 2)) {
                            Log.v("AccountSettings", "addAccount was canceled");
                        }
                        if (done) {
                            AddAccountSettings.this.finish();
                        }
                    }
                } catch (AuthenticatorException e2) {
                    if (Log.isLoggable("AccountSettings", 2)) {
                        Log.v("AccountSettings", "addAccount failed: " + e2);
                    }
                    if (done) {
                        AddAccountSettings.this.finish();
                    }
                } catch (IOException e3) {
                    if (Log.isLoggable("AccountSettings", 2)) {
                        Log.v("AccountSettings", "addAccount failed: " + e3);
                    }
                    if (done) {
                        AddAccountSettings.this.finish();
                    }
                }
            } catch (Throwable th) {
                if (done) {
                    AddAccountSettings.this.finish();
                }
                throw th;
            }
        }
    };
    private boolean mAddAccountCalled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mAddAccountCalled = savedInstanceState.getBoolean("AddAccountCalled");
            if (Log.isLoggable("AccountSettings", 2)) {
                Log.v("AccountSettings", "restored");
            }
        }
        UserManager um = (UserManager) getSystemService("user");
        this.mUserHandle = Utils.getSecureTargetUser(getActivityToken(), um, null, getIntent().getExtras());
        if (um.hasUserRestriction("no_modify_accounts", this.mUserHandle)) {
            Toast.makeText(this, R.string.user_cannot_add_accounts_message, 1).show();
            finish();
            return;
        }
        if (this.mAddAccountCalled) {
            finish();
            return;
        }
        String[] authorities = getIntent().getStringArrayExtra("authorities");
        String[] accountTypes = getIntent().getStringArrayExtra("account_types");
        Intent intent = new Intent(this, (Class<?>) ChooseAccountActivity.class);
        if (authorities != null) {
            intent.putExtra("authorities", authorities);
        }
        if (accountTypes != null) {
            intent.putExtra("account_types", accountTypes);
        }
        intent.putExtra("android.intent.extra.USER", this.mUserHandle);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == 0) {
                    setResult(resultCode);
                    finish();
                } else {
                    addAccount(data.getStringExtra("selected_account"));
                }
                break;
            case 2:
                setResult(resultCode);
                if (this.mPendingIntent != null) {
                    this.mPendingIntent.cancel();
                    this.mPendingIntent = null;
                }
                finish();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("AddAccountCalled", this.mAddAccountCalled);
        if (Log.isLoggable("AccountSettings", 2)) {
            Log.v("AccountSettings", "saved");
        }
    }

    private void addAccount(String accountType) {
        Bundle addAccountOptions = new Bundle();
        Intent identityIntent = new Intent();
        identityIntent.setComponent(new ComponentName("SHOULDN'T RESOLVE!", "SHOULDN'T RESOLVE!"));
        identityIntent.setAction("SHOULDN'T RESOLVE!");
        identityIntent.addCategory("SHOULDN'T RESOLVE!");
        this.mPendingIntent = PendingIntent.getBroadcast(this, 0, identityIntent, 0);
        addAccountOptions.putParcelable("pendingIntent", this.mPendingIntent);
        addAccountOptions.putBoolean("hasMultipleUsers", Utils.hasMultipleUsers(this));
        AccountManager.get(this).addAccountAsUser(accountType, null, null, addAccountOptions, null, this.mCallback, null, this.mUserHandle);
        this.mAddAccountCalled = true;
    }
}
