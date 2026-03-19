package android.accounts;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.R;
import java.util.HashMap;

public class ChooseAccountActivity extends Activity {
    private static final String TAG = "AccountManager";
    private String mCallingPackage;
    private int mCallingUid;
    private Bundle mResult;
    private Parcelable[] mAccounts = null;
    private AccountManagerResponse mAccountManagerResponse = null;
    private HashMap<String, AuthenticatorDescription> mTypeToAuthDescription = new HashMap<>();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        this.mAccounts = getIntent().getParcelableArrayExtra(AccountManager.KEY_ACCOUNTS);
        this.mAccountManagerResponse = (AccountManagerResponse) getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_MANAGER_RESPONSE);
        if (this.mAccounts == null) {
            setResult(0);
            finish();
            return;
        }
        try {
            IBinder activityToken = getActivityToken();
            this.mCallingUid = ActivityManager.getService().getLaunchedFromUid(activityToken);
            this.mCallingPackage = ActivityManager.getService().getLaunchedFromPackage(activityToken);
        } catch (RemoteException e) {
            Log.w(getClass().getSimpleName(), "Unable to get caller identity \n" + e);
        }
        if (UserHandle.isSameApp(this.mCallingUid, 1000) && getIntent().getStringExtra(AccountManager.KEY_ANDROID_PACKAGE_NAME) != null) {
            this.mCallingPackage = getIntent().getStringExtra(AccountManager.KEY_ANDROID_PACKAGE_NAME);
        }
        if (!UserHandle.isSameApp(this.mCallingUid, 1000) && getIntent().getStringExtra(AccountManager.KEY_ANDROID_PACKAGE_NAME) != null) {
            Log.w(getClass().getSimpleName(), "Non-system Uid: " + this.mCallingUid + " tried to override packageName \n");
        }
        getAuthDescriptions();
        AccountInfo[] accountInfoArr = new AccountInfo[this.mAccounts.length];
        for (int i = 0; i < this.mAccounts.length; i++) {
            accountInfoArr[i] = new AccountInfo(((Account) this.mAccounts[i]).name, getDrawableForType(((Account) this.mAccounts[i]).type));
        }
        setContentView(R.layout.choose_account);
        ListView listView = (ListView) findViewById(16908298);
        listView.setAdapter((ListAdapter) new AccountArrayAdapter(this, 17367043, accountInfoArr));
        listView.setChoiceMode(1);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i2, long j) {
                ChooseAccountActivity.this.onListItemClick((ListView) adapterView, view, i2, j);
            }
        });
    }

    private void getAuthDescriptions() {
        for (AuthenticatorDescription authenticatorDescription : AccountManager.get(this).getAuthenticatorTypes()) {
            this.mTypeToAuthDescription.put(authenticatorDescription.type, authenticatorDescription);
        }
    }

    private Drawable getDrawableForType(String str) {
        if (this.mTypeToAuthDescription.containsKey(str)) {
            try {
                AuthenticatorDescription authenticatorDescription = this.mTypeToAuthDescription.get(str);
                return createPackageContext(authenticatorDescription.packageName, 0).getDrawable(authenticatorDescription.iconId);
            } catch (PackageManager.NameNotFoundException e) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "No icon name for account type " + str);
                }
            } catch (Resources.NotFoundException e2) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "No icon resource for account type " + str);
                }
            }
        }
        return null;
    }

    protected void onListItemClick(ListView listView, View view, int i, long j) {
        Account account = (Account) this.mAccounts[i];
        AccountManager accountManager = AccountManager.get(this);
        Integer numValueOf = Integer.valueOf(accountManager.getAccountVisibility(account, this.mCallingPackage));
        if (numValueOf != null && numValueOf.intValue() == 4) {
            accountManager.setAccountVisibility(account, this.mCallingPackage, 2);
        }
        Log.d(TAG, "selected account " + account);
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        bundle.putString("accountType", account.type);
        this.mResult = bundle;
        finish();
    }

    @Override
    public void finish() {
        if (this.mAccountManagerResponse != null) {
            if (this.mResult != null) {
                this.mAccountManagerResponse.onResult(this.mResult);
            } else {
                this.mAccountManagerResponse.onError(4, "canceled");
            }
        }
        super.finish();
    }

    private static class AccountInfo {
        final Drawable drawable;
        final String name;

        AccountInfo(String str, Drawable drawable) {
            this.name = str;
            this.drawable = drawable;
        }
    }

    private static class ViewHolder {
        ImageView icon;
        TextView text;

        private ViewHolder() {
        }
    }

    private static class AccountArrayAdapter extends ArrayAdapter<AccountInfo> {
        private AccountInfo[] mInfos;
        private LayoutInflater mLayoutInflater;

        public AccountArrayAdapter(Context context, int i, AccountInfo[] accountInfoArr) {
            super(context, i, accountInfoArr);
            this.mInfos = accountInfoArr;
            this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = this.mLayoutInflater.inflate(R.layout.choose_account_row, (ViewGroup) null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.account_row_text);
                viewHolder.icon = (ImageView) view.findViewById(R.id.account_row_icon);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.text.setText(this.mInfos[i].name);
            viewHolder.icon.setImageDrawable(this.mInfos[i].drawable);
            return view;
        }
    }
}
