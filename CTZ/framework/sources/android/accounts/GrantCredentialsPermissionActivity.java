package android.accounts;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.R;
import java.io.IOException;

public class GrantCredentialsPermissionActivity extends Activity implements View.OnClickListener {
    public static final String EXTRAS_ACCOUNT = "account";
    public static final String EXTRAS_AUTH_TOKEN_TYPE = "authTokenType";
    public static final String EXTRAS_REQUESTING_UID = "uid";
    public static final String EXTRAS_RESPONSE = "response";
    private Account mAccount;
    private String mAuthTokenType;
    private int mCallingUid;
    protected LayoutInflater mInflater;
    private Bundle mResultBundle = null;
    private int mUid;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        setContentView(R.layout.grant_credentials_permission);
        setTitle(R.string.grant_permissions_header_text);
        this.mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            setResult(0);
            finish();
            return;
        }
        this.mAccount = (Account) extras.getParcelable("account");
        this.mAuthTokenType = extras.getString("authTokenType");
        this.mUid = extras.getInt(EXTRAS_REQUESTING_UID);
        PackageManager packageManager = getPackageManager();
        String[] packagesForUid = packageManager.getPackagesForUid(this.mUid);
        if (this.mAccount == null || this.mAuthTokenType == null || packagesForUid == null) {
            setResult(0);
            finish();
            return;
        }
        try {
            this.mCallingUid = ActivityManager.getService().getLaunchedFromUid(getActivityToken());
        } catch (RemoteException e) {
            Log.w(getClass().getSimpleName(), "Unable to get caller identity \n" + e);
        }
        if (!UserHandle.isSameApp(this.mCallingUid, 1000) && this.mCallingUid != this.mUid) {
            setResult(0);
            finish();
            return;
        }
        try {
            String accountLabel = getAccountLabel(this.mAccount);
            final TextView textView = (TextView) findViewById(R.id.authtoken_type);
            textView.setVisibility(8);
            AccountManagerCallback<String> accountManagerCallback = new AccountManagerCallback<String>() {
                @Override
                public void run(AccountManagerFuture<String> accountManagerFuture) {
                    try {
                        final String result = accountManagerFuture.getResult();
                        if (!TextUtils.isEmpty(result)) {
                            GrantCredentialsPermissionActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!GrantCredentialsPermissionActivity.this.isFinishing()) {
                                        textView.setText(result);
                                        textView.setVisibility(0);
                                    }
                                }
                            });
                        }
                    } catch (AuthenticatorException e2) {
                    } catch (OperationCanceledException e3) {
                    } catch (IOException e4) {
                    }
                }
            };
            if (!AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE.equals(this.mAuthTokenType)) {
                AccountManager.get(this).getAuthTokenLabel(this.mAccount.type, this.mAuthTokenType, accountManagerCallback, null);
            }
            findViewById(R.id.allow_button).setOnClickListener(this);
            findViewById(R.id.deny_button).setOnClickListener(this);
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.packages_list);
            for (String string : packagesForUid) {
                try {
                    string = packageManager.getApplicationLabel(packageManager.getApplicationInfo(string, 0)).toString();
                } catch (PackageManager.NameNotFoundException e2) {
                }
                linearLayout.addView(newPackageView(string));
            }
            ((TextView) findViewById(R.id.account_name)).setText(this.mAccount.name);
            ((TextView) findViewById(R.id.account_type)).setText(accountLabel);
        } catch (IllegalArgumentException e3) {
            setResult(0);
            finish();
        }
    }

    private String getAccountLabel(Account account) {
        for (AuthenticatorDescription authenticatorDescription : AccountManager.get(this).getAuthenticatorTypes()) {
            if (authenticatorDescription.type.equals(account.type)) {
                try {
                    return createPackageContext(authenticatorDescription.packageName, 0).getString(authenticatorDescription.labelId);
                } catch (PackageManager.NameNotFoundException e) {
                    return account.type;
                } catch (Resources.NotFoundException e2) {
                    return account.type;
                }
            }
        }
        return account.type;
    }

    private View newPackageView(String str) {
        View viewInflate = this.mInflater.inflate(R.layout.permissions_package_list_item, (ViewGroup) null);
        ((TextView) viewInflate.findViewById(R.id.package_label)).setText(str);
        return viewInflate;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == 16908707) {
            AccountManager.get(this).updateAppPermission(this.mAccount, this.mAuthTokenType, this.mUid, true);
            Intent intent = new Intent();
            intent.putExtra("retry", true);
            setResult(-1, intent);
            setAccountAuthenticatorResult(intent.getExtras());
        } else if (id == 16908843) {
            AccountManager.get(this).updateAppPermission(this.mAccount, this.mAuthTokenType, this.mUid, false);
            setResult(0);
        }
        finish();
    }

    public final void setAccountAuthenticatorResult(Bundle bundle) {
        this.mResultBundle = bundle;
    }

    @Override
    public void finish() {
        AccountAuthenticatorResponse accountAuthenticatorResponse = (AccountAuthenticatorResponse) getIntent().getParcelableExtra("response");
        if (accountAuthenticatorResponse != null) {
            if (this.mResultBundle != null) {
                accountAuthenticatorResponse.onResult(this.mResultBundle);
            } else {
                accountAuthenticatorResponse.onError(4, "canceled");
            }
        }
        super.finish();
    }
}
