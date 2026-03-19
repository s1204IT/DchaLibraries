package android.accounts;

import android.R;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChooseTypeAndAccountActivity extends Activity implements AccountManagerCallback<Bundle> {
    public static final String EXTRA_ADD_ACCOUNT_AUTH_TOKEN_TYPE_STRING = "authTokenType";
    public static final String EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE = "addAccountOptions";
    public static final String EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY = "addAccountRequiredFeatures";
    public static final String EXTRA_ALLOWABLE_ACCOUNTS_ARRAYLIST = "allowableAccounts";
    public static final String EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY = "allowableAccountTypes";

    @Deprecated
    public static final String EXTRA_ALWAYS_PROMPT_FOR_ACCOUNT = "alwaysPromptForAccount";
    public static final String EXTRA_DESCRIPTION_TEXT_OVERRIDE = "descriptionTextOverride";
    public static final String EXTRA_SELECTED_ACCOUNT = "selectedAccount";
    private static final String KEY_INSTANCE_STATE_ACCOUNT_LIST = "accountList";
    private static final String KEY_INSTANCE_STATE_EXISTING_ACCOUNTS = "existingAccounts";
    private static final String KEY_INSTANCE_STATE_PENDING_REQUEST = "pendingRequest";
    private static final String KEY_INSTANCE_STATE_SELECTED_ACCOUNT_NAME = "selectedAccountName";
    private static final String KEY_INSTANCE_STATE_SELECTED_ADD_ACCOUNT = "selectedAddAccount";
    public static final int REQUEST_ADD_ACCOUNT = 2;
    public static final int REQUEST_CHOOSE_TYPE = 1;
    public static final int REQUEST_NULL = 0;
    private static final int SELECTED_ITEM_NONE = -1;
    private static final String TAG = "AccountChooser";
    private ArrayList<Account> mAccounts;
    private String mCallingPackage;
    private int mCallingUid;
    private String mDescriptionOverride;
    private boolean mDisallowAddAccounts;
    private boolean mDontShowPicker;
    private Button mOkButton;
    private int mSelectedItemIndex;
    private Set<Account> mSetOfAllowableAccounts;
    private Set<String> mSetOfRelevantAccountTypes;
    private String mSelectedAccountName = null;
    private boolean mSelectedAddNewAccount = false;
    private int mPendingRequest = 0;
    private Parcelable[] mExistingAccounts = null;
    private boolean mIsCanceled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "ChooseTypeAndAccountActivity.onCreate(savedInstanceState=" + savedInstanceState + ")");
        }
        try {
            IBinder activityToken = getActivityToken();
            this.mCallingUid = ActivityManagerNative.getDefault().getLaunchedFromUid(activityToken);
            this.mCallingPackage = ActivityManagerNative.getDefault().getLaunchedFromPackage(activityToken);
            if (this.mCallingUid != 0 && this.mCallingPackage != null) {
                Bundle restrictions = UserManager.get(this).getUserRestrictions(new UserHandle(UserHandle.getUserId(this.mCallingUid)));
                this.mDisallowAddAccounts = restrictions.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS, false);
            }
        } catch (RemoteException re) {
            Log.w(getClass().getSimpleName(), "Unable to get caller identity \n" + re);
        }
        Intent intent = getIntent();
        if (savedInstanceState != null) {
            this.mIsCanceled = false;
            this.mPendingRequest = savedInstanceState.getInt(KEY_INSTANCE_STATE_PENDING_REQUEST);
            this.mExistingAccounts = savedInstanceState.getParcelableArray(KEY_INSTANCE_STATE_EXISTING_ACCOUNTS);
            this.mSelectedAccountName = savedInstanceState.getString(KEY_INSTANCE_STATE_SELECTED_ACCOUNT_NAME);
            this.mSelectedAddNewAccount = savedInstanceState.getBoolean(KEY_INSTANCE_STATE_SELECTED_ADD_ACCOUNT, false);
            this.mAccounts = savedInstanceState.getParcelableArrayList(KEY_INSTANCE_STATE_ACCOUNT_LIST);
        } else {
            this.mPendingRequest = 0;
            this.mExistingAccounts = null;
            Account selectedAccount = (Account) intent.getParcelableExtra(EXTRA_SELECTED_ACCOUNT);
            if (selectedAccount != null) {
                this.mSelectedAccountName = selectedAccount.name;
            }
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "selected account name is " + this.mSelectedAccountName);
        }
        this.mSetOfAllowableAccounts = getAllowableAccountSet(intent);
        this.mSetOfRelevantAccountTypes = getReleventAccountTypes(intent);
        this.mDescriptionOverride = intent.getStringExtra(EXTRA_DESCRIPTION_TEXT_OVERRIDE);
        this.mAccounts = getAcceptableAccountChoices(AccountManager.get(this));
        if (this.mAccounts.isEmpty() && this.mDisallowAddAccounts) {
            requestWindowFeature(1);
            setContentView(17367093);
            this.mDontShowPicker = true;
        }
        if (this.mDontShowPicker) {
            super.onCreate(savedInstanceState);
            return;
        }
        if (this.mPendingRequest == 0 && !this.mIsCanceled && this.mAccounts.isEmpty()) {
            setNonLabelThemeAndCallSuperCreate(savedInstanceState);
            if (this.mSetOfRelevantAccountTypes.size() == 1) {
                runAddAccountForAuthenticator(this.mSetOfRelevantAccountTypes.iterator().next());
            } else {
                startChooseAccountTypeActivity();
            }
        }
        String[] listItems = getListOfDisplayableOptions(this.mAccounts);
        this.mSelectedItemIndex = getItemIndexToSelect(this.mAccounts, this.mSelectedAccountName, this.mSelectedAddNewAccount);
        super.onCreate(savedInstanceState);
        setContentView(17367107);
        overrideDescriptionIfSupplied(this.mDescriptionOverride);
        populateUIAccountList(listItems);
        this.mOkButton = (Button) findViewById(R.id.button2);
        this.mOkButton.setEnabled(this.mSelectedItemIndex != -1);
    }

    @Override
    protected void onDestroy() {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "ChooseTypeAndAccountActivity.onDestroy()");
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_INSTANCE_STATE_PENDING_REQUEST, this.mPendingRequest);
        if (this.mPendingRequest == 2) {
            outState.putParcelableArray(KEY_INSTANCE_STATE_EXISTING_ACCOUNTS, this.mExistingAccounts);
        }
        if (this.mSelectedItemIndex != -1) {
            if (this.mSelectedItemIndex == this.mAccounts.size()) {
                outState.putBoolean(KEY_INSTANCE_STATE_SELECTED_ADD_ACCOUNT, true);
            } else {
                outState.putBoolean(KEY_INSTANCE_STATE_SELECTED_ADD_ACCOUNT, false);
                outState.putString(KEY_INSTANCE_STATE_SELECTED_ACCOUNT_NAME, this.mAccounts.get(this.mSelectedItemIndex).name);
            }
        }
        outState.putParcelableArrayList(KEY_INSTANCE_STATE_ACCOUNT_LIST, this.mAccounts);
    }

    public void onCancelButtonClicked(View view) {
        onBackPressed();
    }

    public void onOkButtonClicked(View view) {
        if (this.mSelectedItemIndex == this.mAccounts.size()) {
            startChooseAccountTypeActivity();
        } else {
            if (this.mSelectedItemIndex == -1) {
                return;
            }
            onAccountSelected(this.mAccounts.get(this.mSelectedItemIndex));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String accountType;
        if (Log.isLoggable(TAG, 2)) {
            if (data != null && data.getExtras() != null) {
                data.getExtras().keySet();
            }
            Log.v(TAG, "ChooseTypeAndAccountActivity.onActivityResult(reqCode=" + requestCode + ", resCode=" + resultCode + ", extras=" + (data != null ? data.getExtras() : null) + ")");
        }
        this.mPendingRequest = 0;
        if (resultCode == 0) {
            if (this.mAccounts == null || this.mAccounts.isEmpty()) {
                this.mIsCanceled = true;
                setResult(0);
                finish();
                return;
            }
            return;
        }
        if (resultCode == -1) {
            if (requestCode == 1) {
                if (data != null && (accountType = data.getStringExtra("accountType")) != null) {
                    runAddAccountForAuthenticator(accountType);
                    return;
                }
                Log.d(TAG, "ChooseTypeAndAccountActivity.onActivityResult: unable to find account type, pretending the request was canceled");
            } else if (requestCode == 2) {
                String accountName = null;
                String accountType2 = null;
                if (data != null) {
                    accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    accountType2 = data.getStringExtra("accountType");
                }
                if (accountName == null || accountType2 == null) {
                    Account[] currentAccounts = AccountManager.get(this).getAccountsForPackage(this.mCallingPackage, this.mCallingUid);
                    Set<Account> preExistingAccounts = new HashSet<>();
                    for (Parcelable accountParcel : this.mExistingAccounts) {
                        preExistingAccounts.add((Account) accountParcel);
                    }
                    int i = 0;
                    int length = currentAccounts.length;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        Account account = currentAccounts[i];
                        if (!preExistingAccounts.contains(account)) {
                            accountName = account.name;
                            accountType2 = account.type;
                            break;
                        }
                        i++;
                    }
                }
                if (accountName != null || accountType2 != null) {
                    setResultAndFinish(accountName, accountType2);
                    return;
                }
            }
            Log.d(TAG, "ChooseTypeAndAccountActivity.onActivityResult: unable to find added account, pretending the request was canceled");
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "ChooseTypeAndAccountActivity.onActivityResult: canceled");
        }
        setResult(0);
        finish();
    }

    protected void runAddAccountForAuthenticator(String type) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "runAddAccountForAuthenticator: " + type);
        }
        Bundle options = getIntent().getBundleExtra(EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE);
        String[] requiredFeatures = getIntent().getStringArrayExtra(EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY);
        String authTokenType = getIntent().getStringExtra("authTokenType");
        AccountManager.get(this).addAccount(type, authTokenType, requiredFeatures, options, null, this, null);
    }

    @Override
    public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
        try {
            Bundle accountManagerResult = accountManagerFuture.getResult();
            Intent intent = (Intent) accountManagerResult.getParcelable("intent");
            if (intent != null) {
                this.mPendingRequest = 2;
                this.mExistingAccounts = AccountManager.get(this).getAccountsForPackage(this.mCallingPackage, this.mCallingUid);
                intent.setFlags(intent.getFlags() & (-268435457));
                startActivityForResult(intent, 2);
                return;
            }
        } catch (AuthenticatorException e) {
        } catch (OperationCanceledException e2) {
            setResult(0);
            finish();
            return;
        } catch (IOException e3) {
        }
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "error communicating with server");
        setResult(-1, new Intent().putExtras(bundle));
        finish();
    }

    private void setNonLabelThemeAndCallSuperCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Material_Light_Dialog_NoActionBar);
        super.onCreate(savedInstanceState);
    }

    private void onAccountSelected(Account account) {
        Log.d(TAG, "selected account " + account);
        setResultAndFinish(account.name, account.type);
    }

    private void setResultAndFinish(String accountName, String accountType) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
        bundle.putString("accountType", accountType);
        setResult(-1, new Intent().putExtras(bundle));
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "ChooseTypeAndAccountActivity.setResultAndFinish: selected account " + accountName + ", " + accountType);
        }
        finish();
    }

    private void startChooseAccountTypeActivity() {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "ChooseAccountTypeActivity.startChooseAccountTypeActivity()");
        }
        Intent intent = new Intent(this, (Class<?>) ChooseAccountTypeActivity.class);
        intent.setFlags(524288);
        intent.putExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY, getIntent().getStringArrayExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY));
        intent.putExtra(EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE, getIntent().getBundleExtra(EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE));
        intent.putExtra(EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY, getIntent().getStringArrayExtra(EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY));
        intent.putExtra("authTokenType", getIntent().getStringExtra("authTokenType"));
        startActivityForResult(intent, 1);
        this.mPendingRequest = 1;
    }

    private int getItemIndexToSelect(ArrayList<Account> accounts, String selectedAccountName, boolean selectedAddNewAccount) {
        if (selectedAddNewAccount) {
            return accounts.size();
        }
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).name.equals(selectedAccountName)) {
                return i;
            }
        }
        return -1;
    }

    private String[] getListOfDisplayableOptions(ArrayList<Account> accounts) {
        String[] listItems = new String[(this.mDisallowAddAccounts ? 0 : 1) + accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            listItems[i] = accounts.get(i).name;
        }
        if (!this.mDisallowAddAccounts) {
            listItems[accounts.size()] = getResources().getString(17040534);
        }
        return listItems;
    }

    private ArrayList<Account> getAcceptableAccountChoices(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsForPackage(this.mCallingPackage, this.mCallingUid);
        ArrayList<Account> accountsToPopulate = new ArrayList<>(accounts.length);
        for (Account account : accounts) {
            if ((this.mSetOfAllowableAccounts == null || this.mSetOfAllowableAccounts.contains(account)) && (this.mSetOfRelevantAccountTypes == null || this.mSetOfRelevantAccountTypes.contains(account.type))) {
                accountsToPopulate.add(account);
            }
        }
        return accountsToPopulate;
    }

    private Set<String> getReleventAccountTypes(Intent intent) {
        String[] allowedAccountTypes = intent.getStringArrayExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY);
        AuthenticatorDescription[] descs = AccountManager.get(this).getAuthenticatorTypes();
        Set<String> supportedAccountTypes = new HashSet<>(descs.length);
        for (AuthenticatorDescription desc : descs) {
            supportedAccountTypes.add(desc.type);
        }
        if (allowedAccountTypes != null) {
            Set<String> setOfRelevantAccountTypes = Sets.newHashSet(allowedAccountTypes);
            setOfRelevantAccountTypes.retainAll(supportedAccountTypes);
            return setOfRelevantAccountTypes;
        }
        return supportedAccountTypes;
    }

    private Set<Account> getAllowableAccountSet(Intent intent) {
        Set<Account> setOfAllowableAccounts = null;
        ArrayList<Parcelable> validAccounts = intent.getParcelableArrayListExtra(EXTRA_ALLOWABLE_ACCOUNTS_ARRAYLIST);
        if (validAccounts != null) {
            setOfAllowableAccounts = new HashSet<>(validAccounts.size());
            for (Parcelable parcelable : validAccounts) {
                setOfAllowableAccounts.add((Account) parcelable);
            }
        }
        return setOfAllowableAccounts;
    }

    private void overrideDescriptionIfSupplied(String descriptionOverride) {
        TextView descriptionView = (TextView) findViewById(16909113);
        if (!TextUtils.isEmpty(descriptionOverride)) {
            descriptionView.setText(descriptionOverride);
        } else {
            descriptionView.setVisibility(8);
        }
    }

    private final void populateUIAccountList(String[] listItems) {
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter((ListAdapter) new ArrayAdapter(this, R.layout.simple_list_item_single_choice, listItems));
        list.setChoiceMode(1);
        list.setItemsCanFocus(false);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ChooseTypeAndAccountActivity.this.mSelectedItemIndex = position;
                ChooseTypeAndAccountActivity.this.mOkButton.setEnabled(true);
            }
        });
        if (this.mSelectedItemIndex == -1) {
            return;
        }
        list.setItemChecked(this.mSelectedItemIndex, true);
        if (!Log.isLoggable(TAG, 2)) {
            return;
        }
        Log.v(TAG, "List item " + this.mSelectedItemIndex + " should be selected");
    }
}
